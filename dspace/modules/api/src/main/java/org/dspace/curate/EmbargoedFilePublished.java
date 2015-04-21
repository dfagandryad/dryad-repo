/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.util.Date;
import java.text.DateFormat;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

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
 * ItemsInReview reports on the status of items in the review workflow.
 * /opt/dryad/bin/dspace curate -v -t embargoedfilepublished -i 10255/3 -r - >~/temp/embargoedfilepublished.csv
 * cat ~/temp/embargoedfilepublished.csv 
 *
 * The task succeeds if it was able to calculate the correct result.
 *
 * Input: a collection (any collection)
 * Output: a CSV indicating simple information about the data packages that are in review
 *
 * @author Debra Fagan
 */
@Distributive
public class EmbargoedFilePublished extends AbstractCurationTask {

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
     * Perform - Distributes a task through a DSpace container
     * 
     * @param dso
     * @throws IOException
     */
    @Override
    public int perform(DSpaceObject dso) throws IOException {

        report(itemID + ", " + publicationName + ", " + lastModificationDate);        
        distribute(dso);        
        return Curator.CURATE_SUCCESS;
    }    

    
    


    /**
     * Performs task upon an Item. 
     * 
     * @param item
     * @throws SQLException
     * @throws IOException
     */
    protected void performItem(Item item) throws SQLException, IOException
    {
	// get embargo type
	String emType = "none";
	DCValue[] vals = item.getMetadata("dc.type.embargo");
	if (vals.length == 0) {
	    // there is no type set; check if a date was set. If a date is set, the embargo was "oneyear" and was deleted.
	    DCValue[] emDateVals = item.getMetadata("dc.date.embargoedUntil");
	    if(emDateVals.length != 0) {
			String emDate = emDateVals[0].value;
			if(emDate != null && !emDate.equals("")) {
		    	emType = "oneyear";
			}
	    }
	} else {
	    // there is a type set, so use it
	    emType = vals[0].value;
	}
	report(itemID + ", " + publicationName + ", " + lastModificationDate);
	// clean up the DSpace cache so we don't use excessive memory
	item.decache();
    }



    
    /** returns true if the date given is after today's date and false if it is not */
	public boolean futureDate(String someDate) {
	
        boolean future = false;
        
      	try {
        	if (new SimpleDateFormat("yyyy-MM-dd").parse(someDate).after(new Date())) {
        		future = true;
        	}
      	} catch (ParseException e) {}

        return future;
	}
	


}


