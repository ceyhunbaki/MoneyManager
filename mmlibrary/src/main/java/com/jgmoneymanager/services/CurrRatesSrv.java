package com.jgmoneymanager.services;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;
import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CurrRatesTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.mmlibrary.R;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.GetCurrencyRateTask;
import com.jgmoneymanager.tools.Tools;

import java.util.Date;

public class CurrRatesSrv {

	
	public static boolean rateExists(Context context, long firstCurrID, long secondCurrID, Date rateDate, StringBuilder rate, Long exceptedID)
	{
		boolean result = false;
		try {
			if (firstCurrID != secondCurrID)
			{
				String condition = "((" + CurrRatesTableMetaData.FIRSTCURRID + " = " + String.valueOf(firstCurrID) + " and " +
						CurrRatesTableMetaData.SECONDCURRID + " = " + String.valueOf(secondCurrID) + ") or (" +
						CurrRatesTableMetaData.SECONDCURRID + " = " + String.valueOf(firstCurrID) + " and " +
						CurrRatesTableMetaData.FIRSTCURRID + " = " + String.valueOf(secondCurrID) + ")) and " +
						CurrRatesTableMetaData.RATEDATE + "<=" + Tools.DateToDBString(rateDate);
				if (exceptedID != null)
					condition += " and " + CurrRatesTableMetaData._ID + " <> " + String.valueOf(exceptedID);
				Cursor cursor = context.getContentResolver().query(CurrRatesTableMetaData.CONTENT_URI, 
						new String[] {CurrRatesTableMetaData.VALUE, CurrRatesTableMetaData.RATEDATE, CurrRatesTableMetaData.FIRSTCURRID}, 
						condition, null, CurrRatesTableMetaData.RATEDATE);
				if (cursor.moveToLast())
				{
					Double value = DBTools.getCursorColumnValueDouble(cursor, CurrRatesTableMetaData.VALUE);
					String stValue = (DBTools.getCursorColumnValueInt(cursor, CurrRatesTableMetaData.FIRSTCURRID) == firstCurrID) ?
							Tools.formatDecimal(DBTools.getCursorColumnValueDouble(cursor, CurrRatesTableMetaData.VALUE), Constants.rateDecimalCount):
							Tools.formatDecimal(1/value, Constants.rateDecimalCount);
					if (rate != null) rate.append(stValue);
                    result = DBTools.getCursorColumnValue(cursor, CurrRatesTableMetaData.RATEDATE).equals(Tools.DateToDBString(rateDate));
				}
				else 
				{
					if (rate != null) rate.append(Tools.formatDecimal(0d, Constants.rateDecimalCount));
					result = false;
				}
			}
			else 
			{
				if (rate != null) rate.append(Tools.formatDecimal(1d, Constants.rateDecimalCount));
				result = true;
			}
		}
		catch (IllegalArgumentException e) {
			Tracker myTracker = EasyTracker.getInstance(context);
			myTracker.set(Fields.SCREEN_NAME, "CurrRatesEdit- Error7");
			myTracker.send(MapBuilder.createAppView().build());
		}
		return result;
	}
	
	public static String getRate(Context context, long firstCurrID, long secondCurrID, Date rateDate)
	{
		return getRate(context, firstCurrID, secondCurrID, rateDate, true);
	}
	
