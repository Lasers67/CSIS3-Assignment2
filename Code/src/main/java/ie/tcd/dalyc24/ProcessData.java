package ie.tcd.dalyc24;

import java.io.IOException;

import java.nio.file.Paths;

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
    private static String QUERY_DIRECTORY = "../cran.qry";
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
    public static List<Map<String, String>> readcran_queries()
    {
	List<Map<String, String>> cranQueryList = new ArrayList<Map<String, String>>();
	BufferedReader reader;
        String current_head="";
        String index = "";
        String text = "";
	int queryNumber = 0;
	int counter = 0;
	try{
		//Initialize the reader
		reader = new BufferedReader(new FileReader(QUERY_DIRECTORY));
                String line = reader.readLine();
		Map<String,String> SingleQuery = new HashMap<String,String>();
                while(line!=null)
		{
			String words[] = line.split("\\s+");
                        switch(words[0])
			{
				case ".I":
					//if 1st word is .I it means index
					if(counter>0)
                                        {
						SingleQuery.put("id",index);
						SingleQuery.put("query_no",String.valueOf(++queryNumber));
						SingleQuery.put("text",text);
						cranQueryList.add(SingleQuery);
						SingleQuery = new HashMap<String,String>();
                                                index = "";
                                                text = "";
                                        }
                                        index = words[1];
					break;
				case ".W":
					current_head = words[0];
					break;
				default:
					switch(current_head)
                                        {
                                                case ".W":
                                                        text = text + line + " ";
                                        }
					break;
			}
			counter++;
			line = reader.readLine();
		}
		//also append query number
                SingleQuery.put("id",index);
                SingleQuery.put("query_no",String.valueOf(++queryNumber));
                SingleQuery.put("text",text);
                cranQueryList.add(SingleQuery);

	}
	catch(IOException e)
	{
		e.printStackTrace();
		System.exit(1);
	}
	return cranQueryList;
    }
    public static ArrayList<Document> readFiles_Dataset()
    {
        ArrayList<Document> res;
        String[] DATA_FOLDERS = {"latimes/","ft/","fr94/","fbis/"};
        //latimes
        File directory = new File("../Data/latimes/");
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    if (file.isFile()) {
                        if(file.getName()!="readchg.txt" && file.getName()!="readmela.txt")
                            res.addAll(readFiles_Dataset_File("../Data/latimes/" + file.getName()););
                    }
                }
            } else {
                System.out.println("The directory latimes is empty.");
            }
        } else {
            System.out.println("The specified path latimes is not a directory.");
        }
        //fbis
        File directory = new File("../Data/fbis/");
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    if (file.isFile()) {
                        if(file.getName()!="readchg.txt" && file.getName()!="readmela.txt")
                            res.addAll(readFiles_Dataset_File("../Data/fbis/" + file.getName()));
                    }
                }
            } else {
                System.out.println("The directory fbis is empty.");
            }
        } else {
            System.out.println("The specified path is not a directory. fbis");
        }
        //fr94
        File directory = new File("../Data/fr94/");
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        File new_Files = new File("../Data/fr94/" + file.getName());
                        File[] files_new = new_Files.listFiles();
                        for(File file_new: files_new)
                        {
                            if(file.isFile())
                            {
                                res.addAll(readFiles_Dataset_File("../Data/fr94/" + file.getName() + "/" + file_new.getName()));
                            }
                        }
                    }
                }
            } else {
                System.out.println("The directory fbis is empty.");
            }
        } else {
            System.out.println("The specified path is not a directory. fbis");
        }
        //ft
        File directory = new File("../Data/ft/");
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        File new_Files = new File("../Data/ft/" + file.getName());
                        File[] files_new = new_Files.listFiles();
                        for(File file_new: files_new)
                        {
                            if(file.isFile())
                            {
                                res.addAll(readFiles_Dataset_File("../Data/ft/" + file.getName() + "/" + file_new.getName()));
                            }
                        }
                    }
                }
            } else {
                System.out.println("The directory ft is empty.");
            }
        } else {
            System.out.println("The specified path is not a directory. ft");
        }
        return res;
    }
    public static void main(String[] args) throws IOException
    {
	ArrayList<Document> d = readFiles_Dataset_File("../Data/latimes/la123190");
	for (Document doc : d) {
            System.out.println("Document ID: " + doc.get("documentID"));
            System.out.println("Headline: " + doc.get("headline"));
            System.out.println("Text: " + doc.get("text"));
            System.out.println();
        }
	System.out.println("Run Data Pre-processing");
    }
}
