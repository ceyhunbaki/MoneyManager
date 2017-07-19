package com.jgmoneymanager.database;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.jgmoneymanager.database.MoneyManagerProviderMetaData.AccountTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.BudgetCategoriesTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.BudgetTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.BudgetGoalsTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CategoryTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CurrRatesTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CurrencyTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.DebtsTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransactionsTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransactionStatusTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransferTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VAccountsViewMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VCategoriesViewMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VCurrRatesViewMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VDebtsViewMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VRatesToDefaultViewMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VTransAccountViewMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VTransactionViewMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VTransferViewMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.PaymentMethodsTableMetaData;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.entity.MyApplication;
import com.jgmoneymanager.tools.Tools;

import java.util.Calendar;
import java.util.HashMap;

public class MoneyManagerProvider extends ContentProvider{
	 //Logging helper tag. No significance to providers. 
    //private static final String TAG = "MoneyManagerProvider"; 
 
    //Setup projection Map 
    //Projection maps are similar to "as" (column alias) construct 
    //in an sql statement where by you can rename the  
    //columns. 
	
	MyApplication myApp;

    private static HashMap<String, String> sCurrencyProjectionMap; 
    static  
    { 
        sCurrencyProjectionMap = new HashMap<String, String>(); 
        sCurrencyProjectionMap.put(CurrencyTableMetaData._ID, CurrencyTableMetaData._ID); 
         
        sCurrencyProjectionMap.put(CurrencyTableMetaData.NAME, CurrencyTableMetaData.NAME); 
        sCurrencyProjectionMap.put(CurrencyTableMetaData.SIGN, CurrencyTableMetaData.SIGN);
        sCurrencyProjectionMap.put(CurrencyTableMetaData.ISDEFAULT, CurrencyTableMetaData.ISDEFAULT);
		sCurrencyProjectionMap.put(CurrencyTableMetaData.SORTORDER, CurrencyTableMetaData.SORTORDER);
		sCurrencyProjectionMap.put(CurrencyTableMetaData.RESOURCEID, CurrencyTableMetaData.RESOURCEID);
         
        //created date, modified date 
        sCurrencyProjectionMap.put(CurrencyTableMetaData.CREATED_DATE, CurrencyTableMetaData.CREATED_DATE); 
        sCurrencyProjectionMap.put(CurrencyTableMetaData.MODIFIED_DATE, CurrencyTableMetaData.MODIFIED_DATE); 
    } 
    
    private static HashMap<String, String> sAccountProjectionMap; 
    static  
    { 
    	sAccountProjectionMap = new HashMap<String, String>(); 
    	sAccountProjectionMap.put(AccountTableMetaData._ID, AccountTableMetaData._ID); 
         
    	sAccountProjectionMap.put(AccountTableMetaData.NAME, AccountTableMetaData.NAME); 
    	sAccountProjectionMap.put(AccountTableMetaData.CURRID, AccountTableMetaData.CURRID); 
    	sAccountProjectionMap.put(AccountTableMetaData.DESCRIPTION, AccountTableMetaData.DESCRIPTION); 
    	sAccountProjectionMap.put(AccountTableMetaData.INITIALBALANCE, AccountTableMetaData.INITIALBALANCE); 
    	sAccountProjectionMap.put(AccountTableMetaData.ISDEFAULT, AccountTableMetaData.ISDEFAULT); 
    	sAccountProjectionMap.put(AccountTableMetaData.STATUS, AccountTableMetaData.STATUS);  
    	sAccountProjectionMap.put(AccountTableMetaData.SORTORDER, AccountTableMetaData.SORTORDER); 
         
    	sAccountProjectionMap.put(AccountTableMetaData.CREATED_DATE, AccountTableMetaData.CREATED_DATE); 
    	sAccountProjectionMap.put(AccountTableMetaData.MODIFIED_DATE, AccountTableMetaData.MODIFIED_DATE); 
    } 
    
    private static HashMap<String, String> sCategoryProjectionMap; 
    static  
    { 
    	sCategoryProjectionMap = new HashMap<String, String>(); 
    	sCategoryProjectionMap.put(CategoryTableMetaData._ID, CategoryTableMetaData._ID); 
         
    	sCategoryProjectionMap.put(CategoryTableMetaData.NAME, CategoryTableMetaData.NAME); 
    	sCategoryProjectionMap.put(CategoryTableMetaData.MAINID, CategoryTableMetaData.MAINID);
		sCategoryProjectionMap.put(CategoryTableMetaData.ISINCOME, CategoryTableMetaData.ISINCOME);
		sCategoryProjectionMap.put(CategoryTableMetaData.RESOURCEID, CategoryTableMetaData.RESOURCEID);

		sCategoryProjectionMap.put(AccountTableMetaData.CREATED_DATE, AccountTableMetaData.CREATED_DATE);
    	sCategoryProjectionMap.put(AccountTableMetaData.MODIFIED_DATE, AccountTableMetaData.MODIFIED_DATE); 
    } 
    
    private static HashMap<String, String> sCurrRatesProjectionMap; 
    static  
    { 
    	sCurrRatesProjectionMap = new HashMap<String, String>(); 
    	sCurrRatesProjectionMap.put(CurrRatesTableMetaData._ID, CurrRatesTableMetaData._ID); 
         
    	sCurrRatesProjectionMap.put(CurrRatesTableMetaData.FIRSTCURRID, CurrRatesTableMetaData.FIRSTCURRID); 
    	sCurrRatesProjectionMap.put(CurrRatesTableMetaData.SECONDCURRID, CurrRatesTableMetaData.SECONDCURRID); 
    	sCurrRatesProjectionMap.put(CurrRatesTableMetaData.VALUE, CurrRatesTableMetaData.VALUE); 
    	sCurrRatesProjectionMap.put(CurrRatesTableMetaData.RATEDATE, CurrRatesTableMetaData.RATEDATE); 
    	sCurrRatesProjectionMap.put(CurrRatesTableMetaData.NEXTRATEDATE, CurrRatesTableMetaData.NEXTRATEDATE); 
         
    	sCurrRatesProjectionMap.put(CurrRatesTableMetaData.CREATED_DATE, CurrRatesTableMetaData.CREATED_DATE); 
    	sCurrRatesProjectionMap.put(CurrRatesTableMetaData.MODIFIED_DATE, CurrRatesTableMetaData.MODIFIED_DATE); 
    } 
 
    private static HashMap<String, String> sVCurrRatesProjectionMap; 
    static  
    { 
    	sVCurrRatesProjectionMap = new HashMap<String, String>(); 
    	sVCurrRatesProjectionMap.put(VCurrRatesViewMetaData._ID, VCurrRatesViewMetaData._ID); 
    	
    	sVCurrRatesProjectionMap.put(VCurrRatesViewMetaData.FIRSTCURRID, VCurrRatesViewMetaData.FIRSTCURRID); 
    	sVCurrRatesProjectionMap.put(VCurrRatesViewMetaData.FIRSTCURRSIGN, VCurrRatesViewMetaData.FIRSTCURRSIGN); 
    	sVCurrRatesProjectionMap.put(VCurrRatesViewMetaData.SECONDCURRID, VCurrRatesViewMetaData.SECONDCURRID); 
    	sVCurrRatesProjectionMap.put(VCurrRatesViewMetaData.SECONDCURRSIGN, VCurrRatesViewMetaData.SECONDCURRSIGN); 
    	sVCurrRatesProjectionMap.put(VCurrRatesViewMetaData.VALUE, VCurrRatesViewMetaData.VALUE); 
    	sVCurrRatesProjectionMap.put(VCurrRatesViewMetaData.RATEDATE, VCurrRatesViewMetaData.RATEDATE); 
    	sVCurrRatesProjectionMap.put(VCurrRatesViewMetaData.NEXTRATEDATE, VCurrRatesViewMetaData.NEXTRATEDATE); 
    	sVCurrRatesProjectionMap.put(VCurrRatesViewMetaData.FIRSTISDEFAULT, VCurrRatesViewMetaData.FIRSTISDEFAULT); 
    	sVCurrRatesProjectionMap.put(VCurrRatesViewMetaData.SECONDISDEFAULT, VCurrRatesViewMetaData.SECONDISDEFAULT); 
    } 
 
    private static HashMap<String, String> sTransactionProjectionMap; 
    static  
    { 
    	sTransactionProjectionMap = new HashMap<String, String>(); 
    	sTransactionProjectionMap.put(TransactionsTableMetaData._ID, TransactionsTableMetaData._ID); 
         
    	sTransactionProjectionMap.put(TransactionsTableMetaData.ACCOUNTID, TransactionsTableMetaData.ACCOUNTID); 
    	sTransactionProjectionMap.put(TransactionsTableMetaData.CATEGORYID, TransactionsTableMetaData.CATEGORYID); 
    	sTransactionProjectionMap.put(TransactionsTableMetaData.TRANSDATE, TransactionsTableMetaData.TRANSDATE); 
    	sTransactionProjectionMap.put(TransactionsTableMetaData.AMOUNT, TransactionsTableMetaData.AMOUNT); 
    	sTransactionProjectionMap.put(TransactionsTableMetaData.BALANCE, TransactionsTableMetaData.BALANCE); 
    	sTransactionProjectionMap.put(TransactionsTableMetaData.TRANSTYPE, TransactionsTableMetaData.TRANSTYPE); 
    	sTransactionProjectionMap.put(TransactionsTableMetaData.DESCRIPTION, TransactionsTableMetaData.DESCRIPTION); 
    	sTransactionProjectionMap.put(TransactionsTableMetaData.TRANSFERID, TransactionsTableMetaData.TRANSFERID);
		sTransactionProjectionMap.put(TransactionsTableMetaData.CURRENCYID, TransactionsTableMetaData.CURRENCYID);
		sTransactionProjectionMap.put(TransactionsTableMetaData.PHOTO_PATH, TransactionsTableMetaData.PHOTO_PATH);
		sTransactionProjectionMap.put(TransactionsTableMetaData.STATUS, TransactionsTableMetaData.STATUS);
		sTransactionProjectionMap.put(TransactionsTableMetaData.PAYMENT_METHOD, TransactionsTableMetaData.PAYMENT_METHOD);

		sTransactionProjectionMap.put(TransactionsTableMetaData.CREATED_DATE, TransactionsTableMetaData.CREATED_DATE);
    	sTransactionProjectionMap.put(TransactionsTableMetaData.MODIFIED_DATE, TransactionsTableMetaData.MODIFIED_DATE); 
    } 
    
    private static HashMap<String, String> sTransferProjectionMap; 
    static  
    { 
    	sTransferProjectionMap = new HashMap<String, String>(); 
    	sTransferProjectionMap.put(TransferTableMetaData._ID, TransferTableMetaData._ID); 
         
    	sTransferProjectionMap.put(TransferTableMetaData.FIRSTACCOUNTID, TransferTableMetaData.FIRSTACCOUNTID); 
    	sTransferProjectionMap.put(TransferTableMetaData.SECONDACCOUNTID, TransferTableMetaData.SECONDACCOUNTID); 
    	sTransferProjectionMap.put(TransferTableMetaData.CATEGORYID, TransferTableMetaData.CATEGORYID); 
    	sTransferProjectionMap.put(TransferTableMetaData.TRANSDATE, TransferTableMetaData.TRANSDATE); 
    	sTransferProjectionMap.put(TransferTableMetaData.AMOUNT, TransferTableMetaData.AMOUNT); 
    	sTransferProjectionMap.put(TransferTableMetaData.REPEATTYPE, TransferTableMetaData.REPEATTYPE); 
    	sTransferProjectionMap.put(TransferTableMetaData.DESCRIPTION, TransferTableMetaData.DESCRIPTION); 
    	sTransferProjectionMap.put(TransferTableMetaData.PERIODEND, TransferTableMetaData.PERIODEND); 
    	sTransferProjectionMap.put(TransferTableMetaData.NEXTPAYMENT, TransferTableMetaData.NEXTPAYMENT); 
    	sTransferProjectionMap.put(TransferTableMetaData.CUSTOMINTERVAL, TransferTableMetaData.CUSTOMINTERVAL); 
    	sTransferProjectionMap.put(TransferTableMetaData.CURRENCYID, TransferTableMetaData.CURRENCYID);  
    	sTransferProjectionMap.put(TransferTableMetaData.REMINDER, TransferTableMetaData.REMINDER);
		sTransferProjectionMap.put(TransferTableMetaData.STATUS, TransferTableMetaData.STATUS);
		sTransferProjectionMap.put(TransferTableMetaData.TRANSACTION_STATUS, TransferTableMetaData.TRANSACTION_STATUS);
		sTransferProjectionMap.put(TransferTableMetaData.TRANSACTION_PAYMENT_METHOD, TransferTableMetaData.TRANSACTION_PAYMENT_METHOD);

		sTransferProjectionMap.put(TransferTableMetaData.CREATED_DATE, TransferTableMetaData.CREATED_DATE);
    	sTransferProjectionMap.put(TransferTableMetaData.MODIFIED_DATE, TransferTableMetaData.MODIFIED_DATE); 
    } 
    
