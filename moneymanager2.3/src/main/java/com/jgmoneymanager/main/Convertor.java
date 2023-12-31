package com.jgmoneymanager.main;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CurrencyTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.services.CurrRatesSrv;
import com.jgmoneymanager.services.CurrencySrv;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.GetCurrencyRateTask;
import com.jgmoneymanager.tools.Tools;

public class Convertor extends MyActivity {

    long fromCurrencyID = 0;
    long toCurrencyID = 0;
    double rate = 1d;
    double value = 1d;
    EditText edRate;

    private AdView adView;

    Cursor cursor = null;

    Currency[] currencies = new Currency[7];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.convertor);

        if (savedInstanceState != null) {
            fromCurrencyID = Tools.getLongFromBundle0(savedInstanceState, "fromCurrencyID");
            toCurrencyID = Tools.getLongFromBundle0(savedInstanceState, "toCurrencyID");
            rate = Tools.getDoubleFromBundle0(savedInstanceState, "rate");
            value = Tools.getDoubleFromBundle0(savedInstanceState, "value");
        }
        reloadScreen();

        if (!Tools.proVersionExists(this) && (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE)) {
            adView = new AdView(this, AdSize.BANNER, "ca-app-pub-5995868530154544/6192487319");
            RelativeLayout layout = (RelativeLayout) findViewById(R.id.layConAd);
            // Add the adView to it
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            layout.addView(adView, params); // Initiate a generic request to load it with an ad
            AdRequest adRequest = new AdRequest();
            adView.loadAd(adRequest);
        }

        //generateMenuItemNames();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Tools.putToBundle(outState, "fromCurrencyID", fromCurrencyID);
        Tools.putToBundle(outState, "toCurrencyID", toCurrencyID);
        Tools.putToBundle(outState, "rate", rate);
        Tools.putToBundle(outState, "value", value);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (adView != null)
                adView.removeAllViews();
            adView.destroy();
        } catch (Exception ex) {

        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.convertor);
        reloadScreen();
    }

    private void reloadScreen() {
        cursor = getContentResolver().query(CurrencyTableMetaData.CONTENT_URI,
                new String[]{CurrencyTableMetaData._ID, CurrencyTableMetaData.NAME, CurrencyTableMetaData.SIGN},
                null, null, null);
        if (cursor.moveToFirst()) {
            Currency curr = new Currency(DBTools.getCursorColumnValueLong(cursor, CurrencyTableMetaData._ID),
                    DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData.SIGN));
            Button button = ((Button) findViewById(R.id.btConvFrom1));
            button.setText(curr.getSign());
            currencies[1] = curr;

            button = ((Button) findViewById(R.id.btConvTo1));
            button.setText(curr.getSign());
            currencies[4] = curr;
        }
        if (cursor.moveToNext()) {
            Currency curr = new Currency(DBTools.getCursorColumnValueLong(cursor, CurrencyTableMetaData._ID),
                    DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData.SIGN));
            Button button = ((Button) findViewById(R.id.btConvFrom2));
            button.setText(curr.getSign());
            currencies[2] = curr;

            button = ((Button) findViewById(R.id.btConvTo2));
            button.setText(curr.getSign());
            currencies[5] = curr;
        }
        if (cursor.moveToNext()) {
            Currency curr = new Currency(DBTools.getCursorColumnValueLong(cursor, CurrencyTableMetaData._ID),
                    DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData.SIGN));
            Button button = ((Button) findViewById(R.id.btConvFrom3));
            button.setText(curr.getSign());
            currencies[3] = curr;

            button = ((Button) findViewById(R.id.btConvTo3));
            button.setText(curr.getSign());
            currencies[6] = curr;
        }

        edRate = (EditText) findViewById(R.id.edConRate);
        edRate.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (edRate.getText().toString().length() != 0)
                    try {
                        rate = Double.parseDouble(edRate.getText().toString());
                    } catch (NumberFormatException e) {
                        DialogTools.toastDialog(Convertor.this, R.string.msgInvalidNumber, Toast.LENGTH_SHORT);
                        rate = 0d;
                    }
                else
                    rate = 0d;
                reloadResult();
            }
        });
        if (Double.compare(rate, 0d) != 0)
            edRate.setText(Tools.formatDecimal(rate));

        final EditText edValue = (EditText) findViewById(R.id.edConValue);
        edValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (edValue.getText().toString().length() != 0)
                    try {
                        value = Double.parseDouble(edValue.getText().toString());
                    } catch (NumberFormatException e) {
                        DialogTools.toastDialog(Convertor.this, R.string.msgInvalidNumber, Toast.LENGTH_SHORT);
                        value = 0d;
                    }
                else
                    value = 0d;
                reloadResult();
            }
        });
        if (Double.compare(value, 0d) != 0)
            edValue.setText(Tools.formatDecimal(value));

        final EditText edFrom = (EditText) findViewById(R.id.edConvFrom);
        edFrom.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                fromCurrencyID = CurrencySrv.getCurrencyIDBySign(Convertor.this, s.toString().toUpperCase());
                reloadRate();
                //refreshListValue(R.id.lvConFrom, edFrom.getText().toString(), cursor1);
            }
        });

        final EditText edTo = (EditText) findViewById(R.id.edConvTo);
        edTo.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                toCurrencyID = CurrencySrv.getCurrencyIDBySign(Convertor.this, s.toString().toUpperCase());
                reloadRate();
                //refreshListValue(R.id.lvConTo, edTo.getText().toString(), cursor2);
            }
        });
    }

    private void refreshEditText(int buttonID, long currencyID) {
        EditText ed = (EditText) findViewById(buttonID);
        ed.setText(CurrencySrv.getCurrencySignByID(getBaseContext(), currencyID));
    }

    private void reloadRate() {
        StringBuilder sbRate = new StringBuilder();
        if ((fromCurrencyID != 0) && (toCurrencyID != 0)) {
            if (CurrRatesSrv.rateExists(getBaseContext(), fromCurrencyID,
                    toCurrencyID, Tools.getCurrentDate(), sbRate, null)) {
                edRate.setText(sbRate.toString());
                rate = Tools.parseDouble(sbRate.toString());
                reloadResult();
            } else {
                if (sbRate.length() != 0) {
                    edRate.setText(sbRate.toString());
                    rate = Tools.parseDouble(sbRate.toString());
                    reloadResult();
                }
            }
            GetCurrencyRateTask getRateTask = new GetCurrencyRateTask(
                    getBaseContext(), edRate,
                    CurrencySrv.getCurrencySignByID(getBaseContext(), fromCurrencyID),
                    CurrencySrv.getCurrencySignByID(getBaseContext(), toCurrencyID));
            getRateTask.execute("");
        }
    }

    private void reloadResult() {
        EditText edResult = (EditText) findViewById(R.id.edConResult);
        edResult.setText(Tools.formatDecimal(value * rate));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case Constants.RequestCurrencyForConvertorFrom:
                    Uri selectedUri = data.getData();
                    Cursor cursor = getContentResolver().query(selectedUri, null, null, null, null);
                    cursor.moveToFirst();
                    fromCurrencyID = Integer.parseInt(DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData._ID));
                    refreshEditText(R.id.edConvFrom, fromCurrencyID);
                    reloadRate();
                    break;
                case Constants.RequestCurrencyForConvertorTo:
                    selectedUri = data.getData();
                    cursor = getContentResolver().query(selectedUri, null, null, null, null);
                    cursor.moveToFirst();
                    toCurrencyID = Integer.parseInt(DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData._ID));
                    refreshEditText(R.id.edConvTo, toCurrencyID);
                    reloadRate();
                    break;
                default:
                    break;
            }
        }
    }

    public void myClickHandler(View target) {
        Intent intent;
        switch (target.getId()) {
            case R.id.btConFrom:
                intent = new Intent(getBaseContext(), CurrencyList.class);
                intent.setAction(Intent.ACTION_PICK);
                startActivityForResult(intent, Constants.RequestCurrencyForConvertorFrom);
                break;
            case R.id.btConTo:
                intent = new Intent(getBaseContext(), CurrencyList.class);
                intent.setAction(Intent.ACTION_PICK);
                startActivityForResult(intent, Constants.RequestCurrencyForConvertorTo);
                break;
            case R.id.btConvFrom1:
                Currency currency = currencies[1];
                fromCurrencyID = currency.getId();
                ((EditText)findViewById(R.id.edConvFrom)).setText(currency.getSign());
                break;
            case R.id.btConvFrom2:
                currency = currencies[2];
                fromCurrencyID = currency.getId();
                ((EditText)findViewById(R.id.edConvFrom)).setText(currency.getSign());
                break;
            case R.id.btConvFrom3:
                currency = currencies[3];
                fromCurrencyID = currency.getId();
                ((EditText)findViewById(R.id.edConvFrom)).setText(currency.getSign());
                break;
            case R.id.btConvTo1:
                currency = currencies[4];
                toCurrencyID = currency.getId();
                ((EditText)findViewById(R.id.edConvTo)).setText(currency.getSign());
                break;
            case R.id.btConvTo2:
                currency = currencies[5];
                toCurrencyID = currency.getId();
                ((EditText)findViewById(R.id.edConvTo)).setText(currency.getSign());
                break;
            case R.id.btConvTo3:
                currency = currencies[6];
                toCurrencyID = currency.getId();
                ((EditText)findViewById(R.id.edConvTo)).setText(currency.getSign());
                break;
            case R.id.imConRefresh:
                long id = fromCurrencyID;
                fromCurrencyID = toCurrencyID;
                toCurrencyID = id;
                refreshEditText(R.id.edConvFrom, fromCurrencyID);
                refreshEditText(R.id.edConvTo, toCurrencyID);
                reloadRate();
                break;
            default:
                break;
        }
    }

    public class MyListAdapter extends SimpleCursorAdapter {

        public MyListAdapter(Cursor cursor, Context context, int rowId,
                             String[] from, int[] to) {
            super(context, rowId, cursor, from, to);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return super.getView(position, convertView, parent);
        }
    }

    /**
     * Returns Cursor position for sign
     *
     * @param cursor
     * @param sign   Currency sign or part of Currency Name
     * @return cursor position
     */
    int getCursorPosition(Cursor cursor, String sign) {
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            String curSign = DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData.SIGN);
            if (curSign.equals("HRK")) {
                System.out.print("ds");
            }
            String curName = DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData.NAME);
            if (curSign.equals(sign.toUpperCase(Tools.getLocale(Convertor.this)))
                    || curSign.substring(0, Math.min(sign.length(), curSign.length() - 1)).equals(sign.toUpperCase(Tools.getLocale(Convertor.this)))
                    || (curName.toUpperCase(Tools.getLocale(Convertor.this)).contains(sign.toUpperCase(Tools.getLocale(Convertor.this))))) {
                return cursor.getPosition();
            }
        }
        return 1;
    }

    class Currency {
        private long id;
        private String sign;

        Currency(long id, String sign) {
            this.id = id;
            this.sign = sign;
        }

        public long getId() {
            return this.id;
        }

        public String getSign() {
            return this.sign;
        }
    }
}
