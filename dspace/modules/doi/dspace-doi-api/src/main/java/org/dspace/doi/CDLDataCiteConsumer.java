package org.dspace.doi;

import org.apache.log4j.Logger;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: 10/25/11
 * Time: 8:39 AM
 * To change this template use File | Settings | File Templates.
 */
public class CDLDataCiteConsumer implements Consumer {

    private static Logger log = Logger.getLogger(CDLDataCiteConsumer.class);

    public void initialize() throws Exception {}

    public void finish(Context ctx) throws Exception {}

    public void end(Context ctx) throws Exception {}


    public void consume(Context ctx, Event event) throws Exception {
        int st = event.getSubjectType();
        int et = event.getEventType();

        try {
            ctx = new Context();
            ctx.turnOffAuthorisationSystem();
            switch (st) {

                // When an Item is updated, execute the update against DataCite
                case Constants.ITEM: {
                    if (et == Event.MODIFY) {
                        Item item = (Item) event.getSubject(ctx);
                        if (item != null && item.isArchived()){
			    log.debug("updating DataCite settings for " + item.getHandle() + ", internal itemid=" + item.getID());
                            CDLDataCiteService dataCiteService = new CDLDataCiteService();

                            String doi = getDoiValue(item);

                            Map<String, String> metadatalist = dataCiteService.createMetadataList(item);
                            String response = dataCiteService.update(doi, null, metadatalist);

                            if(response.contains("bad request") || response.contains("BAD REQUEST") || response.contains("UNAUTHORIZED")){
                                dataCiteService.emailException(response, doi, "update");
                            }
                            else if(!response.contains("OK")){
                                dataCiteService.emailException("Verified generic error.", doi, "update");
                            }

                            if(response.contains("error"))
                                log.error("Problem during the Item synchronization against DataCite : " + response);

                            ctx.commit();
                        }
                    }
                    break;
                }
            }
        }
        catch (Exception e) {
            ctx.abort();
	    log.error("Problem updating DataCite settings for an item based on event " + event, e);
        }
        finally {
            ctx.complete();
        }

    }

    private static String getDoiValue(Item item) {
        DCValue[] doiVals = item.getMetadata("dc", "identifier", null, Item.ANY);
        if (doiVals != null && 0 < doiVals.length) {
            return doiVals[0].value;
        }
        return null;

    }





}
