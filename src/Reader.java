import java.io.RandomAccessFile;
import java.time.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Enumeration;

public class Reader {
    static final Pattern popularity = Pattern.compile("(\\d+,\\d+,\\d+,\\d+)");
    static final Pattern identification = Pattern.compile("(\\n)([A-Za-z0-9]+)(,[0-9.]+).*?(\\d+,\\d+,\\d+,\\d+)");

    static int index = 0;
    static final int PAGE_LIMIT = 4096; // Each char equals 2 bytes; 4k = 4000 bytes
    static List<String> list = new ArrayList<>();
    static final int MAX_T = 2;
    static Dictionary<String, MetaData> metaDict = new Hashtable<>();

    public static void main(String[] args) {
        Instant start = Instant.now();
        long pointer = 0;
        try {
            RandomAccessFile myRaf = new RandomAccessFile(args[0], "r");
            myRaf.seek(0);
            long length = myRaf.length();
            byte[] line = new byte[PAGE_LIMIT];
            while (pointer < length) {
                myRaf.read(line, 0, PAGE_LIMIT);
                list.add(new String(line, StandardCharsets.UTF_8));
                pointer += PAGE_LIMIT;
            }
            // System.out.println(list.get(0));
            myRaf.close();
            LocalTime finishTime = java.time.LocalDateTime.now().toLocalTime();
            Instant end = Instant.now();
            // System.out.println("Process with PID: " + ProcessHandle.current().pid() +
            // " File: " + args[0]
            // + " Finish time: " + finishTime
            // + " Time in process (millis): "
            // + Duration.between(start, end).toMillis());
        } catch (Exception e) {
            e.printStackTrace();
        }

        Runnable r1 = new Analyzer(1);
        Runnable r2 = new Analyzer(2);
        Runnable r3= new Analyzer(3);
        Runnable r4 = new Analyzer(4);
        Runnable r5 = new Analyzer(5);

        ExecutorService pool = Executors.newFixedThreadPool(MAX_T);
        // for (int i = 0; i < list.size(); i++) {
        //     Runnable r1 = new Analyzer(i);
        //     pool.execute(r1);
        // }
        pool.execute(r1);
        pool.execute(r2);
        pool.execute(r3);
        pool.execute(r4);
        pool.execute(r5);

        // passes the Task objects to the pool to execute (Step 3)

        pool.shutdown();
        try {
            pool.awaitTermination(10000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
        }

        System.out.println(ConsoleColors.WHITE_BOLD + "MetaDict of file " + args[0] +
                ConsoleColors.RESET);
        printDict(metaDict);

    }

    static void printDict(Dictionary<String, MetaData> metaDict) {
        Enumeration<String> k = metaDict.keys();
        while (k.hasMoreElements()) {
            String key = k.nextElement();
            System.out.println("Key: " + key + ", Value: "
                    + metaDict.get(key));
        }
    }

}

class Analyzer implements Runnable {

    private int listIndex;

    public Analyzer(int i) {
        this.listIndex = i;
    }

    public void run() {
        // TODO: Check if new popularity is greater than old one
        // TODO: If ID found but popularity not then move to the next index position
        // TODO: if popularity found but ID not then do not save into dict
        int maxPopularity = 0;
        String tid = String.valueOf(Thread.currentThread().getId()); // get the key of the current thread
        if (Reader.metaDict.get(tid) != null) {
            maxPopularity = Reader.metaDict.get(tid).popularity;
        }
        String line = Reader.list.get(this.listIndex);
        while (true) {
            String popularity = "";
            if (line.indexOf("\n") != -1){
                line = line.substring(line.indexOf("\n")+1);
            } else{
                break;
            }
            if (line.indexOf(",") == -1) { //THE SUBSTRING LEFT IS TO SHOT AND THE ID IS CUT
                break;
            }
            String id = line.substring(0, line.indexOf(","));
            Matcher m = Reader.popularity.matcher(line);
            if (m.find()) {
                popularity = m.group(1);
                String[] numbersArray = popularity.split(",");
                Integer[] popularityArray = Arrays.stream(numbersArray).map(Integer::parseInt).toArray(Integer[]::new);
                Integer popularityTotal = Arrays.stream(popularityArray).mapToInt(Integer::intValue).sum();     
                if (popularityTotal > maxPopularity) {
                    Reader.metaDict.put(tid, new MetaData(id, popularityTotal));
                }
            } else{ //ID BUT NOT POPULARITY
                break; 
            }
            line = line.substring(line.indexOf(popularity));
        }
    }
}

class MetaData {
    public int popularity;
    public String id;

    public MetaData(String id, int popularity) {
        this.id = id;
        this.popularity = popularity;
    }

    @Override
    public String toString() {
        return "[ " + "Id=" + id + ", popularity=" + popularity + " ]";
    }

}
