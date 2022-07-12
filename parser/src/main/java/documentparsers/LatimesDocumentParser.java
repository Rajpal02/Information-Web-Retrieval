package documentparsers;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static constants.DirectoryConstants.DATA_DIR;
import static utils.CommonUtils.replacePunctuation;

public class LatimesDocumentParser implements Callable<String> {
  private final String LATIMES_DIR_PATH = DATA_DIR + "/latimes";
  private final IndexWriter iwriter;

  private final Set<String> irrelevantTags =
      Set.of("docid", "date", "dateline", "length", "correction-date");

  public LatimesDocumentParser(IndexWriter indexWriter) {
    this.iwriter = indexWriter;
  }

  public String call() throws IOException {
    long start_time = System.currentTimeMillis();
    File directoryPath = new File(LATIMES_DIR_PATH);
    List<File> filesList =
        Arrays.stream(Objects.requireNonNull(directoryPath.listFiles()))
            .filter(file -> !file.toString().toLowerCase(Locale.ROOT).endsWith(".txt"))
            .collect(Collectors.toList());

    int totalCount = 0;

    for (File file : filesList) {
      org.jsoup.nodes.Document fileToParse = Jsoup.parse(file, "UTF-8");
      List<Element> documentsInFile = fileToParse.select("doc");

      int count = 0;
      for (Element element : documentsInFile) {
        List<Node> nodes =
            element.childNodes().stream()
                .filter(node -> !node.toString().equals(" "))
                .collect(Collectors.toList());
        iwriter.addDocument(createDocument(nodes));
        count++;
      }
      System.out.format("Indexed %s documents from file %s\n", count, file);
      totalCount += count;
    }
    System.out.format(
        "Indexed %s documents in %s seconds\n",
        totalCount, TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start_time));
    return "Done parsing LA Times";
  }

  private Document createDocument(List<Node> nodes) {
    Document document = new Document();
    // title is headline + byline
    StringBuilder title = new StringBuilder();

    // alltext is all the other text present in the doc
    StringBuilder allText = new StringBuilder();
    for (Node node : nodes) {
      // do additional processing before indexing as per requirement
      String tag = ((Element) node).tagName().strip().toLowerCase(Locale.ROOT);
      String text = ((Element) node).text().strip().toLowerCase(Locale.ROOT);

      // if tag is not empty AND text is not empty AND tag is not part of irrelevantTags set
      if (!tag.equals("") && !text.equals("") && !irrelevantTags.contains(tag)) {
        if (tag.equals("docno")) {
          document.add(new StringField(tag, ((Element) node).text(), Field.Store.YES));
        } else if (tag.equals("headline") || tag.equals("byline")) {
          title.append(text).append(" ");
        } else {
          allText.append(text).append(" ");
        }
      }
    }

    document.add(
        new TextField("title", replacePunctuation(title.toString().strip()), Field.Store.YES));
    document.add(
        new TextField("content", replacePunctuation(allText.toString().strip()), Field.Store.YES));

    return document;
  }
}

// [date, dateline, docid, subject, length, section, docno, type, p, correction-date, text,
// correction, headline, byline, graphic]
