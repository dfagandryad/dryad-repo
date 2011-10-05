package org.dspace.submit.step;

import org.apache.log4j.Logger;
import org.dspace.content.*;
import org.dspace.core.LogManager;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.authorize.AuthorizeException;
import org.dspace.handle.HandleManager;
import org.dspace.submit.bean.PublicationBean;
import org.dspace.submit.model.ModelPublication;
import org.dspace.workflow.WorkflowRequirementsManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * User: @author kevinvandevelde (kevin at atmire.com)
 * Date: 27-jan-2010
 * Time: 15:12:46
 *
 * The processing of the first step in which a journal can be selected.
 */
public class SelectPublicationStep extends AbstractProcessingStep {

    public static final int STATUS_INVALID_PUBLICATION_ID = 1;
    public static final int STATUS_LICENSE_NOT_ACCEPTED = 2;
    public static final int ERROR_SELECT_JOURNAL = 3;
    public static final int ERROR_INVALID_JOURNAL = 4;

    private static Map<String, DCValue> journalToMetadata = new HashMap<String, DCValue>();
    public static List<String> integratedJournals = new ArrayList<String>();
    public static final List<String> journalNames = new ArrayList<String>();
    public static final List<String> journalVals = new ArrayList<String>();
    public static final List<String> journalDirs = new ArrayList<String>();
    public static final List<Boolean> journalEmbargo = new ArrayList<Boolean>();
    public static final Map<String, List<String>> journalNotifyOnReview = new HashMap<String, List<String>>();
    public static final Map<String, List<String>> journalNotifyOnArchive = new HashMap<String, List<String>>();
    private static Logger log = Logger.getLogger(SelectPublicationStep.class);


    static {
        journalVals.add("other");
        journalNames.add("(please select a journal)");
        journalDirs.add(null);
        journalEmbargo.add(false);

        String journalPropFile = ConfigurationManager.getProperty("submit.journal.config");
        Properties properties = new Properties();

        try {
            properties.load(new FileInputStream(journalPropFile));
            String journalTypes = properties.getProperty("journal.order");
            for (int i = 0; i < journalTypes.split(",").length; i++) {
                String journalType = journalTypes.split(",")[i].trim();
                String journalDisplay = properties.getProperty("journal." + journalType + ".fullname");
                String metadataDir = properties.getProperty("journal." + journalType + ".metadataDir");
                String integrated = properties.getProperty("journal." + journalType + ".integrated");
                String embargo = properties.getProperty("journal." + journalType + ".embargoAllowed", "true");
                List<String> onReviewMails = Arrays.asList(properties.getProperty("journal." + journalType + ".notifyOnReview", "").replace(" ", "").split(","));
                List<String> onArchiveMails = Arrays.asList(properties.getProperty("journal." + journalType + ".notifyOnArchive", "").replace(" ", "").split(","));

                journalVals.add(journalType);
                journalNames.add(journalDisplay);
                journalDirs.add(metadataDir);
                if(integrated != null && Boolean.valueOf(integrated))
                    integratedJournals.add(journalType);
                journalEmbargo.add(Boolean.valueOf(embargo));
                journalNotifyOnReview.put(journalType, onReviewMails);
                journalNotifyOnArchive.put(journalType, onArchiveMails);

            }
        } catch (IOException e) {
            log.error("Error while loading journal properties", e);
        }

        journalVals.add("other");
        journalNames.add("OTHER JOURNAL");
        journalDirs.add(null);
        journalEmbargo.add(false);

        int counter = 1;
        String configLine = ConfigurationManager.getProperty("submit.journal.metadata." + counter);
        while(configLine != null){
            String journalField = configLine.split(":")[0];
            String metadataField = configLine.split(":")[1];
            DCValue dcVal = new DCValue();
            dcVal.schema = metadataField.split("\\.")[0];
            dcVal.element = metadataField.split("\\.")[1];
            if(metadataField.split("\\.").length == 3)
                dcVal.qualifier = metadataField.split("\\.")[2];

            //Add it our map
            journalToMetadata.put(journalField,dcVal);

            //Add one to our counter & read a new line
            counter++;
            configLine = ConfigurationManager.getProperty("submit.journal.metadata." + counter);
        }
    }



