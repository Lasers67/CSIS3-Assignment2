package ie.tcd.dalyc24;

import org.apache.lucene.index.VectorEncoding;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.KnnVectorField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.KnnVectorQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.ScoreDoc;
import java.util.List;
import org.apache.lucene.store.FSDirectory;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.apache.lucene.util.hnsw.HnswGraphBuilder;
import org.apache.lucene.util.hnsw.HnswGraphSearcher;
import ie.tcd.dalyc24.ListRandomAccessVectorValues;

public class HNSWExample {
    private static final int VECTOR_DIMENSION = 3072;
    private Directory directory;
    private static final VectorSimilarityFunction similarityFunction = VectorSimilarityFunction.COSINE;
    private List<String> resultsFile = new ArrayList<>();
    private int num = 1;

    public HNSWExample() throws Exception {
        directory = FSDirectory.open(Paths.get("./index/"));
    }

    public static List<float[]> read2DFloatArrayFromFile(String fileName) {
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
            return null;
        }

        return rows;
    }

    public void search(List<float[]> universe, float[] queryVector, int k) throws Exception {
        System.out.printf("RUNNING FOR QUERY " + num + "\n");
        var ravv = new ListRandomAccessVectorValues(universe, VECTOR_DIMENSION);
        var builder = HnswGraphBuilder.create(ravv, VectorEncoding.FLOAT32, similarityFunction, 16, 100, new Random().nextInt());
        var hnsw = builder.build(ravv.copy());
        var nn = HnswGraphSearcher.search(queryVector, k, ravv.copy(), VectorEncoding.FLOAT32, similarityFunction, hnsw, null, Integer.MAX_VALUE);

        // List to store pairs of similarity scores and result strings
        List<Pair<Float, String>> temp = new ArrayList<>();

        for (var i : nn.nodes()) {
            var neighbor = universe.get(i);
            var similarity = similarityFunction.compare(queryVector, neighbor);
            
            // Add a pair of similarity score and result string
            temp.add(new Pair<>(similarity, num + " Q0 " + (i+1) + " 0 " + similarity + " STANDARD"));
        }

        // Sort by similarity in descending order
        temp.sort((a, b) -> Float.compare(b.getKey(), a.getKey()));

        // Add sorted results to resultsFile
        for (var pair : temp) {
            resultsFile.add(pair.getValue());
        }
        num++;
    
    }

    public void writeResultsToFile() throws IOException {
        Files.write(Paths.get("./resultsNEW.txt"), resultsFile, StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws Exception {
        HNSWExample example = new HNSWExample();
        String indexFileName = "../index.txt";
        String queryFileName = "../query.txt";
        List<float[]> A = read2DFloatArrayFromFile(indexFileName);
        List<float[]> queryVectors = read2DFloatArrayFromFile(queryFileName);

        for (float[] query : queryVectors) {
            example.search(A, query, 50);
        }

        example.writeResultsToFile();
    }
}