    private static HashMap<String, String> sDebtsProjectionMap; 
    static  
    { 
    	sDebtsProjectionMap = new HashMap<String, String>(); 
    	sDebtsProjectionMap.put(DebtsTableMetaData._ID, DebtsTableMetaData._ID); 
         
    	sDebtsProjectionMap.put(DebtsTableMetaData.ISGIVEN, DebtsTableMetaData.ISGIVEN); 
    	sDebtsProjectionMap.put(DebtsTableMetaData.TRANSDATE, DebtsTableMetaData.TRANSDATE); 
    	sDebtsProjectionMap.put(DebtsTableMetaData.AMOUNT, DebtsTableMetaData.AMOUNT); 
    	sDebtsProjectionMap.put(DebtsTableMetaData.DESCRIPTION, DebtsTableMetaData.DESCRIPTION); 
    	sDebtsProjectionMap.put(DebtsTableMetaData.BACKDATE, DebtsTableMetaData.BACKDATE); 
    	sDebtsProjectionMap.put(DebtsTableMetaData.REMINDME, DebtsTableMetaData.REMINDME); 
    	sDebtsProjectionMap.put(DebtsTableMetaData.CURRENCY_ID, DebtsTableMetaData.CURRENCY_ID);
    	sDebtsProjectionMap.put(DebtsTableMetaData.STATUS, DebtsTableMetaData.STATUS); 
         
    	sDebtsProjectionMap.put(DebtsTableMetaData.CREATED_DATE, DebtsTableMetaData.CREATED_DATE); 
    	sDebtsProjectionMap.put(DebtsTableMetaData.MODIFIED_DATE, DebtsTableMetaData.MODIFIED_DATE); 
    }

	private static HashMap<String, String> sBudgetProjectionMap;
	static
	{
		sBudgetProjectionMap = new HashMap<String, String>();
		sBudgetProjectionMap.put(BudgetTableMetaData._ID, BudgetTableMetaData._ID);

		sBudgetProjectionMap.put(BudgetTableMetaData.FROM_DATE, BudgetTableMetaData.FROM_DATE);
		sBudgetProjectionMap.put(BudgetTableMetaData.TO_DATE, BudgetTableMetaData.TO_DATE);
		sBudgetProjectionMap.put(BudgetTableMetaData.INCOME, BudgetTableMetaData.INCOME);
		sBudgetProjectionMap.put(BudgetTableMetaData.CURRENCY_ID, BudgetTableMetaData.CURRENCY_ID);
		sBudgetProjectionMap.put(BudgetTableMetaData.STATUS, BudgetTableMetaData.STATUS);

		sBudgetProjectionMap.put(BudgetTableMetaData.CREATED_DATE, BudgetTableMetaData.CREATED_DATE);
		sBudgetProjectionMap.put(BudgetTableMetaData.MODIFIED_DATE, BudgetTableMetaData.MODIFIED_DATE);
	}

	private static HashMap<String, String> sTransactionStatusProjectionMap;
	static
	{
		sTransactionStatusProjectionMap = new HashMap<String, String>();
		sTransactionStatusProjectionMap.put(TransactionStatusTableMetaData._ID, TransactionStatusTableMetaData._ID);

		sTransactionStatusProjectionMap.put(TransactionStatusTableMetaData.NAME, TransactionStatusTableMetaData.NAME);
		sTransactionStatusProjectionMap.put(TransactionStatusTableMetaData.SORTORDER, TransactionStatusTableMetaData.SORTORDER);
		sTransactionStatusProjectionMap.put(TransactionStatusTableMetaData.RESOURCEID, TransactionStatusTableMetaData.RESOURCEID);

		sTransactionStatusProjectionMap.put(TransactionStatusTableMetaData.CREATED_DATE, TransactionStatusTableMetaData.CREATED_DATE);
		sTransactionStatusProjectionMap.put(TransactionStatusTableMetaData.MODIFIED_DATE, TransactionStatusTableMetaData.MODIFIED_DATE);
	}

	private static HashMap<String, String> sPaymentMethodsProjectionMap;
	static
	{
		sPaymentMethodsProjectionMap = new HashMap<String, String>();
		sPaymentMethodsProjectionMap.put(PaymentMethodsTableMetaData._ID, PaymentMethodsTableMetaData._ID);

		sPaymentMethodsProjectionMap.put(PaymentMethodsTableMetaData.NAME, PaymentMethodsTableMetaData.NAME);
		sPaymentMethodsProjectionMap.put(PaymentMethodsTableMetaData.SORTORDER, PaymentMethodsTableMetaData.SORTORDER);
		sPaymentMethodsProjectionMap.put(PaymentMethodsTableMetaData.RESOURCEID, PaymentMethodsTableMetaData.RESOURCEID);

		sPaymentMethodsProjectionMap.put(PaymentMethodsTableMetaData.CREATED_DATE, PaymentMethodsTableMetaData.CREATED_DATE);
		sPaymentMethodsProjectionMap.put(PaymentMethodsTableMetaData.MODIFIED_DATE, PaymentMethodsTableMetaData.MODIFIED_DATE);
	}


	private static HashMap<String, String> sVDebtsProjectionMap; 
    static  
    { 
    	sVDebtsProjectionMap = new HashMap<String, String>(); 
    	sVDebtsProjectionMap.put(VDebtsViewMetaData._ID, VDebtsViewMetaData._ID); 
         
    	sVDebtsProjectionMap.put(VDebtsViewMetaData.ISGIVEN, VDebtsViewMetaData.ISGIVEN); 
    	sVDebtsProjectionMap.put(VDebtsViewMetaData.TRANSDATE, VDebtsViewMetaData.TRANSDATE); 
    	sVDebtsProjectionMap.put(VDebtsViewMetaData.AMOUNT, VDebtsViewMetaData.AMOUNT); 
    	sVDebtsProjectionMap.put(VDebtsViewMetaData.DESCRIPTION, VDebtsViewMetaData.DESCRIPTION); 
    	sVDebtsProjectionMap.put(VDebtsViewMetaData.BACKDATE, VDebtsViewMetaData.BACKDATE); 
    	sVDebtsProjectionMap.put(VDebtsViewMetaData.REMINDME, VDebtsViewMetaData.REMINDME); 
    	sVDebtsProjectionMap.put(VDebtsViewMetaData.CURRENCY_ID, VDebtsViewMetaData.CURRENCY_ID);
    	sVDebtsProjectionMap.put(VDebtsViewMetaData.STATUS, VDebtsViewMetaData.STATUS);          
    	sVDebtsProjectionMap.put(VDebtsViewMetaData.CURRENCY_NAME, VDebtsViewMetaData.CURRENCY_NAME); 
    	sVDebtsProjectionMap.put(VDebtsViewMetaData.CURRENCY_SIGN, VDebtsViewMetaData.CURRENCY_SIGN); 
    } 
    
    private static HashMap<String, String> sVTransactionProjectionMap; 
    static  
    { 
    	sVTransactionProjectionMap = new HashMap<String, String>(); 
    	sVTransactionProjectionMap.put(VTransactionViewMetaData._ID, VTransactionViewMetaData._ID); 
         
    	sVTransactionProjectionMap.put(VTransactionViewMetaData.ACCOUNTID, VTransactionViewMetaData.ACCOUNTID); 
    	sVTransactionProjectionMap.put(VTransactionViewMetaData.ACCOUNTNAME, VTransactionViewMetaData.ACCOUNTNAME); 
    	sVTransactionProjectionMap.put(VTransactionViewMetaData.CATEGORYID, VTransactionViewMetaData.CATEGORYID); 
    	sVTransactionProjectionMap.put(VTransactionViewMetaData.CATEGORYNAME, VTransactionViewMetaData.CATEGORYNAME);
    	sVTransactionProjectionMap.put(VTransactionViewMetaData.TRANSDATE, VTransactionViewMetaData.TRANSDATE);
    	sVTransactionProjectionMap.put(VTransactionViewMetaData.AMOUNT, VTransactionViewMetaData.AMOUNT);
    	sVTransactionProjectionMap.put(VTransactionViewMetaData.LBAMOUNT, VTransactionViewMetaData.LBAMOUNT);
    	sVTransactionProjectionMap.put(VTransactionViewMetaData.TRANSTYPE, VTransactionViewMetaData.TRANSTYPE);
    	sVTransactionProjectionMap.put(VTransactionViewMetaData.BALANCE, VTransactionViewMetaData.BALANCE);
    	sVTransactionProjectionMap.put(VTransactionViewMetaData.LBALANCE, VTransactionViewMetaData.LBALANCE);
    	sVTransactionProjectionMap.put(VTransactionViewMetaData.DESCRIPTION, VTransactionViewMetaData.DESCRIPTION);
    	sVTransactionProjectionMap.put(VTransactionViewMetaData.TRANSFERID, VTransactionViewMetaData.TRANSFERID);
    	sVTransactionProjectionMap.put(VTransactionViewMetaData.ISTRANSFER, VTransactionViewMetaData.ISTRANSFER);
    	sVTransactionProjectionMap.put(VTransactionViewMetaData.ACCOUNTSTATUS, VTransactionViewMetaData.ACCOUNTSTATUS);
    	sVTransactionProjectionMap.put(VTransactionViewMetaData.CURRID, VTransactionViewMetaData.CURRID);
		sVTransactionProjectionMap.put(VTransactionViewMetaData.ACCOUNTCURRID, VTransactionViewMetaData.ACCOUNTCURRID);
		sVTransactionProjectionMap.put(VTransactionViewMetaData.PHOTO_PATH, VTransactionViewMetaData.PHOTO_PATH);
		sVTransactionProjectionMap.put(VTransactionViewMetaData.STATUS, VTransactionViewMetaData.STATUS);
		sVTransactionProjectionMap.put(VTransactionViewMetaData.PAYMENT_METHOD, VTransactionViewMetaData.PAYMENT_METHOD);
		sVTransactionProjectionMap.put(VTransactionViewMetaData.CURRENCYSIGN, VTransactionViewMetaData.CURRENCYSIGN);
    }
    
    private static HashMap<String, String> sVAccountsProjectionMap; 
    static  
    { 
    	sVAccountsProjectionMap = new HashMap<String, String>(); 
    	sVAccountsProjectionMap.put(VAccountsViewMetaData._ID, VAccountsViewMetaData._ID); 
         
    	sVAccountsProjectionMap.put(VAccountsViewMetaData.ACCOUNTNAME, VAccountsViewMetaData.ACCOUNTNAME); 
    	sVAccountsProjectionMap.put(VAccountsViewMetaData.CURRID, VAccountsViewMetaData.CURRID); 
    	sVAccountsProjectionMap.put(VAccountsViewMetaData.CURRNAME, VAccountsViewMetaData.CURRNAME);
    	sVAccountsProjectionMap.put(VAccountsViewMetaData.DESCRIPTION, VAccountsViewMetaData.DESCRIPTION);
    	sVAccountsProjectionMap.put(VAccountsViewMetaData.INITIALBALANCE, VAccountsViewMetaData.INITIALBALANCE);
    	sVAccountsProjectionMap.put(VAccountsViewMetaData.ISDEFAULT, VAccountsViewMetaData.ISDEFAULT);
    	sVAccountsProjectionMap.put(VAccountsViewMetaData.BALANCE, VAccountsViewMetaData.BALANCE);
    	sVAccountsProjectionMap.put(VAccountsViewMetaData.CURRSIGN, VAccountsViewMetaData.CURRSIGN);
    	sVAccountsProjectionMap.put(VAccountsViewMetaData.STATUS, VAccountsViewMetaData.STATUS);
    	sVAccountsProjectionMap.put(VAccountsViewMetaData.SORTORDER, VAccountsViewMetaData.SORTORDER);
    } 
    
    private static HashMap<String, String> sVTransAccountsProjectionMap; 
    static  
    { 
    	sVTransAccountsProjectionMap = new HashMap<String, String>(); 
    	sVTransAccountsProjectionMap.put(VTransAccountViewMetaData._ID, VTransAccountViewMetaData._ID); 
         
    	sVTransAccountsProjectionMap.put(VTransAccountViewMetaData.NAME, VTransAccountViewMetaData.NAME); 
    	sVTransAccountsProjectionMap.put(VTransAccountViewMetaData.ISDEFAULT, VTransAccountViewMetaData.ISDEFAULT); 
    	sVTransAccountsProjectionMap.put(VTransAccountViewMetaData.STATUS, VTransAccountViewMetaData.STATUS);  
    	sVTransAccountsProjectionMap.put(VTransAccountViewMetaData.SORTORDER, VTransAccountViewMetaData.SORTORDER); 
    } 
 
