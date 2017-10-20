package com.jgmoneymanager.paid;

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
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CurrencyTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.services.AccountSrv;
import com.jgmoneymanager.services.CurrencySrv;
import com.jgmoneymanager.services.TransactionSrv;
import com.jgmoneymanager.tools.Tools;

import java.util.Locale;

public class CurrencyEdit extends MyActivity {

	EditText edName;
	EditText edSign;
	String id;
	String name;
	String sign;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

		LinearLayout mainLayout = (LinearLayout) findViewById(R.id.content_main_layout);
		LayoutInflater inflater = (LayoutInflater)      this.getSystemService(LAYOUT_INFLATER_SERVICE);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		View child = inflater.inflate(R.layout.currencyedit, null);
		mainLayout.addView(child, params);


		if (savedInstanceState != null) {
			id = Tools.getStringFromBundle(savedInstanceState, "id");
			name = Tools.getStringFromBundle(savedInstanceState, "name");
			sign = Tools.getStringFromBundle(savedInstanceState, "sign");
		}
		else {
			Bundle bundle = getIntent().getExtras();
			
			if (getIntent().getAction().equals(Intent.ACTION_EDIT))
			{
				name = bundle.getString(CurrencyTableMetaData.NAME);
				sign = bundle.getString(CurrencyTableMetaData.SIGN);
				id = bundle.getString(CurrencyTableMetaData._ID);
			}
			else
			{
				name = "";
				sign = "";
			}
		}
		reloadScreen();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Tools.putToBundle(outState, "id", id);
		Tools.putToBundle(outState, "name", name);
		Tools.putToBundle(outState, "sign", sign);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setContentView(R.layout.currencyedit);
		reloadScreen();
	}

	private void reloadScreen() {
		edName = (EditText) findViewById(R.id.edCurrName);
		edSign = (EditText) findViewById(R.id.edCurrSign);

		edName.addTextChangedListener(new TextWatcher() {			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,int after) {}
			@Override
			public void afterTextChanged(Editable s) {
				name = Tools.cutName(edName.getText().toString());
			}
		});
		edName.setText(name);

		edSign.addTextChangedListener(new TextWatcher() {			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,int after) {}
			@Override
			public void afterTextChanged(Editable s) {
				sign = edSign.getText().toString().toUpperCase(Locale.UK);
			}
		});
		edSign.setText(sign);
	}
	
	public void myClickHandler(View target) {
    	switch (target.getId()) {
			case R.id.btCurrOk:
				if (edSign.getText().toString().trim().length() == 0)
				{
					DialogTools.toastDialog(getBaseContext(), getResources().getString(R.string.msgEnter) + " " + CurrencyTableMetaData.SIGN, Toast.LENGTH_SHORT);
				}
				else
				{	
					if (getIntent().getAction().equals(Intent.ACTION_INSERT))
					{
						Cursor cursor = getContentResolver().query(CurrencyTableMetaData.CONTENT_URI, null, 
								CurrencyTableMetaData.SIGN + " = '" + sign + "' ", null, null);
						if (cursor.getCount() > 0) {
							DialogTools.toastDialog(getBaseContext(), getString(R.string.msgCurrencyExists), Toast.LENGTH_SHORT);
							cursor.close();
						}
						else {
							ContentValues cv = new ContentValues();
							cv.put(CurrencyTableMetaData.NAME, name);
							cv.put(CurrencyTableMetaData.SIGN, sign);
							cv.put(CurrencyTableMetaData.ISDEFAULT, 0);
							cv.putNull(CurrencyTableMetaData.RESOURCEID);
							getContentResolver().insert(CurrencyTableMetaData.CONTENT_URI, cv);
							setResult(RESULT_OK);
							finish();
						}
					}
					else 
					{
						Cursor cursor = getContentResolver().query(CurrencyTableMetaData.CONTENT_URI, null, 
								CurrencyTableMetaData.SIGN + " = '" + sign + "' and " +
								CurrencyTableMetaData._ID + " <> " + id, null, null);
						if (cursor.getCount() > 0) {
							DialogTools.toastDialog(getBaseContext(), getString(R.string.msgCurrencyExists), Toast.LENGTH_SHORT);
							cursor.close();
						}
						else {
							CurrencySrv.updateCurrency(CurrencyEdit.this, Long.valueOf(id), name, sign, true);
							setResult(RESULT_OK);
							finish();
						}
					}
				}
				break;
			case R.id.btCurrCancel:
				setResult(RESULT_CANCELED);
				finish();
				break;
			case R.id.btCurrNameEd:
				findViewById(R.id.edCurrName).requestFocus();
				break;
			case R.id.btCurrSignEd:
				findViewById(R.id.edCurrSign).requestFocus();
				break;
			default:
				break;				
    	}
	}

	public static void deleteCurrency(Context context, long currID) {
		if (currID != 0)
		{
			AccountSrv.updateAccountCurrencyToDefault(context, currID);
			TransferEdit.updateTransfersCurrencyToDefault(context, currID);
			RPTransactionEdit.updateRPTransactionsCurrencyToDefault(context, currID);
			TransactionSrv.updateTransactionsCurrencyTodefault(context, currID);
			CurrRatesEdit.deleteRate(context, currID);
			context.getContentResolver().delete(Uri.withAppendedPath(CurrencyTableMetaData.CONTENT_URI, String.valueOf(currID)), null, null);
		}
		else
		{
			Cursor cursor = context.getContentResolver().query(CurrencyTableMetaData.CONTENT_URI, 
					new String[] {CurrencyTableMetaData._ID}, CurrencyTableMetaData.ISDEFAULT + " = 0 ", null, null);
			for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
				AccountSrv.updateAccountCurrencyToDefault(context, DBTools.getCursorColumnValueLong(cursor, CurrencyTableMetaData._ID));
				TransferEdit.updateTransfersCurrencyToDefault(context, DBTools.getCursorColumnValueLong(cursor, CurrencyTableMetaData._ID));
				RPTransactionEdit.updateRPTransactionsCurrencyToDefault(context, DBTools.getCursorColumnValueLong(cursor, CurrencyTableMetaData._ID));
				TransactionSrv.updateTransactionsCurrencyTodefault(context, DBTools.getCursorColumnValueLong(cursor, CurrencyTableMetaData._ID));
				CurrRatesEdit.deleteRate(context, DBTools.getCursorColumnValueLong(cursor, CurrencyTableMetaData._ID));
				context.getContentResolver().delete(Uri.withAppendedPath(CurrencyTableMetaData.CONTENT_URI, DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData._ID)), null, null);
			}
		}
	}

	public static void updateSortOrder(Context context, long currencyId) {
		Cursor cursor = context.getContentResolver().query(CurrencyTableMetaData.CONTENT_URI, null, 
				CurrencyTableMetaData._ID + " = " + String.valueOf(currencyId) + 
				" and " + CurrencyTableMetaData.SORTORDER + " is null and " + 
				CurrencyTableMetaData.ISDEFAULT + " = 0 ", null, null);
		if (cursor.moveToFirst()) {
			String query = "update " + CurrencyTableMetaData.TABLE_NAME + 
				" set " + CurrencyTableMetaData.SORTORDER + " = null where " +
				CurrencyTableMetaData.SORTORDER + " = 3";
			DBTools.execQuery(context, query);
			query = "update " + CurrencyTableMetaData.TABLE_NAME + 
				" set " + CurrencyTableMetaData.SORTORDER + " = " + CurrencyTableMetaData.SORTORDER + " + 1 where " +
				CurrencyTableMetaData.SORTORDER + " in (1, 2)";
			DBTools.execQuery(context, query);
			ContentValues values = new ContentValues();		
			values.put(CurrencyTableMetaData.SORTORDER, 1);
			context.getContentResolver().update(CurrencyTableMetaData.CONTENT_URI, values, 
				CurrencyTableMetaData._ID + " = " + String.valueOf(currencyId), null);
		}
		cursor.close();
	}
}
