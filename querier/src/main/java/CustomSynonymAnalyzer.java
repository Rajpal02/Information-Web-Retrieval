import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.FlattenGraphFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.synonym.SynonymGraphFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.synonym.WordnetSynonymParser;
import org.tartarus.snowball.ext.EnglishStemmer;

import utils.StopWordGenerator;

public class CustomSynonymAnalyzer extends StopwordAnalyzerBase {

	private static final StopWordGenerator stopWordGenerator = StopWordGenerator.getInstance();
	public static final CharArraySet ENGLISH_STOP_WORDS_SET = stopWordGenerator.getCharset();
	private final CharArraySet stemExclusionSet;
	private static SynonymMap synonymMap = null;
	private boolean useForIndexing;

	public CustomSynonymAnalyzer() {
		this(ENGLISH_STOP_WORDS_SET);
	}

	public CustomSynonymAnalyzer(CharArraySet stopwords) {
		this(stopwords, CharArraySet.EMPTY_SET);
	}

	private CustomSynonymAnalyzer(CharArraySet stopwords, CharArraySet emptySet) {
		super(stopwords);
		this.stemExclusionSet = CharArraySet.unmodifiableSet(CharArraySet.copy(emptySet));
		synonymMap = generateSynonyms();
	}

	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		final Tokenizer source = new StandardTokenizer();
		TokenStream result = new EnglishPossessiveFilter(source);
		result = new LowerCaseFilter(result);
		result = new StopFilter(result, stopwords);
		if (!stemExclusionSet.isEmpty())
			result = new SetKeywordMarkerFilter(result, stemExclusionSet);
	    result = new SynonymGraphFilter(result, synonymMap, true);
	    if(this.useForIndexing)
	    	result= new FlattenGraphFilter(result);
		result = new SnowballFilter(result,new EnglishStemmer());
		return new TokenStreamComponents(source, result);
	}

	private static SynonymMap generateSynonyms() {
		Reader rulesReader = new InputStreamReader(CustomSynonymAnalyzer.class.getResourceAsStream("wn_s.pl"));
		SynonymMap.Builder parser = null;
		parser = new WordnetSynonymParser(true, true, new StandardAnalyzer());
		try {
			((WordnetSynonymParser) parser).parse(rulesReader);
			synonymMap = parser.build();
		} catch (ParseException e) {
			System.out.println("Error While parsing synonyms");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("I/O Error While loading wordnet synonym file");
			e.printStackTrace();
		}
		return synonymMap;

	}

	public boolean isUseForIndexing() {
		return useForIndexing;
	}

	public void setUseForIndexing(boolean useForIndexing) {
		this.useForIndexing = useForIndexing;
	}
	


}
