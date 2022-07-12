import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.similarities.*;
import query.QueryHandler;
import utils.StopWordGenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static constants.DirectoryConstants.INDEX_DIR;
import static constants.DirectoryConstants.RESULTS_DIR;

public class Querier {

  public static void main(String[] args) throws InterruptedException, IOException, ParseException {
    createDirs();
    StopWordGenerator stopWordGenerator = StopWordGenerator.getInstance();

    // creating a list of analysers
    List<Analyzer> analysers = new ArrayList<>();
    // analysers.add(new StandardAnalyzer(stopWordGenerator.getCharset()));
    analysers.add(new EnglishAnalyzer(stopWordGenerator.getCharset()));
    // analysers.add(new ClassicAnalyzer(stopWordGenerator.getCharset()));
    // analysers.add(new StopAnalyzer(stopWordGenerator.getCharset()));
    // analysers.add(new SimpleAnalyzer());
    //    analysers.add(
    //        new Analyzer() {
    //          @Override
    //          protected TokenStreamComponents createComponents(String s) {
    //            WhitespaceTokenizer src = new WhitespaceTokenizer();
    //            TokenStream result = new LowerCaseFilter(src);
    //            result = new StopFilter(result, stopWordGenerator.getCharset());
    //            result = new PorterStemFilter(result);
    //            return new TokenStreamComponents(src, result);
    //          }
    //        });

    // creating a list of similarities
    List<Similarity> similarities = new ArrayList<>();
    //    similarities.add(new ClassicSimilarity());
    //    similarities.add(new BM25Similarity(0.65F, 0.8F));
    similarities.add(
        new MultiSimilarity(
            new Similarity[] {new BM25Similarity(1.1F, 0.95F), new AxiomaticF2EXP(0.2F)}));
    //    similarities.add(new LMDirichletSimilarity());
    similarities.add(new LMJelinekMercerSimilarity(0.7F));
    //    similarities.add(new AxiomaticF1EXP());
    //    similarities.add(new AxiomaticF1LOG());
    //    similarities.add(new AxiomaticF2EXP(0.2F));
    //    similarities.add(new AxiomaticF2LOG());
    //    similarities.add(new AxiomaticF3EXP(0.5F, 1));

    QueryHandler queryHandler = new QueryHandler();

    for (Analyzer analyser : analysers) {

      Parser parser = new Parser(analyser);
      parser.parseAndIndex();
      TimeUnit.SECONDS.sleep(1);

      for (Similarity similarity : similarities) {
        queryHandler.configure(analyser, similarity, 1000);
        queryHandler.executeQueries();
      }
    }
  }

  private static void createDirs() {
    boolean indexDirectory = new File(INDEX_DIR).mkdir();
    boolean resultsDirectory = new File(RESULTS_DIR).mkdir();
  }
}
