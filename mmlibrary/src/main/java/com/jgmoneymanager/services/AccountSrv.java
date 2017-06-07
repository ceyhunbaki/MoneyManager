package com.jgmoneymanager.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.AccountTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransactionsTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VAccountsViewMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VTransAccountViewMetaData;
import com.jgmoneymanager.entity.CheckBoxItem;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import java.util.ArrayList;
import java.util.Date;

public class AccountSrv {
	
	/*public static boolean haveAccountWithCurrID(Context context, long currID) {
		Cursor cursor = context.getContentResolver().query(AccountTableMetaData.CONTENT_URI, 
				new String[] {AccountTableMetaData._ID}, 
				AccountTableMetaData.CURRID + " = " + String.valueOf(currID), null, null);
		return (cursor.moveToFirst());
	}*/
	
	public static long getDefultAccountID(Context context, StringBuilder accountName) {
		Cursor cursor = context.getContentResolver().query(AccountTableMetaData.CONTENT_URI, 
				new String[] {AccountTableMetaData._ID, AccountTableMetaData.NAME}, 
				AccountTableMetaData.ISDEFAULT + " = 1 and " + AccountTableMetaData.STATUS + " =1 ", null, null);
		if (cursor.moveToFirst())
		{
			if (accountName != null)
				accountName.append(DBTools.getCursorColumnValue(cursor, AccountTableMetaData.NAME));
			return DBTools.getCursorColumnValueLong(cursor, AccountTableMetaData._ID);
		}
		else 
			return 0;
	}
	
	public static boolean isDefultAccountID(Context context, long id) {
		Cursor cursor = context.getContentResolver().query(AccountTableMetaData.CONTENT_URI, 
				new String[] {AccountTableMetaData._ID}, 
				AccountTableMetaData.ISDEFAULT + " = 1 and " + 
				AccountTableMetaData._ID + " = " + String.valueOf(id), null, null);
		boolean resultValue = cursor.moveToFirst();
		cursor.close();
		return resultValue;
	}
	
	public static String getAccountNameByID(Context context, long accountID)
	{
		if (accountID == 0) 
			return "ALL";
		else {
			Cursor cursor = context.getContentResolver().query(Uri.withAppendedPath(AccountTableMetaData.CONTENT_URI, String.valueOf(accountID)), 
					new String[] {AccountTableMetaData.NAME}, null, null, null);
			if (cursor.moveToFirst())
				return DBTools.getCursorColumnValue(cursor, AccountTableMetaData.NAME);
			else 
				return null;
		}
	}
	
	public static long getCurrencyIdByAcocuntID(Context context, long accountID)
	{
		Cursor cursor = context.getContentResolver().query(Uri.withAppendedPath(AccountTableMetaData.CONTENT_URI, String.valueOf(accountID)), 
				new String[] {AccountTableMetaData.CURRID}, null, null, null);
		long value;
		if (cursor.moveToFirst())			
			value = DBTools.getCursorColumnValueLong(cursor, AccountTableMetaData.CURRID);
		else 
			value = 0;
		cursor.close();
		return value;
	}

	public static long getAccountIDByName(Context context, String name) {
		long value;
		Cursor cursor = context.getContentResolver().query(AccountTableMetaData.CONTENT_URI, 
				new String[] {AccountTableMetaData._ID}, AccountTableMetaData.NAME + " = '" + name + "'", null, null);
		if (cursor.moveToFirst())
			value = DBTools.getCursorColumnValueLong(cursor, AccountTableMetaData._ID);
		else 
			value = 0;
		cursor.close();
		return value;
	}

