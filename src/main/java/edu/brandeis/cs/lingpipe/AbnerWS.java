package edu.brandeis.cs.lingpipe;

import abner.Tagger;
import com.aliasi.chunk.*;
import com.aliasi.util.AbstractExternalizable;

import com.aliasi.util.ScoredObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.lappsgrid.api.WebService;
import org.lappsgrid.discriminator.Discriminators;
import edu.brandeis.cs.json.JsonProxy;
import edu.brandeis.cs.json2json.Json2Json;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;

/**
 * Created by lapps on 5/5/2015.
 */
public class AbnerWS implements WebService {

    static Tagger taggerNLPBA = new Tagger(0);;
    static Tagger taggerBIOCR = new Tagger(1);
    static Tagger tagger = taggerNLPBA;

    public static String getResource(String name) throws IOException{
        java.io.InputStream in =  AbnerWS.class.getClassLoader().getResourceAsStream(name);
        return IOUtils.toString(in);
//        GUI gui = new GUI();
    }

    public String execute(String s) {
        try {
            JsonProxy.JsonObject json = null;
            JsonProxy.JsonObject payload = null;
            String txt = s;
            if (s.trim().startsWith("{")) {
                json = JsonProxy.newObject().read(s);
                String discriminator = json.get("discriminator").toString().trim();
                if (discriminator.equals(Discriminators.Uri.TEXT)) {
                    txt = json.get("payload").toString();
                    json.put("discriminator", Discriminators.Uri.JSON_LD);
                    payload = JsonProxy.newObject().put("text", JsonProxy.newObject().put("@value", txt));
                    json.put("payload", payload);
                } else if (discriminator.equals(Discriminators.Uri.JSON_LD)) {
                    payload = (JsonProxy.JsonObject) json.get("payload");
                    JsonProxy.JsonObject textobj = (JsonProxy.JsonObject) payload.get("text");
                    txt = (String) textobj.get("@value");
                }
            } else {
                json = JsonProxy.newObject();
                json.put("discriminator", Discriminators.Uri.JSON_LD);
                payload = JsonProxy.newObject().put("text", JsonProxy.newObject().put("@value", txt));
                json.put("payload", payload);
            }
            payload.put("@context", "http://vocab.lappsgrid.org/context-1.0.0.jsonld");
            JsonProxy.JsonArray annsobj = JsonProxy.newArray();

            String iob = taggerNLPBA.tagIOB(txt);
            int start = 0;
            int end = 0;
            int id = 0;
//            System.out.println("iob="+iob);
            for (String wordAndTag : iob.split("\\n")) {
                String [] arr = wordAndTag.split("\t");
                String word = arr[0];
                String tag = arr[1];
                start = txt.indexOf(word, end);
                end = start + word.length();
//                System.out.println("word="+word);
//                System.out.println("Tag="+tag);
//                System.out.println("Tag" + tag.equalsIgnoreCase("O"));
                if(! tag.trim().equals("O")) {
                    JsonProxy.JsonObject annobj = JsonProxy.newObject();
                    annobj.put("id","ner"+id++);
                    annobj.put("start", start);
                    annobj.put("end", end);
                    annobj.put("label", "http://vocab.lappsgrid.org/NamedEntity");
                    JsonProxy.JsonObject features = JsonProxy.newObject();
                    features.put("category", tag);
                    features.put("word", word);
                    annobj.put("features", features);
                    annobj.put("label", "http://vocab.lappsgrid.org/NamedEntity");
                    annsobj.add(annobj);
                }
            }

            iob = taggerBIOCR.tagIOB(txt);
            start = 0;
            end = 0;
//            System.out.println("iob="+iob);
            for (String wordAndTag : iob.split("\\n")) {
                String [] arr = wordAndTag.split("\t");
                String word = arr[0];
                String tag = arr[1];
                start = txt.indexOf(word, end);
                end = start + word.length();
//                System.out.println("word="+word);
//                System.out.println("Tag="+tag);
//                System.out.println("Tag" + tag.equalsIgnoreCase("O"));
                if(! tag.trim().equals("O")) {
                    JsonProxy.JsonObject annobj = JsonProxy.newObject();
                    annobj.put("id","ner"+id++);
                    annobj.put("start", start);
                    annobj.put("end", end);
                    annobj.put("label", "http://vocab.lappsgrid.org/NamedEntity");
                    JsonProxy.JsonObject features = JsonProxy.newObject();
                    features.put("category", tag);
                    features.put("word", word);
                    annobj.put("features", features);
                    annobj.put("label", "http://vocab.lappsgrid.org/NamedEntity");
                    annsobj.add(annobj);
                }
            }


            JsonProxy.JsonArray viewsobj = null;
            if (payload.has("views")) {
                viewsobj = (JsonProxy.JsonArray) payload.get("views");
            } else {
                viewsobj = JsonProxy.newArray();
                payload.put("views", viewsobj);
            }
            JsonProxy.JsonObject view = JsonProxy.newObject();
            JsonProxy.JsonObject contains = JsonProxy.newObject();
            contains.put(Discriminators.Uri.NE,JsonProxy.newObject().put("producer", this.getClass().getName() + ": 0.0.1")
                    .put("type", "ner:abner"));
            view.put("metadata", JsonProxy.newObject().put("contains", contains));
            view.put("annotations", annsobj);
            viewsobj.add(view);
            return json.toString();
        }catch (Throwable th) {
            JsonProxy.JsonObject json = JsonProxy.newObject();
            json.put("discriminator", Discriminators.Uri.ERROR);
            JsonProxy.JsonObject error = JsonProxy.newObject();
            error.put("class", this.getClass().getName());
            error.put("message", th.getMessage());
            StringWriter sw = new StringWriter();
            th.printStackTrace( new PrintWriter(sw));
            System.err.println(sw.toString());
            error.put("stacktrace", sw.toString());
            json.put("payload", error);
            return json.toString();
        }
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


    public String getXml(String text) throws Exception {
        System.out.println(tagger.tagSGML(text));
        return tagger.tagABNER(text);
    }
}