    private static HashMap<String, String> sVTransferProjectionMap; 
    static  
    { 
    	sVTransferProjectionMap = new HashMap<String, String>(); 
    	sVTransferProjectionMap.put(VTransferViewMetaData._ID, VTransferViewMetaData._ID); 
         
    	sVTransferProjectionMap.put(VTransferViewMetaData.FROMACCOUNTID, VTransferViewMetaData.FROMACCOUNTID); 
    	sVTransferProjectionMap.put(VTransferViewMetaData.FROMACCOUNTNAME, VTransferViewMetaData.FROMACCOUNTNAME); 
    	sVTransferProjectionMap.put(VTransferViewMetaData.TOACCOUNTID, VTransferViewMetaData.TOACCOUNTID);
    	sVTransferProjectionMap.put(VTransferViewMetaData.TOACCOUNTNAME, VTransferViewMetaData.TOACCOUNTNAME);
    	sVTransferProjectionMap.put(VTransferViewMetaData.ACCOUNTLABEL, VTransferViewMetaData.ACCOUNTLABEL);
    	sVTransferProjectionMap.put(VTransferViewMetaData.AMOUNT, VTransferViewMetaData.AMOUNT);
    	sVTransferProjectionMap.put(VTransferViewMetaData.CATEGORYID, VTransferViewMetaData.CATEGORYID);
    	sVTransferProjectionMap.put(VTransferViewMetaData.CATEGORYNAME, VTransferViewMetaData.CATEGORYNAME);
    	sVTransferProjectionMap.put(VTransferViewMetaData.LBAMOUNT, VTransferViewMetaData.LBAMOUNT);
    	sVTransferProjectionMap.put(VTransferViewMetaData.TRANSDATE, VTransferViewMetaData.TRANSDATE);
    	sVTransferProjectionMap.put(VTransferViewMetaData.REPEATTYPE, VTransferViewMetaData.REPEATTYPE);
    	sVTransferProjectionMap.put(VTransferViewMetaData.DESCRIPTION, VTransferViewMetaData.DESCRIPTION);
    	sVTransferProjectionMap.put(VTransferViewMetaData.PERIODEND, VTransferViewMetaData.PERIODEND); 
    	sVTransferProjectionMap.put(VTransferViewMetaData.NEXTPAYMENT, VTransferViewMetaData.NEXTPAYMENT); 
    	sVTransferProjectionMap.put(VTransferViewMetaData.CUSTOMINTERVAL, VTransferViewMetaData.CUSTOMINTERVAL); 
    	sVTransferProjectionMap.put(VTransferViewMetaData.CURRENCYID, VTransferViewMetaData.CURRENCYID); 
    	sVTransferProjectionMap.put(VTransferViewMetaData.FROMACCCURRID, VTransferViewMetaData.FROMACCCURRID); 
    	sVTransferProjectionMap.put(VTransferViewMetaData.TOACCCURRID, VTransferViewMetaData.TOACCCURRID);
    	sVTransferProjectionMap.put(VTransferViewMetaData.ISENABLED, VTransferViewMetaData.ISENABLED);
    	sVTransferProjectionMap.put(VTransferViewMetaData.REMINDER, VTransferViewMetaData.REMINDER);
		sVTransferProjectionMap.put(VTransferViewMetaData.TRANSACTION_STATUS, VTransferViewMetaData.TRANSACTION_STATUS);
		sVTransferProjectionMap.put(VTransferViewMetaData.TRANSACTION_PAYMENT_METHOD, VTransferViewMetaData.TRANSACTION_PAYMENT_METHOD);
		sVTransferProjectionMap.put(VTransferViewMetaData.CURRENCY_SIGN, VTransferViewMetaData.CURRENCY_SIGN);
    } 

    private static HashMap<String, String> sVCategoriesProjectionMap; 
    static  
    { 
    	sVCategoriesProjectionMap = new HashMap<String, String>(); 
    	sVCategoriesProjectionMap.put(VCategoriesViewMetaData._ID, VCategoriesViewMetaData._ID); 
         
    	sVCategoriesProjectionMap.put(VCategoriesViewMetaData.NAME, VCategoriesViewMetaData.NAME); 
    	sVCategoriesProjectionMap.put(VCategoriesViewMetaData.MAINID, VCategoriesViewMetaData.MAINID);
    }

	private static HashMap<String, String> sBudgetCategoriesProjectionMap;
	static
	{
		sBudgetCategoriesProjectionMap = new HashMap<String, String>();
		sBudgetCategoriesProjectionMap.put(BudgetCategoriesTableMetaData._ID, BudgetCategoriesTableMetaData._ID);
		sBudgetCategoriesProjectionMap.put(BudgetCategoriesTableMetaData.BUDGET_ID, BudgetCategoriesTableMetaData.BUDGET_ID);
		sBudgetCategoriesProjectionMap.put(BudgetCategoriesTableMetaData.CATEGORY_ID, BudgetCategoriesTableMetaData.CATEGORY_ID);
		sBudgetCategoriesProjectionMap.put(BudgetCategoriesTableMetaData.BUDGET, BudgetCategoriesTableMetaData.BUDGET);
		sBudgetCategoriesProjectionMap.put(BudgetCategoriesTableMetaData.USED_AMOUNT, BudgetCategoriesTableMetaData.USED_AMOUNT);
		sBudgetCategoriesProjectionMap.put(BudgetCategoriesTableMetaData.REMAINING, BudgetCategoriesTableMetaData.REMAINING);
		sBudgetCategoriesProjectionMap.put(BudgetCategoriesTableMetaData.REPEAT, BudgetCategoriesTableMetaData.REPEAT);

		sBudgetCategoriesProjectionMap.put(BudgetCategoriesTableMetaData.CREATED_DATE, BudgetCategoriesTableMetaData.CREATED_DATE);
		sBudgetCategoriesProjectionMap.put(BudgetCategoriesTableMetaData.MODIFIED_DATE, BudgetCategoriesTableMetaData.MODIFIED_DATE);
	}

	private static HashMap<String, String> sBudgetGoalsProjectionMap;
	static
	{
		sBudgetGoalsProjectionMap = new HashMap<String, String>();
		sBudgetGoalsProjectionMap.put(BudgetGoalsTableMetaData._ID, BudgetGoalsTableMetaData._ID);
		sBudgetGoalsProjectionMap.put(BudgetGoalsTableMetaData.CATEGORY_ID, BudgetGoalsTableMetaData.CATEGORY_ID);
		sBudgetGoalsProjectionMap.put(BudgetGoalsTableMetaData.START_MONTH, BudgetGoalsTableMetaData.START_MONTH);
		sBudgetGoalsProjectionMap.put(BudgetGoalsTableMetaData.TARGET_MONTH, BudgetGoalsTableMetaData.TARGET_MONTH);
		sBudgetGoalsProjectionMap.put(BudgetGoalsTableMetaData.TARGET_AMOUNT, BudgetGoalsTableMetaData.TARGET_AMOUNT);
		sBudgetGoalsProjectionMap.put(BudgetGoalsTableMetaData.DESCRIPTION, BudgetGoalsTableMetaData.DESCRIPTION);

		sBudgetGoalsProjectionMap.put(BudgetGoalsTableMetaData.CREATED_DATE, BudgetGoalsTableMetaData.CREATED_DATE);
		sBudgetGoalsProjectionMap.put(BudgetGoalsTableMetaData.MODIFIED_DATE, BudgetGoalsTableMetaData.MODIFIED_DATE);
	}



