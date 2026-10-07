# Blended BOQA: before and after the blended refactoring

Harness for issue #53, second part. It runs the `blended` benchmark on the last
state before the #47/#50 refactoring and on the refactored state, over
identical inputs, so that any difference in the results comes from the code.
A third build adds two bug fixes on top of the refactored state, to show that
they don't change the output either.

This README and the scripts it calls live on `ph/issue-53-blended-equivalence`.
Run every command below from a checkout of that branch, at the repository
root, except inside a `../boqa-worktrees/...` worktree, where noted. The plain
comparison is in `README_plain.md`; this one reuses its builds, its data and
its `parse_results.py` / `compare_runs.py`.

## The three builds

| Role | Commit | What it is | Worktree |
|---|---|---|---|
| before | `c89782b` | `develop` tip, the merge of PR #45 | `before_sealed_refactoring` |
| after | `2642b11` | merge of PR #61, the `bboqa` tip until PR #69 | `bboqa_pr61` |
| after + fixes | `6fe826f` | PR #69 (`ph/blended-cli-fixes`): `2642b11` plus two fixes | `blended_cli_fixes` |

The `before` worktree and jar are the ones from `README_plain.md`. The other two
are created the same way, detached at their commit:

```bash
# Fetch bboqa, which contains the two fixes since #69 was merged
git fetch origin bboqa

# One worktree per build
git worktree add --detach ../boqa-worktrees/bboqa_pr61        2642b11
git worktree add --detach ../boqa-worktrees/blended_cli_fixes 6fe826f

# Build the CLI jar in each one
(cd ../boqa-worktrees/bboqa_pr61        && ./mvnw -pl boqa-cli,boqa-core -am -B --quiet -Prelease package)
(cd ../boqa-worktrees/blended_cli_fixes && ./mvnw -pl boqa-cli,boqa-core -am -B --quiet -Prelease package)

ls -l ../boqa-worktrees/before_sealed_refactoring/boqa-cli/target/boqa-cli-0.1.0.jar
ls -l ../boqa-worktrees/bboqa_pr61/boqa-cli/target/boqa-cli-0.2.5.jar
ls -l ../boqa-worktrees/blended_cli_fixes/boqa-cli/target/boqa-cli-0.2.5.jar
```

The two fixes, merged into `bboqa` with PR #69:

- `760291e`: `blended` threw a `NullPointerException` whenever `-L` was left
  out, the same bug #62 reported for `plain`.
- `6fe826f`: `CandidateDisease.createCandidateDiseases` threw
  `UnsupportedOperationException` for any two anchors on different genes.
  Adds `CandidateDiseaseTest`.

## The inputs

- Ontology, annotations and disease-gene associations:
  `data/human-phenotype-ontology/v2026-02-16` (`hp.json`, `phenotype.hpoa`,
  `genes_to_disease.txt`), fetched by the `download` step in `README_plain.md`.
- Phenopackets: `data/mgd/`, 130 phenopackets of patients with more than one
  genetic diagnosis. They come from a private repository of the group and are
  not part of phenopacket-store; ask the maintainers for access. The selection
  step below prints a digest, so you can confirm you picked the same sample.

## The sample

`blended` needs anchor genes (`-g`). Here they are the patient's own causal
genes, so each patient is run with the two genes behind their two diagnoses,
and the correct answer is the blend of both diagnoses.
`select_blended_sample.py` keeps every patient the CLI can be run on that way:
exactly two diagnoses, one distinct gene per diagnosis, both genes mapped to a
unique NCBI id, both diagnoses annotated in `phenotype.hpoa`, and no gene
shared between the two diagnoses (the CLI never blends two diseases that share
a gene, so the true pair would be missing).

```bash
python3 scripts/cli_equivalence/select_blended_sample.py \
    --phenopackets data/mgd \
    --hpo-data data/human-phenotype-ontology/v2026-02-16 \
    --out results/blended/sample_mgd.tsv
```

The output:

```
phenopackets: data/mgd (130 files)
selected:     112
excluded:      14  3 diagnoses
excluded:       3  diagnosis not annotated in phenotype.hpoa
excluded:       1  not one distinct gene per diagnosis
digest:       dcfedc9a7b155f54
out:          results/blended/sample_mgd.tsv
```

Each row of the TSV holds the phenopacket path, the two diagnoses, and their
NCBI gene ids in the same order.

## The runs

The CLI reads only one phenopacket per call, so `run_blended.sh` calls it once
per row of the TSV. Everything except the jar is fixed:

```
blended --ontology hp.json --disease-phenotype-associations phenotype.hpoa
        --disease-gene-associations genes_to_disease.txt
        --phenopackets <patient> --anchor-gene <gene1>,<gene2>
        --random-genes 0 --iterations 1 --limit 10000
```

- `--random-genes 0`: with more than one anchor gene, the CLI would otherwise
  add 10 randomly drawn genes, from an unseeded shuffle, so no two runs could be
  compared.
- `--iterations 1`: without random genes, repetitions add nothing.
- `--limit 10000`: far above the largest list any patient gets (at most 18
  single diseases plus their pairs), so nothing is ever cut off.
