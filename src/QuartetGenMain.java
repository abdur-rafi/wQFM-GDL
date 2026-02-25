package src;

import src.PreProcessing.GeneTrees;
import src.Quartets.QuartestsList;

public class QuartetGenMain {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java -cp <classpath> src.QuartetGenMain <gene-trees-file>");
            System.exit(1);
        }

        String path = args[0];

        GeneTrees geneTrees = new GeneTrees(path);
        geneTrees.readTaxaNames();
        
        // System.out.println("Found taxa: " + geneTrees.taxaMap);

        QuartestsList qlist = geneTrees.readGeneTreesAndGenerateQuartets();

        qlist.printQuartets(geneTrees.taxa);

    }
}
