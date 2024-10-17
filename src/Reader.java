import java.io.RandomAccessFile;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.nio.charset.StandardCharsets;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Enumeration;

public class Reader {

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
            System.out.println("Process with PID: " + ProcessHandle.current().pid() +
                    " File: " + args[0]
                    + " Finish time: " + finishTime
                    + " Time in process (millis): "
                    + Duration.between(start, end).toMillis());
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Current Thread: " + Thread.currentThread().threadId());

        Runnable r1 = new Analyzer(1); 
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
        } catch (Exception e) {}
        Enumeration<String> k = metaDict.keys();
        while (k.hasMoreElements()) {
            String key = k.nextElement();
            System.out.println("Key: " + key + ", Value: "
                               + metaDict.get(key));
        }

    }
}

class Analyzer implements Runnable {

    private int index;

    public Analyzer(int i){
        this.index = i;
    }

    public void run(){
        String tid = String.valueOf(Thread.currentThread().threadId());
        
        // Reader.metaDict.put(tid,new MetaData("0", 1));
    }
    
    
}


class MetaData{
    public int popularity;
    public String Id;
    public MetaData(String Id, int popularity){
        this.Id = Id;
        this.popularity = popularity;
    }
}