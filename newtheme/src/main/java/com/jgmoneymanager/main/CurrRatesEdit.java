package com.jgmoneymanager.main;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;
import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CurrRatesTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CurrencyTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.services.CurrRatesSrv;
import com.jgmoneymanager.services.CurrencySrv;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import java.util.Date;

public class CurrRatesEdit extends MyActivity{

	final int rateDateDialogID = 1;
	Date rateDate;
	long fromCurrencyID = 0;
	long toCurrencyID = 0;
	Double rate = 0d;
	long updatedID = 0;
	Button btRateDate;
	Button btFromCurrency;
	Button btToCurrency;

	public static final String fromCurrencyIDTag = "fromCurrencyID";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

		LinearLayout mainLayout = (LinearLayout) findViewById(R.id.content_main_layout);
		LayoutInflater inflater = (LayoutInflater)      this.getSystemService(LAYOUT_INFLATER_SERVICE);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		View child = inflater.inflate(R.layout.currratesedit, null);
		mainLayout.addView(child, params);

		if (savedInstanceState != null) {
			rateDate = Tools.getDateFromBundle(savedInstanceState, "rateDate");
			fromCurrencyID = Tools.getLongFromBundle0(savedInstanceState, "fromCurrencyID");
			toCurrencyID = Tools.getLongFromBundle0(savedInstanceState, "toCurrencyID");
			rate = Tools.getDoubleFromBundle(savedInstanceState, "rate");
			updatedID = Tools.getLongFromBundle0(savedInstanceState, "updatedID");
		}
		else {
			if (getIntent().getAction().equals(Intent.ACTION_INSERT)) {
				Bundle bundle = getIntent().getExtras();
				rateDate = Tools.getCurrentDate();
				if ((bundle != null) && bundle.containsKey(fromCurrencyIDTag))
					fromCurrencyID = bundle.getLong(fromCurrencyIDTag);
				else
					fromCurrencyID = 0;
				rate = 0d;
			}
			else if (getIntent().getAction().equals(Intent.ACTION_EDIT)){
				Cursor cursor = this.managedQuery(getIntent().getData(), null, null, null, null);
				if (cursor.moveToFirst()) {
					updatedID = DBTools.getCursorColumnValueLong(cursor, CurrRatesTableMetaData._ID);
					rateDate = DBTools.getCursorColumnValueDate(cursor, CurrRatesTableMetaData.RATEDATE);
					fromCurrencyID = DBTools.getCursorColumnValueLong(cursor, CurrRatesTableMetaData.FIRSTCURRID);
					toCurrencyID = DBTools.getCursorColumnValueLong(cursor, CurrRatesTableMetaData.SECONDCURRID);
					rate = DBTools.getCursorColumnValueDouble(cursor, CurrRatesTableMetaData.VALUE);			
				}
				else {
					updatedID = 0;
					rateDate = Tools.getCurrentDate();
					fromCurrencyID = 0;
					toCurrencyID = 0;
					rate = 0d;				
				}
			}
		}
		reloadScreen();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Tools.putToBundle(outState, "rateDate", Tools.DateToString(rateDate));
		Tools.putToBundle(outState, "fromCurrencyID", fromCurrencyID);
		Tools.putToBundle(outState, "toCurrencyID", toCurrencyID);
		Tools.putToBundle(outState, "rate", rate);
		Tools.putToBundle(outState, "updatedID", updatedID);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setContentView(R.layout.currratesedit);
		reloadScreen();
	}

	public void reloadScreen() {
		btRateDate = (Button) findViewById(R.id.btCurrRatesDate);
		btFromCurrency = (Button) findViewById(R.id.btCurrRatesFrom);
		btToCurrency = (Button) findViewById(R.id.btCurrRatesTo);
		
		btRateDate.setText(Tools.DateToString(rateDate, Constants.DateFormatUser));
		btFromCurrency.setText(CurrencySrv.getCurrencyNameSignByID(getBaseContext(), fromCurrencyID));
		btToCurrency.setText(CurrencySrv.getCurrencyNameSignByID(getBaseContext(), toCurrencyID));

		btRateDate.setEnabled(getIntent().getAction().equals(Intent.ACTION_INSERT));
		findViewById(R.id.btCurrRatesDateEd).setEnabled(btRateDate.isEnabled());
		final EditText edRate = (EditText) findViewById(R.id.edCurrRatesRate);
		edRate.addTextChangedListener(new TextWatcher() {			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,int after) {}
			@Override
			public void afterTextChanged(Editable s) {
				if (edRate.getText().toString().length() != 0)
					try{
						rate = Double.parseDouble(edRate.getText().toString());
						}
					catch (NumberFormatException e) {
						DialogTools.toastDialog(CurrRatesEdit.this, R.string.msgInvalidNumber, Toast.LENGTH_SHORT);
						rate = 0d;
					}
					catch (IllegalArgumentException e) {
						Tracker myTracker = EasyTracker.getInstance(getBaseContext());
						myTracker.set(Fields.SCREEN_NAME, "CurrRatesEdit- Error1");
						myTracker.send(MapBuilder.createAppView().build());
					}
				else rate = 0d;
			}
		});
		edRate.setText(Tools.formatDecimal(rate, Constants.rateDecimalCount));
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case rateDateDialogID:
			return new DatePickerDialog(this, new OnDateSetListener() {
				@Override
				public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
					try {
						rateDate = new Date(year - 1900, monthOfYear, dayOfMonth);
						btRateDate.setText(Tools.DateToString(rateDate, Constants.DateFormatUser));
					}
					catch (IllegalArgumentException e) {
						Tracker myTracker = EasyTracker.getInstance(getBaseContext());
						myTracker.set(Fields.SCREEN_NAME, "CurrRatesEdit- Error2");
						myTracker.send(MapBuilder.createAppView().build());
					}
				}
			}, rateDate.getYear() + 1900, rateDate.getMonth(), rateDate.getDate());
		}
		return null;
	}
	
	public void myClickHandler(View target) {
		Intent intent;
		switch (target.getId()) {
			case R.id.btBtCurrRatesFromEd:
				(findViewById(R.id.btCurrRatesFrom)).performClick();
				break;
			case R.id.btCurrRatesFrom:
				intent = new Intent(getBaseContext(), CurrencyList.class);
				intent.setAction(Intent.ACTION_PICK);
				startActivityForResult(intent, Constants.RequestCurrencyForFromRate);
				break;
			case R.id.btBtCurrRatesToEd:
				(findViewById(R.id.btCurrRatesTo)).performClick();
				break;
			case R.id.btCurrRatesTo:
				intent = new Intent(getBaseContext(), CurrencyList.class);
				intent.setAction(Intent.ACTION_PICK);
				startActivityForResult(intent, Constants.RequestCurrencyForToRate);
				break;
			case R.id.btCurrRatesDateEd:
				(findViewById(R.id.btCurrRatesDate)).performClick();
				break;
			case R.id.btCurrRatesDate:
				showDialog(rateDateDialogID);
				break;
			case R.id.btCurrRatesRateEd:
				intent = new Intent(getBaseContext(), Calculator.class);
				if (Double.compare(rate, 0) != 0) {
					Bundle bundle = new Bundle();
					bundle.putDouble(Calculator.startupAmountKey, rate);
					intent.putExtras(bundle);
				}
				intent.setAction(Intent.ACTION_PICK);
				if(getCurrentFocus()!=null) {
					InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
					inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
				}
				startActivityForResult(intent, Constants.RequestCalculator);
				break;
			case R.id.btCurrRatesOk:
				try {
					if (fromCurrencyID == 0)
						DialogTools.toastDialog(getBaseContext(),
								getString(R.string.msgChoose) + " " + getString(R.string.from) + " " + getString(R.string.currency), Toast.LENGTH_SHORT);
					else if (toCurrencyID == 0)
						DialogTools.toastDialog(getBaseContext(),
								getString(R.string.msgChoose) + " " + getString(R.string.to) + " " + getString(R.string.currency), Toast.LENGTH_SHORT);
					else if (rate.equals(0d))
						DialogTools.toastDialog(getBaseContext(),
								getString(R.string.msgEnter) + " " + getString(R.string.rate), Toast.LENGTH_SHORT);
					else if (rate.compareTo(0.0001d) < 0)
						DialogTools.toastDialog(getBaseContext(), R.string.msgValueTooSmall, Toast.LENGTH_LONG);
					else if (getIntent().getAction().equals(Intent.ACTION_INSERT)) {
						if (CurrRatesSrv.rateExists(getBaseContext(), fromCurrencyID, toCurrencyID, rateDate, null, null))
							DialogTools.toastDialog(getBaseContext(), getBaseContext().getResources().getString(R.string.msgRateExists), Toast.LENGTH_SHORT);
						else {
							CurrRatesSrv.insertRate(getBaseContext(), fromCurrencyID, toCurrencyID, rate, rateDate);
							finish();
						}
					} else if (getIntent().getAction().equals(Intent.ACTION_EDIT)) {
						if (CurrRatesSrv.rateExists(getBaseContext(), fromCurrencyID, toCurrencyID, rateDate, null, updatedID))
							DialogTools.toastDialog(getBaseContext(), getBaseContext().getResources().getString(R.string.msgRateExists), Toast.LENGTH_SHORT);
						else {
							CurrRatesSrv.updateRateByID(getBaseContext(), fromCurrencyID, toCurrencyID, rate, rateDate, updatedID);
							finish();
						}
					}
				} catch (IllegalArgumentException e) {
					Tracker myTracker = EasyTracker.getInstance(getBaseContext());
					myTracker.set(Fields.SCREEN_NAME, "CurrRatesEdit- Error3");
					myTracker.send(MapBuilder.createAppView().build());
				}
				break;
			case R.id.btCurrRatesCancel:
				finish();
				break;
			default:
				break;
		}
	}
			
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			if (requestCode == Constants.RequestCurrencyForFromRate) {
				Uri selectedUri = data.getData();
				Cursor cursor = this.managedQuery(selectedUri, null, null, null, null);
				cursor.moveToFirst();
				fromCurrencyID = Integer.parseInt(DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData._ID));
				btFromCurrency.setText(DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData.NAME) +
						": " + DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData.SIGN));
			} else if (requestCode == Constants.RequestCurrencyForToRate) {
				Uri selectedUri = data.getData();
				Cursor cursor = this.managedQuery(selectedUri, null, null, null, null);
				cursor.moveToFirst();
				toCurrencyID = Integer.parseInt(DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData._ID));
				btToCurrency.setText(DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData.NAME) +
						": " + DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData.SIGN));
			} else if (requestCode == Constants.RequestCalculator) {
				rate = data.getDoubleExtra(Constants.calculatorValue, 0d);
				((EditText) findViewById(R.id.edCurrRatesRate)).setText(Tools.formatDecimal(rate, Constants.rateDecimalCount));
			}
		}
	}	
			
	/*public static void updateRate(Context context, long firstCurrID, long secondeCurrID, double rate, Date rateDate)
	{
		//TODO burada kohne transaction ve transferleri update etmek lazimdi
		try {
			ContentValues values = new ContentValues();
			values.put(CurrRatesTableMetaData.VALUE, Tools.formatDecimal(rate, Constants.rateDecimalCount));
			context.getContentResolver().update(CurrRatesTableMetaData.CONTENT_URI, values, 
					CurrRatesTableMetaData.FIRSTCURRID + " = " + String.valueOf(firstCurrID) + " and " +
					CurrRatesTableMetaData.SECONDCURRID + " = " + String.valueOf(secondeCurrID) + " and " + 
					CurrRatesTableMetaData.RATEDATE + " = '" + Tools.DateToDBString(rateDate) + "' ", null);
		}
		catch (IllegalArgumentException e) {
			Tracker myTracker = EasyTracker.getInstance(context);
			myTracker.set(Fields.SCREEN_NAME, "CurrRatesEdit- Error8");
			myTracker.send(MapBuilder.createAppView().build());
		}
	}*/
	
	public static void deleteRate(Context context, long currID) {
		context.getContentResolver().delete(Uri.withAppendedPath(CurrRatesTableMetaData.CONTENT_URI, String.valueOf(currID)), null, null);
	}	
}
