#!/bin/bash
# wQFM-GDL: Species tree estimation from multi-copy gene trees
#
# Usage: bash wQFM-GDL.sh -i <input_gene_trees> -o <output_species_tree>
#
# Pipeline:
#   1. Clean input gene trees (treeCleaner.py)
#   2. Resolve polytomies     (arb_resolve_polytomies.py)
#   3. Clean resolved trees   (treeCleaner.py)
#   4. DISCO decomposition    (disco.py)
#   5. Clean decomposed trees (treeCleaner.py)
#   6. Greedy consensus tree  (run_paup_consensus.pl)
#   7. DISCO rooting only     (disco.py --no-decomp)
#   8. Clean rooted trees     (treeCleaner.py)
#   9. Species tree inference (wQFM-GDL, src.Main)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ---------------------------------------------------------------------------
# Argument parsing
# ---------------------------------------------------------------------------
INPUT_FILE=""
OUTPUT_FILE=""

usage() {
    echo "Usage: bash wQFM-GDL.sh -i <input_gene_trees> -o <output_species_tree>"
    echo ""
    echo "Options:"
    echo "  -i, --input    Input multi-copy gene tree file (Newick, one tree per line)"
    echo "  -o, --output   Output species tree file"
    echo "  -h, --help     Show this help message"
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        -i|--input)
            INPUT_FILE="$2"
            shift 2
            ;;
        -o|--output)
            OUTPUT_FILE="$2"
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Error: Unknown argument: $1"
            usage
            exit 1
            ;;
    esac
done

# ---------------------------------------------------------------------------
# Validation
# ---------------------------------------------------------------------------
if [[ -z "$INPUT_FILE" ]]; then
    echo "Error: Input file not specified. Use -i or --input."
    usage
    exit 1
fi

if [[ -z "$OUTPUT_FILE" ]]; then
    echo "Error: Output file not specified. Use -o or --output."
    usage
    exit 1
fi

if [[ ! -f "$INPUT_FILE" ]]; then
    echo "Error: Input file '$INPUT_FILE' not found."
    exit 1
fi

# ---------------------------------------------------------------------------
# Derive intermediate file paths
# ---------------------------------------------------------------------------
INPUT_ABS="$(cd "$(dirname "$INPUT_FILE")" && pwd)/$(basename "$INPUT_FILE")"
INPUT_BASENAME="$(basename "$INPUT_FILE" | sed 's/\.[^.]*$//')"
OUTPUT_ABS="$(cd "$(dirname "$OUTPUT_FILE")" 2>/dev/null || mkdir -p "$(dirname "$OUTPUT_FILE")" && cd "$(dirname "$OUTPUT_FILE")" && pwd)/$(basename "$OUTPUT_FILE")"

WORK_DIR="$(dirname "$OUTPUT_FILE")"
INTERMEDIATE_DIR="$WORK_DIR/${INPUT_BASENAME}-wqfm-files"
mkdir -p "$WORK_DIR" "$INTERMEDIATE_DIR"

GT_CLEANED="$INTERMEDIATE_DIR/${INPUT_BASENAME}-cleaned.tre"
GT_RESOLVED_RAW="${GT_CLEANED}.resolved"
GT_RESOLVED="$INTERMEDIATE_DIR/${INPUT_BASENAME}-resolved.tre"

DISCO_DECOMP="$INTERMEDIATE_DIR/${INPUT_BASENAME}-disco-decomp.tre"
DISCO_DECOMP_CLEANED="$INTERMEDIATE_DIR/${INPUT_BASENAME}-disco-decomp-cleaned.tre"
DISCO_NO_DECOMP="$INTERMEDIATE_DIR/${INPUT_BASENAME}-disco-rooted.tre"
DISCO_NO_DECOMP_CLEANED="$INTERMEDIATE_DIR/${INPUT_BASENAME}-disco-rooted-cleaned.tre"
CONSENSUS_PREFIX="$INTERMEDIATE_DIR/${INPUT_BASENAME}-consensus"
CONSENSUS_TREE="${CONSENSUS_PREFIX}.greedy.tree"

# ---------------------------------------------------------------------------
# Java command setup
# ---------------------------------------------------------------------------
if ! command -v java &>/dev/null; then
    echo "Error: 'java' not found in PATH. Please install Java and try again."
    exit 1
fi

WQFM_JAR="$SCRIPT_DIR/wQFM-GDL-v1.0.0.jar"

