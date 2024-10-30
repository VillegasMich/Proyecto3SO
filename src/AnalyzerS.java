import java.util.Dictionary;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class AnalyzerS {

  private int listIndex;
  public Dictionary<String, MetaData> metaDictMax;
  public Dictionary<String, MetaData> metaDictMin;
  public List<String> splittedList;

  static final Pattern popularity = Pattern.compile("(\\d+,\\d+,\\d+,\\d+)");
  static final Pattern identification = Pattern.compile("^(.[^,]{10})");
  static final Pattern lineStructure = Pattern.compile("^(.{11})(,[0-9.].*),(\\d+,\\d+,\\d+,\\d+),");
  static final Pattern lineStructureSingle = Pattern.compile("^(.{11})(,[0-9.].*),(\\d+,\\d+,\\d+,\\d+),[a-zA-Z]");

  public AnalyzerS(int i, Dictionary<String, MetaData> metaDictMax, Dictionary<String, MetaData> metaDictMin,
      List<String> list, List<String> splittedList) {
    this.splittedList = splittedList;
    this.metaDictMin = metaDictMin;
    this.metaDictMax = metaDictMax;
    this.listIndex = i;
  }

  private String getIdentification(String line) {
    Matcher m = identification.matcher(line);
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
    // String tid = String.valueOf(Thread.currentThread().getId()); // get the key
    // of the current thread
    String tid = String.valueOf(Thread.currentThread().threadId()); // get the key of the current thread
    if (this.metaDictMax.get(tid) != null) {
      maxPopularity = this.metaDictMax.get(tid).popularity;
    }
    if (this.metaDictMin.get(tid) != null) {
      minPopularity = this.metaDictMin.get(tid).popularity;
    }
    String currVideo = this.splittedList.get(this.listIndex);
    if (currVideo.indexOf(",") != -1) {
      //
      String id = getIdentification(currVideo);
      if (id != null) {
        Matcher m = lineStructureSingle.matcher(currVideo);
        if (m.find()) {
          String extractedNumbers = m.group(3);
          extractedNumbers = extractedNumbers.split(",")[0];
          Integer popularityTotal = Integer.parseInt(extractedNumbers);
          if (popularityTotal == 0) {
            return;
          }
          if (popularityTotal > maxPopularity) {
            this.metaDictMax.put(tid, new MetaData(id, popularityTotal));
          }
          if (popularityTotal < minPopularity) {
            this.metaDictMin.put(tid, new MetaData(id, popularityTotal));
          }
        }
      }
    }
  }
}
