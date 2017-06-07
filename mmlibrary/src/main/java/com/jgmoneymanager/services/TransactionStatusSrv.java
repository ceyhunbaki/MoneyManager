package com.jgmoneymanager.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransactionsTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransactionStatusTableMetaData;
import com.jgmoneymanager.mmlibrary.R;

/**
 * Created by User on 02/06/2015.
 */
public class TransactionStatusSrv {

    public static void insertStatus(Context context, String name, int sortOrder, Integer resourseID) {
        ContentValues values = new ContentValues();
        values.put(TransactionStatusTableMetaData.NAME, name);
        if (sortOrder != 0)
            values.put(TransactionStatusTableMetaData.SORTORDER, sortOrder);
        else
            values.putNull(TransactionStatusTableMetaData.SORTORDER);
        if (resourseID != null)
            values.put(TransactionStatusTableMetaData.RESOURCEID, resourseID);
        else
            values.putNull(TransactionStatusTableMetaData.RESOURCEID);
        context.getContentResolver().insert(TransactionStatusTableMetaData.CONTENT_URI, values);
    }

    public static void updateStatus(Context context, long rowId, String name, int sortOrder, boolean emptyResID) {
        ContentValues values = new ContentValues();
        values.put(TransactionStatusTableMetaData.NAME, name);
        if (sortOrder != 0)
            values.put(TransactionStatusTableMetaData.SORTORDER, sortOrder);
        if (emptyResID)
            values.putNull(TransactionStatusTableMetaData.RESOURCEID);
        context.getContentResolver().update(TransactionStatusTableMetaData.CONTENT_URI, values,
                TransactionStatusTableMetaData._ID + " = " + rowId, null);
    }

    public static void deleteStatus(Context context, long rowId) {
        context.getContentResolver().delete(Uri.withAppendedPath(TransactionStatusTableMetaData.CONTENT_URI, String.valueOf(rowId)), null, null);

        ContentValues values = new ContentValues();
        values.putNull(TransactionsTableMetaData.STATUS);
        context.getContentResolver().update(TransactionsTableMetaData.CONTENT_URI, values, TransactionsTableMetaData.STATUS + " = " + String.valueOf(rowId), null);
    }

    /**
     * Returns the first item's ID
     * @param context
     * @param statusName if you want to get the name of default status sent variable, else send null
     * @return
     */
    public static long getDefaultStatusID(Context context, StringBuilder statusName) {
        long id = 0;
        Cursor cursor = context.getContentResolver().query(TransactionStatusTableMetaData.CONTENT_URI, new String[]{TransactionStatusTableMetaData._ID},
                null, null, null);
        if (cursor.moveToFirst()) {
            id = DBTools.getCursorColumnValueLong(cursor, TransactionStatusTableMetaData._ID);
            if (statusName != null)
                statusName.append(DBTools.getCursorColumnValue(cursor, TransactionStatusTableMetaData.NAME));
        }
        else if (statusName != null)
            statusName.append(context.getResources().getString(R.string.notSet));
        return id;
    }

    public static String getNameByID(Context context, long rowID) {
        String name = context.getResources().getString(R.string.notSet);
        Cursor cursor = context.getContentResolver().query(Uri.withAppendedPath(TransactionStatusTableMetaData.CONTENT_URI, String.valueOf(rowID)),
                new String[]{TransactionStatusTableMetaData._ID, TransactionStatusTableMetaData.NAME}, null, null, null);
        if (cursor.moveToFirst()) {
            name = DBTools.getCursorColumnValue(cursor, TransactionStatusTableMetaData.NAME);
        }
        return name;
    }
}
