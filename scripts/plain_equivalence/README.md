# Plain BOQA: before and after the blended refactoring

Harness for issue #53. It runs the `plain` benchmark twice — once on the last
state before the #47/#50 refactoring, once on the refactored state — over
identical inputs, so that any difference in the results comes from the code.

This README and the scripts it calls live on `ph/issue-53-plain-equivalence`.
Run every command below from a checkout of that branch, at the repository
root — except inside a `../boqa-worktrees/...` worktree, where noted.

## The two versions, plus PR #61

All three are local tags:

| Tag | Commit | What it is |
|---|---|---|
| `before_sealed_refactoring` | `c89782b` | `develop` tip, the merge of PR #45 |
| `after_sealed_refactoring` | `34e63e6` | PR #47 head on 2026-09-21, with #50 merged in |
| `pr61_fast_benchmarking` | `873c3e0` | PR #61 head on 2026-09-21, a speed rewrite of the `after` path |

The first two are ordinary local tags. The third points at a commit that only
exists on a branch, so fetch it before tagging it:

```bash
# Make PR #61's commit reachable locally
git fetch origin lc/fast_benchmarking

# Pin all three versions as local tags
git tag before_sealed_refactoring c89782b
git tag after_sealed_refactoring  34e63e6
git tag pr61_fast_benchmarking    873c3e0
```

Build each in its own worktree, so none overwrites another's `target/`. Run
the block below from the repository root, into a dedicated `boqa-worktrees`
directory next to the repository, shared with whatever other worktrees this
repository ends up needing — each one named after its own tag, so the
directory stays self-explanatory as it grows. `git worktree add` creates
missing directories.

```bash
# Create one worktree per tag
git worktree add --detach ../boqa-worktrees/before_sealed_refactoring before_sealed_refactoring
git worktree add --detach ../boqa-worktrees/after_sealed_refactoring  after_sealed_refactoring
git worktree add --detach ../boqa-worktrees/pr61_fast_benchmarking    pr61_fast_benchmarking

# Build the CLI jar in each one
(cd ../boqa-worktrees/before_sealed_refactoring && ./mvnw -pl boqa-cli,boqa-core -am -B --quiet -Prelease package)
(cd ../boqa-worktrees/after_sealed_refactoring  && ./mvnw -pl boqa-cli,boqa-core -am -B --quiet -Prelease package)
(cd ../boqa-worktrees/pr61_fast_benchmarking    && ./mvnw -pl boqa-cli,boqa-core -am -B --quiet -Prelease package)
```

Each worktree's POM version names its jar: `boqa-cli-0.1.0.jar`,
`boqa-cli-0.2.4.jar` and `boqa-cli-0.2.5.jar` respectively. `--quiet` only
silences Maven's own log; `package` still runs the test phase first, and its
`ERROR`/`WARN`/`INFO` lines reach the console regardless of `-q`, because
they come from the tests' own logging, not from Maven. That output is
expected noise, not a failure signal — a real failure prints an `[ERROR]`
block from Maven itself and a non-zero exit. Confirm each jar actually came
out the other end:

```bash
ls -l ../boqa-worktrees/before_sealed_refactoring/boqa-cli/target/boqa-cli-0.1.0.jar
ls -l ../boqa-worktrees/after_sealed_refactoring/boqa-cli/target/boqa-cli-0.2.4.jar
ls -l ../boqa-worktrees/pr61_fast_benchmarking/boqa-cli/target/boqa-cli-0.2.5.jar
```

## The inputs

The same files feed both runs. A different HPO release on either side would
mean measuring the data instead of the code.

- Ontology and annotations: `data/human-phenotype-ontology/v2026-02-16`
- Phenopackets: `data/phenopacket-store/0.1.26` (9,588 files)

### Getting the data

`data/` is not tracked, so a fresh checkout needs it downloaded — with the
release tags pinned explicitly, since `latest` is a moving target.

```bash
# Build once, to get a CLI capable of running `download`
./mvnw -pl boqa-cli,boqa-core -am -B --quiet -Prelease package

# Fetch the pinned HPO and phenopacket-store releases
java -jar boqa-cli/target/boqa-cli-0.1.0.jar download \
    --hpo-release-tag v2026-02-16 \
    --phenopacket-store-release-tag 0.1.26 \
    --data-dir data
```

`select_sample.py` picks every nth phenopacket from the sorted listing — this
is systematic downsampling rather than random downsampling, so it needs no
seed to be reproducible, and it spreads evenly over the whole store rather
than leaving that to chance. That is evidence, not a
guarantee: a sample spread across 85 gene cohorts makes a structural or common
divergence likely to surface, but it cannot rule out one that only triggers on
a phenotype combination or disease structure absent from these 100 files. The
tradeoff is deliberate — a fraction of the full store's runtime and output for
a sample confident enough to act on; the full 9,588 files remain the fallback
if this sample ever turned up a difference worth chasing further.

