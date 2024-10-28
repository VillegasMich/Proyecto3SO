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
import java.util.concurrent.locks.Lock;

public class Reader {
    static final Pattern popularity = Pattern.compile("(\\d+,\\d+,\\d+,\\d+)");
    static final Pattern identification = Pattern.compile("^(.{11}),");
    static final Pattern lineStructure = Pattern.compile("^(.{11})(,[0-9.].*?){3}(\\d+),");

    static int index = 0;
    static final int PAGE_LIMIT = 4096; // Each char equals 2 bytes; 4k = 4000 bytes
    static List<String> list = new ArrayList<>();
    static List<String> splittedList = new ArrayList<>();
    static final int MAX_T = 1;
    static Dictionary<String, MetaData> metaDictMax = new Hashtable<>();
    static Dictionary<String, MetaData> metaDictMin = new Hashtable<>();
    static Lock lock = new java.util.concurrent.locks.ReentrantLock();

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
            // pool1.awaitTermination(600, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
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
            // pool2.awaitTermination(600, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
        }

        LocalTime finishTime = java.time.LocalDateTime.now().toLocalTime();
        Instant end = Instant.now();
        System.out.print(args[0] + ";" + metaDictMax.get(findMaxPopularity(metaDictMax)).id + ","
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
        while (true) {
            Matcher m = Reader.lineStructure.matcher(line);
            if (m.find()) {
                Reader.lock.lock();
                if (line.indexOf("\n") == -1) {
                    Reader.splittedList.add(line.substring(0));
                    Reader.lock.unlock();
                    break;
                }
                Reader.splittedList.add(line.substring(0, line.indexOf("\n")));
                line = line.substring(line.indexOf("\n") + 1);
                Reader.lock.unlock();
            } else {

                if (line.length() == 0) {
                    break;
                }

                // ! Cuando cae por aqui botamos la informacion restante
                Matcher i = Reader.identification.matcher(line);
                if (i.find()) {
                    System.out.println(line + "---------------------- FALTA NUMEROS  \n \n \n \n");

                    // Asegúrate de que no excedas los límites de la lista
                    if (this.listIndex + 1 < Reader.list.size()) {
                        String nextLine = Reader.list.get(this.listIndex + 1);
                        if (nextLine.indexOf("\n") != -1) {
                            line = line + nextLine.substring(0, nextLine.indexOf("\n"));
                            Reader.lock.lock();
                            Reader.splittedList.add(line);
                            Reader.lock.unlock();

                        } else {
                            break;
                        }
                    }
                    System.out.println(line + "---------------------- LUEGO DE AÑADIR NUMEROS");
                    break;

                    // Faltan numeros
                    // contar las comas hasta llegar a los numeros
                } else {
                    // Falta Id
                    // ir al arreglo de antes y buscar la ultima coma en adelante
                    if (this.listIndex - 1 != -1) {
                        int index = -1;
                        while (i.find()) {
                            System.out.println(line + "---------------------- FALTAID \n \n \n \n");
                            line = Reader.list.get(this.listIndex - index)
                                    .substring(Reader.list.get(this.listIndex - index).lastIndexOf("\n") + 1) + line;
                            index--;
                        }
                        try {
                            System.out.println(line + "\n LUEGO DE AÑADIR ID \n \n \n");
                            TimeUnit.SECONDS.sleep(1);

                        } catch (Exception e) {
                            // TODO: handle exception
                        }

                    } else {
                        break;
                    }
                }

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
                Matcher m = Reader.lineStructure.matcher(currVideo);
                if (m.find()) {
                    String extractedNumbers = m.group(3);
                    // extractedNumbers = extractedNumbers.split(",")[0];
                    Integer popularityTotal = Integer.parseInt(extractedNumbers);
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
