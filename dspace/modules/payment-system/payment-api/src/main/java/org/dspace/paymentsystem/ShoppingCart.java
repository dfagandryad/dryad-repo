/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.paymentsystem;


import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the core Shopping Cart Domain Class
 *
 * @author Mark Diggory, mdiggory at atmire.com
 * @author Fabio Bolognesi, fabio at atmire.com
 * @author Lantian Gai, lantian at atmire.com
 */
public class ShoppingCart {
    /** The e-mail field (for sorting) */
    public static final int CART_ID = 1;

    /** The last name (for sorting) */
    public static final int EXPIRATION = 2;

    /** The e-mail field (for sorting) */
    public static final int STATUS = 3;

    /** The netid field (for sorting) */
    public static final int DEPOSITOR = 4;

    /** The e-mail field (for sorting) */
    public static final int ITEM = 5;

    /** The e-mail field (for sorting) */
    public static final int CURRENCY = 6;

    /** The e-mail field (for sorting) */
    public static final int COUNTRY = 7;

    /** The e-mail field (for sorting) */
    public static final int VOUCHER = 8;

    /** The e-mail field (for sorting) */
    public static final int TOTAL = 9;

    public static final int TRANSACTION_ID = 10;

    public static final int SECURETOKEN = 11;

    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_OPEN = "open";
    public static final String STATUS_DENIlED = "deniled";
    public static final String STATUS_VERIFIED = "verified";

    public static final String COUNTRY_US = "us";
    public static final String CURRENCY_US = "dollar";

    public static final String FREE = "true";

    public static final String COUNTRYFREE = "free";
    public static final String COUNTRYNOTFREE = "not_free";
    /** log4j logger */
    private static Logger log = Logger.getLogger(ShoppingCart.class);

    /** Our context */
    private Context myContext;

    /** The row in the table representing this transaction */
    private TableRow myRow;

    /** Flag set when data is modified, for events */
    private boolean modified;

    /** Flag set when metadata is modified, for events */
    private boolean modifiedMetadata;


    ShoppingCart(Context context, TableRow row)
    {
        myContext = context;
        myRow = row;
        // Cache ourselves
        context.cache(this, row.getIntColumn("cart_id"));
        modified = false;
        modifiedMetadata = false;
    }

    public String getHandle(){
        // No Handles for
        return null;
    }

    public String getName()
    {
        return null;
    }

    /**
     * Get the shoppingcart internal identifier
     *
     * @return the internal identifier
     */
    public int getID()
    {
        return myRow.getIntColumn("cart_id");
    }


    /**
     * Get the depositor
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public int getDepositor()
    {
        return myRow.getIntColumn("depositor");
    }

    /**
     * Get the item
     *
     * @return int code (or null if the column is an SQL NULL)
     */
    public int getItem()
    {
        return myRow.getIntColumn("item");
    }


