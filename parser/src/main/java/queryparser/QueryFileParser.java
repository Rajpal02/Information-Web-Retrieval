package queryparser;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;

// this class is just used to parse queries from the file and return
// a ArrayList<LinkedHashMap<String, String>> of them
public class QueryFileParser {
  private final String queryFilePath;

  public QueryFileParser(String filePath) {
    this.queryFilePath = filePath;
  }

  public ArrayList<LinkedHashMap<String, String>> parseQueryFile() {
    String TOP_TAG_START = "<top>";
    String NUM_TAG = "<num>";
    String TITLE_TAG = "<title>";
    String DESC_TAG = "<desc>";
    String NARR_TAG = "<narr>";
    String TOP_TAG_END = "</top>";

    ArrayList<LinkedHashMap<String, String>> parsedQueries = new ArrayList<>();

    try {
      BufferedReader bufferedReader =
          Files.newBufferedReader(Paths.get(this.queryFilePath), StandardCharsets.UTF_8);
      String currentLine = bufferedReader.readLine();

      LinkedHashMap<String, String> newQuery = new LinkedHashMap<String, String>();

      while (currentLine != null) {

        if (currentLine.isEmpty()) currentLine = bufferedReader.readLine();

        if (currentLine.startsWith(TOP_TAG_START)) {
          newQuery = new LinkedHashMap<String, String>();
          currentLine = bufferedReader.readLine();
        }

        if (currentLine.startsWith(NUM_TAG)) {
          String[] values = currentLine.trim().split("\\s");
          newQuery.put("queryID", values[values.length - 1]);
          currentLine = bufferedReader.readLine();
        }

        if (currentLine.startsWith(TITLE_TAG)) {
          String title = currentLine.substring(TITLE_TAG.length());
          newQuery.put("title", title.trim());
          currentLine = bufferedReader.readLine();
        }

        if (currentLine.startsWith(DESC_TAG)) {
          currentLine = bufferedReader.readLine();
          StringBuilder description = new StringBuilder();
          while (currentLine != null && !currentLine.startsWith(NARR_TAG)) {
            description.append(currentLine + "\n");
            currentLine = bufferedReader.readLine();
          }
          newQuery.put("description", description.toString().trim());
        }

        if (currentLine.startsWith(NARR_TAG)) {
          currentLine = bufferedReader.readLine();
          StringBuilder narrative = new StringBuilder();
          while (currentLine != null && !currentLine.startsWith(TOP_TAG_END)) {
            narrative.append(currentLine).append("\n");
            currentLine = bufferedReader.readLine();
          }
          newQuery.put("narrative", narrative.toString().trim());
        }

        if (currentLine.startsWith(TOP_TAG_END)) {
          parsedQueries.add(newQuery);
          currentLine = bufferedReader.readLine();
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return parsedQueries;
  }
}
