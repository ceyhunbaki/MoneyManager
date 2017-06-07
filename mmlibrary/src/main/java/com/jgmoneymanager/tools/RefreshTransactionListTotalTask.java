package com.jgmoneymanager.tools;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VTransactionViewMetaData;
import com.jgmoneymanager.mmlibrary.R;
import com.jgmoneymanager.services.AccountSrv;
import com.jgmoneymanager.services.CurrRatesSrv;
import com.jgmoneymanager.services.CurrencySrv;

public class RefreshTransactionListTotalTask extends AsyncTask<String, Void, Boolean> {
    private final Context ctx;
    private final long selectedAccountID;
    private Cursor cursor;
    private final TextView tvTotal;
    private final String sql;
    private String text;
    private Double balance;
	
	public RefreshTransactionListTotalTask(Context context, /*Cursor cursor,*/String sql, TextView tvTotal, long selectedAccountID) {
		ctx = context;
		this.sql = sql;
		this.tvTotal = tvTotal;
		this.selectedAccountID = selectedAccountID;
	}

    // can use UI thread here
    protected void onPreExecute() {
    	tvTotal.setText("...");
    }

    // automatically done on worker thread (separate from UI thread)
    protected Boolean doInBackground(final String... args) {
    	balance = 0d;
    	try {
    		cursor = ctx.getContentResolver().query(VTransactionViewMetaData.CONTENT_URI, null,
    				sql, null, null);
    		//if (cursor != null)
    		//{
    			if (selectedAccountID == 0) {
    				for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext())
    				{			
    					long transactionCurrencyID = DBTools.getCursorColumnValueLong(cursor, VTransactionViewMetaData.CURRID);
    					double rate = 1.00d;
    					if (Constants.defaultCurrency != transactionCurrencyID)
    						rate = Tools.parseDouble(CurrRatesSrv.getRate(ctx, transactionCurrencyID, Constants.defaultCurrency,
    								DBTools.getCursorColumnValueDate(cursor, VTransactionViewMetaData.TRANSDATE)));
    						
    					balance += DBTools.getCursorColumnValueInt(cursor, VTransactionViewMetaData.TRANSTYPE) * 
    							DBTools.getCursorColumnValueFloat(cursor, VTransactionViewMetaData.AMOUNT) * rate;
    				}
    				/*if (ctx.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
    					text = ctx.getString(R.string.total) + " (" + CurrencySrv.getCurrencySignByID(ctx, Constants.defaultCurrency) + ") " + Tools.formatDecimalInUserFormat(balance);
    				else 
    					text = ctx.getString(R.string.total) + " " + Tools.formatDecimalInUserFormat(balance) + CurrencySrv.getCurrencySignByID(ctx, Constants.defaultCurrency);*/
					text = ctx.getString(R.string.total) + " " + Tools.getFullAmountText(balance, CurrencySrv.getCurrencySignByID(ctx, Constants.defaultCurrency), true);
				}
    			else {
    				cursor.moveToFirst();
    				long accountCurrencyID = AccountSrv.getCurrencyIdByAcocuntID(ctx, selectedAccountID);
    				for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext())
    				{			
    					long transactionCurrencyID = DBTools.getCursorColumnValueLong(cursor, VTransactionViewMetaData.CURRID);
    					double rate = 1.00d;
    					if (accountCurrencyID != transactionCurrencyID)
    						rate = Tools.parseDouble(CurrRatesSrv.getRate(ctx, transactionCurrencyID, accountCurrencyID,
    								DBTools.getCursorColumnValueDate(cursor, VTransactionViewMetaData.TRANSDATE)));
    					Log.i("CURRRRR:" + String.valueOf(transactionCurrencyID), 
    							"amount:" + DBTools.getCursorColumnValueFloat(cursor, VTransactionViewMetaData.AMOUNT) + 
    							"; rate:" + rate);	
    					balance += DBTools.getCursorColumnValueInt(cursor, VTransactionViewMetaData.TRANSTYPE) * 
    							DBTools.getCursorColumnValueFloat(cursor, VTransactionViewMetaData.AMOUNT) * rate;
    				}
    				/*if (ctx.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
    					text = ctx.getString(R.string.total) + " (" + CurrencySrv.getCurrencySignByID(ctx, accountCurrencyID) + ") " + Tools.formatDecimalInUserFormat(balance);
    				else
    					text = ctx.getString(R.string.total) + " " + Tools.formatDecimalInUserFormat(balance) + CurrencySrv.getCurrencySignByID(ctx, accountCurrencyID);*/
					text = ctx.getString(R.string.total) + " " + Tools.getFullAmountText(balance, CurrencySrv.getCurrencySignByID(ctx, accountCurrencyID), true);
    			}
    		//}
    		return true;
    	}
    	catch (Exception e) {
    		return false;
    	}
    }

    // can use UI thread here
    protected void onPostExecute(final Boolean success) {
       if (success) {
    	   try {
    		   tvTotal.setText(text);
    	   }
    	   catch (Exception e) {
    		   
    	   }
       }
    }

 }