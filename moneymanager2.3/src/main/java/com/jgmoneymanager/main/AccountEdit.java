package com.jgmoneymanager.main;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.AccountTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CurrencyTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.services.AccountSrv;
import com.jgmoneymanager.services.CurrRatesSrv;
import com.jgmoneymanager.services.CurrencySrv;
import com.jgmoneymanager.services.TransactionSrv;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import java.util.Date;

public class AccountEdit extends MyActivity {

	private EditText edName;
	private EditText edDescription;
	private EditText edInitialBalance;
	private TextView lbAddedDate;
	private Button btCurrSign;
	private Spinner spStatus;
	private CheckBox cbIsDefault;
	private String initialBalance;
	private String oldInitialBalance;
	private long oldCurrID;
	private long newCurrID;
	private int status;
	private String name;
	private String description;
	private String addedDate;
	private String id;
	private boolean isDefault;
	private boolean defaultIsEdited;//this used if edited account is default account then cbIsDefault will be disabled

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.accountedit);
		
		if (savedInstanceState != null) {
			initialBalance = Tools.getStringFromBundle(savedInstanceState, "initialBalance");
			oldInitialBalance = Tools.getStringFromBundle(savedInstanceState, "oldInitialBalance");
			oldCurrID = Tools.getLongFromBundle0(savedInstanceState, "oldCurrID");
			newCurrID = Tools.getLongFromBundle0(savedInstanceState, "newCurrID");
			status = Tools.getIntegerFromBundle0(savedInstanceState, "status");
			name = Tools.getStringFromBundle(savedInstanceState, "name");
			description = Tools.getStringFromBundle(savedInstanceState, "description");
			addedDate = Tools.getStringFromBundle(savedInstanceState, "addedDate");
			id = Tools.getStringFromBundle(savedInstanceState, "id");
			isDefault = Tools.getBooleanFromBundle0(savedInstanceState, "isDefault");
			defaultIsEdited = Tools.getBooleanFromBundle0(savedInstanceState, "defaultIsEdited");
		}
		else {
			if (getIntent().getAction().equals(Intent.ACTION_EDIT))
			{
				Bundle bundle = getIntent().getExtras();
				name = bundle.getString(AccountTableMetaData.NAME);
				newCurrID = bundle.getLong(AccountTableMetaData.CURRID);
				oldCurrID = newCurrID;
				initialBalance = bundle.getString(AccountTableMetaData.INITIALBALANCE);
				oldInitialBalance = initialBalance;
				description = bundle.getString(AccountTableMetaData.DESCRIPTION);			
				status = bundle.getInt(AccountTableMetaData.STATUS);
				isDefault = bundle.getString(AccountTableMetaData.ISDEFAULT).equals("1");
				defaultIsEdited = isDefault;
				addedDate = Tools.DateToString(Tools.StringToDate(bundle.getString(AccountTableMetaData.CREATED_DATE), Constants.DateFormatDBLong), Constants.DateFormatUser);
				id = bundle.getString(AccountTableMetaData._ID);
			}
			else 
			{
				id = "0";
				name = "";
				newCurrID = Constants.defaultCurrency;
				initialBalance = "";
				description = "";			
				status = Constants.Status.Enabled.index();
				addedDate = Tools.DateToString(Tools.getCurrentDate(), Constants.DateFormatUser);
				isDefault = false;
				defaultIsEdited = false;
			}
		}
		reloadScreen();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Tools.putToBundle(outState, "initialBalance", initialBalance);
		Tools.putToBundle(outState, "oldInitialBalance", oldInitialBalance);
		Tools.putToBundle(outState, "oldCurrID", oldCurrID);
		Tools.putToBundle(outState, "newCurrID", newCurrID);
		Tools.putToBundle(outState, "status", status);
		Tools.putToBundle(outState, "name", name);
		Tools.putToBundle(outState, "description", description);
		Tools.putToBundle(outState, "addedDate", addedDate);
		Tools.putToBundle(outState, "id", id);
		Tools.putToBundle(outState, "isDefault", isDefault);
		Tools.putToBundle(outState, "defaultIsEdited", defaultIsEdited);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setContentView(R.layout.accountedit);
		reloadScreen();
	}

	private void reloadScreen() {
		edName = (EditText) findViewById(R.id.edAccName);
		edInitialBalance = (EditText) findViewById(R.id.edAccInitBalance);
		edDescription = (EditText) findViewById(R.id.edAccDesc);
		cbIsDefault = (CheckBox) findViewById(R.id.cbAccIsDefault);
		btCurrSign = (Button) findViewById(R.id.btAccCurrSign);		
		spStatus = (Spinner)findViewById(R.id.spAccStatus);
		lbAddedDate = (TextView)findViewById(R.id.lbAccAddedDateValue);
		
		edName.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				name = edName.getText().toString();
			}
		});
		edName.setText(name);
		
		btCurrSign.setText(CurrencySrv.getCurrencyNameSignByID(this, newCurrID));
		//btCurrSign.setEnabled(id.equals("0"));
		
		edInitialBalance.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				initialBalance = edInitialBalance.getText().toString();
			}
		});
		edInitialBalance.setText(initialBalance);
		
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
				
		ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, R.layout.simple_spinner_item_blue, R.id.spinItem,
				getResources().getStringArray(R.array.Status));
		spinnerArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
		spStatus.setAdapter(spinnerArrayAdapter);
		spStatus.setSelection(status);
		
		cbIsDefault.setChecked(isDefault);
		cbIsDefault.setOnCheckedChangeListener(new OnCheckedChangeListener() {			
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if (defaultIsEdited && !arg1) {
					DialogTools.toastDialog(AccountEdit.this, R.string.msgChooseAnotherAccountAsDefault, Toast.LENGTH_LONG);
					cbIsDefault.setChecked(true);
				}
				
			}
		});
		lbAddedDate.setText(addedDate);
	}
	/**
	 * Changes new account to Deafult and old default to simple account
	 * @param context
	 * @param accountID 
	 * new Default account ID
	 */
	public static void setDefaultAccount(Context context, long accountID)
	{
		if (!AccountSrv.isAccountVisible(context, accountID))
			DialogTools.toastDialog(context, R.string.msgSetDefaultInvisibleAccount, Toast.LENGTH_LONG);
		else {
			ContentValues values = new ContentValues();
			values.put(AccountTableMetaData.ISDEFAULT, 1);
			context.getContentResolver().update(Uri.withAppendedPath(AccountTableMetaData.CONTENT_URI, String.valueOf(accountID)), 
					values, null, null);
			values.clear();
			values.put(AccountTableMetaData.ISDEFAULT, 0);
			context.getContentResolver().update(AccountTableMetaData.CONTENT_URI, 
				values, AccountTableMetaData._ID + " <> " + String.valueOf(accountID), null);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == Constants.RequestCurrencyForAccount)
		{
			if (resultCode == RESULT_OK) {
				Uri selectedUri = data.getData();
				Cursor cursor = getContentResolver().query(selectedUri, null, null, null, null);
				cursor.moveToFirst();
				newCurrID = Integer.parseInt(DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData._ID));
				btCurrSign.setText(DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData.NAME) + 
						": " + DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData.SIGN));
			}
		}	
	}
	
	public void myClickHandler(View target) {
		Intent intent;
		switch (target.getId()) {
		case R.id.btAccCurrSign:
			intent = new Intent(getBaseContext(), CurrencyList.class);
			intent.setAction(Intent.ACTION_PICK);
			startActivityForResult(intent, Constants.RequestCurrencyForAccount);
			break;
		case R.id.btAccOk:
			if (edName.getText().toString().trim().length() == 0)
			{
				DialogTools.toastDialog(AccountEdit.this, AccountEdit.this.getResources().getString(R.string.msgEnter) + " " +
						AccountEdit.this.getResources().getString(R.string.name), Toast.LENGTH_LONG);
				return;
			}
			if ((initialBalance == null) || (initialBalance.length() == 0) || initialBalance.equals(".") || initialBalance.equals(","))
			{
				DialogTools.toastDialog(AccountEdit.this, R.string.msgInvalidBalance, Toast.LENGTH_SHORT);
				return;
			}
			String name = Tools.cutName(edName.getText().toString());
			if (getIntent().getAction().equals(Intent.ACTION_INSERT) && 
					Tools.existsInTable(getBaseContext(), AccountTableMetaData.CONTENT_URI, AccountTableMetaData.NAME, 
							name, null))
			{
				DialogTools.toastDialog(getBaseContext(), getResources().getString(R.string.msgAccountExists), Toast.LENGTH_LONG);
				return;
			}				
			if (getIntent().getAction().equals(Intent.ACTION_EDIT) && 
					Tools.existsInTable(getBaseContext(), AccountTableMetaData.CONTENT_URI, AccountTableMetaData.NAME, 
							name, AccountTableMetaData._ID + " != " + id))
			{
				DialogTools.toastDialog(getBaseContext(), getResources().getString(R.string.msgAccountExists), Toast.LENGTH_LONG);
				return;
			}
			if (newCurrID == 0)
			{
				DialogTools.toastDialog(getBaseContext(), 
						getResources().getString(R.string.msgChoose) + " " + getResources().getString(R.string.currency), 
						Toast.LENGTH_LONG);
				return;					
			}
			if (cbIsDefault.isChecked() && (spStatus.getSelectedItemId() == 0)) {
				DialogTools.toastDialog(AccountEdit.this, R.string.msgSetDefaultInvisibleAccount, Toast.LENGTH_LONG);
				return;
			}
			final ContentValues values = new ContentValues();
			if (cbIsDefault.isChecked())
			{
				values.put(AccountTableMetaData.ISDEFAULT, "0");
				getContentResolver().update(AccountTableMetaData.CONTENT_URI, values, null, null);
				values.clear();
			}
			values.put(AccountTableMetaData.NAME, name);
			values.put(AccountTableMetaData.CURRID, newCurrID);
			values.put(AccountTableMetaData.INITIALBALANCE, Tools.formatDecimal(edInitialBalance.getText().toString()));
			values.put(AccountTableMetaData.DESCRIPTION, edDescription.getText().toString());
			values.put(AccountTableMetaData.ISDEFAULT, (cbIsDefault.isChecked() ? "1" : "0"));
			values.put(AccountTableMetaData.STATUS, spStatus.getSelectedItemId());
			if (getIntent().getAction().equals(Intent.ACTION_INSERT))
			{
				StringBuilder sb = new StringBuilder();
				//final Date rateDate = TransactionSrv.getFirstTransactionDate(AccountEdit.this, Long.valueOf(id));
				if (!CurrRatesSrv.rateExists(AccountEdit.this, Constants.defaultCurrency, newCurrID, Tools.getCurrentDate(), sb, null)){
					Command cmd = new Command() {				
						@Override
						public void execute() {
							getContentResolver().insert(AccountTableMetaData.CONTENT_URI, values);
							setResult(RESULT_OK);
							finish();
						}
					};
					CurrRatesSrv.askForRate(this, cmd, Constants.defaultCurrency, newCurrID, Tools.getCurrentDate(), sb);
				}
				else {
					getContentResolver().insert(AccountTableMetaData.CONTENT_URI, values);
					setResult(RESULT_OK);
					finish();
				}
			}
			else
			{
					StringBuilder sb = new StringBuilder();
					final Date rateDate = TransactionSrv.getFirstTransactionDate(AccountEdit.this, Long.valueOf(id));
					if (!CurrRatesSrv.rateExists(AccountEdit.this, oldCurrID, newCurrID, rateDate, sb, null)){
						Command cmd = new Command() {				
							@Override
							public void execute() {
								AccountSrv.updateAccount(AccountEdit.this, values, oldInitialBalance, edInitialBalance.getText().toString(), newCurrID, oldCurrID, id);
								setResult(RESULT_OK);
								finish();
							}
						};
						CurrRatesSrv.askForRate(this, cmd, oldCurrID, newCurrID, rateDate, sb);
					}
					else {
						AccountSrv.updateAccount(AccountEdit.this, values, oldInitialBalance, edInitialBalance.getText().toString(), newCurrID, oldCurrID, id);
						setResult(RESULT_OK);
						finish();
					}
			}
			break;
		case R.id.btAccCancel:
			setResult(RESULT_CANCELED);
			finish();
			break;
		}
	}
}
