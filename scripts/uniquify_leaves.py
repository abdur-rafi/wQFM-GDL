#!/usr/bin/env python3
"""
Uniquify duplicate leaf names in gene trees by adding suffixes.

This script processes Newick format gene trees where multi-copy genes may have
the same name (e.g., ((speciesA,speciesB),(speciesB,speciesC))).

Logic:
1. Check if any leaf names contain underscores across all trees
2. If no underscores found, check for duplicates in each tree
3. Add _1, _2, _3, etc. suffixes to duplicate leaves to make them unique

Usage:
    python3 uniquify_leaves.py <input_file> <output_file>
"""

import sys
import re
from collections import Counter

def extract_leaf_names(newick_str):
    """Extract all leaf names from a Newick string."""
    # Remove everything after colons (branch lengths)
    newick_str = re.sub(r':[0-9.eE-]+', '', newick_str)
    # Remove branch support values (numbers immediately after closing parentheses)
    newick_str = re.sub(r'\)([0-9.eE-]+)', ')', newick_str)
    # Remove whitespace
    newick_str = newick_str.strip()
    # Extract leaf names: anything that's not a special character
    # Leaf names are between commas, parentheses, or semicolons
    leaves = re.findall(r'([^(),;:\s]+)', newick_str)
    return [leaf for leaf in leaves if leaf]

def has_underscores(trees):
    """Check if any leaf name contains an underscore across all trees."""
    for tree in trees:
        leaves = extract_leaf_names(tree)
        for leaf in leaves:
            if '_' in leaf:
                return True
    return False

def uniquify_tree(newick_str):
    """Add suffixes to duplicate leaf names in a single tree."""
    # First, extract leaves to identify duplicates (with branch supports removed)
    cleaned_str = re.sub(r':[0-9.eE-]+', '', newick_str)
    cleaned_str = re.sub(r'\)([0-9.eE-]+)', ')', cleaned_str)
    leaves = re.findall(r'([^(),;:\s]+)', cleaned_str)
    leaves = [leaf for leaf in leaves if leaf]
    
    # Count occurrences of each leaf name
    leaf_counts = Counter(leaves)
    duplicates = {leaf: count for leaf, count in leaf_counts.items() if count > 1}
    
    if not duplicates:
        # No duplicates, return as is
        return newick_str
    
    # Parse the tree and replace duplicate leaves
    result = []
    i = 0
    replacement_counter = {leaf: 0 for leaf in duplicates}
    
    while i < len(newick_str):
        char = newick_str[i]
        
        # Check if we're at the start of a potential label (not a special character)
        if char not in '(),;: \t\n\r':
            # Extract the full label
            j = i
            while j < len(newick_str) and newick_str[j] not in '(),:; \t\n\r':
                j += 1
            label = newick_str[i:j]
            
            # Determine if this is a branch support or a leaf name
            # Look back to the previous non-whitespace character
            prev_char_idx = i - 1
            while prev_char_idx >= 0 and newick_str[prev_char_idx] in ' \t\n\r':
                prev_char_idx -= 1
            
            prev_char = newick_str[prev_char_idx] if prev_char_idx >= 0 else None
            
            # If previous char is ')', this label is likely a branch support (if it's numeric)
            # If previous char is '(' or ',', this is definitely a leaf name
            is_likely_support = (prev_char == ')' and 
                               re.match(r'^[0-9.eE+-]+$', label) is not None)
            
            if is_likely_support:
                # This is a branch support value, keep as is
                result.append(label)
            elif label in duplicates:
                # This is a duplicate leaf name - add suffix
                replacement_counter[label] += 1
                result.append(f"{label}_{replacement_counter[label]}")
            else:
                # Regular leaf name (non-duplicate) - keep as is
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
    
    # Check if any tree has underscores in leaf names
    if has_underscores(trees):
        print("Leaf names contain underscores. Assuming format is speciesid_geneid.")
        print("No modification needed. Copying input to output.")
        # Copy as-is
        with open(output_file, 'w') as f:
            for tree in trees:
                f.write(tree + '\n')
        return
    
    print("No underscores found in leaf names. Checking for duplicates...")
    
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
