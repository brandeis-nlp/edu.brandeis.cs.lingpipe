package edu.brandeis.cs.lingpipe;

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
public class LingPipeWS implements WebService, ILingPipe {


    public static String getResource(String name) throws IOException{
        java.io.InputStream in =  LingPipeWS.class.getClassLoader().getResourceAsStream(name);
        return IOUtils.toString(in);
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
            String xml = getNERXml(txt);

            System.out.println("\nXML : \n-----------------------\n"+ xml);
            String annotations =  Json2Json.xml2jsondsl(xml,
                    getResource(this.getClass().getName()+".dsl"));
            System.out.println("\nAnnotations : \n-----------------------\n"+ annotations);
            JsonProxy.JsonObject annsobj = JsonProxy.newObject().read(annotations);
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
                    .put("type", "ner:lingpipe"));
            view.put("metadata", JsonProxy.newObject().put("contains", contains));
            view.put("annotations", annsobj.get("annotations"));
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

    static ConfidenceChunker confidChunker;
    static NBestChunker nbestChunker;
    static Chunker chunker;

    public LingPipeWS() {
        synchronized (LingPipeWS.class) {
            if(confidChunker == null) {
                File modelFile = FileUtils.toFile(LingPipeWS.class.getResource("/ne-en-bio-genetag.HmmChunker"));
                try {
                    Object modelObj = AbstractExternalizable.readObject(modelFile);
                    confidChunker = (ConfidenceChunker) modelObj;
                    nbestChunker = (NBestChunker) modelObj;
                    chunker = (Chunker) modelObj;
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static int MAX_N_BEST_CHUNKS = 16;
    public static int MAX_N_BEST = 16;

    // http://alias-i.com/lingpipe/demos/tutorial/ne/read-me.html
    public String getConfidenceNERXml(String text) throws Exception {
        char[] cs = text.toCharArray();
        Iterator<Chunk> it
                = confidChunker.nBestChunks(cs, 0, cs.length, MAX_N_BEST_CHUNKS);
        System.out.println("Rank          Conf      Span    Type     Phrase");

        XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(sw);
        xmlStreamWriter.writeStartDocument();

        xmlStreamWriter.writeStartElement("nBestEntities");
        xmlStreamWriter.writeStartElement("s");
        xmlStreamWriter.writeCharacters(text);
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("confidence");
        int pos = 0;
        for (int n = 0; it.hasNext(); ++n) {
            Chunk neChunk = it.next();
            int start = neChunk.start();
            int end = neChunk.end();
            double condProb = Math.pow(2.0, neChunk.score());
            String phrase = text.substring(start, end);
                    System.out.println(n + " "
                    + String.format("%6.4f",condProb)
                    + "       (" + start
                    + ", " + end
                    + ")       " + neChunk.type()
                    + "         " + phrase);
//            System.out.println("\nPhrase = " + phrase);

            String type = neChunk.type();
            String chunkText = text.substring(start,end);
//            System.out.println("start="+start+" end="+end);
            xmlStreamWriter.writeStartElement("ENAMEX");
            xmlStreamWriter.writeAttribute("TYPE", type);
            xmlStreamWriter.writeAttribute("start", "" + start);
            xmlStreamWriter.writeAttribute("end", "" + end);
            xmlStreamWriter.writeAttribute("condProb", String.format("%6.4f", condProb));
            xmlStreamWriter.writeAttribute("TEXT", chunkText);
            xmlStreamWriter.writeCharacters(chunkText);
            xmlStreamWriter.writeEndElement();
        }
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndDocument();
        xmlStreamWriter.flush();
        xmlStreamWriter.close();
        return  sw.toString();
    }

    public String getNBestNERXml(String text) throws Exception {
        char[] cs = text.toCharArray();
        Iterator<ScoredObject<Chunking>> it = nbestChunker.nBest(cs,0,cs.length,MAX_N_BEST);
        System.out.println(text);
        for (int n = 0; it.hasNext(); ++n) {
            ScoredObject so = it.next();
            double jointProb = so.score();
            Chunking chunking = (Chunking) so.getObject();
            System.out.println(n + " " + jointProb
                    +  " " + chunking.chunkSet());
            for(Chunk chunk:chunking.chunkSet()) {
                int start = chunk.start();
                int end = chunk.end();
                String phrase = text.substring(start, end);
                System.out.println(n + " "
                        + String.format("%6.4f", Math.pow(2.0, chunk.score()))
                        + "       (" + start
                        + ", " + end
                        + ")       " + chunk.type()
                        + "         " + phrase);


            }
        }
        return null;
    }

    public String getNERXml(String text) throws Exception {
        Chunking chunking = chunker.chunk(text);
        System.out.println("Chunking=" + chunking);
        int n = 0;

        XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(sw);
        xmlStreamWriter.writeStartDocument();

        xmlStreamWriter.writeStartElement("Entities");
        xmlStreamWriter.writeStartElement("s");
        xmlStreamWriter.writeCharacters(text);
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeStartElement("ChunkSet");
        for(Chunk chunk:chunking.chunkSet()) {
            int start = chunk.start();
            int end = chunk.end();
            String phrase = text.substring(start, end);
            System.out.println(n++ + " "
                    + String.format("%6.4f", Math.pow(2.0, chunk.score()))
                    + "       (" + start
                    + ", " + end
                    + ")       " + chunk.type()
                    + "         " + phrase);

            xmlStreamWriter.writeStartElement("Chunk");
            xmlStreamWriter.writeAttribute("TYPE", chunk.type());
            xmlStreamWriter.writeAttribute("start", "" + start);
            xmlStreamWriter.writeAttribute("end", "" + end);
            xmlStreamWriter.writeCharacters(phrase);
            xmlStreamWriter.writeEndElement();
        }
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndDocument();
        xmlStreamWriter.flush();
        xmlStreamWriter.close();
        return  sw.toString();
    }
}
