package ie.tcd.dalyc24;

import java.io.IOException;

import java.nio.file.Paths;
import java.io.File;
import java.util.*;
import java.util.regex.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
// import org.apache.lucene.store.RAMDirectory;
 
public class ProcessData
{
    
    // Directories
    private static String DATA_DIRECTORY = "../Data/";
    private static String QUERY_DIRECTORY = "./topics";
    public static ArrayList<Document> readFiles_Dataset_File(String filePath) {
        ArrayList<Document> documents = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            String documentID = null, headline = null, text = null, byLine=null;
            boolean inText = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.startsWith("<DOCNO>")) {
                    documentID = line.replace("<DOCNO>", "").replace("</DOCNO>", "").trim();
                } else if (line.startsWith("<HEADLINE>")) {
                    StringBuilder headlineBuilder = new StringBuilder();
                    boolean inHeadline = true;
                    while (inHeadline && (line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.startsWith("</HEADLINE>")) {
                            inHeadline = false;
                        } else {
                            headlineBuilder.append(line.replace("<P>", "").replace("</P>", "")).append(" ");
                        }
                    }
                    headline = headlineBuilder.toString().trim();
                }
                else if(line.startsWith("<BYLINE>")){
                    StringBuilder byLineBuilder = new StringBuilder();
                    boolean inbyLine = true;
                    while (inbyLine && (line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.startsWith("</BYLINE>")) {
                            inbyLine = false;
                        } else {
                            byLineBuilder.append(line.replace("<P>", "").replace("</P>", "")).append(" ");
                        }
                    }
                    byLine = byLineBuilder.toString().trim();
                } 
                else if (line.startsWith("<TEXT>")) {
                    StringBuilder textBuilder = new StringBuilder();
                    inText = true;
                    while (inText && (line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.startsWith("</TEXT>")) {
                            inText = false;
                        } else {
                            textBuilder.append(line.replace("<P>", "").replace("</P>", "")).append(" ");
                        }
                    }
                    text = textBuilder.toString().trim();
                } else if (line.startsWith("</DOC>")) {
                    Document doc = new Document();
                    if (documentID != null) {
                        doc.add(new StringField("documentID", documentID, Field.Store.YES));
                    }
                    if (headline != null) {
                        doc.add(new StringField("headline", headline, Field.Store.YES));
                    }
                    if (text != null) {
                        doc.add(new TextField("text", text, Field.Store.YES));
                    }
                    if (byLine !=null)
                    {
                        doc.add(new StringField("byLine", byLine, Field.Store.YES));
                    }
                    documents.add(doc);
                    documentID = headline = text = null;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return documents;
    }
    //function to read the query's after pre processing
    public static List<Map<String, String>> readQueries() {
        List<Map<String, String>> queryList = new ArrayList<>();
        String index = "", title = "", description = "", narrative = "";
        Pattern titlePattern = Pattern.compile("<title>\\s*(.*)");
        int description_start = -1;
        int narrative_start = -1;
        int queryNumber = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(QUERY_DIRECTORY))) {
            String line;
            Map<String, String> singleQuery = new HashMap<>();

            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();

                if (trimmedLine.isEmpty()) continue; // Skip empty lines

                if (trimmedLine.startsWith("<top>")) {
                    // Initialize a new query map
                    singleQuery = new HashMap<>();
                } else if (trimmedLine.startsWith("<num>")) {
                    index = trimmedLine.substring(trimmedLine.indexOf(':') + 1).trim();
                } else if (trimmedLine.startsWith("<title>")) {
                    Matcher matcher = titlePattern.matcher(line);
                    if (matcher.find()) {
                        title = matcher.group(1).trim();
                    }
                } else if (trimmedLine.startsWith("<desc>")) {
                    description_start = 0;
                    description = ""; // Start fresh for multi-line description
                } else if (trimmedLine.startsWith("<narr>")) {
                    description_start = -1;
                    narrative = ""; // Start fresh for multi-line narrative
                    narrative_start = 0;
                } else if (trimmedLine.startsWith("</top>")) {
                    // Store the completed query into the list
                    singleQuery.put("id", index);
                    singleQuery.put("query_no", String.valueOf(++queryNumber));
                    singleQuery.put("title", title);
                    singleQuery.put("description", description.trim());
                    singleQuery.put("narrative", narrative.trim());
                    //System.out.println(String.valueOf(queryNumber));
                    //System.out.println(index);
                    //System.out.println(title);
                    //System.out.println(description);
                    //System.out.println(narrative);
                    queryList.add(singleQuery);

                    // Reset variables for the next query
                    index = "";
                    title = "";
                    description = "";
                    narrative = "";
                    narrative_start=-1;
                } else {
                    // Append to description or narrative if inside respective sections
                    if (description_start==0) {
                        description += " " + trimmedLine;
                    } else if (narrative_start==0) {
                        narrative += " " + trimmedLine;
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return queryList;
    }
    public static void main(String[] args) throws IOException
    {
	System.out.println("Run Data Pre-processing");
    }
}
