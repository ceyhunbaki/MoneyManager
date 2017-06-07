package com.jgmoneymanager.main;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CurrencyTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.DebtsTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.services.CurrencySrv;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import java.util.Date;

public class DebtsEdit extends MyActivity{
	
	long editedID = 0;
	long currencyID = 0;
	Date transDate;
	Date returnDate;
	Double amount = 0d;
	String comment;
	boolean remindMe;
	boolean isGiven;

	final int transDateDialogID = 1;
	final int returnDateDialogID = 2;
	static final String pIsGiven = "isGiven";
	
	EditText edAmount;
	Button btTransDate;
	Button btReturnDate;
	Button btCurrency;
	EditText edDescription;
	CheckBox cbRemindMe;

	private AdView adView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
        Tools.loadSettings(this);
        
		setContentView(R.layout.main_activity);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

		LinearLayout mainLayout = (LinearLayout) findViewById(R.id.content_main_layout);
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		View child = inflater.inflate(R.layout.debtsedit, null);
		mainLayout.addView(child, params);

		edAmount = (EditText) findViewById(R.id.edDebAmount);
		btTransDate = (Button) findViewById(R.id.btDebTransDate);
		btReturnDate = (Button) findViewById(R.id.btDebReturnDate);
		btCurrency = (Button) findViewById(R.id.btDebCurrency);
		edDescription = (EditText) findViewById(R.id.edDebDescription);
		cbRemindMe = (CheckBox) findViewById(R.id.cbDebRemind);
		
		if (savedInstanceState != null) {
			getValuesFromBundle(savedInstanceState);
		}
		else {
			Bundle bundle = getIntent().getExtras();
			if (getIntent().getAction().equals(Intent.ACTION_INSERT)) {
				currencyID = CurrencySrv.getDefaultCurrencyID(getBaseContext());
				transDate = Tools.getCurrentDate();
				isGiven = bundle.getInt(pIsGiven) == 1;
			}
			else {
				try {
					Uri selectedUri = getIntent().getData();
					Cursor cursor = getContentResolver().query(selectedUri, null, null, null, null);
					cursor.moveToFirst();
					editedID = DBTools.getCursorColumnValueInt(cursor, DebtsTableMetaData._ID);
					currencyID = DBTools.getCursorColumnValueInt(cursor, DebtsTableMetaData.CURRENCY_ID);
					transDate = DBTools.getCursorColumnValueDate(cursor, DebtsTableMetaData.TRANSDATE);
					returnDate = DBTools.getCursorColumnValueDate(cursor, DebtsTableMetaData.BACKDATE);
					amount = DBTools.getCursorColumnValueDouble(cursor, DebtsTableMetaData.AMOUNT);
					comment = DBTools.getCursorColumnValue(cursor, DebtsTableMetaData.DESCRIPTION);
					remindMe = DBTools.getCursorColumnValueInt(cursor, DebtsTableMetaData.REMINDME) == 1;
					isGiven = DBTools.getCursorColumnValueInt(cursor, DebtsTableMetaData.ISGIVEN) == 1;
					cursor.close();
				}
				catch (Exception e) {
					Tracker myTracker = EasyTracker.getInstance(getBaseContext());
					myTracker.set(Fields.SCREEN_NAME, "DebtEdit- On getting values");
					myTracker.send(MapBuilder.createAppView().build());
				}
			}
		}

		// Create the adView
		try {
			if (!Tools.proVersionExists(this) /*&& (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE)*/) {
//				adView = new AdView(this, AdSize.BANNER, "ca-app-pub-5995868530154544/5047443716");
//				RelativeLayout layout = (RelativeLayout) findViewById(R.id.DebLayoutAds);
//				// Add the adView to it
//				RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
//				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
//				layout.addView(adView, layoutParams); // Initiate a generic request to load it with an ad
//				AdRequest adRequest = new AdRequest();
//				adView.loadAd(adRequest);
				MobileAds.initialize(getApplicationContext(), "ca-app-pub-5995868530154544/5047443716");
				adView = new AdView(this);
				adView.setAdSize(AdSize.SMART_BANNER);
				adView.setAdUnitId("ca-app-pub-5995868530154544/5047443716");
				RelativeLayout layout = (RelativeLayout) findViewById(R.id.DebLayoutAds);
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
	protected void onDestroy() {
		try {
			super.onDestroy();
			if (adView != null)
				adView.destroy();
		} catch (Exception ex) {

		}
	}

	void getValuesFromBundle(Bundle savedInstanceState) {
		editedID = Tools.getLongFromBundle0(savedInstanceState, "editedID");
		currencyID = Tools.getLongFromBundle0(savedInstanceState, "currencyID");
		transDate = Tools.getDateFromBundle(savedInstanceState, "transDate");
		returnDate = Tools.getDateFromBundle(savedInstanceState, "returnDate");
		amount = Tools.getDoubleFromBundle(savedInstanceState, "amount");
		comment = Tools.getStringFromBundle(savedInstanceState, "comment");
		remindMe = Tools.getBooleanFromBundle0(savedInstanceState, "remindMe");
		isGiven = Tools.getBooleanFromBundle0(savedInstanceState, "isGiven");
	}
	
	void reloadScreen() {
		edAmount.addTextChangedListener(new TextWatcher() {			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,int after) {}
			@Override
			public void afterTextChanged(Editable s) {
				if (edAmount.getText().toString().length() != 0)
					try {
						amount = Double.parseDouble(edAmount.getText().toString());
					}
					catch (NumberFormatException e) {
						DialogTools.toastDialog(DebtsEdit.this, R.string.msgInvalidNumber, Toast.LENGTH_SHORT);
						amount = 0d;
					}
				else amount = 0d;
			}
		});
		
		edDescription.addTextChangedListener(new TextWatcher() {			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,int after) {}
			@Override
			public void afterTextChanged(Editable s) {
				comment = edDescription.getText().toString();
			}
		});
		
		cbRemindMe.setOnCheckedChangeListener(new OnCheckedChangeListener() {			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				remindMe = isChecked;
			}
		});

