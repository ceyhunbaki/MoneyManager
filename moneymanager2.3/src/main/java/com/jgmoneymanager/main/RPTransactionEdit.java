package com.jgmoneymanager.main;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.AccountTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CurrencyTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransactionsTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransferTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VCategoriesViewMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VTransferViewMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.services.AccountSrv;
import com.jgmoneymanager.services.CategorySrv;
import com.jgmoneymanager.services.CurrRatesSrv;
import com.jgmoneymanager.services.CurrencySrv;
import com.jgmoneymanager.services.RPTransactionSrv;
import com.jgmoneymanager.services.TransactionSrv;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.LocalTools;
import com.jgmoneymanager.tools.Tools;

import java.util.Date;

public class RPTransactionEdit extends MyActivity {

	long accountID = 0;
	long oldAccountID = 0;
	long categoryID = 0;
	long oldCategoryID = 0;
    int transactionType = 0;
    int oldTransactionType = 0;
	Double amount = 0d;
	Double oldAmount = 0d;
	long currID = 0;
	long oldCurrID = 0;
	long accountCurrID = 0;
	int repeatType = 0;
	int oldRepeatType = 0;
	Date transDate = Tools.getCurrentDate();
	Date periodEnd = transDate;
	Date oldTransDate = transDate;
	Date oldPeriodEnd = transDate;
	Integer customInterval = 0;
	int oldCustomInterval = 0;
	String description;
	double accRate = 1.00d;
	boolean accRateOK = true;
	int updatedID = 0;
	//Button btAccount;
	Button btCategory;
	Button btCurrency;
	Button btPeriodEnd;
	Button btTransDate;
	Spinner spRepeatType;
	Spinner spRPTransType;
	EditText edCustomInterval;
	final int transDateDialogID = 1;
	final int periodEndDialogID = 2;

