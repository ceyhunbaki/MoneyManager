package com.jgmoneymanager.tools;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.widget.TextView;

import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.mmlibrary.R;
import com.jgmoneymanager.services.CurrRatesSrv;
import com.jgmoneymanager.services.CurrencySrv;

import java.util.Date;

public class RefreshLabelTask extends AsyncTask<String, Void, Boolean> {
    private final Context ctx;
    private Cursor cursor;
    private final TextView tvTotal;
    private final String sql;
    private String text;
    private Double balance;
    String valueAlias;
    boolean needConversion;
    String currIDAlias;
    Date conversionDate;

    public RefreshLabelTask(Context context, String sql, TextView tvTotal, String valueAlias, boolean needConversion, String currIDAlias, Date conversionDate) {
        ctx = context;
        this.sql = sql;
        this.tvTotal = tvTotal;
        this.valueAlias = valueAlias;
        this.needConversion = needConversion;
        this.currIDAlias = currIDAlias;
        this.conversionDate = conversionDate;
    }

    // can use UI thread here
    protected void onPreExecute() {
        tvTotal.setText("...");
    }

    // automatically done on worker thread (separate from UI thread)
    protected Boolean doInBackground(final String... args) {
        balance = 0d;
        try {
            cursor = DBTools.createCursor(ctx, sql);
            long defaultCurrencyID = CurrencySrv.getDefaultCurrencyID(ctx);
            long transactionCurrencyID = defaultCurrencyID;
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                if (needConversion)
                    transactionCurrencyID = DBTools.getCursorColumnValueLong(cursor, currIDAlias);
                balance += CurrRatesSrv.convertAmount(ctx, DBTools.getCursorColumnValueDouble(cursor, valueAlias), transactionCurrencyID,
                        defaultCurrencyID, Tools.getLeastDate(conversionDate, Tools.getCurrentDate()), true);
            }
            /*if (ctx.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
                text = ctx.getString(R.string.total) + " (" + CurrencySrv.getCurrencySignByID(ctx, Constants.defaultCurrency) + ") " + Tools.formatDecimalInUserFormat(balance);
            else*/
                text = ctx.getString(R.string.total) + " " + Tools.getFullAmountText(balance, CurrencySrv.getCurrencySignByID(ctx, Constants.defaultCurrency), true);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // can use UI thread here
    protected void onPostExecute(final Boolean success) {
        if (success) {
            try {
                tvTotal.setText(text);
            } catch (Exception e) {

            }
        }
    }

}