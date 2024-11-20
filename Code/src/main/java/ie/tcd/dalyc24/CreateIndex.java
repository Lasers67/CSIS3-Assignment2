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
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class CreateIndex {

    // Directory where the Lucene index will be stored
    private static final String INDEX_DIRECTORY = "./index/";

    public static void createIndexForEachFile(String analyzerName) {
        Analyzer analyzer = null;
        System.out.println(analyzerName + " For Index");
        ProcessData proc = new ProcessData();

        // Choose the analyzer based on the input name
        switch (analyzerName.toLowerCase()) {
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
                System.out.println("Using default analyzer: StandardAnalyzer");
                break;
        }

        // Define the dataset path
        String DATA_DIRECTORY = "/opt/CSIS3-Assignment2/Data/Assignment Two/";
        String[] DATA_FOLDERS = {"latimes/", "ft/", "fr94/", "fbis/"};

        try {
            // Initialize Lucene's index directory
            Directory indexDirectory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND); // Always create or append to the index
            IndexWriter iwriter = new IndexWriter(indexDirectory, config);

            // Iterate over each data folder and process files
            for (String folder : DATA_FOLDERS) {
                File directory = new File(DATA_DIRECTORY + folder);
                if (directory.isDirectory()) {
                    File[] files = directory.listFiles();
                    if (files != null && files.length > 0) {
                        for (File file : files) {
                            if (file.isFile() && !file.getName().startsWith("read")) {
                                try {
                                    System.out.println("Processing Directory: " + folder + ", File: " + file.getName());
                                    // Read the document from the file and add it to the index
                                    ArrayList<Document> documents = proc.readFiles_Dataset_File(file.getAbsolutePath());
                                    iwriter.addDocuments(documents);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } else {
                        System.out.println("The directory " + folder + " is empty.");
                    }
                } else {
                    System.out.println("The specified path is not a directory: " + folder);
                }
            }

            // Close the writer and directory
            iwriter.close();
            indexDirectory.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deletePreviousIndex() {
        try {
            File indexDir = new File(INDEX_DIRECTORY);

            // Check if the directory exists
            if (indexDir.exists()) {
                // Clean and delete the directory
                FileUtils.cleanDirectory(indexDir); // Clean directory
                FileUtils.forceDelete(indexDir); // Delete directory
            }

            // Recreate the directory
            FileUtils.forceMkdir(indexDir); // Create directory
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        // Delete the previous index directory
        deletePreviousIndex();
        // Create a new index
        createIndexForEachFile(args[0]);
    }
}
