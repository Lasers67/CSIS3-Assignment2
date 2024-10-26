package ie.tcd.dalyc24;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;

import java.io.File;
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
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
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
import org.apache.lucene.search.similarities.Distribution;
import org.apache.lucene.search.similarities.Lambda;
import org.apache.lucene.search.similarities.DistributionLL;
import org.apache.lucene.search.similarities.LambdaDF;
import org.apache.lucene.search.similarities.NormalizationH1;

// Axiomatic Similarity
import org.apache.lucene.search.similarities.AxiomaticF2LOG;


public class LuceneSearch {
	public static void main(String[] args) throws IOException {
        	ProcessData readFile = new ProcessData();
        	List<Map<String, String>> queryList = readFile.readcran_queries();
        	LuceneSearch searcher = new LuceneSearch();
		System.out.println(args[0] + " " + args[1]);
        	searcher.searchQueriesInData(args[0], args[1],queryList);
    	}
	public static void searchQueriesInData(String analyzer_name, String similarity_strat,List<Map<String,String>> queryList)
	{
		try{
			Analyzer analyzer = null;
			//use same analyzer as Create Index class
			switch(analyzer_name)
        		{
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
			//load indexs
			Directory directory = FSDirectory.open(Paths.get("./index"));
			DirectoryReader ireader = DirectoryReader.open(directory);
			IndexSearcher isearcher = new IndexSearcher(ireader);
			//depending on the sys arg choose similarity score
			switch(similarity_strat)
			{
				case "classic":
					isearcher.setSimilarity(new ClassicSimilarity());
					break;
				case "bm25":
					isearcher.setSimilarity(new BM25Similarity(1.9f,1.0f));
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
			List<String> resultsFile = new ArrayList<String>();
			for(int i=0;i<queryList.size();i++)
			{
				Map<String,String> query_collection = queryList.get(i);
				MultiFieldQueryParser queryParser = new MultiFieldQueryParser(
                        	new String[]{"title","author", "text"},
                        	analyzer);
				//add query's with ? as first letter
				queryParser.setAllowLeadingWildcard(true);
				Query query = queryParser.parse(query_collection.get("text"));
				ScoreDoc[] hits = isearcher.search(query, 1000).scoreDocs;
				for(int j=0;j<hits.length;j++)
				{
					Document hitDoc = isearcher.doc(hits[j].doc);
					resultsFile.add(query_collection.get("query_no")+ " Q0 "+ hitDoc.get("id") + " 0 " + hits[j].score + " STANDARD");
				}
			}
			Files.write(Paths.get("./results.txt"),resultsFile,Charset.forName("UTF-8"));

		}
		catch(IOException | ParseException e)
		{
			e.printStackTrace();
			System.exit(1);
		}

	}
}
