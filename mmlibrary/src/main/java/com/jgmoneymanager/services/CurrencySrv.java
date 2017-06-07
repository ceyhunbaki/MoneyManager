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

import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CurrencyTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.mmlibrary.R;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import java.util.Comparator;
import java.util.Currency;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

public class CurrencySrv {
	
	public static void changeDefaultCurrency(final Context context, final long newCurrID, final Command refreshCurrencyList) {
		final long oldCurrID = getDefaultCurrencyID(context);
		if ((oldCurrID != 0) && (!CurrRatesSrv.rateExists(context, oldCurrID, newCurrID, Tools.getCurrentDate(), null, null))) {
			final EditText inputText = new EditText(context);
			inputText.setRawInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
			inputText.setText(CurrRatesSrv.getRate(context, oldCurrID, newCurrID, Tools.getCurrentDate()));
			Command yesCommand = new Command() {			
				@Override
				public void execute() {
					if (Tools.isGreaterZero(context, inputText.getText().toString())) {
						setDefaultCurrency(context, newCurrID);
						refreshDefaultCurrency(context);
						CurrRatesSrv.insertRate(context, oldCurrID, newCurrID, Tools.stringToDouble(context, inputText.getText().toString(), false), Tools.getCurrentDate());
						if (refreshCurrencyList != null)
							refreshCurrencyList.execute();
					}
					else {
						DialogTools.toastDialog(context, R.string.msgValueTooSmall, Toast.LENGTH_SHORT);
					}
				}
			};
			Command noCommand = new Command() {			
				@Override
				public void execute() {
					AlertDialog warning = DialogTools.warningDialog(context, R.string.msgWarning, context.getResources().getString(R.string.msgAddRateForDefaultCurr));
					warning.show();
				}
			};
			String title = context.getResources().getString(R.string.msgAddRateFor) + " " + 
					getCurrencySignByID(context, oldCurrID) + " - " +
					getCurrencySignByID(context, newCurrID);
			AlertDialog dialog = DialogTools.InputDialog(context, yesCommand, noCommand, title, inputText, R.drawable.ic_input_add);
			dialog.show();
			dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(inputText.getText().toString().trim().length() != 0);
		}
		else {
			setDefaultCurrency(context, newCurrID);
			refreshDefaultCurrency(context);
			try {
				refreshCurrencyList.execute();
			}
			catch (Exception e) {
			}
		}
	}

	public static void setDefaultCurrency(Context context, long id) {
		ContentValues values = new ContentValues();
		values.put(CurrencyTableMetaData.ISDEFAULT, 1);
		context.getContentResolver().update(Uri.withAppendedPath(CurrencyTableMetaData.CONTENT_URI, String.valueOf(id)), 
				values, null, null);
		values.clear();
		values.put(CurrencyTableMetaData.ISDEFAULT, 0);
		context.getContentResolver().update(CurrencyTableMetaData.CONTENT_URI, 
				values, CurrencyTableMetaData._ID + " <> " + String.valueOf(id), null);		
	}
	
	public static long insertCurrency(Context context, String name, String sign, int isDefault, long resourceID)
	{
		ContentValues values = new ContentValues();
		values.put(CurrencyTableMetaData.NAME, name);
		values.put(CurrencyTableMetaData.SIGN, sign);
		values.put(CurrencyTableMetaData.ISDEFAULT, isDefault);
		if (resourceID == 0)
			values.putNull(CurrencyTableMetaData.RESOURCEID);
		else
			values.put(CurrencyTableMetaData.RESOURCEID, resourceID);
		Uri insertedUri = context.getContentResolver().insert(CurrencyTableMetaData.CONTENT_URI, values);
		if (isDefault == 1) 
			refreshDefaultCurrency(context);
		Cursor cursor = context.getContentResolver().query(insertedUri, null, null, null, null);
		if (cursor.moveToFirst())
			return DBTools.getCursorColumnValueLong(cursor, CurrencyTableMetaData._ID);
		else
			return 0;
	}

