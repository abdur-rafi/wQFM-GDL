package src;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import src.InitialPartition.ConsensusTreePartitionDC;
import src.InitialPartition.IMakePartition;
import src.PreProcessing.Preprocess;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

// 5:18
public class Main {

    /**
     * Prints usage/help information and exits.
     */
    private static void printUsageAndExit() {
        System.out.println("Usage: wQFM-GDL [OPTIONS]");
        System.out.println();
        System.out.println("Options (flag-based):");
        System.out.println("  -i,  --input            <path>    Path to the input gene trees file  (required)");
        System.out.println("  -c,  --consensus        <path>    Path to a pre-built consensus tree  (optional)");
        System.out.println("                                     If omitted, the consensus is built automatically");
        System.out.println("                                     from the input file using run_paup_consensus.pl.");
        System.out.println("  -o,  --output           <path>    Path to the output species tree file (required)");
        System.out.println("  -s,  --scripts-dir      <path>    Directory containing helper scripts");
        System.out.println("                                     (default: ./scripts)");
        System.out.println("  -t,  --consensus-type   <type>    Consensus type used for auto-generation:");
        System.out.println("                                     greedy (default), majority, or strict");
        System.out.println("  -e,  --external-tagging           Enable external tagging (default: false)");
        System.out.println("  -h,  --help                       Show this help message");
        System.out.println();
        System.out.println("Legacy positional usage (still supported):");
        System.out.println("  wQFM-GDL <inputFile> <consensusFile> <outputFile> [true|false]");
        System.exit(0);
    }

    /**
     * Runs run_paup_consensus.pl to build a consensus tree from the given gene trees
     * file, and returns the path to the resulting consensus tree file.
     *
     * @param inputFilePath  path to the gene trees input file
     * @param scriptsDir     directory that contains run_paup_consensus.pl
     * @param consensusType  "greedy", "majority", or "strict"
     * @return path to the generated consensus tree file
     */
    private static String generateConsensusTree(String inputFilePath,
                                                String scriptsDir,
                                                String consensusType) throws IOException {

        // Derive a temp-file prefix next to the input file so paths stay tidy.
        File inputFile = new File(inputFilePath).getAbsoluteFile();
        String prefix = inputFile.getParent() + File.separator
                + "wqfm_auto_consensus_" + inputFile.getName();

        String scriptPath = scriptsDir + File.separator + "run_paup_consensus.pl";

        System.out.println("[wQFM] No consensus file provided — generating automatically...");
        System.out.println("[wQFM]   Script  : " + scriptPath);
        System.out.println("[wQFM]   Prefix  : " + prefix);
        System.out.println("[wQFM]   Type    : " + consensusType);

        ProcessBuilder pb = new ProcessBuilder(
                "perl", scriptPath,
                "-i=" + inputFilePath,
                "-o=" + prefix);
        pb.redirectErrorStream(true);   // merge stderr → stdout so we see all output
        pb.inheritIO();                 // pipe directly to the parent's console

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            System.err.println("[wQFM] Error: could not launch Perl. Make sure perl is on your PATH.");
            System.err.println("       " + e.getMessage());
            System.exit(-1);
            return null; // unreachable
        }