    /**
     * Get the expiration date
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public String getExpiration()
    {
        return myRow.getStringColumn("expiration");
    }

    /**
     * Get the CURRENCY
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public String getCurrency()
    {
        return myRow.getStringColumn("currency");
    }

    /**
     * Get the Country
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public String getCountry()
    {
        return myRow.getStringColumn("country");
    }

    /**
     * Get the Status
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public String getStatus()
    {
        return myRow.getStringColumn("status");
    }

    /**
     * Get the VOUCHER
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public String getVoucher()
    {
        return myRow.getStringColumn("voucher");
    }

    /**
     * Get the payflow_id
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public String getTransactionId()
    {
        return myRow.getStringColumn("transaction_id");
    }

    /**
     * Set the payflow_id
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public void setTransactionId(String id)
    {

        if(id==null)
        {
           myRow.setColumnNull("transaction_id");
        }
        else{
        myRow.setColumn("transaction_id",id);
        }
        modified = true;
    }
    /**
     * Set the depositor
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public void setDepositor(Integer eperson)
    {
        if(eperson==null)
        {
            myRow.setColumnNull("depositor");
        }
        else{
        myRow.setColumn("depositor",eperson);
        }
        modified = true;
    }

    /**
     * Set the item
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public void setItem(Integer itemId)
    {
        if(itemId==null)
        {
            myRow.setColumnNull("item");
        }
        else{
        myRow.setColumn("item", itemId);
        }
        modified = true;
    }


    /**
     * Set the expiration date
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public void setExpiration(String expiration)
    {
        if(expiration==null)
        {
            myRow.setColumnNull("expiration");
        }
        else{
        myRow.setColumn("expiration",expiration);
        }
        modified = true;

    }

    /**
     * Set the CURRENCY
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public void setCurrency(String currency)
    {
        if(currency==null)
        {
            myRow.setColumnNull("currency");
        }
        else{
        myRow.setColumn("currency",currency);
        }
        modified = true;
    }

    /**
     * Set the Country
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public void setCountry(String country)
    {
        if(country==null)
        {
            myRow.setColumnNull("country");
        }
        else{
        myRow.setColumn("country",country);
        }
        modified = true;
    }

    /**
     * Set the Status
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public void setStatus(String status)
    {
        if(status==null)
        {
            myRow.setColumnNull("status");
        }
        else{
        myRow.setColumn("status",status);
        }
        modified = true;
    }

    /**
     * Set the VOUCHER
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public void setVoucher(String voucher)
    {
        if(voucher==null)
        {
            myRow.setColumnNull("voucher");
        }
        else{
        myRow.setColumn("voucher",voucher);
        }
    }


    /**
     * Update the payment
     */
    public void update() throws SQLException
    {

        DatabaseManager.update(myContext, myRow);

        log.info(LogManager.getHeader(myContext, "update_payment",
                "payment_id=" + getID()));

        if (modified)
        {
            modified = false;
        }
        if (modifiedMetadata)
        {
            modifiedMetadata = false;
        }
    }

    public void setModified(Boolean i){

        this.modified=i;

    }

    /**
     * Create a new shoppingcart
     *
     * @param context
     *            DSpace context object
     */
    public static ShoppingCart create(Context context,DSpaceObject dso) throws SQLException,
            AuthorizeException
    {
        // authorized?
        try{
            AuthorizeManager.authorizeAction(context,dso, Constants.WRITE);
        }catch (AuthorizeException e)
        {
            throw new AuthorizeException(
                    "You must be an admin to create a ShoppingCart");
        }

        // Create a table row
        TableRow row = DatabaseManager.create(context, "shoppingcart");

        ShoppingCart e = new ShoppingCart(context, row);

        log.info(LogManager.getHeader(context, "create_shoppingcart", "cart_id="
                + e.getID()));


        return e;
    }

    /**
     * Delete an simpleproperty
     *
     */
    public void delete() throws SQLException, AuthorizeException,
            PaymentSystemException
    {
        // authorized?
        if (!AuthorizeManager.isAdmin(myContext))
        {
            throw new AuthorizeException(
                    "You must be an admin to delete an shoppingcart");
        }

        // Remove from cache
        myContext.removeCached(this, getID());

        // Remove ourself
        DatabaseManager.delete(myContext, myRow);

        log.info(LogManager.getHeader(myContext, "delete_shoppingcart",
                "cart_id=" + getID()));
    }

    public static ArrayList<ShoppingCart> findAllByEpeople(Context context, int epeopleId)
            throws SQLException
    {

        String   s = "cart_id";

        // NOTE: The use of 's' in the order by clause can not cause an SQL
        // injection because the string is derived from constant values above.
        TableRowIterator rows = DatabaseManager.query(context,
                "SELECT * FROM shoppingcart WHERE depositor = "+ epeopleId+ " ORDER BY "+s);

        try
        {
            List<TableRow> propertyRows = rows.toList();

            ArrayList<ShoppingCart> transactions = new ArrayList<ShoppingCart>();

            for (int i = 0; i < propertyRows.size(); i++)
            {
                TableRow row = (TableRow) propertyRows.get(i);

                // First check the cache
                ShoppingCart fromCache = (ShoppingCart) context.fromCache(ShoppingCart.class, row
                        .getIntColumn("cart_id"));

                if (fromCache != null)
                {
                    transactions.add(fromCache);
                }
                else
                {
                    ShoppingCart newProperty = new ShoppingCart(context, row);
                    transactions.add(newProperty);
                }
            }

            return transactions;
        }
        finally
        {
            if (rows != null)
            {
                rows.close();
            }
        }
    }

