package util;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * This class is use to create a Term vectors for relevant documents and
 * queries.
 *
 **/
public class QueryTermVector {

	private Map<String, Integer> termFreqMap;

	public String getField() {
		return null;
	}

	public QueryTermVector(Map<String, Integer> termFreqMap) {
		this.termFreqMap = termFreqMap;
	}

	public QueryTermVector(String queryString, Analyzer analyzer) {
		if (analyzer != null) {
			this.termFreqMap = new HashMap<String, Integer>();
			try (TokenStream stream = analyzer.tokenStream("", new StringReader(queryString))) {
				stream.addAttribute(CharTermAttribute.class);
				stream.reset();
				while (stream.incrementToken()) {
					CharTermAttribute token = stream.getAttribute(CharTermAttribute.class);
					if (termFreqMap.get(token.toString()) != null) {
						int value = termFreqMap.get(token.toString());
						termFreqMap.put(token.toString(), (value + 1));
					} else {
						termFreqMap.put(token.toString(), 1);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	public Map<String, Integer> getTermFreqMap() {
		return termFreqMap;
	}

	public void setTermFreqMap(Map<String, Integer> termFreqMap) {
		this.termFreqMap = termFreqMap;
	}
}
