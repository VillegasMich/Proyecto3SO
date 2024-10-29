import java.io.InputStreamReader;
import java.lang.Process;
import java.lang.ProcessBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Dictionary;
import java.util.Hashtable;
import java.io.BufferedReader;
import java.io.IOException;
import java.time.*;

public class Initializer {
    private int pathsLength;
    private FilesManager manager;
    private String[] flag;
    private List<Thread> threads = new ArrayList<>();
    private List<Process> processes = new ArrayList<>();
    private Dictionary<String, MetaData> metaDictMin = new Hashtable<>();
    private Dictionary<String, MetaData> metaDictMax = new Hashtable<>();

    /**
     * Creates a FileManger instance and saves all
     * the .csv files in the path property, saving it's return
     * value at this class property pathsLength.
     * 
     * @param flag works to store the command flag received form App.class.
     */
    public Initializer(String[] flag) {
        try {
            if (flag.length == 3) {
                this.manager = new FilesManager(flag[2]);
                this.pathsLength = manager.addCsvFiles();
                this.flag = flag;
            } else {
                this.manager = new FilesManager(flag[1]);
                this.pathsLength = manager.addCsvFiles();
                this.flag = flag;
            }

        } catch (Exception e) {
            System.err.println(ConsoleColors.RED + "La direccion dada no es valida" + ConsoleColors.RESET);
        }
    }

    /**
     * Get the core where the process woth the given id is running.
     * Only working on linux!
     *
     * @param pid pid of the process to get the running core
     * @return The core where the process is running
     * @throws IOException
     *                     If there is a problem with the process to get the result
     */
    @SuppressWarnings("unused")
    private String getCore(long pid) throws IOException {
        // Get parent PID and current core
        String[] commands = { "ps", "-o", "psr", "-p", "" + pid };
        Process proc = Runtime.getRuntime().exec(commands);
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

        // Read the output from the command
        String s;
        String core = "0";
        while ((s = stdInput.readLine()) != null) {
            if (!(s.equals("PSR".strip()))) {
                core = s.strip();
            }
        }
        return core;
    }

