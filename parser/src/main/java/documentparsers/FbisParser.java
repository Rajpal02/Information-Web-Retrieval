package documentparsers;

import constants.FbisTags;
import constants.FbisTagsData;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;

import static constants.DirectoryConstants.DATA_DIR;

public class FbisParser implements Callable<String> {

  private static BufferedReader br;
  private static final String[] IGNORE_FILES = {"readchg.txt", "readmefb.txt"};

  private final String DIR_PATH = DATA_DIR + "/fbis";
  private final IndexWriter indexWriter;

  public FbisParser(IndexWriter indexWriter) {
    this.indexWriter = indexWriter;
  }

  public String call() throws IOException {
    Directory fbisDir = FSDirectory.open(Paths.get(this.DIR_PATH));

    for (String file : fbisDir.listAll()) {
      if (!file.equals(IGNORE_FILES[0]) && !file.equals(IGNORE_FILES[1])) {
        br = new BufferedReader(new FileReader(this.DIR_PATH + "/" + file));
        System.out.println(this.DIR_PATH + "/" + file);
        indexDoc(this.indexWriter);
      }
    }
    return "Done FBIs";
  }

  private void indexDoc(IndexWriter iwr) throws IOException {

    StringBuilder sbld = new StringBuilder();
    String buffRead = br.readLine();
    try {

      while (buffRead != null) {
        sbld.append(buffRead);
        sbld.append("\n");
        buffRead = br.readLine();
      }
    } finally {
      br.close();
    }

    org.jsoup.nodes.Document document = Jsoup.parse(sbld.toString());

    List<Element> list = document.getElementsByTag("doc");
    for (Element doc : list) {

      FbisTagsData fbisData = new FbisTagsData();
      doc.getElementsByTag(FbisTags.DOCNO.name());
      fbisData.setDocNum(removeTags(doc, FbisTags.DOCNO));
      doc.getElementsByTag(FbisTags.TEXT.name());
      fbisData.setText(removeTags(doc, FbisTags.TEXT));
      doc.getElementsByTag(FbisTags.TI.name());
      fbisData.setTi(removeTags(doc, FbisTags.TI));
      fbisData.setAll(fbisData.getText() + " " + fbisData.getTi().trim());
      createFBISDocument(fbisData, iwr);
    }
  }

  private String removeTags(Element doc, FbisTags tag) {

    Elements ele = doc.getElementsByTag(tag.name());
    Elements duplicateElement = ele.clone();

    for (FbisTags tagValue : FbisTags.values()) {
      if (ele.toString().contains("<" + tagValue.name().toLowerCase() + ">")
          && ele.toString().contains("</" + tagValue.name().toLowerCase() + ">")
          && !tagValue.equals(tag)) {
        ele.select(tagValue.toString()).remove();
      }
    }

    String text = duplicateElement.toString();
    if (text.contains("\n")) text = text.replaceAll("\n", "").trim();
    if (text.contains(("<" + tag.name() + ">").toLowerCase()))
      text = text.replaceAll("<" + tag.name().toLowerCase() + ">", "").trim();
    if (text.contains(("</" + tag.name() + ">").toLowerCase()))
      text = text.replaceAll("</" + tag.name().toLowerCase() + ">", "").trim();

    text = text.trim().replaceAll(" +", " ");
    return text;
  }

  private void createFBISDocument(FbisTagsData fbisData, IndexWriter iwr) {
    Document document = new Document();
    document.add(new StringField("docno", fbisData.getDocNum(), Field.Store.YES));
    document.add(new TextField("title", fbisData.getTi(), Field.Store.YES));
    document.add(new TextField("content", fbisData.getAll(), Field.Store.YES));
    try {
      iwr.addDocument(document);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
