package org.dspace.doi;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.*;
import org.apache.log4j.Logger;
import java.io.IOException;
import java.util.*;
import org.dspace.core.*;

public class CDLDataCiteServiceTest {

    private static Logger log = Logger.getLogger(CDLDataCiteServiceTest.class);
    private String baseUrl = "https://n2t.net/ezid";
    private String myUsername;
    private String myPassword;

    public String publisher = null;

    public static void main(String[] args) throws IOException {
        Map<String, String> metadata = createMetadataListXML();
        CDLDataCiteServiceTest service = new CDLDataCiteServiceTest();
        String updateOutput = service.update("10.5061/DRYAD.2222", metadata);
	log.info("Output of the update command: " + updateOutput);
    }


    public CDLDataCiteServiceTest() {
	myUsername = ConfigurationManager.getProperty("doi.service.username");
	myPassword = ConfigurationManager.getProperty("doi.service.password");
	baseUrl =  ConfigurationManager.getProperty("doi.service.url");
    }


    public String update(String aDOI,Map<String, String> metadata) throws IOException {
        PostMethod post = new PostMethod(baseUrl + "/id/doi%3A" + aDOI);
        return executeHttpMethod(metadata, post);
    }

    private String executeHttpMethod(Map<String, String> metadata, EntityEnclosingMethod httpMethod) throws IOException {

        logMetadata(metadata);

        httpMethod.setRequestEntity(new StringRequestEntity(encodeAnvl(metadata), "text/plain", "UTF-8"));
        httpMethod.setRequestHeader("Content-Type", "text/plain");
        httpMethod.setRequestHeader("Accept", "text/plain");

        this.getClient(false).executeMethod(httpMethod);
	log.info("HTTP status: " + httpMethod.getStatusLine());
	log.debug("HTTP response text: " + httpMethod.getResponseBodyAsString(1000));
        return httpMethod.getStatusLine().toString();
    }

    private void logMetadata(Map<String, String> metadata) {
        log.info("Adding the following Metadata:");
	log.info(encodeAnvl(metadata));
    }


    HttpClient client = new HttpClient();

    private HttpClient getClient(boolean lookup) throws IOException {
        List authPrefs = new ArrayList(2);
        authPrefs.add(AuthPolicy.DIGEST);
        authPrefs.add(AuthPolicy.BASIC);
        client.getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs);
        if(!lookup) client.getState().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(myUsername, myPassword));
        return client;
    }


    private static Map<String, String> createMetadataListXML() {
        Map<String, String> metadata = new HashMap<String, String>();

        String xmlout =  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
	    "<resource xmlns=\"http://datacite.org/schema/kernel-2.2\" " +
	    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
	    "xsi:schemaLocation=\"http://datacite.org/schema/kernel-2.2 http://schema.datacite.org/meta/kernel-2.2/metadata.xsd\" " +
	    "metadataVersionNumber=\"1\" lastMetadataUpdate=\"2006-05-04\">" +
	        "<identifier identifierType=\"DOI\">10.5061/DRYAD.2222</identifier>" +
                "<creators>" +
                "<creator>" +
                "<creatorName>Tester, Testy</creatorName>" +
                "</creator>" +
	        "<creator>" +
                "<creatorName>Tester, Testette</creatorName>" +
                "</creator>" +
                "</creators>" +
                "<titles>" +
                "<title>A Test Item for the Purposes of Testing DOI Metadata</title>" +
                "</titles>" +
                "<publisher>Dryad Digital Repository</publisher>" +
                "<publicationYear>2012</publicationYear>" +
                "</resource>";

        log.debug("test metadata is " + xmlout);
        metadata.put("datacite", xmlout);

        return metadata;
    }

    private String encodeAnvl(Map<String, String> metadata) {
        Iterator<Map.Entry<String, String>> i = metadata.entrySet().iterator();
        StringBuffer b = new StringBuffer();
        while (i.hasNext()) {
            Map.Entry<String, String> e = i.next();
            b.append(escape(e.getKey()) + ": " + escape(e.getValue()) + "");
        }
        return b.toString();
    }

    private String escape(String s) {
        return s.replace("%", "%25").replace("\n", "%0A").
	    replace("\r", "%0D").replace(":", "%3A");
    }

}