	private static String getRate(Context context, long firstCurrID, long secondCurrID, Date rateDate, boolean tryFromInternet)
	{
		try {
			String value;
			if (firstCurrID != secondCurrID)
			{
				Cursor cursor = context.getContentResolver().query(CurrRatesTableMetaData.CONTENT_URI, 
						new String[] {CurrRatesTableMetaData.VALUE, CurrRatesTableMetaData.FIRSTCURRID}, 
						"((" + CurrRatesTableMetaData.FIRSTCURRID + " = " + String.valueOf(firstCurrID) + " and " +
						CurrRatesTableMetaData.SECONDCURRID + " = " + String.valueOf(secondCurrID) + ") or (" +
						CurrRatesTableMetaData.SECONDCURRID + " = " + String.valueOf(firstCurrID) + " and " +
						CurrRatesTableMetaData.FIRSTCURRID + " = " + String.valueOf(secondCurrID) + ")) and " +
						CurrRatesTableMetaData.RATEDATE + "<='" + Tools.DateToDBString(rateDate) + "' and ifnull(" +
						CurrRatesTableMetaData.NEXTRATEDATE + ", '" + Tools.DateToDBString(Tools.getCurrentDate()) +
						"') >= '" + Tools.DateToDBString(rateDate) + "'", null, CurrRatesTableMetaData.RATEDATE + " desc ");
				if (cursor.moveToFirst())
				{
					if (DBTools.getCursorColumnValueLong(cursor, CurrRatesTableMetaData.FIRSTCURRID) == firstCurrID)
						value = DBTools.getCursorColumnValue(cursor, CurrRatesTableMetaData.VALUE);
					else
						value = Tools.formatDecimal(1/DBTools.getCursorColumnValueDouble(cursor, CurrRatesTableMetaData.VALUE), Constants.rateDecimalCount);
				}
				else {
					value = "0.00";
					if (tryFromInternet) {
						GetCurrencyRateTask getRateTask = new GetCurrencyRateTask(
								context, null,
								CurrencySrv.getCurrencySignByID(context, firstCurrID),
								CurrencySrv.getCurrencySignByID(context, secondCurrID));
						getRateTask.execute("");
					}
				}
				cursor.close();
			}
			else 
				value = "1.00";
			return value;
		}
		catch (IllegalArgumentException e) {
			Tracker myTracker = EasyTracker.getInstance(context);
			myTracker.set(Fields.SCREEN_NAME, "CurrRatesEdit- Error6");
			myTracker.send(MapBuilder.createAppView().build());
			return "0.00";
		}
		catch (Exception e) {
			return "0.00";
		}
	}
	
	public static String getRatesUrl(String fromSign, String toSign) {
		return "http://www.xe.com/ucc/convert.cgi?template=mobile&Amount=1&From=" + fromSign + "&To=" + toSign;
	}
	
	public static Date getNextRateDate(Context context, long firstCurrID, long secondCurrID, Date rateDate) {
		try {
			Cursor cursor = DBTools.createCursor(context, "Select min(" + CurrRatesTableMetaData.RATEDATE + ") " +
					CurrRatesTableMetaData.RATEDATE + " from " + 
					CurrRatesTableMetaData.TABLE_NAME + " where ((" + 
					CurrRatesTableMetaData.FIRSTCURRID + " = " + String.valueOf(firstCurrID) + " and " +
					CurrRatesTableMetaData.SECONDCURRID + " = " + String.valueOf(secondCurrID) + ") or (" +
					CurrRatesTableMetaData.SECONDCURRID + " = " + String.valueOf(firstCurrID) + " and " +
					CurrRatesTableMetaData.FIRSTCURRID + " = " + String.valueOf(secondCurrID) + ")) and " +
					CurrRatesTableMetaData.RATEDATE + ">" + Tools.DateToDBString(rateDate));
			if (cursor.moveToFirst())
				return DBTools.getCursorColumnValueDate(cursor, CurrRatesTableMetaData.RATEDATE);
			else
				return null;
		}
		catch (IllegalArgumentException e) {
			Tracker myTracker = EasyTracker.getInstance(context);
			myTracker.set(Fields.SCREEN_NAME, "CurrRatesEdit- Error10");
			myTracker.send(MapBuilder.createAppView().build());
			return null;
		}
	}

	public static double convertAmount(Context context, double amount, long fromCurrID, long toCurrID, Date rateDate){
		return convertAmount(context, amount, fromCurrID, toCurrID, rateDate, false);
	}

	public static double convertAmount(Context context, double amount, long fromCurrID, long toCurrID, Date rateDate, boolean tryFromInternet){
		try {
			double rate = Tools.stringToDouble(context, getRate(context, fromCurrID, toCurrID, rateDate, tryFromInternet), false);
			return Tools.round(amount * rate);
		}
		catch (IllegalArgumentException e) {
			Tracker myTracker = EasyTracker.getInstance(context);
			myTracker.set(Fields.SCREEN_NAME, "CurrRatesEdit- Error11");
			myTracker.send(MapBuilder.createAppView().build());
			return amount;
		}
	}	

