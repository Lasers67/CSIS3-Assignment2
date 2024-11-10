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

// Inner class to hold similarity score and result string
class Result {
    float similarity;
    String resultString;

    Result(float similarity, String resultString) {
        this.similarity = similarity;
        this.resultString = resultString;
    }
}


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
var builder = HnswGraphBuilder.create(ravv, VectorEncoding.FLOAT32, similarityFunction, 500, 500, new Random().nextInt());
var hnsw = builder.build(ravv.copy());
var nn = HnswGraphSearcher.search(queryVector, k, ravv.copy(), VectorEncoding.FLOAT32, similarityFunction, hnsw, null, Integer.MAX_VALUE);

// List to store results with similarity scores
List<Result> temp = new ArrayList<>();

for (var i : nn.nodes()) {
    var neighbor = universe.get(i);
    var similarity = similarityFunction.compare(queryVector, neighbor);
    
    // Create a Result object with similarity and formatted result string
    temp.add(new Result(similarity, num + " Q0 " + (i + 1) + " PLACEHOLDER_RANK " + similarity + " STANDARD"));
}

// Sort by similarity in descending order
temp.sort((a, b) -> Float.compare(b.similarity, a.similarity));

// Add sorted results to resultsFile with rank
int rank = 1;
for (var result : temp) {
    // Replace PLACEHOLDER_RANK with the actual rank
    String rankedResult = result.resultString.replace("PLACEHOLDER_RANK", String.valueOf(rank));
    resultsFile.add(rankedResult);
    rank++;
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
