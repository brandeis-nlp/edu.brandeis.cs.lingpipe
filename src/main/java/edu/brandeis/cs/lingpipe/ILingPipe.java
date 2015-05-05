package edu.brandeis.cs.lingpipe;

/**
 * Created by lapps on 5/5/2015.
 */
public interface ILingPipe {

    public String getConfidenceNERXml(String text) throws  Exception;

    public String getNBestNERXml(String text) throws  Exception;


    public String getNERXml(String text) throws  Exception;
}
