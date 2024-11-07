package ie.tcd.dalyc24;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.KnnVectorField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.KnnVectorQuery;
import org.apache.lucene.store.Directory;
//import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.ScoreDoc;
import java.util.List;
import org.apache.lucene.store.FSDirectory;           // For FSDirectory
import java.nio.file.Paths;                           // For Paths
import java.util.ArrayList;                           // For ArrayList
import java.io.BufferedReader;                        // For BufferedReader
import java.io.FileReader;                            // For FileReader
import java.io.IOException;                           // For IOException
import org.apache.lucene.index.VectorSimilarityFunction;
public class HNSWExample {
    private static final int VECTOR_DIMENSION = 1536;
    private Directory directory;
    private IndexWriter indexWriter;
    
    public HNSWExample() throws Exception {
        directory = FSDirectory.open(Paths.get("./index/"));
        indexWriter = new IndexWriter(directory, new IndexWriterConfig(new StandardAnalyzer()));
    }

    // Method to index documents with vector fields
    public void addDocument(float[] vector) throws Exception {
        Document doc = new Document();
        doc.add(new KnnVectorField("embedding", vector, VectorSimilarityFunction.COSINE));
        indexWriter.addDocument(doc);
    }

    public void close() throws Exception {
        indexWriter.close();
    }


public static float[][] read2DFloatArrayFromFile(String fileName) {
        List<float[]> rows = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] numbers = line.split(" ");
                float[] row = new float[numbers.length];
                
                for (int i = 0; i < numbers.length; i++) {
                    row[i] = Float.parseFloat(numbers[i]);
                }
                rows.add(row);
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;  // Return null if there was an error reading the file
        }

        // Convert List to a 2D float array
        return rows.toArray(new float[rows.size()][]);
    }


public void search(float[] queryVector, int k) throws Exception {
    DirectoryReader reader = DirectoryReader.open(directory);
    IndexSearcher searcher = new IndexSearcher(reader);
    KnnVectorQuery query = new KnnVectorQuery("embedding", queryVector, k);
    
    // Perform the search
    TopDocs results = searcher.search(query, k);
    for (ScoreDoc scoreDoc : results.scoreDocs) {
        Document doc = searcher.doc(scoreDoc.doc);
        System.out.println("Doc ID: " + scoreDoc.doc + ", Score: " + scoreDoc.score);
    }
    reader.close();
}

public static void main(String[] args) throws Exception {
    HNSWExample example = new HNSWExample();
    String IndexfileName = "index.txt";
    String queryfileName = "query.txt";
    float[][] A = read2DFloatArrayFromFile(IndexfileName);
    // Assuming float[][] A contains all embeddings
    for (float[] embedding : A) {
        example.addDocument(embedding);
    }
    example.close();
    //get queryVectors
    // Perform search with a query vector
    float[][] queryVectors = read2DFloatArrayFromFile(queryfileName);
    for(float[] query : queryVectors)
    {
        example.search(query, 5); 
    }
}
}
