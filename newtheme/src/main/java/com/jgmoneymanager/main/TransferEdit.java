package com.jgmoneymanager.main;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CurrencyTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransactionsTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransferTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VTransferViewMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.AccountTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.services.AccountSrv;
import com.jgmoneymanager.services.CurrRatesSrv;
import com.jgmoneymanager.services.CurrencySrv;
import com.jgmoneymanager.services.TransactionSrv;
import com.jgmoneymanager.services.TransferSrv;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.LocalTools;
import com.jgmoneymanager.tools.Tools;

import java.util.Date;

public class TransferEdit extends MyActivity {

	long fromAccountID;
	long oldFromAccountID = 0;
	long toAccountID;
	long oldToAccountID = 0;
	Double oldAmount;
	Double amount;
	long currID = Constants.defaultCurrency;
	long oldCurrID = Constants.defaultCurrency;
	long fromAccountCurrID = Constants.defaultCurrency;
	long oldFromAccountCurrID = Constants.defaultCurrency;
	long toAccountCurrID = Constants.defaultCurrency;
	long oldToAccountCurrID = Constants.defaultCurrency;
	int repeatType;
	int oldRepeatType;
	int customInterval;
	private Date transDate;
	private Date periodEnd;
	Date oldTransDate = transDate;
	Date oldPeriodEnd = transDate;
	int oldCustomInterval;
	String description;
	//Button btFromAccount;
	//Button btToAccount;
	Button btCurrency;
	Button btPeriodEnd;
	Button btTransDate;
	EditText edCustomInterval;
	protected double fromAccRate = 1.00d;
	protected double toAccRate = 1.00d;
	protected boolean fromAccRateOK = true;
	protected boolean toAccRateOK = true;
	String updatedID;
	Spinner spRepeatType;
	static int descriptionMaxtLength = 20;
	final int transDateDialogID = 1;
	final int periodEndDialogID = 2;

	private AdView adView;

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
		View child = inflater.inflate(R.layout.transferedit, null);
		mainLayout.addView(child, params);

		StringBuilder sbFromAccountName = new StringBuilder();
		String toAccountName = null;
		
		if (savedInstanceState != null) {
			fromAccountID = Tools.getLongFromBundle0(savedInstanceState, "fromAccountID");
			oldFromAccountID = Tools.getLongFromBundle0(savedInstanceState, "oldFromAccountID");
			toAccountID = Tools.getLongFromBundle0(savedInstanceState, "toAccountID");
			oldToAccountID = Tools.getLongFromBundle0(savedInstanceState, "oldToAccountID");
			oldAmount = Tools.getDoubleFromBundle(savedInstanceState, "oldAmount");
			amount = Tools.getDoubleFromBundle(savedInstanceState, "amount");
			currID = Tools.getLongFromBundle0(savedInstanceState, "currID");
			oldCurrID = Tools.getLongFromBundle0(savedInstanceState, "oldCurrID");
			fromAccountCurrID = Tools.getLongFromBundle0(savedInstanceState, "fromAccountCurrID");
			oldFromAccountCurrID = Tools.getLongFromBundle0(savedInstanceState, "oldFromAccountCurrID");
			toAccountCurrID = Tools.getLongFromBundle0(savedInstanceState, "toAccountCurrID");
			oldToAccountCurrID = Tools.getLongFromBundle0(savedInstanceState, "oldToAccountCurrID");
			repeatType = Tools.getIntegerFromBundle0(savedInstanceState, "repeatType");
			oldRepeatType = Tools.getIntegerFromBundle0(savedInstanceState, "oldRepeatType");
			customInterval = Tools.getIntegerFromBundle0(savedInstanceState, "customInterval");
			transDate = Tools.getDateFromBundle(savedInstanceState, "transDate");
			periodEnd = Tools.getDateFromBundle(savedInstanceState, "periodEnd");
			oldTransDate = Tools.getDateFromBundle(savedInstanceState, "oldTransDate");
			oldPeriodEnd = Tools.getDateFromBundle(savedInstanceState, "oldPeriodEnd");
			oldCustomInterval = Tools.getIntegerFromBundle0(savedInstanceState, "oldCustomInterval");
			description = Tools.getStringFromBundle(savedInstanceState, "description");
			fromAccRate = Tools.getDoubleFromBundle0(savedInstanceState, "fromAccRate");
			toAccRate = Tools.getDoubleFromBundle0(savedInstanceState, "toAccRate");
			fromAccRateOK = Tools.getBooleanFromBundle0(savedInstanceState, "fromAccRateOK");
			toAccRateOK = Tools.getBooleanFromBundle0(savedInstanceState, "toAccRateOK");
			updatedID = Tools.getStringFromBundle(savedInstanceState, "updatedID");
			
			sbFromAccountName.append(AccountSrv.getAccountNameByID(getBaseContext(), fromAccountID));
			toAccountName = AccountSrv.getAccountNameByID(getBaseContext(), toAccountID);
		}
		else {
			if (getIntent().getAction().equals(Intent.ACTION_INSERT))
			{
				transDate = Tools.getCurrentDate();
				if (getIntent().getExtras() != null)
					if (getIntent().getExtras().containsKey(Constants.paramAccountID))
					{
						fromAccountID = getIntent().getExtras().getLong(Constants.paramAccountID);
						sbFromAccountName.append(AccountSrv.getAccountNameByID(getBaseContext(), fromAccountID));
					}
					else
						fromAccountID = AccountSrv.getDefultAccountID(this, sbFromAccountName);
				else
					fromAccountID = AccountSrv.getDefultAccountID(this, sbFromAccountName);
				fromAccountCurrID = AccountSrv.getCurrencyIdByAcocuntID(this, fromAccountID);
				toAccountID = AccountSrv.getFirstNonThisAccountID(this, fromAccountID);
				transDate = Tools.getCurrentDate();
				amount = 0d;
				description = getResources().getString(R.string.transferShort) + 
						Tools.DateToString(Tools.getCurrentDateTime(), Constants.DateFormatForDescriptions);
				repeatType = Constants.TransferType.Once.index();
				periodEnd = null;
				customInterval = 0;
				currID = AccountSrv.getCurrencyIdByAcocuntID(this, fromAccountID);
			}
			else if (getIntent().getAction().equals(Intent.ACTION_EDIT))
			{
				Cursor cursor = this.managedQuery(
						getIntent().getData(), 
						new String[] {}, null, null, null);
				cursor.moveToFirst();
				updatedID = DBTools.getCursorColumnValue(cursor, VTransferViewMetaData._ID);
				fromAccountID = DBTools.getCursorColumnValueInt(cursor, VTransferViewMetaData.FROMACCOUNTID);
				oldFromAccountID = fromAccountID;
				sbFromAccountName.append(DBTools.getCursorColumnValue(cursor, VTransferViewMetaData.FROMACCOUNTNAME));
				toAccountID = DBTools.getCursorColumnValueInt(cursor, VTransferViewMetaData.TOACCOUNTID);
				oldToAccountID = toAccountID;
				toAccountName = DBTools.getCursorColumnValue(cursor, VTransferViewMetaData.TOACCOUNTNAME);
				transDate = DBTools.getCursorColumnValueDate(cursor, VTransferViewMetaData.TRANSDATE);
				oldTransDate = transDate;
				amount = DBTools.getCursorColumnValueDouble(cursor, VTransferViewMetaData.AMOUNT);
				oldAmount = amount;
				description = DBTools.getCursorColumnValue(cursor, VTransferViewMetaData.DESCRIPTION);
				repeatType = DBTools.getCursorColumnValueInt(cursor, VTransferViewMetaData.REPEATTYPE);
				oldRepeatType = repeatType;			
				if (DBTools.getCursorColumnValue(cursor, VTransferViewMetaData.PERIODEND) != null)
				{
					periodEnd = DBTools.getCursorColumnValueDate(cursor, VTransferViewMetaData.PERIODEND);
					oldPeriodEnd = periodEnd;
				}
				if (DBTools.getCursorColumnValue(cursor, VTransferViewMetaData.CUSTOMINTERVAL) != null)
				{
					customInterval = DBTools.getCursorColumnValueInt(cursor, VTransferViewMetaData.CUSTOMINTERVAL);
					oldCustomInterval = customInterval;
				}
				currID = DBTools.getCursorColumnValueLong(cursor, VTransferViewMetaData.CURRENCYID);
				oldCurrID = currID;
				fromAccountCurrID = DBTools.getCursorColumnValueLong(cursor, VTransferViewMetaData.FROMACCCURRID);
				toAccountCurrID = DBTools.getCursorColumnValueLong(cursor, VTransferViewMetaData.TOACCCURRID);
			}
		}

