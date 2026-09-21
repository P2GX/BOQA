#!/usr/bin/env bash
# Run the plain BOQA benchmark for one build of the CLI against the pinned inputs.
# Everything that could differ between the two versions is fixed here, so the only
# variable left is the jar.

set -uo pipefail

# Resolve the inputs relative to the repository, not to the caller's directory
SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)

if [ $# -lt 2 ]; then
    echo "usage: $0 <boqa-cli.jar> <output.json> [extra CLI args...]" >&2
    exit 2
fi

JAR="$1"
OUT="$2"
shift 2

HPO_DATA="${HPO_DATA:-$REPO_ROOT/data/human-phenotype-ontology/latest_20260504}"
PHENOPACKET_LIST="${PHENOPACKET_LIST:-$REPO_ROOT/results/issue53/sample_100.txt}"

mkdir -p "$(dirname "$OUT")"
LOG="${OUT%.json}.log"

start=$(date +%s)
java -jar "$JAR" plain \
    --ontology "$HPO_DATA/hp.json" \
    --disease-phenotype-associations "$HPO_DATA/phenotype.hpoa" \
    --phenopackets "$PHENOPACKET_LIST" \
    --out "$OUT" \
    "$@" 2>&1 | tee "$LOG"
status=${PIPESTATUS[0]}

echo "elapsed: $(( $(date +%s) - start ))s, exit status: $status" | tee -a "$LOG"
exit "$status"
