#!/usr/bin/env python3
"""Read BOQA result JSONs of either shape and write one tidy row per ranked disease.

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


def sort_blend(disease_id):
    """Write a blended id such as OMIM:b-OMIM:a with its diseases in sorted order."""
    return "-".join(sorted(disease_id.split("-")))


def read_results(result_json):
    """The per-patient results of one file, and the shape they are written in."""
    results = json.loads(result_json.read_text())["results"]
    if not results or not results[0]["boqaResults"]:
        raise SystemExit(f"No results in {result_json}")
    return results, detect_shape(results[0]["boqaResults"][0])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("result_json", type=Path, nargs="+",
                        help="One or more result files of the same run, all in the same shape")
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--blended", action="store_true",
                        help="Sort the diseases in blended ids, and make the diagnosis "
                             "the blend of all the patient's diagnoses")
    arguments = parser.parse_args()

    shapes, patient_ids = set(), set()
    arguments.out.parent.mkdir(parents=True, exist_ok=True)
    with arguments.out.open("w", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(COLUMNS)
        for result_json in arguments.result_json:
            results, shape = read_results(result_json)
            if shapes and shape not in shapes:
                raise SystemExit(f"{result_json} is in shape {shape}, the files before it are not; "
                                 f"parse each run separately")
            shapes.add(shape)
            for result in results:
                patient = result["patientData"]
                # Rows are grouped by patient later, so one patient in two files would merge
                if patient["id"] in patient_ids:
                    raise SystemExit(f"Patient {patient['id']} appears twice ({result_json})")
                patient_ids.add(patient["id"])

                # A phenopacket may carry no diagnosis; rank metrics skip those patients
                diagnoses = patient.get("diagnosis") or []
                if arguments.blended:
                    diagnosis_id = sort_blend("-".join(diagnosis["id"] for diagnosis in diagnoses))
                else:
                    diagnosis_id = diagnoses[0]["id"] if diagnoses else ""

                for rank, entry in enumerate(result["boqaResults"], start=1):
                    disease_id, score, counts = read_entry(entry, shape)
                    if arguments.blended:
                        disease_id = sort_blend(disease_id)
                    writer.writerow([patient["id"], diagnosis_id, rank, disease_id, repr(score),
                                     counts["tpBoqaCount"], counts["fpBoqaCount"],
                                     counts["tnBoqaCount"], counts["fnBoqaCount"]])

    print(f"files={len(arguments.result_json)}, shape={shapes.pop()}, "
          f"patients={len(patient_ids)}, out={arguments.out}")


if __name__ == "__main__":
    main()
