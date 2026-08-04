#!/usr/bin/env python3
"""
Uniquify duplicate leaf names in gene trees by adding suffixes.

This script processes Newick format gene trees where multi-copy genes may have
the same name (e.g., ((speciesA,speciesB),(speciesB,speciesC))).

Logic:
1. Validate leaf labels containing underscores. A label with one underscore is
   treated as species_copy. Labels with more than one underscore are ambiguous
   and rejected for now.
2. Check for duplicate plain species labels in each tree.
3. Add _1, _2, _3, etc. suffixes to duplicate plain species labels.

Usage:
    python3 uniquify_leaves.py <input_file> <output_file>
"""

import sys
from collections import Counter

DELIMITERS = set('(),;: \t\n\r')

def previous_non_whitespace(text, index):
    """Return the previous non-whitespace character before index, or None."""
    index -= 1
    while index >= 0 and text[index] in ' \t\n\r':
        index -= 1
    return text[index] if index >= 0 else None

def extract_leaf_names(newick_str):
    """Extract all leaf names from a Newick string."""
    leaves = []
    i = 0

    while i < len(newick_str):
        char = newick_str[i]
        if char in DELIMITERS:
            i += 1
            continue

        j = i
        while j < len(newick_str) and newick_str[j] not in DELIMITERS:
            j += 1

        label = newick_str[i:j]
        prev_char = previous_non_whitespace(newick_str, i)
        if prev_char in ('(', ',') or prev_char is None:
            leaves.append(label)

        i = j

    return leaves

def underscored_leaf_names(trees):
    """Return leaf names containing underscores across all trees."""
    underscored = []
    for tree_index, tree in enumerate(trees, 1):
        leaves = extract_leaf_names(tree)
        for leaf in leaves:
            if '_' in leaf:
                underscored.append((tree_index, leaf))
    return underscored

def ambiguous_copy_labels(trees):
    """Return underscore labels that are not valid species_copy labels."""
    ambiguous = []
    for tree_index, tree in enumerate(trees, 1):
        leaves = extract_leaf_names(tree)
        for leaf in leaves:
            if '_' not in leaf:
                continue

            parts = leaf.split('_')
            if len(parts) != 2 or not parts[0] or not parts[1]:
                ambiguous.append((tree_index, leaf))

    return ambiguous

def duplicate_copy_labels(trees):
    """Return exact duplicate labels that already use species_copy form."""
    duplicates = []
    for tree_index, tree in enumerate(trees, 1):
        leaves = extract_leaf_names(tree)
        counts = Counter(leaves)
        for leaf, count in counts.items():
            if '_' in leaf and count > 1:
                duplicates.append((tree_index, leaf, count))

    return duplicates

def uniquify_tree(newick_str):
    """Add suffixes to duplicate leaf names in a single tree."""
    # First, extract leaves to identify duplicates.
    leaves = extract_leaf_names(newick_str)
    
    # Count occurrences of each leaf name
    leaf_counts = Counter(leaves)
    duplicates = {leaf: count for leaf, count in leaf_counts.items() if '_' not in leaf and count > 1}
    
    if not duplicates:
        # No duplicates, return as is
        return newick_str
    
    # Parse the tree and replace duplicate leaves
    result = []
    i = 0
    replacement_counter = {leaf: 0 for leaf in duplicates}
    used_labels = set(leaves)
    
    while i < len(newick_str):
        char = newick_str[i]
        
        # Check if we're at the start of a potential label (not a special character)
        if char not in DELIMITERS:
            # Extract the full label
            j = i
            while j < len(newick_str) and newick_str[j] not in DELIMITERS:
                j += 1
            label = newick_str[i:j]
            
            prev_char = previous_non_whitespace(newick_str, i)
            is_leaf_label = prev_char in ('(', ',') or prev_char is None

            if is_leaf_label and label in duplicates:
                # This is a duplicate leaf name - add suffix
                while True:
                    replacement_counter[label] += 1
                    new_label = f"{label}_{replacement_counter[label]}"
                    if new_label not in used_labels:
                        used_labels.add(new_label)
                        result.append(new_label)
                        break
            else:
                # Regular label, branch length, or internal node label - keep as is
                result.append(label)
            
            i = j
        else:
            # Regular character (delimiter), keep as is
            result.append(char)
            i += 1
    
    return ''.join(result)

def process_trees(input_file, output_file):
    """Process all trees in the input file."""
    with open(input_file, 'r') as f:
        trees = [line.strip() for line in f if line.strip()]
    
    # Labels with one underscore are accepted as species_copy. More underscores
    # are ambiguous because species names themselves cannot contain underscores.
    ambiguous = ambiguous_copy_labels(trees)
    if ambiguous:
        print("Error: ambiguous underscores in input leaf labels.", file=sys.stderr)
        print('wQFM-GDL treats "_" as the species/copy separator and supports labels as species_copy.', file=sys.stderr)
        print('Species labels and copy IDs must not themselves contain "_".', file=sys.stderr)
        print("Examples:", file=sys.stderr)
        for tree_index, leaf in ambiguous[:5]:
            print(f"  tree {tree_index}: {leaf}", file=sys.stderr)
        if len(ambiguous) > 5:
            print(f"  ... and {len(ambiguous) - 5} more labels", file=sys.stderr)
        sys.exit(1)

    duplicate_copies = duplicate_copy_labels(trees)
    if duplicate_copies:
        print("Error: duplicate copy-labeled leaf labels found.", file=sys.stderr)
        print("Input labels in species_copy form must be unique within each gene tree.", file=sys.stderr)
        print("Examples:", file=sys.stderr)
        for tree_index, leaf, count in duplicate_copies[:5]:
            print(f"  tree {tree_index}: {leaf} appears {count} times", file=sys.stderr)
        if len(duplicate_copies) > 5:
            print(f"  ... and {len(duplicate_copies) - 5} more labels", file=sys.stderr)
        sys.exit(1)
    
    print("Checking for duplicate plain species labels...")
    
    # Process each tree to uniquify duplicates
    modified_trees = []
    total_duplicates = 0
    
    for i, tree in enumerate(trees, 1):
        leaves = extract_leaf_names(tree)
        leaf_counts = Counter(leaves)
        duplicates_in_tree = sum(1 for count in leaf_counts.values() if count > 1)
        
        if duplicates_in_tree > 0:
            total_duplicates += duplicates_in_tree
            modified_tree = uniquify_tree(tree)
            modified_trees.append(modified_tree)
            if i <= 5:  # Show first few examples
                print(f"  Tree {i}: Found {duplicates_in_tree} species with duplicates")
        else:
            modified_trees.append(tree)
    
    print(f"Total trees with duplicates: {total_duplicates}")
    print(f"Writing uniquified trees to {output_file}")
    
    # Write output
    with open(output_file, 'w') as f:
        for tree in modified_trees:
            f.write(tree + '\n')

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python3 uniquify_leaves.py <input_file> <output_file>")
        sys.exit(1)
    
    input_file = sys.argv[1]
    output_file = sys.argv[2]
    
    process_trees(input_file, output_file)
    print("Done!")
