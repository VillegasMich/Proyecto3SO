import java.io.InputStreamReader;
import java.lang.Process;
import java.lang.ProcessBuilder;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.IOException;
import java.time.*;

public class Initializer {
    private int pathsLength;
    private FilesManager manager;
    private String[] flag;
    private List<Process> processes = new ArrayList<>();

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
            System.err.println("La direccion dada no es valida");
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
     * process in the same core as the main program.
     *
     * @return 0 if Ok ; 1 if ends with error
     * @throws IOException
     *                     If there is a problem with the processes
     */
    private int pathS() throws IOException {
        long mainId = ProcessHandle.current().pid();
        for (int i = 0; i < this.pathsLength; i++) {
            String core = getCore(mainId);
            // Create and start new process
            ProcessBuilder pb = new ProcessBuilder("taskset", "-c", core, "java", "./Reader.java",
                    manager.getFolder() + manager.getPaths()[i]);
            Process p = pb.start();
            // processes.add(p);
            if (i == 0) {
                LocalTime firtstFile = java.time.LocalDateTime.now().toLocalTime();
                System.out.println("First file load time: " + firtstFile);
            }
            if (i == this.pathsLength - 1) {
                LocalTime lastFile = java.time.LocalDateTime.now().toLocalTime();
                System.out.println("Last file load time: " + lastFile);
                // System.out.println(p.isAlive());
            }
            printData(p);
            p.destroy();
        }
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
                    manager.getFolder() + manager.getPaths()[i]);
            Process p = pb.start();
            // this.processes.add(p);
            if (i == 0) {
                LocalTime firtstFile = java.time.LocalDateTime.now().toLocalTime();
                System.out.println("First file load time: " + firtstFile);
            }
            if (i == this.pathsLength - 1) {
                LocalTime lastFile = java.time.LocalDateTime.now().toLocalTime();
                System.out.println("Last file load time: " + lastFile);
                // System.out.println(p.isAlive());
            }
            printData(p);
            p.destroy();
        }
        return 0;
    }

    /**
     * It takes a process p and gets the input stream to print the info
     * in the main process output.
     *
     * @param p Procces to get the input stream
     * @throws IOException
     *                     if there is a problem with the given process
     */
    private void printData(Process p) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream()))) {
            String line;
            if ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
    }

    public void printData() throws IOException {
        if (this.processes.size() != 0) {
            for (int i = 0; i < this.pathsLength; i++) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(processes.get(i).getInputStream()))) {
                    String line;
                    if ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                    processes.get(i).destroy();
                }
            }
        }
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
            manager.readAllFiles();
        }
        // processes.getLast().isAlive();
        // this.printData();
        Instant endTime = Instant.now();
        long executionTime = Duration.between(startTime, endTime).toMillis();
        System.out.println("Execute total time: " + executionTime + " milliseconds");
        return 0;
    }

}
