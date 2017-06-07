package com.jgmoneymanager.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransferTableMetaData;
import com.jgmoneymanager.mmlibrary.R;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import java.util.Date;

public class RPTransactionSrv {

	
	public static void controlRPTransactions(Context context)
	{
		long accountID;
		int transactionType;
		String updateIDs = "";
		Cursor cursor = context.getContentResolver().query(TransferTableMetaData.CONTENT_URI, null, 
				TransferTableMetaData.NEXTPAYMENT + " <= '" + Tools.DateToDBString(Tools.getCurrentDate()) + "' and " +
				TransferTableMetaData.NEXTPAYMENT + " is not null and (" + 
				TransferTableMetaData.FIRSTACCOUNTID + " is null or " + 
				TransferTableMetaData.SECONDACCOUNTID + " is null) and " +
				TransferTableMetaData.STATUS + " = " + String.valueOf(Constants.Status.Enabled.index()), 
						null, TransferTableMetaData.TRANSDATE);
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()){
			if (DBTools.getCursorColumnValue(cursor, TransferTableMetaData.FIRSTACCOUNTID) != null)
			{
				accountID = DBTools.getCursorColumnValueLong(cursor, TransferTableMetaData.FIRSTACCOUNTID);
				transactionType = Constants.TransactionTypeExpence;
			}
			else 
			{				
				accountID = DBTools.getCursorColumnValueLong(cursor, TransferTableMetaData.SECONDACCOUNTID);
				transactionType = Constants.TransactionTypeIncome;
			}
			insertTransactionsFromRPTransaction(context, accountID, 
					transactionType, 
					DBTools.getCursorColumnValueDate(cursor, TransferTableMetaData.NEXTPAYMENT, Constants.DateFormatDB), 
					DBTools.getCursorColumnValueDouble(cursor, TransferTableMetaData.AMOUNT), 
					DBTools.getCursorColumnValue(cursor, TransferTableMetaData.DESCRIPTION), 
					DBTools.getCursorColumnValueInt(cursor, TransferTableMetaData.REPEATTYPE), 
					DBTools.getCursorColumnValueInt(cursor, TransferTableMetaData.CUSTOMINTERVAL), 
					DBTools.getCursorColumnValueDate(cursor, TransferTableMetaData.PERIODEND, Constants.DateFormatDB), 
					DBTools.getCursorColumnValueLong(cursor, TransferTableMetaData._ID),
					DBTools.getCursorColumnValueLong(cursor, TransferTableMetaData.CURRENCYID),
					DBTools.getCursorColumnValueLong(cursor, TransferTableMetaData.CATEGORYID),
					DBTools.getCursorColumnValueInt(cursor, TransferTableMetaData.TRANSACTION_STATUS),
					DBTools.getCursorColumnValueInt(cursor, TransferTableMetaData.TRANSACTION_PAYMENT_METHOD));
			updateIDs += ", " + DBTools.getCursorColumnValue(cursor, TransferTableMetaData._ID);
		}
		if (updateIDs.length() > 0)
			updateReminders(context, updateIDs.substring(2), true);
	}

    public static void updateReminders(Context context, String updateIDs, boolean toOriginal) {
		String query = "Update " + TransferTableMetaData.TABLE_NAME + " set " + 
				TransferTableMetaData.REMINDER + " = " + TransferTableMetaData.REMINDER;
		if (toOriginal) 
			query += " / 10 ";
		else 
			query += " * 10";
		query += " where " +
				TransferTableMetaData._ID + " in (" + updateIDs + " ) ";
		DBTools.execQuery(context, query);
    }  
	
	public static void insertTransactionsFromRPTransaction(Context context, long accountID, int transactionType, Date transDate, Double amount, String description, 
			int repeatType, int customInterval, Date periodEnd, long transferID, long currId, long categoryID, int transactionStatus, int transactionPaymentMethod) {
		if (repeatType == Constants.TransferType.Once.index())
		{
			ContentValues values = new ContentValues();
			if (transDate.compareTo(Tools.getCurrentDate())<=0)
			{
				TransactionSrv.insertTransaction(context, accountID, categoryID, transDate, amount, transactionType, 
						context.getString(R.string.recurring) + context.getString(R.string.twopoints) + description, 
						transferID, currId, 0, null, transactionStatus, transactionPaymentMethod);
				values.putNull(TransferTableMetaData.NEXTPAYMENT);
			}
			else 
			{
				values.put(TransferTableMetaData.NEXTPAYMENT, Tools.DateToDBString(transDate));
			}
			context.getContentResolver().update(Uri.withAppendedPath(TransferTableMetaData.CONTENT_URI, String.valueOf(transferID)), values, null, null);				
		}
		else {
			Date beginDate = transDate;
			Date endDate = Tools.getCurrentDate();
			if (periodEnd.compareTo(endDate) < 0)
				endDate = periodEnd;
			while (beginDate.compareTo(endDate) <= 0)
			{
				TransactionSrv.insertTransaction(context, accountID, categoryID, beginDate, amount, transactionType, 
						context.getString(R.string.recurring) + context.getString(R.string.twopoints) + description, 
						transferID, currId, 0, null, transactionStatus, transactionPaymentMethod);
				beginDate = Tools.getNextDate(repeatType, beginDate, customInterval);
			}
			ContentValues values = new ContentValues();
			if (beginDate.compareTo(periodEnd) <= 0)
				values.put(TransferTableMetaData.NEXTPAYMENT, Tools.DateToDBString(beginDate));
			else
				values.putNull(TransferTableMetaData.NEXTPAYMENT);
			context.getContentResolver().update(Uri.withAppendedPath(TransferTableMetaData.CONTENT_URI, String.valueOf(transferID)), values, null, null);
		}
	}
}
