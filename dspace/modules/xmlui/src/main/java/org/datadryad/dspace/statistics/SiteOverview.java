package org.datadryad.dspace.statistics;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.excalibur.source.SourceValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.handle.HandleManager;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class SiteOverview extends AbstractDSpaceTransformer implements
		CacheableProcessingComponent {

	private static final Logger LOGGER = Logger.getLogger(SiteOverview.class);

	private static final Message STATS_TEXT = message("xmlui.Site.stats");

	private static final String PUB_SEARCH = "/select/?q=*:*&facet=on&rows=0&facet.field=prism.publicationName_filter&fq=location:l2";

	private static final String PUB_COUNTER = "count(//lst[@name='prism.publicationName_filter']/int[.!='0'])";

	private SourceValidity validity;

	public void addBody(Body body) throws SAXException, WingException,
			UIException, SQLException, IOException, AuthorizeException {
		String solr = ConfigurationManager.getProperty("solr.search.server");
		Division overviewStats = body.addDivision("front-page-stats");
		int dataPackageCount = 0;
		int dataFileCount = 0;
		int journalCount = 0;

		try {
			dataFileCount = ((Collection) HandleManager.resolveToObject(
					context, ConfigurationManager
							.getProperty("stats.datafiles.coll"))).countItems();
		}
		catch (ClassCastException details) {
			LOGGER.error("stats.datafiles.coll property isn't set properly");
		}

		try {
			dataPackageCount = ((Collection) HandleManager.resolveToObject(
					context, ConfigurationManager
							.getProperty("stats.datapkgs.coll"))).countItems();
		}
		catch (ClassCastException details) {
			LOGGER.error("stats.datapkgs.coll property isn't set properly");
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
				XPathFactory xpf = XPathFactory.newInstance();
				XPath xpath = xpf.newXPath();
				String xpathResult = xpath.evaluate(PUB_COUNTER, doc);

				journalCount = Integer.parseInt(xpathResult);
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

		Object[] params = new String[] {
				SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM)
						.format(new Date()),
				Integer.toString(dataPackageCount),
				Integer.toString(dataFileCount), Integer.toString(journalCount) };
		overviewStats.addPara(STATS_TEXT.parameterize(params));
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
}
