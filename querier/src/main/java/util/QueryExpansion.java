package util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.Similarity;

/**
 * Implementation of Rocchio's pseudo feedback Query Expansion algorithm.
 */
public class QueryExpansion {
	/**
	 * how much importance of document decays as doc rank gets higher. decay = decay
	 * * rank 0 - no decay
	 */
	public static final String DECAY_FLD = "QE.decay";
	/**
	 * Number of documents to use
	 */
	public static final String DOC_NUM_FLD = "QE.doc.num";
	/**
	 * Number of terms to produce
	 */
	public static final String TERM_NUM_FLD = "QE.term.num";

	/**
	 * Rocchio Params
	 */
	public static final String ROCCHIO_ALPHA_FLD = "rocchio.alpha";
	public static final String ROCCHIO_BETA_FLD = "rocchio.beta";

	private Properties prop;
	private Analyzer analyzer;
	private IndexSearcher searcher;
	private IndexReader reader;
	private static Logger logger = Logger.getLogger(QueryExpansion.class.getName());

	/**
	 * Creates a new instance of QueryExpansion
	 *
	 */
	public QueryExpansion(Analyzer analyzer, IndexSearcher searcher, Similarity similarity, IndexReader reader) {
		this.analyzer = analyzer;
		this.searcher = searcher;
		this.reader = reader;
		this.prop = new Properties();
		try {
			this.prop.load(QueryExpansion.class.getClassLoader().getResourceAsStream("config.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String expandQuery(String queryStr, ScoreDoc[] docs) throws IOException, ParseException {
		Vector<Document> vHits = getDocs(queryStr, docs);
		return expandQuery(queryStr, docs, prop);
	}

	/**
	 * Gets documents that will be used in query expansion. number of docs indicated
	 * by <code>QueryExpansion.DOC_NUM_FLD</code>
	 * 
	 */
	private Vector<Document> getDocs(String query, ScoreDoc[] hits) throws IOException {
		Vector<Document> vHits = new Vector<Document>();
		int docNum = Integer.valueOf(prop.getProperty(QueryExpansion.DOC_NUM_FLD)).intValue();
		for (int i = 0; ((i < docNum) && (i < hits.length)); i++) {
			vHits.add(this.searcher.doc(hits[i].doc));
		}

		return vHits;
	}

	/**
	 * Expands the query based on terms in relevent documents.
	 * 
	 * @param queryStr
	 * @param hits
	 * @param prop
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 */

	public String expandQuery(String queryStr, ScoreDoc[] hits, Properties prop) throws IOException, ParseException {
		float alpha = Float.valueOf(prop.getProperty(QueryExpansion.ROCCHIO_ALPHA_FLD)).floatValue();
		float beta = Float.valueOf(prop.getProperty(QueryExpansion.ROCCHIO_BETA_FLD)).floatValue();
		int docNum = Integer.valueOf(prop.getProperty(QueryExpansion.DOC_NUM_FLD)).intValue();
		int termNum = Integer.valueOf(prop.getProperty(QueryExpansion.TERM_NUM_FLD)).intValue();
		List<Map<String, Integer>> docsTermVector = getDocsTerms(hits, docNum, analyzer);
		String expandedQuery = adjust(docsTermVector, queryStr, alpha, beta, docNum, termNum);
		return expandedQuery;
	}

	/**
	 * Adjust term features of queries with boost with alpha * query; and of
	 * documents with beta * docTerms
	 *
	 */
	public String adjust(List<Map<String, Integer>> docsTermsVector, String queryStr, float alpha, float beta,
			int docsRelevantCount, int maxExpandedQueryTerms) throws IOException, ParseException {
		Map<String, Double> docsTerms = setBoost(docsTermsVector, beta);
		logger.finer(docsTerms.toString());
		QueryTermVector queryTermsVector = new QueryTermVector(queryStr, this.analyzer);
		Map<String, Double> queryTerms = setBoost(queryTermsVector.getTermFreqMap(), alpha);
		Map<String, Double> expandedQueryTerms = combine(queryTerms, docsTerms);
		Map<String, Double> topexpandedQueryTerms = new HashMap<String, Double>();
		expandedQueryTerms.entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed())
				.limit(maxExpandedQueryTerms)
				.forEachOrdered(item -> topexpandedQueryTerms.put(item.getKey(), item.getValue()));
		StringBuffer expandedQuery = new StringBuffer(queryStr);
		for (String t : topexpandedQueryTerms.keySet()) {
			expandedQuery.append(" " + t);
		}
		return expandedQuery.toString();
	}

	/**
	 * Extracts terms of the documents; Adds them to vector in the same order
	 *
	 */
	public List<Map<String, Integer>> getDocsTerms(ScoreDoc[] hits, int docsRelevantCount, Analyzer analyzer)
			throws IOException {
		List<Map<String, Integer>> docsTerms = new ArrayList<>();
		for (int i = 0; ((i < docsRelevantCount) && (i < hits.length)); i++) {
			Map<String, Integer> freqMap = new HashMap<>();
			Document doc = this.searcher.doc(hits[i].doc);
			String content = doc.get("content");
			QueryTermVector vec = new QueryTermVector(content, this.analyzer);
			freqMap = vec.getTermFreqMap();
			docsTerms.add(freqMap);
		}
		return docsTerms;
	}

	/**
	 * Sets boost of terms based on alpha and beta parameters.
	 *
	 */
	public Map<String, Double> setBoost(Map<String, Integer> termVector, float factor) throws IOException {
		List<Map<String, Integer>> v = new ArrayList<Map<String, Integer>>();
		v.add(termVector);
		return setBoost(v, factor);
	}

	/**
	 * Sets boost of terms based on alpha and beta parameters.
	 *
	 */
	public Map<String, Double> setBoost(List<Map<String, Integer>> docsTerms, float factor) throws IOException {
		Map<String, Double> termWeights = new HashMap<>();
		for (int g = 0; g < docsTerms.size(); g++) {
			Map<String, Integer> docTerms = docsTerms.get(g);
			for (Map.Entry<String, Integer> entryset : docTerms.entrySet()) {
				String term = entryset.getKey();
				float tf = entryset.getValue();
				double idf = (double) (Math.log((docsTerms.size() + 1) / (double) (tf + 1)) + 1.0);
				double weight = tf * idf;
				if (termWeights.containsKey(term)) {
					double w = termWeights.get(term);
					termWeights.put(term, (factor * weight) + w);
				} else {
					termWeights.put(term, (factor * weight));
				}

			}
		}
		return termWeights;
	}

	/**
	 * combine weights according to expansion formula
	 */
	public Map<String, Double> combine(Map<String, Double> queryTerms, Map<String, Double> docsTerms) {
		Map<String, Double> terms = new HashMap<>();
		terms.putAll(docsTerms);
		for (Map.Entry<String, Double> entryset : queryTerms.entrySet()) {
			String key = entryset.getKey();
			if (terms.containsKey(key)) {
				double docweight = terms.get(key);
				double qweight = entryset.getValue();
				terms.put(key, (qweight + docweight));
			} else {
				terms.put(key, entryset.getValue());
			}
		}
		return terms;
	}

}
