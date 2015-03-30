package client;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Created by Ilya.Igolnikov on 29.03.2015.
 */
public class Client {

    private static final String SERVER_URL = "http://localhost:8080";

    public static void main(String[] args) {

        for (int i = 0; i < 130; i++) {
            final int n = i;
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {

                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    HttpClient httpClient = HttpClientBuilder.create().build();
                    try {
                        HttpPost request = new HttpPost(SERVER_URL);

                        JSONObject req = new JSONObject();
                        req.put("a", 3);
                        req.put("b", 2);
                        StringEntity params = new StringEntity(req.toString());
                        request.addHeader("content-type", "application/json");
                        request.setEntity(params);
                        System.out.println("" + n + ". Sending request");
                        HttpResponse response = httpClient.execute(request);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                        String json = reader.readLine();
                        JSONTokener tokener = new JSONTokener(json);
                        JSONObject finalResult = new JSONObject(tokener);
                        System.out.println("" + n + ". " + finalResult.toString());

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    } finally {
                        httpClient.getConnectionManager().shutdown();
                    }
                }
            });
            //        thread.setDaemon(true);
            thread.setName("client thread " + i);
            thread.start();
        }
    }
}
