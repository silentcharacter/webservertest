import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ilya.Igolnikov on 09.04.2015.
 */
public class SolrClient {

    public static void main(String[] args) throws IOException, SolrServerException {
        // create the SolrJ client
        HttpSolrServer client = new HttpSolrServer("http://localhost:8983/solr/master_tn_Product");

        // create the document
        SolrInputDocument sdoc = new SolrInputDocument();
        sdoc.addField("id","book1");

//        Map<String,Object> fieldModifier = new HashMap<>(1);
//        fieldModifier.put("add", "тестовый");
//        sdoc.addField("cat_string_mv", fieldModifier);  // add the map as the field value

        Map<String,Object> fieldModifier = new HashMap<>(1);
        fieldModifier.put("set", "sdfsdf");
        sdoc.addField("author_string", fieldModifier);  // add the map as the field value

//        sdoc.removeField("cat_string");

        client.add( sdoc );  // send it to the solr server
        client.commit();
        client.shutdown();  // shutdown client before we exit
    }
}
