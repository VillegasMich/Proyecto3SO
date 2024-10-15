import java.io.RandomAccessFile;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

public class Reader {

    static final int PAGE_LIMIT = 4096; // Each char equals 2 bytes; 4k = 4000 bytes

    public static void main(String[] args) {
        Instant start = Instant.now();
        List<byte[]> list = new ArrayList<>();
        // int limit = PAGE_LIMIT;
        long pointer = 0;
        // long difference = 0;
        try {
            RandomAccessFile myRaf = new RandomAccessFile(args[0], "r");
            myRaf.seek(0);
            long length = myRaf.length();
            byte[] line = new byte[PAGE_LIMIT];
            while (pointer < length) {
                int numRead = myRaf.read(line, 0, PAGE_LIMIT);
                // System.out.println(numRead + "\n------------------------------");
                list.add(line);
                pointer += PAGE_LIMIT;
            }
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
    }
}
