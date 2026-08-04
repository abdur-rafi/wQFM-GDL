#!/usr/bin/env python3
"""
Prepare branch-length-preserving gene trees for branch annotation.

wQFM-GDL uses _ as the species/copy separator for multi-copy labels. The branch
annotation step works with repeated species names directly, so this script
converts leaf labels from species_copy back to repeated species labels while
preserving the rest of each Newick line, including branch lengths and
branch-support annotations.

Usage:
    python3 prepare_branch_annotation_input.py <uniquified_gene_trees> <output_gene_trees>
"""

import sys


DELIMITERS = set("(),;: \t\n\r")


def previous_non_whitespace(text, index):
    """Return the previous non-whitespace character before index, or None."""
    index -= 1
    while index >= 0 and text[index] in " \t\n\r":
        index -= 1
    return text[index] if index >= 0 else None


def strip_copy_suffix(label):
    """Strip a wQFM-GDL copy suffix from a leaf label."""
    if "_" not in label:
        return label

    parts = label.split("_")
    if len(parts) == 2 and parts[0] and parts[1]:
        return parts[0]

    raise ValueError(f"ambiguous copy-labeled leaf label: {label}")


def restore_repeated_species(tree):
    """Strip copy suffixes from leaf labels without changing other Newick text."""
    result = []
    i = 0

    while i < len(tree):
        char = tree[i]
        if char in DELIMITERS:
            result.append(char)
            i += 1
            continue

        j = i
        while j < len(tree) and tree[j] not in DELIMITERS:
            j += 1

        label = tree[i:j]
        prev_char = previous_non_whitespace(tree, i)

        # Leaf labels appear after "(" or ",". Labels after ")" are internal
        # node labels or supports and should be preserved exactly.
        if prev_char in ("(", ",") or prev_char is None:
            result.append(strip_copy_suffix(label))
        else:
            result.append(label)

        i = j

    return "".join(result)


def process(input_file, output_file):
    with open(input_file, "r") as handle:
        trees = [line.rstrip("\n") for line in handle if line.strip()]

    with open(output_file, "w") as handle:
        for tree_index, tree in enumerate(trees, 1):
            try:
                handle.write(restore_repeated_species(tree) + "\n")
            except ValueError as exc:
                print(f"Error in tree {tree_index}: {exc}", file=sys.stderr)
                sys.exit(1)


def main():
    if len(sys.argv) != 3:
        print("Usage: python3 prepare_branch_annotation_input.py <uniquified_gene_trees> <output_gene_trees>", file=sys.stderr)
        sys.exit(1)

    process(sys.argv[1], sys.argv[2])


if __name__ == "__main__":
    main()