		// Create the adView
		try {
			/*if (!Tools.proVersionExists(this))*/ {
				MobileAds.initialize(getApplicationContext(), "ca-app-pub-5995868530154544/9373885312");
				adView = new AdView(this);
				adView.setAdSize(AdSize.SMART_BANNER);
				adView.setAdUnitId("ca-app-pub-5995868530154544/9373885312");
				RelativeLayout layout = (RelativeLayout) findViewById(R.id.TfLayoutAds);
				layout.addView(adView);
				AdRequest adRequest = new AdRequest.Builder().build();
				adView.loadAd(adRequest);
			}
		}
		catch (Exception e) {

		}
		
		reloadScreen();
	}		
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Tools.putToBundle(outState, "fromAccountID", fromAccountID);
		Tools.putToBundle(outState, "oldFromAccountID", oldFromAccountID);
		Tools.putToBundle(outState, "toAccountID", toAccountID);
		Tools.putToBundle(outState, "oldToAccountID", oldToAccountID);
		Tools.putToBundle(outState, "oldAmount", oldAmount);
		Tools.putToBundle(outState, "amount", amount);
		Tools.putToBundle(outState, "currID", currID);
		Tools.putToBundle(outState, "oldCurrID", oldCurrID);
		Tools.putToBundle(outState, "fromAccountCurrID", fromAccountCurrID);
		Tools.putToBundle(outState, "oldFromAccountCurrID", oldFromAccountCurrID);
		Tools.putToBundle(outState, "toAccountCurrID", toAccountCurrID);
		Tools.putToBundle(outState, "oldToAccountCurrID", oldToAccountCurrID);
		Tools.putToBundle(outState, "repeatType", repeatType);
		Tools.putToBundle(outState, "oldRepeatType", oldRepeatType);
		Tools.putToBundle(outState, "customInterval", customInterval);
		Tools.putToBundle(outState, "transDate", transDate);
		Tools.putToBundle(outState, "periodEnd", periodEnd);
		Tools.putToBundle(outState, "oldTransDate", oldTransDate);
		Tools.putToBundle(outState, "oldPeriodEnd", oldPeriodEnd);
		Tools.putToBundle(outState, "oldCustomInterval", oldCustomInterval);
		Tools.putToBundle(outState, "description", description);
		Tools.putToBundle(outState, "fromAccRate", fromAccRate);
		Tools.putToBundle(outState, "toAccRate", toAccRate);
		Tools.putToBundle(outState, "fromAccRateOK", fromAccRateOK);
		Tools.putToBundle(outState, "toAccRateOK", toAccRateOK);
		Tools.putToBundle(outState, "updatedID", updatedID);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	      super.onConfigurationChanged(newConfig);
	      setContentView(R.layout.transferedit);
	      reloadScreen();
	}
	
	private void reloadScreen() {
		btTransDate = (Button) findViewById(R.id.btTfTransDate);
		btPeriodEnd = (Button) findViewById(R.id.btTfPeriodEnd);
		btCurrency = (Button) findViewById(R.id.btTfCurrency);				
		spRepeatType = (Spinner)findViewById(R.id.spTfRepeat);
		edCustomInterval = (EditText) findViewById(R.id.edTfCustomInterval);

		setTransdate(transDate);
	
		final EditText edAmount = (EditText)findViewById(R.id.edTfAmount);
		edAmount.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				if (edAmount.getText().toString().length() != 0)
					try {
						amount = Double.parseDouble(edAmount.getText().toString());
					} catch (NumberFormatException e) {
						DialogTools.toastDialog(TransferEdit.this, R.string.msgInvalidNumber, Toast.LENGTH_SHORT);
						amount = 0d;
					}
				else amount = 0d;
			}
		});
		if (amount.compareTo(0d) != 0)
			edAmount.setText(Tools.formatDecimal(amount));
		
		final EditText edDescription = (EditText)findViewById(R.id.edTfDescription);
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

		Cursor cursor = this.getContentResolver().query(AccountTableMetaData.CONTENT_URI,
				new String[] {AccountTableMetaData._ID, AccountTableMetaData.NAME}, AccountTableMetaData.STATUS + " = 1 ", null, null);
		final Spinner spAccountFrom = ((Spinner) findViewById(R.id.spTfAccountFrom));
		LocalTools.fillSpinner(spAccountFrom, this, cursor, AccountTableMetaData.NAME);
		if (fromAccountID != 0) {
			Tools.setAccountSpinnerValue(cursor, spAccountFrom, fromAccountID);
		}
		spAccountFrom.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				fromAccountID = id;
				fromAccountCurrID = AccountSrv.getCurrencyIdByAcocuntID(TransferEdit.this, fromAccountID);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});

		cursor = this.getContentResolver().query(AccountTableMetaData.CONTENT_URI,
				new String[] {AccountTableMetaData._ID, AccountTableMetaData.NAME}, AccountTableMetaData.STATUS + " = 1 ", null, null);
		final Spinner spAccountTo = ((Spinner) findViewById(R.id.spTfAccountTo));
		LocalTools.fillSpinner(spAccountTo, this, cursor, AccountTableMetaData.NAME);
		if (toAccountID != 0) {
			Tools.setAccountSpinnerValue(cursor, spAccountTo, toAccountID);
		}
		else
			spAccountTo.setSelection(-1);
		spAccountTo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				toAccountID = id;
				toAccountCurrID = AccountSrv.getCurrencyIdByAcocuntID(TransferEdit.this, toAccountID);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});
		
		ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, R.layout.simple_spinner_item, R.id.spinItem,
				getResources().getStringArray(R.array.TransferTypes));
		spinnerArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
		spRepeatType.setAdapter(spinnerArrayAdapter);
		spRepeatType.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				repeatType = (int) arg3;
				btPeriodEnd.setEnabled(repeatType != Constants.TransferType.Once.index());
				findViewById(R.id.btTfPeriodEndEd).setEnabled(btPeriodEnd.isEnabled());
				edCustomInterval.setEnabled(repeatType == Constants.TransferType.Custom.index());
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
						DialogTools.toastDialog(TransferEdit.this, R.string.msgInvalidNumber, Toast.LENGTH_SHORT);
						customInterval = 0;
					}
				else customInterval = 0;
			}
		});
		if (customInterval != 0)
			edCustomInterval.setText(String.valueOf(customInterval));
		
		btCurrency.setText(CurrencySrv.getCurrencyNameSignByID(this, currID));
	}	

	@Override
	protected Dialog onCreateDialog(int id) {
		Date date;
		if (periodEnd != null)
			date = periodEnd;
		else 
			date = Tools.getCurrentDate();
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
			}, date.getYear() + 1900, date.getMonth(), date.getDate());
		}
		return null;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			if (requestCode == Constants.RequestCurrencyForTransfer)
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
				((EditText)findViewById(R.id.edTfAmount)).setText(Tools.formatDecimal(amount));
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
	}
	
	private void insertTransfer(Context context, ContentValues values)
	{
		Uri insertedUri = context.getContentResolver().insert(TransferTableMetaData.CONTENT_URI, values);
		Cursor cursor = getContentResolver().query(insertedUri, null, null, null, null);
		cursor.moveToFirst();
		TransferSrv.insertTransactionsFromTransfer(getBaseContext(), 
				values.getAsLong(TransferTableMetaData.FIRSTACCOUNTID), values.getAsLong(TransferTableMetaData.SECONDACCOUNTID), 
				Tools.StringToDate(values.getAsString(TransferTableMetaData.TRANSDATE), Constants.DateFormatDB), values.getAsDouble(TransferTableMetaData.AMOUNT), 
				values.getAsString(TransferTableMetaData.DESCRIPTION), 
				values.getAsInteger(TransferTableMetaData.REPEATTYPE), 
				values.containsKey(TransferTableMetaData.CUSTOMINTERVAL) ? values.getAsInteger(TransferTableMetaData.CUSTOMINTERVAL) : 0,
				values.containsKey(TransferTableMetaData.PERIODEND) ? Tools.StringToDate(values.getAsString(TransferTableMetaData.PERIODEND), Constants.DateFormatDB) : 
					Tools.StringToDate(values.getAsString(TransferTableMetaData.TRANSDATE), Constants.DateFormatDB), 
				DBTools.getCursorColumnValueLong(cursor, TransferTableMetaData._ID), 
				values.getAsLong(TransferTableMetaData.CURRENCYID));
	}
	
	public static void deleteTransfer(final Context context, final long rowID, boolean witConfirmQuestion)
	{
		if (rowID != 0)
		{
			if (witConfirmQuestion)
			{
				Command deleteYesCommand = new Command() {
					public void execute() {
						deleteTransactionsFromTransfer(context, rowID);
						disableTransfer(context, rowID);
					}
				};
				Command deleteNoCommand = new Command() {
					public void execute() {
						disableTransfer(context, rowID);
					}
				};
				Command deleteCancelCommand = new Command() {
					public void execute() {

					}
				};
				AlertDialog deleteAllDialog = DialogTools.confirmWithCancelDialog(context, deleteYesCommand, deleteNoCommand, deleteCancelCommand, 
						R.string.msgConfirm, R.string.msgDeletePreviosTransactions);
				deleteAllDialog.show();
			}
			else 
			{
				deleteTransactionsFromTransfer(context, rowID);									
				disableTransfer(context, rowID);
			}
		}
		else 
		{
			if (witConfirmQuestion)
			{
				Command deleteYesCommand = new Command() {
					public void execute() {
						Cursor cursor = context.getContentResolver().query(TransferTableMetaData.CONTENT_URI, null, 
								TransferTableMetaData.FIRSTACCOUNTID + " is not null and " + TransferTableMetaData.SECONDACCOUNTID + " is not null ", null, null);
						for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext())
						{
							deleteTransactionsFromTransfer(context, DBTools.getCursorColumnValueLong(cursor, TransferTableMetaData._ID));
						}
						disableTransfer(context, 0);
					}
				};
				Command deleteNoCommand = new Command() {
					public void execute() {
						disableTransfer(context, 0);
					}
				};
				Command deleteCancelCommand = new Command() {
					public void execute() {

					}
				};
				AlertDialog deleteAllDialog = DialogTools.confirmWithCancelDialog(context, deleteYesCommand, deleteNoCommand, deleteCancelCommand, 
						R.string.msgConfirm, R.string.msgDeletePreviosTransactions);
				deleteAllDialog.show();
			}
			else 
			{
				Cursor cursor = context.getContentResolver().query(TransferTableMetaData.CONTENT_URI, null, 
						TransferTableMetaData.FIRSTACCOUNTID + " is not null and " + TransferTableMetaData.SECONDACCOUNTID + " is not null ", null, null);
				for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext())
				{
					deleteTransactionsFromTransfer(context, DBTools.getCursorColumnValueLong(cursor, TransferTableMetaData._ID));
				}
				disableTransfer(context, 0);				
			}
		}
	}
	
	public static void deleteAllFinished(final Context context) {
		final Cursor cursor = context.getContentResolver().query(VTransferViewMetaData.CONTENT_URI, null,
				VTransferViewMetaData.FROMACCOUNTID + " is not null and " + VTransferViewMetaData.TOACCOUNTID + " is not null and " +
				VTransferViewMetaData.ISENABLED + " = " + String.valueOf(Constants.Status.Disabled.index()), null, null);
		Command deleteYesCommand = new Command() {
			public void execute() {
				for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
					deleteTransactionsFromTransfer(context, DBTools.getCursorColumnValueLong(cursor, TransferTableMetaData._ID));
					disableTransfer(context, DBTools.getCursorColumnValueLong(cursor, TransferTableMetaData._ID));
				}
			}
		};
		Command deleteNoCommand = new Command() {
			public void execute() {
				for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
					disableTransfer(context, DBTools.getCursorColumnValueLong(cursor, TransferTableMetaData._ID));
				}
			}
		};
		Command deleteCancelCommand = new Command() {
			public void execute() {

			}
		};
		AlertDialog deleteAllDialog = DialogTools.confirmWithCancelDialog(context, deleteYesCommand, deleteNoCommand,
				deleteCancelCommand, R.string.msgConfirm, R.string.msgDeletePreviosTransactions);
		deleteAllDialog.show();
	}
	
	public static void disableTransfer(Context context, long transferID) {
		ContentValues values = new ContentValues();
		values.put(TransferTableMetaData.STATUS, Constants.Status.Disabled.index());
		if (transferID == 0) 
			context.getContentResolver().update(TransferTableMetaData.CONTENT_URI, values, 
					TransferTableMetaData.FIRSTACCOUNTID + " is not null and " + TransferTableMetaData.SECONDACCOUNTID + " is not null ", 
					null);
		else 
			context.getContentResolver().update(TransferTableMetaData.CONTENT_URI, values, 
					TransferTableMetaData._ID + " = " + String.valueOf(transferID), null);
	}
	
 	public static void deleteTransactionsFromTransfer(Context context, long transferID)
	{
		Cursor cursor = context.getContentResolver().query(TransactionsTableMetaData.CONTENT_URI, 
				new String[] {TransactionsTableMetaData._ID}, TransactionsTableMetaData.TRANSFERID + " = " + String.valueOf(transferID), null, null);
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			TransactionSrv.deleteTransaction(context, DBTools.getCursorColumnValue(cursor, TransactionsTableMetaData._ID));
		}
	}
	
	private static void updateTransactionFromTransfer(Context context, long oldFromAccountId, long newFromAccountId, 
			long oldToAccountId, long newToAccountId, Date oldTransDate, Date newTransDate, Double newAmount,
			String description, int oldRepeatType, int newRepeatType, 
			Date oldPeriodEnd, Date newPeriodEnd, int oldCustomInterval, int newCustomInterval, long transferID, long oldCurrID, long newCurrID)
	{
		/*eger kohne ve yeni tip bir defelikse ve yaxud da eger baslanma ve bitme tarixleri eyniyse, tip eyniyse, interval eyniyse update kifayet eder*/
		if ((oldRepeatType == newRepeatType) && (((newTransDate.compareTo(oldTransDate) == 0) &&  
				(Tools.compareDates(oldPeriodEnd,newPeriodEnd) == 0) && (oldCustomInterval == newCustomInterval)) ||
				(newRepeatType == Constants.TransferType.Once.index())))
		{
			Cursor cursor = context.getContentResolver().query(TransactionsTableMetaData.CONTENT_URI, 
					new String[] {TransactionsTableMetaData._ID, TransactionsTableMetaData.TRANSTYPE, TransactionsTableMetaData.TRANSDATE}, 
					TransactionsTableMetaData.TRANSFERID + " = " + String.valueOf(transferID), null, null);
			Double oldTransactionAmount = TransactionSrv.getTransactionAmountFromTransfer(context, transferID);
			for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext())
			{
				if (DBTools.getCursorColumnValueInt(cursor, TransactionsTableMetaData.TRANSTYPE) == Constants.TransactionTypeExpence)
					TransactionSrv.updateTransaction(context, DBTools.getCursorColumnValue(cursor, TransactionsTableMetaData._ID), 
							oldFromAccountId, newFromAccountId, 0, 0, 
							DBTools.getCursorColumnValueDate(cursor, TransactionsTableMetaData.TRANSDATE, Constants.DateFormatDB), 
							Tools.AddDays(DBTools.getCursorColumnValueDate(cursor, TransactionsTableMetaData.TRANSDATE, Constants.DateFormatDB), 
								(int) Tools.getDateDifference(oldTransDate, newTransDate)), 
							oldTransactionAmount, newAmount, Constants.TransactionTypeExpence, 
							context.getString(R.string.transfer) + context.getString(R.string.twopoints) + description, 
							oldCurrID, newCurrID, 0, 0, transferID, null, 0, 0);
				else 
					TransactionSrv.updateTransaction(context, DBTools.getCursorColumnValue(cursor, TransactionsTableMetaData._ID), 
							oldToAccountId, newToAccountId, 0, 0, 
							DBTools.getCursorColumnValueDate(cursor, TransactionsTableMetaData.TRANSDATE, Constants.DateFormatDB), 
							Tools.AddDays(DBTools.getCursorColumnValueDate(cursor, TransactionsTableMetaData.TRANSDATE, Constants.DateFormatDB), 
								(int) Tools.getDateDifference(oldTransDate, newTransDate)),  
							oldTransactionAmount, newAmount, Constants.TransactionTypeIncome, 
							context.getString(R.string.transfer) + context.getString(R.string.twopoints) + description, 
							oldCurrID, newCurrID, 0, 0, transferID, null, 0, 0);
			}
		}
		else  
		{
			deleteTransactionsFromTransfer(context, transferID);
			TransferSrv.insertTransactionsFromTransfer(context, newFromAccountId, newToAccountId, newTransDate, newAmount, description, newRepeatType, newCustomInterval, newPeriodEnd, transferID, newCurrID);
		}
	}
		
	public static boolean controlRepeatedTransferForFuture(Context context, String accountID)
	{
		Cursor cursor = context.getContentResolver().query(TransferTableMetaData.CONTENT_URI, new String[] {TransferTableMetaData._ID}, 
				TransferTableMetaData.NEXTPAYMENT + " is not null and (" + 
				TransferTableMetaData.FIRSTACCOUNTID + " = " + accountID + " or " +
				TransferTableMetaData.SECONDACCOUNTID + " = " + accountID + " ) and " +
				TransferTableMetaData.STATUS + " = " + String.valueOf(Constants.Status.Enabled.index()), null, null);
		boolean result = (cursor.getCount() > 0);
		cursor.close();
		return  result;
	}
	
	/*public static void disableRepeatedTransferForFuture(Context context, String accountID)
	{
		ContentValues values = new ContentValues();
		values.putNull(TransferTableMetaData.NEXTPAYMENT);
		context.getContentResolver().update(TransferTableMetaData.CONTENT_URI, values,  
				TransferTableMetaData.NEXTPAYMENT + " is not null and (" + 
						TransferTableMetaData.FIRSTACCOUNTID + " = " + accountID + " or " +
						TransferTableMetaData.SECONDACCOUNTID + " = " + accountID + ")", null);	
	}*/
	
	private void insertUpdateData(final Context context, final ContentValues values)
	{
		if (getIntent().getAction().equals(Intent.ACTION_INSERT))
		{
			insertTransfer(context, values);
			setResult(RESULT_OK);
			finish();
		}
		if (getIntent().getAction().equals(Intent.ACTION_EDIT))
		{
			Command yesCommand = new Command() {
				public void execute() {
					updateTransactionFromTransfer(context, oldFromAccountID, fromAccountID, oldToAccountID, toAccountID, 
							oldTransDate, transDate, amount,
							values.get(TransactionsTableMetaData.DESCRIPTION).toString(), 
							oldRepeatType, Integer.parseInt(values.get(TransferTableMetaData.REPEATTYPE).toString()),
							oldPeriodEnd, periodEnd, oldCustomInterval, 
							(spRepeatType.getSelectedItemId() == Constants.TransferType.Custom.index()) ? Integer.parseInt(((EditText)findViewById(R.id.edTfCustomInterval)).getText().toString()) : 0,
							Long.parseLong(updatedID), oldCurrID, currID);
					int repeatType = (int)spRepeatType.getSelectedItemId();
					if ((repeatType == Constants.TransferType.Once.index()) && (transDate.compareTo(Tools.getCurrentDate()) > 0))
						values.put(TransferTableMetaData.NEXTPAYMENT, Tools.DateToDBString(transDate));
					else if (repeatType != Constants.TransferType.Once.index())
					{
						Date nextDate = Tools.getNextDateFrom(repeatType, 
								transDate, 
								(spRepeatType.getSelectedItemId() == Constants.TransferType.Custom.index()) ? Integer.parseInt(((EditText)findViewById(R.id.edTfCustomInterval)).getText().toString()) : 0, 
								Tools.getCurrentDate());
						if (nextDate.compareTo(periodEnd) <= 0)
							values.put(TransferTableMetaData.NEXTPAYMENT, Tools.DateToDBString(nextDate));
					}
					context.getContentResolver().update(Uri.withAppendedPath(TransferTableMetaData.CONTENT_URI, updatedID), values, null, null);								
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
								(spRepeatType.getSelectedItemId() == Constants.TransferType.Custom.index()) ? Integer.parseInt(((EditText)findViewById(R.id.edTfCustomInterval)).getText().toString()) : 0, 
								Tools.getCurrentDate());
						if (nextDate.compareTo(periodEnd) <= 0)
							values.put(TransferTableMetaData.NEXTPAYMENT, Tools.DateToDBString(nextDate));
					}
					context.getContentResolver().update(Uri.withAppendedPath(TransferTableMetaData.CONTENT_URI, updatedID), values, null, null);								
					finish();
				}
			};
			Command cancelCommand = new Command() {
				public void execute() {
					finish();								
				}
			};
			AlertDialog dialog = DialogTools.confirmWithCancelDialog(context, yesCommand, noCommand, cancelCommand, R.string.msgConfirm, R.string.msgUpdatePreviosTransactions);
			dialog.show();
			setResult(RESULT_OK);
		}		
	}
	
	public static String truncDescriptionText(String text) 
	{
		return text.substring(0, Math.min(descriptionMaxtLength, text.length()));
	}

	public void myClickHandler(View target) {
		Intent intent;
		switch (target.getId()) {
			case R.id.btTfTransDate:
				showDialog(transDateDialogID);
				break;
			case R.id.btTfPeriodEnd:
				showDialog(periodEndDialogID);
				break;
			case R.id.btTfCurrency:
				intent = new Intent(getBaseContext(), CurrencyList.class);
				intent.setAction(Intent.ACTION_PICK);
				startActivityForResult(intent, Constants.RequestCurrencyForTransfer);
				break;
			case R.id.btTfOk:
				try {
					amount = Tools.stringToDouble(TransferEdit.this, ((EditText) findViewById(R.id.edTfAmount)).getText().toString().length() == 0 ? "0" : ((EditText) findViewById(R.id.edTfAmount)).getText().toString(), false);
				} catch (NumberFormatException e) {
					DialogTools.toastDialog(TransferEdit.this, TransferEdit.this.getResources().getString(R.string.msgEnter) + " " +
							TransferEdit.this.getResources().getString(R.string.amount), Toast.LENGTH_LONG);
					findViewById(R.id.edTfAmount).requestFocus();
					return;
				}
				if ((amount == null) || (amount == 0)) {
					DialogTools.toastDialog(TransferEdit.this, TransferEdit.this.getResources().getString(R.string.msgEnter) + " " +
							TransferEdit.this.getResources().getString(R.string.amount), Toast.LENGTH_LONG);
					findViewById(R.id.edTfAmount).requestFocus();
				} else if (fromAccountID == 0)
					DialogTools.toastDialog(TransferEdit.this, TransferEdit.this.getResources().getString(R.string.msgChoose) + " " +
							TransferEdit.this.getResources().getString(R.string.from) + " " +
							TransferEdit.this.getResources().getString(R.string.account), Toast.LENGTH_LONG);
				else if (toAccountID == 0)
					DialogTools.toastDialog(TransferEdit.this, TransferEdit.this.getResources().getString(R.string.msgChoose) + " " +
							TransferEdit.this.getResources().getString(R.string.to) + " " +
							TransferEdit.this.getResources().getString(R.string.account), Toast.LENGTH_LONG);
				else if (((EditText) findViewById(R.id.edTfDescription)).getText().toString().trim().length() == 0) {
					DialogTools.toastDialog(TransferEdit.this, TransferEdit.this.getResources().getString(R.string.msgChoose) + " " +
							TransferEdit.this.getResources().getString(R.string.description), Toast.LENGTH_LONG);
					findViewById(R.id.edTfDescription).requestFocus();
				} else if ((spRepeatType.getSelectedItemId() != Constants.TransferType.Once.index()) &
						(btPeriodEnd.getText().toString().length() == 0))
					DialogTools.toastDialog(TransferEdit.this, TransferEdit.this.getResources().getString(R.string.msgChoose) + " " +
							TransferEdit.this.getResources().getString(R.string.periodEnd), Toast.LENGTH_LONG);
				else if ((spRepeatType.getSelectedItemId() == Constants.TransferType.Custom.index()) &
						(edCustomInterval.getText().toString().length() == 0)) {
					DialogTools.toastDialog(TransferEdit.this, TransferEdit.this.getResources().getString(R.string.msgEnter) + " " +
							TransferEdit.this.getResources().getString(R.string.interval) + " " +
							TransferEdit.this.getResources().getString(R.string.days), Toast.LENGTH_LONG);
					edCustomInterval.requestFocus();
				} else if (fromAccountID == toAccountID)
					DialogTools.toastDialog(TransferEdit.this, TransferEdit.this.getResources().getString(R.string.msgChooseDifferentAccounts), Toast.LENGTH_LONG);
				else {
					final ContentValues values = new ContentValues();
					values.put(TransferTableMetaData.FIRSTACCOUNTID, fromAccountID);
					values.put(TransferTableMetaData.SECONDACCOUNTID, toAccountID);
					values.put(TransferTableMetaData.TRANSDATE, Tools.UserDateToDBDate(btTransDate.getText().toString()));
					values.put(TransferTableMetaData.AMOUNT, Tools.formatDecimal(amount));
					values.put(TransferTableMetaData.REPEATTYPE, spRepeatType.getSelectedItemId());
					values.put(TransferTableMetaData.DESCRIPTION, truncDescriptionText(((EditText) findViewById(R.id.edTfDescription)).getText().toString()));
					values.put(TransferTableMetaData.CURRENCYID, currID);
					if (spRepeatType.getSelectedItemId() != Constants.TransferType.Once.index())
						values.put(TransferTableMetaData.PERIODEND, Tools.UserDateToDBDate(btPeriodEnd.getText().toString()));
					else if (getIntent().getAction().equals(Intent.ACTION_EDIT))
						values.putNull(TransferTableMetaData.PERIODEND);
					if (spRepeatType.getSelectedItemId() == Constants.TransferType.Custom.index())
						values.put(TransferTableMetaData.CUSTOMINTERVAL, ((EditText) findViewById(R.id.edTfCustomInterval)).getText().toString());
					else if (getIntent().getAction().equals(Intent.ACTION_EDIT))
						values.putNull(TransferTableMetaData.CUSTOMINTERVAL);

					//eger secilmish pul vahidi accountlarin her hansi biinden ferqli olarsa ve bazada yoxdursa yeni mezenne istenilir
					fromAccRateOK = currID == fromAccountCurrID;
					toAccRateOK = currID == toAccountCurrID;
					if (!fromAccRateOK) {
						StringBuilder sbRate = new StringBuilder();
						if (CurrRatesSrv.rateExists(TransferEdit.this, currID, fromAccountCurrID, transDate, sbRate, null)) {
							fromAccRate = Tools.parseDouble(sbRate.toString());
							fromAccRateOK = true;
						} else {
							final EditText inputText = new EditText(TransferEdit.this);
							inputText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
							inputText.setText(sbRate.toString());
							fromAccRateOK = false;
							Command rateCommand = new Command() {
								public void execute() {
									if ((inputText != null) && (inputText.length() != 0)) {
										fromAccRate = Tools.parseDouble(inputText.getText().toString());
										fromAccRateOK = true;
										CurrRatesSrv.insertRate(TransferEdit.this, currID, fromAccountCurrID, fromAccRate, transDate);
										if (toAccRateOK)
											insertUpdateData(TransferEdit.this, values);
									} else
										DialogTools.toastDialog(TransferEdit.this, R.string.msgEnterRate, Toast.LENGTH_LONG);
								}
							};
							AlertDialog inputDialog = DialogTools.InputDialog(TransferEdit.this, rateCommand,
									TransferEdit.this.getResources().getString(R.string.msgAddRateFor) + " " +
											CurrencySrv.getCurrencySignByID(TransferEdit.this, currID) + " - " +
											CurrencySrv.getCurrencySignByID(TransferEdit.this, fromAccountCurrID), inputText, R.drawable.ic_input_add);
							inputDialog.show();
							inputDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(inputText.getText().toString().trim().length() != 0);
						}
					}
					if (!toAccRateOK) {

						StringBuilder sbRate = new StringBuilder();
						if (CurrRatesSrv.rateExists(TransferEdit.this, currID, toAccountCurrID, transDate, sbRate, null)) {
							toAccRate = Tools.parseDouble(sbRate.toString());
							toAccRateOK = true;
						} else {
							final EditText inputText = new EditText(TransferEdit.this);
							inputText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
							inputText.setText(sbRate.toString());
							toAccRateOK = false;
							Command rateCommand = new Command() {
								public void execute() {
									if ((inputText != null) && (inputText.length() != 0)) {
										toAccRate = Tools.parseDouble(inputText.getText().toString());
										toAccRateOK = true;
										CurrRatesSrv.insertRate(TransferEdit.this, currID, toAccountCurrID, toAccRate, transDate);
										if (fromAccRateOK) {
											insertUpdateData(TransferEdit.this, values);
											toAccRateOK = false;
										}
									} else
										DialogTools.toastDialog(TransferEdit.this, R.string.msgEnterRate, Toast.LENGTH_LONG);
								}
							};
							AlertDialog inputDialog = DialogTools.InputDialog(TransferEdit.this, rateCommand,
									TransferEdit.this.getResources().getString(R.string.msgAddRateFor) + " " +
											CurrencySrv.getCurrencySignByID(TransferEdit.this, currID) + " - " +
											CurrencySrv.getCurrencySignByID(TransferEdit.this, toAccountCurrID), inputText, R.drawable.ic_input_add);
							inputDialog.show();
							inputDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(inputText.getText().toString().trim().length() != 0);
						}
					}

					if (fromAccRateOK && toAccRateOK)
						insertUpdateData(TransferEdit.this, values);
				}
				break;
			case R.id.btTfCancel:
				setResult(RESULT_CANCELED);
				finish();
				break;
			case R.id.btTfCalc:
				intent = new Intent(getBaseContext(), Calculator.class);
				if (Double.compare(amount, 0) != 0) {
					Bundle bundle = new Bundle();
					bundle.putDouble(Calculator.startupAmountKey, amount);
					intent.putExtras(bundle);
				}
				intent.setAction(Intent.ACTION_PICK);
				if(getCurrentFocus()!=null) {
					InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
					inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
				}
				startActivityForResult(intent, Constants.RequestCalculator);
				break;
			case R.id.btTfAccountFromEd:
				findViewById(R.id.spTfAccountFrom).performClick();
				break;
			case R.id.btTfAccountToEd:
				findViewById(R.id.spTfAccountTo).performClick();
				break;
			case R.id.btTfTransDateEd:
				findViewById(R.id.btTfTransDate).performClick();
				break;
			case R.id.btTfDescriptionEd:
				findViewById(R.id.edTfDescription).requestFocus();
				break;
			case R.id.btTfRepeatEd:
				findViewById(R.id.spTfRepeat).performClick();
				break;
			case R.id.btTfPeriodEndEd:
				findViewById(R.id.btTfPeriodEnd).performClick();
				break;
			case R.id.btTfCustomIntervalEd:
				findViewById(R.id.edTfCustomInterval).requestFocus();
				break;
			case R.id.btTfCurrencyEd:
				findViewById(R.id.btTfCurrency).performClick();
				break;
			default:
				break;
		}
	}

	public static void updateTransfersCurrencyToDefault(Context context, long currID) {
		Cursor cursor = context.getContentResolver().query(TransferTableMetaData.CONTENT_URI, null, 
				TransferTableMetaData.CURRENCYID + " = " + String.valueOf(currID) + " and " +
				TransferTableMetaData.FIRSTACCOUNTID + " is not null and " + 
				TransferTableMetaData.SECONDACCOUNTID + " is not null ", null, null);
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			updateTransactionFromTransfer(context, 
					DBTools.getCursorColumnValueLong(cursor, TransferTableMetaData.FIRSTACCOUNTID), 
					DBTools.getCursorColumnValueLong(cursor, TransferTableMetaData.FIRSTACCOUNTID), 
					DBTools.getCursorColumnValueLong(cursor, TransferTableMetaData.SECONDACCOUNTID), 
					DBTools.getCursorColumnValueLong(cursor, TransferTableMetaData.SECONDACCOUNTID), 
					DBTools.getCursorColumnValueDate(cursor, TransferTableMetaData.TRANSDATE), 
					DBTools.getCursorColumnValueDate(cursor, TransferTableMetaData.TRANSDATE),
					DBTools.getCursorColumnValueDouble(cursor, TransferTableMetaData.AMOUNT), 
					DBTools.getCursorColumnValue(cursor, TransferTableMetaData.DESCRIPTION), 
					DBTools.getCursorColumnValueInt(cursor, TransferTableMetaData.REPEATTYPE), 
					DBTools.getCursorColumnValueInt(cursor, TransferTableMetaData.REPEATTYPE),
					DBTools.getCursorColumnValueDate(cursor, TransferTableMetaData.PERIODEND), 
					DBTools.getCursorColumnValueDate(cursor, TransferTableMetaData.PERIODEND), 
					DBTools.getCursorColumnValueInt(cursor, TransferTableMetaData.CUSTOMINTERVAL), 
					DBTools.getCursorColumnValueInt(cursor, TransferTableMetaData.CUSTOMINTERVAL),
					DBTools.getCursorColumnValueLong(cursor, TransferTableMetaData._ID), 
					currID, Constants.defaultCurrency);
			ContentValues values = new ContentValues();
			values.put(TransferTableMetaData.CURRENCYID, Constants.defaultCurrency);
			context.getContentResolver().update(
					Uri.withAppendedPath(TransferTableMetaData.CONTENT_URI, DBTools.getCursorColumnValue(cursor, TransferTableMetaData._ID)), 
					values, null, null);											
		}
	}

	public static void updateTransferReminder(Context context, long rowID, long reminder) {
		ContentValues values = new ContentValues();
		values.put(TransferTableMetaData.REMINDER, reminder);
		context.getContentResolver().update(TransferTableMetaData.CONTENT_URI, values, 
				TransferTableMetaData._ID + " = " + rowID, null);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		try {
			super.onDestroy();
			if (adView != null)
				adView.destroy();
		} catch (Exception ex) {

		}
	}
}
