import java.io.RandomAccessFile;
import java.time.*;
import java.util.ArrayList;
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
import java.util.concurrent.locks.Lock;

/* 
 * Perfect: US CA GB IN JP KR RU
 * Not perfect: FR-(max) DE-(min) MX-(min)
 * */

public class Reader {
    static final Pattern popularity = Pattern.compile("(\\d+,\\d+,\\d+,\\d+)");
    static final Pattern identification = Pattern.compile("^(.[^,]{10})");
    static final Pattern lineStructure = Pattern.compile("(.{11})(,[0-9.].*),(\\d+,\\d+,\\d+,\\d+),");
    static final Pattern lineStructureSingle = Pattern.compile("^(.{11})(,[0-9.].*),(\\d+,\\d+,\\d+,\\d+),[a-zA-Z]");

    static int index = 0;
    static final int PAGE_LIMIT = 4096; // Each char equals 2 bytes; 4k = 4000 bytes
    static List<String> list = new ArrayList<>();
    static List<String> splittedList = new ArrayList<>();
    static final int MAX_T = 10;
    static Dictionary<String, MetaData> metaDictMax = new Hashtable<>();
    static Dictionary<String, MetaData> metaDictMin = new Hashtable<>();
    static Lock lock = new java.util.concurrent.locks.ReentrantLock();