```bash
python3 scripts/plain_equivalence/select_sample.py \
    --store data/phenopacket-store/0.1.26 \
    --count 100 \
    --out results/issue53/sample_100.txt
```

## The runs

This is where each version actually runs, producing the output files the next
step compares. `run_plain.sh` fixes every option except the jar, and records
the wall-clock time and the CLI's own output next to the results. It finds its
inputs relative to the repository rather than to the working directory, so it
can be called from anywhere; the output path is taken as given.

```bash
scripts/plain_equivalence/run_plain.sh \
    ../boqa-worktrees/before_sealed_refactoring/boqa-cli/target/boqa-cli-0.1.0.jar \
    results/issue53/before.json -L 100

scripts/plain_equivalence/run_plain.sh \
    ../boqa-worktrees/after_sealed_refactoring/boqa-cli/target/boqa-cli-0.2.4.jar \
    results/issue53/after.json -L 100

scripts/plain_equivalence/run_plain.sh \
    ../boqa-worktrees/pr61_fast_benchmarking/boqa-cli/target/boqa-cli-0.2.5.jar \
    results/issue53/pr61.json -L 100
```

`-L` is mandatory here, not just a comparison choice: this harness's first finding was that
`after_sealed_refactoring` NPEs without it, unboxing the null `-L` field instead of using the
guarded limit a few lines above it. `before_sealed_refactoring` doesn't have the bug, and PR #61
fixes it, but all three get `-L 100` too, so every run reports the same number of diseases per patient.

Results land in `results/`, which is not tracked. Each run locks its output
path (`mkdir`, atomic, released on exit), so a second invocation racing the
same `.json`/`.log` fails fast instead of corrupting them — run the two
commands above one after another, not both at once.

Each `.log` ends with the wall-clock time; nothing compares this automatically, so pull it by hand:

```bash
grep "elapsed:" results/issue53/before.log results/issue53/after.log results/issue53/pr61.log
```

## The comparison

This is where the actual equivalence question gets answered: do the three
builds score patients the same way? The three JSON files aren't directly
diffable: the refactoring moved the disease identity out of `BoqaCounts` and
into a `candidate` object, so `before.json` and `after.json`/`pr61.json` don't
even share a shape. `parse_results.py` reduces either shape to one common set
of columns — `patient_id, diagnosis_id, rank, disease_id, score, tp, fp, tn,
fn`. `compare_runs.py` then does the actual diffing, hardest test first:
scores compared exactly (identical maths should give identical doubles), then
ranks compared by `(score, disease_id)` rather than position, since a genuine
tie under `parallelStream` has no defined order in either version — a naive
positional diff would flag those as differences when nothing actually moved.

### A small worked example, before trusting either script on real data

Two tiny fixtures under `scripts/plain_equivalence/example/` prove both the
parsing and the diffing do the right thing — no real data needed. One
patient, three diseases: `OMIM:100`'s score genuinely differs between the
fixtures (`-5.0` vs `-5.5`); `OMIM:200` and `OMIM:300` are tied at `-6.0` in
both, just listed in swapped order, the kind of reordering `parallelStream`
can introduce without meaning anything.

```bash
python3 scripts/plain_equivalence/parse_results.py scripts/plain_equivalence/example/before_demo.json --out /tmp/before_demo.csv
python3 scripts/plain_equivalence/parse_results.py scripts/plain_equivalence/example/after_demo.json  --out /tmp/after_demo.csv
```

`before_demo.json` uses the pre-refactoring shape (score and counts directly
on the entry); `after_demo.json` uses the refactored one (nested under
`component`/`candidate`). Both parse to the same column layout:

```
$ cat /tmp/before_demo.csv
patient_id,diagnosis_id,rank,disease_id,score,tp,fp,tn,fn
DEMO_1,OMIM:100,1,OMIM:100,-5.0,3,0,10,1
DEMO_1,OMIM:100,2,OMIM:200,-6.0,2,1,9,2
DEMO_1,OMIM:100,3,OMIM:300,-6.0,2,1,9,2

$ cat /tmp/after_demo.csv
patient_id,diagnosis_id,rank,disease_id,score,tp,fp,tn,fn
DEMO_1,OMIM:100,1,OMIM:100,-5.5,3,0,10,1
DEMO_1,OMIM:100,2,OMIM:300,-6.0,2,1,9,2
DEMO_1,OMIM:100,3,OMIM:200,-6.0,2,1,9,2
```

Same patient, same diagnosis, same disease identities and counts either way —
the only differences are OMIM:100's score and the tied pair's order, exactly
the two things the fixtures were built to differ by. That is the proof the
shape-normalization itself is correct, before any diffing logic runs at all.

```bash
python3 scripts/plain_equivalence/compare_runs.py /tmp/before_demo.csv /tmp/after_demo.csv
```

The output:

