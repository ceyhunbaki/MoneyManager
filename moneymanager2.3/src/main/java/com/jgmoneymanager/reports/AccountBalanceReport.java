package com.jgmoneymanager.reports;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.AccountTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CurrencyTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransactionsTableMetaData;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.main.R;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.RefreshLabelTask;
import com.jgmoneymanager.tools.Tools;

import java.util.Date;

/**
 * Created by Ceyhun on 17.12.2015.
 */

public class AccountBalanceReport extends MyActivity {
    Date selectedDate;
    ListView listView;
    Double balance;
    final String accountCurrIDAlias = "acc_curr_id";
    String query;

    final int dateDialogID = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listreport);
        ((TextView)findViewById(R.id.tvATTitle)).setText(R.string.repAccountBalanceReport);

        (findViewById(R.id.btATMenu)).setVisibility(View.GONE);

        if (savedInstanceState != null) {
            selectedDate = Tools.getDateFromBundle(savedInstanceState, "selectedDate");
            balance = Tools.getDoubleFromBundle(savedInstanceState, "balance");
        }
        else {
            selectedDate = Tools.truncDate(Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncMonth);
            balance = 0d;
        }

        findViewById(R.id.repLayInterval).setVisibility(View.GONE);
        findViewById(R.id.repImgDateRight).setVisibility(View.INVISIBLE);
        findViewById(R.id.repImgDateLeft).setVisibility(View.INVISIBLE);
        findViewById(R.id.repTotal).setVisibility(View.VISIBLE);

        refreshList(selectedDate);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Tools.putToBundle(outState, "selectedDate", selectedDate);
        Tools.putToBundle(outState, "balance", balance);
        super.onSaveInstanceState(outState);
    }

    void refreshList(Date selectedDate){
        balance = 0d;

        ((Button)findViewById(R.id.repBtDate)).setText(Tools.DateToString(selectedDate, Constants.DateFormatUser));

        listView = new ListView(this);
        listView.setScrollingCacheEnabled(true);
        listView.setCacheColorHint(00000000);
        listView.setBackgroundColor(getResources().getColor(R.color.White));
        listView.setDivider(getResources().getDrawable(R.color.newThemeBlue));
        listView.setDividerHeight(Math.round(getResources().getDimension(R.dimen.main_round_button_side) / getResources().getDisplayMetrics().density));

        String accountNameAlias = "account_name";
        String currencyNameAlias = "currency_name";
        String valueAlias = "valueAlias";
        query = "Select " + TransactionsTableMetaData.BALANCE + " || ' ' || " + currencyNameAlias + " " + valueAlias +
                ", " + TransactionsTableMetaData.BALANCE +
                ", " + accountNameAlias + ", " + accountCurrIDAlias + ", t4.account_id " +
                AccountTableMetaData._ID + ", " + AccountTableMetaData.ISDEFAULT + ", " + AccountTableMetaData.SORTORDER + ", " + currencyNameAlias +
                "  From transactions t3\n" +
                "  Join (Select max_date, account_id, " + accountNameAlias + ", " + accountCurrIDAlias + ", " + currencyNameAlias +
                ", Max (t1." + TransactionsTableMetaData._ID + ") max_id\n" + ", " + AccountTableMetaData.ISDEFAULT + ", " + AccountTableMetaData.SORTORDER +
                "          From transactions t1\n" +
                "          Join (Select Max(tr." + TransactionsTableMetaData.TRANSDATE + ") max_date, ar." + AccountTableMetaData._ID +
                "                       , ar." + AccountTableMetaData.NAME + " " + accountNameAlias + ", ar." + AccountTableMetaData.CURRID +
                "                       " + accountCurrIDAlias + ", ar." + AccountTableMetaData.ISDEFAULT + ", ar." + AccountTableMetaData.SORTORDER +
                "                       , cu." + CurrencyTableMetaData.SIGN + " " + currencyNameAlias +
                "                  From " + TransactionsTableMetaData.TABLE_NAME + " tr\n" +
                "                  Join (Select a." + AccountTableMetaData._ID + ", " + AccountTableMetaData.NAME + ", a." + AccountTableMetaData.CURRID +
                "                              , " + AccountTableMetaData.ISDEFAULT + ", " + AccountTableMetaData.SORTORDER +
                "                          From " + AccountTableMetaData.TABLE_NAME + " a \n" +
                "                           Join " + TransactionsTableMetaData.TABLE_NAME + " tr on tr." + TransactionsTableMetaData.ACCOUNTID + " = " +
                "                               a." + AccountTableMetaData._ID +
                "                           Where a." + AccountTableMetaData.STATUS + " = 1 " +
                "                           or " + TransactionsTableMetaData.TRANSDATE + " between '" + Tools.DateToDBString(Tools.AddMonth(selectedDate,-1)) +
                "                                   ' and '" + Tools.DateToDBString(selectedDate) + "'\n" +
                "                           group by a." + AccountTableMetaData._ID + ", " + AccountTableMetaData.NAME + ", a." + AccountTableMetaData.CURRID +
                "                                    , " + AccountTableMetaData.ISDEFAULT + ", " + AccountTableMetaData.SORTORDER + ") ar\n" +
                "                    On ar." + AccountTableMetaData._ID + " = tr." + TransactionsTableMetaData.ACCOUNTID +
                "                   Join " + CurrencyTableMetaData.TABLE_NAME + " cu on cu." + CurrencyTableMetaData._ID + " = ar." + AccountTableMetaData.CURRID +
                "                  Where " + TransactionsTableMetaData.TRANSDATE + " <= '" + Tools.DateToDBString(selectedDate) + "' " +
                "                  Group By ar." + AccountTableMetaData._ID + ", ar." + AccountTableMetaData.NAME + ", ar." + AccountTableMetaData.CURRID +
                "                       , " + currencyNameAlias + ", ar." + AccountTableMetaData.ISDEFAULT + ", ar." + AccountTableMetaData.SORTORDER + ") t2\n" +
                "            On t1.account_id = t2._id And t1." + TransactionsTableMetaData.TRANSDATE + " = t2.max_date\n" +
                "         Group By max_date, account_id, " + accountNameAlias + ", " + accountCurrIDAlias + ", " + AccountTableMetaData.ISDEFAULT +
                "               , " + AccountTableMetaData.SORTORDER + ") t4\n" +
                "    On t4.max_id = t3." + TransactionsTableMetaData._ID +
                " order by " + AccountTableMetaData.ISDEFAULT + " desc, " + AccountTableMetaData.SORTORDER + ", " + accountNameAlias;
        Cursor cursor = DBTools.createCursor(AccountBalanceReport.this, query);
        String[] from = new String[]{accountNameAlias, valueAlias};
        int to[] = new int[] {R.id.l2column1, R.id.l2column2};
        SimpleCursorAdapter adapter = new MyListAdapter(cursor, this, R.layout.list2columnrow, from, to);
        listView.setAdapter(adapter);
        DBTools.closeDatabase();

        RelativeLayout myLayout = (RelativeLayout) findViewById(R.id.repLayList);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        layoutParams.setMargins(Math.round(getResources().getDimension(R.dimen.combined_list_label_left_margin) / getResources().getDisplayMetrics().density), 0,
                Math.round(getResources().getDimension(R.dimen.combined_list_label_right_margin) / getResources().getDisplayMetrics().density), 0);

        listView.setLayoutParams(layoutParams);

        myLayout.addView(listView);

        refreshTotalLabel();
    }

    void refreshTotalLabel() {
        TextView tvTotal = (TextView)findViewById(R.id.repTotal);
        RefreshLabelTask refreshLabelTask = new RefreshLabelTask(AccountBalanceReport.this, query, tvTotal, TransactionsTableMetaData.BALANCE,
                true, accountCurrIDAlias, selectedDate);
        refreshLabelTask.execute();
    }

    class MyListAdapter extends SimpleCursorAdapter {

        Context context;

        public MyListAdapter(Cursor cursor, Context context, int rowId,
                             String[] from, int[] to) {
            super(context, rowId, cursor, from, to);
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            if (view == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                view = inflater.inflate(R.layout.list2columnrow, null);
            }
            if (position % 2 == 0)
                view.setBackgroundColor(getResources().getColor(
                        R.color.AntiqueWhite));
            else
                view.setBackgroundColor(getResources().getColor(R.color.White));

            //Cursor cursor = (Cursor) super.getItem(position);
            //long defaultCurrencyID = CurrencySrv.getDefaultCurrencyID(AccountBalanceReport.this);
            //long currencyID = DBTools.getCursorColumnValueLong(cursor, accountCurrIDAlias);
            //double amount = DBTools.getCursorColumnValueDouble(cursor, TransactionsTableMetaData.BALANCE);

            /*if (defaultCurrencyID != currencyID) {
                amount = CurrRatesSrv.convertAmount(AccountBalanceReport.this, amount, currencyID, defaultCurrencyID, Tools.getLeastDate(selectedDate, Tools.getCurrentDate()));
            }*/
            //TextView

            return view;
        }
    }

    public void myClickHandler(View target) {
        switch (target.getId()) {
            case R.id.repBtDate:
                showDialog(dateDialogID);
                break;
            default:
                break;
        }
    }
    @Override
    protected Dialog onCreateDialog(int id) {
        Date date;
        switch (id) {
            case dateDialogID:
                if (selectedDate != null)
                    date = selectedDate;
                else
                    date = Tools.getCurrentDate();
                return new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year,
                                          int monthOfYear, int dayOfMonth) {
                        selectedDate = new Date(year - 1900, monthOfYear, dayOfMonth);
                        refreshList(selectedDate);
                    }
                }, date.getYear() + 1900, date.getMonth(), date.getDate());
        }
        return null;
    }

}