	public static void insertRate(Context context, long firstCurrID, long secondCurrID, double rate, Date rateDate)
	{
		//@ TODO hemin dovrun tranzaksiyalarini update et
		try {
			if ((firstCurrID == secondCurrID) || (Double.compare(rate, 0d) == 0))
				return;
			Cursor cursor = context.getContentResolver().query(CurrRatesTableMetaData.CONTENT_URI,
                    new String[]{CurrRatesTableMetaData.VALUE, CurrRatesTableMetaData._ID, CurrRatesTableMetaData.RATEDATE, CurrRatesTableMetaData.FIRSTCURRID,
                                    CurrRatesTableMetaData.SECONDCURRID},
                    "((" + CurrRatesTableMetaData.FIRSTCURRID + " = " + String.valueOf(firstCurrID) + " and " +
                            CurrRatesTableMetaData.SECONDCURRID + " = " + String.valueOf(secondCurrID) + ") or (" +
                            CurrRatesTableMetaData.SECONDCURRID + " = " + String.valueOf(firstCurrID) + " and " +
                            CurrRatesTableMetaData.FIRSTCURRID + " = " + String.valueOf(secondCurrID) + ")) and " +
                            CurrRatesTableMetaData.RATEDATE + " <= " + Tools.DateToDBString(rateDate), null, CurrRatesTableMetaData.RATEDATE + " desc");
			if (cursor.moveToFirst())
			{
				Double cursorRate = DBTools.getCursorColumnValueDouble(cursor, CurrRatesTableMetaData.VALUE);
				Date cursorRateDate = DBTools.getCursorColumnValueDate(cursor, CurrRatesTableMetaData.RATEDATE, Constants.DateFormatDB);
				if ((rateDate.compareTo(cursorRateDate) != 0) && 
						(((Double.compare(cursorRate, rate) != 0) && (DBTools.getCursorColumnValueLong(cursor, CurrRatesTableMetaData.FIRSTCURRID) == firstCurrID)) ||
						((Tools.compareDouble(cursorRate, rate) != 0) && (DBTools.getCursorColumnValueLong(cursor, CurrRatesTableMetaData.FIRSTCURRID) != secondCurrID))))
					insertRateValues(context, firstCurrID, secondCurrID, rate, rateDate);
				/*else if ((rateDate.compareTo(cursorRateDate) == 0) &&
                        (Double.compare(cursorRate, rate) != 0) &&
                        (DBTools.getCursorColumnValueLong(cursor, CurrRatesTableMetaData.FIRSTCURRID) == firstCurrID) &&
                        (DBTools.getCursorColumnValueLong(cursor, CurrRatesTableMetaData.SECONDCURRID) == secondCurrID))
                    updateRateByID(context, firstCurrID, secondCurrID, rate, rateDate, DBTools.getCursorColumnValueLong(cursor, CurrRatesTableMetaData._ID));*/
			}
			else
				insertRateValues(context, firstCurrID, secondCurrID, rate, rateDate);
		}
		catch (IllegalArgumentException e) {
			Tracker myTracker = EasyTracker.getInstance(context);
			myTracker.set(Fields.SCREEN_NAME, "CurrRatesEdit- Error4");
			myTracker.send(MapBuilder.createAppView().build());
		}
	}
	
	private static void insertRateValues(Context context, long firstCurrID, long secondCurrID, double rate, Date rateDate)
	{
		try {
			ContentValues values = new ContentValues();
			values.put(CurrRatesTableMetaData.FIRSTCURRID, firstCurrID);
			values.put(CurrRatesTableMetaData.SECONDCURRID, secondCurrID);
			values.put(CurrRatesTableMetaData.VALUE, Tools.formatDecimal(rate, Constants.rateDecimalCount));
			values.put(CurrRatesTableMetaData.RATEDATE, Tools.DateToDBString(rateDate));
			Date nextRateDate = getNextRateDate(context, firstCurrID, secondCurrID, rateDate);
			if (nextRateDate == null)
				values.putNull(CurrRatesTableMetaData.NEXTRATEDATE);
			else
				values.put(CurrRatesTableMetaData.NEXTRATEDATE, Tools.DateToDBString(nextRateDate));
			context.getContentResolver().insert(CurrRatesTableMetaData.CONTENT_URI, values);
			//if there was any row for these currencies before this, update the last next rate date to the previoius day of this rate date
			Cursor cursor = context.getContentResolver().query(CurrRatesTableMetaData.CONTENT_URI, 
					new String[] {"MAX(" + CurrRatesTableMetaData.RATEDATE + ") as " + CurrRatesTableMetaData.RATEDATE}, 
					"((" + CurrRatesTableMetaData.FIRSTCURRID + " =? and " + CurrRatesTableMetaData.SECONDCURRID + " =?) or (" +
							CurrRatesTableMetaData.SECONDCURRID + " =? and " + CurrRatesTableMetaData.FIRSTCURRID + " =? )) and " +
							CurrRatesTableMetaData.RATEDATE + " <?", 
					new String[] {String.valueOf(firstCurrID), String.valueOf(secondCurrID),
							String.valueOf(firstCurrID), String.valueOf(secondCurrID), Tools.DateToDBString(rateDate)}, null);
			if (cursor.moveToFirst()) {
				values.clear();
				values.put(CurrRatesTableMetaData.NEXTRATEDATE, Tools.DateToDBString(Tools.AddDays(rateDate, -1)));
				context.getContentResolver().update(CurrRatesTableMetaData.CONTENT_URI, values, 
						CurrRatesTableMetaData.FIRSTCURRID + " =? and " + CurrRatesTableMetaData.SECONDCURRID + " =? and " +
								CurrRatesTableMetaData.RATEDATE + " =?", 
						new String[] {String.valueOf(firstCurrID), String.valueOf(secondCurrID),
								DBTools.getCursorColumnValue(cursor, CurrRatesTableMetaData.RATEDATE)});
			}
		}
		catch (IllegalArgumentException e) {
			Tracker myTracker = EasyTracker.getInstance(context);
			myTracker.set(Fields.SCREEN_NAME, "CurrRatesEdit- Error5");
			myTracker.send(MapBuilder.createAppView().build());
		}
	}