    public int doProcessing(Context context, HttpServletRequest request, HttpServletResponse response, SubmissionInfo submissionInfo) throws ServletException, IOException, SQLException, AuthorizeException {
        Item item = submissionInfo.getSubmissionItem().getItem();

        //First of all check if we have accepted our license
        if(request.getParameter("license_accept") == null || !Boolean.valueOf(request.getParameter("license_accept")))
            return STATUS_LICENSE_NOT_ACCEPTED;

        String journalID = request.getParameter("journalID");
        String manuscriptNumber = request.getParameter("manu");

        //We have selected to choose a journal, retrieve it
        if(!journalID.equals("other")){
            if(!integratedJournals.contains(journalID) || (integratedJournals.contains(journalID) && manuscriptNumber != null && manuscriptNumber.trim().equals(""))){
                //Just add the journal title
                String title = journalNames.get(journalVals.indexOf(journalID));
                //Should it end with a *, remove it.
                if(title.endsWith("*"))
                    title = title.substring(0, title.length() - 1);

                Boolean embargoAllowed = Boolean.valueOf(journalEmbargo.get(journalVals.indexOf(journalID)));
                if(!embargoAllowed){
                    //We don't need to show the embargo option to any of our data files
                    item.addMetadata("internal", "submit", "showEmbargo", null, String.valueOf(embargoAllowed));
                }
                item.addMetadata("prism", "publicationName", null, null, title);
                item.update();


            } else {
                String journalPath = journalDirs.get(journalVals.indexOf(journalID));
                //We have a valid journal
                PublicationBean pBean = ModelPublication.getDataFromPublisherFile(manuscriptNumber, journalID, journalPath);
                if (pBean.getMessage().equals((""))) {
                    importJournalMetadata(context, item, pBean);
                    List<String> reviewEmails = journalNotifyOnReview.get(journalID);
                    item.addMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "review", "mailUsers", null, reviewEmails.toArray(new String[reviewEmails.size()]));

                    List<String> archiveEmails = journalNotifyOnArchive.get(journalID);
                    item.addMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "archive", "mailUsers", null, archiveEmails.toArray(new String[archiveEmails.size()]));

                    boolean embargoAllowed = journalEmbargo.get(journalVals.indexOf(journalID));
                    if(!embargoAllowed){
                        //We don't need to show the embargo option to any of our data files
                        item.addMetadata("internal", "submit", "showEmbargo", null, String.valueOf(embargoAllowed));
                    }
                    item.update();
                }else{
                    //Add the error to our session so we know which one to display
                    request.getSession().setAttribute("submit_error", pBean.getMessage());
                    return ERROR_SELECT_JOURNAL;
                }
            }
        }

        return STATUS_COMPLETE;
    }

    private void importJournalMetadata(Context context, Item item, PublicationBean pBean){
        addSingleMetadataValueFromJournal(context, item, "journalName", pBean.getJournalName());
        addSingleMetadataValueFromJournal(context, item, "journalVolume", pBean.getJournalVolume());
        //addSingleMetadataValueFromJournal(context, item, "fullCitation", pBean.getFullCitation());
        addSingleMetadataValueFromJournal(context, item, "title", pBean.getTitle());
        addSingleMetadataValueFromJournal(context, item, "abstract", pBean.getAbstract());
        addSingleMetadataValueFromJournal(context, item, "correspondingAuthor", pBean.getCorrespondingAuthor());
        addSingleMetadataValueFromJournal(context, item, "doi", pBean.getDOI());
        addMultiMetadataValueFromJournal(context, item, "authors", pBean.getAuthors());
        addMultiMetadataValueFromJournal(context, item, "subjectKeywords", pBean.getSubjectKeywords());
        addMultiMetadataValueFromJournal(context, item, "taxonomicNames", pBean.getTaxonomicNames());
        addMultiMetadataValueFromJournal(context, item, "coverageSpatial", pBean.getCoverageSpatial());
        addMultiMetadataValueFromJournal(context, item, "coverageTemporal", pBean.getCoverageTemporal());
        addSingleMetadataValueFromJournal(context, item, "publicationDate", pBean.getPublicationDate());
        addSingleMetadataValueFromJournal(context, item, "journalISSN", pBean.getJournalISSN());
        addSingleMetadataValueFromJournal(context, item, "journalNumber", pBean.getJournalNumber());
        addSingleMetadataValueFromJournal(context, item, "publisher", pBean.getPublisher());
        addSingleMetadataValueFromJournal(context, item, "manuscriptNumber", pBean.getManuscriptNumber());
        addSingleMetadataValueFromJournal(context, item, "journalID", pBean.getJournalID());
//        if(pBean.getEmail() != null){
//            addMultiMetadataValueFromJournal(context, item, "email", Arrays.asList(pBean.getEmail().split(",")));
//        }
        addSingleMetadataValueFromJournal(context, item, "status", String.valueOf(pBean.isSkipReviewStep()));

    }


    private void addSingleMetadataValueFromJournal(Context ctx, Item publication, String key, String value){
        DCValue dcVal = journalToMetadata.get(key);
        if(dcVal == null){
            log.error(LogManager.getHeader(ctx, "error importing field from journal", "Could not retrieve a metadata field for journal getter: " + key));
            return;
        }

        if(value != null)
            publication.addMetadata(dcVal.schema, dcVal.element, dcVal.qualifier, null, value);
    }

    private void addMultiMetadataValueFromJournal(Context ctx, Item publication, String key, List<String> values){
        DCValue dcVal = journalToMetadata.get(key);
        if(dcVal == null){
            log.error(LogManager.getHeader(ctx, "error importing field from journal", "Could not retrieve a metadata field for journal getter: " + key));
            return;
        }

        if(values != null && 0 < values.size())
            publication.addMetadata(dcVal.schema, dcVal.element, dcVal.qualifier, null, values.toArray(new String[values.size()]));
    }

    

    public int getNumberOfPages(HttpServletRequest request, SubmissionInfo submissionInfo) throws ServletException {
        return 1;
    }

    @Override
    public boolean isStepAccessible(Context context, Item item) {
        //If we already have a handle there is no need to use this step
        boolean stepAccessible = true;
        if(item.getHandle() == null){
            try {
                WorkspaceItem wsItem = WorkspaceItem.findByItemId(context, item.getID());
                if(wsItem != null){
                    //Only allow this step if the user hasn't passed it
                    stepAccessible = 1 == wsItem.getStageReached() || -1 == wsItem.getStageReached();
                }
            } catch (SQLException e) {
                log.error("Error in isStepAccessible: " + e.getMessage(), e);
            }
        }else{
            stepAccessible = false;
        }

        return stepAccessible;
//        return item.getMetadata(MetadataSchema.DC_SCHEMA, "relation", "ispartof", Item.ANY).length == 0;
    }
}
