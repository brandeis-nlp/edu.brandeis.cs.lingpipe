package edu.brandeis.cs.lingpipe;

import org.apache.commons.io.IOUtils;
import org.lappsgrid.api.WebService;
import org.lappsgrid.discriminator.Discriminators;
import edu.brandeis.cs.json.JsonProxy;
import edu.brandeis.cs.json2json.Json2Json;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.lappsgrid.api.WebService;
import org.lappsgrid.discriminator.Discriminators;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by lapps on 5/5/2015.
 */
public class LingPipeWS implements WebService {
    public String execute(String s) {
        return null;
    }

    public String getMetadata() {
        // get caller name using reflection
        String name = this.getClass().getName();
        //
        String resName = "/metadata/"+ name +".json";
        System.out.println("load resources:" + resName);
        try {
            String meta = IOUtils.toString(this.getClass().getResourceAsStream(resName));
            JsonProxy.JsonObject json = JsonProxy.newObject();
            json.put("discriminator", Discriminators.Uri.META);
            json.put("payload", JsonProxy.newObject().read(meta));
            System.out.println("---------------------META:-------------------\n" + json.toString());
            return json.toString();
        }catch (Throwable th) {
            JsonProxy.JsonObject json = JsonProxy.newObject();
            json.put("discriminator", Discriminators.Uri.ERROR);
            JsonProxy.JsonObject error = JsonProxy.newObject();
            error.put("class", name);
            error.put("error", "NOT EXIST: " + resName);
            error.put("message", th.getMessage());
            StringWriter sw = new StringWriter();
            th.printStackTrace( new PrintWriter(sw));
            System.err.println(sw.toString());
            error.put("stacktrace", sw.toString());
            json.put("payload", error);
            return json.toString();
        }
    }
}
