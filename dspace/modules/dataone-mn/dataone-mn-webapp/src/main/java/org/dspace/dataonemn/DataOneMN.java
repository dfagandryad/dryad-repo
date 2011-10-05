package org.dspace.dataonemn;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

import org.joda.time.format.DateTimeFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class accepts an HTTP request and passes off to the appropriate location
 * to perform an action. It is very lightweight, just for testing some initial
 * setup. It will eventually be merged into other code.
 * 
 * @author Ryan Scherle
 * @author Kevin S. Clarke
 **/
public class DataOneMN extends HttpServlet implements Constants {

	private static final long serialVersionUID = -3545762362447908735L;

	private static final Logger LOGGER = LoggerFactory
			.getLogger(DataOneMN.class);

	private static final String CONTENT_TYPE = "application/xml; charset=UTF-8";

	private static final String TEXT_XML_CONTENT_TYPE = "text/xml; charset=UTF-8";

	private String myData;

	private String mySolr;

	protected void doHead(HttpServletRequest aReq, HttpServletResponse aResp)
			throws ServletException, IOException {
		String reqPath = aReq.getPathInfo();
		Context ctxt = null;

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("pathinfo=" + reqPath);
		}

		try {
			ctxt = new Context();
			ctxt.ignoreAuthorization();

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("DSpace context initialized");
			}
		}
		catch (SQLException details) {
			LOGGER.error("Unable to initialize DSpace", details);
			
			try {
				if (ctxt != null) {
					ctxt.complete();
				}
			}
			catch (SQLException deets) {
				LOGGER.warn(deets.getMessage(), deets);
			}
			
			throw new ServletException(details);
		}

		if (reqPath.startsWith("/object/")) {
			ObjectManager objManager = new ObjectManager(ctxt, myData, mySolr);
			String id = reqPath.substring("/object/".length());
			String[] parts = objManager.parseIDFormat(id);
			String name = parts[0];
			String format = parts[1];

			try {
				long length = objManager.getObjectSize(name, format);
				aResp.setContentLength((int) length);

				if (format.equals("xml") || format.equals("dap")) {
					aResp.setContentType(CONTENT_TYPE);
				}
				else {
					ServletContext context = getServletContext();
					String mimeType = context.getMimeType("f." + format);

					if (mimeType == null || mimeType.equals("")) {
						mimeType = "application/octet-stream";
					}

					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Checking mimeType of " + format);

						LOGGER.debug("Setting data file MIME type to: "
								+ mimeType + " (this is configurable)");
					}

					aResp.setContentType(mimeType);
				}
			}
			catch (SQLException details) {
				LOGGER.error(details.getMessage(), details);
				throw new ServletException(details);
			}
			catch (StringIndexOutOfBoundsException details) {
				LOGGER.error("Passed request did not find a match", details);
				aResp.sendError(HttpServletResponse.SC_NOT_FOUND);
			}
			catch (Exception details) {
				LOGGER.error("UNEXPECTED EXCEPTION", details);
				aResp.sendError(HttpServletResponse.SC_NOT_FOUND);
			}
			finally {
				try {
					ctxt.complete();
				}
				catch (SQLException sqlDetails) {
					LOGGER.warn("Couldn't complete DSpace context");
				}
			}
		}
		else {
			aResp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
			
			try {
				if (ctxt != null) {
					ctxt.complete();
				}
			}
			catch (SQLException details) {
				LOGGER.warn(details.getMessage(), details);
			}
		}
	}

	/**
	 * We don't implement this yet.
	 */
	@Override
	protected void doPost(HttpServletRequest aReq, HttpServletResponse aResp)
			throws ServletException, IOException {
		aResp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
	}

	/**
	 * We don't implement this yet.
	 */
	@Override
	protected void doPut(HttpServletRequest aReq, HttpServletResponse aResp)
			throws ServletException, IOException {
		aResp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
	}



	/**
	 * Receives the GET HTTP call and passes off to the appropriate method.
	 **/
	@Override
	protected void doGet(HttpServletRequest aReq, HttpServletResponse aResp)
			throws ServletException, IOException {
		String reqPath = aReq.getPathInfo();
		Context ctxt = null;
		
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("pathinfo=" + reqPath);
		}

		try {
			ctxt = new Context();
			ctxt.ignoreAuthorization();

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("DSpace context initialized");
			}
		}
		catch (SQLException details) {
			LOGGER.error("Unable to initialize DSpace", details);
			
			try {
				if (ctxt != null) {
					ctxt.complete();
				}
			}
			catch (SQLException deets) {
				LOGGER.warn(deets.getMessage(), deets);
			}
			
			throw new ServletException(details);
		}

		if (reqPath.startsWith("/object")) {			
			ObjectManager objManager = new ObjectManager(ctxt, myData, mySolr);

			try {
				if (reqPath.equals("/object")) {
					String format = aReq.getParameter("objectFormat");
					Date from = parseDate(aReq, "startTime");
					Date to = parseDate(aReq, "endTime");

					int start = parseInt(aReq, "start",
							ObjectManager.DEFAULT_START);
					int count = parseInt(aReq, "count",
							ObjectManager.DEFAULT_COUNT);

					aResp.setContentType(CONTENT_TYPE);

					if (count <= 0) {
						OutputStream out = aResp.getOutputStream();
						objManager.printList(from, to, format, out);
					}
					else {
						OutputStream out = aResp.getOutputStream();
						objManager.printList(start, count, from, to, format,
								out);
					}
				}
				else if (reqPath.startsWith("/object/")) {
					String id = reqPath.substring("/object/".length());
					int lastSlashIndex = id.lastIndexOf("/");
					String format = id.substring(lastSlashIndex + 1);
					String name = id.substring(0, lastSlashIndex);
					String fileName = name.startsWith("doi:") ? name
							.substring(4) : name;

					if (format.equals("dap")) {
						aResp.setContentType(CONTENT_TYPE);
					}
					else {
						ServletContext context = getServletContext();
						String mimeType = context.getMimeType("f." + format);

						if (mimeType == null || mimeType.equals("")) {
							mimeType = "application/octet-stream";
						}

						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("Setting data file MIME type to: "
									+ mimeType + " (this is configurable)");
						}

						// We need to check types supported here and add to it
						aResp.setContentType(mimeType);

						// We want to download it if viewing in the browser
						aResp.setHeader(
								"Content-Disposition",
								"attachment; filename=\""
										+ fileName.replaceAll("[\\/|\\.]", "_")
										+ "." + format + "\"");
					}

					try {
						objManager.getObject(name, format,
								aResp.getOutputStream());
					}
					catch (NotFoundException details) {
						aResp.sendError(HttpServletResponse.SC_NOT_FOUND, name
								+ "." + format + " couldn't be found");
					}
				}
				else {
					aResp.sendError(HttpServletResponse.SC_NOT_FOUND,
							"Did you mean '/object' or '/object/doi:...'");
				}

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("DSpace context completed");
				}
			}
			catch (SQLException details) {
				LOGGER.error(details.getMessage(), details);
				throw new ServletException(details);
			}
			catch (StringIndexOutOfBoundsException details) {
				LOGGER.error("Passed request did not find a match", details);
				aResp.sendError(HttpServletResponse.SC_NOT_FOUND);
			}
			catch (Exception details) {
				LOGGER.error("UNEXPECTED EXCEPTION", details);
			}
			finally {
				try {
					ctxt.complete();
				}
				catch (SQLException sqlDetails) {
					LOGGER.warn("Couldn't complete DSpace context");
				}
			}
		}
		else if (reqPath.startsWith("/meta/")) {
			SysMetaManager sysMeta = new SysMetaManager(ctxt, myData, mySolr);
			String id = reqPath.substring("/meta/".length());

			aResp.setContentType(TEXT_XML_CONTENT_TYPE); // default for /meta

			try {
				sysMeta.getObjectMetadata(id, aResp.getOutputStream());
			}
			catch (NotFoundException details) {
				aResp.sendError(HttpServletResponse.SC_NOT_FOUND, id
						+ " couldn't be found");
			}
			catch (SQLException details) {
				LOGGER.error(details.getMessage(), details);
				throw new ServletException(details);
			}
			catch (SolrServerException details) {
				LOGGER.error(details.getMessage(), details);
				throw new ServletException(details);
			}
			catch (StringIndexOutOfBoundsException details) {
				LOGGER.error("Passed request did not find a match", details);
				aResp.sendError(HttpServletResponse.SC_NOT_FOUND);
			}
			finally {
				try {
					ctxt.complete();
				}
				catch (SQLException sqlDetails) {
					LOGGER.warn("Couldn't complete DSpace context");
				}
			}
		}
		else if (reqPath.startsWith("/checksum/")) {
			ObjectManager objManager = new ObjectManager(ctxt, myData, mySolr);
			String id = reqPath.substring("/checksum/".length());
			int lastSlashIndex = id.lastIndexOf("/");
			String format = id.substring(lastSlashIndex + 1);
			String name = id.substring(0, lastSlashIndex);

			aResp.setContentType(TEXT_XML_CONTENT_TYPE);

			try {
				String[] checksum = objManager.getObjectChecksum(name, format);
				PrintWriter writer = aResp.getWriter();

				writer.print("<checksum xmlns=\"" + MN_SERVICE_TYPES_NAMESPACE
						+ "\" algorithm=\"" + checksum[1] + "\">" + checksum[0]
						+ "</checksum>");

				writer.close();
			}
			catch (NotFoundException details) {
				aResp.sendError(HttpServletResponse.SC_NOT_FOUND, name + "/"
						+ format + " not found");
			}
			catch (SQLException details) {
				aResp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						details.getMessage());
			}
			catch (IOException details) {
				aResp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						details.getMessage());
			}
		}
		else if (reqPath.startsWith("/isAuthorized/")) {
			aResp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
		}
		else if (reqPath.startsWith("/accessRules/")) {
			aResp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
		}
		else if (reqPath.startsWith("/log")) {
			aResp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
		}
		else if (reqPath.startsWith("/node")) {
			aResp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
		}
		else if (reqPath.startsWith("/error")) {
			aResp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
		}
		else if (reqPath.startsWith("/monitor/ping")) {
			aResp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
		}
		else if (reqPath.startsWith("/monitor/object")) {
			aResp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
		}
		else if (reqPath.startsWith("/monitor/event")) {
			aResp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
		}
		else if (reqPath.startsWith("/monitor/status")) {
			aResp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
		}
		else if (reqPath.startsWith("/replicate")) {
			aResp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
		}
		else {
			aResp.sendError(HttpServletResponse.SC_BAD_REQUEST);
		}
		
		try {
			if (ctxt != null && ctxt.isValid()) {
				ctxt.complete();
			}
		}
		catch (SQLException details) {
			LOGGER.warn(details.getMessage(), details);
		}
	}

	/**
	 * Initializes the DSpace context, so we have access to the DSpace objects.
	 * Requires the location of the dspace.cfg file to be set in the web.xml.
	 **/
	public void init() throws ServletException {
		ServletContext context = this.getServletContext();
		String configFileName = context.getInitParameter("dspace.config");
		File aConfig = new File(configFileName);

		if (aConfig != null) {
			if (aConfig.exists() && aConfig.canRead() && aConfig.isFile()) {
				ConfigurationManager.loadConfig(aConfig.getAbsolutePath());

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("DSpace config loaded from " + aConfig);
				}
			}
			else if (!aConfig.exists()) {
				throw new RuntimeException(aConfig.getAbsolutePath()
						+ " doesn't exist");
			}
			else if (!aConfig.canRead()) {
				throw new RuntimeException("Can't read the dspace.cfg file");
			}
			else if (!aConfig.isFile()) {
				throw new RuntimeException("Err, dspace.cfg isn't a file?");
			}
		}

		myData = ConfigurationManager.getProperty("stats.datafiles.coll");
		mySolr = ConfigurationManager.getProperty("solr.dryad.server");
	}

	private int parseInt(HttpServletRequest aReq, String aParam, int aDefault) {
		String intString = aReq.getParameter(aParam);
		int intValue = aDefault;

		try {
			if (intString != null) {
				intValue = Integer.parseInt(intString);
			}
		}
		catch (NumberFormatException details) {
			if (LOGGER.isWarnEnabled()) {
				LOGGER.warn(aParam + " parameter not an int: " + intString);
			}
		}

		return intValue;
	}

	private Date parseDate(HttpServletRequest aReq, String aParam)
			throws ParseException {
		String date = aReq.getParameter(aParam);

		if (date == null) {
			return null;
		}

		try {
			return DateTimeFormat.fullDateTime().parseDateTime(date).toDate();
		}
		catch (IllegalArgumentException details) {}
		
		try {
			return DateTimeFormat.forPattern("yyyyMMdd").parseDateTime(date).toDate();
		}
		catch (IllegalArgumentException details) {}
		
		try {
			return DateTimeFormat.forPattern("yyyy-MM-dd").parseDateTime(date).toDate();
		}
		catch (IllegalArgumentException details) {}
		
		try {
			return DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSS").parseDateTime(date).toDate();
		}
		catch (IllegalArgumentException details) {}
		
		try {
			return DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss.SSSS").parseDateTime(date).toDate();
		}
		catch (IllegalArgumentException details) {}
		
		try {
			return DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSS+HH:mm").parseDateTime(date).toDate();
		}
		catch (IllegalArgumentException details) {}
		
		try {
			return DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss.SSSS+HHmm").parseDateTime(date).toDate();
		}
		catch (IllegalArgumentException details) {}

		return null;
	}
}
