package com.jgmoneymanager.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.google.analytics.tracking.android.Log;
import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.AccountTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransactionsTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransferTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VTransactionViewMetaData;
import com.jgmoneymanager.entity.Transaction;
import com.jgmoneymanager.tools.AfterTransactionOperationsTask;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import java.util.Date;

public class TransactionSrv {		

	public static void updateTransactionsCurrencyTodefault(final Context context, long currID) {
		final Cursor cursor = context.getContentResolver().query(VTransactionViewMetaData.CONTENT_URI, 
				null, VTransactionViewMetaData.CURRID + " = " + String.valueOf(currID), null, 
				VTransactionViewMetaData.ACCOUNTID + ", " + VTransactionViewMetaData.TRANSDATE + " desc ");
		if (cursor.getCount() > 0)
		{
			for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()){
				TransactionSrv.updateTransaction(context, 
						DBTools.getCursorColumnValue(cursor, VTransactionViewMetaData._ID), 
						DBTools.getCursorColumnValueLong(cursor, VTransactionViewMetaData.ACCOUNTID), 
						DBTools.getCursorColumnValueLong(cursor, VTransactionViewMetaData.ACCOUNTID), 
						DBTools.getCursorColumnValueLong(cursor, VTransactionViewMetaData.CATEGORYID), 
						DBTools.getCursorColumnValueLong(cursor, VTransactionViewMetaData.CATEGORYID), 
						DBTools.getCursorColumnValueDate(cursor, VTransactionViewMetaData.TRANSDATE), 
						DBTools.getCursorColumnValueDate(cursor, VTransactionViewMetaData.TRANSDATE), 
						DBTools.getCursorColumnValueDouble(cursor, VTransactionViewMetaData.AMOUNT), 
						DBTools.getCursorColumnValueDouble(cursor, VTransactionViewMetaData.AMOUNT), 
						DBTools.getCursorColumnValueInt(cursor, VTransactionViewMetaData.TRANSTYPE), 
						DBTools.getCursorColumnValue(cursor, VTransactionViewMetaData.DESCRIPTION), 
						DBTools.getCursorColumnValueLong(cursor, VTransactionViewMetaData.CURRID), 
						Constants.defaultCurrency, 0, 0,
						DBTools.getCursorColumnValueLong(cursor, VTransactionViewMetaData.TRANSFERID),
                        DBTools.getCursorColumnValue(cursor, VTransactionViewMetaData.PHOTO_PATH),
						DBTools.getCursorColumnValueLong(cursor, VTransactionViewMetaData.STATUS),
						DBTools.getCursorColumnValueLong(cursor, VTransactionViewMetaData.PAYMENT_METHOD));
			}
		}
	}
	
	/*public static boolean haveTransactionsWithCurrency(Context context, long currID) {
		final Cursor cursor = context.getContentResolver().query(VTransactionViewMetaData.CONTENT_URI, 
				null, VTransactionViewMetaData.CURRID + " = " + String.valueOf(currID), null, 
				VTransactionViewMetaData.ACCOUNTID + ", " + VTransactionViewMetaData.TRANSDATE + " desc ");
		return (cursor.getCount() > 0);
	}*/
	
	public static Date getFirstTransactionDate(Context context, long accountID) {
		Cursor cursor = DBTools.createCursor(context, "Select min(" + TransactionsTableMetaData.TRANSDATE + ") " + TransactionsTableMetaData.TRANSDATE + 
				" from " + TransactionsTableMetaData.TABLE_NAME + " where " + TransactionsTableMetaData.ACCOUNTID + " = " + String.valueOf(accountID));
		if (cursor.moveToFirst() && (DBTools.getCursorColumnValueDate(cursor, TransactionsTableMetaData.TRANSDATE) != null))
			return DBTools.getCursorColumnValueDate(cursor, TransactionsTableMetaData.TRANSDATE);
		else
			return AccountSrv.getAccountCreatedDate(context, accountID);
	}
	
	public static void updateTransactionsRate(Context context, long fromCurrID, long toCurrID, Double oldRate, Double newRate, Date fromDate, Date toDate) {
		if (oldRate.compareTo(newRate) != 0)
		{
			Double amount;
			ContentValues values = new ContentValues();
			String query = " " + VTransactionViewMetaData.CURRID + " = " + String.valueOf(fromCurrID) + 
					" and " + VTransactionViewMetaData.ACCOUNTCURRID + " = " + String.valueOf(toCurrID) + 
					" and " + VTransactionViewMetaData.TRANSDATE + " >= '" + Tools.DateToDBString(fromDate) + "'";
			if (toDate != null)
				query += " and " + VTransactionViewMetaData.TRANSDATE + " < '" + Tools.DateToDBString(toDate) + "'";
			Cursor cursor = context.getContentResolver().query(VTransactionViewMetaData.CONTENT_URI, null, query, null, 
					VTransactionViewMetaData.TRANSDATE + " desc, " + VTransactionViewMetaData._ID + " desc");
			for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
				amount = DBTools.getCursorColumnValueDouble(cursor, VTransactionViewMetaData.AMOUNT);
				values.clear();
				values.put(TransactionsTableMetaData.BALANCE, 
					Tools.formatDecimal(DBTools.getCursorColumnValueDouble(cursor, VTransactionViewMetaData.BALANCE) + 
						DBTools.getCursorColumnValueInt(cursor, VTransactionViewMetaData.TRANSTYPE) * (amount * newRate - amount * oldRate)));
				context.getContentResolver().update(Uri.withAppendedPath(TransactionsTableMetaData.CONTENT_URI, DBTools.getCursorColumnValue(cursor, VTransactionViewMetaData._ID)), 
					values, null, null);

				long transactionID = DBTools.getCursorColumnValueLong(cursor, TransactionsTableMetaData._ID);
				long categoryID = DBTools.getCursorColumnValueLong(cursor, TransactionsTableMetaData.CATEGORYID);
				long accountID = DBTools.getCursorColumnValueLong(cursor, TransactionsTableMetaData.ACCOUNTID);
				long transferID = DBTools.getCursorColumnValueLong(cursor, TransactionsTableMetaData.TRANSFERID);
				int transactionType = DBTools.getCursorColumnValueInt(cursor, TransactionsTableMetaData.TRANSTYPE);
				Date transactionDate = DBTools.getCursorColumnValueDate(cursor, VTransactionViewMetaData.TRANSDATE);
				Transaction oldTransactionEntity = new Transaction(transactionID, accountID, categoryID, transactionDate,
						amount * oldRate, transactionType, 0d, null, transferID, Constants.defaultCurrency, null, 0, 0);
				Transaction newTransactionEntity = new Transaction(transactionID, accountID, categoryID, transactionDate,
						amount * newRate, transactionType, 0d, null, transferID, Constants.defaultCurrency, null, 0, 0);
				AfterTransactionOperationsTask atTask = new AfterTransactionOperationsTask(context,
						oldTransactionEntity, newTransactionEntity, Constants.DBOperationType.Update.index());
				atTask.execute("");

				//Update followed transactions balance
				String whereClause = "(" + TransactionsTableMetaData.TRANSDATE + " > " + 
					Tools.DateToDBString(transactionDate) + " or (" +
					TransactionsTableMetaData.TRANSDATE + " = " + Tools.DateToDBString(transactionDate) + " and " +
					TransactionsTableMetaData._ID + " > " + DBTools.getCursorColumnValue(cursor, VTransactionViewMetaData._ID) + 
					")) and " + TransactionsTableMetaData.ACCOUNTID + " = " + DBTools.getCursorColumnValue(cursor, VTransactionViewMetaData.ACCOUNTID);
				Cursor cursor2 = context.getContentResolver().query(TransactionsTableMetaData.CONTENT_URI, 
					new String[] {TransactionsTableMetaData.BALANCE, TransactionsTableMetaData._ID}, whereClause, null, null);
				for (cursor2.moveToFirst(); !cursor2.isAfterLast(); cursor2.moveToNext()){
					values.clear();
					values.put(TransactionsTableMetaData.BALANCE, 
						Tools.formatDecimal(DBTools.getCursorColumnValueDouble(cursor2, TransactionsTableMetaData.BALANCE) + 
							DBTools.getCursorColumnValueInt(cursor, VTransactionViewMetaData.TRANSTYPE) * (amount * newRate - amount * oldRate)));
						context.getContentResolver().update(Uri.withAppendedPath(TransactionsTableMetaData.CONTENT_URI, DBTools.getCursorColumnValue(cursor2, TransactionsTableMetaData._ID)), values, null, null);
				}
			}
			DBTools.closeDatabase();
		}
	}

    private static boolean transferExists(Context context, long accountID, long transferID, Date transDate, Double amount) {
		if (transferID == 0) 
			return false;
		String query = VTransactionViewMetaData.TRANSFERID + " = " + String.valueOf(transferID) + " and " +
				TransactionsTableMetaData.AMOUNT + " = '" + Tools.formatDecimal(amount) + "' and " +
				TransactionsTableMetaData.TRANSDATE + " = '" + Tools.DateToDBString(transDate) + "' and " +
				TransactionsTableMetaData.ACCOUNTID + " = " + String.valueOf(accountID);
		Cursor cursor = context.getContentResolver().query(TransactionsTableMetaData.CONTENT_URI, null, query, null, null);
		boolean resultValue = cursor.moveToFirst();
		cursor.close();
		return resultValue;
	}

	/*public static long getTransferOtherAccountID(Context context, long transactonID) {
		Cursor cursor = context.getContentResolver().query(TransactionsTableMetaData.CONTENT_URI, 
				new String[] {TransactionsTableMetaData.TRANSFERID, TransactionsTableMetaData.TRANSDATE}, 
				TransactionsTableMetaData._ID + " = " + String.valueOf(transactonID), null, null);
		if (cursor.moveToFirst()) {
			Cursor cursor2 = context.getContentResolver().query(TransactionsTableMetaData.CONTENT_URI, 
					new String[] {TransactionsTableMetaData.ACCOUNTID},
					TransactionsTableMetaData.TRANSFERID + " = " + DBTools.getCursorColumnValue(cursor, TransactionsTableMetaData.TRANSFERID) + " and " +  
					TransactionsTableMetaData.TRANSDATE + " = '" + DBTools.getCursorColumnValue(cursor, TransactionsTableMetaData.TRANSDATE) + "' and " + 
					TransactionsTableMetaData._ID + " <> " + String.valueOf(transactonID), 
					null, null);
			if (cursor2.moveToFirst())
				return DBTools.getCursorColumnValueLong(cursor2, TransactionsTableMetaData.ACCOUNTID);
		}
		return 0;
	}*/

	/*public static boolean existsInTransfers(Context context, long transactionId) {
		Cursor cursor = context.getContentResolver().query(VTransactionViewMetaData.CONTENT_URI, 
				new String[] {VTransactionViewMetaData.TRANSFERID}, 
				VTransactionViewMetaData._ID + " = " + String.valueOf(transactionId), null, null);
		if (cursor.moveToFirst()) {
			long transferId = DBTools.getCursorColumnValueLong(cursor, VTransactionViewMetaData.TRANSFERID);
			if (transferId != 0) {
				cursor = context.getContentResolver().query(TransferTableMetaData.CONTENT_URI, 
						new String[] {TransferTableMetaData._ID}, 
						TransferTableMetaData._ID + " = " + String.valueOf(transferId) + " and " + 
						TransferTableMetaData.STATUS + " = " + Constants.Status.Enabled.index(), null, null);
				if (cursor.moveToFirst())
					return true;
			}
		}
		return false;
	}*/

	public static int getMinTransactionYear(Context context) {
		String query = "select min(" + TransactionsTableMetaData.TRANSDATE + ") " 
				+ TransactionsTableMetaData.TRANSDATE + " from " 
				+ TransactionsTableMetaData.TABLE_NAME;
		Cursor cursor = DBTools.createCursor(context, query);
		try {
			cursor.moveToFirst();
			String value = DBTools.getCursorColumnValue(cursor, TransactionsTableMetaData.TRANSDATE);
			if (value != null)
				return Integer.parseInt(value.substring(0, 4));
			else 
				return Tools.getCurrentDate().getYear() + 1900;
		} catch (Exception e) {
			return Tools.getCurrentDate().getYear() + 1900;
		}
	}
	
	public static long getTransferOtherID(Context context, long transactionID, StringBuilder sbAccountID) {
		String query = "Select " + TransactionsTableMetaData._ID + " || ';' || " + TransactionsTableMetaData.ACCOUNTID + " from " + 
				TransactionsTableMetaData.TABLE_NAME + " t1 where exists (select 1 from " + 
				TransactionsTableMetaData.TABLE_NAME + " t2 where t1." + 
				TransactionsTableMetaData.TRANSFERID + " = t2." + TransactionsTableMetaData.TRANSFERID + " and t1." +
				TransactionsTableMetaData.TRANSDATE + " = t2." + TransactionsTableMetaData.TRANSDATE + " and t2." +
				TransactionsTableMetaData._ID + " = " + String.valueOf(transactionID) + ") and " + 
				TransactionsTableMetaData._ID + " <> " + String.valueOf(transactionID) + " and t1." + 
				TransactionsTableMetaData.TRANSFERID + " <> 0 ";
		String strResult = DBTools.execQueryWithReturn(context, query);
		if (strResult == null)
			return 0;
		else {
			if (sbAccountID != null)
				sbAccountID.append(strResult.substring(strResult.indexOf(";")+1, strResult.length()));
			return Long.parseLong(strResult.substring(0, strResult.indexOf(";")));			
		}
	}
	
	/**
	 * Returns transaction type by transactionID
	 * @param context
	 * @param transactionID
	 * @return {@link Constants.TransFOperType}
	 */
	public static int getTransactionType(Context context, long transactionID, boolean controlStatus) {
		String statusCondition = " ";
		if (controlStatus)
			statusCondition = " and v2." + TransferTableMetaData.STATUS + " = " + Constants.Status.Enabled.index() + " ";
		String sql = "select " + VTransactionViewMetaData.TRANSFERID + ", " + VTransactionViewMetaData.ISTRANSFER
				+ " from " + VTransactionViewMetaData.VIEW_NAME + " v1 join "
				+ TransferTableMetaData.TABLE_NAME + " v2 on v1." + VTransactionViewMetaData.TRANSFERID 
				+ " = v2." + TransferTableMetaData._ID + statusCondition 
				+ " where v1." + VTransactionViewMetaData._ID + " = " + String.valueOf(transactionID); 
		Cursor cursor = DBTools.createCursor(context, sql);
		if (cursor.moveToFirst())
			if ((DBTools.getCursorColumnValueInt(cursor, VTransactionViewMetaData.ISTRANSFER) == 1) &&
					(DBTools.getCursorColumnValueInt(cursor, VTransactionViewMetaData.TRANSFERID) != 0))
				return Constants.TransFOperType.Transfer.index();
			else if (DBTools.getCursorColumnValueInt(cursor, VTransactionViewMetaData.TRANSFERID) != 0)
				return Constants.TransFOperType.RpTransaction.index();
			else 
				return Constants.TransFOperType.Transaction.index();
		else 
			return Constants.TransFOperType.Transaction.index();
	}

	/**
	 * Calculates the sum of all transactions
	 * @param context
	 * @param accounts Account IDs(25,54,45,512)
	 * @param categories Category IDs(25,54,45,512)
	 * @param startPeriod
	 * @param endPeriod
	 * @param transactionType {@link Constants.TransactionTypeIncome}
	 * @param requiredCurrencyID if not required send 0
	 * @return
	 */
	public static Double getTransactionSum(Context context, String accounts, String categories, Date startPeriod, 
			Date endPeriod, int transactionType, long requiredCurrencyID, boolean convertAsToday) {
		Double sum = 0d;
		String accountIDQuery;
		if (!accounts.equals("0")) 
			accountIDQuery = " " + VTransactionViewMetaData.ACCOUNTID + " in (" + accounts + ")";
		else 
			accountIDQuery = VTransactionViewMetaData.ISTRANSFER + " =0 ";//" 1=1 ";
		String categoryQuery = " ";
		if (!categories.equals("0")) {
			categoryQuery = " and " + VTransactionViewMetaData.CATEGORYID + " in (" + categories + ") ";
		}
		//birbawa olaraq income/expence olmasi gelir
		String transactionTypeQuery = " and " + VTransactionViewMetaData.TRANSTYPE + " = " + transactionType;
		//eger parametr olaraq sifirdan ferqli valyuta gelibse onu, eks halda 
		//eger account secilibse butun emeliyyatlar onun valyutasile, eks halda default ile cixmalidir
		long defaultCurrID;
		if (requiredCurrencyID != 0)
			defaultCurrID = requiredCurrencyID;
		else 
			defaultCurrID = (accounts.equals("0") || accounts.contains(",")) ? CurrencySrv.getDefaultCurrencyID(context) : AccountSrv.getCurrencyIdByAcocuntID(context, Integer.parseInt(accounts));
		
		String periodQuery = " and " + VTransactionViewMetaData.TRANSDATE 
				+ " between '" + Tools.DateToDBString(startPeriod) + "' and '" + Tools.DateToDBString(endPeriod) + "'";
		
		Cursor cursor = context.getContentResolver().query(VTransactionViewMetaData.CONTENT_URI, 
				new String[] {VTransactionViewMetaData.AMOUNT, VTransactionViewMetaData.TRANSDATE, VTransactionViewMetaData.CURRID},
				accountIDQuery + transactionTypeQuery + periodQuery + categoryQuery, null, null);
		
		Double test = 600000d;
		
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			long currencyID = DBTools.getCursorColumnValueLong(cursor, VTransactionViewMetaData.CURRID);
			//eger emeliyyatin valyutasi accountunki ile eyni deyilse, hemin gun ucun mezenne goturulerek cevrilir
			if (currencyID == defaultCurrID)
				sum += DBTools.getCursorColumnValueDouble(cursor, VTransactionViewMetaData.AMOUNT);
			else {
				Date rateDate = convertAsToday ? Tools.getCurrentDate() : DBTools.getCursorColumnValueDate(cursor, VTransactionViewMetaData.TRANSDATE);
				double rate = Tools.parseDouble(CurrRatesSrv.getRate(context, currencyID, defaultCurrID, rateDate));
				sum += DBTools.getCursorColumnValueFloat(cursor, VTransactionViewMetaData.AMOUNT) * rate;
			}
			if (sum.compareTo(test) > 0)
				Log.i("kecdi");
		}		
		return sum;		
	}

	public static Double getUnCategorizedTransactionsSum(Context context, Date startPeriod, Date endPeriod, 
			int transactionType, long defaultCurrID) {
		Double result = 0d;
		String condition = TransactionsTableMetaData.TRANSDATE 
				+ " between " + Tools.DateToDBString(startPeriod) + " and " + Tools.DateToDBString(endPeriod) 
				+ " and " + TransactionsTableMetaData.CATEGORYID + " = 0 and " 
			    + TransactionsTableMetaData.TRANSFERID + " = 0 and " 
				+ TransactionsTableMetaData.TRANSTYPE + " = " + String.valueOf(transactionType);
		Cursor cursor = context.getContentResolver().query(TransactionsTableMetaData.CONTENT_URI, 
				new String[] {TransactionsTableMetaData.TRANSDATE, TransactionsTableMetaData.AMOUNT,
					TransactionsTableMetaData.CURRENCYID}, condition, null, null);
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			long currencyID = DBTools.getCursorColumnValueLong(cursor, VTransactionViewMetaData.CURRID);
			//eger emeliyyatin valyutasi accountunki ile eyni deyilse, hemin gun ucun mezenne goturulerek cevrilir
			if (currencyID == defaultCurrID)
				result += DBTools.getCursorColumnValueDouble(cursor, TransactionsTableMetaData.AMOUNT);
			else {
				double rate = Tools.parseDouble(CurrRatesSrv.getRate(context, currencyID, defaultCurrID,
					DBTools.getCursorColumnValueDate(cursor, TransactionsTableMetaData.TRANSDATE)));				
				result += DBTools.getCursorColumnValueFloat(cursor, TransactionsTableMetaData.AMOUNT) * rate;
			}
		}		
		return result;
	}
	
	public static Double getTransactionsSum(Context context, long accountID, int period, int transactionType) {
		/*Double sum = 0d;
		String accountIDQuery;
		if (accountID != 0) 
			accountIDQuery = " " + VTransactionViewMetaData.ACCOUNTID + " = " + String.valueOf(accountID);
		else 
			accountIDQuery = VTransactionViewMetaData.ISTRANSFER + " =0 ";//" 1=1 ";
		//birbawa olaraq income/expence olmasi gelir
		String transactionTypeQuery = " and " + VTransactionViewMetaData.TRANSTYPE + " = " + transactionType;
		//eger account secilibse butun emeliyyatlar onun valyutasile, eks halda default ile cixmalidir
		long defaultCurrID = (accountID == 0) ? CurrencyEdit.getDefaultCurrencyID(context) : AccountSrv.getCurrencyIdByAcocuntID(context, accountID);
		
		//dovr ayliq, illik ve ya default olaraq gunluk gelir - Constants.DateFilterValues
		String periodQuery = " and ";
		if (period == Constants.DateFilterValues.ThisMonth.index()) 
			periodQuery += VTransactionViewMetaData.TRANSDATE 
				+ " between '" + Tools.DateToDBString(Tools.truncDate(Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncMonth)) 
				+ "' and '" + Tools.DateToDBString(Tools.getCurrentDate()) + "'";
		else if (period == Constants.DateFilterValues.ThisYear.index())
			periodQuery += VTransactionViewMetaData.TRANSDATE 
				+ " between '" + Tools.DateToDBString(Tools.truncDate(Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncYear)) 
				+ "' and '" + Tools.DateToDBString(Tools.getCurrentDate()) + "'";
		else 
			periodQuery += VTransactionViewMetaData.TRANSDATE + " = '" + Tools.DateToDBString(Tools.getCurrentDate()) + "' ";
		
		Cursor cursor = context.getContentResolver().query(VTransactionViewMetaData.CONTENT_URI, 
				new String[] {VTransactionViewMetaData.AMOUNT, VTransactionViewMetaData.TRANSDATE, VTransactionViewMetaData.CURRID},
				accountIDQuery + transactionTypeQuery + periodQuery, null, null);
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			long currencyID = DBTools.getCursorColumnValueLong(cursor, VTransactionViewMetaData.CURRID);
			//eger emeliyyatin valyutasi accountunki ile eyni deyilse, hemin gun ucun mezenne goturulerek cevrilir
			if (currencyID == defaultCurrID)
				sum += DBTools.getCursorColumnValueDouble(cursor, VTransactionViewMetaData.AMOUNT);
			else {
				double rate = Double.parseDouble(CurrRatesSrv.getRate(context, currencyID, defaultCurrID, 
					DBTools.getCursorColumnValueDate(cursor, VTransactionViewMetaData.TRANSDATE)));				
				sum += DBTools.getCursorColumnValueFloat(cursor, VTransactionViewMetaData.AMOUNT) * rate;
			}
		}*/		
		Date startPeriod;
		Date endPeriod;
		if (period == Constants.DateFilterValues.ThisMonth.index()) {
			startPeriod = Tools.truncDate(context, Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncMonth);
			endPeriod = Tools.getCurrentDate();
		}
		else if (period == Constants.DateFilterValues.ThisYear.index()) {
			startPeriod = Tools.truncDate(context, Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncYear);
			endPeriod = Tools.getCurrentDate();
		}
		else if (period == Constants.DateFilterValues.ThisWeek.index()) {
			startPeriod = Tools.truncDate(context, Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncWeek);
			endPeriod = Tools.getCurrentDate();
		}
		else {
			startPeriod = Tools.getCurrentDate();
			endPeriod = Tools.getCurrentDate();
		}
		
		return getTransactionSum(context, String.valueOf(accountID), "0", startPeriod, endPeriod, transactionType, 0, false);
	}
	
	public static void insertTransaction(Context context, long accountID, long categoryID, Date transactionDate, Double amount, 
			int transactionType, String description, long transferId, long currID, long accountCurrID, String photoPath,
			long status, long paymentMethod)
	{	
		if (!TransactionSrv.transferExists(context, accountID, transferId, transactionDate, amount)){
			ContentValues values = new ContentValues();
			values.put(TransactionsTableMetaData.ACCOUNTID, accountID);
			values.put(TransactionsTableMetaData.CATEGORYID, categoryID);
			values.put(TransactionsTableMetaData.TRANSDATE, Tools.DateToDBString(transactionDate));
			values.put(TransactionsTableMetaData.AMOUNT, Tools.formatDecimal(amount));
			values.put(TransactionsTableMetaData.TRANSTYPE, transactionType);				
			values.put(TransactionsTableMetaData.DESCRIPTION, description);
			values.put(TransactionsTableMetaData.TRANSFERID, transferId);
            values.put(TransactionsTableMetaData.CURRENCYID, currID);
			values.put(TransactionsTableMetaData.PHOTO_PATH, photoPath);
			values.put(TransactionsTableMetaData.STATUS, status);
			values.put(TransactionsTableMetaData.PAYMENT_METHOD, paymentMethod);
			if (accountCurrID == 0)
				accountCurrID = AccountSrv.getCurrencyIdByAcocuntID(context, accountID);
	
			double rate = Tools.parseDouble(CurrRatesSrv.getRate(context, currID, accountCurrID, transactionDate));
	
			/*if (currID != accountCurrID)
				CurrRatesEdit.insertRate(context, currID, accountCurrID, rate, transactionDate);*/
			Double balance = Tools.parseDouble(getBalance(context, String.valueOf(accountID), Tools.DateToDBString(transactionDate), null));
			String balanceValue;
			double amountForBalance = ((currID == accountCurrID) ? amount : amount * rate);
			if (transactionType == Constants.TransactionTypeIncome)
				balanceValue = Tools.formatDecimal(balance + amountForBalance);
			else 
				balanceValue = Tools.formatDecimal(balance - amountForBalance);
			values.put(TransactionsTableMetaData.BALANCE, balanceValue);
			context.getContentResolver().insert(TransactionsTableMetaData.CONTENT_URI, values);
			updateFollowedTransactions(context, accountID, Tools.StringToDate(values.getAsString(TransactionsTableMetaData.TRANSDATE), Constants.DateFormatDB), 
					transactionType, "0", amountForBalance);
			
			Transaction transactionEntity = new Transaction(0l, accountID, categoryID, transactionDate, amount, transactionType, 0d, description, transferId, currID, photoPath, status, paymentMethod);
			AfterTransactionOperationsTask atTask = new AfterTransactionOperationsTask(context, null, transactionEntity, Constants.DBOperationType.Insert.index());
			atTask.execute("");
		}
	}
	
	public static String getBalance(Context context, String accountID, String forDate, String transactionRowID)
	{
		String result = "0";
		String query = "Select max(" + TransactionsTableMetaData.TRANSDATE + ") from " + TransactionsTableMetaData.TABLE_NAME +
				" where " + TransactionsTableMetaData.ACCOUNTID + " = " + accountID + " and (" + TransactionsTableMetaData.TRANSDATE;
		if (transactionRowID == null)
			query = query + " <= '" + forDate + "')";
		else
			query = query + " < '" + forDate + "' or (" + TransactionsTableMetaData.TRANSDATE + " = " +
                    forDate + " and " + TransactionsTableMetaData._ID + " < " + transactionRowID + "))";
		String maxTransDate = DBTools.execQueryWithReturn(context, query);
		if (maxTransDate != null)
		{
			query = "Select max(" + TransactionsTableMetaData._ID + ") from " + TransactionsTableMetaData.TABLE_NAME +
					" where " + TransactionsTableMetaData.ACCOUNTID + " = " + accountID + " and " + 
					TransactionsTableMetaData.TRANSDATE + " = '" + maxTransDate + "'";
			if ((transactionRowID != null) && (maxTransDate.equals(forDate)))
				query = query + " and " + TransactionsTableMetaData._ID + " < " + transactionRowID;
			String maxID = DBTools.execQueryWithReturn(context, query);
			Cursor cursor = context.getContentResolver().query(Uri.withAppendedPath(TransactionsTableMetaData.CONTENT_URI, maxID), 
					new String[] {TransactionsTableMetaData.BALANCE}, null, null, null);
			cursor.moveToFirst();
			result = DBTools.getCursorColumnValue(cursor, TransactionsTableMetaData.BALANCE);
			cursor.close();
		}
		else
		{
			Cursor cursor = context.getContentResolver().query(Uri.withAppendedPath(AccountTableMetaData.CONTENT_URI, accountID), 
					new String[]{AccountTableMetaData.INITIALBALANCE}, null, null, null);
			if (cursor.moveToFirst())
				result = DBTools.getCursorColumnValue(cursor, AccountTableMetaData.INITIALBALANCE);
			cursor.close();
		}
		if (result.length() == 0)
			return "0";
		else
			return result;
	}
	
	
	public static void updateTransaction(Context context, String rowID, long oldAccountID, long newAccountID, 
			long oldCategoryID, long newCategoryID, 
			Date oldTransactionDate, Date newTransactionDate, Double oldAmount, Double newAmount, 
			int transactionType, String description, 
			long oldCurrID, long newCurrID, long oldAccountCurrID, long newAccountCurrID, long transferID, String photoPath,
			long status, long paymentMethod)
	{
		//getting rates
		if (oldAccountCurrID == 0)
			oldAccountCurrID = AccountSrv.getCurrencyIdByAcocuntID(context, oldAccountID);
		if (newAccountCurrID == 0)
			newAccountCurrID = AccountSrv.getCurrencyIdByAcocuntID(context, newAccountID);
		Double oldRate = Tools.parseDouble(CurrRatesSrv.getRate(context, oldCurrID, oldAccountCurrID, oldTransactionDate));
		Double newRate = Tools.parseDouble(CurrRatesSrv.getRate(context, newCurrID, newAccountCurrID, newTransactionDate));
		
		ContentValues values = new ContentValues();		
		if (newAccountID != 0) values.put(TransactionsTableMetaData.ACCOUNTID, newAccountID);
		if (newCategoryID != 0) values.put(TransactionsTableMetaData.CATEGORYID, newCategoryID);
		values.put(TransactionsTableMetaData.TRANSDATE, Tools.DateToDBString(newTransactionDate));
		values.put(TransactionsTableMetaData.AMOUNT, Tools.formatDecimal(newAmount));
		values.put(TransactionsTableMetaData.TRANSTYPE, transactionType);
		values.put(TransactionsTableMetaData.CURRENCYID, newCurrID);
		values.put(TransactionsTableMetaData.STATUS, status);
		values.put(TransactionsTableMetaData.PAYMENT_METHOD, paymentMethod);
		if (description != null)
            values.put(TransactionsTableMetaData.DESCRIPTION, description);
        else
            values.putNull(TransactionsTableMetaData.DESCRIPTION);
		if (photoPath != null)
            values.put(TransactionsTableMetaData.PHOTO_PATH, photoPath);
        else
            values.putNull(TransactionsTableMetaData.PHOTO_PATH);
		
		//new account current balance
		Double balance = Tools.parseDouble(getBalance(context, String.valueOf(newAccountID), Tools.DateToDBString(newTransactionDate), rowID));
		//new row balance value
		String balanceValue;
		double newAmountForBalance = newAmount * newRate;
		balanceValue = Tools.formatDecimal(balance + transactionType * newAmountForBalance);
		//old row balance value
		double oldAmountForBalance = oldAmount * oldRate;
		
		values.put(TransactionsTableMetaData.BALANCE, balanceValue);		
		context.getContentResolver().update(Uri.withAppendedPath(TransactionsTableMetaData.CONTENT_URI, rowID), values, null, null);

		Transaction oldTransactionEntity = new Transaction(0l, oldAccountCurrID, oldCategoryID, oldTransactionDate,
				oldAmount, transactionType, 0d, description, transferID, oldCurrID, photoPath, status, paymentMethod);
		Transaction newTransactionEntity = new Transaction(0l, newAccountCurrID, newCategoryID, newTransactionDate, 
				newAmount, transactionType, 0d, description, transferID, newCurrID, photoPath, status, paymentMethod);
		AfterTransactionOperationsTask atTask = new AfterTransactionOperationsTask(context,  
				oldTransactionEntity, newTransactionEntity, Constants.DBOperationType.Update.index());
		atTask.execute("");
		
		if ((newAccountID != oldAccountID) || (Double.compare(newAmountForBalance, oldAmountForBalance) != 0) || (oldTransactionDate.compareTo(newTransactionDate) != 0))
		{//TODO burani optimallasdirmaq olar, yalniz account deyisdikde buna ehtiyac var
			updateFollowedTransactions(context, oldAccountID, oldTransactionDate, -transactionType, rowID, oldAmountForBalance);
			updateFollowedTransactions(context, newAccountID, newTransactionDate, transactionType, rowID, newAmountForBalance);
		}
	}
		
	public static void deleteTransaction(Context context, String rowID)
	{
		Cursor cursor = context.getContentResolver().query(Uri.withAppendedPath(TransactionsTableMetaData.CONTENT_URI, rowID), 
				null, null, null, null);		
		if (cursor.moveToFirst()) {
			Double rate = 1d;
			long accountCurrencyID = AccountSrv.getCurrencyIdByAcocuntID(context, DBTools.getCursorColumnValueLong(cursor, TransactionsTableMetaData.ACCOUNTID));
			long transactionCurrencyID = DBTools.getCursorColumnValueLong(cursor, TransactionsTableMetaData.CURRENCYID);
			if (accountCurrencyID != transactionCurrencyID)
				rate = Tools.parseDouble(CurrRatesSrv.getRate(context, transactionCurrencyID, accountCurrencyID,
						DBTools.getCursorColumnValueDate(cursor, TransactionsTableMetaData.TRANSDATE, Constants.DateFormatDB)));
			updateFollowedTransactions(context, DBTools.getCursorColumnValueLong(cursor, TransactionsTableMetaData.ACCOUNTID), 
					DBTools.getCursorColumnValueDate(cursor, TransactionsTableMetaData.TRANSDATE, Constants.DateFormatDB), 
					-1*DBTools.getCursorColumnValueInt(cursor, TransactionsTableMetaData.TRANSTYPE), rowID, 
					DBTools.getCursorColumnValueDouble(cursor, TransactionsTableMetaData.AMOUNT) * rate);
		}
		context.getContentResolver().delete( Uri.withAppendedPath( TransactionsTableMetaData.CONTENT_URI, rowID), null, null);
		AfterTransactionOperationsTask atTask = new AfterTransactionOperationsTask(context, null, new Transaction(cursor), Constants.DBOperationType.Delete.index());
		atTask.execute("");
	}

	private static void updateFollowedTransactions(final Context context, long accountID, Date transactionDate, final int transactionType, String rowID, final Double amount)
	{		
		String whereClause = "(" + TransactionsTableMetaData.TRANSDATE + " > " + 
		Tools.DateToDBString(transactionDate);
		if (!rowID.equals("0"))
			whereClause += " or (" +
		TransactionsTableMetaData.TRANSDATE + " = " + Tools.DateToDBString(transactionDate) + " and " +
		TransactionsTableMetaData._ID + " > " + rowID + 
		")";
		whereClause += ") and " + TransactionsTableMetaData.ACCOUNTID + " = " + accountID;
		final Cursor cursor = context.getContentResolver().query(TransactionsTableMetaData.CONTENT_URI, 
				new String[] {TransactionsTableMetaData.BALANCE, TransactionsTableMetaData._ID}, whereClause, null, null);
		final ContentValues values = new ContentValues();
		
    	for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext())
    	{
    		values.clear();
    		values.put(TransactionsTableMetaData.BALANCE, 
    				Tools.formatDecimal(DBTools.getCursorColumnValueDouble(cursor, TransactionsTableMetaData.BALANCE) + (transactionType * amount)));
    		context.getContentResolver().update(Uri.withAppendedPath(TransactionsTableMetaData.CONTENT_URI, DBTools.getCursorColumnValue(cursor, TransactionsTableMetaData._ID)), values, null, null);
     	}
		cursor.close();
	}
	
	/**
	 * Return the amount of transaction added from transfer
	 * @param context
	 * @param trasferID
	 * @return
	 */
	public static Double getTransactionAmountFromTransfer(Context context, long trasferID) {
		Cursor cursor = context.getContentResolver().query(TransactionsTableMetaData.CONTENT_URI, 
				new String[] {TransactionsTableMetaData.AMOUNT}, 
				TransactionsTableMetaData.TRANSFERID + " = " + String.valueOf(trasferID), null, null);
		if (cursor.moveToFirst())
			return DBTools.getCursorColumnValueDouble(cursor, TransactionsTableMetaData.AMOUNT);
		else 
			return 0d;
	}
}
