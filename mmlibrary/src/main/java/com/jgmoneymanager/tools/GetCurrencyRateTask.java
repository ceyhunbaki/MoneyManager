package com.jgmoneymanager.tools;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.EditText;

import com.jgmoneymanager.mmlibrary.R;
import com.jgmoneymanager.services.CurrRatesSrv;
import com.jgmoneymanager.services.CurrencySrv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class GetCurrencyRateTask extends AsyncTask<String, Void, Boolean> {
    private final Context ctx;
    private final EditText editText;
    private final String fromCurrSign;
    private final String toCurrSign;
    private String stAmount;
    private boolean resultHas = false;
	
	public GetCurrencyRateTask(Context context, EditText edRate, String fromCurrSign, String toCurrSign) {
		ctx = context;
		editText = edRate;
		this.fromCurrSign = fromCurrSign;
		this.toCurrSign = toCurrSign;
	}

    // can use UI thread here
    protected void onPreExecute() {
    }

    // automatically done on worker thread (separate from UI thread)
    protected Boolean doInBackground(final String... args) {

		stAmount = Tools.formatDecimal(1d);
    	if (!(Tools.isInternetAvailable(ctx) && Tools.getPreferenceBool(ctx, R.string.internetRatesKey, true))) {
    		stAmount = CurrRatesSrv.getRate(ctx,
    				CurrencySrv.getCurrencyIDBySign(ctx, fromCurrSign), 
    				CurrencySrv.getCurrencyIDBySign(ctx, toCurrSign), 
    				Tools.getCurrentDate());
    		resultHas = true;
    		return true;
    	}
    	else {
	    	String stUrl = CurrRatesSrv.getRatesUrl(fromCurrSign, toCurrSign);
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
		            	stAmount = inputLine.substring(startPos + 2, inputLine.indexOf(toCurrSign) - 1).replace(",", "");
						stAmount = Tools.formatDecimal(Tools.parseDouble(stAmount), Constants.rateDecimalCount);
		            	resultHas = true;
		            }
		        }
		        bufReader.close();
		        return true;
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
    	}
    }

    // can use UI thread here
    protected void onPostExecute(final Boolean success) {
       if (success && resultHas)
    	   if (editText != null)
        	  editText.setText(Tools.formatDecimal(stAmount, Constants.rateDecimalCount));
    	   //else
    		   CurrRatesSrv.insertRate(ctx, CurrencySrv.getCurrencyIDBySign(ctx, fromCurrSign), 
    				   CurrencySrv.getCurrencyIDBySign(ctx, toCurrSign), 
    				   Tools.stringToDouble(ctx, stAmount, false), Tools.getCurrentDate());
    }

 }