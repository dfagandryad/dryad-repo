package org.dspace.workflow;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Bram De Schouwer
 *
 * This class has been ajusted to support some extra methods including:
 *  findAll()
 *  findByItemId()
 */
public class WorkflowItem implements InProgressSubmission {

    /*
     * The current step in the workflow system in which this workflow item is present
     */
    private static Logger log = Logger.getLogger(WorkflowItem.class);

    private Collection collection;

    private Item item;

    private TableRow wfRow;

    private Context ourContext;


    /*
     * In the case where multiple epersons can claim and perform an action, this map will represent the progress
     * of each of these epersons in the step
     */
//    private ArrayList<StepRecord> activeSteps;



    WorkflowItem(Context context, TableRow row) throws SQLException, AuthorizeException, IOException {
        ourContext = context;
        wfRow = row;
 //       activeSteps = new ArrayList<StepRecord>();

        item = Item.find(context, wfRow.getIntColumn("item_id"));
        collection = Collection.find(context, wfRow.getIntColumn("collection_id"));
        // Cache ourselves
        context.cache(this, row.getIntColumn("workflow_id"));
//        initialize();
//        update();
    }

    /**
     * Get a workflow item from the database. The item, collection and submitter
     * are loaded into memory.
     *
     * @param context
     *            DSpace context object
     * @param id
     *            ID of the workspace item
     *
     * @return the workflow item, or null if the ID is invalid.
     */
    public static WorkflowItem find(Context context, int id)
            throws SQLException, AuthorizeException, IOException {
        // First check the cache
        WorkflowItem fromCache = (WorkflowItem) context.fromCache(
                WorkflowItem.class, id);

        if (fromCache != null)
        {
            return fromCache;
        }

        TableRow row = DatabaseManager.find(context, "workflowitem", id);
//        TableRow row = DatabaseManager.querySingle(context, "SELECT * FROM workflowitem WHERE item_id= "+id);

        if (row == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "find_workflow_item",
                        "not_found,workflow_id=" + id));
            }

            return null;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "find_workflow_item",
                        "workflow_id=" + id));
            }

            return new WorkflowItem(context, row);
        }
    }
    /*
     * Returns all workflow items submitted by an eperson
     */
    public static WorkflowItem[] findByEPerson(Context context, EPerson ep)
            throws SQLException, AuthorizeException, IOException {
            List wfItems = new ArrayList();

            TableRowIterator tri = DatabaseManager.queryTable(context, "workflowitem",
                    "SELECT workflowitem.* FROM workflowitem, item WHERE " +
                    "workflowitem.item_id=item.item_id AND " +
                    "item.submitter_id= ? " +
                    "ORDER BY workflowitem.workflow_id",
                    ep.getID());

            while (tri.hasNext())
            {
                TableRow row = tri.next();

                // Check the cache
                WorkflowItem wi = (WorkflowItem) context.fromCache(
                        WorkflowItem.class, row.getIntColumn("workflow_id"));

                if (wi == null)
                {
                    wi = new WorkflowItem(context, row);
                }

                wfItems.add(wi);
            }

            tri.close();

            WorkflowItem[] wfArray = new WorkflowItem[wfItems.size()];
            wfArray = (WorkflowItem[]) wfItems.toArray(wfArray);

            return wfArray;
        }



    /**
     * return all workflowitems
     *
     * @param c  active context
     * @return WorkflowItem [] of all workflows in system
     */
    public static WorkflowItem[] findAll(Context c) throws SQLException, AuthorizeException, IOException {
        List wfItems = new ArrayList();
        TableRowIterator tri = DatabaseManager.queryTable(c, "workflowitem",
                "SELECT * FROM workflowitem");

        // make a list of workflow items
        while (tri.hasNext())
        {
            TableRow row = tri.next();
            WorkflowItem wi = new WorkflowItem(c, row);
            wfItems.add(wi);
        }

        tri.close();

        WorkflowItem[] wfArray = new WorkflowItem[wfItems.size()];
        wfArray = (WorkflowItem[]) wfItems.toArray(wfArray);

        return wfArray;
    }


    public static WorkflowItem findByItemId(Context context, int itemId) throws SQLException, AuthorizeException, IOException {
        TableRow row = DatabaseManager.querySingleTable(context, "workflowitem", "SELECT * FROM workflowitem WHERE item_id= ?", itemId);

        if (row == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "find_workflow_item_by_itemid",
                        "not_found,item_id=" + itemId));
            }

            return null;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "find_workflow_item_by_itemid",
                        "item_id=" + itemId));
            }

            return new WorkflowItem(context, row);
        }

    }


    /**
     * Update the workflow item, including the unarchived item.
     */
    public void update() throws SQLException, IOException, AuthorizeException {
        // FIXME check auth
        log.info(LogManager.getHeader(ourContext, "update_workflow_item",
                "workflow_item_id=" + getID()));

        // Update the item
        item.update();

        // Update ourselves
        DatabaseManager.update(ourContext, wfRow);
    }


    /**
     * Get the internal ID of this workflow item
     *
     * @return the internal identifier
     */
    public int getID()
    {
        return wfRow.getIntColumn("workflow_id");
    }


    public Collection getCollection(){
        return this.collection;
    }

    public Item getItem()
    {
        return item;
    }

    public EPerson getSubmitter() throws SQLException
    {
        return item.getSubmitter();
    }

    public boolean hasMultipleFiles()
    {
        return wfRow.getBooleanColumn("multiple_files");
    }

    public void setMultipleFiles(boolean b)
    {
        wfRow.setColumn("multiple_files", b);
    }

    public boolean hasMultipleTitles()
    {
        return wfRow.getBooleanColumn("multiple_titles");
    }

    public void setMultipleTitles(boolean b)
    {
        wfRow.setColumn("multiple_titles", b);
    }

    public boolean isPublishedBefore()
    {
        return wfRow.getBooleanColumn("published_before");
    }

    public void setPublishedBefore(boolean b)
    {
        wfRow.setColumn("published_before", b);
    }

    /**
     * delete the WorkflowItem, retaining the Item
     */
    public void deleteWrapper() throws SQLException, IOException,
            AuthorizeException
    {
        // Remove from cache
        ourContext.removeCached(this, getID());

        //TODO: delete any pending tasks
        WorkflowManager.deleteAllTasks(ourContext, this);

        // FIXME - auth?
        DatabaseManager.delete(ourContext, wfRow);
    }
}
