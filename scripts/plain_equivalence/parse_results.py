#!/usr/bin/env python3
"""Read a BOQA result JSON of either shape and write one tidy row per ranked disease.

The refactoring moved the disease identity out of the counts and into a candidate
object, so the two versions cannot be compared as JSON. Both shapes are reduced
here to the same columns.
"""

import argparse
import csv
import json
from pathlib import Path

COLUMNS = ["patient_id", "diagnosis_id", "rank", "disease_id", "score",
           "tp", "fp", "tn", "fn"]


def detect_shape(first_result):
    """Tell the pre-refactoring shape from the refactored one by where the score sits."""
    if "boqaScore" in first_result:
        return "before"
    if "component" in first_result:
        return "after"
    raise SystemExit(f"Unrecognised result shape: {sorted(first_result)}")


def read_entry(entry, shape):
    """Pull identity, score and counts out of one ranked entry."""
    if shape == "before":
        counts = entry["counts"]
        return counts["diseaseId"], entry["boqaScore"], counts
    component = entry["component"]
    return component["candidate"]["disease"]["diseaseId"], component["boqaScore"], component["counts"]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("result_json", type=Path)
    parser.add_argument("--out", type=Path, required=True)
    arguments = parser.parse_args()

    document = json.loads(arguments.result_json.read_text())
    results = document["results"]
    if not results or not results[0]["boqaResults"]:
        raise SystemExit(f"No results in {arguments.result_json}")

    shape = detect_shape(results[0]["boqaResults"][0])

    arguments.out.parent.mkdir(parents=True, exist_ok=True)
    with arguments.out.open("w", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(COLUMNS)
        for result in results:
            patient = result["patientData"]
            # A phenopacket may carry no diagnosis; rank metrics skip those patients
            diagnoses = patient.get("diagnosis") or []
            diagnosis_id = diagnoses[0]["id"] if diagnoses else ""
            for rank, entry in enumerate(result["boqaResults"], start=1):
                disease_id, score, counts = read_entry(entry, shape)
                writer.writerow([patient["id"], diagnosis_id, rank, disease_id, repr(score),
                                 counts["tpBoqaCount"], counts["fpBoqaCount"],
                                 counts["tnBoqaCount"], counts["fnBoqaCount"]])

    print(f"{arguments.result_json.name}: shape={shape}, patients={len(results)}, out={arguments.out}")


if __name__ == "__main__":
    main()
