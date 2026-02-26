# wQFM-GDL

This repository contains the official implementation of

[**wQFM-GDL Enables Accurate Quartet-based Genome-scale Species Tree Inference Under Gene Duplication and Loss**](https://www.biorxiv.org/content/10.1101/2025.04.04.647228v3)

wQFM-GDL is a scalable and accurate quartet-based method for species tree estimation from multi-copy gene family trees, explicitly modeling both gene duplication and loss (GDL) and incomplete lineage sorting (ILS). wQFM-GDL integrates the concept of species-driven quartets (SQs) within a re-engineered QFM framework, enabling direct computation of refined quartet scores from gene family trees containing both orthologs and paralogs.

Through extensive evaluation on simulated and real biological datasets, wQFM-GDL outperforms leading methods  in the majority of tested conditions, with particularly strong improvements on large datasets. For datasets with 200–500 taxa, it achieves , on average, nearly a 25% reduction in reconstruction error compared with ASTRAL-Pro3. The method is quite scalable, successfully analyzing 500-taxon datasets with high duplication rates (over 2,000 leaves per gene tree) within 20 hours using only 16GB of memory.

wQFM-GDL is designed for large-scale phylogenomic analyses where both paralogy and orthology must be considered.

wQFM-GDL includes two variants, wQFM-GDL-Q and wQFM-GDL-T, which operate on quartets and gene trees, respectively. wQFM-GDL-T works directly on gene family trees without any quartet enumeration and designed for large-scale phylogenomic analyses. On small to moderate-sized datasets, where quartet enumeration is computationally feasible, wQFM-GDL-Q can sometimes achieve higher accuracy than wQFM-GDL-T and is therefore a suitable option. The generated quartet file is also provided with the output for any downstream analyses. However, wQFM-GDL-T is both highly accurate and scalable and should be the preferred choice for large-scale analyses.


## Execution dependencies

### Packages, Programming Languages and Operating Systems Requirements

- Java 11+ (developed using OpenJDK 17) 
- Platform: Ubuntu Linux (developed and tested on Ubuntu but should work on other distributions)
- Python 3, Perl

## Input and output formats

### Input
- The input gene trees should be in the Newick format
- The gene trees can contain multi-copy genes, missing taxa or polytomies.
- The gene trees may or may not contain branch support/length.
- For multicopy gene trees, the multicopy genes can have the same name in the input gene trees (e.g., ((speciesA,speciesB),(speciesB,speciesC));) or the names can be in the format speciesid_geneid (e.g., ((speciesA_gene1,speciesB_gene1),(speciesB_gene2,speciesC_gene1));).

### Output

Output file contains the estimated species tree in the Newick format.

## Quick Start

### Basic Usage

Download and extract the '.tar.gz' or '.zip' archive from the [**Github Releases**](https://github.com/abdur-rafi/wQFM-GDL/releases).

wQFM-GDL provides a unified script (`wQFM-GDL.sh`) that runs the complete pipeline. The script supports two modes:

**wQFM-GDL-T (Tree-based)**: Recommended for large-scale datasets (100+ taxa/ 1000+ genes).
```bash
./wQFM-GDL.sh -i <input_gene_trees.tre> -o <output_species_tree.tre> -t
```

**wQFM-GDL-Q (Quartet-based)**: Sometimes more accurate than wQFM-GDL-T but only applicable for small datasets where quartet enumeration is feasible (<100 taxa, <1000 genes)
```bash
./wQFM-GDL.sh -i <input_gene_trees.tre> -o <output_species_tree.tre> -q
```

### Options

- `-i`, `--input`: Input multi-copy gene tree file (Newick format, one tree per line)
- `-o`, `--output`: Output species tree file
- `-t`, `--tree`: Use tree-based pipeline (wQFM-GDL-T)
- `-q`, `--quartet`: Use quartet-based pipeline (wQFM-GDL-Q)
- `-m`, `--memory`: Java heap size (e.g., `8g`, `16g`, `60g`). Default: JVM default
- `-h`, `--help`: Show help message

### Examples

**Running wQFM-GDL-T with 16GB memory:**
```bash
./wQFM-GDL.sh -i gene_trees.tre -o species_tree.tre -t -m 16g
```

**Running wQFM-GDL-Q  with 8GB memory:**
```bash
./wQFM-GDL.sh -i gene_trees.tre -o species_tree.tre -q -m 8g
```
Two example input files are provided to test wQFM-GDL
**Using test data:**
```bash
./wQFM-GDL.sh -i testData/fungi16_genetrees.newick -o my_output.tre -t
```

### Intermediate files

All intermediate files are stored in `<basename>-wqfm-files/` directory next to the output file. This includes cleaned trees, resolved trees, DISCO outputs, consensus trees, and quartets (for `-q` mode).


## Building from source

 All source files are under the `src/` directory

### Step 1 — Compile all sources

From the repository root, compile all Java source files into the `bin/` directory:

```bash
mkdir -p bin
javac -d bin $(find src -name "*.java")
```

### Step 2 — Build `wQFM-GDL-v1.0.2.jar` (wQFM-GDL-T)

This JAR is used by the tree-based pipeline (`-t` flag). Its main class is `src.Main`.

```bash
echo "Main-Class: src.Main" > manifest-main.txt
jar cfm wQFM-GDL-v1.0.2.jar manifest-main.txt -C bin .
rm manifest-main.txt
```

### Step 3 — Build `QuartetGenMain.jar` (wQFM-GDL-Q)

This JAR generates species-driven quartets from gene family trees and is used by the quartet-based pipeline (`-q` flag). Its main class is `src.QuartetGenMain`.

```bash
echo "Main-Class: src.QuartetGenMain" > manifest-quartet.txt
jar cfm QuartetGenMain.jar manifest-quartet.txt -C bin .
rm manifest-quartet.txt
```

### Verify

```bash
java -jar wQFM-GDL-v1.0.2.jar          # should print: Input file: ...
java -jar QuartetGenMain.jar            # should print usage
```

## Simulated Dataset

To extensively evaluate the performance under GDL on large datasets, we generated two new large-scale simulated datasets comprising 200 and 500 taxa, which we call SIM200 and SIM500, respectively. The datasets are publicly available on [**Zenodo**](https://zenodo.org/records/18605522). The simulation scripts are provided in the [Simulation Folder](https://github.com/abdur-rafi/wQFM-GDL/issues). The detailed parameters and simulation pipeline are presented in the paper.

## Experiments and Results

All scripts used for the experiments, plots, simulation and most of the results (output species trees and RF scores of the experiments) are shared in the repository [**wQFM-GDL Experiments**](https://github.com/abdur-rafi/wqfm-gdl-experiments).

## Bug Report

We are always looking to improve our codebase.

For any issues, please post on [wQFM-GDL issues page](https://github.com/abdur-rafi/wQFM-GDL/issues).

Alternatively, you can email at ```amsrumi@gmail.com``` or ```abdurrafi403@gmail.com```.
