# wQFM-GDL

This repository contains the official implementation of

[**wQFM-GDL Enables Accurate Quartet-based Genome-scale Species Tree Inference Under Gene Duplication and Loss**](https://www.biorxiv.org/content/10.1101/2025.04.04.647228v3)

wQFM-GDL is a scalable and accurate quartet-based method for species tree estimation from multi-copy gene family trees, explicitly modeling both gene duplication and loss (GDL) and incomplete lineage sorting (ILS). wQFM-GDL integrates the concept of species-driven quartets (SQs) within a re-engineered QFM framework, enabling direct computation of refined quartet scores from gene family trees containing both orthologs and paralogs.

Through extensive evaluation on simulated and real biological datasets, wQFM-GDL outperforms leading methods  in the majority of tested conditions, with particularly strong improvements on large datasets. For datasets with 200–500 taxa, it achieves , on average, nearly a 25% reduction in reconstruction error compared with ASTRAL-Pro3. The method is quite scalable, successfully analyzing 500-taxon datasets with high duplication rates (over 2,000 leaves per gene tree) within 20 hours using only 16GB of memory.

wQFM-GDL is designed for large-scale phylogenomic analyses where both paralogy and orthology must be considered.

wQFM-GDL includes two variants, wQFM-GDL-Q and wQFM-GDL-T, which operate on quartets and gene trees, respectively. In particular, we make the following key contributions. wQFM-GDL-T works directly from gene family trees without any quartet enumeration and designed for large-scale phylogenomic analyses. wQFM-GDL-Q enumerates the SQs from a given set of gene family trees and apply wQFM on this set of SQs. On small to moderate size dataset, where quartet enumeration is feasible, wQFM-GDL-Q is sometimes more accurate than wQFM-GDL-T. 

## Execution dependencies

### Packages, Programming Languages and Operating Systems Requirements

- Java 11+ (tested with OpenJDK 17) 
- Linux O.S.
- Python 3

## Input and output formats

### Input
-The input gene trees should be in the Newick format
- The gene trees can contain multi-copy genes, missing taxa or polytomies.
- For multicopy gene trees, they can have the same name in the input gene trees (e.g., ((speciesA,speciesB),(speciesB,speciesC));). Otherwise, the names can be in the format speciesid_geneid (e.g., ((speciesA_gene1,speciesB_gene1),(speciesB_gene2,speciesC_gene1));).

### Output

Output file contains the estimated species tree in the Newick format.

## Quick Start


## Building from source

### Prerequisites

- Java Development Kit (JDK) 11 or later
- All source files are under the `src/` directory

### Step 1 — Compile all sources

From the repository root, compile all Java source files into the `bin/` directory:

```bash
mkdir -p bin
javac -d bin $(find src -name "*.java")
```

### Step 2 — Build `wQFM-GDL-v1.0.0.jar` (wQFM-GDL-T)

This JAR is used by the tree-based pipeline (`-t` flag). Its main class is `src.Main`.

```bash
echo "Main-Class: src.Main" > manifest-main.txt
jar cfm wQFM-GDL-v1.0.0.jar manifest-main.txt -C bin .
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
java -jar wQFM-GDL-v1.0.0.jar          # should print: Input file: ...
java -jar QuartetGenMain.jar            # should print usage
```

## Simulated Dataset

To extensively evaluate the performance under GDL on large datasets, we generated two new large-scale simulated datasets comprising 200 and 500 taxa, which we call SIM200 and SIM500, respectively. The datasets are publicly available on [**Zenodo**](https://zenodo.org/records/18605522). The simulation scripts are provided in the [Simulation Folder](https://github.com/abdur-rafi/wQFM-GDL/issues). The detailed parameters and simulation pipeline are presented in the paper.

## Bug Report

We are always looking to improve our codebase.

For any issues, please post on [wQFM-GDL issues page](https://github.com/abdur-rafi/wQFM-GDL/issues).

Alternatively, you can email at ```amsrumi@gmail.com``` or ```rafi08236@gmail.com```.