	private AdView adView;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rptransactionedit);		

		StringBuilder accountName = new StringBuilder();

		if (savedInstanceState != null) {
			accountID = Tools.getLongFromBundle0(savedInstanceState, "accountID");
			oldAccountID = Tools.getLongFromBundle0(savedInstanceState, "oldAccountID");
			categoryID = Tools.getLongFromBundle0(savedInstanceState, "categoryID");
			oldCategoryID = Tools.getLongFromBundle0(savedInstanceState, "oldCategoryID");
            transactionType = Tools.getIntegerFromBundle0(savedInstanceState, "transactionType");
            oldTransactionType = Tools.getIntegerFromBundle0(savedInstanceState, "oldTransactionType");
			amount = Tools.getDoubleFromBundle(savedInstanceState, "amount");
			oldAmount = Tools.getDoubleFromBundle(savedInstanceState, "oldAmount");
			currID = Tools.getLongFromBundle0(savedInstanceState, "currID");
			oldCurrID = Tools.getLongFromBundle0(savedInstanceState, "oldCurrID");
			accountCurrID = Tools.getLongFromBundle0(savedInstanceState, "accountCurrID");
			repeatType = Tools.getIntegerFromBundle0(savedInstanceState, "repeatType");
			oldRepeatType = Tools.getIntegerFromBundle0(savedInstanceState, "oldRepeatType");
			transDate = Tools.StringToDate(Tools.getStringFromBundle(savedInstanceState, "transDate"));
			oldTransDate = Tools.StringToDate(Tools.getStringFromBundle(savedInstanceState, "oldTransDate"));
			periodEnd = Tools.StringToDate(Tools.getStringFromBundle(savedInstanceState, "periodEnd"));
			oldPeriodEnd = Tools.StringToDate(Tools.getStringFromBundle(savedInstanceState, "oldPeriodEnd"));
			customInterval = Tools.getIntegerFromBundle(savedInstanceState, "customInterval");
			oldCustomInterval = Tools.getIntegerFromBundle0(savedInstanceState, "oldCustomInterval");
			description = Tools.getStringFromBundle(savedInstanceState, "description");
			updatedID = Tools.getIntegerFromBundle0(savedInstanceState, "updatedID");
		}
		else {
			if (getIntent().getAction().equals(Intent.ACTION_INSERT))
			{
				Bundle bundle = getIntent().getExtras();
				transDate = Tools.getCurrentDate();
				if ((bundle != null) && (bundle.containsKey(Constants.paramAccountID)))
					accountID = bundle.getLong(Constants.paramAccountID);
				else 
					accountID = AccountSrv.getDefultAccountID(this, accountName);
				amount = 0d;
				categoryID = 0;
				periodEnd = transDate;
				description = getResources().getString(R.string.rpTransShort) + 
						Tools.DateToString(Tools.getCurrentDateTime(), Constants.DateFormatForDescriptions);
				customInterval = 0;
				repeatType = Constants.TransferType.Once.index();
				transactionType = Constants.TransactionType;
                oldTransactionType = transactionType;
				currID = AccountSrv.getCurrencyIdByAcocuntID(this, accountID);	
				updatedID = 0;
				accountCurrID = currID;
				
			}
			else if (getIntent().getAction().equals(Intent.ACTION_EDIT))
			{
				Cursor cursor = this.managedQuery(
						getIntent().getData(), 
						new String[] {}, null, null, null);
				cursor.moveToFirst();
				updatedID = DBTools.getCursorColumnValueInt(cursor, VTransferViewMetaData._ID);
				if (DBTools.getCursorColumnValue(cursor, VTransferViewMetaData.FROMACCOUNTID) != null)
				{
					accountID = DBTools.getCursorColumnValueInt(cursor, VTransferViewMetaData.FROMACCOUNTID);
					transactionType = Constants.TransactionTypeExpence;
					accountCurrID = DBTools.getCursorColumnValueLong(cursor, VTransferViewMetaData.FROMACCCURRID);
					accountName.append(DBTools.getCursorColumnValue(cursor, VTransferViewMetaData.FROMACCOUNTNAME));
				}
				else 
				{
					accountID = DBTools.getCursorColumnValueInt(cursor, VTransferViewMetaData.TOACCOUNTID);
					transactionType = Constants.TransactionTypeIncome;				
					accountCurrID = DBTools.getCursorColumnValueLong(cursor, VTransferViewMetaData.TOACCCURRID);
					accountName.append(DBTools.getCursorColumnValue(cursor, VTransferViewMetaData.TOACCOUNTNAME));
				}
                oldTransactionType = transactionType;
				oldAccountID = accountID;
				transDate = DBTools.getCursorColumnValueDate(cursor, VTransferViewMetaData.TRANSDATE);
				oldTransDate = transDate;
				if (DBTools.getCursorColumnValue(cursor, VTransferViewMetaData.PERIODEND) != null)
				{
					periodEnd = DBTools.getCursorColumnValueDate(cursor, VTransferViewMetaData.PERIODEND);
					oldPeriodEnd = periodEnd;
				}
				amount = DBTools.getCursorColumnValueDouble(cursor, VTransferViewMetaData.AMOUNT);
				categoryID = DBTools.getCursorColumnValueInt(cursor, VTransferViewMetaData.CATEGORYID);
				oldCategoryID = categoryID;
				oldAmount = amount;
				description = DBTools.getCursorColumnValue(cursor, VTransferViewMetaData.DESCRIPTION);
				repeatType = DBTools.getCursorColumnValueInt(cursor, VTransferViewMetaData.REPEATTYPE);
				oldRepeatType = repeatType;			
				if (DBTools.getCursorColumnValue(cursor, VTransferViewMetaData.CUSTOMINTERVAL) != null)
				{
					customInterval = DBTools.getCursorColumnValueInt(cursor, VTransferViewMetaData.CUSTOMINTERVAL);
					oldCustomInterval = customInterval;
				}
				currID = DBTools.getCursorColumnValueLong(cursor, VTransferViewMetaData.CURRENCYID);
				oldCurrID = currID;
			}
		}

		// Create the adView
		try {
			if (!Tools.proVersionExists(this) /*&& (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE)*/) {
				adView = new AdView(this, AdSize.BANNER, "ca-app-pub-5995868530154544/5164011719");
				RelativeLayout layout = (RelativeLayout) findViewById(R.id.RtpLayoutAds);
				// Add the adView to it
				RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
				params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
				layout.addView(adView, params); // Initiate a generic request to load it with an ad
				AdRequest adRequest = new AdRequest();
				adView.loadAd(adRequest);
			}
		}
		catch (Exception e) {

		}

		reloadScreen(accountName);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Tools.putToBundle(outState, "accountID", accountID);
		Tools.putToBundle(outState, "oldAccountID", oldAccountID);
		Tools.putToBundle(outState, "categoryID", categoryID);
		Tools.putToBundle(outState, "oldCategoryID", oldCategoryID);
        Tools.putToBundle(outState, "transactionType", transactionType);
        Tools.putToBundle(outState, "oldTransactionType", oldTransactionType);
		Tools.putToBundle(outState, "amount", amount);
		Tools.putToBundle(outState, "oldAmount", oldAmount);
		Tools.putToBundle(outState, "currID", currID);
		Tools.putToBundle(outState, "oldCurrID", oldCurrID);
		Tools.putToBundle(outState, "accountCurrID", accountCurrID);
		Tools.putToBundle(outState, "repeatType", repeatType);
		Tools.putToBundle(outState, "oldRepeatType", oldRepeatType);
		Tools.putToBundle(outState, "transDate", Tools.DateToString(transDate));
		Tools.putToBundle(outState, "oldTransDate", Tools.DateToString(oldTransDate));
		Tools.putToBundle(outState, "periodEnd", Tools.DateToString(periodEnd));
		Tools.putToBundle(outState, "oldPeriodEnd", Tools.DateToString(oldPeriodEnd));
		Tools.putToBundle(outState, "customInterval", customInterval);
		Tools.putToBundle(outState, "oldCustomInterval", oldCustomInterval);
		Tools.putToBundle(outState, "description", description);
		Tools.putToBundle(outState, "updatedID", updatedID);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			super.onDestroy();
			if (adView != null)
				adView.removeAllViews();
			adView.destroy();
		} catch (Exception ex) {

		}
	}

	/*@Override
	public void onConfigurationChanged(Configuration newConfig) {
	      super.onConfigurationChanged(newConfig);
	      setContentView(R.layout.transferedit);
	      reloadScreen(null);
	}*/
	
	private void reloadScreen(StringBuilder accountName) {
		//btAccount = (Button) findViewById(R.id.btRtpAccount);
		btCategory = (Button) findViewById(R.id.btRtpCategory);		
		spRPTransType = (Spinner)findViewById(R.id.spRtpRPTransType);
		btTransDate = (Button) findViewById(R.id.btRtpTransDate);
		spRepeatType = (Spinner)findViewById(R.id.spRtpRepeat);
		btPeriodEnd = (Button) findViewById(R.id.btRtpPeriodEnd);		
		btCurrency = (Button) findViewById(R.id.btRtpCurrency);
		edCustomInterval = (EditText)findViewById(R.id.edRtpCustomInterval);

		Cursor cursor = this.getContentResolver().query(AccountTableMetaData.CONTENT_URI,
                new String[] {AccountTableMetaData._ID, AccountTableMetaData.NAME}, AccountTableMetaData.STATUS + " = 1 ", null, null);
		Spinner spAccount = ((Spinner) findViewById(R.id.spRtpAccount));
		LocalTools.fillSpinner(spAccount, this, cursor, AccountTableMetaData.NAME);
		if (accountID != 0) {
            setAccountSpinnerValue(cursor, spAccount);
			/*int position = 0;
			for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
				if (DBTools.getCursorColumnValueLong(cursor, AccountTableMetaData._ID) == accountID) {
					spAccount.setSelection(position);
					break;
				}
				position++;
			}*/
		}

		/*if (accountID != 0)
			if ((accountName != null) && (accountName.length() != 0))
				btAccount.setText(accountName.toString());
			else 
				btAccount.setText(AccountSrv.getAccountNameByID(getBaseContext(), accountID));*/


		ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, R.layout.simple_spinner_item_blue, R.id.spinItem, getResources().getStringArray(R.array.RPTransTypes));
		spinnerArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
		spRPTransType.setAdapter(spinnerArrayAdapter);
		spRPTransType.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                transactionType = getTransactionTypeByIndex((int) spRPTransType.getSelectedItemId());
                if (oldTransactionType != transactionType) {
                    categoryID = 0;
                    btCategory.setText(CategorySrv.getCategoryNameByID(getBaseContext(), categoryID));
                    oldTransactionType = transactionType;
                }
            }

            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
		if (transactionType == Constants.TransactionTypeExpence)
			spRPTransType.setSelection(1);
		else
			spRPTransType.setSelection(0);
		spRPTransType.setEnabled(updatedID == 0);
		spRPTransType.setVisibility(View.VISIBLE);
		
		setTransdate(transDate);
		
		final EditText edAmount = (EditText)findViewById(R.id.edRtpAmount);
		edAmount.addTextChangedListener(new TextWatcher() {			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,int after) {}
			@Override
			public void afterTextChanged(Editable s) {
				if (edAmount.getText().toString().length() != 0)
					try{
						amount = Double.parseDouble(edAmount.getText().toString());
						}
					catch (NumberFormatException e) {
						DialogTools.toastDialog(RPTransactionEdit.this, R.string.msgInvalidNumber, Toast.LENGTH_SHORT);
						amount = 0d;
					}
				else amount = 0d;
			}
		});
		if (amount.compareTo(0d) != 0)
			edAmount.setText(String.valueOf(amount));	
				
		final EditText edDescription = (EditText)findViewById(R.id.edRtpDescription);
		edDescription.addTextChangedListener(new TextWatcher() {			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,int after) {}
			@Override
			public void afterTextChanged(Editable s) {
				description = edDescription.getText().toString();
			}
		});
		edDescription.setText(description);
		
		spinnerArrayAdapter = new ArrayAdapter<String>(this, R.layout.simple_spinner_item_blue, R.id.spinItem,
				getResources().getStringArray(R.array.TransferTypes));
		spinnerArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
		spRepeatType.setAdapter(spinnerArrayAdapter);
		spRepeatType.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				repeatType = (int) spRepeatType.getSelectedItemId();
				btPeriodEnd.setEnabled(arg3 != Constants.TransferType.Once.index());
				edCustomInterval.setEnabled(arg3 == Constants.TransferType.Custom.index());
			}
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		spRepeatType.setSelection(repeatType);
		
		setPeriodEnd(periodEnd);
		
		edCustomInterval.addTextChangedListener(new TextWatcher() {			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,int after) {}
			@Override
			public void afterTextChanged(Editable s) {
				if (edCustomInterval.getText().toString().length() != 0)
					try {
						customInterval = Integer.parseInt(edCustomInterval.getText().toString());
					}
					catch (NumberFormatException e) {
						DialogTools.toastDialog(RPTransactionEdit.this, R.string.msgInvalidNumber, Toast.LENGTH_SHORT);
						customInterval = 0;
					}
				else customInterval = 0;
			}
		});
		if ((customInterval != null) && (customInterval != 0)) 
			edCustomInterval.setText(String.valueOf(customInterval));

		btCurrency.setText(CurrencySrv.getCurrencyNameSignByID(getBaseContext(), currID));
		btCategory.setText(CategorySrv.getCategoryNameByID(getBaseContext(), categoryID));
	}

    void setAccountSpinnerValue(Cursor cursor, Spinner spAccount) {
        int position = 0;
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            if (DBTools.getCursorColumnValueLong(cursor, AccountTableMetaData._ID) == accountID) {
                spAccount.setSelection(position);
                break;
            }
            position++;
        }
    }

    /*void setAccountSpinnerValue() {
        Cursor cursor = this.getContentResolver().query(AccountTableMetaData.CONTENT_URI,
                new String[] {AccountTableMetaData._ID, AccountTableMetaData.NAME}, AccountTableMetaData.STATUS + " = 1 ", null, null);
        Spinner spAccount = ((Spinner) findViewById(R.id.spRtpAccount));
        setAccountSpinnerValue(cursor, spAccount);
    }*/

	@Override
	protected Dialog onCreateDialog(int id) {
		//Date date;
		switch (id) {
		case transDateDialogID:
			return new DatePickerDialog(this, new OnDateSetListener() {

				@Override
				public void onDateSet(DatePicker view, int year,
						int monthOfYear, int dayOfMonth) {
					setTransdate(new Date(year - 1900, monthOfYear, dayOfMonth));
				}
			}, transDate.getYear() + 1900, transDate.getMonth(), transDate.getDate());
		case periodEndDialogID:
			return new DatePickerDialog(this, new OnDateSetListener() {

				@Override
				public void onDateSet(DatePicker view, int year,
						int monthOfYear, int dayOfMonth) {
					setPeriodEnd(new Date(year - 1900, monthOfYear, dayOfMonth));
				}
			}, periodEnd.getYear() + 1900, periodEnd.getMonth(), periodEnd.getDate());
		}
		return null;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			/*if (requestCode == Constants.RequestAccountForTransferFrom)
			{
				Uri selectedUri = data.getData();
				Cursor cursor = this.managedQuery(selectedUri, null, null, null, null);
				cursor.moveToFirst();
				accountID = Integer.parseInt(DBTools.getCursorColumnValue(cursor, AccountTableMetaData._ID));
                setAccountSpinnerValue();
				//btAccount.setText(DBTools.getCursorColumnValue(cursor, AccountTableMetaData.NAME));
				accountCurrID = DBTools.getCursorColumnValueLong(cursor, AccountTableMetaData.CURRID);
			}
			else*/ if (requestCode == Constants.RequestCurrencyForTransfer)
			{
				Uri selectedUri = data.getData();
				Cursor cursor = this.managedQuery(selectedUri, null, null, null, null);
				cursor.moveToFirst();
				currID = Integer.parseInt(DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData._ID));
				btCurrency.setText(DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData.NAME) + 
						": " + DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData.SIGN));
			}
			else if (requestCode == Constants.RequestCalculator)
			{
				amount = data.getDoubleExtra(Constants.calculatorValue, 0d);
				((EditText)findViewById(R.id.edRtpAmount)).setText(Tools.formatDecimal(amount));
			}
			else if (requestCode == Constants.RequestCategoryForRPTransaction)
			{
				Uri selectedUri = data.getData();
				Cursor cursor = this.managedQuery(selectedUri, null, null, null, null);
				cursor.moveToFirst();
				categoryID = Integer.parseInt(DBTools.getCursorColumnValue(cursor, VCategoriesViewMetaData._ID));
				btCategory.setText(DBTools.getCursorColumnValue(cursor, VCategoriesViewMetaData.NAME));
			}
		}
	}
	
	public void setTransdate (Date inDate) {
		transDate = inDate;
		btTransDate.setText(Tools.DateToString(transDate, Constants.DateFormatUser));
	}
	
	public void setPeriodEnd (Date inDate) {
		if (inDate != null) {
			periodEnd = inDate;
			btPeriodEnd.setText(Tools.DateToString(periodEnd, Constants.DateFormatUser));
		}
		else
			btPeriodEnd.setText("");
	}
	
	private void insertRPTransaction(Context context, ContentValues values)
	{
		Uri insertedUri = context.getContentResolver().insert(TransferTableMetaData.CONTENT_URI, values);
		Cursor cursor = getContentResolver().query(insertedUri, null, null, null, null);
		cursor.moveToFirst();
		RPTransactionSrv.insertTransactionsFromRPTransaction(getBaseContext(), 
				accountID, transactionType, 
				Tools.StringToDate(values.getAsString(TransferTableMetaData.TRANSDATE), Constants.DateFormatDB), values.getAsDouble(TransferTableMetaData.AMOUNT), 
				values.getAsString(TransferTableMetaData.DESCRIPTION), 
				values.getAsInteger(TransferTableMetaData.REPEATTYPE), 
				values.containsKey(TransferTableMetaData.CUSTOMINTERVAL) ? values.getAsInteger(TransferTableMetaData.CUSTOMINTERVAL) : 0,
				values.containsKey(TransferTableMetaData.PERIODEND) ? Tools.StringToDate(values.getAsString(TransferTableMetaData.PERIODEND), Constants.DateFormatDB) : 
					Tools.StringToDate(values.getAsString(TransferTableMetaData.TRANSDATE), Constants.DateFormatDB), 
				DBTools.getCursorColumnValueLong(cursor, TransferTableMetaData._ID), 
				values.getAsLong(TransferTableMetaData.CURRENCYID), 
				values.getAsLong(TransferTableMetaData.CATEGORYID));
	}
		
	private static void updateTransactionFromRPTransaction(Context context, long oldAccountId, long newAccountId, 
			Date oldTransDate, Date newTransDate, Double oldAmount, Double newAmount, 
			String description, int oldRepeatType, int newRepeatType, int transactionType,
			Date oldPeriodEnd, Date newPeriodEnd, int oldCustomInterval, int newCustomInterval, 
			long transferID, long oldCurrID, long newCurrID, long oldCategoryID, long newCategoryID)
	{
		/*eger kohne ve yeni tip bir defelikse ve yaxud da eger baslanma ve bitme tarixleri eyniyse, tip eyniyse, interval eyniyse update kifayet eder*/
		if ((oldRepeatType == newRepeatType) && (((newTransDate.compareTo(oldTransDate) == 0) &&  
				(Tools.compareDates(oldPeriodEnd, newPeriodEnd) == 0) && (oldCustomInterval == newCustomInterval)) ||
				(newRepeatType == Constants.TransferType.Once.index())))
		{
			Cursor cursor = context.getContentResolver().query(TransactionsTableMetaData.CONTENT_URI, 
					new String[] {TransactionsTableMetaData._ID, TransactionsTableMetaData.TRANSTYPE, TransactionsTableMetaData.TRANSDATE}, 
					TransactionsTableMetaData.TRANSFERID + " = " + String.valueOf(transferID), null, null);
			for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext())
			{
				TransactionSrv.updateTransaction(context, DBTools.getCursorColumnValue(cursor, TransactionsTableMetaData._ID), 
					oldAccountId, newAccountId, oldCategoryID, newCategoryID,
					DBTools.getCursorColumnValueDate(cursor, TransactionsTableMetaData.TRANSDATE, Constants.DateFormatDB), 
					DBTools.getCursorColumnValueDate(cursor, TransactionsTableMetaData.TRANSDATE, Constants.DateFormatDB), 
					oldAmount, newAmount, transactionType, 
					context.getString(R.string.recurring) + context.getString(R.string.twopoints) + description, 
					oldCurrID, newCurrID, 0, 0, transferID, null, 0, 0);
			}
		}
		else  
		{
			TransferEdit.deleteTransactionsFromTransfer(context, transferID);
			RPTransactionSrv.insertTransactionsFromRPTransaction(context, newAccountId, transactionType, newTransDate, newAmount, description, newRepeatType, newCustomInterval, newPeriodEnd, transferID, newCurrID, newCategoryID);
		}
	}
	
	public static void generateRPTransNotification(Context context) {
		int [] reminder = new int[context.getResources().getIntArray(R.array.RPTransRemindValues).length];
		String notification = "";
		String updateIDs = "";
		Cursor cursor = context.getContentResolver().query(TransferTableMetaData.CONTENT_URI, 
				new String[] {TransferTableMetaData._ID, TransferTableMetaData.REMINDER}, 
				TransferTableMetaData.NEXTPAYMENT + " + 1 - " + TransferTableMetaData.REMINDER + 
				" = " + Tools.DateToDBString(Tools.getCurrentDate()) + " and " +
				TransferTableMetaData.NEXTPAYMENT + " is not null and " + 
				TransferTableMetaData.REMINDER + " is not null and " + 
				TransferTableMetaData.REMINDER + " > 0 and (" + 
				TransferTableMetaData.FIRSTACCOUNTID + " is null or " + 
				TransferTableMetaData.SECONDACCOUNTID + " is null) ", null, null);
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()){
			reminder[DBTools.getCursorColumnValueInt(cursor, TransferTableMetaData.REMINDER)]++;
			updateIDs += ", " + DBTools.getCursorColumnValue(cursor, TransferTableMetaData._ID);
		}
		for (int i = 1; i < reminder.length; i++) {
			if (reminder[i] > 0) {
				notification += ", " + String.valueOf(reminder[i]) + " ";
				switch (i) {
				case 1:
					notification += context.getString(R.string.msgForToday);
					break;
				case 2:
					notification += context.getString(R.string.msgAfter1Days);
					break;
				case 3:
					notification += context.getString(R.string.msgAfter2Days);
					break;
				case 4:
					notification += context.getString(R.string.msgAfter3Days);
					break;
				case 5:
					notification += context.getString(R.string.msgAfter7Days);
					break;
				default:
					break;
				}		
			}
		}
		if (notification.length() > 0) {
			notification = context.getString(R.string.msgYouHaveTransactions) + notification.substring(2);
			Intent notificationIntent = new Intent(context, TransferList.class);
			notificationIntent.setAction(Constants.ActionViewRPTransactionsByAccount);
			DialogTools.systemNotification(context, notificationIntent, 1, context.getString(R.string.app_name), 
					context.getString(R.string.menuRepeatingTransactions), notification, R.drawable.icon);
			RPTransactionSrv.updateReminders(context, updateIDs.substring(2), false);
		}
		
	}
	
	private void insertUpdateData(final ContentValues values)
	{
		if (getIntent().getAction().equals(Intent.ACTION_INSERT))
		{
			insertRPTransaction(getBaseContext(), values);
			setResult(RESULT_OK);
			finish();
		}
		if (getIntent().getAction().equals(Intent.ACTION_EDIT))
		{
			Command yesCommand = new Command() {
				public void execute() {
					updateTransactionFromRPTransaction(getBaseContext(), oldAccountID, accountID, 
							oldTransDate, transDate, oldAmount, amount, 
							values.get(TransactionsTableMetaData.DESCRIPTION).toString(), 
							oldRepeatType, Integer.parseInt(values.get(TransferTableMetaData.REPEATTYPE).toString()),
							transactionType,
							oldPeriodEnd, periodEnd, oldCustomInterval, 
							(spRepeatType.getSelectedItemId() == Constants.TransferType.Custom.index()) ? Integer.parseInt(((EditText)findViewById(R.id.edRtpCustomInterval)).getText().toString()) : 0,
							updatedID, oldCurrID, currID, oldCategoryID, categoryID);
					int repeatType = (int)spRepeatType.getSelectedItemId();
					if ((repeatType == Constants.TransferType.Once.index()) && (transDate.compareTo(Tools.getCurrentDate()) > 0))
						values.put(TransferTableMetaData.NEXTPAYMENT, Tools.DateToDBString(transDate));
					else if (repeatType != Constants.TransferType.Once.index())
					{
						Date nextDate = Tools.getNextDateFrom(repeatType, 
								transDate, 
								(spRepeatType.getSelectedItemId() == Constants.TransferType.Custom.index()) ? Integer.parseInt(((EditText)findViewById(R.id.edRtpCustomInterval)).getText().toString()) : 0, 
								Tools.getCurrentDate());
						if (nextDate.compareTo(periodEnd) <= 0)
							values.put(TransferTableMetaData.NEXTPAYMENT, Tools.DateToDBString(nextDate));
					}
					getContentResolver().update(Uri.withAppendedPath(TransferTableMetaData.CONTENT_URI, String.valueOf(updatedID)), values, null, null);								
					finish();
				}
			};
			Command noCommand = new Command() {
				public void execute() {
					int repeatType = (int)spRepeatType.getSelectedItemId();
					if ((repeatType == Constants.TransferType.Once.index()) && (transDate.compareTo(Tools.getCurrentDate()) > 0))
						values.put(TransferTableMetaData.NEXTPAYMENT, Tools.DateToDBString(transDate));
					else if (repeatType != Constants.TransferType.Once.index())
					{
						Date nextDate = Tools.getNextDateFrom(repeatType, 
								transDate, 
								(spRepeatType.getSelectedItemId() == Constants.TransferType.Custom.index()) ? Integer.parseInt(((EditText)findViewById(R.id.edRtpCustomInterval)).getText().toString()) : 0, 
								Tools.getCurrentDate());
						if (nextDate.compareTo(periodEnd) <= 0)
							values.put(TransferTableMetaData.NEXTPAYMENT, Tools.DateToDBString(nextDate));
					}
					getContentResolver().update(Uri.withAppendedPath(TransferTableMetaData.CONTENT_URI, String.valueOf(updatedID)), values, null, null);								
					finish();
				}
			};
			Command cancelCommand = new Command() {
				public void execute() {
					finish();								
				}
			};
			AlertDialog dialog = DialogTools.confirmWithCancelDialog(RPTransactionEdit.this, yesCommand, noCommand, cancelCommand, R.string.msgConfirm, R.string.msgUpdatePreviosTransactions);
			dialog.show();
			setResult(RESULT_OK);
		}		
	}

	private int getTransactionTypeByIndex(int index) {
		switch (index) {
		case 0:
			return 1;
		case 1:
			return -1;
		default:
			return 0;
		}
	}

	public static void deleteAll(final Context context) {
		Command deleteYesCommand = new Command() {
			public void execute() {
				Cursor cursor = context.getContentResolver().query(TransferTableMetaData.CONTENT_URI, null, 
						TransferTableMetaData.FIRSTACCOUNTID + " is null or " + TransferTableMetaData.SECONDACCOUNTID + " is null ", null, null);
				for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext())
				{
					TransferEdit.deleteTransactionsFromTransfer(context, DBTools.getCursorColumnValueLong(cursor, TransferTableMetaData._ID));
				}
				context.getContentResolver().delete(TransferTableMetaData.CONTENT_URI, TransferTableMetaData.FIRSTACCOUNTID + " is null or " + TransferTableMetaData.SECONDACCOUNTID + " is null ", null);
			}
		};
		Command deleteNoCommand = new Command() {
			public void execute() {
				context.getContentResolver().delete(TransferTableMetaData.CONTENT_URI, TransferTableMetaData.FIRSTACCOUNTID + " is null or " + TransferTableMetaData.SECONDACCOUNTID + " is null ", null);
			}
		};
		Command deleteCancelCommand = new Command() {
			public void execute() {
				return;
			}
		};
		AlertDialog deleteAllDialog = DialogTools.confirmWithCancelDialog(context, deleteYesCommand, deleteNoCommand, deleteCancelCommand, 
				R.string.msgConfirm, R.string.msgDeletePreviosTransactions);
		deleteAllDialog.show();
	}

	public static void deleteAllFinished(final Context context) {
		final String allValuesQuery = "(" + VTransferViewMetaData.FROMACCOUNTID + " is null or " + VTransferViewMetaData.TOACCOUNTID + " is null) and " +
				VTransferViewMetaData.ISENABLED + " = " + String.valueOf(Constants.Status.Disabled.index());
		Command deleteYesCommand = new Command() {
			public void execute() {
				Cursor cursor = context.getContentResolver().query(VTransferViewMetaData.CONTENT_URI, null, allValuesQuery, null, null);
				for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext())
				{
					TransferEdit.deleteTransactionsFromTransfer(context, DBTools.getCursorColumnValueLong(cursor, VTransferViewMetaData._ID));
					context.getContentResolver().delete(TransferTableMetaData.CONTENT_URI,
							TransferTableMetaData._ID + " = " + DBTools.getCursorColumnValue(cursor, VTransferViewMetaData._ID), null);
				}
				//context.getContentResolver().delete(TransferTableMetaData.CONTENT_URI, allValuesQuery, null);
			}
		};
		Command deleteNoCommand = new Command() {
			public void execute() {
				context.getContentResolver().delete(TransferTableMetaData.CONTENT_URI, allValuesQuery, null);
			}
		};
		Command deleteCancelCommand = new Command() {
			public void execute() {
				return;
			}
		};
		AlertDialog deleteAllDialog = DialogTools.confirmWithCancelDialog(context, deleteYesCommand, deleteNoCommand, deleteCancelCommand, 
				R.string.msgConfirm, R.string.msgDeletePreviosTransactions);
		deleteAllDialog.show();
	}

    public void myClickHandler(View target) {
    	Intent intent;
    	switch (target.getId()) {
		/*case R.id.btRtpAccount:
			intent = new Intent(getBaseContext(), AccountList.class);
			intent.setAction(Intent.ACTION_PICK);
			startActivityForResult(intent, Constants.RequestAccountForTransferFrom);
			break;*/
		case R.id.btRtpCategory:
			if (transactionType == Constants.TransactionTypeExpence) {
				intent = new Intent(getBaseContext(), CategoryListForExpense.class);
				intent.setAction(Intent.ACTION_PICK);
				startActivityForResult(intent, Constants.RequestCategoryForRPTransaction);
			}
			else if (transactionType == Constants.TransactionTypeIncome) {
				intent = new Intent(getBaseContext(), CategoryListForIncome.class);
				intent.setAction(Intent.ACTION_PICK);
				startActivityForResult(intent, Constants.RequestCategoryForRPTransaction);
			}
			break;
		case R.id.btRtpTransDate:
			showDialog(transDateDialogID);
			break;
		case R.id.btRtpPeriodEnd:
			showDialog(periodEndDialogID);
			break;
		case R.id.btRtpCurrency:
			intent = new Intent(getBaseContext(), CurrencyList.class);
			intent.setAction(Intent.ACTION_PICK);
			startActivityForResult(intent, Constants.RequestCurrencyForTransfer);
			break;	
		case R.id.btRtpOk:
			if ((amount == null) || (amount == 0))
			{
				DialogTools.toastDialog(RPTransactionEdit.this, RPTransactionEdit.this.getResources().getString(R.string.msgEnter) + " " +
						RPTransactionEdit.this.getResources().getString(R.string.amount), Toast.LENGTH_LONG);
				findViewById(R.id.edRtpAmount).requestFocus();
			}
			else if (accountID == 0)
				DialogTools.toastDialog(RPTransactionEdit.this, RPTransactionEdit.this.getResources().getString(R.string.msgChoose) + " " +
						RPTransactionEdit.this.getResources().getString(R.string.account), Toast.LENGTH_LONG);
			else if (description == null)
			{
				DialogTools.toastDialog(RPTransactionEdit.this, RPTransactionEdit.this.getResources().getString(R.string.msgChoose) + " " +
						RPTransactionEdit.this.getResources().getString(R.string.description), Toast.LENGTH_LONG);
				findViewById(R.id.edRtpDescription).requestFocus();
			}
			else if ((repeatType != Constants.TransferType.Once.index()) & (periodEnd == transDate))
				DialogTools.toastDialog(RPTransactionEdit.this, RPTransactionEdit.this.getResources().getString(R.string.msgChoose) + " " +
						RPTransactionEdit.this.getResources().getString(R.string.periodEnd), Toast.LENGTH_LONG);
			else if ((repeatType == Constants.TransferType.Custom.index()) & ((customInterval == null) || (customInterval == 0)))
			{
				DialogTools.toastDialog(RPTransactionEdit.this, RPTransactionEdit.this.getResources().getString(R.string.msgEnter) + " " +
						RPTransactionEdit.this.getResources().getString(R.string.interval) + " " +
						RPTransactionEdit.this.getResources().getString(R.string.days), Toast.LENGTH_LONG);
				edCustomInterval.requestFocus();
			}
			else 
			{
				final ContentValues values = new ContentValues();
                accountID = ((Spinner) findViewById(R.id.spRtpAccount)).getSelectedItemId();
				if (transactionType == Constants.TransactionTypeExpence)
				{
					values.put(TransferTableMetaData.FIRSTACCOUNTID, accountID);
					values.putNull(TransferTableMetaData.SECONDACCOUNTID);
				}
				else
				{
					values.putNull(TransferTableMetaData.FIRSTACCOUNTID);
					values.put(TransferTableMetaData.SECONDACCOUNTID, accountID);
				}
				values.put(TransferTableMetaData.TRANSDATE, Tools.UserDateToDBDate(btTransDate.getText().toString()));
				values.put(TransferTableMetaData.AMOUNT, Tools.formatDecimal(amount));
				values.put(TransferTableMetaData.REPEATTYPE, repeatType);
				values.put(TransferTableMetaData.DESCRIPTION, TransferEdit.truncDescriptionText(description));
				values.put(TransferTableMetaData.CURRENCYID, currID);
				values.put(TransferTableMetaData.CATEGORYID, categoryID);
				if (repeatType != Constants.TransferType.Once.index())
					values.put(TransferTableMetaData.PERIODEND, Tools.DateToDBString(periodEnd));
				else if (getIntent().getAction().equals(Intent.ACTION_EDIT))
					values.putNull(TransferTableMetaData.PERIODEND);
				if (repeatType == Constants.TransferType.Custom.index())
					values.put(TransferTableMetaData.CUSTOMINTERVAL, customInterval);
				else if (getIntent().getAction().equals(Intent.ACTION_EDIT))
					values.put(TransferTableMetaData.CUSTOMINTERVAL, 0);
				
				//eger secilmish pul vahidi accountlarin her hansi biinden ferqli olarsa ve bazada yoxdursa yeni mezenne istenilir
				accRateOK = currID == accountCurrID;
				if (!accRateOK)
				{
					StringBuilder sbRate = new StringBuilder();
					if (CurrRatesSrv.rateExists(RPTransactionEdit.this, currID, accountCurrID, transDate, sbRate, null)){
						accRate = Tools.parseDouble(sbRate.toString());
						accRateOK = true;
					}
					else 
					{
						final EditText inputText = new EditText(RPTransactionEdit.this);
						inputText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
						inputText.setText(sbRate.toString());
						accRateOK = false;
						Command rateCommand = new Command() {								
							public void execute() {
								if ((inputText != null) && (inputText.length() != 0)) {
									accRate = Tools.parseDouble(inputText.getText().toString());
									accRateOK = true;
									CurrRatesSrv.insertRate(RPTransactionEdit.this, currID, accountCurrID, accRate, transDate);
									insertUpdateData(values);
								}
								else 
									DialogTools.toastDialog(RPTransactionEdit.this, R.string.msgEnterRate, Toast.LENGTH_LONG);
							}
						};
						AlertDialog inputDialog = DialogTools.InputDialog(RPTransactionEdit.this, rateCommand, 
								RPTransactionEdit.this.getResources().getString(R.string.msgAddRateFor) + " " + 
										CurrencySrv.getCurrencySignByID(RPTransactionEdit.this, currID) + " - " +
										CurrencySrv.getCurrencySignByID(RPTransactionEdit.this, accountCurrID), inputText, R.drawable.ic_input_add);
						inputDialog.show();
						inputDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(inputText.getText().toString().trim().length() != 0);
					}
				}

				if (accRateOK)
					insertUpdateData(values);
			}		
			break;
		case R.id.btRtpCancel:
			setResult(RESULT_CANCELED);
			finish();
			break;
		case R.id.btRtpCalc:
			intent = new Intent(getBaseContext(), Calculator.class);
			if (Double.compare(amount, 0) != 0){
				Bundle bundle = new Bundle();
				bundle.putDouble(Calculator.startupAmountKey, amount);
				intent.putExtras(bundle);
			}
			intent.setAction(Intent.ACTION_PICK);
			startActivityForResult(intent, Constants.RequestCalculator);
			break;
		default:
			break;
		}
    }

    public static void updateRPTransactionsCurrencyToDefault(Context context, long currID) {
		Cursor cursor = context.getContentResolver().query(TransferTableMetaData.CONTENT_URI, null, 
				TransferTableMetaData.CURRENCYID + " = " + String.valueOf(currID) + " and (" +
				TransferTableMetaData.FIRSTACCOUNTID + " is null or " + 
				TransferTableMetaData.SECONDACCOUNTID + " is null) ", null, null);
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			long accountID; 
			int transactionType;
			if (DBTools.getCursorColumnValue(cursor, TransferTableMetaData.FIRSTACCOUNTID) == null) {
				accountID = DBTools.getCursorColumnValueLong(cursor, TransferTableMetaData.SECONDACCOUNTID);
				transactionType = Constants.TransactionTypeIncome;
			}
			else {
				accountID = DBTools.getCursorColumnValueLong(cursor, TransferTableMetaData.FIRSTACCOUNTID);
				transactionType = Constants.TransactionTypeExpence;
			}
			updateTransactionFromRPTransaction(context, accountID, accountID, 
					DBTools.getCursorColumnValueDate(cursor, TransferTableMetaData.TRANSDATE), 
					DBTools.getCursorColumnValueDate(cursor, TransferTableMetaData.TRANSDATE), 
					DBTools.getCursorColumnValueDouble(cursor, TransferTableMetaData.AMOUNT), 
					DBTools.getCursorColumnValueDouble(cursor, TransferTableMetaData.AMOUNT), 
					DBTools.getCursorColumnValue(cursor, TransferTableMetaData.DESCRIPTION), 
					DBTools.getCursorColumnValueInt(cursor, TransferTableMetaData.REPEATTYPE), 
					DBTools.getCursorColumnValueInt(cursor, TransferTableMetaData.REPEATTYPE),
					transactionType,
					DBTools.getCursorColumnValueDate(cursor, TransferTableMetaData.PERIODEND), 
					DBTools.getCursorColumnValueDate(cursor, TransferTableMetaData.PERIODEND), 
					DBTools.getCursorColumnValueInt(cursor, TransferTableMetaData.CUSTOMINTERVAL), 
					DBTools.getCursorColumnValueInt(cursor, TransferTableMetaData.CUSTOMINTERVAL),
					DBTools.getCursorColumnValueLong(cursor, TransferTableMetaData._ID), 
					currID, Constants.defaultCurrency,
					DBTools.getCursorColumnValueLong(cursor, TransferTableMetaData.CATEGORYID),
					DBTools.getCursorColumnValueLong(cursor, TransferTableMetaData.CATEGORYID));
			ContentValues values = new ContentValues();
			values.put(TransferTableMetaData.CURRENCYID, Constants.defaultCurrency);
			context.getContentResolver().update(
					Uri.withAppendedPath(TransferTableMetaData.CONTENT_URI, DBTools.getCursorColumnValue(cursor, TransferTableMetaData._ID)), 
					values, null, null);											
		}
    }
}
