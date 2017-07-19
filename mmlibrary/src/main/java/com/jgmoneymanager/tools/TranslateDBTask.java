package com.jgmoneymanager.tools;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;
import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CurrencyTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.PaymentMethodsTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransactionStatusTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CategoryTableMetaData;
import com.jgmoneymanager.services.CategorySrv;
import com.jgmoneymanager.services.CurrencySrv;
import com.jgmoneymanager.services.PaymentMethodsSrv;
import com.jgmoneymanager.services.TransactionStatusSrv;

/**
 * Created by Jeyhun on 02/08/2015.
 * Translates category names in DB to selected language
 */
public class TranslateDBTask extends AsyncTask<String, Void, Boolean> {
    Context context = null;

    public TranslateDBTask(Context context) {
        this.context = context;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        Cursor cursor = context.getContentResolver().query(CategoryTableMetaData.CONTENT_URI,
                new String[] {CategoryTableMetaData._ID, CategoryTableMetaData.NAME, CategoryTableMetaData.RESOURCEID},
                CategoryTableMetaData.RESOURCEID + " is not null ", null, null);
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            int stringID = 0;
            try {
                stringID = DBTools.getCursorColumnValueInt(cursor, CategoryTableMetaData.RESOURCEID);
                CategorySrv.changeCategoryName(context, DBTools.getCursorColumnValueLong(cursor, CategoryTableMetaData._ID),
                        context.getString(stringID), false);
            }
            catch (Exception e) {
                Tracker myTracker = EasyTracker.getInstance(context);
                myTracker.set(Fields.SCREEN_NAME, "TranslateDBTask- Error1. StringID:" + stringID);
                myTracker.send(MapBuilder.createAppView().build());
            }
        }

        cursor = context.getContentResolver().query(TransactionStatusTableMetaData.CONTENT_URI,
                new String[] {TransactionStatusTableMetaData._ID, TransactionStatusTableMetaData.NAME, TransactionStatusTableMetaData.RESOURCEID},
                TransactionStatusTableMetaData.RESOURCEID + " is not null ", null, null);
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            int stringID = DBTools.getCursorColumnValueInt(cursor, TransactionStatusTableMetaData.RESOURCEID);
            TransactionStatusSrv.updateStatus(context, DBTools.getCursorColumnValueLong(cursor, TransactionStatusTableMetaData._ID),
                    context.getString(stringID), 0, false);
        }

        cursor = context.getContentResolver().query(PaymentMethodsTableMetaData.CONTENT_URI,
                new String[] {PaymentMethodsTableMetaData._ID, PaymentMethodsTableMetaData.NAME, PaymentMethodsTableMetaData.RESOURCEID},
                PaymentMethodsTableMetaData.RESOURCEID + " is not null ", null, null);
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            int stringID = DBTools.getCursorColumnValueInt(cursor, PaymentMethodsTableMetaData.RESOURCEID);
            PaymentMethodsSrv.updateMethod(context, DBTools.getCursorColumnValueLong(cursor, PaymentMethodsTableMetaData._ID),
                    context.getString(stringID), 0, false);
        }

        cursor = context.getContentResolver().query(CurrencyTableMetaData.CONTENT_URI,
                new String[] {CurrencyTableMetaData._ID, CurrencyTableMetaData.NAME, CurrencyTableMetaData.RESOURCEID, CurrencyTableMetaData.SIGN},
                CurrencyTableMetaData.RESOURCEID + " is not null ", null, null);
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            int stringID = DBTools.getCursorColumnValueInt(cursor, CurrencyTableMetaData.RESOURCEID);
            CurrencySrv.updateCurrency(context, DBTools.getCursorColumnValueLong(cursor, CurrencyTableMetaData._ID),
                    context.getString(stringID),
                    DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData.SIGN),
                    false);
        }

        return null;
    }
}
