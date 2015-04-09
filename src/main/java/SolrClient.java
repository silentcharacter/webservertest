import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Ilya.Igolnikov on 09.04.2015.
 */
public class SolrClient {

    public static void main(String[] args) throws IOException, SolrServerException {
        // create the SolrJ client
        HttpSolrServer client = new HttpSolrServer("http://localhost:8983/solr/master_tn_Product");

        List<SolrInputDocument> docs = new ArrayList<>();
        // create the document
        SolrInputDocument sdoc = new SolrInputDocument();
        sdoc.addField("id","book1");

//        Map<String,Object> fieldModifier = new HashMap<>(1);
//        fieldModifier.put("add", "тестовый");
//        sdoc.addField("cat_string_mv", fieldModifier);  // add the map as the field value

        Map<String,Object> fieldModifier = new HashMap<>(1);
        fieldModifier.put("set", "sdfsdf");
        sdoc.addField("author_string", fieldModifier);  // add the map as the field value

        Map<String,Object> fieldModifier1 = new HashMap<>(1);
        fieldModifier1.put("inc", "1");
        sdoc.addField("copies_double", fieldModifier1);  // add the map as the field value

//        sdoc.removeField("cat_string");
        docs.add(sdoc);

        SolrInputDocument sdoc1 = new SolrInputDocument();
        sdoc1.addField("id","book2");
        sdoc1.addField("test_string", "test");
        docs.add(sdoc1);

        client.add(docs);

        client.commit();
        client.shutdown();  // shutdown client before we exit
    }
}
