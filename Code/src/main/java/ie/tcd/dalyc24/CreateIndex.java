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
	public static void createIndexForEachFile(String analyzer_name) 
	{
		Analyzer analyzer = null;
		System.out.println(analyzer_name + " For Index");
		ProcessData proc = new ProcessData();
		// Choose the analyzer based on the input name
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
				System.out.println("default analyzer\n");
				break;
		}

		String[] DATA_FOLDERS = {"latimes/"};
		Directory indexDirectory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND); // Always create a new index
		IndexWriter iwriter = new IndexWriter(indexDirectory, config);
		// Iterate over each data folder and process files
		for (String folder : DATA_FOLDERS) {
			File directory = new File("../Data/" + folder);
			if (directory.isDirectory()) {
				File[] files = directory.listFiles();
				if (files != null && files.length > 0) {
					for (File file : files) {
						if (file.isFile()) {
							if (!file.getName().startsWith("read")) {
								try {
									System.out.println("Directory:- " + folder + " file:- " + file.getName());
									// Read the document from the file and add it to the index
									ArrayList<Document> documents = proc.readFiles_Dataset_File(file.getAbsolutePath());
									for (Document doc : documents) {
										iwriter.addDocument(doc);
									}
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						} else if (file.isDirectory()) {
							// Handle nested directories
							File[] nestedFiles = file.listFiles();
								if (nestedFiles != null) {
									for (File nestedFile : nestedFiles) {
										if (nestedFile.isFile()) {
											try {
												System.out.println("Directory:- " + folder + " file:- " + nestedFile.getAbsolutePath());
									// Create a new index for each file

									// Read the document from the file and add it to the index
									ArrayList<Document> documents = proc.readFiles_Dataset_File(nestedFile.getAbsolutePath());
									for (Document doc : documents) {
										iwriter.addDocument(doc);
									}
								} catch (IOException e) {
									e.printStackTrace();
								}
										}
									}
								}
						}
					}
				} else {
					System.out.println("The directory " + folder + " is empty.");
				}
			} else {
				System.out.println("The specified path is not a directory: " + folder);
			}
			iwriter.close(); // Close the writer
			indexDirectory.close(); // Close the directory
		}
	}
    public static void deleteprevDir() {
    try {
        File f = new File(INDEX_DIRECTORY);
        
        // Check if the directory exists
        if (f.exists()) {
            // Clean the directory
            FileUtils.cleanDirectory(f); // clean directory
            FileUtils.forceDelete(f); // delete directory
        }
        
        // Create the directory
        FileUtils.forceMkdir(f); // create directory
    } catch (IOException e) {
        e.printStackTrace();
    }
}
    public static void main(String[] args) throws IOException
    {
	//delete the previous directory
	deleteprevDir();
	//create Index
	createIndexForEachFile(args[0]);
    }
}
