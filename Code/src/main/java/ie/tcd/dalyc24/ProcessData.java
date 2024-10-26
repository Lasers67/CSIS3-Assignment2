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
    private static String DATA_DIRECTORY = "../cran.all.1400";
    private static String QUERY_DIRECTORY = "../cran.qry";
    public static ArrayList<Document> readFiles_Cran_Dataset()
    {
	ArrayList<Document> documents_cran = new ArrayList<Document>();
        BufferedReader reader;
	String current_head="";
	String index = "";
	String text = "";
	String title = "";
	String author = "";
	int counter = 0;
	try{
		reader = new BufferedReader(new FileReader(DATA_DIRECTORY));
		String line = reader.readLine();
		while(line!=null)
		{
			String words[] = line.split("\\s+");
			switch(words[0])
			{
				case ".I":
					//if the first word is .I it means second word is index
					if(counter>0)
					{
						//create document if not the 1st line
						Document document = new Document();
                				document.add(new StringField("id", index, Field.Store.YES));
                				document.add(new TextField("title", title, Field.Store.YES));
                				document.add(new TextField("author", author, Field.Store.YES));
                				document.add(new TextField("text", text, Field.Store.YES));
                				documents_cran.add(document);
						index = "";
						text = "";
						title = "";
						author = "";
					}
					index = words[1];
					break;
				case ".T":
				case ".A":
				case ".W":
				case ".B":
					//for all others, change head
					current_head = words[0];
					break;
				default:
					switch(current_head)
					{
						case ".T":
							title=title + line + " ";
						       break;
						case ".A":
							author= author + line + " ";
					 		break;
						case ".W":
							text = text + line + " ";
					}		
			}
			counter++;
			line = reader.readLine();
		}
		//create document for the last set
		Document document = new Document();
        	document.add(new StringField("id", index, Field.Store.YES));
        	document.add(new TextField("title", title, Field.Store.YES));
        	document.add(new TextField("authors", author, Field.Store.YES));
        	document.add(new TextField("text", text, Field.Store.YES));
		documents_cran.add(document);
		reader.close();

	}
	catch (IOException e)
	{
		e.printStackTrace();
		System.exit(1);
	}
	return documents_cran;
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
    public static void main(String[] args) throws IOException
    {
	ArrayList<Document> d = readFiles_Cran_Dataset();
	List<Map<String,String>> cranQueries = readcran_queries();
	System.out.println("Run Data Pre-processing");
    }
}
