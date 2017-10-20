package com.jgmoneymanager.services;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.DebtsTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.paid.DebtsList;
import com.jgmoneymanager.mmlibrary.R;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

public class DebtsSrv {
	
	public static void generateUnpaidDebtsNotification(Context context) {
		String today = Tools.DateToDBString(Tools.getCurrentDate());
		Cursor cursor = context.getContentResolver().query(DebtsTableMetaData.CONTENT_URI, null, 
				DebtsTableMetaData.BACKDATE + " is not null and " + 
				DebtsTableMetaData.BACKDATE + " = '" + today + "' and " +
				DebtsTableMetaData.REMINDME + " = 1 and " + 
				DebtsTableMetaData.STATUS + " = " + Constants.Status.Enabled.index(), null, null);
		if (cursor.moveToFirst()) {
			ContentValues values = new ContentValues();
			values.put(DebtsTableMetaData.REMINDME, 0);
			for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
				context.getContentResolver().update(DebtsTableMetaData.CONTENT_URI, values, 
						DebtsTableMetaData._ID + " = ?", 
						new String[] {DBTools.getCursorColumnValue(cursor, DebtsTableMetaData._ID)});
			}
			String notification = context.getString(R.string.msgYouHaveUnpaidDebts);
			Intent notificationIntent = new Intent(context, DebtsList.class);
			DialogTools.systemNotification(context, notificationIntent, 2, 
					context.getString(R.string.app_name_pro) + " - " + notification,
					context.getString(R.string.app_name_pro), notification, R.drawable.icon);
		}
		cursor.close();		
	}
}