```
score differs for 1 disease-patient pairs
rank differs for 2 disease-patient pairs
  score  DEMO_1 OMIM:100: -5.0 vs -5.5
  rank   DEMO_1 OMIM:200: 2 vs 3
  rank   DEMO_1 OMIM:300: 3 vs 2
...
disease sets differing for a reason other than a tie at the cut: 0
score groups above the cut holding different diseases: 0
=> the two runs rank identically; only the order within tied scores differs
```

The real score change is caught (`score differs for 1...`). The swapped tie
moves both ranks but is not mistaken for a difference: `tie_analysis` checks
that `OMIM:200` and `OMIM:300` hold the same score in both runs and clears
them — exactly the distinction the real comparison below relies on to tell
"the refactoring changed something" from "two ties landed in a different
order."

### Running it on the real data

```bash
python3 scripts/plain_equivalence/parse_results.py results/issue53/before.json --out results/issue53/before.csv
python3 scripts/plain_equivalence/parse_results.py results/issue53/after.json  --out results/issue53/after.csv
python3 scripts/plain_equivalence/parse_results.py results/issue53/pr61.json   --out results/issue53/pr61.csv
```

Each CSV comes out to 10,000 rows: 100 phenopackets times the `-L 100` cap on
diseases reported per patient.

```bash
python3 scripts/plain_equivalence/compare_runs.py results/issue53/before.csv results/issue53/after.csv
python3 scripts/plain_equivalence/compare_runs.py results/issue53/after.csv  results/issue53/pr61.csv
```

The first call answers issue #53 directly: it reports zero score and count
differences, and confirms every rank difference is confined to tied scores.
The second checks whether PR #61's speed rewrite changes anything on top of
that — it doesn't; every column matches for all 100 patients.

## What the comparison found (2026-09-22)

Sample of 100 phenopackets, HPO release `v2026-02-16`, phenopacket-store
release `0.1.26`, 85 gene cohorts, `-L 100`, all three builds run back to back
on the same machine.

| | before (`c89782b`) | after (`34e63e6`) | PR #61 (`873c3e0`) |
|---|---|---|---|
| loading | ~1 s | ~1 s | ~1 s |
| scoring 100 phenopackets | 9 s | 33 s | 9 s |
| output | 2.6 MB | 12.3 MB | 12.3 MB |

`before` vs `after`: a naive read reports 3,347 differing ranks and 35
patients with a different disease set; all of it is reorderings within groups
of equal scores, which have no defined order in either version, plus the
arbitrary cut among diseases tied at the score `-L` truncates. `tie_analysis`
confirms it directly: 0 disease sets differing for any other reason, 0 score
groups above the cut holding different diseases. Counts and scores are
identical for the 9,935 of 10,000 pairs both runs report; diagnosis in top 1
is 50/50.

`after` vs `pr61`: identical in every column — 0 disease-set, score, count or
rank differences across all 10,000 pairs, and 0 patients with a changed
diagnosis rank. Dropping the `HP:0000118` filter, which PR #61 does, changes
nothing across 8,614 diseases and 100 patients.

Two things the runs turned up, both filed as issues: the refactored command
cannot run without `-L` (#62), and it recomputes every disease layer for every
patient instead of once.

Run twice, the two versions behave differently: the pre-refactoring jar
produces a different result for all 100 patients on a second run, while the
refactored one repeats itself exactly. Only tied scores move, so the ranked
score sequence is identical either way; what changes is which of the tied
diseases survives the `-L` cut. Any comparison at the cut is therefore between
two moving targets on the old side.

## What the comparison found (2026-09-23)

Sample of 100 phenopackets, HPO release `v2026-02-16`, phenopacket-store
release `0.1.26`, 85 gene cohorts, all three builds run back to back on the
same machine — but `-L 8614`, the total disease count, so every disease is
reported for every patient.

| | before (`c89782b`) | after (`34e63e6`) | PR #61 (`873c3e0`) |
|---|---|---|---|
| scoring 100 phenopackets | 10 s | 36 s | 13 s |
| output | 222 MB | 564 MB | 564 MB |

`before` vs `after`: disease set differs for 0 patients, every patient's
diagnosis appears in both reports, and counts and scores are identical for
all 861,400 disease-patient pairs. `rank differs for 788,952 disease-patient
pairs` looks large, but `tie_analysis` explains it completely: 0 disease sets
differing for any other reason, 0 score groups holding different diseases —
every one of those rank changes is reordering within a group of diseases
tied at the same score, which has no defined order under `parallelStream`.
Diagnosis rank changed for 16 patients, for the same reason: their diagnosis
ties with other diseases at a shared score, and which one lands first shifts
between runs.

`after` vs `pr61`: identical in every column — 0 disease-set, score, count or
rank differences across all 861,400 pairs, 0 patients with a changed
diagnosis rank.

The scoring is unchanged: every disease gets the same counts and score in all
three builds; the only thing that ever moves is the order among diseases tied
at an identical score.