	public static void updateCurrency(Context context, long id, String name, String sign, boolean emptyResID) {
		ContentValues cv = new ContentValues();
		cv.put(CurrencyTableMetaData.NAME, name);
		cv.put(CurrencyTableMetaData.SIGN, sign);
		if (emptyResID)
			cv.putNull(CurrencyTableMetaData.RESOURCEID);
		context.getContentResolver().update(Uri.withAppendedPath(CurrencyTableMetaData.CONTENT_URI, String.valueOf(id)), cv, null, null);
	}
	
	public static void refreshDefaultCurrency(Context context) {
		//currency list-de var
		Constants.defaultCurrency = getDefaultCurrencyID(context);
	}

	public static int getDefaultCurrencyID(Context context) {
		int currID = 0;
		Cursor cursor = context.getContentResolver().query(CurrencyTableMetaData.CONTENT_URI, 
				new String[] {CurrencyTableMetaData._ID}, 
				CurrencyTableMetaData.ISDEFAULT + " = 1 ", null, null);
		if (cursor.moveToFirst())
			currID = DBTools.getCursorColumnValueInt(cursor, CurrencyTableMetaData._ID);
		else
		{
			DialogTools.toastDialog(context, context.getResources().getString(R.string.msgSetDefaultCurrency), Toast.LENGTH_LONG);
		}
		return currID;
	}

	public static String getDefaultCurrencySign(Context context) {
		String currSign = "";
		Cursor cursor = context.getContentResolver().query(CurrencyTableMetaData.CONTENT_URI, 
				new String[] {CurrencyTableMetaData.SIGN}, 
				CurrencyTableMetaData.ISDEFAULT + " = 1 ", null, null);
		if (cursor.moveToFirst())
			currSign = DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData.SIGN);
		else
		{
			DialogTools.toastDialog(context, context.getResources().getString(R.string.msgSetDefaultCurrency), Toast.LENGTH_LONG);
		}
		return currSign;
	}

	public static String getCurrencyNameSignByID(Context context, long id) {
		Cursor cursor = context.getContentResolver().query(Uri.withAppendedPath(CurrencyTableMetaData.CONTENT_URI, String.valueOf(id)), 
				new String[] {CurrencyTableMetaData.NAME, CurrencyTableMetaData.SIGN}, null, null, null);
		if (cursor.moveToFirst())
			return DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData.NAME) + 
					": " + DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData.SIGN);
		else 
			return context.getResources().getString(R.string.pNotSet);
	}
	
	public static String getCurrencySignByID(Context context, long ID) {
		Cursor cursor = context.getContentResolver().query(Uri.withAppendedPath(CurrencyTableMetaData.CONTENT_URI, String.valueOf(ID)), 
				new String[] {CurrencyTableMetaData.SIGN}, null, null, null);
		if (cursor.moveToFirst()) 
			return DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData.SIGN);
		else 
			return "";
	}

	public static long getCurrencyIDBySign(Context context, String sign) {
		Cursor cursor = context.getContentResolver().query(CurrencyTableMetaData.CONTENT_URI, 
				new String[] {CurrencyTableMetaData._ID}, 
				CurrencyTableMetaData.SIGN + " = '" + sign + "'", null, null);
		if (cursor.moveToFirst()) 
			return DBTools.getCursorColumnValueLong(cursor, CurrencyTableMetaData._ID);
		else 
			return 0;
	}

	public static String getCurrencySymbol(String currencySign) {
		try {
			return Utils.getCurrencySymbol(currencySign);
		}
		catch (Exception e) {
			return currencySign;
		}
	}

	static class Utils {
		public static SortedMap<Currency, Locale> currencyLocaleMap;

		static {
			currencyLocaleMap = new TreeMap<Currency, Locale>(new Comparator<Currency>() {
				public int compare(Currency c1, Currency c2) {
					return c1.getCurrencyCode().compareTo(c2.getCurrencyCode());
				}
			});
			for (Locale locale : Locale.getAvailableLocales()) {
				try {
					Currency currency = Currency.getInstance(locale);
					currencyLocaleMap.put(currency, locale);
				} catch (Exception e) {
				}
			}
		}


		public static String getCurrencySymbol(String currencyCode) {
			Currency currency = Currency.getInstance(currencyCode);
			System.out.println(currencyCode + ":-" + currency.getSymbol(currencyLocaleMap.get(currency)));
			return currency.getSymbol(currencyLocaleMap.get(currency));
		}

	}
}
