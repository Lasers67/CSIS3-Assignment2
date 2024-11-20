package ie.tcd.dalyc24;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;

import java.io.FileWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

// BM25Similarity
import org.apache.lucene.search.similarities.BM25Similarity;

// ClassicSimilarity (TF-IDF)
import org.apache.lucene.search.similarities.ClassicSimilarity;

// BooleanSimilarity
import org.apache.lucene.search.similarities.BooleanSimilarity;

// Language Model with Dirichlet Smoothing
import org.apache.lucene.search.similarities.LMDirichletSimilarity;

// Language Model with Jelinek-Mercer Smoothing
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;

// Information-Based (IB) Similarity
import org.apache.lucene.search.similarities.IBSimilarity;
import org.apache.lucene.search.similarities.DistributionLL;
import org.apache.lucene.search.similarities.LambdaDF;
import org.apache.lucene.search.similarities.NormalizationH1;

// Axiomatic Similarity
import org.apache.lucene.search.similarities.AxiomaticF2LOG;

public class LuceneSearch {
    public static void main(String[] args) throws IOException {
        ProcessData readFile = new ProcessData();
        List<Map<String, String>> queryList = readFile.readQueries();
        LuceneSearch searcher = new LuceneSearch();
        System.out.println(args[0] + " " + args[1]);
        searcher.searchAndExtractFeatures(args[0], args[1], queryList);
    }

    public static void searchAndExtractFeatures(String analyzer_name, String similarity_strat, List<Map<String, String>> queryList) {
        try {
            Analyzer analyzer = null;
            // Use the same analyzer as the CreateIndex class
            switch (analyzer_name) {
                case "standard":
                    analyzer = new StandardAnalyzer(EnglishAnalyzer.getDefaultStopSet());
                    break;
                case "keyword":
                    analyzer = new KeywordAnalyzer();
                    break;
                case "whitespace":
                    analyzer = new WhitespaceAnalyzer();
                    break;
                case "simple":
                    analyzer = new SimpleAnalyzer();
                    break;
                case "stop":
                    analyzer = new StopAnalyzer();
                    break;
                case "english":
                    analyzer = new EnglishAnalyzer();
                    break;
                default:
                    analyzer = new StandardAnalyzer();
                    break;
            }

            // Load indexes
            Directory directory = FSDirectory.open(Paths.get("./index/"));
            DirectoryReader ireader = DirectoryReader.open(directory);
            IndexSearcher isearcher = new IndexSearcher(ireader);

            // Depending on the system argument, choose the similarity score
            switch (similarity_strat) {
                case "classic":
                    isearcher.setSimilarity(new ClassicSimilarity());
                    break;
                case "bm25":
                    isearcher.setSimilarity(new BM25Similarity(1.9f, 1.0f));
                    break;
                case "boolean":
                    isearcher.setSimilarity(new BooleanSimilarity());
                    break;
                case "lmd":
                    isearcher.setSimilarity(new LMDirichletSimilarity());
                    break;
                case "lmj":
                    isearcher.setSimilarity(new LMJelinekMercerSimilarity(0.7f));
                    break;
                case "ibs":
                    isearcher.setSimilarity(new IBSimilarity(new DistributionLL(), new LambdaDF(), new NormalizationH1()));
                    break;
                case "axiom":
                    isearcher.setSimilarity(new AxiomaticF2LOG());
                    break;
                default:
                    break;
            }

            int queries_to_consider = 25; // queryList.size()
            List<String> resultsFile = new ArrayList<>();

            // Prepare feature file
            FileWriter featureFile = new FileWriter("features.csv");
            featureFile.write("query_id,doc_id,bm25_score,relevance\n");

            for (int i = 0; i < queries_to_consider; i++) {
                Map<String, String> query_collection = queryList.get(i);

                MultiFieldQueryParser queryParser = new MultiFieldQueryParser(
                        new String[]{"headline", "text", "byLine"},
                        analyzer
                );

                // Only using description + narrative for start
                String d = query_collection.get("description");
                String escapedDescription = queryParser.escape(d);
                String n = query_collection.get("narrative");
                String escapedNarrative = queryParser.escape(n);
                Query query = queryParser.parse(escapedDescription + " " + escapedNarrative);

                ScoreDoc[] hits = isearcher.search(query, 1000).scoreDocs;

                for (int j = 0; j < hits.length; j++) {
                    Document hitDoc = isearcher.doc(hits[j].doc);
                    String queryId = query_collection.get("id");
                    String docId = hitDoc.get("documentID");
                    float bm25Score = hits[j].score;

                    // Placeholder for relevance (set to 0 for now)
                    int relevance = 0; // Replace with actual relevance lookup logic if available

                    // Add the feature row
                    featureFile.write(queryId + "," + docId + "," + bm25Score + "," + relevance + "\n");

                    // Also add to results.txt for standard output
                    resultsFile.add(queryId + " Q0 " + docId + " 0 " + bm25Score + " STANDARD");
                }
            }

            featureFile.close();
            Files.write(Paths.get("./results.txt"), resultsFile, Charset.forName("UTF-8"));
            System.out.println("Feature extraction and results file generation completed!");

        } catch (IOException | ParseException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