    /**
     * Read all the files, where each file must be on an independent
     * thread in the same core as the main program.
     *
     * @return 0 if Ok ; 1 if ends with error
     * @throws IOException
     *                     If there is a problem with the processes
     */
    private int pathS() throws IOException {
        for (int i = 0; i < this.pathsLength; i++) {
            final int Tindex = i;
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    MetaData[] metadata = manager.readIndexFile(Tindex);
                    metaDictMin.put(manager.getPaths()[Tindex], metadata[1]);
                    metaDictMax.put(manager.getPaths()[Tindex], metadata[0]);
                }
            });
            t.start();
            threads.add(t);
            if (i == 0) {
                LocalTime firtstFile = java.time.LocalDateTime.now().toLocalTime();
                System.out
                        .println(ConsoleColors.BLUE_BOLD + "First file load time: " + firtstFile + ConsoleColors.RESET);
            }
            if (i == this.pathsLength - 1) {
                LocalTime lastFile = java.time.LocalDateTime.now().toLocalTime();
                System.out.println(
                        ConsoleColors.BLUE_BOLD + "Last file load time: " + lastFile + "\n" + ConsoleColors.RESET);
            }
        }
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        String max = DictFinder.findMaxPopularity(metaDictMax);
        String min = DictFinder.findMinPopularity(metaDictMin);
        System.out.println(ConsoleColors.WHITE_BOLD
                + "----------------------------------------------------------------------" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.GREEN_BOLD + "The most popular video is: "
                + metaDictMax.get(DictFinder.findMaxPopularity(metaDictMax)) + " of the file " + max
                + ConsoleColors.RESET);
        System.out.println(ConsoleColors.CYAN_BOLD + "The least popular video is: "
                + metaDictMin.get(DictFinder.findMinPopularity(metaDictMin)) + " of the file " + min
                + ConsoleColors.RESET);
        System.out.println(ConsoleColors.WHITE_BOLD
                + "----------------------------------------------------------------------" + ConsoleColors.RESET);
        return 0;
    }

    /**
     * Read all the files, where each file must be on an independent
     * process but assigned to any available core
     *
     * @return 0 if Ok ; 1 if ends with error
     * @throws IOException
     *                     If there is a problem with the processes
     */
    private int pathM() throws IOException {
        for (int i = 0; i < this.pathsLength; i++) {
            ProcessBuilder pb = new ProcessBuilder("java", "./Reader.java",
                    manager.getFolder() + manager.getPaths()[i]).redirectErrorStream(true);
            Process p = pb.start();
            processes.add(p);
            if (i == 0) {
                LocalTime firtstFile = java.time.LocalDateTime.now().toLocalTime();
                System.out
                        .println(ConsoleColors.BLUE_BOLD + "First file load time: " + firtstFile + ConsoleColors.RESET);
            }
            if (i == this.pathsLength - 1) {
                LocalTime lastFile = java.time.LocalDateTime.now().toLocalTime();
                System.out.println(
                        ConsoleColors.BLUE_BOLD + "Last file load time: " + lastFile + "\n" + ConsoleColors.RESET);
            }
        }

        for (Process p : processes) {
            try {
                p.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        for (Process p : processes) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                StringBuilder output = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
                String[] outputArr = output.toString().split(";");
                System.out.println(ConsoleColors.WHITE_BOLD
                        + "----------------------------------------------------------------------"
                        + ConsoleColors.RESET);
                System.out.println(ConsoleColors.WHITE_BOLD + outputArr[outputArr.length - 1] + ConsoleColors.RESET);
                String fileName = outputArr[0];
                String[] videoMax = outputArr[1].split(",");
                String idMax = videoMax[0];
                Integer popularityMax = Integer.parseInt(videoMax[1]);
                String[] videoMin = outputArr[2].split(",");
                String idMin = videoMin[0];
                Integer popularityMin = Integer.parseInt(videoMin[1]);
                this.metaDictMin.put(fileName, new MetaData(idMin, popularityMin));
                this.metaDictMax.put(fileName, new MetaData(idMax, popularityMax));
                System.out.println(ConsoleColors.GREEN + "The most popular video of " + fileName + " is: " + idMax
                        + " with " + popularityMax + " popularity" + ConsoleColors.RESET);
                System.out.println(ConsoleColors.CYAN + "The least popular video of " + fileName + " is: " + idMin
                        + " with " + popularityMin + " of popularity" + ConsoleColors.RESET);
                System.out.println();
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        String max = DictFinder.findMaxPopularity(metaDictMax);
        String min = DictFinder.findMinPopularity(metaDictMin);
        System.out.println(ConsoleColors.WHITE_BOLD
                + "----------------------------------------------------------------------" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.GREEN_BOLD + "The most popular video is: "
                + metaDictMax.get(DictFinder.findMaxPopularity(metaDictMax)) + " of the file " + max
                + ConsoleColors.RESET);
        System.out.println(ConsoleColors.CYAN_BOLD + "The least popular video is: "
                + metaDictMin.get(DictFinder.findMinPopularity(metaDictMin)) + " of the file " + min
                + ConsoleColors.RESET);
        System.out.println(ConsoleColors.WHITE_BOLD
                + "----------------------------------------------------------------------" + ConsoleColors.RESET);
        return 0;

    }

    private int pathDefault() {
        for (int i = 0; i < this.pathsLength; i++) {
            MetaData[] metadata = manager.readIndexFile(i);
            metaDictMin.put(manager.getPaths()[i], metadata[1]);
            metaDictMax.put(manager.getPaths()[i], metadata[0]);

        }
        String max = DictFinder.findMaxPopularity(metaDictMax);
        String min = DictFinder.findMinPopularity(metaDictMin);
        System.out.println(ConsoleColors.WHITE_BOLD
                + "----------------------------------------------------------------------" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.GREEN_BOLD + "The most popular video is: "
                + metaDictMax.get(DictFinder.findMaxPopularity(metaDictMax)) + " of the file " + max
                + ConsoleColors.RESET);
        System.out.println(ConsoleColors.CYAN_BOLD + "The least popular video is: "
                + metaDictMin.get(DictFinder.findMinPopularity(metaDictMin)) + " of the file " + min
                + ConsoleColors.RESET);
        System.out.println(ConsoleColors.WHITE_BOLD
                + "----------------------------------------------------------------------" + ConsoleColors.RESET);
        return 0;
    }

    /**
     * At calling the function is going to check for the flag
     * value and decide which of the listed actions in the project
     * is going to execute.
     * 
     * @throws Exception
     *                   If there is a problem with the child processes
     */
    public int Initialize() throws IOException {
        Instant startTime = Instant.now();
        if (this.flag.length != 0 && this.flag.length == 3 && this.flag[0].equals("-m")) { // path -m
            pathM();
        } else if (this.flag.length != 0 && this.flag.length == 3 && this.flag[0].equals("-s")) { // path -s
            pathS();
        } else { // path no flag
            pathDefault();
        }
        Instant endTime = Instant.now();
        long executionTime = Duration.between(startTime, endTime).toMillis();
        System.out.println("\nExecute total time: " + executionTime + " milliseconds");
        return 0;
    }

}