        int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[wQFM] Error: interrupted while waiting for PAUP.");
            System.exit(-1);
            return null;
        }

        if (exitCode != 0) {
            System.err.println("[wQFM] Error: run_paup_consensus.pl exited with code " + exitCode + ".");
            System.exit(-1);
        }

        // The script writes <prefix>.greedy.tree / .majority.tree / .strict.tree
        String consensusFile = prefix + "." + consensusType + ".tree";

        if (!new File(consensusFile).exists()) {
            System.err.println("[wQFM] Error: expected consensus file not found: " + consensusFile);
            System.exit(-1);
        }

        System.out.println("[wQFM] Consensus tree written to: " + consensusFile);
        return consensusFile;
    }

    public static void main(String[] args) throws IOException {

        String inputFilePath    = "";
        String consensusFilePath = "";
        String outputFilePath   = "";
        String scriptsDir       = "./scripts";
        String consensusType    = "greedy";

        Config.USE_EXTERNAL_TAGGING = false;

        if (args.length == 0) {
            printUsageAndExit();
        }

        // Detect whether flag-based or legacy positional arguments are used.
        boolean usingFlags = args[0].startsWith("-");

        if (usingFlags) {
            // ── Flag-based argument parsing ──────────────────────────────────────
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-h":
                    case "--help":
                        printUsageAndExit();
                        break;

                    case "-i":
                    case "--input":
                        if (i + 1 >= args.length) {
                            System.err.println("Error: " + args[i] + " requires a value.");
                            System.exit(-1);
                        }
                        inputFilePath = args[++i];
                        break;

                    case "-c":
                    case "--consensus":
                        if (i + 1 >= args.length) {
                            System.err.println("Error: " + args[i] + " requires a value.");
                            System.exit(-1);
                        }
                        consensusFilePath = args[++i];
                        break;

                    case "-o":
                    case "--output":
                        if (i + 1 >= args.length) {
                            System.err.println("Error: " + args[i] + " requires a value.");
                            System.exit(-1);
                        }
                        outputFilePath = args[++i];
                        break;

                    case "-s":
                    case "--scripts-dir":
                        if (i + 1 >= args.length) {
                            System.err.println("Error: " + args[i] + " requires a value.");
                            System.exit(-1);
                        }
                        scriptsDir = args[++i];
                        break;

                    case "-t":
                    case "--consensus-type":
                        if (i + 1 >= args.length) {
                            System.err.println("Error: " + args[i] + " requires a value.");
                            System.exit(-1);
                        }
                        consensusType = args[++i];
                        if (!consensusType.equals("greedy") &&
                            !consensusType.equals("majority") &&
                            !consensusType.equals("strict")) {
                            System.err.println("Error: --consensus-type must be greedy, majority, or strict.");
                            System.exit(-1);
                        }
                        break;

                    case "-e":
                    case "--external-tagging":
                        Config.USE_EXTERNAL_TAGGING = true;
                        break;

                    default:
                        System.err.println("Error: Unknown flag: " + args[i]);
                        System.err.println("Run with --help for usage information.");
                        System.exit(-1);
                }
            }

            // Validate required flags
            if (inputFilePath.isEmpty()) {
                System.err.println("Error: --input / -i is required.");
                System.exit(-1);
            }
            if (outputFilePath.isEmpty()) {
                System.err.println("Error: --output / -o is required.");
                System.exit(-1);
            }

            // Auto-generate the consensus tree if -c/--consensus was not provided
            if (consensusFilePath.isEmpty()) {
                consensusFilePath = generateConsensusTree(inputFilePath, scriptsDir, consensusType);
            }

        } else {
            // ── Legacy positional argument parsing ───────────────────────────────
            if (args.length < 3) {
                System.err.println("Error: Specify all file paths (inputFile, consensusFile, outputFile).");
                System.err.println("Run with --help for usage information.");
                System.exit(-1);
            }

            inputFilePath     = args[0];
            consensusFilePath = args[1];
            outputFilePath    = args[2];

            if (args.length >= 4) {
                String useExternalTagging = args[3];
                if (useExternalTagging.equals("true")) {
                    Config.USE_EXTERNAL_TAGGING = true;
                } else if (useExternalTagging.equals("false")) {
                    Config.USE_EXTERNAL_TAGGING = false;
                } else {
                    System.err.println("Error: 4th argument must be 'true' or 'false'.");
                    System.exit(-1);
                }
            }
        }
       
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long time_1 = System.currentTimeMillis(); //calculate starting time
        long cpuTimeBefore = threadMXBean.getCurrentThreadCpuTime();
        

        Preprocess.PreprocessReturnType ret = Preprocess.preprocess(inputFilePath);
        ConsensusTreePartitionDC consensusTreePartitionDC = new ConsensusTreePartitionDC(consensusFilePath, ret.taxaMap, ret.dc);
        IMakePartition  partitionMakerDC = consensusTreePartitionDC;

        QFMDC qfm = new QFMDC(ret.dc, ret.realTaxa , partitionMakerDC);

        var spTree = qfm.runWQFM();

        FileWriter writer = new FileWriter(outputFilePath);

        writer.write(spTree.getNewickFormat());

        writer.close();

        long cpuTimeAfter = threadMXBean.getCurrentThreadCpuTime();

        long time_del = System.currentTimeMillis() - time_1;
        long minutes = (time_del / 1000) / 60;
        long seconds = (time_del / 1000) % 60;
        System.out.format("\nElapsed Time taken = %d ms ==> %d minutes and %d seconds.\n", time_del, minutes, seconds);

        long cpuTimeUsed = cpuTimeAfter - cpuTimeBefore;

        seconds = cpuTimeUsed / 1_000_000_000;
        
        minutes = seconds / 60;
        seconds = seconds % 60;

        System.out.println("CPU time used: " + minutes + " minutes, " + seconds + " seconds");
    
    }

    
}
