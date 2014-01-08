/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflow.actions.processingaction;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.*;
import org.dspace.paymentsystem.PaymentSystemService;
import org.dspace.paymentsystem.PaypalService;
import org.dspace.paymentsystem.ShoppingCart;
import org.dspace.paymentsystem.Voucher;
import org.dspace.utils.DSpace;
import org.dspace.workflow.*;
import org.dspace.workflow.actions.ActionResult;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;


/**
 *  This action completes the final payment if the shopping cart has a valid
 *  transaction id present. In the event htat the transaction should fail, this step will
 *  forward the user to the ReAuthorizationPayment step, which will provide them with
 *  an opportunity to initiate a new payment.
 *
 * @author Mark Diggory, mdiggory at atmire.com
 * @author Fabio Bolognesi, fabio at atmire.com
 * @author Lantian Gai, lantian at atmire.com
 */
public class FinalPaymentAction extends ProcessingAction {

    private static Logger log = Logger.getLogger(DryadReviewAction.class);



    @Override
    public void activate(Context c, WorkflowItem wfItem) {}

    @Override
    public ActionResult execute(Context c, WorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {
	int itemID =  wfi.getItem().getID();
	log.info("Verifying payment status of Item " + itemID);
	
        try{
	    PaymentSystemService paymentSystemService = new DSpace().getSingletonService(PaymentSystemService.class);   
	    ShoppingCart shoppingCart = paymentSystemService.getShoppingCartByItemId(c,itemID);

	    // if fee waiver is in place, transaction is paid
	    if(shoppingCart.getCountry() != null && shoppingCart.getCountry().length() > 0) {
		log.info("processed fee waiver for Item " + itemID + ", country = " + shoppingCart.getCountry());
		return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
	    }

	    // if journal-based subscription is in place, transaction is paid
	    if(shoppingCart.getJournalSub()) {
		log.info("processed journal subscription for Item " + itemID + ", journal = " + shoppingCart.getJournal());
		return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
	    }

	    // if a valid voucher is in place, transaction is paid
	    Voucher voucher = Voucher.findById(c,shoppingCart.getVoucher());
	    log.debug("voucher is " + voucher);
	    if(voucher != null) {
		log.debug("voucher status " + voucher.getStatus());
		if(voucher.getStatus().equals(Voucher.STATUS_OPEN)) {
		    voucher.setStatus(Voucher.STATUS_USED);
		    voucher.update();
		    c.commit();      
		    log.info("processed voucher for Item " + itemID + ", voucherID = " + voucher.getID());
		    return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
		}
	    }
			  
        if(paymentSystemService.hasDiscount(c,shoppingCart,""))
        {
            //if any type of discount exist the transaction is paid
            shoppingCart.setStatus(ShoppingCart.STATUS_COMPLETED);
            Date date= new Date();
            if(shoppingCart.getOrderDate()==null)
            {
                shoppingCart.setOrderDate(date);
            }
            shoppingCart.setPaymentDate(date);
            shoppingCart.update();
        }
	    
	    // process payment via PayPal
            PaypalService paypalService = new DSpace().getSingletonService(PaypalService.class);
            if(paypalService.submitReferenceTransaction(c,wfi,request)){
		log.info("processed PayPal payment for Item " + itemID);
                return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
            }


        } catch (Exception e){
            log.error(e.getMessage(),e);
        }

        //Send us to the re authorization of paypal payment
	log.info("no payment processded for Item " + itemID + ", sending to revalidation step");
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, 1);

    }
}
