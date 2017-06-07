package com.jgmoneymanager.main;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.AccountTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CurrencyTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.PaymentMethodsTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransactionStatusTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransactionsTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VCategoriesViewMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VTransactionViewMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.services.AccountSrv;
import com.jgmoneymanager.services.BudgetSrv;
import com.jgmoneymanager.services.CategorySrv;
import com.jgmoneymanager.services.CurrRatesSrv;
import com.jgmoneymanager.services.CurrencySrv;
import com.jgmoneymanager.services.PaymentMethodsSrv;
import com.jgmoneymanager.services.TransactionSrv;
import com.jgmoneymanager.services.TransactionStatusSrv;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.GetCurrencyRateTask;
import com.jgmoneymanager.tools.LocalTools;
import com.jgmoneymanager.tools.Tools;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TransactionEdit extends MyActivity {

    long accountID = 0;
    long oldAccountID = 0;
    long categoryID = 0;
    long oldCategoryID = 0;
    long statusID = 0;
    long methodID = 0;
    long currID = Constants.defaultCurrency;
    long oldCurrID = Constants.defaultCurrency;
    long accountCurrID = Constants.defaultCurrency;
    long oldAccountCurrID = Constants.defaultCurrency;
    Double rate;
    Double oldRate;
    Double amount;
    String description;
    String editedID;
    Date transDate = Tools.getCurrentDate();
    Date oldTransDate = Tools.getCurrentDate();
    //Button btAccount;
    Button btCategory;
    Button btCurrency;
    EditText edRate;
    Button btTrTransDate;
    int transactionType;
    Double oldAmount;
    long isTransfer = 0;
    long transactionFOperType = 0;
    final int transDateDialogID = 1;

    private AdView adView;

    String mCurrentPhotoPath;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        LinearLayout mainLayout = (LinearLayout) findViewById(R.id.content_main_layout);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        View child = inflater.inflate(R.layout.transactionedit, null);
        mainLayout.addView(child, params);

        //((TextView)findViewById(R.id.cusTitleText)).setText(R.string.transactions);
        if (savedInstanceState != null) {
            accountID = Tools.getLongFromBundle0(savedInstanceState, "accountID");
            oldAccountID = Tools.getLongFromBundle0(savedInstanceState, "oldAccountID");
            categoryID = Tools.getLongFromBundle0(savedInstanceState, "categoryID");
            oldCategoryID = Tools.getLongFromBundle0(savedInstanceState, "oldCategoryID");
            currID = Tools.getLongFromBundle0(savedInstanceState, "currID");
            oldCurrID = Tools.getLongFromBundle0(savedInstanceState, "oldCurrID");
            accountCurrID = Tools.getLongFromBundle0(savedInstanceState, "accountCurrID");
            oldAccountCurrID = Tools.getLongFromBundle0(savedInstanceState, "oldAccountCurrID");
            rate = Tools.getDoubleFromBundle(savedInstanceState, "rate");
            oldRate = Tools.getDoubleFromBundle(savedInstanceState, "oldRate");
            amount = Tools.getDoubleFromBundle(savedInstanceState, "amount");
            oldAmount = Tools.getDoubleFromBundle(savedInstanceState, "oldAmount");
            description = Tools.getStringFromBundle(savedInstanceState, "description");
            editedID = Tools.getStringFromBundle(savedInstanceState, "editedID");
            transDate = Tools.getDateFromBundle(savedInstanceState, "transDate");
            oldTransDate = Tools.getDateFromBundle(savedInstanceState, "oldTransDate");
            transactionType = Tools.getIntegerFromBundle0(savedInstanceState, "transactionType");
            isTransfer = Tools.getLongFromBundle0(savedInstanceState, "isTransfer");
            transactionFOperType = Tools.getLongFromBundle0(savedInstanceState, "transactionFOperType");
            mCurrentPhotoPath = Tools.getStringFromBundle(savedInstanceState, "mCurrentPhotoPath");
            statusID = Tools.getLongFromBundle0(savedInstanceState, "statusID");
            methodID = Tools.getLongFromBundle0(savedInstanceState, "methodID");
            reloadScreen(null);
            refreshCurrency(getBaseContext(), true);
            if (SplitTransaction.splitItemsList != null)
                reloadSplitItems();
        } else {
            if (getIntent().getAction().equals(Intent.ACTION_INSERT)) {
                StringBuilder sb;
                transactionType = this.getIntent().getExtras().getInt(TransactionsTableMetaData.TRANSTYPE);
                switch (transactionType) {
                    case Constants.TransactionTypeExpence:
                        setTitle(R.string.addExpence);
                        break;
                    case Constants.TransactionTypeIncome:
                        setTitle(R.string.addIncome);
                        break;
                    default:
                        break;
                }
                if (this.getIntent().getExtras().containsKey(Constants.paramAccountID)) {
                    accountID = getIntent().getExtras().getLong(Constants.paramAccountID);
                    //btAccount.setText(AccountEdit.getAccountNameByID(getBaseContext(), accountID));
                } else {
                    sb = new StringBuilder();
                    accountID = AccountSrv.getDefultAccountID(this, sb);
                    //btAccount.setText(sb.toString());
                }
                //setTransdate(TransactionEdit.this, Tools.getCurrentDate());
                currID = AccountSrv.getCurrencyIdByAcocuntID(TransactionEdit.this, accountID);
                accountCurrID = currID;
                categoryID = 0;
                transDate = Tools.getCurrentDate();
                isTransfer = 0;
                amount = 0d;
                rate = 1.0d;
                description = "";
                statusID = TransactionStatusSrv.getDefaultStatusID(this, null);
                methodID = PaymentMethodsSrv.getDefaultMethodID(this, null);
                reloadScreen(null);
                refreshCurrency(TransactionEdit.this, true);
            } else if (getIntent().getAction().equals(Intent.ACTION_EDIT)) {
                setTitle(R.string.editTransaction);
                findViewById(R.id.btTrInc).setEnabled(false);
                findViewById(R.id.btTrExp).setEnabled(false);
                Cursor cursor = this.managedQuery(
                        Uri.withAppendedPath(VTransactionViewMetaData.CONTENT_URI, getIntent().getExtras().getString(VTransactionViewMetaData._ID)),
                        new String[]{}, null, null, null);
                editedID = getIntent().getExtras().getString(VTransactionViewMetaData._ID);
                transactionFOperType = TransactionSrv.getTransactionType(TransactionEdit.this, Long.valueOf(editedID), false);
                if (transactionFOperType == Constants.TransFOperType.Transfer.index())
                    isTransfer = 1;
                cursor.moveToFirst();
                accountID = DBTools.getCursorColumnValueLong(cursor, VTransactionViewMetaData.ACCOUNTID);
                oldAccountID = accountID;
                categoryID = DBTools.getCursorColumnValueLong(cursor, VTransactionViewMetaData.CATEGORYID);
                oldCategoryID = categoryID;
                transDate = DBTools.getCursorColumnValueDate(cursor, VTransactionViewMetaData.TRANSDATE);
                oldTransDate = transDate;
                amount = DBTools.getCursorColumnValueDouble(cursor, VTransactionViewMetaData.AMOUNT);
                oldAmount = amount;
                description = DBTools.getCursorColumnValue(cursor, VTransactionViewMetaData.DESCRIPTION);
                //((EditText) findViewById(R.id.edTrAmount)).setText(String.valueOf(amount));
                //((EditText) findViewById(R.id.edTrDescription)).setText(DBTools.getCursorColumnValue(cursor, VTransactionViewMetaData.DESCRIPTION));
                transactionType = DBTools.getCursorColumnValueInt(cursor, VTransactionViewMetaData.TRANSTYPE);
                currID = DBTools.getCursorColumnValueInt(cursor, VTransactionViewMetaData.CURRID);
                oldCurrID = currID;
                accountCurrID = DBTools.getCursorColumnValueInt(cursor, VTransactionViewMetaData.ACCOUNTCURRID);
                oldAccountCurrID = accountCurrID;
                rate = Tools.parseDouble(CurrRatesSrv.getRate(this, oldCurrID, oldAccountCurrID, oldTransDate));
                oldRate = rate;
                mCurrentPhotoPath = DBTools.getCursorColumnValue(cursor, VTransactionViewMetaData.PHOTO_PATH);

                statusID = DBTools.getCursorColumnValueLong(cursor, VTransactionViewMetaData.STATUS);
                methodID = DBTools.getCursorColumnValueLong(cursor, VTransactionViewMetaData.PAYMENT_METHOD);
                //((Button)findViewById(R.id.btTrStatus)).setText(TransactionStatusSrv.getNameByID(this, statusID));

                //cursor.close();

                reloadScreen(null);
                refreshCurrency(this, true);

                findViewById(R.id.layTrSplitMain).setVisibility(View.GONE);
            }
        }

        // Create the adView
        try {
            if (!Tools.proVersionExists(this) /*&& (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE)*/) {
//                adView = new AdView(this, AdSize.BANNER, "ca-app-pub-5995868530154544/3468276114");
//                RelativeLayout layout = (RelativeLayout) findViewById(R.id.TrLayoutAds);
//                // Add the adView to it
//                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
//                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
//                layout.addView(adView, layoutParams); // Initiate a generic request to load it with an ad
//                AdRequest adRequest = new AdRequest();
//                adView.loadAd(adRequest);
                MobileAds.initialize(getApplicationContext(), "ca-app-pub-5995868530154544/5913285717");
                adView = new AdView(this);
                adView.setAdSize(AdSize.SMART_BANNER);
                adView.setAdUnitId("ca-app-pub-5995868530154544/5913285717");
                LinearLayout layout = (LinearLayout) findViewById(R.id.TrLayoutAds);
                layout.addView(adView);
                AdRequest adRequest = new AdRequest.Builder().build();
                adView.loadAd(adRequest);
            }
        }
        catch (Exception e) {

        }

        registerForContextMenu(findViewById(R.id.btTrPhoto));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Tools.putToBundle(outState, "accountID", accountID);
        Tools.putToBundle(outState, "oldAccountID", oldAccountID);
        Tools.putToBundle(outState, "categoryID", categoryID);
        Tools.putToBundle(outState, "oldCategoryID", oldCategoryID);
        Tools.putToBundle(outState, "currID", currID);
        Tools.putToBundle(outState, "oldCurrID", oldCurrID);
        Tools.putToBundle(outState, "accountCurrID", accountCurrID);
        Tools.putToBundle(outState, "oldAccountCurrID", oldAccountCurrID);
        Tools.putToBundle(outState, "rate", rate);
        Tools.putToBundle(outState, "oldRate", oldRate);
        Tools.putToBundle(outState, "amount", amount);
        Tools.putToBundle(outState, "oldAmount", oldAmount);
        Tools.putToBundle(outState, "description", description);
        Tools.putToBundle(outState, "editedID", editedID);
        Tools.putToBundle(outState, "transDate", transDate);
        Tools.putToBundle(outState, "oldTransDate", oldTransDate);
        Tools.putToBundle(outState, "transactionType", transactionType);
        Tools.putToBundle(outState, "isTransfer", isTransfer);
        Tools.putToBundle(outState, "transactionFOperType", transactionFOperType);
        Tools.putToBundle(outState, "mCurrentPhotoPath", mCurrentPhotoPath);
        Tools.putToBundle(outState, "statusID", statusID);
        Tools.putToBundle(outState, "methodID", methodID);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.transactionedit);
        reloadScreen(null);
    }

    private void reloadScreen(String categoryName) {
        //onCreateden emeliyyatlari bura kecirib bunu hem de confChange-de cagir
        edRate = (EditText) findViewById(R.id.edTrRate);
        //btAccount = (Button) findViewById(R.id.btTrAccount);
        btCategory = (Button) findViewById(R.id.btTrCategory);
        btCurrency = (Button) findViewById(R.id.btTrCurrency);
        btTrTransDate = (Button) findViewById(R.id.btTrTransDate);

        final EditText edAmount = (EditText) findViewById(R.id.edTrAmount);
        edAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                /*if (edAmount.getText().toString().length() != 0)
                    try {
                        amount = Tools.controlCorrectNumberInUserFormat(TransactionEdit.this, edAmount.getText().toString());
                    } catch (NumberFormatException e) {
                        DialogTools.toastDialog(TransactionEdit.this, R.string.msgInvalidNumber, Toast.LENGTH_SHORT);
                        //amount = 0d;
                    }
                else amount = 0d;
                Tools.formatEditTextAmountInUserFormat(edAmount);*/
                if (edAmount.getText().toString().length() != 0)
                    try {
                        amount = Double.parseDouble(edAmount.getText().toString());
                    } catch (NumberFormatException e) {
                        DialogTools.toastDialog(TransactionEdit.this, R.string.msgInvalidNumber, Toast.LENGTH_SHORT);
                        amount = 0d;
                    }
                else amount = 0d;
            }
        });
        if (amount.compareTo(0d) != 0)
            edAmount.setText(Tools.formatDecimal(amount));

        final EditText edDescription = (EditText) findViewById(R.id.edTrDescription);
        edDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                description = edDescription.getText().toString();
            }
        });
        edDescription.setText(description);

        edRate.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (edRate.getText().toString().length() != 0)
                    try {
                        rate = Double.parseDouble(edRate.getText().toString());
                    } catch (NumberFormatException e) {
                        DialogTools.toastDialog(TransactionEdit.this, R.string.msgInvalidNumber, Toast.LENGTH_SHORT);
                        rate = 0d;
                    }
                else rate = 0d;
            }
        });
        edRate.setText(Tools.formatDecimal(rate));

        if (categoryName == null)
            btCategory.setText(CategorySrv.getCategoryNameByID(getBaseContext(), categoryID));
        else
            btCategory.setText(categoryName);
    	/*btCategory.setEnabled(isTransfer == 0);
		((Button)findViewById(R.id.btTrCategoryEd)).setEnabled(isTransfer == 0);*/
        setTransdate(getBaseContext(), transDate);

        if (mCurrentPhotoPath != null)
            reloadPhotoTumbnail();

        Cursor cursor = this.getContentResolver().query(TransactionStatusTableMetaData.CONTENT_URI, null, null, null, null);
        Spinner spStatus = ((Spinner) findViewById(R.id.spTrStatus));
        LocalTools.fillSpinner(spStatus, this, cursor, TransactionStatusTableMetaData.NAME);
        if (statusID != 0) {
            int position = 0;
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                if (DBTools.getCursorColumnValueLong(cursor, TransactionStatusTableMetaData._ID) == statusID) {
                    spStatus.setSelection(position);
                    break;
                }
                position++;
            }
        }

        cursor = this.getContentResolver().query(PaymentMethodsTableMetaData.CONTENT_URI, null, null, null, null);
        Spinner spMethod = ((Spinner) findViewById(R.id.spTrMethod));
        LocalTools.fillSpinner(spMethod, this, cursor, PaymentMethodsTableMetaData.NAME);
        if (methodID != 0) {
            int position = 0;
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                if (DBTools.getCursorColumnValueLong(cursor, PaymentMethodsTableMetaData._ID) == methodID) {
                    spMethod.setSelection(position);
                    break;
                }
                position++;
            }
        }

        cursor = this.getContentResolver().query(AccountTableMetaData.CONTENT_URI,
                new String[] {AccountTableMetaData._ID, AccountTableMetaData.NAME}, AccountTableMetaData.STATUS + " = 1 ", null, null);
        final Spinner spAccount = ((Spinner) findViewById(R.id.spTrAccount));
        LocalTools.fillSpinner(spAccount, this, cursor, AccountTableMetaData.NAME);
        if (accountID != 0) {
            Tools.setAccountSpinnerValue(cursor, spAccount, accountID);
        }
        spAccount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                accountID = id;
                accountCurrID = AccountSrv.getCurrencyIdByAcocuntID(TransactionEdit.this, accountID);
                refreshCurrency(TransactionEdit.this, true);

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        showBudgetValue();

        if (getIntent().getAction().equals(Intent.ACTION_EDIT)) {
            findViewById(R.id.btTrInc).setEnabled(false);
            findViewById(R.id.btTrExp).setEnabled(false);
            setTitle(R.string.editTransaction);
        }
        else {
            if (transactionType == Constants.TransactionTypeExpence)
                setTitle(R.string.addExpence);
            else
                setTitle(R.string.addIncome);
            findViewById(R.id.btTrDelete).setVisibility(View.GONE);
        }
    }

    /**
     * If budget for selected category is avialable, then shows {tvTrProgressText} and sets text
     */
    void showBudgetValue() {
        if ((transactionType == Constants.TransactionTypeExpence)
                && Tools.getPreferenceBool(TransactionEdit.this, R.string.enablebudgetkey, true)) {
            StringBuilder sbRemaining = new StringBuilder();
            StringBuilder sbBudget = new StringBuilder();
            StringBuilder sbUsed = new StringBuilder();
            RelativeLayout layout = (RelativeLayout) findViewById(R.id.layTrBudget);
            /**@TODO burda ayin birinde ve iksinde xeta cixir*/
            try {
                if (BudgetSrv.getCategoryRemainingBudget(getBaseContext(), categoryID, transDate, sbRemaining, sbBudget, sbUsed)) {
                    layout.setVisibility(View.VISIBLE);
                    TextView tvBudget = (TextView) findViewById(R.id.tvTrProgressText);
                    tvBudget.setText(getResources().getString(R.string.remainingBudget) + " " + sbRemaining.toString());
                    ProgressBar pBar = (ProgressBar) findViewById(R.id.pbTrProgress);
                    pBar.setProgress((int) Math.round(Tools.stringToDouble(TransactionEdit.this, sbUsed.toString(), false) * 100
                            / Tools.stringToDouble(TransactionEdit.this, sbBudget.toString(), false)));
                } else
                    layout.setVisibility(View.GONE);
            } catch (Exception e) {
                layout.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case transDateDialogID:

                return new DatePickerDialog(this, new OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        setTransdate(TransactionEdit.this, new Date(year - 1900, monthOfYear, dayOfMonth));
                    }
                }, transDate.getYear() + 1900, transDate.getMonth(), transDate.getDate());
        }
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == Constants.RequestCategoryForTransaction) {
                Uri selectedUri = data.getData();
                Cursor cursor = this.managedQuery(selectedUri, null, null, null, null);
                cursor.moveToFirst();
                categoryID = Integer.parseInt(DBTools.getCursorColumnValue(cursor, VCategoriesViewMetaData._ID));
                btCategory.setText(DBTools.getCursorColumnValue(cursor, VCategoriesViewMetaData.NAME));
                showBudgetValue();
            } else if (requestCode == Constants.RequestCurrencyForTransaction) {
                Uri selectedUri = data.getData();
                Cursor cursor = this.managedQuery(selectedUri, null, null, null, null);
                cursor.moveToFirst();
                currID = Integer.parseInt(DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData._ID));
                btCurrency.setText(DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData.NAME) +
                        ": " + DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData.SIGN));
                StringBuilder sbRate = new StringBuilder();
                edRate.setEnabled(!CurrRatesSrv.rateExists(TransactionEdit.this, currID, accountCurrID, transDate, sbRate, null));
                (findViewById(R.id.btTrRateEd)).setEnabled(edRate.isEnabled());
                edRate.setText(sbRate.toString());
                rate = Tools.stringToDouble(TransactionEdit.this, sbRate.toString(), false);
                if (edRate.isEnabled()) {
                    GetCurrencyRateTask getRateTask = new GetCurrencyRateTask(TransactionEdit.this, edRate,
                            CurrencySrv.getCurrencySignByID(TransactionEdit.this, currID),
                            CurrencySrv.getCurrencySignByID(TransactionEdit.this, accountCurrID));
                    getRateTask.execute("");
                }
            } else if (requestCode == Constants.RequestCalculator) {
                amount = data.getDoubleExtra(Constants.calculatorValue, 0d);
                ((EditText) findViewById(R.id.edTrAmount)).setText(Tools.formatDecimal(amount));
            } else if (requestCode == Constants.RequestCalculatorForRate) {
                rate = data.getDoubleExtra(Constants.calculatorValue, 0d);
                ((EditText) findViewById(R.id.edTrRate)).setText(Tools.formatDecimal(rate));
            }
            else if (requestCode == Constants.RequestCamera) {
                reloadPhotoTumbnail();
            }
            else if (requestCode == Constants.RequestGallery) {
                Uri selectedImage = data.getData();
                String[] filePathColumn = { MediaStore.Images.Media.DATA };
                Cursor cursor = getContentResolver().query(selectedImage,filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                mCurrentPhotoPath = cursor.getString(columnIndex);
                reloadPhotoTumbnail();
            }
            else if (requestCode == Constants.RequestSplitTransaction) {
                reloadSplitItems();
            }
        }
    }

    void reloadPhotoTumbnail() {
        if (mCurrentPhotoPath == null)
            ((ImageButton)findViewById(R.id.btTrPhoto)).setImageResource(R.drawable.camera);
        else {
            Bitmap photo = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(mCurrentPhotoPath), 64, 64);
            if (photo != null)
                ((ImageButton) findViewById(R.id.btTrPhoto)).setImageBitmap(photo);
            else
                ((ImageButton) findViewById(R.id.btTrPhoto)).setImageResource(R.drawable.camera);
        }
    }

    boolean existsPhoto() {
        if (mCurrentPhotoPath == null)
            return false;
        else {
            Bitmap photo = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(mCurrentPhotoPath), 64, 64);
            return (photo != null);
        }
    }

    private void setTransdate(Context context, Date inDate) {
        transDate = inDate;
        btTrTransDate.setText(Tools.DateToString(transDate, Constants.DateFormatUser));
        refreshCurrency(context, false);
        showBudgetValue();
    }

    private void refreshCurrency(Context context, boolean refreshRate) {
        btCurrency.setText(CurrencySrv.getCurrencyNameSignByID(context, currID));
        edRate.setEnabled(currID != accountCurrID);
        (findViewById(R.id.btTrRateEd)).setEnabled(currID != accountCurrID);
        StringBuilder sbRate = new StringBuilder();
        edRate.setEnabled(!CurrRatesSrv.rateExists(context, currID, accountCurrID, transDate, sbRate, null));
        (findViewById(R.id.btTrRateEd)).setEnabled(edRate.isEnabled());
        if (refreshRate) {
            rate = Tools.stringToDouble(context, sbRate.toString(), false);
            edRate.setText(sbRate.toString());
        } else
            edRate.setText(String.valueOf(rate));
        if (edRate.isEnabled()) {
            GetCurrencyRateTask getRateTask = new GetCurrencyRateTask(TransactionEdit.this, edRate,
                    CurrencySrv.getCurrencySignByID(TransactionEdit.this, currID),
                    CurrencySrv.getCurrencySignByID(TransactionEdit.this, accountCurrID));
            getRateTask.execute("");
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderIcon(R.drawable.camera);
        menu.setHeaderTitle(R.string.msgPleaseSelect);
        if (existsPhoto()) {
            menu.add(R.string.msgViewCurrentPhoto);
            menu.add(R.string.msgDeleteCurrentPhoto);
        }
        menu.add(R.string.msgTakeNew);
        menu.add(R.string.msgChooseFromGallery);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getTitle().toString().equals(getResources().getString(R.string.msgTakeNew))) {
            dispatchTakePictureIntent();
        }
        else if (item.getTitle().toString().equals(getResources().getString(R.string.msgChooseFromGallery))) {
            Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(pickPhoto , Constants.RequestGallery);
        }
        else if (item.getTitle().toString().equals(getResources().getString(R.string.msgDeleteCurrentPhoto))) {
            mCurrentPhotoPath = null;
            reloadPhotoTumbnail();
        }
        else if (item.getTitle().toString().equals(getResources().getString(R.string.msgViewCurrentPhoto))) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse("file://" + mCurrentPhotoPath), "image/*");
            startActivity(intent);
        }
        return super.onContextItemSelected(item);
    }

    public void myClickHandler(View target) {
        Intent intent;
        try {
            switch (target.getId()) {
                case R.id.btTrAccountEd:
                    (findViewById(R.id.spTrAccount)).performClick();
                    break;
                case R.id.btTrCategory:
                    if (isTransfer == 1)
                        DialogTools.toastDialog(TransactionEdit.this, R.string.msgCategoryNotAllowedTransfer, Toast.LENGTH_LONG);
                    else if (findViewById(R.id.layTrSplitRow1).getVisibility() == View.VISIBLE) {
                        DialogTools.toastDialog(TransactionEdit.this, R.string.msgCategoryNotAllowedSplit, Toast.LENGTH_SHORT);
                    }
                    else {
                        if (transactionType == Constants.TransactionTypeIncome)
                            intent = new Intent(getBaseContext(), CategoryListForIncome.class);
                        else
                            intent = new Intent(getBaseContext(), CategoryListForExpense.class);
                        intent.setAction(Intent.ACTION_PICK);
                        startActivityForResult(intent, Constants.RequestCategoryForTransaction);
                    }
                    break;
                case R.id.btTrCategoryEd:
                    myClickHandler(findViewById(R.id.btTrCategory));
                    break;
                case R.id.btTrCurrency:
                    intent = new Intent(getBaseContext(), CurrencyList.class);
                    intent.setAction(Intent.ACTION_PICK);
                    startActivityForResult(intent, Constants.RequestCurrencyForTransaction);
                    break;
                case R.id.btTrCurrencyEd:
                    myClickHandler(findViewById(R.id.btTrCurrency));
                    break;
                case R.id.btTrTransDate:
                    showDialog(transDateDialogID);
                    break;
                case R.id.btTrTransDateEd:
                    myClickHandler(findViewById(R.id.btTrTransDate));
                    break;
                case R.id.btTrInc:
                    if (transactionType != Constants.TransactionTypeIncome)
                        switchToIncome();
                    break;
                case R.id.btTrExp:
                    if (transactionType != Constants.TransactionTypeExpence)
                        switchToExpense();
                    break;
                case R.id.btTrPhoto:
                    //dispatchTakePictureIntent();
                    openContextMenu(target);
                    break;
                case R.id.btTrStatusEd:
                    (findViewById(R.id.spTrStatus)).performClick();
                    break;
                case R.id.btTrMethodEd:
                    (findViewById(R.id.spTrMethod)).performClick();
                    break;
                case R.id.btTrOk:
                    String amount = ((EditText) findViewById(R.id.edTrAmount)).getText().toString();
                    if ((amount == null) || (amount.length() == 0) || amount.equals(".") || amount.equals(",")) {
                        DialogTools.toastDialog(TransactionEdit.this, TransactionEdit.this.getResources().getString(R.string.msgEnter) + " " +
                                TransactionEdit.this.getResources().getString(R.string.amount), Toast.LENGTH_LONG);
                    } else if (accountID == 0)
                        DialogTools.toastDialog(TransactionEdit.this, getString(R.string.msgAccountNotSelected), Toast.LENGTH_LONG);
                    else if ((currID != accountCurrID) && (rate == 0))
                        DialogTools.toastDialog(TransactionEdit.this, getString(R.string.msgEnter) + " " + getString(R.string.rate), Toast.LENGTH_LONG);
                    else {
                        statusID = ((Spinner) findViewById(R.id.spTrStatus)).getSelectedItemId();
                        methodID = ((Spinner) findViewById(R.id.spTrMethod)).getSelectedItemId();
                        CurrRatesSrv.insertRate(TransactionEdit.this, currID, accountCurrID, Tools.stringToDouble(TransactionEdit.this, edRate.getText().toString(), false), transDate);
                        if (getIntent().getAction().equals(Intent.ACTION_INSERT)) {
                            if (findViewById(R.id.layTrSplitRow1).getVisibility() == View.VISIBLE) {
                                for (int i=0; i<10; i++) {
                                    SplitTransaction.SplitItem splitItem = SplitTransaction.splitItemsList.get(i);
                                    if ((Double.compare(splitItem.amount, 0d) >0) && (splitItem.categoryID != 0))
                                        TransactionSrv.insertTransaction(getBaseContext(), accountID, splitItem.categoryID, transDate,
                                                splitItem.amount, transactionType,
                                                description, 0, currID, accountCurrID, mCurrentPhotoPath, statusID, methodID);
                                }
                            }
                            else
                                TransactionSrv.insertTransaction(getBaseContext(), accountID, categoryID, transDate, Tools.stringToDouble(TransactionEdit.this, amount, false), transactionType,
                                    description, 0, currID, accountCurrID, mCurrentPhotoPath, statusID, methodID);
                            setResult(RESULT_OK);
                            finish();
                        } else if (getIntent().getAction().equals(Intent.ACTION_EDIT)) {
                            final StringBuilder sbAccountID = new StringBuilder();
                            long otherTransactionID2 = 0;
                            try {
                                otherTransactionID2 = TransactionSrv.getTransferOtherID(TransactionEdit.this, Long.parseLong(editedID), sbAccountID);
                            } catch (NumberFormatException e) {
                                Tracker myTracker = EasyTracker.getInstance(getBaseContext());     // Get a reference to tracker.
                                myTracker.set(Fields.SCREEN_NAME, "Transaction Screen- Error2");
                                myTracker.send(MapBuilder.createAppView().build());
                            }
                            final long otherTransactionID = otherTransactionID2;
                            if (otherTransactionID != 0) {
                                Command updateYesCommand = new Command() {
                                    public void execute() {
                                        //StringBuilder sbAccountID = new StringBuilder();
                                        //long otherTransactionID = TransactionEdit.getTransferOtherID(TransactionEdit.this, Long.parseLong(editedID), sbAccountID);
                                        try {
                                            long otherAccountID = Long.parseLong(sbAccountID.toString());
                                            long otherAccountCurrID = AccountSrv.getCurrencyIdByAcocuntID(TransactionEdit.this, otherAccountID);
                                            if (otherTransactionID != 0)
                                                TransactionSrv.updateTransaction(TransactionEdit.this, String.valueOf(otherTransactionID),
                                                        otherAccountID, otherAccountID, 0, 0, oldTransDate,
                                                        Tools.StringToDate(btTrTransDate.getText().toString(), Constants.DateFormatUser),
                                                        oldAmount, Tools.stringToDouble(TransactionEdit.this, ((EditText) findViewById(R.id.edTrAmount)).getText().toString(), false), -transactionType,
                                                        null, oldCurrID, currID, otherAccountCurrID, otherAccountCurrID, isTransfer, mCurrentPhotoPath, statusID, methodID);
                                        } catch (NumberFormatException nm) {
                                            Tracker myTracker = EasyTracker.getInstance(getBaseContext());     // Get a reference to tracker.
                                            myTracker.set(Fields.SCREEN_NAME, "Transaction Screen- Error1");
                                            myTracker.send(MapBuilder.createAppView().build());
                                        }
                                        TransactionSrv.updateTransaction(TransactionEdit.this, editedID, oldAccountID, accountID,
                                                oldCategoryID, categoryID, oldTransDate,
                                                Tools.StringToDate(btTrTransDate.getText().toString(), Constants.DateFormatUser),
                                                oldAmount, Tools.stringToDouble(TransactionEdit.this, ((EditText) findViewById(R.id.edTrAmount)).getText().toString(), false), transactionType,
                                                description, oldCurrID, currID, oldAccountCurrID, accountCurrID, isTransfer, mCurrentPhotoPath, statusID, methodID);
                                        setResult(RESULT_OK);
                                        finish();
                                    }
                                };
                                Command updateNoCommand = new Command() {
                                    public void execute() {
                                        TransactionSrv.updateTransaction(TransactionEdit.this, editedID, oldAccountID, accountID,
                                                oldCategoryID, categoryID, oldTransDate,
                                                Tools.StringToDate(btTrTransDate.getText().toString(), Constants.DateFormatUser),
                                                oldAmount, Tools.stringToDouble(TransactionEdit.this, ((EditText) findViewById(R.id.edTrAmount)).getText().toString(), false), transactionType,
                                                ((EditText) findViewById(R.id.edTrDescription)).getText().toString(),
                                                oldCurrID, currID, oldAccountCurrID, accountCurrID, isTransfer, mCurrentPhotoPath, statusID, methodID);
                                        setResult(RESULT_OK);
                                        finish();
                                    }
                                };
                                Command updateCancelCommand = new Command() {
                                    public void execute() {
                                        setResult(RESULT_CANCELED);
                                        finish();
                                    }
                                };
                                AlertDialog deleteOtherTransfer = DialogTools.confirmWithCancelDialog(TransactionEdit.this, updateYesCommand,
                                        updateNoCommand, updateCancelCommand, R.string.msgConfirm, R.string.msgUpdateOtherAccountTransaction);
                                deleteOtherTransfer.show();
                            } else {
                                TransactionSrv.updateTransaction(TransactionEdit.this, editedID, oldAccountID, accountID,
                                        oldCategoryID, categoryID, oldTransDate,
                                        Tools.StringToDate(btTrTransDate.getText().toString(), Constants.DateFormatUser),
                                        oldAmount, Tools.stringToDouble(TransactionEdit.this, ((EditText) findViewById(R.id.edTrAmount)).getText().toString(), false), transactionType,
                                        ((EditText) findViewById(R.id.edTrDescription)).getText().toString(),
                                        oldCurrID, currID, oldAccountCurrID, accountCurrID, isTransfer, mCurrentPhotoPath, statusID, methodID);
                                setResult(RESULT_OK);
                                finish();
                            }
                        }
                    }
                    break;
                case R.id.btTrCancel:
                    setResult(RESULT_CANCELED);
                    finish();
                    break;
                case R.id.btTrCalc:
                    if (findViewById(R.id.layTrSplitRow1).getVisibility() == View.VISIBLE) {
                        DialogTools.toastDialog(TransactionEdit.this, R.string.msgAmountNotAllowedSplit, Toast.LENGTH_SHORT);
                    }
                    else {
                        intent = new Intent(getBaseContext(), Calculator.class);
                        if (Double.compare(TransactionEdit.this.amount, 0) != 0) {
                            Bundle bundle = new Bundle();
                            bundle.putDouble(Calculator.startupAmountKey, TransactionEdit.this.amount);
                            intent.putExtras(bundle);
                        }
                        intent.setAction(Intent.ACTION_PICK);
                        if(getCurrentFocus()!=null) {
                            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                        }
                        startActivityForResult(intent, Constants.RequestCalculator);
                    }
                    break;
                case R.id.btTrRateEd:
                    intent = new Intent(getBaseContext(), Calculator.class);
                    if (Double.compare(TransactionEdit.this.rate, 0) != 0) {
                        Bundle bundle = new Bundle();
                        bundle.putDouble(Calculator.startupAmountKey, TransactionEdit.this.rate);
                        intent.putExtras(bundle);
                    }
                    intent.setAction(Intent.ACTION_PICK);
                    if(getCurrentFocus()!=null) {
                        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                    }
                    startActivityForResult(intent, Constants.RequestCalculatorForRate);
                    break;
                case R.id.btTrDelete:
                    final long otherTransactionID = TransactionSrv.getTransferOtherID(TransactionEdit.this, Long.parseLong(editedID), null);
                    if (otherTransactionID != 0) {
                        Command deleteCommand = new Command() {

                            public void execute() {
                                Command deleteYesCommand = new Command() {
                                    public void execute() {
                                        if (otherTransactionID != 0)
                                            TransactionSrv.deleteTransaction(TransactionEdit.this, String.valueOf(otherTransactionID));
                                        TransactionSrv.deleteTransaction(TransactionEdit.this, editedID);
                                        setResult(RESULT_OK);
                                        finish();
                                    }
                                };
                                Command deleteNoCommand = new Command() {
                                    public void execute() {
                                        TransactionSrv.deleteTransaction(TransactionEdit.this, editedID);
                                        setResult(RESULT_OK);
                                        finish();
                                    }
                                };
                                Command deleteCancelCommand = new Command() {
                                    public void execute() {

                                    }
                                };
                                AlertDialog deleteOtherTransfer = DialogTools.confirmWithCancelDialog(TransactionEdit.this, deleteYesCommand,
                                        deleteNoCommand, deleteCancelCommand, R.string.msgConfirm, R.string.msgDeleteOtherAccountTransaction);
                                deleteOtherTransfer.show();
                            }
                        };
                        AlertDialog deleteDialog = DialogTools.confirmDialog(TransactionEdit.this, deleteCommand, R.string.msgConfirm, R.string.msgDeleteItem);
                        deleteDialog.show();
                    } else {
                        final String rowId = editedID;
                        Command cmd1 = new Command() {
                            @Override
                            public void execute() {
                                TransactionSrv.deleteTransaction(TransactionEdit.this, rowId);
                                setResult(RESULT_OK);
                                finish();
                            }
                        };
                        AlertDialog deleteDialog = DialogTools.confirmDialog(TransactionEdit.this, cmd1, R.string.msgConfirm, R.string.msgDeleteItem);
                        deleteDialog.show();
                    }
                    break;
                case R.id.btTrSplit:
                    intent = new Intent(this, SplitTransaction.class);
                    Bundle bundle = new Bundle();
                    bundle.putInt(Constants.paramTransactionType, transactionType);
                    if (findViewById(R.id.layTrSplitRow1).getVisibility() == View.VISIBLE)
                        bundle.putBoolean(Constants.dontRefreshValues, true);
                    intent.putExtras(bundle);
                    startActivityForResult(intent, Constants.RequestSplitTransaction);
                    break;
                case R.id.btTrSplitDel:
                    SplitTransaction.splitItemsList = null;
                    findViewById(R.id.edTrAmount).setEnabled(true);
                    hideAllSplitItems();
                    btCategory.setText(null);
                    break;
                case R.id.btTrSplitEdit:
                    intent = new Intent(this, SplitTransaction.class);
                    bundle = new Bundle();
                    bundle.putInt(Constants.paramTransactionType, transactionType);
                    bundle.putBoolean(Constants.dontRefreshValues, true);
                    intent.putExtras(bundle);
                    startActivityForResult(intent, Constants.RequestSplitTransaction);
                default:
                    break;
            }
        } catch (NumberFormatException e) {
            Tracker myTracker = EasyTracker.getInstance(getBaseContext());     // Get a reference to tracker.
            myTracker.set(Fields.SCREEN_NAME, "Transaction Screen- Error3");
            myTracker.send(MapBuilder.createAppView().build());
        }
    }

    void switchToIncome() {
        setTitle(R.string.addIncome);
        transactionType = Constants.TransactionTypeIncome;
        categoryID = 0;
        btCategory.setText(R.string.notSet);
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.layTrBudget);
        layout.setVisibility(View.GONE);
    }

    void switchToExpense() {
        setTitle(R.string.addExpence);
        transactionType = Constants.TransactionTypeExpence;
        categoryID = 0;
        btCategory.setText(R.string.notSet);
    }

    @Override
    protected void onDestroy() {
        try {
            super.onDestroy();
            if (adView != null)
                adView.destroy();
        } catch (Exception ex) {

        }
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = /*Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);*/new File(Constants.receiptDirectory);
        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                AlertDialog warning = DialogTools.warningDialog(TransactionEdit.this, com.jgmoneymanager.mmlibrary.R.string.msgWarning,
                        getString(com.jgmoneymanager.mmlibrary.R.string.msgChooseReceiptFolder));
                warning.show();
            }
        }
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getPath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                DialogTools.toastDialog(this, R.string.msgCameraError, Toast.LENGTH_LONG);
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, Constants.RequestCamera);
            }
        }
    }

    void hideAllSplitItems() {
        findViewById(R.id.layTrSplitRow1).setVisibility(View.GONE);
        findViewById(R.id.layTrSplitRow2).setVisibility(View.GONE);
        findViewById(R.id.layTrSplitRow3).setVisibility(View.GONE);
        findViewById(R.id.layTrSplitRow4).setVisibility(View.GONE);
        findViewById(R.id.layTrSplitRow5).setVisibility(View.GONE);
        findViewById(R.id.layTrSplitRow6).setVisibility(View.GONE);
        findViewById(R.id.layTrSplitRow7).setVisibility(View.GONE);
        findViewById(R.id.layTrSplitRow8).setVisibility(View.GONE);
        findViewById(R.id.layTrSplitRow9).setVisibility(View.GONE);
        findViewById(R.id.layTrSplitRow10).setVisibility(View.GONE);
    }

    void setSplitRow(int layoutID, int categoryViewID, int amountViewID, SplitTransaction.SplitItem splitItem) {
        findViewById(layoutID).setVisibility(View.VISIBLE);
        ((TextView) findViewById(categoryViewID)).setText(CategorySrv.getCategoryNameByID(this, splitItem.categoryID));
        ((TextView) findViewById(amountViewID)).setText(Tools.formatDecimal(splitItem.amount));

    }

    void reloadSplitItems() {

        hideAllSplitItems();
        double sumAmount = 0d;
        for (int i = 0; i < SplitTransaction.splitItemsList.size(); i++) {
            SplitTransaction.SplitItem splitItem = SplitTransaction.splitItemsList.get(i);
            if ((splitItem.categoryID != 0) && (Double.compare(splitItem.amount, 0d) != 0)) {
                sumAmount += splitItem.amount;
                switch (i) {
                    case 0:
                        setSplitRow(R.id.layTrSplitRow1, R.id.lbTrSplitCategories1, R.id.lbTrSplitAmounts1, splitItem);
                        break;
                    case 1:
                        setSplitRow(R.id.layTrSplitRow2, R.id.lbTrSplitCategories2, R.id.lbTrSplitAmounts2, splitItem);
                        break;
                    case 2:
                        setSplitRow(R.id.layTrSplitRow3, R.id.lbTrSplitCategories3, R.id.lbTrSplitAmounts3, splitItem);
                        break;
                    case 3:
                        setSplitRow(R.id.layTrSplitRow4, R.id.lbTrSplitCategories4, R.id.lbTrSplitAmounts4, splitItem);
                        break;
                    case 4:
                        setSplitRow(R.id.layTrSplitRow5, R.id.lbTrSplitCategories5, R.id.lbTrSplitAmounts5, splitItem);
                        break;
                    case 5:
                        setSplitRow(R.id.layTrSplitRow6, R.id.lbTrSplitCategories6, R.id.lbTrSplitAmounts6, splitItem);
                        break;
                    case 6:
                        setSplitRow(R.id.layTrSplitRow7, R.id.lbTrSplitCategories7, R.id.lbTrSplitAmounts7, splitItem);
                        break;
                    case 7:
                        setSplitRow(R.id.layTrSplitRow8, R.id.lbTrSplitCategories8, R.id.lbTrSplitAmounts8, splitItem);
                        break;
                    case 8:
                        setSplitRow(R.id.layTrSplitRow9, R.id.lbTrSplitCategories9, R.id.lbTrSplitAmounts9, splitItem);
                        break;
                    case 9:
                        setSplitRow(R.id.layTrSplitRow10, R.id.lbTrSplitCategories10, R.id.lbTrSplitAmounts10, splitItem);
                        break;
                }
            }
        }
        if (Double.compare(sumAmount, 0d) > 0) {
            btCategory.setText(R.string.split);
            categoryID = 0;
            ((EditText) findViewById(R.id.edTrAmount)).setText(Tools.formatDecimal(sumAmount));
            findViewById(R.id.edTrAmount).setEnabled(false);

            if ((description == null) || (description.trim().length() == 0)) {
                description = getResources().getString(R.string.split) + ":" + ((EditText) findViewById(R.id.edTrAmount)).getText() +
                        CurrencySrv.getCurrencySignByID(this, currID);
                ((EditText) findViewById(R.id.edTrDescription)).setText(description);
            }
        }
    }

}