if [[ ! -f "$WQFM_JAR" ]]; then
    echo "Error: JAR file not found at '$WQFM_JAR'."
    echo "Please build it first:"
    echo "  javac -d bin \$(find src -name '*.java')"
    echo "  echo 'Main-Class: src.Main' > manifest.txt"
    echo "  jar cfm wQFM-GDL-v1.0.0.jar manifest.txt -C bin ."
    exit 1
fi

# ---------------------------------------------------------------------------
# Pipeline
# ---------------------------------------------------------------------------
echo "=== wQFM-GDL Pipeline ==="
echo "Input gene trees    : $INPUT_FILE"
echo "Output species tree : $OUTPUT_FILE"
echo "Intermediate files  : $INTERMEDIATE_DIR"
echo ""

# Step 1 — Clean raw input gene trees
echo "[1/9] Cleaning input gene trees..."
python3 "$SCRIPT_DIR/scripts/treeCleaner.py" < "$INPUT_FILE" > "$GT_CLEANED"
if [[ $? -ne 0 ]]; then
    echo "Error: treeCleaner.py failed on input gene trees."
    exit 1
fi

# Step 2 — Resolve polytomies
echo "[2/9] Resolving polytomies..."
python3 "$SCRIPT_DIR/scripts/arb_resolve_polytomies.py" "$GT_CLEANED"
if [[ $? -ne 0 ]]; then
    echo "Error: arb_resolve_polytomies.py failed."
    exit 1
fi

# Step 3 — Clean the resolved output
echo "[3/9] Cleaning resolved trees..."
python3 "$SCRIPT_DIR/scripts/treeCleaner.py" < "$GT_RESOLVED_RAW" > "$GT_RESOLVED"
if [[ $? -ne 0 ]]; then
    echo "Error: treeCleaner.py failed on resolved trees."
    exit 1
fi

# Step 4 — DISCO decomposition
echo "[4/9] Running DISCO decomposition..."
python3 "$SCRIPT_DIR/scripts/disco.py" -i "$GT_RESOLVED" -o "$DISCO_DECOMP" -d _
if [[ $? -ne 0 ]]; then
    echo "Error: DISCO decomposition failed."
    exit 1
fi

# Step 5 — Clean DISCO decomposition output
echo "[5/9] Cleaning decomposed trees..."
python3 "$SCRIPT_DIR/scripts/treeCleaner.py" < "$DISCO_DECOMP" > "$DISCO_DECOMP_CLEANED"
if [[ $? -ne 0 ]]; then
    echo "Error: treeCleaner.py failed on decomposed trees."
    exit 1
fi

# Step 6 — Greedy consensus tree via PAUP
echo "[6/9] Generating greedy consensus tree (PAUP)..."
perl "$SCRIPT_DIR/scripts/run_paup_consensus.pl" \
    -i "$DISCO_DECOMP_CLEANED" \
    -o "$CONSENSUS_PREFIX" \
    > "$INTERMEDIATE_DIR/consLog.txt" 2>"$INTERMEDIATE_DIR/consErr.txt"
if [[ $? -ne 0 ]]; then
    echo "Error: PAUP consensus generation failed. See '$INTERMEDIATE_DIR/consErr.txt'."
    exit 1
fi

# Step 7 — DISCO rooting (no decomposition)
echo "[7/9] Running DISCO rooting (--no-decomp)..."
python3 "$SCRIPT_DIR/scripts/disco.py" -i "$GT_RESOLVED" -o "$DISCO_NO_DECOMP" -d _ --no-decomp
if [[ $? -ne 0 ]]; then
    echo "Error: DISCO rooting failed."
    exit 1
fi

# Step 8 — Clean DISCO rooted output
echo "[8/9] Cleaning rooted trees..."
python3 "$SCRIPT_DIR/scripts/treeCleaner.py" < "$DISCO_NO_DECOMP" > "$DISCO_NO_DECOMP_CLEANED"
if [[ $? -ne 0 ]]; then
    echo "Error: treeCleaner.py failed on rooted trees."
    exit 1
fi

# Step 9 — wQFM-GDL species tree inference
echo "[9/9] Running wQFM-GDL species tree inference..."
java -jar "$WQFM_JAR" \
    "$DISCO_NO_DECOMP_CLEANED" \
    "$CONSENSUS_TREE" \
    "$OUTPUT_FILE"
if [[ $? -ne 0 ]]; then
    echo "Error: wQFM-GDL inference failed."
    exit 1
fi

echo ""
echo "=== Pipeline complete ==="
echo "Species tree written to: $OUTPUT_FILE"
