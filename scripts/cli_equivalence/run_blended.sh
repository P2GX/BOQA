#!/usr/bin/env bash
# Run the blended BOQA benchmark for one build of the CLI, once per patient of a
# sample written by select_blended_sample.py. Everything that could differ between
# the versions is fixed here, so the only variable left is the jar.

set -uo pipefail

# Resolve the inputs relative to the repository, not to the caller's directory
SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)

if [ $# -lt 3 ]; then
    echo "usage: $0 <boqa-cli.jar> <output_dir> <sample.tsv> [--force]" >&2
    exit 2
fi

JAR="$1"
OUT_DIR="$2"
SAMPLE="$3"
FORCE=0
if [ "${4:-}" = "--force" ]; then
    FORCE=1
elif [ $# -gt 3 ]; then
    echo "$0: unknown argument '$4'" >&2
    exit 2
fi

for input in "$JAR" "$SAMPLE"; do
    if [ ! -f "$input" ]; then
        echo "$0: $input not found" >&2
        exit 2
    fi
done

HPO_DATA="${HPO_DATA:-$REPO_ROOT/data/human-phenotype-ontology/v2026-02-16}"

# No anchor pair has more than a few hundred entries, so nothing is ever cut off
RESULTS_LIMIT=10000

# Refuse to silently clobber a previous run's results; --force opts in
if [ "$FORCE" -ne 1 ] && [ -n "$(ls -A "$OUT_DIR" 2>/dev/null)" ]; then
    echo "$0: $OUT_DIR is not empty - refusing to overwrite; pass --force to overwrite" >&2
    exit 1
fi
mkdir -p "$OUT_DIR"

# Refuse to race another run writing the same directory: mkdir is atomic, so only
# one concurrent invocation can claim the lock directory
LOCK="${OUT_DIR%/}.lock"
if ! mkdir "$LOCK" 2>/dev/null; then
    echo "$0: another run already holds $LOCK (writing $OUT_DIR?) - refusing to race it" >&2
    exit 1
fi
trap 'rmdir "$LOCK"' EXIT

RUN_LOG="$OUT_DIR/run.log"
echo "jar: $JAR" | tee "$RUN_LOG"
echo "sample: $SAMPLE" | tee -a "$RUN_LOG"

# Lists the results of this run only, so stale files left by an earlier run are never read
RESULT_LIST="$OUT_DIR/results.txt"
: > "$RESULT_LIST"

patient_count=0
failed_count=0
start=$(date +%s)
while IFS=$'\t' read -r phenopacket_path diagnosis_ids anchor_genes; do
    patient_count=$((patient_count + 1))
    patient=$(basename "$phenopacket_path" .json)
    out="$OUT_DIR/$patient.json"

    # A malformed gene id matches no disease, yet the CLI would still run and exit 0
    if ! [[ "$anchor_genes" =~ ^NCBIGene:[0-9]+,NCBIGene:[0-9]+$ ]]; then
        failed_count=$((failed_count + 1))
        echo "$patient_count $patient: malformed anchor genes $(printf '%q' "$anchor_genes"), skipped" | tee -a "$RUN_LOG"
        continue
    fi

    # One CLI call per patient, since the CLI reads only one phenopacket per run
    patient_start=$(date +%s)
    java -jar "$JAR" blended \
        --ontology "$HPO_DATA/hp.json" \
        --disease-phenotype-associations "$HPO_DATA/phenotype.hpoa" \
        --disease-gene-associations "$HPO_DATA/genes_to_disease.txt" \
        --phenopackets "$phenopacket_path" \
        --anchor-gene "$anchor_genes" \
        --random-genes 0 \
        --iterations 1 \
        --limit "$RESULTS_LIMIT" \
        --out "$out" \
        > "$OUT_DIR/$patient.log" 2>&1 < /dev/null
    status=$?

    if [ "$status" -eq 0 ]; then
        echo "$out" >> "$RESULT_LIST"
    else
        failed_count=$((failed_count + 1))
    fi
    echo "$patient_count $patient $diagnosis_ids $anchor_genes" \
         "elapsed: $(( $(date +%s) - patient_start ))s, exit status: $status" | tee -a "$RUN_LOG"
done < <(tail -n +2 "$SAMPLE")

echo "patients: $patient_count, failed: $failed_count," \
     "elapsed: $(( $(date +%s) - start ))s" | tee -a "$RUN_LOG"
[ "$failed_count" -eq 0 ]
