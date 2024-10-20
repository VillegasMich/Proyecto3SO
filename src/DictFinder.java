import java.util.Dictionary;
import java.util.Enumeration;

public class DictFinder {
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
