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
import java.util.Random;
import org.apache.lucene.util.hnsw.HnswGraphBuilder;
import org.apache.lucene.util.hnsw.HnswGraphSearcher;


public class HNSWExample {
    private static final int VECTOR_DIMENSION = 1536;
    private Directory directory;
    private static final VectorSimilarityFunction similarityFunction = VectorSimilarityFunction.COSINE;

    public HNSWExample() throws Exception {
        directory = FSDirectory.open(Paths.get("./index/"));
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
            return null;
        }

        return rows.toArray(new float[rows.size()][]);
    }

    public void search(float[][] universe, float[] queryVector, int k) throws Exception {
        int universeSize = universe.length;
        List<float[]> universeList = new ArrayList<>(List.of(universe));

        System.out.println("Constructing HNSW graph...");
        // Ensure HnswGraphBuilder and HnswGraphSearcher are properly defined or imported.
        var ravv = new RandomAccessVectorValues(universe, VECTOR_DIMENSION);
        var builder = HnswGraphBuilder.create(ravv, VectorEncoding.FLOAT32, similarityFunction, 16, 100, new Random().nextInt());
        var hnsw = builder.build(universeList);

        System.out.println("Searching for top 10 neighbors of a random vector");
        var nn = HnswGraphSearcher.search(queryVector, k, universeList, VectorEncoding.FLOAT32, similarityFunction, hnsw, null, Integer.MAX_VALUE);
        
        // Placeholder for loop over nearest neighbors:
        for (var i : nn.nodes()) {
             var neighbor = universe[i];
             var similarity = similarityFunction.compare(queryVector, neighbor);
             System.out.printf("  ordinal %d (similarity: %s)%n", i, similarity);
         }
    }

    public static void main(String[] args) throws Exception {
        HNSWExample example = new HNSWExample();
        String indexFileName = "../index.txt";
        String queryFileName = "../query.txt";
        float[][] A = read2DFloatArrayFromFile(indexFileName);

        float[][] queryVectors = read2DFloatArrayFromFile(queryFileName);
        for (float[] query : queryVectors) {
            example.search(A, query, 5);
        }
    }
}
