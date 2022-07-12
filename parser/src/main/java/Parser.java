import documentparsers.FTDocumentParser;
import documentparsers.FbisParser;
import documentparsers.Fr94Parser;
import documentparsers.LatimesDocumentParser;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static constants.DirectoryConstants.INDEX_DIR;

public class Parser {
  final Analyzer analyzer;

  public Parser(Analyzer analyzer) {
    this.analyzer = analyzer;
  }

  public void parseAndIndex() throws IOException, InterruptedException {

    IndexWriterConfig config = new IndexWriterConfig(analyzer);
    config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
    config.setRAMBufferSizeMB(2048);

    Directory directory = FSDirectory.open(Paths.get(INDEX_DIR));
    IndexWriter iwriter = new IndexWriter(directory, config);

    LatimesDocumentParser latimesDocumentParser = new LatimesDocumentParser(iwriter);
    FTDocumentParser ftDocumentParser = new FTDocumentParser(iwriter);
    Fr94Parser fr94Parser = new Fr94Parser(iwriter);
    FbisParser fbisParser = new FbisParser(iwriter);

    //Multithreading to reduce time to index
    ExecutorService executorService = Executors.newFixedThreadPool(4);
    List<Callable<String>> tasks = new ArrayList<>(4);

    tasks.add(latimesDocumentParser);
    tasks.add(fr94Parser);
    tasks.add(ftDocumentParser);
    tasks.add(fbisParser);

    List<Future<String>> answers = executorService.invokeAll(tasks);

    executorService.shutdown();
    System.out.println(answers);

    iwriter.close();
    directory.close();
  }
}
