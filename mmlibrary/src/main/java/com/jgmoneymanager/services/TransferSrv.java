package com.jgmoneymanager.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransactionsTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransferTableMetaData;
import com.jgmoneymanager.mmlibrary.R;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import java.util.Date;

public class TransferSrv {

	public static void controlTransfers(Context context)
	{
		Cursor cursor = context.getContentResolver().query(TransferTableMetaData.CONTENT_URI, null, 
				TransferTableMetaData.NEXTPAYMENT + " <= '" + Tools.DateToDBString(Tools.getCurrentDate()) + "' and " +
				TransferTableMetaData.NEXTPAYMENT + " is not null  and (" + 
				TransferTableMetaData.FIRSTACCOUNTID + " is not null and " + 
				TransferTableMetaData.SECONDACCOUNTID + " is not null) and " +
				TransferTableMetaData.STATUS + " = " + String.valueOf(Constants.Status.Enabled.index()), 
				null, TransferTableMetaData.TRANSDATE);
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()){
			insertTransactionsFromTransfer(context, DBTools.getCursorColumnValueLong(cursor, TransferTableMetaData.FIRSTACCOUNTID), 
					DBTools.getCursorColumnValueLong(cursor, TransferTableMetaData.SECONDACCOUNTID), 
					DBTools.getCursorColumnValueDate(cursor, TransferTableMetaData.NEXTPAYMENT, Constants.DateFormatDB), 
					DBTools.getCursorColumnValueDouble(cursor, TransferTableMetaData.AMOUNT), 
					DBTools.getCursorColumnValue(cursor, TransferTableMetaData.DESCRIPTION), 
					DBTools.getCursorColumnValueInt(cursor, TransferTableMetaData.REPEATTYPE), 
					DBTools.getCursorColumnValueInt(cursor, TransferTableMetaData.CUSTOMINTERVAL), 
					DBTools.getCursorColumnValueDate(cursor, TransferTableMetaData.PERIODEND, Constants.DateFormatDB), 
					DBTools.getCursorColumnValueLong(cursor, TransferTableMetaData._ID),
					DBTools.getCursorColumnValueLong(cursor, TransferTableMetaData.CURRENCYID));
		}
	}
	
	public static void insertTransactionsFromTransfer(Context context, long fromAccountID, long toAccountID, Date transDate, Double amount, String description, 
			int repeatType, int customInterval, Date periodEnd, long transferID, long currId) {
		if (repeatType == Constants.TransferType.Once.index())
		{
			ContentValues values = new ContentValues();
			if (transDate.compareTo(Tools.getCurrentDate())<=0)
			{
				TransactionSrv.insertTransaction(context, fromAccountID, 0, transDate, amount, Constants.TransactionTypeExpence, 
						context.getString(R.string.transfer) + context.getString(R.string.twopoints) + description, 
						transferID, currId, 0, null, 0, 0);
				TransactionSrv.insertTransaction(context, toAccountID, 0, transDate, amount, Constants.TransactionTypeIncome, 
						context.getString(R.string.transfer) + context.getString(R.string.twopoints) + description, transferID, 
						currId, 0, null, 0, 0);
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
				TransactionSrv.insertTransaction(context, fromAccountID, 0, beginDate, amount, Constants.TransactionTypeExpence, 
						context.getString(R.string.transfer) + context.getString(R.string.twopoints) + description, transferID, 
						currId, 0, null, 0, 0);
				TransactionSrv.insertTransaction(context, toAccountID, 0, beginDate, amount, Constants.TransactionTypeIncome, 
						context.getString(R.string.transfer) + context.getString(R.string.twopoints) + description, transferID, 
						currId, 0, null, 0, 0);
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

	public static void deleteTransfersFromAccount(Context context, String accountID) {
		String query = "delete from " + TransactionsTableMetaData.TABLE_NAME + " tr \n" +
				"  where exists (select 1 from " + TransferTableMetaData.TABLE_NAME + " tf \n" +
				"                  where tf." + TransferTableMetaData._ID + " = tr." + TransactionsTableMetaData.TRANSFERID +
				" and (tf." + TransferTableMetaData.FIRSTACCOUNTID + " = " + accountID + " or tr." + TransferTableMetaData.SECONDACCOUNTID +
				" = " + accountID + ")";
		DBTools.execQuery(context, query);
		context.getContentResolver().delete(TransferTableMetaData.CONTENT_URI, TransferTableMetaData.FIRSTACCOUNTID + " = " + accountID + " or " +
								TransferTableMetaData.SECONDACCOUNTID + " = " + accountID, null);
	}
}