    public static void updateRateByID(Context context, long firstCurrID, long secondCurrID, double rate, Date rateDate, long id)
    {
        try {
            Double oldRate = 0d;
            Cursor cursor = context.getContentResolver().query(Uri.withAppendedPath(CurrRatesTableMetaData.CONTENT_URI, String.valueOf(id)), new String[] {CurrRatesTableMetaData.VALUE}, null, null, null);
            if (cursor.moveToFirst())
                oldRate = DBTools.getCursorColumnValueDouble(cursor, CurrRatesTableMetaData.VALUE);
            TransactionSrv.updateTransactionsRate(context, firstCurrID, secondCurrID, oldRate, rate, rateDate,
                    CurrRatesSrv.getNextRateDate(context, firstCurrID, secondCurrID, rateDate));
            ContentValues values = new ContentValues();
            values.put(CurrRatesTableMetaData.FIRSTCURRID, firstCurrID);
            values.put(CurrRatesTableMetaData.SECONDCURRID, secondCurrID);
            values.put(CurrRatesTableMetaData.VALUE, Tools.formatDecimal(rate, Constants.rateDecimalCount));
            values.put(CurrRatesTableMetaData.RATEDATE, Tools.DateToDBString(rateDate));
            context.getContentResolver().update(Uri.withAppendedPath(CurrRatesTableMetaData.CONTENT_URI, String.valueOf(id)),
                    values, null, null);
        }
        catch (IllegalArgumentException e) {
            Tracker myTracker = EasyTracker.getInstance(context);
            myTracker.set(Fields.SCREEN_NAME, "CurrRatesEdit- Error9");
            myTracker.send(MapBuilder.createAppView().build());
        }
    }
	
	public static void askForRate(final Context context, final Command command, final long fromCurrencyID, 
			final long toCurrencyID, final Date rateDate, StringBuilder oldRate) {
		final EditText inputText = new EditText(context);
		inputText.setRawInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
		if (oldRate != null)
			inputText.setText(oldRate.toString());
		//istifadeci girene qeder internetden dolduraq
		GetCurrencyRateTask getRateTask = new GetCurrencyRateTask(context, inputText, 
				CurrencySrv.getDefaultCurrencySign(context), 
				CurrencySrv.getCurrencySignByID(context, toCurrencyID));
		getRateTask.execute("");
		
		Command rateCommand = new Command() {								
			public void execute() {
				if ((inputText.getText().toString() != null) && (inputText.getText().toString().length() != 0)
						&& (Tools.isCorrectNumber(inputText.getText().toString()))
						&& (Tools.parseDouble(inputText.getText().toString()) != 0)) {
					CurrRatesSrv.insertRate(context, fromCurrencyID, toCurrencyID, 
							Tools.stringToDouble(context, inputText.getText().toString(), false), rateDate);
					command.execute();
				}
				else 
					DialogTools.toastDialog(context, R.string.msgEnterRate, Toast.LENGTH_LONG);
			}
		};
		AlertDialog inputDialog = DialogTools.InputDialog(context, rateCommand, 
				context.getResources().getString(R.string.msgAddRateFor) + " " + 
						CurrencySrv.getCurrencySignByID(context, fromCurrencyID) + " - " +
						CurrencySrv.getCurrencySignByID(context, toCurrencyID), inputText, 
						R.drawable.ic_input_add);
		inputDialog.show();		
		inputDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(inputText.getText().toString().trim().length() != 0);
	}

}
