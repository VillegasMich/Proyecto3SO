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
    static final Pattern popularity = Pattern.compile("\\|[^,]*,((\\d+,?)+),[^,]*");
    static final Pattern identification = Pattern.compile("([A-Za-z0-9]+)(,[^,]+)+");
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

        Runnable r1 = new Analyzer(0);
        Runnable r2 = new Analyzer(2);
        Runnable r3 = new Analyzer(3);
        Runnable r4 = new Analyzer(4);
        Runnable r5 = new Analyzer(5);

        ExecutorService pool = Executors.newFixedThreadPool(MAX_T);

        // passes the Task objects to the pool to execute (Step 3)
        pool.execute(r1);
        pool.execute(r2);
        pool.execute(r3);
        pool.execute(r4);
        pool.execute(r5);

        pool.shutdown();
        try {
            pool.awaitTermination(1000, TimeUnit.MILLISECONDS);
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

    private int getPopularity(String line) {
        Matcher m = Reader.popularity.matcher(line);
        if (m.find()) {
            String extractedNumbers = m.group(1);
            String[] numbersArray = extractedNumbers.split(",");
            Integer[] popularityArray = Arrays.stream(numbersArray)
                    .map(Integer::parseInt)
                    .toArray(Integer[]::new);
            Integer popularityTotal = Arrays.stream(popularityArray).mapToInt(Integer::intValue).sum();
            return popularityTotal;
        } else {
            System.out.println(ConsoleColors.RED_BOLD + "Match for popularity not found" + ConsoleColors.RESET);
            return -1;
        }
    }

    private String getIdentification(String line) {
        Matcher m = Reader.identification.matcher(line);
        if (m.find()) {
            String videoId = m.group(1);
            return videoId;
        } else {
            System.out.println(ConsoleColors.RED_BOLD + "Match ID not found" + ConsoleColors.RESET);
            return null;
        }
    }

    public void run() {
        String tid = String.valueOf(Thread.currentThread().threadId());
        int popularityTotal = getPopularity(Reader.list.get(this.listIndex));
        String videoId = getIdentification(Reader.list.get(this.listIndex));
        // TODO: Check if new popularity is greater than old one
        // TODO: If ID found but popularity not then move to the next index position
        // TODO: if popularity found but ID not then do not save into dict
        if (popularityTotal > 0 && videoId != null) {
            Reader.metaDict.put(tid, new MetaData(videoId, popularityTotal));
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
