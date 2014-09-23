/*
 */
package org.datadryad.api;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.identifier.IdentifierException;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.workflow.WorkflowItem;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DryadDataPackage extends DryadObject {
    private static final String PACKAGES_COLLECTION_HANDLE_KEY = "stats.datapkgs.coll";

    private static final String PROVENANCE_SCHEMA = "dc";
    private static final String PROVENANCE_ELEMENT = "description";
    private static final String PROVENANCE_QUALIFIER = "provenance";
    private static final String PROVENANCE_LANGUAGE = "en";

    private static final String WORKFLOWITEM_TABLE = "workflowitem";
    private static final String WORKFLOWITEM_COLUMN_ITEMID = "item_id";
    private static final String WORKFLOWITEM_COLUMN_COLLECTIONID = "collection_id";

    private static final String PUBLICATION_NAME_SCHEMA = "prism";
    private static final String PUBLICATION_NAME_ELEMENT = "publicationName";
    private static final String PUBLICATION_NAME_QUALIFIER = null;

    private Set<DryadDataFile> dataFiles;
    private static Logger log = Logger.getLogger(DryadDataPackage.class);

    public DryadDataPackage(Item item) {
        super(item);
    }

    public static Collection getCollection(Context context) throws SQLException {
        String handle = ConfigurationManager.getProperty(PACKAGES_COLLECTION_HANDLE_KEY);
        return DryadObject.collectionFromHandle(context, handle);
    }

    public static DryadDataPackage create(Context context) throws SQLException {
        Collection collection = DryadDataPackage.getCollection(context);
        DryadDataPackage dataPackage = null;
        try {
            WorkspaceItem wsi = WorkspaceItem.create(context, collection, true);
            Item item = wsi.getItem();
            dataPackage = new DryadDataPackage(item);
            dataPackage.createIdentifier(context);
            dataPackage.addToCollectionAndArchive(collection);
            wsi.deleteWrapper();
            return dataPackage;
        } catch (IdentifierException ex) {
            log.error("Identifier exception creating a Data Package", ex);
        } catch (AuthorizeException ex) {
            log.error("Authorize exception creating a Data Package", ex);
        } catch (IOException ex) {
            log.error("IO exception creating a Data Package", ex);
        }
        return dataPackage;
    }

    public static DryadDataPackage createInWorkflow(Context context) throws SQLException {
        /*
         * WorkflowItems are normally created by WorkflowManager.start(),
         * but this method has a lot of side effects (activating steps, sending
         * emails) and generally heavyweight.
         * Instead we'll just create rows in the workflowitem table for now.
         */
        Collection collection = DryadDataPackage.getCollection(context);
        DryadDataPackage dataPackage = null;
        try {
            WorkspaceItem wsi = WorkspaceItem.create(context, collection, true);
            Item item = wsi.getItem();
            TableRow row = DatabaseManager.create(context, WORKFLOWITEM_TABLE);
            row.setColumn(WORKFLOWITEM_COLUMN_ITEMID, item.getID());
            row.setColumn(WORKFLOWITEM_COLUMN_COLLECTIONID, collection.getID());
            DatabaseManager.update(context, row);
            dataPackage = new DryadDataPackage(item);
            dataPackage.createIdentifier(context);
            wsi.deleteWrapper();
        } catch (IdentifierException ex) {
            log.error("Identifier exception creating a Data Package", ex);
        } catch (AuthorizeException ex) {
            log.error("Authorize exception creating a Data Package", ex);
        } catch (IOException ex) {
            log.error("IO exception creating a Data Package", ex);
        }
        return dataPackage;
    }

    public WorkflowItem getWorkflowItem(Context context) throws SQLException {
        try {
            return WorkflowItem.findByItemId(context, getItem().getID());
        } catch (AuthorizeException ex) {
            log.error("Authorize exception getting workflow item for data package", ex);
        } catch (IOException ex) {
            log.error("IO exception getting workflow item for data package", ex);
        }
        return null;
    }

    /**
     * Find any data packages containing the file by identifier. Used to prevent
     * files from appearing in multiple packages
     * @param context database context
     * @param dataFile a data file with an identifier
     * @return a set of data packages where dc.relation.haspart = the file's identifier
     * @throws SQLException
     */
    static Set<DryadDataPackage> getPackagesContainingFile(Context context, DryadDataFile dataFile) throws SQLException {
        Set<DryadDataPackage> packageSet = new HashSet<DryadDataPackage>();
        String fileIdentifier = dataFile.getIdentifier();
        if(fileIdentifier == null || fileIdentifier.length() == 0) {
            throw new IllegalArgumentException("Data file must have an identifier");
        }
        try {
            ItemIterator dataPackages = Item.findByMetadataField(context, RELATION_SCHEMA, RELATION_ELEMENT, RELATION_HASPART_QUALIFIER, fileIdentifier);
            while(dataPackages.hasNext()) {
                packageSet.add(new DryadDataPackage(dataPackages.next()));
            }
        } catch (AuthorizeException ex) {
            log.error("Authorize exception getting data packages for file", ex);
        } catch (IOException ex) {
            log.error("IO exception getting data packages for file", ex);
        }
        return packageSet;
    }

    static Set<DryadDataFile> getFilesInPackage(Context context, DryadDataPackage dataPackage) throws SQLException {
        // files and packages are linked by DOI
        Set<DryadDataFile> fileSet = new HashSet<DryadDataFile>();
        String packageIdentifier = dataPackage.getIdentifier();
        if(packageIdentifier == null || packageIdentifier.length() == 0) {
            throw new IllegalArgumentException("Data package must have an identifier");
        }
        try {
            ItemIterator dataFiles = Item.findByMetadataField(context, RELATION_SCHEMA, RELATION_ELEMENT, RELATION_ISPARTOF_QUALIFIER, packageIdentifier);
            while(dataFiles.hasNext()) {
                fileSet.add(new DryadDataFile(dataFiles.next()));
            }
        } catch (AuthorizeException ex) {
            log.error("Authorize exception getting files for data package", ex);
        } catch (IOException ex) {
            log.error("IO exception getting files for data package", ex);
        }
        return fileSet;
    }

    public Set<DryadDataFile> getDataFiles(Context context) throws SQLException {
        if(dataFiles == null) {
            // how are data files and packages linked? By DOI
            dataFiles = DryadDataPackage.getFilesInPackage(context, this);
        }
        return dataFiles;
    }

    void setHasPart(DryadDataFile dataFile) throws SQLException {
        String dataFileIdentifier = dataFile.getIdentifier();
        if(dataFileIdentifier == null || dataFileIdentifier.length() == 0) {
            throw new IllegalArgumentException("Data file must have an identifier");
        }
        this.getItem().addMetadata(RELATION_SCHEMA, RELATION_ELEMENT, RELATION_HASPART_QUALIFIER, null, dataFileIdentifier);
        try {
            this.getItem().update();
        } catch (AuthorizeException ex) {
            log.error("Authorize exception assigning package haspart file", ex);
        }
    }

    public void addDataFile(Context context, DryadDataFile dataFile) throws SQLException {
        dataFile.setDataPackage(context, this);
    }

    void clearDataFilesCache() {
        this.dataFiles = null;
    }

    /**
     * Removes the identifier for a data file from this package's
     * dc.relation.haspart metadata.
     * @param dataFile
     * @throws SQLException
     */
    public void removeDataFile(Context context, DryadDataFile dataFile) throws SQLException {
        String dataFileIdentifier = dataFile.getIdentifier();
        if(dataFileIdentifier == null) {
            throw new IllegalArgumentException("Data file must have an identifier");
        }

        // Get the metadata
        DCValue[] hasPartValues = getItem().getMetadata(RELATION_SCHEMA, RELATION_ELEMENT, RELATION_HASPART_QUALIFIER, Item.ANY);

        Integer indexOfFileIdentifier = indexOfValue(hasPartValues, dataFileIdentifier);
        if(indexOfFileIdentifier >= 0) {
            // remove that element from the array
            hasPartValues = (DCValue[]) ArrayUtils.remove(hasPartValues, indexOfFileIdentifier);
            // clear the metadata in the database
            getItem().clearMetadata(RELATION_SCHEMA, RELATION_ELEMENT, RELATION_HASPART_QUALIFIER, Item.ANY);
            // set them
            for(DCValue value : hasPartValues) {
                getItem().addMetadata(value.schema, value.element, value.qualifier, value.language, value.value, value.authority, value.confidence);
            }
            try {
                getItem().update();
            } catch (AuthorizeException ex) {
                log.error("Authorize exception removing data file from data package", ex);
            }
            dataFile.clearDataPackage(context);
        }
    }

    static Integer indexOfValue(final DCValue[] dcValues, final String value) {
        Integer foundIndex = -1;
        for(Integer index = 0;index < dcValues.length;index++) {
            if(dcValues[index].value.equals(value)) {
                foundIndex = index;
            }
        }
        return foundIndex;
    }


    public void setPublicationName(String publicationName) throws SQLException {
        getItem().clearMetadata(PUBLICATION_NAME_SCHEMA, PUBLICATION_NAME_ELEMENT, PUBLICATION_NAME_QUALIFIER, null);
        getItem().addMetadata(PUBLICATION_NAME_SCHEMA, PUBLICATION_NAME_ELEMENT, PUBLICATION_NAME_QUALIFIER, null, publicationName);
        try {
            getItem().update();
        } catch (AuthorizeException ex) {
            log.error("Authorize exception setting publication name", ex);
        }
    }

    /**
     * Generate a Dryad-formatted 'Submitted by ...' provenance string
     * @param date
     * @param submitterName
     * @param submitterEmail
     * @param provenanceStartId
     * @param bitstreamProvenanceMessage
     * @return
     */
    static String makeSubmittedProvenance(DCDate date, String submitterName,
            String submitterEmail, String provenanceStartId, String bitstreamProvenanceMessage) {
        StringBuilder builder = new StringBuilder();
        builder.append("Submitted by ");
        if(submitterName == null || submitterEmail == null) {
            builder.append("unknown (probably automated)");
        } else {
            builder.append(submitterName);
            builder.append(" (");
            builder.append(submitterEmail);
            builder.append(")");
        }
        builder.append(" on ");
        builder.append(date.toString());
        builder.append(" workflow start=");
        builder.append(provenanceStartId);
        builder.append("\n");
        builder.append(bitstreamProvenanceMessage);
        return builder.toString();
    }

    /**
     * Gets the most-recent provenance metadata beginning with
     * 'Submitted by '
     * @return the provenance information
     */
    public String getSubmittedProvenance() {
        String provenance = null;
        // Assumes metadata are ordered by place
        DCValue[] metadata = item.getMetadata(PROVENANCE_SCHEMA, PROVENANCE_ELEMENT, PROVENANCE_QUALIFIER, PROVENANCE_LANGUAGE);
        // find the last entry that starts with "Submitted by "
        ArrayUtils.reverse(metadata);
        for(DCValue dcValue : metadata) {
            if(dcValue.value.startsWith("Submitted by ")) {
                provenance = dcValue.value;
                break;
            }
        }
        return provenance;
    }

    /**
     * Adds Dryad-formatted 'Submitted by ...' metadata to a data package. Does
     * not remove existing provenance metadata.
     * @param date
     * @param submitterName
     * @param submitterEmail
     * @param provenanceStartId
     * @param bitstreamProvenanceMessage
     * @throws SQLException
     */
    public void addSubmittedProvenance(DCDate date, String submitterName,
            String submitterEmail, String provenanceStartId, String bitstreamProvenanceMessage) throws SQLException {
        String metadataValue = makeSubmittedProvenance(date, submitterName, submitterEmail, provenanceStartId, bitstreamProvenanceMessage);
        getItem().addMetadata(PROVENANCE_SCHEMA, PROVENANCE_ELEMENT, PROVENANCE_QUALIFIER, PROVENANCE_LANGUAGE, metadataValue);
        try {
            getItem().update();
        } catch (AuthorizeException ex) {
            log.error("Authorize exception adding submitted provenance", ex);
        }
    }

    @Override
    Set<DryadObject> getRelatedObjects(final Context context) throws SQLException {
        return new HashSet<DryadObject>(getDataFiles(context));
    }
}