    public static void main(String[] args) {

        Instant start = Instant.now();
        long pointer = 0;
        try {
            // RandomAccessFile myRaf = new RandomAccessFile("src/output/CAvideos.csv",
            // "r");
            RandomAccessFile myRaf = new RandomAccessFile(args[0], "r");
            myRaf.seek(0);
            long length = myRaf.length();
            byte[] line = new byte[PAGE_LIMIT];
            while (pointer < length) {
                myRaf.read(line, 0, PAGE_LIMIT);
                list.add(new String(line, StandardCharsets.UTF_8));
                pointer += PAGE_LIMIT;
            }
            myRaf.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        ExecutorService pool1 = Executors.newFixedThreadPool(MAX_T);

        for (int i = 0; i < list.size(); i++) {
            Runnable r1 = new Splitter(i);
            pool1.execute(r1);
        }

        pool1.shutdown();
        try {
            while (!pool1.isTerminated()) {
                pool1.awaitTermination(1, TimeUnit.MILLISECONDS); // Espera brevemente para no hacer spinlock
            }
        } catch (Exception e) {
            pool1.shutdownNow();
        }

        ExecutorService pool2 = Executors.newFixedThreadPool(MAX_T);

        for (int i = 0; i < splittedList.size(); i++) {
            Runnable r1 = new Analyzer(i);
            pool2.execute(r1);
        }

        pool2.shutdown();
        try {
            while (!pool2.isTerminated()) {
                pool2.awaitTermination(1, TimeUnit.MILLISECONDS); // Espera brevemente para no hacer spinlock
            }
        } catch (Exception e) {
            pool2.shutdownNow();
        }

        // printDict(metaDictMax);
        // printDict(metaDictMin);

        // splittedList.stream().forEach((s) -> System.out.println(s + "\n"));
        LocalTime finishTime = java.time.LocalDateTime.now().toLocalTime();
        Instant end = Instant.now();
        System.out.print(args[0] + ";" +
                metaDictMax.get(findMaxPopularity(metaDictMax)).id + ","
                + metaDictMax.get(findMaxPopularity(metaDictMax)).popularity + ";"
                + metaDictMin.get(findMinPopularity(metaDictMin)).id + ","
                + metaDictMin.get(findMinPopularity(metaDictMin)).popularity + ";" + "Process with PID: " +
                ProcessHandle.current().pid() +
                " File: " + args[0]
                + " Finish time: " + finishTime
                + " Time in process (millis): "
                + Duration.between(start, end).toMillis()
                + " ms");
    }

    public static String findMaxPopularity(Dictionary<String, MetaData> metaDict) {
        String maxKey = null;
        int maxPopularity = Integer.MIN_VALUE;

        // Recorrer todas las claves del Dictionary
        Enumeration<String> claves = metaDict.keys();

        while (claves.hasMoreElements()) {
            String clave = claves.nextElement();
            MetaData metadata = metaDict.get(clave);

            if (metadata.popularity > maxPopularity) {
                maxPopularity = metadata.popularity;
                maxKey = clave;
            }
        }
        return maxKey;
    }

    public static String findMinPopularity(Dictionary<String, MetaData> metaDict) {
        String minKey = null;
        int minPopularity = Integer.MAX_VALUE;

        // Recorrer todas las claves del Dictionary
        Enumeration<String> claves = metaDict.keys();

        while (claves.hasMoreElements()) {
            String clave = claves.nextElement();
            MetaData metadata = metaDict.get(clave);

            if (metadata.popularity < minPopularity) {
                minPopularity = metadata.popularity;
                minKey = clave;
            }
        }
        return minKey;
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

class Splitter implements Runnable {
    private int listIndex;

    public Splitter(int i) {
        this.listIndex = i;
    }

    public void run() {
        String line = Reader.list.get(this.listIndex);
        Boolean flag = false;
        while (!flag) {
            Matcher m = Reader.lineStructure.matcher(line);
            if (m.find()) {
                String videoInfo = "";
                if (line.indexOf("\n") == -1) {
                    videoInfo = line.substring(0);
                    flag = true;
                } else {
                    videoInfo = line.substring(0, line.indexOf("\n"));
                    int pos = line.indexOf("\n") + 1;
                    if (pos == 0) {
                        flag = true;
                    }
                    line = line.substring(pos);
                }
                Matcher v = Reader.lineStructure.matcher(videoInfo);
                if (v.find()) {
                    Reader.splittedList.add(videoInfo);
                } else {
                    // System.out.println(v.find());
                    // Revisar videoInfo con que no cumple
                }
            } else {
                Matcher idMatcher = Reader.identification.matcher(line);
                Matcher popMatcher = Reader.popularity.matcher(line);
                if (idMatcher.find() && !popMatcher.find()) {
                    int index = 1;
                    if (this.listIndex + index < Reader.list.size()
                            && Reader.list.get(this.listIndex + index).indexOf("\n") != -1) {
                        line = line + Reader.list.get(this.listIndex + index).substring(0,
                                Reader.list.get(this.listIndex + index).indexOf("\n"));
                    }
                    String videoInfo = "";
                    if (line.indexOf("\n") == -1) {
                        videoInfo = line.substring(0);
                        flag = true;
                    } else {
                        videoInfo = line.substring(0, line.indexOf("\n"));
                        int pos = line.indexOf("\n") + 1;
                        if (pos == 0) {
                            flag = true;
                        }
                        line = line.substring(pos);
                    }
                    Matcher v = Reader.lineStructure.matcher(videoInfo);
                    if (v.find()) {
                        Reader.splittedList.add(videoInfo);
                    }
                    if (line.length() == 0) {
                        break;
                    }
                    // falta popularidad
                } else {
                    int index = 1;
                    if (this.listIndex - index >= 0) {
                        line = Reader.list.get(this.listIndex - index)
                                .substring(Reader.list.get(this.listIndex - index).lastIndexOf("\n") + 1) + line;
                    }
                    String videoInfo = "";
                    if (line.indexOf("\n") == -1) {
                        videoInfo = line.substring(0);
                        flag = true;
                    } else {
                        videoInfo = line.substring(0, line.indexOf("\n"));
                        int pos = line.indexOf("\n") + 1;
                        if (pos == 0) {
                            flag = true;
                        }
                        line = line.substring(pos);
                    }
                    Matcher v = Reader.lineStructure.matcher(videoInfo);
                    if (v.find()) {
                        Reader.splittedList.add(videoInfo);
                    }
                }
                // Revisar videoInfo con que no cumple
            }
        }
    }
}

class Analyzer implements Runnable {

    private int listIndex;

    public Analyzer(int i) {
        this.listIndex = i;
    }

    private String getIdentification(String line) {
        Matcher m = Reader.identification.matcher(line);
        if (m.find()) {
            String videoId = m.group(1);
            return videoId;
        } else {
            return null;
        }
    }

    public void run() {
        int maxPopularity = Integer.MIN_VALUE;
        int minPopularity = Integer.MAX_VALUE;
        String tid = String.valueOf(Thread.currentThread().getId()); // get the key of the current thread
        if (Reader.metaDictMax.get(tid) != null) {
            maxPopularity = Reader.metaDictMax.get(tid).popularity;
        }
        if (Reader.metaDictMin.get(tid) != null) {
            minPopularity = Reader.metaDictMin.get(tid).popularity;
        }
        String currVideo = Reader.splittedList.get(this.listIndex);
        if (currVideo.indexOf(",") != -1) {
            //
            String id = getIdentification(currVideo);
            if (id != null) {
                Matcher m = Reader.lineStructureSingle.matcher(currVideo);
                if (m.find()) {
                    String extractedNumbers = m.group(3);
                    extractedNumbers = extractedNumbers.split(",")[0];
                    Integer popularityTotal = Integer.parseInt(extractedNumbers);
                    if (popularityTotal == 0) {
                        return;
                    }
                    if (popularityTotal > maxPopularity) {
                        Reader.metaDictMax.put(tid, new MetaData(id, popularityTotal));
                    }
                    if (popularityTotal < minPopularity) {
                        Reader.metaDictMin.put(tid, new MetaData(id, popularityTotal));
                    }
                }
            }
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
        return "[ " + "Id = " + id + ", popularity = " + popularity + " ]";
    }

}
