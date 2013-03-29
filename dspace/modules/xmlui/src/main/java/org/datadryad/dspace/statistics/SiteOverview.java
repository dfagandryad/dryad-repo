package org.datadryad.dspace.statistics;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.excalibur.source.SourceValidity;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchUtils;
import org.dspace.handle.HandleManager;
import org.dspace.statistics.content.DatasetDSpaceObjectGenerator;
import org.dspace.statistics.content.StatisticsDataVisits;
import org.dspace.statistics.content.StatisticsListing;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class SiteOverview extends AbstractDSpaceTransformer implements
        CacheableProcessingComponent {

    private static final Logger LOGGER = Logger.getLogger(SiteOverview.class);

    private static final Message STATS_TEXT = message("xmlui.Site.stats");


    private static final String PUB_SEARCH = "/select/?q=DSpaceStatus:Archived&facet=on&rows=0&facet.field=prism.publicationName_filter&fq=location:l2&facet.limit=-1";
    private static final String AUTH_SEARCH = "/select/?q=DSpaceStatus:Archived&facet=on&rows=0&facet.field=dc.contributor.author_filter&fq=location:l2&facet.limit=-1";
    private static final String DOWN_SEARCH = "/select/?q=*%3A*&facet=on&rows=0&facet.limit=-1&facet.field=type";

    private static final String PUB_COUNTER = "count(//lst[@name='prism.publicationName_filter']/int[.!='0'])";
    private static final String AUTH_COUNTER = "count(//lst[@name='dc.contributor.author_filter']/int[.!='0'])";
    private static final String DOWN_COUNTER = "//lst[@name='type']/int[@name='"+Constants.BITSTREAM+"']";

    private SourceValidity validity;

    protected QueryResponse queryResults;

    /**
     * Cached query arguments
     */
    protected SolrQuery queryArgs;

    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException {
        String solr = ConfigurationManager.getProperty("solr.search.server");
        Division overviewStats = body.addDivision("front-page-stats");
        long dataPackageCount = 0;
        long dataFileCount = 0;
        String journalCount = "0";

        String uniqAuthors="0";
        String totalFileDownload="0";

        try {
            dataFileCount = ((Collection) HandleManager.resolveToObject(
                    context, ConfigurationManager
                    .getProperty("stats.datafiles.coll"))).countItems();


        }
        catch (ClassCastException details) {
            LOGGER.error("stats.datafiles.coll property isn't set properly");
        }
        try {
            SolrQuery query = new SolrQuery();
            query= query.setQuery("location:l2 AND DSpaceStatus:Archived");
            QueryResponse response = getSolr().query(query);
            dataPackageCount = response.getResults().getNumFound();
        }catch (SolrServerException details) {
            LOGGER.error(details.getMessage(), details);
        }

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            GetMethod get = new GetMethod(solr + PUB_SEARCH);


            switch (new HttpClient().executeMethod(get)) {
                case 200:
                case 201:
                case 202:
                    Document doc = db.parse(get.getResponseBodyAsStream());
                    doc.getDocumentElement().normalize();

                    // xmlToString(doc);


                    XPathFactory xpf = XPathFactory.newInstance();
                    XPath xpath = xpf.newXPath();
                    String xpathResult = xpath.evaluate(PUB_COUNTER, doc);

                    journalCount =xpathResult;
                    break;
                default:
                    LOGGER.error("Solr search failed to respond as expected");
            }

            get.releaseConnection();
        }
        catch (ParserConfigurationException details) {
            LOGGER.error(details.getMessage(), details);
        }
        catch (XPathExpressionException details) {
            LOGGER.error(details.getMessage(), details);
        }

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            GetMethod get = new GetMethod(solr + AUTH_SEARCH);


            switch (new HttpClient().executeMethod(get)) {
                case 200:
                case 201:
                case 202:
                    Document doc = db.parse(get.getResponseBodyAsStream());
                    doc.getDocumentElement().normalize();

                    // xmlToString(doc);


                    XPathFactory xpf = XPathFactory.newInstance();
                    XPath xpath = xpf.newXPath();
                    String xpathResult = xpath.evaluate(AUTH_COUNTER, doc);

                    uniqAuthors = xpathResult;
                    break;
                default:
                    LOGGER.error("Solr search failed to respond as expected");
            }

            get.releaseConnection();
        }
        catch (ParserConfigurationException details) {
            LOGGER.error(details.getMessage(), details);
        }
        catch (XPathExpressionException details) {
            LOGGER.error(details.getMessage(), details);
        }


       //remove duplicate code

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            GetMethod get = new GetMethod(solr.replace("search","statistics") + DOWN_SEARCH);


            switch (new HttpClient().executeMethod(get)) {
                case 200:
                case 201:
                case 202:
                    Document doc = db.parse(get.getResponseBodyAsStream());
                    doc.getDocumentElement().normalize();

                    // xmlToString(doc);


                    XPathFactory xpf = XPathFactory.newInstance();
                    XPath xpath = xpf.newXPath();
                    String xpathResult = xpath.evaluate(DOWN_COUNTER, doc);

                    totalFileDownload = xpathResult;
                    break;
                default:
                    LOGGER.error("Solr search failed to respond as expected");
            }

            get.releaseConnection();
        }
        catch (ParserConfigurationException details) {
            LOGGER.error(details.getMessage(), details);
        }
        catch (XPathExpressionException details) {
            LOGGER.error(details.getMessage(), details);
        }

        org.dspace.app.xmlui.wing.element.Table infoTable= overviewStats.addTable("list-table",5,2);

        String value = Long.toString(dataPackageCount) ;

        Row row = infoTable.addRow();
        row.addCell("data").addContent("Total data packages");
        row.addCell("data").addContent(Long.toString(dataPackageCount));

        row = infoTable.addRow();
        row.addCell("data").addContent("Total data files");
        row.addCell("data").addContent(Long.toString(dataFileCount));

        row = infoTable.addRow();
        row.addCell("data").addContent("Journals represented");
        row.addCell("data").addContent(journalCount);

        row = infoTable.addRow();
        row.addCell("data").addContent("Total unique authors");
        row.addCell("data").addContent(uniqAuthors);

        row = infoTable.addRow();
        row.addCell("data").addContent("Total file downloads");
        row.addCell("data").addContent(totalFileDownload);

    }

    //	@Override
    public Serializable getKey() {
        return getClass().getName();
    }

    //	@Override
    public SourceValidity getValidity() {
        if (validity == null) {
            DSpaceValidity newValidity = new DSpaceValidity();
            newValidity.setAssumedValidityDelay(86400000);
            validity = newValidity.complete();
        }

        return validity;
    }


    /**
     * Method used for debugging scope.
     *
     */

    public static void xmlToString(Node node) {
        try {
            TransformerFactory transFactory = TransformerFactory.newInstance();
            Transformer transformer = null;

            transformer = transFactory.newTransformer();

            StringWriter buffer = new StringWriter();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(node), new StreamResult(buffer));
            String str = buffer.toString();
            LOGGER.error("SiteOverviewStatistic - journalXML: " + str);

        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    private CommonsHttpSolrServer getSolr() throws MalformedURLException, SolrServerException {
        CommonsHttpSolrServer solr;
        String solrService = ConfigurationManager.getProperty("solr.search.server");
        solr = new CommonsHttpSolrServer(solrService);
        solr.setBaseURL(solrService);
        SolrQuery solrQuery = new SolrQuery().setQuery("*:*");
        solr.query(solrQuery);
        return solr;
    }



}
