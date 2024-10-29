import java.io.File;
import java.io.FilenameFilter;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

public class FilesManager {

  static final int PAGE_LIMIT = 4096;

  private String folder;
  private String[] paths;
  // public List<String> list;
  // public List<String> splittedList;

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
    // this.list = new ArrayList<>();
    // this.splittedList = new ArrayList<>();
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
  }

  public MetaData[] readIndexFile(int index) {

    Dictionary<String, MetaData> metaDictMax = new Hashtable<>();
    Dictionary<String, MetaData> metaDictMin = new Hashtable<>();
    List<String> list = new ArrayList<>();
    List<String> splittedList = new ArrayList<>();

    long pointer = 0;
    Instant start = Instant.now();
    try {
      RandomAccessFile myRaf = new RandomAccessFile(this.folder + this.paths[index], "r");
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

    for (int i = 0; i < list.size(); i++) {
      SplitterS splitter = new SplitterS(i, list, splittedList);
      splitter.run();
    }

    for (int i = 0; i < splittedList.size(); i++) {
      AnalyzerS analyzer = new AnalyzerS(i, metaDictMax, metaDictMin, list, splittedList);
      analyzer.run();
    }

    MetaData max = new MetaData(metaDictMax.get(DictFinder
        .findMaxPopularity(metaDictMax)).id, metaDictMax.get(
            DictFinder
                .findMaxPopularity(metaDictMax)).popularity);
    MetaData min = new MetaData(metaDictMin.get(DictFinder
        .findMinPopularity(metaDictMin)).id, metaDictMin.get(
            DictFinder
                .findMinPopularity(metaDictMin)).popularity);
    MetaData[] maxMin = { max, min };

    System.out.println(ConsoleColors.WHITE_BOLD
        + "----------------------------------------------------------------------"
        + ConsoleColors.RESET);
    Instant end = Instant.now();
    System.out.println(ConsoleColors.WHITE_BOLD + "File: " + this.paths[index]
        + " Time in process (millis): "
        + Duration.between(start, end).toMillis()
        + " ms" + ConsoleColors.RESET);
    System.out.println(
        ConsoleColors.GREEN + "The most popular video of " + this.paths[index] + " is: " + max + ConsoleColors.RESET);
    System.out.println(
        ConsoleColors.CYAN + "The least popular video of " + this.paths[index] + " is: " + min + ConsoleColors.RESET);

    System.out.println(ConsoleColors.WHITE_BOLD
        + "----------------------------------------------------------------------"
        + ConsoleColors.RESET);

    return maxMin;

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