	public static void updateAccountCurrency(Context context, long accountID, long oldCurrID, long newCurrID) {
		Cursor cursor = context.getContentResolver().query(TransactionsTableMetaData.CONTENT_URI, null,
				TransactionsTableMetaData.ACCOUNTID + " = " + String.valueOf(accountID), null, TransactionsTableMetaData.TRANSDATE + ", " + TransactionsTableMetaData._ID);
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()){
			TransactionSrv.updateTransaction(context, 
					DBTools.getCursorColumnValue(cursor, TransactionsTableMetaData._ID), 
					DBTools.getCursorColumnValueLong(cursor, TransactionsTableMetaData.ACCOUNTID), 
					DBTools.getCursorColumnValueLong(cursor, TransactionsTableMetaData.ACCOUNTID), 
					DBTools.getCursorColumnValueLong(cursor, TransactionsTableMetaData.CATEGORYID), 
					DBTools.getCursorColumnValueLong(cursor, TransactionsTableMetaData.CATEGORYID), 
					DBTools.getCursorColumnValueDate(cursor, TransactionsTableMetaData.TRANSDATE), 
					DBTools.getCursorColumnValueDate(cursor, TransactionsTableMetaData.TRANSDATE), 
					DBTools.getCursorColumnValueDouble(cursor, TransactionsTableMetaData.AMOUNT), 
					DBTools.getCursorColumnValueDouble(cursor, TransactionsTableMetaData.AMOUNT), 
					DBTools.getCursorColumnValueInt(cursor, TransactionsTableMetaData.TRANSTYPE), 
					DBTools.getCursorColumnValue(cursor, TransactionsTableMetaData.DESCRIPTION), 
					DBTools.getCursorColumnValueLong(cursor, TransactionsTableMetaData.CURRENCYID), 
					DBTools.getCursorColumnValueLong(cursor, TransactionsTableMetaData.CURRENCYID), 
					oldCurrID, newCurrID,
					DBTools.getCursorColumnValueLong(cursor, TransactionsTableMetaData.TRANSFERID),
					DBTools.getCursorColumnValue(cursor, TransactionsTableMetaData.PHOTO_PATH),
					DBTools.getCursorColumnValueLong(cursor, TransactionsTableMetaData.STATUS),
					DBTools.getCursorColumnValueLong(cursor, TransactionsTableMetaData.PAYMENT_METHOD));
		}
		
	}
	
	public static Date getAccountCreatedDate(Context context, long accountID) {
		Cursor cursor = context.getContentResolver().query(Uri.withAppendedPath(AccountTableMetaData.CONTENT_URI, String.valueOf(accountID)), 
				new String[] {AccountTableMetaData.CREATED_DATE}, null, null, null);
		if (cursor.moveToFirst())
			return DBTools.getCursorColumnValueDate(cursor, AccountTableMetaData.CREATED_DATE, Constants.DateFormatDBLong);
		else
			return Tools.getCurrentDate();
	}

	public static void updateAccountCurrencyToDefault(Context context, long currID) {
		Cursor cursor = context.getContentResolver().query(AccountTableMetaData.CONTENT_URI, null, 
				AccountTableMetaData.CURRID + " = " + String.valueOf(currID), null, null);
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()){
			ContentValues values = new ContentValues();
			values.put(AccountTableMetaData.CURRID, Constants.defaultCurrency);
			values.put(AccountTableMetaData.INITIALBALANCE, 
					CurrRatesSrv.convertAmount(context, 
							DBTools.getCursorColumnValueDouble(cursor, AccountTableMetaData.INITIALBALANCE), 
							currID, Constants.defaultCurrency, 
							AccountSrv.getAccountCreatedDate(context, DBTools.getCursorColumnValueLong(cursor, AccountTableMetaData._ID))));
			updateAccount(context, values, 
					DBTools.getCursorColumnValue(cursor, AccountTableMetaData.INITIALBALANCE), 
					DBTools.getCursorColumnValue(cursor, AccountTableMetaData.INITIALBALANCE), 
					Constants.defaultCurrency, currID, DBTools.getCursorColumnValue(cursor, AccountTableMetaData._ID));
		}
	}
	
	public static void updateAccount(final Context context, ContentValues values, String oldInitialBalance, String newInitialBalance, 
			final long newCurrID, final long oldCurrID, final String id)
	{
		if (!newInitialBalance.equals(oldInitialBalance))
		{
			String balanceDifference = Tools.formatDecimal(Tools.parseDouble(newInitialBalance) -
					Tools.parseDouble(oldInitialBalance));
			String sql = "update " + TransactionsTableMetaData.TABLE_NAME + 
					" set " + TransactionsTableMetaData.BALANCE + 
						" = " + TransactionsTableMetaData.BALANCE + " + " + balanceDifference + 
					" where " + TransactionsTableMetaData.ACCOUNTID + " = " + id;
			DBTools.execQuery(context, sql);
		}
		context.getContentResolver().update(Uri.withAppendedPath(AccountTableMetaData.CONTENT_URI, id), values, null, null);
		if (newCurrID != oldCurrID)
		{
			AccountSrv.updateAccountCurrency(context, Long.valueOf(id), oldCurrID, newCurrID);
		}
	}
	
	public static int getAccountCount(Context context) {
		Cursor cursor = context.getContentResolver().query(AccountTableMetaData.CONTENT_URI, 
				new String[] {AccountTableMetaData._ID}, null, null, null);
		int resultValue = cursor.getCount();
		cursor.close();
		return resultValue;
	}
	
	public static float getAccountBalance(Context context, long accountID, StringBuffer currencySign) {
		if (accountID != 0) {
			Cursor cursor = context.getContentResolver().query(Uri.withAppendedPath(VAccountsViewMetaData.CONTENT_URI, String.valueOf(accountID)), 
					new String[] {VAccountsViewMetaData.BALANCE, VAccountsViewMetaData.CURRSIGN}, null, null, null);
			if (cursor.moveToFirst()) {
				currencySign.append(DBTools.getCursorColumnValue(cursor, VAccountsViewMetaData.CURRSIGN));
				return DBTools.getCursorColumnValueFloat(cursor, VAccountsViewMetaData.BALANCE);
			}
			else {
				currencySign.append(CurrencySrv.getDefaultCurrencySign(context));
				return 0f;
			}
		}
		else {
			float sumAmount = 0;
			Cursor cursor = context.getContentResolver().query(VAccountsViewMetaData.CONTENT_URI, 
					new String[] {VAccountsViewMetaData.BALANCE, VAccountsViewMetaData.CURRID}, 
					VAccountsViewMetaData.STATUS + " = 1 ", null, null);		
			for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
				int currID = DBTools.getCursorColumnValueInt(cursor, VAccountsViewMetaData.CURRID);
				if (currID == Constants.defaultCurrency)
					sumAmount += DBTools.getCursorColumnValueFloat(cursor, VAccountsViewMetaData.BALANCE);
				else 
					sumAmount += DBTools.getCursorColumnValueFloat(cursor, VAccountsViewMetaData.BALANCE) * 
						Float.parseFloat(CurrRatesSrv.getRate(context, currID, Constants.defaultCurrency, Tools.getCurrentDate()));
			}
			currencySign.append(CurrencySrv.getDefaultCurrencySign(context));
			return sumAmount;
		}
	}
	
	public static ArrayList<CheckBoxItem> generateAccountsList(Context context, String stringAll) {
		ArrayList<CheckBoxItem> list = new ArrayList<>();
		/*Cursor cursor = context.getContentResolver().query(VTransAccountViewMetaData.CONTENT_URI,
				new String[] {VTransAccountViewMetaData.NAME, VTransAccountViewMetaData._ID}, 
				VTransAccountViewMetaData.STATUS + " = ? ", new String[] {"1"},
				VTransAccountViewMetaData.ISDEFAULT + " desc, " + VTransAccountViewMetaData.SORTORDER);*/
		String query = "Select " + VTransAccountViewMetaData.NAME + ", " + VTransAccountViewMetaData._ID
				+ " from " + VTransAccountViewMetaData.VIEW_NAME + " where " + VTransAccountViewMetaData.STATUS + " = 1 order by "
				+ VTransAccountViewMetaData.ISDEFAULT + " desc, " + VTransAccountViewMetaData.SORTORDER;
 		Cursor cursor = DBTools.createCursor(context, query);
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			String accountName = DBTools.getCursorColumnValue(cursor, VTransAccountViewMetaData.NAME);
			if (accountName != null) {
				if (accountName.equals("ALL"))
					accountName = stringAll;
				list.add(new CheckBoxItem(DBTools.getCursorColumnValueInt(cursor, VTransAccountViewMetaData._ID),
						accountName));
			}
		}
		return list;
	}

	/**
	 * Checks the visibility state of account
	 * @param context
	 * @param accountID
	 * @return 
	 * true if Account is visible, false if unvisible
	 */
	public static boolean isAccountVisible(Context context, long accountID) {
		Cursor cursor = context.getContentResolver().query(AccountTableMetaData.CONTENT_URI, 
				new String[] {AccountTableMetaData.STATUS}, 
				AccountTableMetaData._ID + " =? ", 
				new String[] {String.valueOf(accountID)}, null);
		if (cursor.moveToFirst()) 
			return (DBTools.getCursorColumnValueInt(cursor, AccountTableMetaData.STATUS) == 1);
		else 
			return false;
	}
	
	public static Double getBalanceForDate(Context context, long accountID, Date filterDate, long currencyID, Date conversionDate) {
		String accountIDCondition = null ;
		Double result = 0d;
		if (accountID != 0) 
			accountIDCondition = " and " + AccountTableMetaData._ID + " = " + String.valueOf(accountID);
		Cursor cursor = context.getContentResolver().query(AccountTableMetaData.CONTENT_URI, 
				new String[] {AccountTableMetaData._ID, AccountTableMetaData.CURRID}, 
				/*AccountTableMetaData.STATUS + " = " + Constants.Status.Enabled.index() +*/ accountIDCondition, 
				null, null);
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			Double currentValue = Double.valueOf(TransactionSrv.getBalance(context, 
					DBTools.getCursorColumnValue(cursor, AccountTableMetaData._ID), 
					Tools.DateToDBString(filterDate), null));
			long accountCurrency = DBTools.getCursorColumnValueLong(cursor, AccountTableMetaData.CURRID);
			if (accountCurrency == currencyID) 
				result += currentValue;
			else
				result += CurrRatesSrv.convertAmount(context, currentValue, accountCurrency, currencyID, conversionDate);
		}
		return result;
	}

	public static Double getBalanceForDate(Context context, long accountID, Date filterDate, long currencyID) {
		return getBalanceForDate(context, accountID, filterDate, currencyID, filterDate);
	}
	
	public static long insertAccount(Context context, String name, String initialBalance, String isDefault){
		ContentValues values = new ContentValues();
		values.put(AccountTableMetaData.NAME, name);
		values.put(AccountTableMetaData.INITIALBALANCE, initialBalance);
		values.put(AccountTableMetaData.CURRID, CurrencySrv.getDefaultCurrencyID(context));
		values.put(AccountTableMetaData.STATUS, "1");
		values.put(AccountTableMetaData.ISDEFAULT, isDefault);
		Uri insertedUri = context.getContentResolver().insert(AccountTableMetaData.CONTENT_URI, values);
		Cursor cursor = context.getContentResolver().query(insertedUri, null, null, null, null);
		cursor.moveToFirst();
		long value = DBTools.getCursorColumnValueLong(cursor, AccountTableMetaData._ID);
		cursor.close();
		return value;
	}

	public static long getFirstNonThisAccountID(Context context, long currentID) {
		Cursor cursor = context.getContentResolver().query(AccountTableMetaData.CONTENT_URI, new String[] {AccountTableMetaData._ID},
				AccountTableMetaData._ID + " <> " + String.valueOf(currentID), null, AccountTableMetaData.SORTORDER);
		if (cursor.moveToFirst()) {
			return DBTools.getCursorColumnValueLong(cursor, AccountTableMetaData._ID);
		}
		else
			return 0;
	}
}
