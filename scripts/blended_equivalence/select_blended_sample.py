#!/usr/bin/env python3
"""Pick the two-gene patients the blended CLI can be run on and write one TSV row per patient.

Each row holds the phenopacket, its two diagnoses and the NCBI ids of its two causal
genes, which become the anchor genes (-g) of the blended run.
"""

import argparse
import csv
import hashlib
import json
from collections import Counter, defaultdict
from pathlib import Path

COLUMNS = ["phenopacket_path", "diagnosis_ids", "anchor_genes"]


def read_gene_associations(path):
    """Map gene symbols to NCBI gene ids, NCBI gene ids to disease ids, and back."""
    ncbi_ids_by_symbol = defaultdict(set)
    disease_ids_by_gene = defaultdict(set)
    genes_by_disease = defaultdict(set)
    with path.open() as handle:
        for row in csv.DictReader(handle, delimiter="\t"):
            ncbi_ids_by_symbol[row["gene_symbol"]].add(row["ncbi_gene_id"])
            disease_ids_by_gene[row["ncbi_gene_id"]].add(row["disease_id"])
            genes_by_disease[row["disease_id"]].add(row["ncbi_gene_id"])
    return ncbi_ids_by_symbol, disease_ids_by_gene, genes_by_disease


def read_annotated_diseases(path):
    """Disease ids that have phenotype annotations, i.e. that BOQA can score."""
    with path.open() as handle:
        return {line.split("\t", 1)[0] for line in handle if not line.startswith("#")}


def read_diagnoses(phenopacket):
    """Pairs of (disease id, set of causal gene symbols), one per interpretation."""
    diagnoses = []
    for interpretation in phenopacket.get("interpretations", []):
        diagnosis = interpretation["diagnosis"]
        gene_symbols = {
            genomic["variantInterpretation"]["variationDescriptor"]["geneContext"]["symbol"]
            for genomic in diagnosis.get("genomicInterpretations", [])
            if "geneContext" in genomic.get("variantInterpretation", {}).get("variationDescriptor", {})
        }
        diagnoses.append((diagnosis["disease"]["id"], gene_symbols))
    return diagnoses


def exclusion_reason(diagnoses, ncbi_ids_by_symbol, disease_ids_by_gene, genes_by_disease,
                     annotated_diseases):
    """Why a patient cannot be used, or None if it can."""
    if len(diagnoses) != 2:
        return f"{len(diagnoses)} diagnoses"
    gene_symbols = set().union(*(symbols for _, symbols in diagnoses))
    if len(gene_symbols) != 2 or any(len(symbols) != 1 for _, symbols in diagnoses):
        return "not one distinct gene per diagnosis"
    if any(len(ncbi_ids_by_symbol.get(symbol, ())) != 1 for symbol in gene_symbols):
        return "gene symbol without a unique NCBI id"
    if any(disease_id not in annotated_diseases for disease_id, _ in diagnoses):
        return "diagnosis not annotated in phenotype.hpoa"

    # Both diagnoses must be among the anchor genes' diseases, or the true pair is never formed
    anchor_genes = {ncbi_id for symbol in gene_symbols for ncbi_id in ncbi_ids_by_symbol[symbol]}
    anchor_diseases = set().union(*(disease_ids_by_gene[gene] for gene in anchor_genes))
    if any(disease_id not in anchor_diseases for disease_id, _ in diagnoses):
        return "diagnosis not linked to its gene in genes_to_disease.txt"

    # The CLI never blends two diseases that share a gene, so the true pair would be missing
    first_disease, second_disease = (disease_id for disease_id, _ in diagnoses)
    if genes_by_disease[first_disease] & genes_by_disease[second_disease]:
        return "diagnoses share a gene, so they are never blended"
    return None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--phenopackets", type=Path, required=True,
                        help="Directory of multi-diagnosis phenopackets (e.g. data/mgd)")
    parser.add_argument("--hpo-data", type=Path, required=True,
                        help="Directory holding phenotype.hpoa and genes_to_disease.txt")
    parser.add_argument("--out", type=Path, required=True,
                        help="TSV written for run_blended.sh")
    arguments = parser.parse_args()

    ncbi_ids_by_symbol, disease_ids_by_gene, genes_by_disease = read_gene_associations(
        arguments.hpo_data / "genes_to_disease.txt")
    annotated_diseases = read_annotated_diseases(arguments.hpo_data / "phenotype.hpoa")

    # Sort by file name, so the order never depends on the filesystem
    all_phenopackets = sorted(arguments.phenopackets.glob("*.json"))
    if not all_phenopackets:
        raise SystemExit(f"No phenopackets found in {arguments.phenopackets}")

    rows, exclusions = [], Counter()
    for path in all_phenopackets:
        diagnoses = read_diagnoses(json.loads(path.read_text()))
        reason = exclusion_reason(diagnoses, ncbi_ids_by_symbol, disease_ids_by_gene,
                                  genes_by_disease, annotated_diseases)
        if reason:
            exclusions[reason] += 1
            continue
        # Sort by diagnosis, and list each diagnosis's gene at the same position
        diagnosis_ids, anchor_genes = [], []
        for disease_id, symbols in sorted(diagnoses, key=lambda diagnosis: diagnosis[0]):
            (symbol,) = symbols
            (ncbi_id,) = ncbi_ids_by_symbol[symbol]
            diagnosis_ids.append(disease_id)
            anchor_genes.append(ncbi_id)
        rows.append([str(path.resolve()), ",".join(diagnosis_ids), ",".join(anchor_genes)])

    arguments.out.parent.mkdir(parents=True, exist_ok=True)
    with arguments.out.open("w", newline="") as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        writer.writerow(COLUMNS)
        writer.writerows(rows)

    # Digest file names and anchors, so the same sample is recognisable wherever the data lives
    sample_key = "".join(f"{Path(row[0]).name}\t{row[1]}\t{row[2]}\n" for row in rows)
    digest = hashlib.sha256(sample_key.encode()).hexdigest()

    print(f"phenopackets: {arguments.phenopackets} ({len(all_phenopackets)} files)")
    print(f"selected:     {len(rows)}")
    for reason, count in exclusions.most_common():
        print(f"excluded:     {count:>3}  {reason}")
    print(f"digest:       {digest[:16]}")
    print(f"out:          {arguments.out}")


if __name__ == "__main__":
    main()
