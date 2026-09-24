#!/usr/bin/env bash
# Run the plain BOQA benchmark for one build of the CLI against the pinned inputs.
# Everything that could differ between the two versions is fixed here, so the only
# variable left is the jar.

set -uo pipefail

# Resolve the inputs relative to the repository, not to the caller's directory
SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)

if [ $# -lt 3 ]; then
    echo "usage: $0 <boqa-cli.jar> <output.json> <phenopacket_list> [--force] [extra CLI args...]" >&2
    exit 2
fi

JAR="$1"
OUT="$2"
PHENOPACKET_LIST="$3"
shift 3

# Pull --force out of the extra args so it never reaches the java command
FORCE=0
EXTRA_ARGS=()
for arg in "$@"; do
    if [ "$arg" = "--force" ]; then
        FORCE=1
    else
        EXTRA_ARGS+=("$arg")
    fi
done
set -- "${EXTRA_ARGS[@]+"${EXTRA_ARGS[@]}"}"

HPO_DATA="${HPO_DATA:-$REPO_ROOT/data/human-phenotype-ontology/v2026-02-16}"

mkdir -p "$(dirname "$OUT")"
LOG="${OUT%.json}.log"

# Refuse to silently clobber a previous run's results; --force opts in
if [ "$FORCE" -ne 1 ] && { [ -e "$OUT" ] || [ -e "$LOG" ]; }; then
    echo "$0: $OUT or $LOG already exists - refusing to overwrite; pass --force to overwrite" >&2
    exit 1
fi

# Refuse to race another run writing the same output: mkdir is atomic, so only
# one concurrent invocation can claim the lock directory
LOCK="${OUT%.json}.lock"
if ! mkdir "$LOCK" 2>/dev/null; then
    echo "$0: another run already holds $LOCK (writing $OUT?) - refusing to race it" >&2
    exit 1
fi
trap 'rmdir "$LOCK"' EXIT

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
