import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;

class SplitterS {
  private int listIndex;
  public List<String> list;
  public List<String> splittedList;
  static final Pattern popularity = Pattern.compile("(\\d+,\\d+,\\d+,\\d+)");
  static final Pattern identification = Pattern.compile("^(.[^,]{10})");
  static final Pattern lineStructure = Pattern.compile("(.{11})(,[0-9.].*),(\\d+,\\d+,\\d+,\\d+),");

  public SplitterS(int i, List<String> list, List<String> splittedList) {
    this.list = list;
    this.splittedList = splittedList;
    this.listIndex = i;
  }

  public void run() {
    String line = this.list.get(this.listIndex);
    Boolean flag = false;
    while (!flag) {
      Matcher m = lineStructure.matcher(line);
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
        Matcher v = lineStructure.matcher(videoInfo);
        if (v.find()) {
          this.splittedList.add(videoInfo);
        } else {
          // System.out.println(v.find());
          // Revisar videoInfo con que no cumple
        }
      } else {
        Matcher idMatcher = identification.matcher(line);
        Matcher popMatcher = popularity.matcher(line);
        if (idMatcher.find() && !popMatcher.find()) {
          int index = 1;
          if (this.listIndex + index < list.size()
              && list.get(this.listIndex + index).indexOf("\n") != -1) {
            line = line + list.get(this.listIndex + index).substring(0,
                list.get(this.listIndex + index).indexOf("\n"));
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
          Matcher v = lineStructure.matcher(videoInfo);
          if (v.find()) {
            splittedList.add(videoInfo);
          }
          if (line.length() == 0) {
            break;
          }
          // falta popularidad
        } else {
          int index = 1;
          if (this.listIndex - index >= 0) {
            line = list.get(this.listIndex - index)
                .substring(list.get(this.listIndex - index).lastIndexOf("\n") + 1) + line;
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
            splittedList.add(videoInfo);
          }
        }

      }
    }
  }
}