- Two anchor genes select the `ANCHOR_VS_ANCHOR` strategy: the anchors'
  diseases, plus every pair of them that shares no gene.

```bash
scripts/cli_equivalence/run_blended.sh \
    ../boqa-worktrees/before_sealed_refactoring/boqa-cli/target/boqa-cli-0.1.0.jar \
    results/blended/before \
    results/blended/sample_mgd.tsv

scripts/cli_equivalence/run_blended.sh \
    ../boqa-worktrees/bboqa_pr61/boqa-cli/target/boqa-cli-0.2.5.jar \
    results/blended/after \
    results/blended/sample_mgd.tsv

scripts/cli_equivalence/run_blended.sh \
    ../boqa-worktrees/blended_cli_fixes/boqa-cli/target/boqa-cli-0.2.5.jar \
    results/blended/after_fixes \
    results/blended/sample_mgd.tsv
```

Each output directory gets one `.json` and `.log` per patient, a `run.log`
with the time and exit status of every patient, and `results.txt`, the list of
this run's successful results. The script refuses a non-empty output directory
unless you pass `--force`, and it refuses a gene id that is not of the form
`NCBIGene:<digits>`, since the CLI silently skips anchor genes it does not
know. It exits non-zero if any patient failed. Run the three one after another,
not at once, so the timings stay comparable.

## The comparison

`parse_results.py` reads the files listed in `results.txt`, never `*.json`,
so results left over from an earlier run are never picked up. `--blended`
makes the ids comparable:

- A blended disease has an id such as `OMIM:162200-OMIM:163950`, and the order
  of the two parts depends on hash iteration. `--blended` sorts them.
- The diagnosis becomes the sorted blend of the patient's two diagnoses, so
  `compare_runs.py` finds the rank of the true pair.

```bash
for build in before after after_fixes; do
    python3 scripts/cli_equivalence/parse_results.py \
        $(cat results/blended/$build/results.txt) \
        --blended --out results/blended/$build.csv
done

python3 scripts/cli_equivalence/compare_runs.py results/blended/before.csv results/blended/after.csv
python3 scripts/cli_equivalence/compare_runs.py results/blended/after.csv  results/blended/after_fixes.csv
```

The first call answers issue #53 for `blended`. The second checks that the two
fixes change nothing.

## What the comparison found (2026-09-24 and 2026-09-28)

112 patients from `data/mgd`, HPO release `v2026-02-16`, `ANCHOR_VS_ANCHOR`,
`-r 0 -i 1 -L 10000`, the builds run one after another on the same machine.

| | before (`c89782b`) | after (`2642b11`) | after + fixes (`6fe826f`) |
|---|---|---|---|
| patients finished | 112 | 112 | 112 |
| run time, 112 CLI calls | 151 s | 155 s | 153 s |
| output | 1.2 MB | 2.3 MB | 2.3 MB |
| entries reported (of which blends) | 1,488 (880) | 1,488 (880) | 1,488 (880) |
| true pair at rank 1 | 59 | 58 | 58 |

`before` vs `after`: the disease set differs for 0 patients, and scores and
counts are identical for all 1,488 entries. The rank differs for 125 entries,
and `tie_analysis` explains all of them: each is a reordering within a group of
entries tied at the same score. The rank of the true pair changed for 4
patients, for the same reason: in each case the pair is tied with other
entries, and the tied group occupies the same ranks in both builds.

These counts differ from run to run, because the `before` build orders tied
entries differently on every run, as in the plain comparison. A second,
independent run gave 119 rank differences and 3 patients with a changed
diagnosis rank; its `after` and `after + fixes` CSVs were byte-identical to
the first run's, and its `before` CSV differed only within tied groups. The
conclusion is the same for every run: 0 differences outside tied groups.

`after` vs `after + fixes`: identical in every column, and the two CSVs are
byte-identical.

The scoring is unchanged: every entry gets the same counts and score in all
three builds. The only thing that moves is the order among entries tied at an
identical score.

## What this does and does not show

- Only `ANCHOR_VS_ANCHOR` with two anchor genes is covered. A single anchor
  gene (`ANCHOR_VS_ALL`) and runs with random genes are not.
- The refactored command still builds every blend with the old code
  (`BlendedDiseaseData`) and then hands each result to the new
  `CandidateDisease.createCandidateDiseases`, with the same placeholder gene
  symbol for all of them. That second step therefore never forms a blend: all
  1,488 entries come out as single results, 880 of them wrapping an old blended
  id. The comparison shows that the command's output is unchanged; it does not
  exercise the new blending code at all.
- In the refactored output, a blend therefore looks like a single disease. An
  entry such as `OMIM:162200-OMIM:163950` has one `component`, not the
  `components` and `blendedDisease` of a blended result, and every entry
  carries the placeholders `"diseaseLabel": "LABEL"`, `"geneId": "geneID"` and
  `"geneSymbol": "geneName"`. Only the `-` in the id shows that an entry is a
  blend.
- `compare_runs.py` treats the lowest score in each list as a possible cut,
  even though nothing is cut here. So its tie check is slightly weaker for that
  one score group.