    public static ArrayList<ShoppingCart> findAllByItem(Context context, int itemId)
            throws SQLException
    {

        String   s = "cart_id";

        // NOTE: The use of 's' in the order by clause can not cause an SQL
        // injection because the string is derived from constant values above.
        TableRowIterator rows = DatabaseManager.queryTable(context, "shoppingcart", "SELECT * FROM shoppingcart WHERE item = "+ itemId+ " ORDER BY "+s);

        try
        {
            List<TableRow> propertyRows = rows.toList();

            ArrayList<ShoppingCart> transactions = new ArrayList<ShoppingCart>();

            for (int i = 0; i < propertyRows.size(); i++)
            {
                TableRow row = (TableRow) propertyRows.get(i);

                // First check the cache
                ShoppingCart fromCache = (ShoppingCart) context.fromCache(ShoppingCart.class, row
                        .getIntColumn("cart_id"));

                if (fromCache != null)
                {
                    transactions.add(fromCache);
                }
                else
                {
                    ShoppingCart newProperty = new ShoppingCart(context, row);
                    transactions.add(newProperty);
                }
            }

            return transactions;
        }
        finally
        {
            if (rows != null)
            {
                rows.close();
            }
        }
    }
    public static ShoppingCart find(Context context, int cartId)
            throws SQLException
    {

        String   s = "cart_id";

        // NOTE: The use of 's' in the order by clause can not cause an SQL
        // injection because the string is derived from constant values above.
        TableRowIterator rows = DatabaseManager.queryTable(context, "shoppingcart", "SELECT * FROM shoppingcart WHERE cart_id = "+ cartId+ "limit 1");

        try
        {
            List<TableRow> propertyRows = rows.toList();

            ArrayList<ShoppingCart> transactions = new ArrayList<ShoppingCart>();

            for (int i = 0; i < propertyRows.size(); i++)
            {
                TableRow row = (TableRow) propertyRows.get(i);

                // First check the cache
                ShoppingCart fromCache = (ShoppingCart) context.fromCache(ShoppingCart.class, row
                        .getIntColumn("cart_id"));

                if (fromCache != null)
                {
                    transactions.add(fromCache);
                }
                else
                {
                    ShoppingCart newProperty = new ShoppingCart(context, row);
                    transactions.add(newProperty);
                }
            }

            return transactions.get(0);
        }
        finally
        {
            if (rows != null)
            {
                rows.close();
            }
        }
    }

    /**
     * Find the transaction by its id.
     *
     * @return Transaction, or {@code null} if none such exists.
     */
    public static ShoppingCart findByTransactionId(Context context, Integer id)
            throws SQLException
    {
        if (id == null)
        {
            return null;
        }

        // All name addresses are stored as lowercase, so ensure that the name address is lowercased for the lookup
        TableRow row = DatabaseManager.findByUnique(context, "shoppingcart",
                "cart_id", id);

        if (row == null)
        {
            return null;
        }
        else
        {
            // First check the cache
            ShoppingCart fromCache = (ShoppingCart) context.fromCache(ShoppingCart.class, row
                    .getIntColumn("cart_id"));

            if (fromCache != null)
            {
                return fromCache;
            }
            else
            {
                return new ShoppingCart(context, row);
            }
        }
    }

    public boolean getModified(){
        return this.modified;
    }

    public double getTotal(){
        return myRow.getDoubleColumn("total");
    }
    public void setTotal(double total){
        myRow.setColumn("total",total);
    }



    public String getSecureToken()
    {
        return myRow.getStringColumn("securetoken");
    }


    /**
     * return type found in Constants
     */
    public void setSecureToken(String secureToken)
    {
        if(secureToken==null)
        {
            myRow.setColumnNull("securetoken");
        }
        else{
            myRow.setColumn("securetoken",secureToken);
        }
        modified = true;
    }


    /**
     * return type found in Constants
     */
    public static ShoppingCart findBySecureToken(Context context,String secureToken) throws SQLException
    {
        if(secureToken==null)
        {
            return null;
        }
        else
        {
            // All name addresses are stored as lowercase, so ensure that the name address is lowercased for the lookup
            TableRow row = DatabaseManager.findByUnique(context, "shoppingcart",
                    "securetoken", secureToken);

            if (row == null)
            {
                return null;
            }
            else
            {
                // First check the cache
                ShoppingCart fromCache = (ShoppingCart) context.fromCache(ShoppingCart.class, row
                        .getIntColumn("cart_id"));

                if (fromCache != null)
                {
                    return fromCache;
                }
                else
                {
                    return new ShoppingCart(context, row);
                }
            }
        }
    }

