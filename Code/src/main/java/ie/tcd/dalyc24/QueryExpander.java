package ie.tcd.dalyc24;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.io.UnsupportedEncodingException;

public class QueryExpander {

    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=AIzaSyC1hFQ7BP3SGNkiTAO5v_dpquKcFudrS-4";

    // function returns List<Map<String, String>> so that it is compatible with querying index code already written
    public List<Map<String, String>> expandQueries(List<Map<String, String>> queryList) {
        List<Map<String, String>> expandedQueryList = new ArrayList<>(); 
        Map<String, String> expandedQuery = new HashMap<>();
        int requests = 0, queryNo = 0; // counters for API requests and queries
        long startTime = System.currentTimeMillis();

        for (Map<String, String> query : queryList) {
            queryNo += 1;
            String formattedQuery = formatForExpansion(query);
            expandedQuery = new HashMap<>();
            expandedQuery.put("id", query.get("id"));
            expandedQuery.put("description", expandSingleQuery(formattedQuery));
            if (expandedQuery.get("description") == null) { // if we get a 400 response from api, just use query description 
                expandedQuery.put("description", query.get("description"));
                expandedQueryList.add(expandedQuery);
                System.out.println("Bad response from API, using query description: " + query.get("description"));
            } else {
                // TODO : fix bug where same token is added hundreds of time to expanded query - may have been fixed by changing prompt
                expandedQueryList.add(expandedQuery);
                System.out.println("Expanded query " + expandedQuery.get("id") + " is as follows: " + expandedQuery);
                System.out.println(" ");
            }
            requests += 1;

            // gemini api only allows 15 requests per minute for free version (and only 1500 per day)
            if (requests >= 15) {
                requests = 0;
                long time = System.currentTimeMillis() - startTime;
                if (time < 60000) {
                    try {
                    Thread.sleep(60000 - time); 
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); 
                        e.printStackTrace();
                    }
                }
                startTime = System.currentTimeMillis();
            }
        }
        return expandedQueryList;
    }

    public String expandSingleQuery(String query) {
        String jsonInputString = "{\"contents\":[{\"parts\":[{\"text\":\" Based on the following title, description and narrative please create a query by generating additional keywords and terms that are closely related. Include synonyms, broader concepts, industry-specific terminology, and any relevant document-specific keywords that would capture the context effectively. Aim to add terms that improve retrieval accuracy by reflecting similar words, relevant phrases, and contextually relevant keywords. Give it to me just as a string of 40 tokens I should use: "
                                     + query + "\"}]}]}";
        System.out.println(jsonInputString);
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }   

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            return processResponse(response.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } 
        return null;
    }

    public String processResponse(String jsonResponse) {
        JSONObject jsonObject = new JSONObject(jsonResponse);
        JSONArray candidates = jsonObject.getJSONArray("candidates");
        
        if (candidates.length() > 0) {
            JSONObject object = candidates.getJSONObject(0);
            JSONObject content = object.getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");
            
            if (parts.length() > 0) {
                StringBuilder textBuilder = new StringBuilder();
                for (int i = 0; i < parts.length(); i++) {
                    JSONObject part = parts.getJSONObject(i);
                    textBuilder.append(part.getString("text"));
                }
                return textBuilder.toString();
            }
        }
        return null; 
    }

    public static String formatForExpansion(Map<String, String> query) {
        String title = query.get("title");
        String description = query.get("description");
        String narrative = query.get("narrative");
        return "title: " + title + ", description: " + description + ", narrative: " + narrative;
    }
    

}