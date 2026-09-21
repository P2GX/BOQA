# Plain BOQA: before and after the blended refactoring

Harness for issue #53. It runs the `plain` benchmark twice — once on the last
state before the #47/#50 refactoring, once on the refactored state — over
identical inputs, so that any difference in the results comes from the code.

## The two versions

Both are local tags:

| Tag | Commit | What it is |
|---|---|---|
| `before_sealed_refactoring` | `c89782b` | `develop` tip, the merge of PR #45 |
| `after_sealed_refactoring` | `34e63e6` | PR #47 head on 2026-09-21, with #50 merged in |

Build each in its own worktree, so neither overwrites the other's `target/`:

```bash
git worktree add --detach ../boqa-before before_sealed_refactoring
git worktree add --detach ../boqa-after  after_sealed_refactoring

(cd ../boqa-before && ./mvnw -pl boqa-cli,boqa-core -am -B --quiet -Prelease package)
(cd ../boqa-after  && ./mvnw -pl boqa-cli,boqa-core -am -B --quiet -Prelease package)
```

## The inputs

The same files feed both runs. A different HPO release on either side would
mean measuring the data instead of the code.

- Ontology and annotations: `data/human-phenotype-ontology/latest_20260504`
- Phenopackets: `data/phenopacket-store/latest_20260504` (9,588 files)

`select_sample.py` picks every nth phenopacket from the sorted listing, so the
sample is reproducible and spreads over the whole store rather than clustering
in one cohort. The 100-file sample covers 85 gene cohorts.

```bash
python3 scripts/plain_equivalence/select_sample.py \
    --store data/phenopacket-store/latest_20260504 \
    --count 100 \
    --out results/issue53/sample_100.txt
```

## The runs

`run_plain.sh` fixes every option except the jar, and records the wall-clock
time and the CLI's own output next to the results.

```bash
scripts/plain_equivalence/run_plain.sh \
    ../boqa-before/boqa-cli/target/boqa-cli-0.1.0.jar \
    results/issue53/before.json -L 100

scripts/plain_equivalence/run_plain.sh \
    ../boqa-after/boqa-cli/target/boqa-cli-0.2.4.jar \
    results/issue53/after.json -L 100
```

Results land in `results/`, which is not tracked.

## What the first comparison found (2026-09-21)

Sample of 100 phenopackets from `latest_20260504`, 85 gene cohorts, `-L 100`,
both runs back to back on the same machine. Each run reports 100 x 100 =
10,000 entries; 9,936 of those pairs appear in both runs, the other 64 per
side being diseases tied at the score the limit cuts through.

| | before (`c89782b`) | after (`34e63e6`) |
|---|---|---|
| counts and scores | identical for the 9,936 pairs both runs report |
| ranking | identical: every score holds the same diseases |
| diagnosis in top 1 | 50 | 50 |
| loading | ~1 s | ~1 s |
| scoring 100 phenopackets | 6 s | 36 s |
| output | 2.6 MB | 12.3 MB |

The scoring is unchanged. A naive comparison reports 3,402 differing ranks and
38 patients with different disease sets; all of them are reorderings within
groups of equal scores, which have no defined order in either version, plus the
arbitrary cut among diseases tied at the score `-L` truncates.

Two things the runs turned up, both filed as issues: the refactored command
cannot run without `-L`, and it recomputes every disease layer for every
patient instead of once.
