import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;

class SplitterS {
  private int listIndex;
  public List<String> list;
  public List<String> splittedList;

  static final Pattern popularity = Pattern.compile("(\\d+,\\d+,\\d+,\\d+)");
  static final Pattern identification = Pattern.compile("^([A-Za-z0-9]{11}),");
  static final Pattern lineStructure = Pattern.compile("^([A-Za-z0-9]{11})(,[0-9.]+).*?(\\d+,\\d+,\\d+,\\d+),");

  public SplitterS(int i, List<String> list, List<String> splittedList) {
    this.list = list;
    this.splittedList = splittedList;
    this.listIndex = i;
  }

  public void run() {
    String line = this.list.get(this.listIndex);
    while (true) {
      Matcher m = lineStructure.matcher(line);
      if (line.indexOf("\n") != -1 && m.find()) {
        this.splittedList.add(line.substring(0, line.indexOf("\n")));
        line = line.substring(line.indexOf("\n") + 1);
      } else {
        // ! Cuando cae por aqui botamos la informacion restante
        Matcher i = identification.matcher(line);
        if (i.find()) {
          // Faltan numeros
          break;
        } else {
          // Falta Id
          break;
        }

      }
    }
  }
}
