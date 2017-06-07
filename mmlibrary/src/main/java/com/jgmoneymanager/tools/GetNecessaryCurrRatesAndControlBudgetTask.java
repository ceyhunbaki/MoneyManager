package com.jgmoneymanager.tools;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;

import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.AccountTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CurrencyTableMetaData;
import com.jgmoneymanager.entity.MyApplication;
import com.jgmoneymanager.mmlibrary.R;
import com.jgmoneymanager.services.CurrRatesSrv;
import com.jgmoneymanager.services.CurrencySrv;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * 	Get currency rates from internet and control new months budget
 * 	(if corresponding preferences are enabled)
 */
public class GetNecessaryCurrRatesAndControlBudgetTask extends AsyncTask<String, Void, Boolean> {
    private final Context ctx;
    private String fromCurrSign;
    private String toCurrSign;
    private long fromCurrID;
    private long toCurrID;
    private String stRate;
    private boolean resultHas = false;

    private final MyApplication cMyApp;
	
	public GetNecessaryCurrRatesAndControlBudgetTask(Context context, MyApplication myApp) {
		ctx = context;
		cMyApp = myApp;
	}

    // automatically done on worker thread (separate from UI thread)
    protected Boolean doInBackground(final String... args) {

    	if (Tools.isInternetAvailable(ctx) && Tools.getPreferenceBool(ctx, R.string.internetRatesKey, true)) {
    		toCurrSign = CurrencySrv.getDefaultCurrencySign(ctx);
    		String query = "Select distinct " + CurrencyTableMetaData.SIGN + " from " 
    				+ AccountTableMetaData.TABLE_NAME + " acc "
    				+ "join " + CurrencyTableMetaData.TABLE_NAME + " cu on cu." 
    				+ CurrencyTableMetaData._ID + " = acc." + AccountTableMetaData.CURRID 
    				+ " where " + CurrencyTableMetaData.SIGN + " != '" + toCurrSign + "' and "
    				+ AccountTableMetaData.STATUS + " = " + Constants.Status.Enabled.index();
    		Cursor cursor = DBTools.createCursor(ctx, query);
    		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
    			fromCurrSign = DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData.SIGN);
    			fromCurrID = CurrencySrv.getCurrencyIDBySign(ctx, fromCurrSign);
    			toCurrID = CurrencySrv.getCurrencyIDBySign(ctx, toCurrSign);
    			if (!CurrRatesSrv.rateExists(ctx, fromCurrID, toCurrID, Tools.getCurrentDate(), null, null)) {
    				String stUrl = CurrRatesSrv.getRatesUrl(fromCurrSign, toCurrSign);
    				stRate = Tools.formatDecimal(1, Constants.rateDecimalCount);
    				resultHas = false;
    				try {
    					URL url = new URL(stUrl);
    					URLConnection urlConn = url.openConnection();
	            
    					BufferedReader bufReader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
    					String inputLine;
    					boolean resultRateFound = false;
    					while (((inputLine = bufReader.readLine()) != null) && (!resultHas))
    					{
    						if (inputLine.indexOf("resultRate") != -1)
    							resultRateFound = true;
    						if (inputLine.length() > 0)
    							if ((inputLine.indexOf("resultColRght") != -1) && (resultRateFound) && (!resultHas)) {
    								int startPos = inputLine.indexOf("\">");
    								stRate = inputLine.substring(startPos + 2, inputLine.indexOf(toCurrSign) - 1).replace(",", "");
    								resultHas = true;
    							}
    					}
        				if (resultHas)
        					CurrRatesSrv.insertRate(ctx, fromCurrID, toCurrID, Tools.stringToDouble(ctx, stRate, false), Tools.getCurrentDate());
        				bufReader.close();
    				} catch (Exception e) {
    				}
    			}
    		}
    	}
    	return true;
    }

    // can use UI thread here
    protected void onPostExecute(final Boolean success) {
    	if (success)
    		cMyApp.refreshMainDetails();
    }

 }