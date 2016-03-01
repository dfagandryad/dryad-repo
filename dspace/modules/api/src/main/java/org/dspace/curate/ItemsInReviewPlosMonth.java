/**
 * 
 * /opt/dryad/bin/dspace curate -v -t itemsinreviewplosmonth -i 10255/3 -r - >~/temp/itemsinreviewplosmonth.csv
 * 
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.lang.Math;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.io.IOException;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.Constants;

import org.dspace.workflow.WorkflowItem;
import org.datadryad.api.DryadDataPackage;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

import org.apache.log4j.Logger;

/**
 * ItemsInReviewPlosMonth reports on the status of items in the review workflow.
 *
 * The task succeeds if it was able to calculate the correct result.
 *
 * Input: a collection (any collection)
 * Output: a CSV indicating simple information about the data packages that are in review
 *
 * @author Debra Fagan / Ryan Scherle
 */
@Distributive
public class ItemsInReviewPlosMonth extends AbstractCurationTask {

    private static Logger log = Logger.getLogger(FileSimpleStats.class);
    private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
    Context context;

    @Override 
    public void init(Curator curator, String taskId) throws IOException {
        super.init(curator, taskId);

        try {
            context = new Context();
        } catch (SQLException e) {
            log.fatal("Cannot initialize database connection", e);
        }
    }
    

    /**
     * returns the number of days between one date and secondDate, which
     * is passed in
     */
    public static int numDaysBetween(Date firstDate, Date secondDate) {

        long firstDateMS = firstDate.getTime();
        long secondDateMS = secondDate.getTime();

        long timeBetweenDatesMS = Math.abs(firstDateMS - secondDateMS);
        long daysBetweenDates = timeBetweenDatesMS / (24 * 60 * 60 * 1000);
        return (int) daysBetweenDates;
    }


 /**
     * returns the number of days between one date and secondDate, which
     * is passed in
     */
    public static int numDaysBetweenDates(Date firstDate, Date secondDate) {

        long firstDateMS = firstDate.getTime();
        long secondDateMS = secondDate.getTime();

        long timeBetweenDatesMS = Math.abs(firstDateMS - secondDateMS);
        long daysBetweenDates = timeBetweenDatesMS / (24 * 60 * 60 * 1000);
        return (int) daysBetweenDates;
    }


    
    /**
       Perform 
     **/
    @Override
    public int perform(DSpaceObject dso) throws IOException {
    
    String PUBNAME = "plosone";

    try {
        
            if (dso.getType() == Constants.COLLECTION) {
                // output headers for the CSV file that will be created by processing all items in this collection
                report("itemID, publicationName, lastModificationDate");

                // Iterate over the workflow "collection", calling this perform method on each item.
                // This bypasses the normal functionality of the curation task system, since items in
                // workflow don't yet belong to a real collection.
                WorkflowItem[] wfis = WorkflowItem.findAll(context);
                for(int i = 0; i < wfis.length; i++) {
                    perform(wfis[i].getItem());
                }

            } else if (dso.getType() == Constants.ITEM) {
                // determine whether this item is in the review workflow
                // workflow stage is stored in taskowner table
                DryadDataPackage dataPackage = new DryadDataPackage((Item)dso);
                log.debug("processing " + dataPackage.getItem().getID());
                WorkflowItem wfi = dataPackage.getWorkflowItem(context);
                if(wfi != null) {
                    log.debug(" -- is in workflow");
                    int workflowID = wfi.getID();
                    TableRow tr = DatabaseManager.querySingleTable(context,"taskowner", "SELECT * FROM taskowner WHERE workflow_item_id= ?", workflowID);
                    if(tr != null && tr.getStringColumn("step_id").equals("reviewStep")) {
                        log.debug(" -- is in review");
                        // report on the item

                        String publicationName = dataPackage.getPublicationName();
                        if (publicationName.toLowerCase().contains(PUBNAME)) {
                            Date todaysDate = new Date();
                            Date lastModificationDate = dataPackage.getItem().getLastModified();
                            int daysSincePlacedInReview = numDaysBetweenDates(todaysDate, lastModificationDate);
                            if daysSincePlacedInReview > 30 {
                                int itemID = dataPackage.getItem().getID();
                                report(itemID + ", " + publicationName + ", " + lastModificationDate);
                            }
                        }
                    }
                }
                
                // clean up the DSpace cache so we don't use excessive memory
                ((Item)dso).decache();
            }
        } catch (SQLException e) {
        log.fatal("Problem with database access", e);
    } catch (AuthorizeException e) {
            log.fatal("Problem with authorization", e);
        }
        
        return Curator.CURATE_SUCCESS;
    }    
}

