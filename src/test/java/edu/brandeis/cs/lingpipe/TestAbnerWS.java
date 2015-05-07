package edu.brandeis.cs.lingpipe;

import org.junit.Test;

/**
 * Created by lapps on 5/5/2015.
 */
public class TestAbnerWS {
    AbnerWS lingpipe = new AbnerWS();

    @Test
    public void testXml() throws Exception{
        System.out.println(lingpipe.getXml("p53 regulates human insulin-like growth factor II gene expression through active P4 promoter in rhabdomyosarcoma cells."));
    }

    @Test
    public void testText() throws Exception {
        System.out.println(lingpipe.execute("beautiful gene saf sas p53"));

        //
        System.out.println(lingpipe.execute("{\"discriminator\":\"http://vocab.lappsgrid.org/ns/media/text\", \"payload\": \"beautiful gene saf sas p53\"}"));

        //
        System.out.println(lingpipe.execute("{\"discriminator\":\"http://vocab.lappsgrid.org/ns/media/jsonld\",\"payload\":{\"@context\":\"http://vocab.lappsgrid.org/context-1.0.0.jsonld\",\"metadata\":{},\"text\":{\"@value\":\"beautiful gene saf sas p53\"},\"views\":[{\"metadata\":{\"contains\":{\"http://vocab.lappsgrid.org/Token\":{\"producer\":\"org.anc.lapps.stanford.Tokenizer:2.0.0\",\"type\":\"stanford\"}}},\"annotations\":[{\"id\":\"tok0\",\"start\":0,\"end\":9,\"label\":\"http://vocab.lappsgrid.org/Token\",\"features\":{\"word\":\"beautiful\"}},{\"id\":\"tok1\",\"start\":10,\"end\":14,\"label\":\"http://vocab.lappsgrid.org/Token\",\"features\":{\"word\":\"gene\"}},{\"id\":\"tok2\",\"start\":15,\"end\":18,\"label\":\"http://vocab.lappsgrid.org/Token\",\"features\":{\"word\":\"saf\"}},{\"id\":\"tok3\",\"start\":19,\"end\":22,\"label\":\"http://vocab.lappsgrid.org/Token\",\"features\":{\"word\":\"sas\"}},{\"id\":\"tok4\",\"start\":23,\"end\":26,\"label\":\"http://vocab.lappsgrid.org/Token\",\"features\":{\"word\":\"p53\"}}]}]}}"));
    }
}
