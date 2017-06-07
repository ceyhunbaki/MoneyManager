package com.jgmoneymanager.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.PaymentMethodsTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransactionsTableMetaData;
import com.jgmoneymanager.mmlibrary.R;

/**
 * Created by User on 10/03/2016.
 */
public class PaymentMethodsSrv {

    public static void insertMethod(Context context, String name, int sortOrder, Integer resourseID) {
        ContentValues values = new ContentValues();
        values.put(PaymentMethodsTableMetaData.NAME, name);
        if (sortOrder != 0)
            values.put(PaymentMethodsTableMetaData.SORTORDER, sortOrder);
        else
            values.putNull(PaymentMethodsTableMetaData.SORTORDER);
        if (resourseID != null)
            values.put(PaymentMethodsTableMetaData.RESOURCEID, resourseID);
        else
            values.putNull(PaymentMethodsTableMetaData.RESOURCEID);
        context.getContentResolver().insert(PaymentMethodsTableMetaData.CONTENT_URI, values);
    }

    public static void updateMethod(Context context, long rowId, String name, int sortOrder, boolean emptyResID) {
        ContentValues values = new ContentValues();
        values.put(PaymentMethodsTableMetaData.NAME, name);
        if (sortOrder != 0)
            values.put(PaymentMethodsTableMetaData.SORTORDER, sortOrder);
        if (emptyResID)
            values.putNull(PaymentMethodsTableMetaData.RESOURCEID);
        context.getContentResolver().update(PaymentMethodsTableMetaData.CONTENT_URI, values,
                PaymentMethodsTableMetaData._ID + " = " + rowId, null);
    }

    public static void deleteMethod(Context context, long rowId) {
        context.getContentResolver().delete(Uri.withAppendedPath(PaymentMethodsTableMetaData.CONTENT_URI, String.valueOf(rowId)), null, null);

        ContentValues values = new ContentValues();
        values.putNull(TransactionsTableMetaData.PAYMENT_METHOD);
        context.getContentResolver().update(TransactionsTableMetaData.CONTENT_URI, values, TransactionsTableMetaData.PAYMENT_METHOD + " = " + String.valueOf(rowId), null);
    }

    /**
     * Returns the first item's ID
     * @param context
     * @param methodName if you want to get the name of default method sent variable, else send null
     * @return
     */
    public static long getDefaultMethodID(Context context, StringBuilder methodName) {
        long id = 0;
        Cursor cursor = context.getContentResolver().query(PaymentMethodsTableMetaData.CONTENT_URI, new String[]{PaymentMethodsTableMetaData._ID},
                null, null, null);
        if (cursor.moveToFirst()) {
            id = DBTools.getCursorColumnValueLong(cursor, PaymentMethodsTableMetaData._ID);
            if (methodName != null)
                methodName.append(DBTools.getCursorColumnValue(cursor, PaymentMethodsTableMetaData.NAME));
        }
        else if (methodName != null)
            methodName.append(context.getResources().getString(R.string.notSet));
        return id;
    }

    public static String getNameByID(Context context, long rowID) {
        String name = context.getResources().getString(R.string.notSet);
        Cursor cursor = context.getContentResolver().query(Uri.withAppendedPath(PaymentMethodsTableMetaData.CONTENT_URI, String.valueOf(rowID)),
                new String[]{PaymentMethodsTableMetaData._ID, PaymentMethodsTableMetaData.NAME}, null, null, null);
        if (cursor.moveToFirst()) {
            name = DBTools.getCursorColumnValue(cursor, PaymentMethodsTableMetaData.NAME);
        }
        return name;
    }

    public static void controlFirstItems(Context context) {
        if (getDefaultMethodID(context, null) == 0) {
            PaymentMethodsSrv.insertMethod(context, context.getResources().getString(R.string.cash), 1, R.string.cash);
            PaymentMethodsSrv.insertMethod(context, context.getResources().getString(R.string.card), 2, R.string.card);
            PaymentMethodsSrv.insertMethod(context, context.getResources().getString(R.string.check), 3, R.string.check);
        }
    }
}
