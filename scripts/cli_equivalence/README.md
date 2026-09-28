# CLI before/after comparisons

Harness for issue #53: do the benchmark commands of the CLI still give the same
results after the #47/#50 refactoring? Each comparison runs one command on the
last state before the refactoring and on the refactored state, over identical
inputs, and diffs the results.

| Command | README | Sample | Result |
|---|---|---|---|
| `plain` | [README_plain.md](README_plain.md) | 100 and 1,000 phenopackets from phenopacket-store `0.1.26` | scoring unchanged; only tied diseases reorder |
| `blended` | [README_blended.md](README_blended.md) | 112 two-diagnosis patients from `data/mgd` | scoring unchanged; only tied entries reorder |

## The scripts

| Script | Used by | What it does |
|---|---|---|
| `select_sample.py` | plain | picks every nth phenopacket from the store |
| `run_plain.sh` | plain | runs `plain` for one build over a phenopacket list |
| `select_blended_sample.py` | blended | picks the two-gene patients from `data/mgd` |
| `run_blended.sh` | blended | runs `blended` for one build, once per patient |
| `parse_results.py` | both | reduces a result JSON of either build to one row per ranked disease (`--blended` for blended runs) |
| `compare_runs.py` | both | diffs two parsed runs and tells ties from real differences |

`example/` holds two tiny result files that show `parse_results.py` and
`compare_runs.py` at work without real data; see `README_plain.md`.

Results go to `results/`, which is not tracked.