    public static ShoppingCart[] findAll(Context context)
            throws SQLException
    {

        TableRowIterator rows = DatabaseManager.query(context,
                "SELECT * FROM shoppingcart order by cart_id");

        try
        {
            List<TableRow> propertyRows = rows.toList();

            ShoppingCart[] shoppingCarts = new ShoppingCart[propertyRows.size()];

            for (int i = 0; i < propertyRows.size(); i++)
            {
                TableRow row = (TableRow) propertyRows.get(i);

                // First check the cache
                ShoppingCart fromCache = (ShoppingCart) context.fromCache(ShoppingCart.class, row
                        .getIntColumn("cart_id"));

                if (fromCache != null)
                {
                    shoppingCarts[i] = fromCache;
                }
                else
                {
                    shoppingCarts[i] = new ShoppingCart(context, row);
                }
            }

            return shoppingCarts;
        }
        finally
        {
            if (rows != null)
            {
                rows.close();
            }
        }
    }
    public static ShoppingCart[] search(Context context, String query)
            throws SQLException
    {
        return search(context, query, -1, -1);
    }

    public static ShoppingCart[] search(Context context, String query, int offset, int limit)
            throws SQLException
    {
        String params = "%"+query.toLowerCase()+"%";
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append("SELECT * FROM shoppingcart WHERE cart_id = ? OR ");
        queryBuf.append("LOWER(status) LIKE LOWER(?) OR LOWER(transaction_id) LIKE LOWER(?) OR LOWER(country) LIKE LOWER(?) ORDER BY cart_id ASC ");

        // Add offset and limit restrictions - Oracle requires special code
        if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
        {
            // First prepare the query to generate row numbers
            if (limit > 0 || offset > 0)
            {
                queryBuf.insert(0, "SELECT /*+ FIRST_ROWS(n) */ rec.*, ROWNUM rnum  FROM (");
                queryBuf.append(") ");
            }

            // Restrict the number of rows returned based on the limit
            if (limit > 0)
            {
                queryBuf.append("rec WHERE rownum<=? ");
                // If we also have an offset, then convert the limit into the maximum row number
                if (offset > 0)
                {
                    limit += offset;
                }
            }

            // Return only the records after the specified offset (row number)
            if (offset > 0)
            {
                queryBuf.insert(0, "SELECT * FROM (");
                queryBuf.append(") WHERE rnum>?");
            }
        }
        else
        {
            if (limit > 0)
            {
                queryBuf.append(" LIMIT ? ");
            }

            if (offset > 0)
            {
                queryBuf.append(" OFFSET ? ");
            }
        }

        String dbquery = queryBuf.toString();

        // When checking against the shoppingcart-id, make sure the query can be made into a number
        Integer int_param;
        try {
            int_param = Integer.valueOf(query);
        }
        catch (NumberFormatException e) {
            int_param = Integer.valueOf(-1);
        }

        // Create the parameter array, including limit and offset if part of the query
        Object[] paramArr = new Object[] {int_param,params,params,params};
        if (limit > 0 && offset > 0)
        {
            paramArr = new Object[]{int_param, params, params, params, limit, offset};
        }
        else if (limit > 0)
        {
            paramArr = new Object[]{int_param, params, params, params, limit};
        }
        else if (offset > 0)
        {
            paramArr = new Object[]{int_param, params, params, params, offset};
        }

        // Get all the shoppingcart that match the query
        TableRowIterator rows = DatabaseManager.query(context,
                dbquery, paramArr);
        try
        {
            List<TableRow> shoppingcartRows = rows.toList();
            ShoppingCart[] shoppingcart = new ShoppingCart[shoppingcartRows.size()];

            for (int i = 0; i < shoppingcartRows.size(); i++)
            {
                TableRow row = (TableRow) shoppingcartRows.get(i);

                // First check the cache
                ShoppingCart fromCache = (ShoppingCart) context.fromCache(ShoppingCart.class, row
                        .getIntColumn("cart_id"));

                if (fromCache != null)
                {
                    shoppingcart[i] = fromCache;
                }
                else
                {
                    shoppingcart[i] = new ShoppingCart(context, row);
                }
            }

            return shoppingcart;
        }
        finally
        {
            if (rows != null)
            {
                rows.close();
            }
        }
    }



}
