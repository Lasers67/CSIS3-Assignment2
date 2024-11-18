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
import java.io.File;
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
    private static final int VECTOR_DIMENSION = 786;
    private Directory directory;
    private static final VectorSimilarityFunction similarityFunction = VectorSimilarityFunction.COSINE;
    private List<String> resultsFile = new ArrayList<>();
    private int num = 1;
    List<float[]> embedding_rows = new ArrayList<>();
    List<String> embedding_rows_filesnames = new ArrayList<>();
    public HNSWExample() throws Exception {
        directory = FSDirectory.open(Paths.get("./index/"));
    }

    public void read2DFloatArrayFromFile(String fileNameEmbedding, String fileNameFileNames ) {

        try (BufferedReader br = new BufferedReader(new FileReader(fileNameEmbedding))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] numbers = line.split(" ");
                float[] row = new float[numbers.length];
                for (int i = 0; i < numbers.length; i++) {
                    row[i] = Float.parseFloat(numbers[i]);
                }
                embedding_rows.add(row);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(fileNameFileNames))) {
            String line;
            while ((line = br.readLine()) != null) {
                String name = line;
                embedding_rows_filesnames.add(name);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
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
        String basePath = "./Embeddings/";
        String[] dirs = {"ft", "fbis", "latimes", "fr94"};
        for (String dir : dirs) {
            File directory = new File(basePath + "/" + dir);
            
            // Ensure the directory exists
            if (!directory.exists() || !directory.isDirectory()) {
                System.out.println("Directory not found: " + directory.getAbsolutePath());
                continue;
            }

            // List all files in the directory
            File[] files = directory.listFiles((d, name) -> name.endsWith(".txt"));

            if (files == null || files.length == 0) {
                System.out.println("No files found in: " + directory.getAbsolutePath());
                continue;
            }

            // Process pairs of embeddings and document files
            for (File file : files) {
                String fileName = file.getName();

                // Check for "-embeddings_" pattern
                if (fileName.contains("-embeddings_")) {
                    String correspondingDocFileName = fileName.replace("-embeddings_", "-documents_");
                    File correspondingDocFile = new File(directory, correspondingDocFileName);

                    // Ensure the corresponding document file exists
                    if (correspondingDocFile.exists()) {
                        System.out.println("Processing pair: " + file.getAbsolutePath() + " and " + correspondingDocFile.getAbsolutePath());
                        read2DFloatArrayFromFile(file.getAbsolutePath(), correspondingDocFile.getAbsolutePath());
                    } else {
                        System.out.println("Corresponding document file not found for: " + fileName);
                    }
                }
            }
        }
        System.out.println("RET");
    }
}
