import java.io.File;
import java.io.FilenameFilter;
import java.io.RandomAccessFile;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FilesManager {

  static final int PAGE_LIMIT = 4096;

  private String folder;
  private String[] paths;
  private List<byte[]> list;
  // private int limit = PAGE_LIMIT; // Each char equals 2 bytes; 4k = 4000 bytes

  /**
   * @param folder folder path to work with (relative from your open folder)
   * @throws IllegalArgumentException
   *                                  if param is no folder
   */
  public FilesManager(String folder) throws IllegalArgumentException {
    if (!(new File(folder).isDirectory())) {
      throw new IllegalArgumentException();
    }
    this.folder = folder;
    this.list = new ArrayList<>();
  }

  /**
   * Adds all the .csv files from the corresponding folder
   * 
   * @return number of .csv files found
   */
  public int addCsvFiles() {
    File directory = new File(this.folder);
    this.paths = directory.list(new FilenameFilter() {
      @Override
      public boolean accept(File directory, String name) {
        return name.toLowerCase().endsWith(".csv");
      }
    });
    return this.paths.length;
  }

  public void readAllFiles() {
    for (int i = 0; i < this.paths.length; i++) {
      this.readIndexFile(i);
    }
    System.out.println("Total pages: " + list.size());
  }

  public void readIndexFile(int index) {

    long pointer = 0;
    try {
      Instant start = Instant.now();
      // System.out.println("Reading file: " + this.paths[index]);
      RandomAccessFile myRaf = new RandomAccessFile(this.folder + this.paths[index], "r");
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
      Instant end = Instant.now();
      System.out.println("Time in process (millis): " + Duration.between(start, end).toMillis());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public String getFolder() {
    return this.folder;
  }

  public String getPathsToString() {
    return Arrays.toString(this.paths);
  }

  public String[] getPaths() {
    return this.paths;
  }
}
