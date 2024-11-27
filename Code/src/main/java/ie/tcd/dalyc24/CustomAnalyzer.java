package ie.tcd.dalyc24;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.synonym.SynonymGraphFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.util.CharsRef;

import static org.apache.lucene.analysis.en.EnglishAnalyzer.ENGLISH_STOP_WORDS_SET;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CustomAnalyzer extends StopwordAnalyzerBase {

    public CustomAnalyzer() {
    };

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final Tokenizer source = new StandardTokenizer();
        TokenStream result = new EnglishPossessiveFilter(source);
        result = new LowerCaseFilter(result);
        result = new StopFilter(result, ENGLISH_STOP_WORDS_SET);
        result = new PorterStemFilter(result);
        try {
            result = new SynonymGraphFilter(result, synonyms(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new TokenStreamComponents(source, result);
    }

    public static SynonymMap synonyms() throws IOException {
        SynonymMap.Builder builder = new SynonymMap.Builder(true);
        try (BufferedReader reader = new BufferedReader(new FileReader("../Code/synonymsList.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] synonymsList = line.split(",");
                for (int i = 1; i < synonymsList.length; i++) {
                    builder.add(new CharsRef(synonymsList[0]), new CharsRef(synonymsList[i]), true);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder.build();

    }
}
