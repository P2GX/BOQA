#!/usr/bin/env python3
"""Pick a reproducible sample of phenopackets and write the list the CLI's -p option expects."""

import argparse
import hashlib
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--store", type=Path, required=True,
                        help="Phenopacket store directory to sample from")
    parser.add_argument("--count", type=int, default=100,
                        help="How many phenopackets to pick (default: %(default)s)")
    parser.add_argument("--out", type=Path, required=True,
                        help="File list written for the CLI's --phenopackets option")
    arguments = parser.parse_args()

    # Sort by relative path, so the order never depends on the filesystem
    all_phenopackets = sorted(arguments.store.rglob("*.json"),
                              key=lambda path: str(path.relative_to(arguments.store)))
    if not all_phenopackets:
        raise SystemExit(f"No phenopackets found under {arguments.store}")

    # Take every nth, so the sample spreads across the store instead of clustering in one cohort
    step = max(1, len(all_phenopackets) // arguments.count)
    sample = all_phenopackets[::step][:arguments.count]

    arguments.out.parent.mkdir(parents=True, exist_ok=True)
    arguments.out.write_text("".join(f"{path.resolve()}\n" for path in sample))

    # Digest the relative paths, so the same sample is recognisable wherever the store lives
    relative_paths = "".join(f"{path.relative_to(arguments.store)}\n" for path in sample)
    digest = hashlib.sha256(relative_paths.encode()).hexdigest()

    print(f"store:  {arguments.store} ({len(all_phenopackets)} phenopackets)")
    print(f"sample: {len(sample)} phenopackets, every {step}th")
    print(f"digest: {digest[:16]}")
    print(f"out:    {arguments.out}")


if __name__ == "__main__":
    main()
