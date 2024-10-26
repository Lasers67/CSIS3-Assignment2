package ie.tcd.dalyc24;

import java.io.IOException;
import java.io.File;
import org.apache.commons.io.FileUtils;
import java.nio.file.Paths;
import java.util.ArrayList;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.search.similarities.BM25Similarity;
// import org.apache.lucene.store.RAMDirectory;
 
public class CreateIndex
{
    
    // index Directory
    private static String INDEX_DIRECTORY = "./index/";

    public static void createCranIndex(String analyzer_name, ArrayList<Document> readFiles_Cran_Dataset)
    {
	Analyzer analyzer = null;
	System.out.println(analyzer_name + " For Index");
	//depending on the sys arg, choose the analyzer
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
			System.out.println("default analyzer\n");
			break;

	}
	try{
		Directory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND); //mode as CREATE_OR_APPEND
		IndexWriter iwriter = new IndexWriter(directory, config);
		for (Document doc : readFiles_Cran_Dataset) {
                	iwriter.addDocument(doc); //add the documents after pre-processing
           	}
		iwriter.close();
		directory.close();
	}
	catch(IOException e)
	{
		e.printStackTrace();
	}
    }
    public static void deleteprevDir()
    {
	try{
            File f = new File(INDEX_DIRECTORY);
            FileUtils.cleanDirectory(f); //clean directory
            FileUtils.forceDelete(f); //delete directory
            FileUtils.forceMkdir(f); //create directory
	}
	catch(IOException e)
	{
	    e.printStackTrace();
	}
    }
    public static void main(String[] args) throws IOException
    {
	//read the files and create list of documents
	ProcessData proc = new ProcessData();
	ArrayList<Document> list_of_documents = proc.readFiles_Cran_Dataset();
	//delete the previous directory
	deleteprevDir();
	//create Index
	createCranIndex(args[0],list_of_documents);
    }
}
