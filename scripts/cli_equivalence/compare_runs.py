#!/usr/bin/env python3
"""Compare two normalized BOQA runs, hardest test first.

Identical maths should give identical doubles, so scores are compared exactly
before anything softer is tried. Ties are the one honest source of reordering,
so ranks are judged on the (score, disease_id) order rather than on position.
"""

import argparse
import csv
from collections import defaultdict
from pathlib import Path


def read_rows(path):
    with path.open() as handle:
        return list(csv.DictReader(handle))


def by_patient(rows):
    grouped = defaultdict(list)
    for row in rows:
        grouped[row["patient_id"]].append(row)
    return grouped


def diagnosis_rank(rows):
    """Rank at which a patient's own diagnosis appears, or None if it is not in the list."""
    for row in rows:
        if row["disease_id"] == row["diagnosis_id"]:
            return int(row["rank"])
    return None


def tie_analysis(before, after):
    """Decide whether the differences are ties, or a genuinely different ranking.

    Two runs rank identically if each score holds the same diseases in both. The
    score the result limit cuts through is excluded: it is truncated on both
    sides, so which of its tied diseases survives is arbitrary.
    """
    unexplained_sets, group_mismatches = [], []

    for patient_id in sorted(set(before) & set(after)):
        before_rows, after_rows = before[patient_id], after[patient_id]
        before_cut, after_cut = before_rows[-1]["score"], after_rows[-1]["score"]

        # Diseases reported by only one run must all sit at a cut score
        before_scores = {row["disease_id"]: row["score"] for row in before_rows}
        after_scores = {row["disease_id"]: row["score"] for row in after_rows}
        only_one_run = set(before_scores) ^ set(after_scores)
        if only_one_run:
            scores_of_differing = {before_scores.get(d) or after_scores.get(d)
                                   for d in only_one_run}
            if not scores_of_differing <= {before_cut, after_cut}:
                unexplained_sets.append(patient_id)

        # Above the cut, each score must hold exactly the same diseases
        def group_by_score(rows):
            grouped = defaultdict(set)
            for row in rows:
                grouped[row["score"]].add(row["disease_id"])
            return grouped

        before_groups, after_groups = group_by_score(before_rows), group_by_score(after_rows)
        for score in (set(before_groups) & set(after_groups)) - {before_cut, after_cut}:
            if before_groups[score] != after_groups[score]:
                group_mismatches.append((patient_id, score))

    print(f"\ndisease sets differing for a reason other than a tie at the cut: "
          f"{len(unexplained_sets)}")
    print(f"score groups above the cut holding different diseases: {len(group_mismatches)}")
    if not unexplained_sets and not group_mismatches:
        print("=> the two runs rank identically; only the order within tied scores differs")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("before_csv", type=Path)
    parser.add_argument("after_csv", type=Path)
    arguments = parser.parse_args()

    before = by_patient(read_rows(arguments.before_csv))
    after = by_patient(read_rows(arguments.after_csv))

    print(f"patients: before={len(before)} after={len(after)}")
    only_before = sorted(set(before) - set(after))
    only_after = sorted(set(after) - set(before))
    if only_before or only_after:
        print(f"  MISMATCH  only before: {only_before[:5]}  only after: {only_after[:5]}")
    else:
        print("  same patient set")

    # Per patient: same diseases, same scores, same counts, same order
    disease_set_mismatches, score_mismatches, count_mismatches, order_mismatches = [], [], [], []
    rank_changes = []

    for patient_id in sorted(set(before) & set(after)):
        before_rows, after_rows = before[patient_id], after[patient_id]

        before_by_disease = {row["disease_id"]: row for row in before_rows}
        after_by_disease = {row["disease_id"]: row for row in after_rows}
        if set(before_by_disease) != set(after_by_disease):
            disease_set_mismatches.append(patient_id)

        for disease_id in set(before_by_disease) & set(after_by_disease):
            before_row, after_row = before_by_disease[disease_id], after_by_disease[disease_id]
            if before_row["score"] != after_row["score"]:
                score_mismatches.append((patient_id, disease_id,
                                         before_row["score"], after_row["score"]))
            if [before_row[c] for c in ("tp", "fp", "tn", "fn")] != \
               [after_row[c] for c in ("tp", "fp", "tn", "fn")]:
                count_mismatches.append((patient_id, disease_id))
            if before_row["rank"] != after_row["rank"]:
                rank_changes.append((patient_id, disease_id,
                                     int(before_row["rank"]), int(after_row["rank"])))

        # Ties may legitimately reorder, so compare the sorted key instead of position
        before_order = [(row["score"], row["disease_id"]) for row in before_rows]
        after_order = [(row["score"], row["disease_id"]) for row in after_rows]
        if sorted(before_order) != sorted(after_order):
            order_mismatches.append(patient_id)

    print(f"disease set differs for {len(disease_set_mismatches)} patients")
    print(f"score differs for {len(score_mismatches)} disease-patient pairs")
    print(f"counts differ for {len(count_mismatches)} disease-patient pairs")
    print(f"rank differs for {len(rank_changes)} disease-patient pairs")

    for patient_id, disease_id, before_score, after_score in score_mismatches[:5]:
        print(f"  score  {patient_id} {disease_id}: {before_score} vs {after_score}")
    for patient_id, disease_id, before_rank, after_rank in rank_changes[:5]:
        print(f"  rank   {patient_id} {disease_id}: {before_rank} vs {after_rank}")

    # The metric that matters: where each patient's own diagnosis lands
    moved, missing_before, missing_after = [], 0, 0
    top1_before = top1_after = 0
    for patient_id in sorted(set(before) & set(after)):
        rank_before = diagnosis_rank(before[patient_id])
        rank_after = diagnosis_rank(after[patient_id])
        missing_before += rank_before is None
        missing_after += rank_after is None
        top1_before += rank_before == 1
        top1_after += rank_after == 1
        if rank_before != rank_after:
            moved.append((patient_id, rank_before, rank_after))

    print(f"\ndiagnosis in top 1: before={top1_before} after={top1_after}")
    print(f"diagnosis absent from the reported list: before={missing_before} after={missing_after}")
    print(f"diagnosis rank changed for {len(moved)} patients")
    for patient_id, rank_before, rank_after in moved[:10]:
        print(f"  {patient_id}: {rank_before} -> {rank_after}")

    tie_analysis(before, after)


if __name__ == "__main__":
    main()
