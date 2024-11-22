package ie.tcd.dalyc24;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.lucene.index.VectorEncoding;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.hnsw.HnswGraph;
import org.apache.lucene.util.hnsw.HnswGraphBuilder;
import org.apache.lucene.util.hnsw.HnswGraphSearcher;

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
    private static final int VECTOR_DIMENSION = 768;
    private Directory directory;
    private static final VectorSimilarityFunction similarityFunction = VectorSimilarityFunction.COSINE;
    private List<String> resultsFile = new ArrayList<>();
    private int num = 1;
    private static List<float[]> embedding_rows = new ArrayList<>();
    private static List<String> embedding_rows_filesnames = new ArrayList<>();
    private static List<float[]> query_embedding_rows = new ArrayList<>();
    private static List<String> query_embedding_rows_filesnames = new ArrayList<>();
    public HNSWExample() throws Exception {
        directory = FSDirectory.open(Paths.get("./index/"));
    }

    public static void read2DFloatArrayFromFile(String fileNameEmbedding, String fileNameFileNames , boolean query) {
        float[] row = {};
        String name="";
        try (BufferedReader br = new BufferedReader(new FileReader(fileNameEmbedding))) {
            String line;
            System.out.println(fileNameEmbedding);
            while ((line = br.readLine()) != null) {
                String[] numbers = line.split(" ");
                row = new float[numbers.length];
                for (int i = 0; i < numbers.length; i++) {
                    row[i] = Float.parseFloat(numbers[i]);
                }
                if(query)
                query_embedding_rows.add(row);
                else{
                    embedding_rows.add(row);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(fileNameFileNames))) {
            String line;
            while ((line = br.readLine()) != null) {
                name = line;
                if(query)
                query_embedding_rows_filesnames.add(name);
                else
                embedding_rows_filesnames.add(name);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    public void search(ListRandomAccessVectorValues ravv, HnswGraph hnsw, float[] queryVector, int k) throws Exception {
        if(num>25)
            return;
        System.out.printf("RUNNING FOR QUERY " + query_embedding_rows_filesnames.get(num-1) + "\n");
        var nn = HnswGraphSearcher.search(queryVector, k, ravv.copy(), VectorEncoding.FLOAT32, similarityFunction, hnsw, null, Integer.MAX_VALUE);

        // List to store results with similarity scores
        List<Result> temp = new ArrayList<>();

        for (var i : nn.nodes()) {
            var neighbor = embedding_rows.get(i);
            var similarity = similarityFunction.compare(queryVector, neighbor);
            System.out.printf("  ordinal %s (similarity: %s)%n", embedding_rows_filesnames.get(i), similarity);
            // Create a Result object with similarity and formatted result string
            temp.add(new Result(similarity, query_embedding_rows_filesnames.get(num-1) + " Q0 " + embedding_rows_filesnames.get(i) + " PLACEHOLDER_RANK " + similarity + " STANDARD"));
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
        String basePath = "../Embeddings/";
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
                        read2DFloatArrayFromFile(file.getAbsolutePath(), correspondingDocFile.getAbsolutePath(),false);
                    } else {
                        System.out.println("Corresponding document file not found for: " + fileName);
                    }
                }
            }
        }
        basePath = "../";
        String[] dirs2 = {"query"};
        for (String dir : dirs2) {
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
                        read2DFloatArrayFromFile(file.getAbsolutePath(), correspondingDocFile.getAbsolutePath(),true);
                    } else {
                        System.out.println("Corresponding document file not found for: " + fileName);
                    }
                }
            }
        }
        System.out.println(query_embedding_rows.get(0).length + " " + embedding_rows.get(0).length);
        
        var ravv = new ListRandomAccessVectorValues(embedding_rows, VECTOR_DIMENSION);
        var builder = HnswGraphBuilder.create(ravv, VectorEncoding.FLOAT32, similarityFunction, 100, 150, new Random().nextInt());
        var hnsw = builder.build(ravv.copy());
        for (float[] query : query_embedding_rows) {
            example.search(ravv, hnsw, query, 1000);
        }
        example.writeResultsToFile();
    }
}