	//Provide a mechanism to identify
    //all the incoming uri patterns. 
    private static final UriMatcher sUriMatcher; 
    private static final int CURRENCY_COLLECTION_URI_INDICATOR = 1; 
    private static final int CURRENCY_SINGLE_URI_INDICATOR = 2; 
    private static final int ACCOUNT_COLLECTION_URI_INDICATOR = 3; 
    private static final int ACCOUNT_SINGLE_URI_INDICATOR = 4; 
    private static final int CATEGORY_COLLECTION_URI_INDICATOR = 5; 
    private static final int CATEGORY_SINGLE_URI_INDICATOR = 6; 
    private static final int CURRRATES_COLLECTION_URI_INDICATOR = 7; 
    private static final int CURRRATES_SINGLE_URI_INDICATOR = 8; 
    private static final int VCURRRATES_COLLECTION_URI_INDICATOR = 9; 
    private static final int VCURRRATES_SINGLE_URI_INDICATOR = 10; 
    private static final int TRANSACTIONS_COLLECTION_URI_INDICATOR = 11; 
    private static final int TRANSACTIONS_SINGLE_URI_INDICATOR = 12; 
    private static final int VTRANSACTION_COLLECTION_URI_INDICATOR = 15; 
    private static final int VTRANSACTION_SINGLE_URI_INDICATOR = 16; 
    private static final int VACCOUNT_COLLECTION_URI_INDICATOR = 17; 
    private static final int VACCOUNT_SINGLE_URI_INDICATOR = 18; 
    private static final int VTRANSACCOUNT_COLLECTION_URI_INDICATOR = 19; 
    private static final int TRANSFER_COLLECTION_URI_INDICATOR = 20; 
    private static final int TRANSFER_SINGLE_URI_INDICATOR = 21; 
    private static final int VTRANSFER_COLLECTION_URI_INDICATOR = 22; 
    private static final int VTRANSFER_SINGLE_URI_INDICATOR = 23; 
    private static final int VCATEGORIES_COLLECTION_URI_INDICATOR = 24; 
    private static final int VCATEGORIES_SINGLE_URI_INDICATOR = 25; 
    private static final int DEBTS_COLLECTION_URI_INDICATOR = 26; 
    private static final int DEBTS_SINGLE_URI_INDICATOR = 27; 
    private static final int VDEBTS_COLLECTION_URI_INDICATOR = 28; 
    private static final int VDEBTS_SINGLE_URI_INDICATOR = 29;  
    private static final int BUDGET_COLLECTION_URI_INDICATOR = 30; 
    private static final int BUDGET_SINGLE_URI_INDICATOR = 31;
	private static final int BUDGET_CATEGORY_COLLECTION_URI_INDICATOR = 32;
	private static final int BUDGET_CATEGORY_SINGLE_URI_INDICATOR = 33;
	private static final int TRANSACTION_STATUS_COLLECTION_URI_INDICATOR = 34;
	private static final int TRANSACTION_STATUS_SINGLE_URI_INDICATOR = 35;
	private static final int PAYMENT_METHODS_COLLECTION_URI_INDICATOR = 36;
	private static final int PAYMENT_METHODS_SINGLE_URI_INDICATOR = 37;
	private static final int BUDGET_GOALS_COLLECTION_URI_INDICATOR = 38;
	private static final int BUDGET_GOALS_SINGLE_URI_INDICATOR = 39;
	static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH); 
        sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, CurrencyTableMetaData.TABLE_NAME, CURRENCY_COLLECTION_URI_INDICATOR); 
        sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, CurrencyTableMetaData.TABLE_NAME + "/#", CURRENCY_SINGLE_URI_INDICATOR); 
        sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, AccountTableMetaData.TABLE_NAME, ACCOUNT_COLLECTION_URI_INDICATOR); 
        sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, AccountTableMetaData.TABLE_NAME + "/#", ACCOUNT_SINGLE_URI_INDICATOR); 
        sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, CategoryTableMetaData.TABLE_NAME, CATEGORY_COLLECTION_URI_INDICATOR); 
        sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, CategoryTableMetaData.TABLE_NAME + "/#", CATEGORY_SINGLE_URI_INDICATOR); 
        sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, CurrRatesTableMetaData.TABLE_NAME, CURRRATES_COLLECTION_URI_INDICATOR); 
        sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, CurrRatesTableMetaData.TABLE_NAME + "/#", CURRRATES_SINGLE_URI_INDICATOR); 
        sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, VCurrRatesViewMetaData.VIEW_NAME, VCURRRATES_COLLECTION_URI_INDICATOR); 
        sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, VCurrRatesViewMetaData.VIEW_NAME + "/#", VCURRRATES_SINGLE_URI_INDICATOR); 
        sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, TransactionsTableMetaData.TABLE_NAME, TRANSACTIONS_COLLECTION_URI_INDICATOR); 
        sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, TransactionsTableMetaData.TABLE_NAME + "/#", TRANSACTIONS_SINGLE_URI_INDICATOR); 
        sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, VTransactionViewMetaData.VIEW_NAME, VTRANSACTION_COLLECTION_URI_INDICATOR); 
        sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, VTransactionViewMetaData.VIEW_NAME + "/#", VTRANSACTION_SINGLE_URI_INDICATOR); 
        sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, VAccountsViewMetaData.VIEW_NAME, VACCOUNT_COLLECTION_URI_INDICATOR); 
        sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, VAccountsViewMetaData.VIEW_NAME + "/#", VACCOUNT_SINGLE_URI_INDICATOR); 
        sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, VTransAccountViewMetaData.VIEW_NAME, VTRANSACCOUNT_COLLECTION_URI_INDICATOR); 
        sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, TransferTableMetaData.TABLE_NAME, TRANSFER_COLLECTION_URI_INDICATOR); 
        sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, TransferTableMetaData.TABLE_NAME + "/#", TRANSFER_SINGLE_URI_INDICATOR); 
        sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, DebtsTableMetaData.TABLE_NAME, DEBTS_COLLECTION_URI_INDICATOR); 
        sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, DebtsTableMetaData.TABLE_NAME + "/#", DEBTS_SINGLE_URI_INDICATOR); 
        sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, VTransferViewMetaData.VIEW_NAME, VTRANSFER_COLLECTION_URI_INDICATOR); 
        sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, VTransferViewMetaData.VIEW_NAME + "/#", VTRANSFER_SINGLE_URI_INDICATOR); 
        sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, VCategoriesViewMetaData.VIEW_NAME, VCATEGORIES_COLLECTION_URI_INDICATOR); 
        sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, VCategoriesViewMetaData.VIEW_NAME + "/#", VCATEGORIES_SINGLE_URI_INDICATOR); 
        sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, VDebtsViewMetaData.VIEW_NAME, VDEBTS_COLLECTION_URI_INDICATOR); 
        sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, VDebtsViewMetaData.VIEW_NAME + "/#", VDEBTS_SINGLE_URI_INDICATOR); 
        sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, BudgetTableMetaData.TABLE_NAME, BUDGET_COLLECTION_URI_INDICATOR); 
        sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, BudgetTableMetaData.TABLE_NAME + "/#", BUDGET_SINGLE_URI_INDICATOR);
		sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, BudgetCategoriesTableMetaData.TABLE_NAME, BUDGET_CATEGORY_COLLECTION_URI_INDICATOR);
		sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, BudgetCategoriesTableMetaData.TABLE_NAME + "/#", BUDGET_CATEGORY_SINGLE_URI_INDICATOR);
		sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, BudgetGoalsTableMetaData.TABLE_NAME, BUDGET_GOALS_COLLECTION_URI_INDICATOR);
		sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, BudgetGoalsTableMetaData.TABLE_NAME + "/#", BUDGET_GOALS_SINGLE_URI_INDICATOR);
		sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, TransactionStatusTableMetaData.TABLE_NAME, TRANSACTION_STATUS_COLLECTION_URI_INDICATOR);
		sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, TransactionStatusTableMetaData.TABLE_NAME + "/#", TRANSACTION_STATUS_SINGLE_URI_INDICATOR);
		sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, PaymentMethodsTableMetaData.TABLE_NAME, PAYMENT_METHODS_COLLECTION_URI_INDICATOR);
		sUriMatcher.addURI(MoneyManagerProviderMetaData.AUTHORITY, PaymentMethodsTableMetaData.TABLE_NAME + "/#", PAYMENT_METHODS_SINGLE_URI_INDICATOR);
	}
 
    /** 
     * Setup/Create Database 
     * This class helps open, create, and upgrade the database file. 
     */ 
    public static class DatabaseHelper extends SQLiteOpenHelper { 
    	// Database creation sql statement
		private static final String DATABASE_CREATE_CURRENCY = "create table currency (_id integer primary key autoincrement, "
    			+ "name text not null, sign text not null, isdefault integer, created text, modified text, sortorder integer, resourceID integer);";
    	private static final String DATABASE_CREATE_ACCOUNT = "create table accounts (_id integer primary key autoincrement, "
    			+ "name text not null, curr_id integer, description text, initial_balance text default '0', isdefault integer, "
    			+ "status integer default 1, sortorder integer, created text, modified text);";
    	private static final String DATABASE_CREATE_CURR_RATES = "create table curr_rates (_id integer primary key autoincrement, " +
    			"first_curr_id integer not null, second_curr_id integer not null, value text not null, " +
    			"rate_date text, next_rate_date text, created text, modified text);";
    	private static final String DATABASE_CREATE_CATEGORY = "create table category (_id integer primary key autoincrement, "
    			+ "name text not null, main_id integer, is_income integer, created text, modified text, resourceID integer);";
    	private static final String DATABASE_CREATE_TRANSACTION = "create table transactions (_id integer primary key autoincrement, "
    			+ "account_id integer not null, category_id integer, trans_date text not null, amount text not null, " 
    			+ "trans_type integer default 1, balance text not null, description text, curr_id integer, " 
    			+ "transfer_id long, created text, modified text, photo_path text, status integer, payment_method integer);";
    	private static final String DATABASE_CREATE_TRANSFER = "create table transfer (_id integer primary key autoincrement, " +
    			"first_account_id integer, second_account_id integer, category_id integer, trans_date text, amount text not null, " +
    			"repeat_type integer not null, description text not null, period_end text, next_payment text, " +
    			"custom_interval number, curr_id integer, reminder integer, " +
				"transaction_status integer, transaction_payment_method integer, status integer default 1, " +
    			"created text, modified text);";
    	private static final String DATABASE_CREATE_DEBTS = "create table debts (_id integer primary key autoincrement, " +
    			"is_given integer, trans_date text, amount text not null, " +
    			"description text, back_date text, remind_me integer, currency_id integer, " +
    			"status integer default 1, created text, modified text);";
    	private static final String DATABASE_CREATE_BUDGET = "create table budget (_id integer primary key autoincrement, " +
    			"from_date text, to_date text," +
    			"income text not null, currency_id integer not null, " +
    			"status integer default 1, created text, modified text)";
    	private static final String DATABASE_CREATE_BUDGET_CATEGORIES = "create table budget_categories "
    			+ "(_id integer primary key autoincrement, budget_id integer not null, category_id integer not null, "
    			+ "budget text, used_amount text, remaining text, repeat integer, created text, modified text)";
		private static final String DATABASE_CREATE_PAYMENT_METHODS = "create table payment_methods (_id integer primary key autoincrement, "
				+ "name text not null, sortorder integer, created text, modified text, resourceID integer);";
		private static final String DATABASE_CREATE_TRANSACTION_STATUS = "create table transaction_status (_id integer primary key autoincrement, "
				+ "name text not null, sortorder integer, created text, modified text, resourceID integer);";
		private static final String DATABASE_CREATE_BUDGET_GOALS = "create table budget_goals (_id integer primary key autoincrement, " +
				"created text, modified text, category_id integer not null, " +
				"start_month text not null, target_month text not null, target_amount text not null, description text)";
    	public static final String DATABASE_CREATE_VIEW_VTRANSFER = "create view vtransfer as " +
    			"select tr._id, acc1._id from_account_id, acc1.name from_account_name, " +
    			"acc2._id to_account_id, acc2.name to_account_name, " +
    			"case " +
    			"	when acc1._id is not null and acc2._id is not null then acc1.name || '-' || acc2.name " +
    			"	when acc1._id is null then acc2.name || '-income' " +
    			"	when acc2._id is null then acc1.name || '-expense' " +
    			"else 'sd' end account_label, " +
    			"tr.amount, tr.trans_date, tr.repeat_type, tr.description," +
    			"tr.period_end, tr.next_payment, tr.custom_interval, tr.curr_id, " +
    			"acc1.curr_id from_account_curr_id, acc2.curr_id to_account_curr_id, tr.amount || cr.sign lbamount," +
    			"case when ((repeat_type != 0 and next_payment is null) or (repeat_type = 0 and trans_date <= strftime('%Y%m%d','now', 'localtime')))" +
    			"	then 0 else 1 end is_enabled, tr.reminder, tr.category_id, cat.name category_name, " +
				"tr.transaction_status, tr.transaction_payment_method, cr.sign currency_sign " +
    			"from transfer tr " +
    			"join currency cr on cr._id = tr.curr_id and tr.status = 1 " +
    			"left join accounts acc1 on acc1._id = tr.first_account_id " +
    			"left join accounts acc2 on acc2._id = tr.second_account_id " + 
    			"left join category cat on cat._id = tr.category_id";
    	private static final String DATABASE_CREATE_VIEW_VCUR_RATES = "create view vcurrates " +
    			"as select a1._id _id, a2._id first_curr_id, a2.sign first_curr_sign, " +
    			"a3._id second_curr_id, a3.sign second_curr_sign, a1.value value, a1.rate_date, a1.next_rate_date, " +
    			"a2.isdefault firstisdefault, a3.isdefault secondisdefault from " +
    			"curr_rates a1 " +
    			"join currency a2 on a1.first_curr_id = a2._id " +
    			"join currency a3 on a1.second_curr_id = a3._id;";
    	private static final String DATABASE_CREATE_VIEW_VTRANSACTION = "create view vtransaction " 
    			+ "as select a1._id _id, a1.account_id, acc.name account_name, a1.category_id, "
				+ "case when (transfer_id is null or transfer_id = 0 or tr.first_account_id is null or tr.second_account_id is null) "
    			+ "then 0 else 1 end is_transfer, "
				+ "case when (transfer_id is null or transfer_id = 0 or tr.first_account_id is null or tr.second_account_id is null) "
				+ "then a2.name  else  t1.name || ' - ' || t2.name end category_name, " 
				+ "a1.trans_date, a1.amount, a1.amount || cr.sign lbamount, a1.trans_type, balance, '(' || balance || ')' lbalance,a1.description,  "
				+ "a1.transfer_id, acc.status account_status, a1.curr_id, acc.curr_id account_curr_id, a1.photo_path, a1.status, a1.payment_method, "
				+ "cr.sign currency_sign "
				+ "from transactions a1  "
				+ "join accounts acc on acc._id = a1.account_id " 
				+ "left join vcategories a2 on a2._id = a1.category_id " 
				+ "left join transfer tr on tr._id =a1.transfer_id "
				+ "left join accounts t1 on t1._id = tr.first_account_id "
				+ "left join accounts t2 on t2._id = tr.second_account_id "
				+ "left join currency cr on cr._id = a1.curr_id";
    	private static final String DATABASE_CREATE_VIEW_VACCOUNTS = "create view vaccounts as " +				
    			"select a1._id, a1.name account_name, a1.curr_id, curr.name currency_name, a1.description, initial_balance, a1.isdefault, "+ 
    			"ifnull(tr1.balance, initial_balance) balance, curr.sign currency_sign, a1.status, a1.sortorder  "+
    			"from accounts a1  "+
    			"join currency curr on curr._id = a1.curr_id "+ 
    			"left join (select t3.balance, t3.account_id from transactions t3 "+ 
    			"join (select max(t1._id) _id, t1.account_id from transactions t1  "+
    			"join (select max(trans_date) trans_date, account_id from transactions where trans_date <= strftime('%Y%m%d','now', 'localtime') group by account_id) t2 "+
    			"on t1.account_id = t2.account_id and t1.trans_date = t2.trans_date group by t1.account_id) t4 on t3._id = t4._id) tr1 on tr1.account_id = a1._id";
    	public static final String DATABASE_CREATE_VIEW_VTRANSACCOUNTS = "create view vtrans_accounts as " +				
    			"select a1._id, a1.name account_name, isdefault, a1.status, a1.sortorder " +
    			"from accounts a1 " +
    			"union all " +
    			"select 0, 'ALL', 2, 1, -1 ";
    	private static final String DATABASE_CREATE_VIEW_VCATEGORIES = "create view vcategories as " +
    			"select c1._id, c1.main_id, " +
    			"case when c2._id is not null then c2.name || ':' || c1.name else c1.name end name " +
    			"from category c1 left join category c2 on c1.main_id = c2._id";
    	private static final String DATABASE_CREATE_VIEW_VRATESTODEFAULT = "create view vratestodefault as " +
    			"select case when first_curr_id != cu._id then first_curr_id else second_curr_id end curr_id, " +
    			"rate_date, ifnull(next_rate_date, strftime('%Y%m%d','now')) next_rate_date, " +
    			"case when cu._id = cr.second_curr_id then value else round(1/value, 6) end value " +
    			"from curr_rates cr " +
    			"join currency cu on (cu._id = cr.first_curr_id or cu._id = second_curr_id) and cu.isdefault = 1";
    	private static final String DATABASE_CREATE_VIEW_VDEBTS = "create view vdebts as " +
    			"select d._id, d.is_given, d.trans_date, d.amount, d.description, d.back_date, "+
    			"d.remind_me, d.currency_id, d.status, cur.name currency_name, cur.sign currency_sign " +
    			"from debts d " +
    			"join currency cur on cur._id = d.currency_id ";
    	/*private static final String DATABASE_CREATE_VIEW_VGROUP_BUDGETS = "create view vgroup_budgets as " +
    			"select c2._ID, c2.NAME, ifnull(sum(BUDGET),0) BUDGET, " +
    		    "ifnull(sum(case when is_main = 1 then BUDGET else 0 end),0) groupBudget, " + 
    		    "ifnull(sum(USED_AMOUNT),0) USED_AMOUNT, ifnull(sum(REMAINING),0) REMAINING,  " +
    		    "ifnull(sum(case when is_main = 1 then REMAINING else 0 end),0) groupRemaining,  " +
    		    "ifnull(sum(BUDGET + REMAINING - USED_AMOUNT),0) remainingColumn  " +
    		    "from (select _ID categMainIDColName, _ID categIDColName, 1 is_main  " +
    		    "        from category where MAIN_ID is null  " +
    		    "     union all  " +
    		    "     select MAIN_ID, _ID, 0 is_main " + 
    		    "        from category where MAIN_ID is not null) c1 " + 
    		    "  join category c2 on c2._ID = c1.categMainIDColName " +
    		    "  left join (select b1.*  " +
    			"	from budget_categories b1  " +
    			"	join budget b2 on b2._ID = b1.BUDGET_ID)b " + 
    			"on b.CATEGORY_ID = c1.categIDColName " + 
    			"group by c2._ID, c2.NAME  " +
    			"order by NAME ";*/
    	
        DatabaseHelper(Context context) { 
            super(context, MoneyManagerProviderMetaData.DATABASE_NAME, null, MoneyManagerProviderMetaData.DATABASE_VERSION);
        } 
 
        @Override 
        public void onCreate(SQLiteDatabase db)  
        { 
            Log.d("MoneyManager.DB","inner oncreate called");
            try{
            	db.execSQL(DATABASE_CREATE_CURRENCY);
            }
            catch (Exception ex)
            {}
    		db.execSQL(DATABASE_CREATE_ACCOUNT);
    		db.execSQL(DATABASE_CREATE_CURR_RATES);
    		db.execSQL(DATABASE_CREATE_CATEGORY);
    		db.execSQL(DATABASE_CREATE_TRANSACTION);
    		//db.execSQL(DATABASE_CREATE_TRANSACTION_INDEX);
    		db.execSQL(DATABASE_CREATE_TRANSFER);
    		db.execSQL(DATABASE_CREATE_DEBTS);
    		db.execSQL(DATABASE_CREATE_BUDGET);
			db.execSQL(DATABASE_CREATE_BUDGET_CATEGORIES);
			db.execSQL(DATABASE_CREATE_BUDGET_GOALS);
			db.execSQL(DATABASE_CREATE_TRANSACTION_STATUS);
			db.execSQL(DATABASE_CREATE_PAYMENT_METHODS);
    		db.execSQL(DATABASE_CREATE_VIEW_VCUR_RATES);
    		db.execSQL(DATABASE_CREATE_VIEW_VACCOUNTS);
    		db.execSQL(DATABASE_CREATE_VIEW_VCATEGORIES);
    		db.execSQL(DATABASE_CREATE_VIEW_VTRANSFER);
    		db.execSQL(DATABASE_CREATE_VIEW_VTRANSACTION);
    		db.execSQL(DATABASE_CREATE_VIEW_VTRANSACCOUNTS);
    		db.execSQL(DATABASE_CREATE_VIEW_VRATESTODEFAULT);
    		db.execSQL(DATABASE_CREATE_VIEW_VDEBTS);
        } 
 
        @Override 
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.d("MoneyManager.DB", "inner onupgrade called");
			Log.w("MoneyManager.DB", "Upgrading database from version " + oldVersion + " to " + newVersion);
			for (int i = oldVersion; i < newVersion; i++) {
				try {
					switch (i) {
						case 13:
							db.execSQL("DROP view if exists " + VTransactionViewMetaData.VIEW_NAME);
							db.execSQL(DATABASE_CREATE_VIEW_VTRANSACTION);
							break;
						case 15:
							db.execSQL(DATABASE_CREATE_VIEW_VRATESTODEFAULT);
						case 18:
							db.execSQL("DROP view if exists " + VTransAccountViewMetaData.VIEW_NAME);
							db.execSQL(DATABASE_CREATE_VIEW_VTRANSACCOUNTS);
						case 19:
							db.execSQL("DROP view if exists " + VRatesToDefaultViewMetaData.VIEW_NAME);
							db.execSQL(DATABASE_CREATE_VIEW_VRATESTODEFAULT);
						case 20:
							db.execSQL("update " + CurrRatesTableMetaData.TABLE_NAME
									+ " set " + CurrRatesTableMetaData.VALUE + "  = 0.0001 where "
									+ CurrRatesTableMetaData.VALUE + "  < 0.0001");
						case 21:
							db.execSQL("alter table " + TransferTableMetaData.TABLE_NAME + " add " +
									TransferTableMetaData.CATEGORYID + " integer");
						case 22:
							db.execSQL("DROP view if exists " + VTransferViewMetaData.VIEW_NAME);
							db.execSQL(DATABASE_CREATE_VIEW_VTRANSFER);
						case 23:
							db.execSQL("DROP view if exists " + VTransferViewMetaData.VIEW_NAME);
							db.execSQL(DATABASE_CREATE_VIEW_VTRANSFER);
						case 25:
							db.execSQL("DROP view if exists " + VTransactionViewMetaData.VIEW_NAME);
							db.execSQL(DATABASE_CREATE_VIEW_VTRANSACTION);
						case 31:
							db.execSQL(DATABASE_CREATE_DEBTS);
							db.execSQL(DATABASE_CREATE_VIEW_VDEBTS);
						case 42:
							//db.execSQL("DROP table " + BudgetTableMetaData.TABLE_NAME);
							db.execSQL(DATABASE_CREATE_BUDGET);
							//db.execSQL("DROP table " + BudgetCategoriesTableMetaData.TABLE_NAME);
							db.execSQL(DATABASE_CREATE_BUDGET_CATEGORIES);
						case 44:
							db.execSQL("alter table " + CategoryTableMetaData.TABLE_NAME + " add " +
									CategoryTableMetaData.ISINCOME + " integer ");
							db.execSQL("update " + CategoryTableMetaData.TABLE_NAME + " set " +
									CategoryTableMetaData.ISINCOME + " = 0 ");
						/*db.execSQL("update " + CategoryTableMetaData.TABLE_NAME + " set "
								+ CategoryTableMetaData.ISINCOME + " = 1 where " 
								+ CategoryTableMetaData._ID + " = 465");
						db.execSQL("update " + CategoryTableMetaData.TABLE_NAME + " set "
								+ CategoryTableMetaData.ISINCOME + " = 1 where " 
								+ CategoryTableMetaData.MAINID + " = 465");*/
						case 45:
							db.execSQL("alter table " + TransactionsTableMetaData.TABLE_NAME + " add " +
									TransactionsTableMetaData.PHOTO_PATH + " text ");
							db.execSQL("DROP view if exists " + VTransactionViewMetaData.VIEW_NAME);
							db.execSQL(DATABASE_CREATE_VIEW_VTRANSACTION);
							break;
						case 46:
							db.execSQL(DATABASE_CREATE_TRANSACTION_STATUS);
							break;
						case 47:
							db.execSQL("alter table " + TransactionsTableMetaData.TABLE_NAME + " add " +
									TransactionsTableMetaData.STATUS + " integer ");
							break;
						case 49:
							db.execSQL("DROP view if exists " + VTransactionViewMetaData.VIEW_NAME);
							db.execSQL(DATABASE_CREATE_VIEW_VTRANSACTION);
							break;
						case 50:
							db.execSQL("DROP view if exists " + VRatesToDefaultViewMetaData.VIEW_NAME);
							db.execSQL(DATABASE_CREATE_VIEW_VRATESTODEFAULT);
							break;
						case 51:
							db.execSQL("alter table " + CategoryTableMetaData.TABLE_NAME + " add " +
									CategoryTableMetaData.RESOURCEID + " integer ");
							break;
						case 52:
							db.execSQL("alter table " + TransactionStatusTableMetaData.TABLE_NAME + " add " +
									TransactionStatusTableMetaData.RESOURCEID + " integer ");
							break;
						case 53:
							db.execSQL(DATABASE_CREATE_PAYMENT_METHODS);
							db.execSQL("alter table " + TransactionsTableMetaData.TABLE_NAME + " add " +
									TransactionsTableMetaData.PAYMENT_METHOD + " integer ");
							break;
						case 54:
							db.execSQL("DROP view if exists " + VTransactionViewMetaData.VIEW_NAME);
							db.execSQL(DATABASE_CREATE_VIEW_VTRANSACTION);
							break;
						case 55:
							db.execSQL("alter table " + CurrencyTableMetaData.TABLE_NAME + " add " +
									CurrencyTableMetaData.RESOURCEID + " integer ");
							break;
						case 56:
							db.execSQL("update curr_rates  \n" +
									"set next_rate_date = (select min(rate_date) from curr_rates c2 \n" +
									"\t\twhere ((curr_rates.first_curr_id = c2.first_curr_id and curr_rates.second_curr_id  =c2.second_curr_id) \n" +
									"\t\t\tor (curr_rates.first_curr_id = c2.second_curr_id and curr_rates.second_curr_id  =c2.first_curr_id))\n" +
									"\t\t\tand curr_rates.rate_date < c2.rate_date)\n" +
									"where next_rate_date is null\n" +
									"\tand exists (select 1 from curr_rates c2 \n" +
									"\t\twhere ((curr_rates.first_curr_id = c2.first_curr_id and curr_rates.second_curr_id  =c2.second_curr_id) \n" +
									"\t\t\tor (curr_rates.first_curr_id = c2.second_curr_id and curr_rates.second_curr_id  =c2.first_curr_id))\n" +
									"\t\t\tand curr_rates.rate_date < c2.rate_date)");
							break;
						case 57:
							db.execSQL(DATABASE_CREATE_BUDGET_GOALS);
							break;
						case 58:
							db.execSQL("alter table " + TransferTableMetaData.TABLE_NAME + " add " +
									TransferTableMetaData.TRANSACTION_STATUS + " integer ");
							db.execSQL("alter table " + TransferTableMetaData.TABLE_NAME + " add " +
									TransferTableMetaData.TRANSACTION_PAYMENT_METHOD + " integer ");
							db.execSQL("DROP view if exists " + VTransferViewMetaData.VIEW_NAME);
							db.execSQL(DATABASE_CREATE_VIEW_VTRANSFER);
							break;
						case 59:
							db.execSQL("alter table " + BudgetGoalsTableMetaData.TABLE_NAME + " add " +
									BudgetGoalsTableMetaData.DESCRIPTION + " text ");
						case 60:
							db.execSQL("alter table " + BudgetCategoriesTableMetaData.TABLE_NAME + " add " +
									BudgetCategoriesTableMetaData.REPEAT + " integer ");
						case 61:
							db.execSQL("DROP view if exists " + VTransferViewMetaData.VIEW_NAME);
							db.execSQL(DATABASE_CREATE_VIEW_VTRANSFER);
						case 62:
							db.execSQL("DROP view if exists " + VTransactionViewMetaData.VIEW_NAME);
							db.execSQL(DATABASE_CREATE_VIEW_VTRANSACTION);
							break;
					}
				} catch (Exception e) {
					Log.e("DB Upgrd err(version:" + String.valueOf(i) + ")", e.getMessage());
				}
			}
			//db.execSQL("DROP TABLE IF EXISTS " + MoneyManagerProviderMetaData.DATABASE_NAME);
			//onCreate(db);
		}
	}
 
    private DatabaseHelper mOpenHelper; 
 
    @Override 
    public boolean onCreate()  
    { 
        Log.d("MoneyManager.DB","main onCreate called"); 
        mOpenHelper = new DatabaseHelper(getContext()); 
        myApp = (MyApplication)getContext().getApplicationContext();
        return true; 
    }
    
    public void execSQL(String sql)
    {
    	SQLiteDatabase db = mOpenHelper.getReadableDatabase();
    	db.execSQL(sql);
    }
    
    @Override 
    public Cursor query(Uri uri, String[] projection, String selection,  
            String[] selectionArgs,  String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		String orderBy = sortOrder;

		switch (sUriMatcher.match(uri)) {
			case CURRENCY_COLLECTION_URI_INDICATOR:
				qb.setTables(CurrencyTableMetaData.TABLE_NAME);
				qb.setProjectionMap(sCurrencyProjectionMap);
				if (TextUtils.isEmpty(sortOrder))
					orderBy = CurrencyTableMetaData.DEFAULT_SORT_ORDER;
				break;
			case CURRENCY_SINGLE_URI_INDICATOR:
				qb.setTables(CurrencyTableMetaData.TABLE_NAME);
				qb.setProjectionMap(sCurrencyProjectionMap);
				qb.appendWhere(CurrencyTableMetaData._ID + "=" + uri.getPathSegments().get(1));
				if (TextUtils.isEmpty(sortOrder))
					orderBy = CurrencyTableMetaData.DEFAULT_SORT_ORDER;
				break;

			case ACCOUNT_COLLECTION_URI_INDICATOR:
				qb.setTables(AccountTableMetaData.TABLE_NAME);
				qb.setProjectionMap(sAccountProjectionMap);
				if (TextUtils.isEmpty(sortOrder))
					orderBy = AccountTableMetaData.DEFAULT_SORT_ORDER;
				break;
			case ACCOUNT_SINGLE_URI_INDICATOR:
				qb.setTables(AccountTableMetaData.TABLE_NAME);
				qb.setProjectionMap(sAccountProjectionMap);
				qb.appendWhere(AccountTableMetaData._ID + "=" + uri.getPathSegments().get(1));
				if (TextUtils.isEmpty(sortOrder))
					orderBy = AccountTableMetaData.DEFAULT_SORT_ORDER;
				break;

			case CATEGORY_COLLECTION_URI_INDICATOR:
				qb.setTables(CategoryTableMetaData.TABLE_NAME);
				qb.setProjectionMap(sCategoryProjectionMap);
				if (TextUtils.isEmpty(sortOrder))
					orderBy = CategoryTableMetaData.DEFAULT_SORT_ORDER;
				break;
			case CATEGORY_SINGLE_URI_INDICATOR:
				qb.setTables(CategoryTableMetaData.TABLE_NAME);
				qb.setProjectionMap(sCategoryProjectionMap);
				qb.appendWhere(CategoryTableMetaData._ID + "=" + uri.getPathSegments().get(1));
				if (TextUtils.isEmpty(sortOrder))
					orderBy = CategoryTableMetaData.DEFAULT_SORT_ORDER;
				break;

			case CURRRATES_COLLECTION_URI_INDICATOR:
				qb.setTables(CurrRatesTableMetaData.TABLE_NAME);
				qb.setProjectionMap(sCurrRatesProjectionMap);
				if (TextUtils.isEmpty(sortOrder))
					orderBy = CurrRatesTableMetaData.DEFAULT_SORT_ORDER;
				break;
			case CURRRATES_SINGLE_URI_INDICATOR:
				qb.setTables(CurrRatesTableMetaData.TABLE_NAME);
				qb.setProjectionMap(sCurrRatesProjectionMap);
				qb.appendWhere(CurrRatesTableMetaData._ID + "=" + uri.getPathSegments().get(1));
				if (TextUtils.isEmpty(sortOrder))
					orderBy = CurrRatesTableMetaData.DEFAULT_SORT_ORDER;
				break;

			case VCURRRATES_COLLECTION_URI_INDICATOR:
				qb.setTables(VCurrRatesViewMetaData.VIEW_NAME);
				qb.setProjectionMap(sVCurrRatesProjectionMap);
				if (TextUtils.isEmpty(sortOrder))
					orderBy = VCurrRatesViewMetaData.DEFAULT_SORT_ORDER;
				break;

			case TRANSACTIONS_COLLECTION_URI_INDICATOR:
				qb.setTables(TransactionsTableMetaData.TABLE_NAME);
				qb.setProjectionMap(sTransactionProjectionMap);
				if (TextUtils.isEmpty(sortOrder))
					orderBy = TransactionsTableMetaData.DEFAULT_SORT_ORDER;
				break;
			case TRANSACTIONS_SINGLE_URI_INDICATOR:
				qb.setTables(TransactionsTableMetaData.TABLE_NAME);
				qb.setProjectionMap(sTransactionProjectionMap);
				qb.appendWhere(TransactionsTableMetaData._ID + "=" + uri.getPathSegments().get(1));
				if (TextUtils.isEmpty(sortOrder))
					orderBy = TransactionsTableMetaData.DEFAULT_SORT_ORDER;
				break;

			case VTRANSACTION_COLLECTION_URI_INDICATOR:
				qb.setTables(VTransactionViewMetaData.VIEW_NAME);
				qb.setProjectionMap(sVTransactionProjectionMap);
				if (TextUtils.isEmpty(sortOrder))
					orderBy = VTransactionViewMetaData.DEFAULT_SORT_ORDER;
				break;
			case VTRANSACTION_SINGLE_URI_INDICATOR:
				qb.setTables(VTransactionViewMetaData.VIEW_NAME);
				qb.setProjectionMap(sVTransactionProjectionMap);
				qb.appendWhere(VTransactionViewMetaData._ID + "=" + uri.getPathSegments().get(1));
				if (TextUtils.isEmpty(sortOrder))
					orderBy = VTransactionViewMetaData.DEFAULT_SORT_ORDER;
				break;

			case VACCOUNT_COLLECTION_URI_INDICATOR:
				qb.setTables(VAccountsViewMetaData.VIEW_NAME);
				qb.setProjectionMap(sVAccountsProjectionMap);
				if (TextUtils.isEmpty(sortOrder))
					orderBy = VAccountsViewMetaData.DEFAULT_SORT_ORDER;
				break;
			case VACCOUNT_SINGLE_URI_INDICATOR:
				qb.setTables(VAccountsViewMetaData.VIEW_NAME);
				qb.setProjectionMap(sVAccountsProjectionMap);
				qb.appendWhere(VAccountsViewMetaData._ID + "=" + uri.getPathSegments().get(1));
				if (TextUtils.isEmpty(sortOrder))
					orderBy = VAccountsViewMetaData.DEFAULT_SORT_ORDER;
				break;

			case VTRANSACCOUNT_COLLECTION_URI_INDICATOR:
				qb.setTables(VTransAccountViewMetaData.VIEW_NAME);
				qb.setProjectionMap(sVTransAccountsProjectionMap);
				if (TextUtils.isEmpty(sortOrder))
					orderBy = VTransAccountViewMetaData.DEFAULT_SORT_ORDER;
				break;

			case TRANSFER_COLLECTION_URI_INDICATOR:
				qb.setTables(TransferTableMetaData.TABLE_NAME);
				qb.setProjectionMap(sTransferProjectionMap);
				if (TextUtils.isEmpty(sortOrder))
					orderBy = TransferTableMetaData.DEFAULT_SORT_ORDER;
				break;
			case TRANSFER_SINGLE_URI_INDICATOR:
				qb.setTables(TransferTableMetaData.TABLE_NAME);
				qb.setProjectionMap(sTransferProjectionMap);
				qb.appendWhere(TransferTableMetaData._ID + "=" + uri.getPathSegments().get(1));
				if (TextUtils.isEmpty(sortOrder))
					orderBy = TransferTableMetaData.DEFAULT_SORT_ORDER;
				break;

			case DEBTS_COLLECTION_URI_INDICATOR:
				qb.setTables(DebtsTableMetaData.TABLE_NAME);
				qb.setProjectionMap(sDebtsProjectionMap);
				if (TextUtils.isEmpty(sortOrder))
					orderBy = DebtsTableMetaData.DEFAULT_SORT_ORDER;
				break;
			case DEBTS_SINGLE_URI_INDICATOR:
				qb.setTables(DebtsTableMetaData.TABLE_NAME);
				qb.setProjectionMap(sDebtsProjectionMap);
				qb.appendWhere(DebtsTableMetaData._ID + "=" + uri.getPathSegments().get(1));
				if (TextUtils.isEmpty(sortOrder))
					orderBy = DebtsTableMetaData.DEFAULT_SORT_ORDER;
				break;

			case BUDGET_COLLECTION_URI_INDICATOR:
				qb.setTables(BudgetTableMetaData.TABLE_NAME);
				qb.setProjectionMap(sBudgetProjectionMap);
				if (TextUtils.isEmpty(sortOrder))
					orderBy = BudgetTableMetaData.DEFAULT_SORT_ORDER;
				break;
			case BUDGET_SINGLE_URI_INDICATOR:
				qb.setTables(BudgetTableMetaData.TABLE_NAME);
				qb.setProjectionMap(sBudgetProjectionMap);
				qb.appendWhere(BudgetTableMetaData._ID + "=" + uri.getPathSegments().get(1));
				if (TextUtils.isEmpty(sortOrder))
					orderBy = BudgetTableMetaData.DEFAULT_SORT_ORDER;
				break;

			case BUDGET_CATEGORY_COLLECTION_URI_INDICATOR:
				qb.setTables(BudgetCategoriesTableMetaData.TABLE_NAME);
				qb.setProjectionMap(sBudgetCategoriesProjectionMap);
				if (TextUtils.isEmpty(sortOrder))
					orderBy = BudgetCategoriesTableMetaData.DEFAULT_SORT_ORDER;
				break;
			case BUDGET_CATEGORY_SINGLE_URI_INDICATOR:
				qb.setTables(BudgetCategoriesTableMetaData.TABLE_NAME);
				qb.setProjectionMap(sBudgetCategoriesProjectionMap);
				qb.appendWhere(BudgetCategoriesTableMetaData._ID + "=" + uri.getPathSegments().get(1));
				if (TextUtils.isEmpty(sortOrder))
					orderBy = BudgetCategoriesTableMetaData.DEFAULT_SORT_ORDER;
				break;

			case BUDGET_GOALS_COLLECTION_URI_INDICATOR:
				qb.setTables(BudgetGoalsTableMetaData.TABLE_NAME);
				qb.setProjectionMap(sBudgetGoalsProjectionMap);
				if (TextUtils.isEmpty(sortOrder))
					orderBy = BudgetGoalsTableMetaData.DEFAULT_SORT_ORDER;
				break;
			case BUDGET_GOALS_SINGLE_URI_INDICATOR:
				qb.setTables(BudgetGoalsTableMetaData.TABLE_NAME);
				qb.setProjectionMap(sBudgetGoalsProjectionMap);
				qb.appendWhere(BudgetGoalsTableMetaData._ID + "=" + uri.getPathSegments().get(1));
				if (TextUtils.isEmpty(sortOrder))
					orderBy = BudgetGoalsTableMetaData.DEFAULT_SORT_ORDER;
				break;

			case TRANSACTION_STATUS_COLLECTION_URI_INDICATOR:
				qb.setTables(TransactionStatusTableMetaData.TABLE_NAME);
				qb.setProjectionMap(sTransactionStatusProjectionMap);
				if (TextUtils.isEmpty(sortOrder))
					orderBy = TransactionStatusTableMetaData.DEFAULT_SORT_ORDER;
				break;
			case TRANSACTION_STATUS_SINGLE_URI_INDICATOR:
				qb.setTables(TransactionStatusTableMetaData.TABLE_NAME);
				qb.setProjectionMap(sTransactionStatusProjectionMap);
				qb.appendWhere(TransactionStatusTableMetaData._ID + "=" + uri.getPathSegments().get(1));
				if (TextUtils.isEmpty(sortOrder))
					orderBy = TransactionStatusTableMetaData.DEFAULT_SORT_ORDER;
				break;

			case PAYMENT_METHODS_COLLECTION_URI_INDICATOR:
				qb.setTables(PaymentMethodsTableMetaData.TABLE_NAME);
				qb.setProjectionMap(sPaymentMethodsProjectionMap);
				if (TextUtils.isEmpty(sortOrder))
					orderBy = PaymentMethodsTableMetaData.DEFAULT_SORT_ORDER;
				break;
			case PAYMENT_METHODS_SINGLE_URI_INDICATOR:
				qb.setTables(PaymentMethodsTableMetaData.TABLE_NAME);
				qb.setProjectionMap(sPaymentMethodsProjectionMap);
				qb.appendWhere(PaymentMethodsTableMetaData._ID + "=" + uri.getPathSegments().get(1));
				if (TextUtils.isEmpty(sortOrder))
					orderBy = PaymentMethodsTableMetaData.DEFAULT_SORT_ORDER;
				break;

			case VTRANSFER_COLLECTION_URI_INDICATOR:
				qb.setTables(VTransferViewMetaData.VIEW_NAME);
				qb.setProjectionMap(sVTransferProjectionMap);
				if (TextUtils.isEmpty(sortOrder))
					orderBy = VTransferViewMetaData.DEFAULT_SORT_ORDER;
				break;
			case VTRANSFER_SINGLE_URI_INDICATOR:
				qb.setTables(VTransferViewMetaData.VIEW_NAME);
				qb.setProjectionMap(sVTransferProjectionMap);
				qb.appendWhere(VTransferViewMetaData._ID + "=" + uri.getPathSegments().get(1));
				if (TextUtils.isEmpty(sortOrder))
					orderBy = VTransferViewMetaData.DEFAULT_SORT_ORDER;
				break;

			case VCATEGORIES_COLLECTION_URI_INDICATOR:
				qb.setTables(VCategoriesViewMetaData.VIEW_NAME);
				qb.setProjectionMap(sVCategoriesProjectionMap);
				if (TextUtils.isEmpty(sortOrder))
					orderBy = VCategoriesViewMetaData.DEFAULT_SORT_ORDER;
				break;
			case VCATEGORIES_SINGLE_URI_INDICATOR:
				qb.setTables(VCategoriesViewMetaData.VIEW_NAME);
				qb.setProjectionMap(sVCategoriesProjectionMap);
				qb.appendWhere(VCategoriesViewMetaData._ID + "=" + uri.getPathSegments().get(1));
				if (TextUtils.isEmpty(sortOrder))
					orderBy = VCategoriesViewMetaData.DEFAULT_SORT_ORDER;
				break;

			case VDEBTS_COLLECTION_URI_INDICATOR:
				qb.setTables(VDebtsViewMetaData.VIEW_NAME);
				qb.setProjectionMap(sVDebtsProjectionMap);
				if (TextUtils.isEmpty(sortOrder))
					orderBy = VDebtsViewMetaData.DEFAULT_SORT_ORDER;
				break;
			case VDEBTS_SINGLE_URI_INDICATOR:
				qb.setTables(VDebtsViewMetaData.VIEW_NAME);
				qb.setProjectionMap(sVDebtsProjectionMap);
				qb.appendWhere(VDebtsViewMetaData._ID + "=" + uri.getPathSegments().get(1));
				if (TextUtils.isEmpty(sortOrder))
					orderBy = VDebtsViewMetaData.DEFAULT_SORT_ORDER;
				break;

			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// Get the database and run the query
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

		// Tell the cursor what uri to watch,
		// so it knows when its source data changes
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}
 
    @Override 
    public Uri insert(Uri uri, ContentValues initialValues) {
		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

		//MyApplication myApp = (MyApplication)

		//Long now = Long.valueOf(System.currentTimeMillis());
		String now = Tools.DateToString(Calendar.getInstance().getTime(), Constants.DateFormatDBLong);

		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		long rowId;
		Uri contentUri;

		switch (sUriMatcher.match(uri)) {
			case CURRENCY_COLLECTION_URI_INDICATOR:
				if (!values.containsKey(CurrencyTableMetaData.CREATED_DATE))
					values.put(CurrencyTableMetaData.CREATED_DATE, now);
				if (!values.containsKey(CurrencyTableMetaData.NAME))
					throw new SQLException("Failed to insert row because Currency Name is needed " + uri);
				if (!values.containsKey(CurrencyTableMetaData.SIGN))
					throw new SQLException("Failed to insert row because Currency Sign is needed " + uri);
				rowId = db.insert(CurrencyTableMetaData.TABLE_NAME, null, values);
				contentUri = CurrencyTableMetaData.CONTENT_URI;
				break;
			case ACCOUNT_COLLECTION_URI_INDICATOR:
				if (!values.containsKey(AccountTableMetaData.CREATED_DATE))
					values.put(CurrencyTableMetaData.CREATED_DATE, now);
				if (!values.containsKey(AccountTableMetaData.NAME))
					throw new SQLException("Failed to insert row because Account Name is needed " + uri);
				rowId = db.insert(AccountTableMetaData.TABLE_NAME, null, values);
				contentUri = VAccountsViewMetaData.CONTENT_URI;
				break;
			case TRANSACTION_STATUS_COLLECTION_URI_INDICATOR:
				if (!values.containsKey(TransactionStatusTableMetaData.CREATED_DATE))
					values.put(TransactionStatusTableMetaData.CREATED_DATE, now);
				if (!values.containsKey(TransactionStatusTableMetaData.NAME))
					throw new SQLException("Failed to insert row because Name is needed " + uri);
				rowId = db.insert(TransactionStatusTableMetaData.TABLE_NAME, null, values);
				contentUri = TransactionStatusTableMetaData.CONTENT_URI;
				break;
			case PAYMENT_METHODS_COLLECTION_URI_INDICATOR:
				if (!values.containsKey(PaymentMethodsTableMetaData.CREATED_DATE))
					values.put(PaymentMethodsTableMetaData.CREATED_DATE, now);
				if (!values.containsKey(PaymentMethodsTableMetaData.NAME))
					throw new SQLException("Failed to insert row because Name is needed " + uri);
				rowId = db.insert(PaymentMethodsTableMetaData.TABLE_NAME, null, values);
				contentUri = PaymentMethodsTableMetaData.CONTENT_URI;
				break;
			case CATEGORY_COLLECTION_URI_INDICATOR:
				if (!values.containsKey(CategoryTableMetaData.CREATED_DATE))
					values.put(CurrencyTableMetaData.CREATED_DATE, now);
				if (!values.containsKey(CategoryTableMetaData.NAME))
					throw new SQLException("Failed to insert row because Category Name is needed " + uri);
				rowId = db.insert(CategoryTableMetaData.TABLE_NAME, null, values);
				contentUri = CategoryTableMetaData.CONTENT_URI;
				break;
			case CURRRATES_COLLECTION_URI_INDICATOR:
				if (!values.containsKey(CurrRatesTableMetaData.CREATED_DATE))
					values.put(CurrRatesTableMetaData.CREATED_DATE, now);
				if (!values.containsKey(CurrRatesTableMetaData.FIRSTCURRID))
					throw new SQLException("Failed to insert row because first currency is needed " + uri);
				if (!values.containsKey(CurrRatesTableMetaData.SECONDCURRID))
					throw new SQLException("Failed to insert row because second currency is needed " + uri);
				if (!values.containsKey(CurrRatesTableMetaData.VALUE))
					throw new SQLException("Failed to insert row because rate is needed " + uri);
				rowId = db.insert(CurrRatesTableMetaData.TABLE_NAME, null, values);
				contentUri = CurrRatesTableMetaData.CONTENT_URI;
				myApp.refreshMainDetails();
				//MyApplication.refreshMainDetails();
				break;
			case TRANSACTIONS_COLLECTION_URI_INDICATOR:
				if (!values.containsKey(TransactionsTableMetaData.CREATED_DATE))
					values.put(TransactionsTableMetaData.CREATED_DATE, now);
				if (!values.containsKey(TransactionsTableMetaData.ACCOUNTID))
					throw new SQLException("Failed to insert row because account is needed " + uri);
				if (!values.containsKey(TransactionsTableMetaData.CATEGORYID))
					values.put(TransactionsTableMetaData.CREATED_DATE, "0");
				if (!values.containsKey(TransactionsTableMetaData.TRANSDATE))
					throw new SQLException("Failed to insert row because date is needed " + uri);
				if (!values.containsKey(TransactionsTableMetaData.AMOUNT))
					throw new SQLException("Failed to insert row because amount is needed " + uri);
				rowId = db.insert(TransactionsTableMetaData.TABLE_NAME, null, values);
				contentUri = TransactionsTableMetaData.CONTENT_URI;
				myApp.refreshMainDetails();
				//MainScreen.refreshDetails();
				break;
			case TRANSFER_COLLECTION_URI_INDICATOR:
				if (!values.containsKey(TransferTableMetaData.CREATED_DATE))
					values.put(TransferTableMetaData.CREATED_DATE, now);
				if (!values.containsKey(TransferTableMetaData.AMOUNT))
					throw new SQLException("Failed to insert row because amount is needed " + uri);
				if (!values.containsKey(TransferTableMetaData.REPEATTYPE))
					throw new SQLException("Failed to insert row because repeat type is needed " + uri);
				if (!values.containsKey(TransferTableMetaData.TRANSDATE))
					throw new SQLException("Failed to insert row because date is needed " + uri);
				rowId = db.insert(TransferTableMetaData.TABLE_NAME, null, values);
				contentUri = TransferTableMetaData.CONTENT_URI;
				myApp.refreshMainDetails();
				//MainScreen.refreshDetails();
				break;
			case DEBTS_COLLECTION_URI_INDICATOR:
				if (!values.containsKey(DebtsTableMetaData.CREATED_DATE))
					values.put(DebtsTableMetaData.CREATED_DATE, now);
				rowId = db.insert(DebtsTableMetaData.TABLE_NAME, null, values);
				contentUri = DebtsTableMetaData.CONTENT_URI;
				getContext().getContentResolver().notifyChange(VDebtsViewMetaData.CONTENT_URI, null);
				break;
			case BUDGET_COLLECTION_URI_INDICATOR:
				if (!values.containsKey(BudgetTableMetaData.CREATED_DATE))
					values.put(BudgetTableMetaData.CREATED_DATE, now);
				rowId = db.insert(BudgetTableMetaData.TABLE_NAME, null, values);
				contentUri = BudgetTableMetaData.CONTENT_URI;
				break;
			case BUDGET_CATEGORY_COLLECTION_URI_INDICATOR:
				if (!values.containsKey(BudgetCategoriesTableMetaData.CREATED_DATE))
					values.put(BudgetCategoriesTableMetaData.CREATED_DATE, now);
				if (!values.containsKey(BudgetCategoriesTableMetaData.USED_AMOUNT))
					values.put(BudgetCategoriesTableMetaData.USED_AMOUNT, 0);
				if (!values.containsKey(BudgetCategoriesTableMetaData.REPEAT))
					values.put(BudgetCategoriesTableMetaData.REPEAT, 0);
				rowId = db.insert(BudgetCategoriesTableMetaData.TABLE_NAME, null, values);
				contentUri = BudgetCategoriesTableMetaData.CONTENT_URI;
				break;
			case BUDGET_GOALS_COLLECTION_URI_INDICATOR:
				if (!values.containsKey(BudgetGoalsTableMetaData.CREATED_DATE))
					values.put(BudgetGoalsTableMetaData.CREATED_DATE, now);
				rowId = db.insert(BudgetGoalsTableMetaData.TABLE_NAME, null, values);
				contentUri = BudgetGoalsTableMetaData.CONTENT_URI;
				break;
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}

		if (rowId > 0) {
			Uri insertedUri = ContentUris.withAppendedId(contentUri, rowId);
			getContext().getContentResolver().notifyChange(insertedUri, null);
			return insertedUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}
 
    @Override 
    public int delete(Uri uri, String where, String[] whereArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		String rowId;
		switch (sUriMatcher.match(uri)) {
			case CURRENCY_COLLECTION_URI_INDICATOR:
				count = db.delete(CurrencyTableMetaData.TABLE_NAME, where, whereArgs);
				break;
			case CURRENCY_SINGLE_URI_INDICATOR:
				rowId = uri.getPathSegments().get(1);
				count = db.delete(CurrencyTableMetaData.TABLE_NAME, CurrencyTableMetaData._ID + "=" + rowId
						+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
				break;

			case ACCOUNT_COLLECTION_URI_INDICATOR:
				count = db.delete(AccountTableMetaData.TABLE_NAME, where, whereArgs);
				getContext().getContentResolver().notifyChange(VAccountsViewMetaData.CONTENT_URI, null);
				break;
			case ACCOUNT_SINGLE_URI_INDICATOR:
				rowId = uri.getPathSegments().get(1);
				count = db.delete(AccountTableMetaData.TABLE_NAME, AccountTableMetaData._ID + "=" + rowId
						+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
				String id = "12";
				getContext().getContentResolver().notifyChange(Uri.withAppendedPath(VAccountsViewMetaData.CONTENT_URI, id), null);
				break;

			case CATEGORY_COLLECTION_URI_INDICATOR:
				count = db.delete(CategoryTableMetaData.TABLE_NAME, where, whereArgs);
				break;
			case CATEGORY_SINGLE_URI_INDICATOR:
				rowId = uri.getPathSegments().get(1);
				count = db.delete(CategoryTableMetaData.TABLE_NAME, CategoryTableMetaData._ID + "=" + rowId
						+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
				break;

			case CURRRATES_COLLECTION_URI_INDICATOR:
				count = db.delete(CurrRatesTableMetaData.TABLE_NAME, where, whereArgs);
				break;
			case CURRRATES_SINGLE_URI_INDICATOR:
				rowId = uri.getPathSegments().get(1);
				count = db.delete(CurrRatesTableMetaData.TABLE_NAME, CurrRatesTableMetaData._ID + "=" + rowId
						+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
				break;

			case TRANSACTIONS_COLLECTION_URI_INDICATOR:
				count = db.delete(TransactionsTableMetaData.TABLE_NAME, where, whereArgs);
				getContext().getContentResolver().notifyChange(VTransactionViewMetaData.CONTENT_URI, null);
				myApp.refreshMainDetails();
				//MainScreen.refreshDetails();
				break;
			case TRANSACTIONS_SINGLE_URI_INDICATOR:
				rowId = uri.getPathSegments().get(1);
				count = db.delete(TransactionsTableMetaData.TABLE_NAME, TransactionsTableMetaData._ID + "=" + rowId
						+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
				getContext().getContentResolver().notifyChange(VTransactionViewMetaData.CONTENT_URI, null);
				myApp.refreshMainDetails();
				//MainScreen.refreshDetails();
				break;

			case TRANSFER_COLLECTION_URI_INDICATOR:
				count = db.delete(TransferTableMetaData.TABLE_NAME, where, whereArgs);
				getContext().getContentResolver().notifyChange(VTransferViewMetaData.CONTENT_URI, null);
				myApp.refreshMainDetails();
				//MainScreen.refreshDetails();
				break;
			case TRANSFER_SINGLE_URI_INDICATOR:
				rowId = uri.getPathSegments().get(1);
				count = db.delete(TransferTableMetaData.TABLE_NAME, TransferTableMetaData._ID + "=" + rowId
						+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
				getContext().getContentResolver().notifyChange(VTransferViewMetaData.CONTENT_URI, null);
				myApp.refreshMainDetails();
				//MainScreen.refreshDetails();
				break;

			case DEBTS_COLLECTION_URI_INDICATOR:
				count = db.delete(DebtsTableMetaData.TABLE_NAME, where, whereArgs);
				getContext().getContentResolver().notifyChange(VDebtsViewMetaData.CONTENT_URI, null);
				break;
			case DEBTS_SINGLE_URI_INDICATOR:
				rowId = uri.getPathSegments().get(1);
				count = db.delete(DebtsTableMetaData.TABLE_NAME, DebtsTableMetaData._ID + "=" + rowId
						+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
				getContext().getContentResolver().notifyChange(VDebtsViewMetaData.CONTENT_URI, null);
				break;

			case BUDGET_COLLECTION_URI_INDICATOR:
				count = db.delete(BudgetTableMetaData.TABLE_NAME, where, whereArgs);
				break;
			case BUDGET_SINGLE_URI_INDICATOR:
				rowId = uri.getPathSegments().get(1);
				count = db.delete(BudgetTableMetaData.TABLE_NAME, BudgetTableMetaData._ID + "=" + rowId
						+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
				break;

			case BUDGET_CATEGORY_COLLECTION_URI_INDICATOR:
				count = db.delete(BudgetCategoriesTableMetaData.TABLE_NAME, where, whereArgs);
				break;
			case BUDGET_CATEGORY_SINGLE_URI_INDICATOR:
				rowId = uri.getPathSegments().get(1);
				count = db.delete(BudgetCategoriesTableMetaData.TABLE_NAME, BudgetCategoriesTableMetaData._ID + "=" + rowId
						+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
				break;

			case BUDGET_GOALS_COLLECTION_URI_INDICATOR:
				count = db.delete(BudgetGoalsTableMetaData.TABLE_NAME, where, whereArgs);
				break;
			case BUDGET_GOALS_SINGLE_URI_INDICATOR:
				rowId = uri.getPathSegments().get(1);
				count = db.delete(BudgetGoalsTableMetaData.TABLE_NAME, BudgetGoalsTableMetaData._ID + "=" + rowId
						+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
				break;

			case TRANSACTION_STATUS_COLLECTION_URI_INDICATOR:
				count = db.delete(TransactionStatusTableMetaData.TABLE_NAME, where, whereArgs);
				break;
			case TRANSACTION_STATUS_SINGLE_URI_INDICATOR:
				rowId = uri.getPathSegments().get(1);
				count = db.delete(TransactionStatusTableMetaData.TABLE_NAME, TransactionStatusTableMetaData._ID + "=" + rowId
						+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
				break;

			case PAYMENT_METHODS_COLLECTION_URI_INDICATOR:
				count = db.delete(PaymentMethodsTableMetaData.TABLE_NAME, where, whereArgs);
				break;
			case PAYMENT_METHODS_SINGLE_URI_INDICATOR:
				rowId = uri.getPathSegments().get(1);
				count = db.delete(PaymentMethodsTableMetaData.TABLE_NAME, PaymentMethodsTableMetaData._ID + "=" + rowId
						+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
				break;

			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}
 
    @Override 
    public int update(Uri uri, ContentValues values,  
            String where, String[] whereArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		String rowId;
		//Long now = Long.valueOf(System.currentTimeMillis());
		String now = Tools.DateToString(Calendar.getInstance().getTime(), Constants.DateFormatDBLong);
		switch (sUriMatcher.match(uri)) {
			case CURRENCY_COLLECTION_URI_INDICATOR:
				if (!values.containsKey(CurrencyTableMetaData.MODIFIED_DATE))
					values.put(CurrencyTableMetaData.MODIFIED_DATE, now);
				count = db.update(CurrencyTableMetaData.TABLE_NAME,
						values, where, whereArgs);
				break;
			case CURRENCY_SINGLE_URI_INDICATOR:
				if (!values.containsKey(CurrencyTableMetaData.MODIFIED_DATE))
					values.put(CurrencyTableMetaData.MODIFIED_DATE, now);
				rowId = uri.getPathSegments().get(1);
				count = db.update(CurrencyTableMetaData.TABLE_NAME,
						values, CurrencyTableMetaData._ID + "=" + rowId
								+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
						whereArgs);
				break;

			case ACCOUNT_COLLECTION_URI_INDICATOR:
				if (!values.containsKey(AccountTableMetaData.MODIFIED_DATE))
					values.put(AccountTableMetaData.MODIFIED_DATE, now);
				count = db.update(AccountTableMetaData.TABLE_NAME,
						values, where, whereArgs);
				getContext().getContentResolver().notifyChange(VAccountsViewMetaData.CONTENT_URI, null);
				break;
			case ACCOUNT_SINGLE_URI_INDICATOR:
				if (!values.containsKey(AccountTableMetaData.MODIFIED_DATE))
					values.put(AccountTableMetaData.MODIFIED_DATE, now);
				rowId = uri.getPathSegments().get(1);
				count = db.update(AccountTableMetaData.TABLE_NAME,
						values, AccountTableMetaData._ID + "=" + rowId
								+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
						whereArgs);
				getContext().getContentResolver().notifyChange(VAccountsViewMetaData.CONTENT_URI, null);
				break;

			case CATEGORY_COLLECTION_URI_INDICATOR:
				if (!values.containsKey(CategoryTableMetaData.MODIFIED_DATE))
					values.put(CategoryTableMetaData.MODIFIED_DATE, now);
				count = db.update(CategoryTableMetaData.TABLE_NAME,
						values, where, whereArgs);
				break;
			case CATEGORY_SINGLE_URI_INDICATOR:
				if (!values.containsKey(CategoryTableMetaData.MODIFIED_DATE))
					values.put(CategoryTableMetaData.MODIFIED_DATE, now);
				rowId = uri.getPathSegments().get(1);
				count = db.update(CategoryTableMetaData.TABLE_NAME,
						values, CategoryTableMetaData._ID + "=" + rowId
								+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
						whereArgs);
				break;

			case CURRRATES_COLLECTION_URI_INDICATOR:
				if (!values.containsKey(CurrRatesTableMetaData.MODIFIED_DATE))
					values.put(CurrRatesTableMetaData.MODIFIED_DATE, now);
				count = db.update(CurrRatesTableMetaData.TABLE_NAME,
						values, where, whereArgs);
				break;
			case CURRRATES_SINGLE_URI_INDICATOR:
				if (!values.containsKey(CurrRatesTableMetaData.MODIFIED_DATE))
					values.put(CurrRatesTableMetaData.MODIFIED_DATE, now);
				rowId = uri.getPathSegments().get(1);
				count = db.update(CurrRatesTableMetaData.TABLE_NAME,
						values, CurrRatesTableMetaData._ID + "=" + rowId
								+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
						whereArgs);
				break;

			case TRANSACTIONS_COLLECTION_URI_INDICATOR:
				if (!values.containsKey(TransactionsTableMetaData.MODIFIED_DATE))
					values.put(TransactionsTableMetaData.MODIFIED_DATE, now);
				count = db.update(TransactionsTableMetaData.TABLE_NAME,
						values, where, whereArgs);
				myApp.refreshMainDetails();
				//MainScreen.refreshDetails();
				break;
			case TRANSACTIONS_SINGLE_URI_INDICATOR:
				if (!values.containsKey(TransactionsTableMetaData.MODIFIED_DATE))
					values.put(TransactionsTableMetaData.MODIFIED_DATE, now);
				rowId = uri.getPathSegments().get(1);
				count = db.update(TransactionsTableMetaData.TABLE_NAME,
						values, TransactionsTableMetaData._ID + "=" + rowId
								+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
						whereArgs);
				getContext().getContentResolver().notifyChange(VTransactionViewMetaData.CONTENT_URI, null);
				myApp.refreshMainDetails();
				//MainScreen.refreshDetails();
				break;

			case TRANSFER_COLLECTION_URI_INDICATOR:
				if (!values.containsKey(TransferTableMetaData.MODIFIED_DATE))
					values.put(TransferTableMetaData.MODIFIED_DATE, now);
				count = db.update(TransferTableMetaData.TABLE_NAME,
						values, where, whereArgs);
				getContext().getContentResolver().notifyChange(VTransferViewMetaData.CONTENT_URI, null);
				getContext().getContentResolver().notifyChange(VTransactionViewMetaData.CONTENT_URI, null);
				myApp.refreshMainDetails();
				//MainScreen.refreshDetails();
				break;
			case TRANSFER_SINGLE_URI_INDICATOR:
				if (!values.containsKey(TransferTableMetaData.MODIFIED_DATE))
					values.put(TransferTableMetaData.MODIFIED_DATE, now);
				rowId = uri.getPathSegments().get(1);
				count = db.update(TransferTableMetaData.TABLE_NAME,
						values, TransferTableMetaData._ID + "=" + rowId
								+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
						whereArgs);
				getContext().getContentResolver().notifyChange(VTransferViewMetaData.CONTENT_URI, null);
				getContext().getContentResolver().notifyChange(VTransactionViewMetaData.CONTENT_URI, null);
				myApp.refreshMainDetails();
				//MainScreen.refreshDetails();
				break;

			case DEBTS_COLLECTION_URI_INDICATOR:
				if (!values.containsKey(DebtsTableMetaData.MODIFIED_DATE))
					values.put(DebtsTableMetaData.MODIFIED_DATE, now);
				count = db.update(DebtsTableMetaData.TABLE_NAME, values, where, whereArgs);
				getContext().getContentResolver().notifyChange(VDebtsViewMetaData.CONTENT_URI, null);
				break;
			case DEBTS_SINGLE_URI_INDICATOR:
				if (!values.containsKey(DebtsTableMetaData.MODIFIED_DATE))
					values.put(DebtsTableMetaData.MODIFIED_DATE, now);
				rowId = uri.getPathSegments().get(1);
				count = db.update(DebtsTableMetaData.TABLE_NAME,
						values, DebtsTableMetaData._ID + "=" + rowId
								+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
						whereArgs);
				getContext().getContentResolver().notifyChange(VDebtsViewMetaData.CONTENT_URI, null);
				break;

			case BUDGET_COLLECTION_URI_INDICATOR:
				if (!values.containsKey(BudgetTableMetaData.MODIFIED_DATE))
					values.put(BudgetTableMetaData.MODIFIED_DATE, now);
				count = db.update(BudgetTableMetaData.TABLE_NAME, values, where, whereArgs);
				break;
			case BUDGET_SINGLE_URI_INDICATOR:
				if (!values.containsKey(BudgetTableMetaData.MODIFIED_DATE))
					values.put(BudgetTableMetaData.MODIFIED_DATE, now);
				rowId = uri.getPathSegments().get(1);
				count = db.update(BudgetTableMetaData.TABLE_NAME,
						values, BudgetTableMetaData._ID + "=" + rowId
								+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
						whereArgs);
				break;

			case BUDGET_CATEGORY_COLLECTION_URI_INDICATOR:
				if (!values.containsKey(BudgetCategoriesTableMetaData.MODIFIED_DATE))
					values.put(BudgetCategoriesTableMetaData.MODIFIED_DATE, now);
				count = db.update(BudgetCategoriesTableMetaData.TABLE_NAME, values, where, whereArgs);
				break;
			case BUDGET_CATEGORY_SINGLE_URI_INDICATOR:
				if (!values.containsKey(BudgetCategoriesTableMetaData.MODIFIED_DATE))
					values.put(BudgetCategoriesTableMetaData.MODIFIED_DATE, now);
				rowId = uri.getPathSegments().get(1);
				count = db.update(BudgetCategoriesTableMetaData.TABLE_NAME,
						values, BudgetCategoriesTableMetaData._ID + "=" + rowId
								+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
						whereArgs);
				break;

			case BUDGET_GOALS_COLLECTION_URI_INDICATOR:
				if (!values.containsKey(BudgetGoalsTableMetaData.MODIFIED_DATE))
					values.put(BudgetGoalsTableMetaData.MODIFIED_DATE, now);
				count = db.update(BudgetGoalsTableMetaData.TABLE_NAME, values, where, whereArgs);
				break;
			case BUDGET_GOALS_SINGLE_URI_INDICATOR:
				if (!values.containsKey(BudgetGoalsTableMetaData.MODIFIED_DATE))
					values.put(BudgetGoalsTableMetaData.MODIFIED_DATE, now);
				rowId = uri.getPathSegments().get(1);
				count = db.update(BudgetGoalsTableMetaData.TABLE_NAME,
						values, BudgetGoalsTableMetaData._ID + "=" + rowId
								+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
						whereArgs);
				break;

			case TRANSACTION_STATUS_COLLECTION_URI_INDICATOR:
				if (!values.containsKey(TransactionStatusTableMetaData.MODIFIED_DATE))
					values.put(TransactionStatusTableMetaData.MODIFIED_DATE, now);
				count = db.update(TransactionStatusTableMetaData.TABLE_NAME, values, where, whereArgs);
				break;
			case TRANSACTION_STATUS_SINGLE_URI_INDICATOR:
				if (!values.containsKey(TransactionStatusTableMetaData.MODIFIED_DATE))
					values.put(TransactionStatusTableMetaData.MODIFIED_DATE, now);
				rowId = uri.getPathSegments().get(1);
				count = db.update(TransactionStatusTableMetaData.TABLE_NAME,
						values, TransactionStatusTableMetaData._ID + "=" + rowId
								+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
						whereArgs);
				break;

			case PAYMENT_METHODS_COLLECTION_URI_INDICATOR:
				if (!values.containsKey(PaymentMethodsTableMetaData.MODIFIED_DATE))
					values.put(PaymentMethodsTableMetaData.MODIFIED_DATE, now);
				count = db.update(PaymentMethodsTableMetaData.TABLE_NAME, values, where, whereArgs);
				break;
			case PAYMENT_METHODS_SINGLE_URI_INDICATOR:
				if (!values.containsKey(PaymentMethodsTableMetaData.MODIFIED_DATE))
					values.put(PaymentMethodsTableMetaData.MODIFIED_DATE, now);
				rowId = uri.getPathSegments().get(1);
				count = db.update(PaymentMethodsTableMetaData.TABLE_NAME,
						values, PaymentMethodsTableMetaData._ID + "=" + rowId
								+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
						whereArgs);
				break;

			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}	
}