		if (amount.compareTo(0d) != 0)
			edAmount.setText(Tools.formatDecimal(amount));
		if (currencyID != 0)
			btCurrency.setText(CurrencySrv.getCurrencyNameSignByID(getBaseContext(), currencyID));
		if (transDate != null) 
			btTransDate.setText(Tools.DateToString(transDate));
		if (returnDate != null) 
			btReturnDate.setText(Tools.DateToString(returnDate));
		if (comment != null)
			edDescription.setText(comment);
		cbRemindMe.setChecked(remindMe);
		
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Tools.putToBundle(outState, "editedID", editedID);
		Tools.putToBundle(outState, "currencyID", currencyID);
		Tools.putToBundle(outState, "transDate", transDate);
		Tools.putToBundle(outState, "returnDate", returnDate);
		Tools.putToBundle(outState, "amount", amount);
		Tools.putToBundle(outState, "comment", comment);
		Tools.putToBundle(outState, "remindMe", remindMe);
		Tools.putToBundle(outState, "isGiven", isGiven);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case transDateDialogID: 
			return new DatePickerDialog(this, new OnDateSetListener() {
				@Override
				public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
					setDateButton(new Date(year - 1900, monthOfYear, dayOfMonth), R.id.btDebTransDate);
				}
			}, transDate.getYear() + 1900, transDate.getMonth(), transDate.getDate());
		case returnDateDialogID: 
			Date tempDate = transDate;
			if (returnDate != null) 
				tempDate = returnDate;
			return new DatePickerDialog(this, new OnDateSetListener() {
				@Override
				public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
					setDateButton(new Date(year - 1900, monthOfYear, dayOfMonth), R.id.btDebReturnDate);
				}
			}, tempDate.getYear() + 1900, tempDate.getMonth(), tempDate.getDate());
		default:  
			return null;
		}
	}
	
	private void setDateButton (Date inDate, int buttonID) {
		if (buttonID == R.id.btDebTransDate) 
			transDate = inDate;
		else if (buttonID == R.id.btDebReturnDate)
			returnDate = inDate;
		((Button)findViewById(buttonID)).setText(Tools.DateToString(inDate, Constants.DateFormatUser));
	}

	public void myClickHandler(View target) {
		Intent intent;
		switch (target.getId()) {
			case R.id.btDebTransDateEd:
				(findViewById(R.id.btDebTransDate)).performClick();
				break;
			case R.id.btDebCurrencyEd:
				(findViewById(R.id.btDebCurrency)).performClick();
				break;
			case R.id.btDebDescriptionEd:
				(findViewById(R.id.edDebDescription)).requestFocus();
				break;
			case R.id.btDebReturnDateEd:
				(findViewById(R.id.btDebReturnDate)).performClick();
				break;
			case R.id.btDebCancel:
				setResult(RESULT_CANCELED);
				finish();
				break;
			case R.id.btDebOk:
				if ((amount == null) || (amount == 0))
					DialogTools.toastDialog(getBaseContext(), DebtsEdit.this.getResources().getString(R.string.msgEnter) + " " +
							DebtsEdit.this.getResources().getString(R.string.amount), Toast.LENGTH_LONG);
				else if (transDate == null)
					DialogTools.toastDialog(getBaseContext(), DebtsEdit.this.getResources().getString(R.string.msgEnter) + " " +
							DebtsEdit.this.getResources().getString(R.string.date), Toast.LENGTH_LONG);
				else if (remindMe && (returnDate == null))
					DialogTools.toastDialog(getBaseContext(), DebtsEdit.this.getResources().getString(R.string.msgEnter) + " " +
							DebtsEdit.this.getResources().getString(R.string.returnDate), Toast.LENGTH_LONG);
				else if ((comment == null) || (comment.length() == 0))
					DialogTools.toastDialog(getBaseContext(), DebtsEdit.this.getResources().getString(R.string.msgEnter) + " " +
							DebtsEdit.this.getResources().getString(R.string.description), Toast.LENGTH_LONG);
				else {
					if (getIntent().getAction().equals(Intent.ACTION_INSERT))
						insertValues(getBaseContext(), isGiven, transDate, returnDate, amount, comment, remindMe, currencyID);
					else if (getIntent().getAction().equals(Intent.ACTION_EDIT))
						updateValues(getBaseContext(), isGiven, transDate, returnDate, amount, comment, remindMe, editedID, currencyID);
					setResult(RESULT_OK);
					finish();
				}
				break;
			case R.id.btDebTransDate:
				showDialog(transDateDialogID);
				break;
			case R.id.btDebReturnDate:
				showDialog(returnDateDialogID);
				break;
			case R.id.btDebCalc:
				intent = new Intent(getBaseContext(), Calculator.class);
				if (Double.compare(DebtsEdit.this.amount, 0) != 0) {
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
			case R.id.btDebCurrency:
				intent = new Intent(getBaseContext(), CurrencyList.class);
				intent.setAction(Intent.ACTION_PICK);
				startActivityForResult(intent, Constants.RequestCurrencyForTransaction);
				break;
			case R.id.btDebDelRetDate:
				//isGiven = false;
				setDateButton(null, R.id.btDebReturnDate);
				break;
			default:
				break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			if (requestCode == Constants.RequestCalculator)
			{
				amount = data.getDoubleExtra(Constants.calculatorValue, 0d);
				edAmount.setText(Tools.formatDecimal(amount));
			}
			else if (requestCode == Constants.RequestCurrencyForTransaction)
			{
				Uri selectedUri = data.getData();
				Cursor cursor = getContentResolver().query(selectedUri, null, null, null, null);
				cursor.moveToFirst();
				currencyID = Integer.parseInt(DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData._ID));
				btCurrency.setText(DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData.NAME) + 
					": " + DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData.SIGN));
			}
		}
	}

	void insertValues(Context context, boolean isGiven, Date transDate, Date returnDate, 
			Double amount, String comment, boolean remindMe, long currencyID) {
		ContentValues values = new ContentValues();
		values.put(DebtsTableMetaData.ISGIVEN, isGiven?1:0);
		values.put(DebtsTableMetaData.TRANSDATE, Tools.DateToDBString(transDate));
		values.put(DebtsTableMetaData.AMOUNT, Tools.formatDecimal(amount));
		if (comment != null)
			values.put(DebtsTableMetaData.DESCRIPTION, comment);
		if (returnDate != null)
			values.put(DebtsTableMetaData.BACKDATE, Tools.DateToDBString(returnDate));
		values.put(DebtsTableMetaData.REMINDME, remindMe?1:0);
		values.put(DebtsTableMetaData.CURRENCY_ID, currencyID);
		values.put(DebtsTableMetaData.STATUS, Constants.Status.Enabled.index());
		
		context.getContentResolver().insert(DebtsTableMetaData.CONTENT_URI, values);
	}

	void updateValues(Context context, boolean isGiven, Date transDate, Date returnDate, 
			Double amount, String comment, boolean remindMe, long editedID, long currencyID) {
		ContentValues values = new ContentValues();
		values.put(DebtsTableMetaData.ISGIVEN, isGiven?1:0);
		values.put(DebtsTableMetaData.TRANSDATE, Tools.DateToDBString(transDate));
		values.put(DebtsTableMetaData.AMOUNT, Tools.formatDecimal(amount));
		if (comment != null)
			values.put(DebtsTableMetaData.DESCRIPTION, comment);
		if (returnDate != null)
			values.put(DebtsTableMetaData.BACKDATE, Tools.DateToDBString(returnDate));
		else
			values.putNull(DebtsTableMetaData.BACKDATE);
		values.put(DebtsTableMetaData.REMINDME, remindMe?1:0);
		values.put(DebtsTableMetaData.CURRENCY_ID, currencyID);
		values.put(DebtsTableMetaData.STATUS, Constants.Status.Enabled.index());
		
		context.getContentResolver().update(DebtsTableMetaData.CONTENT_URI, values, 
				DebtsTableMetaData._ID + " = " + String.valueOf(editedID), null);
	}
}
