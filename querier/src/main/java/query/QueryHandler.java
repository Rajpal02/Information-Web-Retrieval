package query;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import queryparser.QueryFileParser;
import util.QueryExpansion;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;

import static constants.DirectoryConstants.*;
import static utils.CommonUtils.replacePunctuation;

public class QueryHandler {
  private Analyzer analyzer;
  private Similarity similarity;
  private int max_results;
  private final LinkedHashMap<String, String> refinedQueries;

  public QueryHandler() {
    QueryFileParser queryFileParser = new QueryFileParser(TOPIC_PATH);
    ArrayList<LinkedHashMap<String, String>> parsedQueries = queryFileParser.parseQueryFile();

    LinkedHashMap<String, String> refinedQueries = new LinkedHashMap<>();
    for (LinkedHashMap<String, String> query : parsedQueries) {
      String queryString = prepareQueryString(query);
      refinedQueries.put(query.get("queryID"), queryString);
    }
    this.refinedQueries = refinedQueries;
  }

  public void configure(Analyzer analyzer, Similarity similarity, int max_results) {
    this.analyzer = analyzer;
    this.similarity = similarity;
    this.max_results = max_results;
  }

  public void executeQueries() throws IOException, ParseException {

    long start_time = System.currentTimeMillis();
    // Configure the index details to query
    IndexReader indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(INDEX_DIR)));
    IndexSearcher indexSearcher = new IndexSearcher(indexReader);
    indexSearcher.setSimilarity(similarity);

    HashMap<String, Float> booster = new HashMap<String, Float>();
    booster.put("title", 0.07F);
    booster.put("content", 1.1F);

    MultiFieldQueryParser indexParser =
        new MultiFieldQueryParser(new String[] {"title", "content"}, analyzer, booster);

    String filename =
        RESULTS_DIR
            + "/results_"
            + analyzer.getClass().getSimpleName().toLowerCase(Locale.ROOT)
            + "_"
            + similarity.getClass().getSimpleName().toLowerCase(Locale.ROOT);

    PrintWriter resultsWriter = new PrintWriter(filename, StandardCharsets.UTF_8);

    for (String queryId : refinedQueries.keySet()) {
      String queryString = refinedQueries.get(queryId);
      Query finalQuery = indexParser.parse(QueryParser.escape(queryString));

      TopDocs initresults = indexSearcher.search(finalQuery, max_results);
      ScoreDoc[] hits = initresults.scoreDocs;
      QueryExpansion expander =
          new QueryExpansion(analyzer, indexSearcher, similarity, indexReader);

      queryString = expander.expandQuery(queryString, hits);
      finalQuery = indexParser.parse(QueryParser.escape(queryString));
      TopDocs results = indexSearcher.search(finalQuery, max_results);
      hits = results.scoreDocs;

      for (int i = 0; i < hits.length; i++) {
        Document document = indexSearcher.doc(hits[i].doc);
        resultsWriter.println(
            queryId
                + " Q0 "
                + document.get("docno")
                + " "
                + i
                + " "
                + hits[i].score
                + " HYLIT"); // HYLIT - Have You Lucene It?
      }
    }

    resultsWriter.close();
    indexReader.close();

    System.out.format(
        "Result file %s generated in "
            + (System.currentTimeMillis() - start_time)
            + " milliseconds\n",
        filename);
  }

  private String prepareQueryString(LinkedHashMap<String, String> query) {
    StringBuilder finalQueryString = new StringBuilder();
    String title = replacePunctuation(query.get("title"));
    String desc = replacePunctuation(query.get("description"));
    String narrative = processNarrativeTag(query.get("narrative"));
    return finalQueryString
        .append(title)
        .append(" ")
        .append(desc)
        .append(" ")
        .append(narrative)
        .toString();
  }

  private String processNarrativeTag(String stringToProcess) {

    String[] unprocessedString =
        stringToProcess.strip().toLowerCase(Locale.ROOT).split("\\p{Punct}");
    StringBuilder processedString = new StringBuilder();

    for (String str : unprocessedString) {
      str = str.strip();
      if (!((str.contains("not") && str.contains("relevant")) || str.contains("irrelevant"))) {
        processedString.append(str.replaceAll("\n", " ")).append(" ");
      }
    }
    return replacePunctuation(processedString.toString());
  }
}
