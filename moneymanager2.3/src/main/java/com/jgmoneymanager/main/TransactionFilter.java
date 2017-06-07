package com.jgmoneymanager.main;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;
import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.PaymentMethodsTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CurrencyTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.AccountTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CategoryTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransactionStatusTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VTransactionViewMetaData;
import com.jgmoneymanager.dialogs.CheckBoxDialog;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.entity.CheckBoxItem;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.entity.Group;
import com.jgmoneymanager.tools.Tools;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class TransactionFilter extends MyActivity {

	Date fromDatePeriod = null;
	Date toDatePeriod = null;
	String amountFrom = null;
	String amountTo = null;
	int operationTypeID = Constants.TransFOperType.All.index();
	int transactionTypeID = Constants.TransFTransaction.All.index();
	HashMap<Integer, Integer> checkedCategoryItems = new HashMap<Integer, Integer>();
	ArrayList<CheckBoxItem> accountsList;
	ArrayList<CheckBoxItem> currencyList;
	ArrayList<CheckBoxItem> statusList;
	ArrayList<CheckBoxItem> paymentMethodList;
	ArrayList<Group> categoriesList;
	ArrayList<Integer> checkedCategoryIDs;
	Spinner spOperation;
	Spinner spTransaction;
	final int fromDateDialogID = 1;
	final int toDateDialogID = 2;

	private AdView adView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.transactionfilter);
		
		if (savedInstanceState != null) {
			getValuesFromBundle(savedInstanceState);
		}
		else if (getIntent().getExtras() != null)
			getValuesFromBundle(getIntent().getExtras());
		
		try {
			categoriesList = CategoryFilter.generateData(TransactionFilter.this);
			//DBTools.closeDatabase();
			if ((checkedCategoryItems == null) || (checkedCategoryItems.isEmpty())) 
				checkedCategoryIDs = new ArrayList<Integer>(categoriesList.size());
		}
		catch (Exception e) {
			Tracker myTracker = EasyTracker.getInstance(getBaseContext());     // Get a reference to tracker.
			myTracker.set(Fields.SCREEN_NAME, "Transaction Filter- Error2");
			myTracker.send(MapBuilder.createAppView().build());			
		}

		try {
			spOperation = (Spinner) findViewById(R.id.spTrFOperation);
			ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, R.layout.simple_spinner_item_blue, R.id.spinItem,
					getResources().getStringArray(R.array.TransFOperType));
			spinnerArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
			spOperation.setAdapter(spinnerArrayAdapter);
			spTransaction = (Spinner) findViewById(R.id.spTrFTransaction);
			spinnerArrayAdapter = new ArrayAdapter<String>(this, R.layout.simple_spinner_item_blue, R.id.spinItem,
					getResources().getStringArray(R.array.TransFTransaction));
			spinnerArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
			spTransaction.setAdapter(spinnerArrayAdapter);
		}
		catch (Exception e) {
			Tracker myTracker = EasyTracker.getInstance(getBaseContext());     // Get a reference to tracker.
			myTracker.set(Fields.SCREEN_NAME, "Transaction Filter- Error3");
			myTracker.send(MapBuilder.createAppView().build());			
		}

		// Create the adView
		try {
			if (!Tools.proVersionExists(this) /*&& (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE)*/) {
				adView = new AdView(this, AdSize.BANNER, "ca-app-pub-5995868530154544/9731982113");
				RelativeLayout layout = (RelativeLayout) findViewById(R.id.TrFLayoutAds);
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
		
		reloadScreen();
	}

	@Override
	protected void onDestroy() {
		try {
			super.onDestroy();
			if (adView != null)
				adView.removeAllViews();
			adView.destroy();
		} catch (Exception ex) {

		}
	}
	
	void reloadScreen() {
		try {
			setButtonFromList(R.id.btTrFAccount, accountsList);
			setButtonFromList(R.id.btTrFCurrency, currencyList);
			setButtonFromList(R.id.btTrFStatus, statusList);
			setButtonFromList(R.id.btTrFMethod, paymentMethodList);
			setCategoryButtonFromList(categoriesList, checkedCategoryIDs, checkedCategoryItems);
			setDateButtonText(R.id.btTrFDateFrom, fromDatePeriod);
			setDateButtonText(R.id.btTrFDateTo, toDatePeriod);
		}
		catch (Exception e) {
			Tracker myTracker = EasyTracker.getInstance(getBaseContext());     // Get a reference to tracker.
			myTracker.set(Fields.SCREEN_NAME, "Transaction Filter- Error4");
			myTracker.send(MapBuilder.createAppView().build());			
		}
		
		try {
			if (amountFrom != null)
				((EditText) findViewById(R.id.edTrFAmountFrom)).setText(amountFrom);
			else 
				((EditText) findViewById(R.id.edTrFAmountFrom)).setText("");
			if (amountTo != null)
				((EditText) findViewById(R.id.edTrFAmountTo)).setText(amountTo);
			else 
				((EditText) findViewById(R.id.edTrFAmountTo)).setText("");
			spOperation.setSelection(operationTypeID);
			spTransaction.setSelection(transactionTypeID);
		}
		catch (Exception e) {
			Tracker myTracker = EasyTracker.getInstance(getBaseContext());     // Get a reference to tracker.
			myTracker.set(Fields.SCREEN_NAME, "Transaction Filter- Error5");
			myTracker.send(MapBuilder.createAppView().build());			
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		putValuesToBundle(outState);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Date date;
		switch (id) {
		case fromDateDialogID:
			if (fromDatePeriod != null)
				date = fromDatePeriod;
			else
				date = Tools.getCurrentDate();
			return new DatePickerDialog(this, new OnDateSetListener() {
				@Override
				public void onDateSet(DatePicker view, int year,
						int monthOfYear, int dayOfMonth) {
					fromDatePeriod = new Date(year - 1900, monthOfYear, dayOfMonth);
					setDateButtonText(R.id.btTrFDateFrom, fromDatePeriod);
				}
			}, date.getYear() + 1900, date.getMonth(), date.getDate());
		case toDateDialogID:
			if (toDatePeriod != null)
				date = toDatePeriod;
			else
				date = Tools.getCurrentDate();
			return new DatePickerDialog(this, new OnDateSetListener() {

				@Override
				public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
					toDatePeriod = new Date(year - 1900, monthOfYear, dayOfMonth);
					setDateButtonText(R.id.btTrFDateTo, toDatePeriod);
				}
			}, date.getYear() + 1900, date.getMonth(), date.getDate());
		}
		return null;
	}

	private String createConditionQuery() {
		String query = "";

		if ((accountsList != null) && (Tools.getIDsFromCheckBoxList(accountsList).length() != 0)) {
			query += " and " + VTransactionViewMetaData.ACCOUNTID + " in (" + Tools.getIDsFromCheckBoxList(accountsList) + ")";
		}

		if ((currencyList != null) && (Tools.getIDsFromCheckBoxList(currencyList).length() != 0)) {
			query += " and " + VTransactionViewMetaData.CURRID + " in (" + Tools.getIDsFromCheckBoxList(currencyList) + ")";
		}

		if ((statusList != null) && (Tools.getIDsFromCheckBoxList(statusList).length() != 0)) {
			query += " and " + VTransactionViewMetaData.STATUS + " in (" + Tools.getIDsFromCheckBoxList(statusList) + ")";
		}

		if ((paymentMethodList != null) && (Tools.getIDsFromCheckBoxList(paymentMethodList).length() != 0)) {
			query += " and " + VTransactionViewMetaData.PAYMENT_METHOD + " in (" + Tools.getIDsFromCheckBoxList(paymentMethodList) + ")";
		}

		if (checkedCategoryIDs.size() > 0) {
			query += " and " + VTransactionViewMetaData.CATEGORYID + " in (";
			String ids = "";
			for (int i = 0; i < checkedCategoryIDs.size(); i++)
				ids += checkedCategoryIDs.get(i).toString() + ", ";
			ids = "(" + ids.substring(0, ids.length() - 2) + ")";
			query += "select " + CategoryTableMetaData._ID + " from "
					+ CategoryTableMetaData.TABLE_NAME + " where "
					+ CategoryTableMetaData._ID + " in " + ids + " or "
					+ CategoryTableMetaData.MAINID + " in " + ids + ")";
		}

		if (fromDatePeriod != null)
			query += " and " + VTransactionViewMetaData.TRANSDATE + " >= '"
					+ Tools.DateToDBString(fromDatePeriod) + "'";
		if (toDatePeriod != null)
			query += " and " + VTransactionViewMetaData.TRANSDATE + " <= '"
					+ Tools.DateToDBString(toDatePeriod) + "'";

		EditText editText = (EditText) findViewById(R.id.edTrFAmountFrom);
		if (editText.getText().toString().length() > 0) {
			amountFrom = editText.getText().toString();
			query += " and CAST(" + VTransactionViewMetaData.AMOUNT + " as integer) >= "
					+ amountFrom + " ";
		} else
			amountFrom = null;
		editText = (EditText) findViewById(R.id.edTrFAmountTo);
		if (editText.getText().toString().length() > 0) {
			amountTo = editText.getText().toString();
			query += " and CAST(" + VTransactionViewMetaData.AMOUNT + " as integer) <= "
					+ amountTo + " ";
		} else
			amountTo = null;

		Spinner spinner = (Spinner) findViewById(R.id.spTrFOperation);
		operationTypeID = (int) spinner.getSelectedItemId();
		if (operationTypeID == Constants.TransFOperType.Transaction.index())
			query += " and " + VTransactionViewMetaData.ISTRANSFER + " = 0 ";
		else if (operationTypeID == Constants.TransFOperType.Transfer.index())
			query += " and " + VTransactionViewMetaData.ISTRANSFER + " = 1 ";

		spinner = (Spinner) findViewById(R.id.spTrFTransaction);
		transactionTypeID = (int) spinner.getSelectedItemId();
		if (transactionTypeID == Constants.TransFTransaction.Income.index())
			query += " and " + VTransactionViewMetaData.TRANSTYPE + " = '1' ";
		else if (transactionTypeID == Constants.TransFTransaction.Expence.index())
			query += " and " + VTransactionViewMetaData.TRANSTYPE + " = '-1' ";
		return query;
	}

	private void setButtonFromList(int buttonId, ArrayList<CheckBoxItem> list) {
		((Button)findViewById(buttonId)).setText(Tools.getNamesFromCheckBoxList(list));
	}
	
	private void setCategoryButtonFromList(ArrayList<Group> list, ArrayList<Integer> checkedIDs, HashMap checkedItems) {
		Button button = (Button) findViewById(R.id.btTrFCategory);
		checkedIDs.clear();
		String name = "";
		for (int i = 0; i < list.size(); i++) {
			Group group = list.get(i);
			//if (group.isChecked()) {
			if (checkedItems.containsKey(group.getID())) {
				checkedIDs.add(group.getID());
				name += ", " + group.getName();
			}
			List<CheckBoxItem> subCategories = group.getChildren();
			for (int j = 0; j < subCategories.size(); j++) {
				CheckBoxItem subCategoryItem = subCategories.get(j);
				//if (subCategoryItem.isSelected()) {
				if (checkedItems.containsKey(subCategoryItem.getID())) {
					name += ", " + subCategoryItem.getName();
					checkedIDs.add(subCategoryItem.getID());
				}
			}
		}
		if (name.length() > 0)
			if (name.length() > Constants.maxButtonTextLength)
				button.setText(name.substring(2, Constants.maxButtonTextLength) + "...");
			else
				button.setText(name.substring(2));
		else
			button.setText(R.string.notSet);
	}

	private void setDateButtonText(int buttonID, Date date) {
		if (date != null)
			((Button) findViewById(buttonID)).setText(Tools.DateToString(date, Constants.DateFormatUser));
		else 
			((Button) findViewById(buttonID)).setText("");
	}

	void clearFilter() {
		fromDatePeriod = null;
		toDatePeriod = null;
		amountFrom = null;
		amountTo = null;
		operationTypeID = Constants.TransFOperType.All.index();
		transactionTypeID = Constants.TransFTransaction.All.index();
		/*for (int i = 0; i < checkedAccountItems.length; i++)
			checkedAccountItems[i] = false;*/
		accountsList = null;
		currencyList = null;
		statusList = null;
		paymentMethodList = null;
		checkedCategoryItems.clear();
		for (int i = 0; i < categoriesList.size(); i++) {
			Group group = categoriesList.get(i);
			group.setChecked(false);
			List<CheckBoxItem> subCategories = group.getChildren();
			for (int j = 0; j < subCategories.size(); j++) {
				CheckBoxItem subCategory = subCategories.get(j);
				subCategory.setSelected(false);
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			if (requestCode == Constants.RequestCategoryForTransaction) {
				checkedCategoryItems.clear();
				categoriesList = CategoryFilter.group;
				for (int i = 0; i < categoriesList.size(); i++) {
					Group group = categoriesList.get(i);
					//if (group.isChecked())
					//	checkedCategoryItems.put(group.getID(), group.getID());
					List<CheckBoxItem> subCategories = group.getChildren();
					for (int j = 0; j < subCategories.size(); j++) {
						CheckBoxItem subCategory = subCategories.get(j);
						if (subCategory.isSelected())
							checkedCategoryItems.put(subCategory.getID(), subCategory.getID());
					}
				}
				setCategoryButtonFromList(categoriesList, checkedCategoryIDs, checkedCategoryItems);
			}
			if (requestCode == Constants.RequestAccountForTransaction) {
				accountsList = CheckBoxDialog.itemsList;
				setButtonFromList(R.id.btTrFAccount, accountsList);
			}
			if (requestCode == Constants.RequestCurrencyForTransaction) {
				currencyList = CheckBoxDialog.itemsList;
				setButtonFromList(R.id.btTrFCurrency, currencyList);
			}
			if (requestCode == Constants.RequestStatusForTransaction) {
				statusList = CheckBoxDialog.itemsList;
				setButtonFromList(R.id.btTrFStatus, statusList);
			}
			if (requestCode == Constants.RequestMethodForTransaction) {
				paymentMethodList = CheckBoxDialog.itemsList;
				setButtonFromList(R.id.btTrFMethod, paymentMethodList);
			}
		}
	}

	public void myClickHandler(View target) {
		Intent intent;
		switch (target.getId()) {
			case R.id.btTrFAccount:
				intent = new Intent(TransactionFilter.this, CheckBoxDialog.class);
				Bundle bundle = new Bundle();
				if ((accountsList != null) && (accountsList.size() > 0)) {
					bundle.putBoolean(Constants.dontRefreshValues, true);
					CheckBoxDialog.itemsList = accountsList;
				}
				else
					bundle.putBoolean(Constants.dontRefreshValues, false);
				bundle.putString(Constants.query, createAccountQuery());
				bundle.putString(Constants.paramTitle, AccountTableMetaData.NAME);
				bundle.putSerializable(Constants.paramValues, Tools.convertCheckBoxListToHashMap(accountsList));
				intent.putExtras(bundle);
				startActivityForResult(intent, Constants.RequestAccountForTransaction);
				break;
			case R.id.btTrFCurrency:
				intent = new Intent(TransactionFilter.this, CheckBoxDialog.class);
				bundle = new Bundle();
				if ((currencyList != null) && (currencyList.size() > 0)) {
					bundle.putBoolean(Constants.dontRefreshValues, true);
					CheckBoxDialog.itemsList = currencyList;
				}
				else
					bundle.putBoolean(Constants.dontRefreshValues, false);
				bundle.putString(Constants.query, createCurrencyQuery());
				bundle.putString(Constants.paramTitle, CurrencyTableMetaData.NAME);
				bundle.putSerializable(Constants.paramValues, Tools.convertCheckBoxListToHashMap(currencyList));
				intent.putExtras(bundle);
				startActivityForResult(intent, Constants.RequestCurrencyForTransaction);
				break;
			case R.id.btTrFStatus:
				intent = new Intent(TransactionFilter.this, CheckBoxDialog.class);
				bundle = new Bundle();
				if ((statusList != null) && (statusList.size() > 0)) {
					bundle.putBoolean(Constants.dontRefreshValues, true);
					CheckBoxDialog.itemsList = statusList;
				}
				else
					bundle.putBoolean(Constants.dontRefreshValues, false);
				bundle.putString(Constants.query, createStatusQuery());
				bundle.putString(Constants.paramTitle, TransactionStatusTableMetaData.NAME);
				bundle.putSerializable(Constants.paramValues, Tools.convertCheckBoxListToHashMap(statusList));
				intent.putExtras(bundle);
				startActivityForResult(intent, Constants.RequestStatusForTransaction);
				break;
			case R.id.btTrFMethod:
				intent = new Intent(TransactionFilter.this, CheckBoxDialog.class);
				bundle = new Bundle();
				if ((paymentMethodList != null) && (paymentMethodList.size() > 0)) {
					bundle.putBoolean(Constants.dontRefreshValues, true);
					CheckBoxDialog.itemsList = paymentMethodList;
				}
				else
					bundle.putBoolean(Constants.dontRefreshValues, false);
				bundle.putString(Constants.query, createPaymentMethodQuery());
				bundle.putString(Constants.paramTitle, PaymentMethodsTableMetaData.NAME);
				bundle.putSerializable(Constants.paramValues, Tools.convertCheckBoxListToHashMap(paymentMethodList));
				intent.putExtras(bundle);
				startActivityForResult(intent, Constants.RequestMethodForTransaction);
				break;
			case R.id.btTrFCategory:
				intent = new Intent(TransactionFilter.this, CategoryFilter.class);
				if (!((checkedCategoryItems == null) || (checkedCategoryItems.isEmpty())))
					intent.putExtra(Constants.dontRefreshValues, true);
				startActivityForResult(intent, Constants.RequestCategoryForTransaction);
				break;
			case R.id.btTrFDateFrom:
				showDialog(fromDateDialogID);
				break;
			case R.id.btTrFDateTo:
				showDialog(toDateDialogID);
				break;
			case R.id.btTrFOk:
				String query = createConditionQuery();
				intent = new Intent();
				bundle = new Bundle();
				bundle.putString(Constants.query, query);
				putValuesToBundle(bundle);
				intent.putExtras(bundle);
				setResult(RESULT_OK, intent);
				finish();
				break;
			case R.id.btTrFCancel:
				setResult(RESULT_CANCELED);
				finish();
				break;
			case R.id.btTrFReset:
				clearFilter();
				reloadScreen();
				break;
			default:
				break;
		}
	}

	String createAccountQuery() {
		return "select " + AccountTableMetaData._ID + ", " + AccountTableMetaData.NAME + " from "
				+ AccountTableMetaData.TABLE_NAME + " order by " + AccountTableMetaData.STATUS + " desc, " + AccountTableMetaData.SORTORDER
				+ ", " + AccountTableMetaData.NAME;
	}

	String createCurrencyQuery() {
		return "select " + CurrencyTableMetaData._ID + ", " + CurrencyTableMetaData.NAME + " from "
				+ CurrencyTableMetaData.TABLE_NAME + " order by " + CurrencyTableMetaData.DEFAULT_SORT_ORDER;
	}

	String createStatusQuery() {
		return "select " + TransactionStatusTableMetaData._ID + ", " + TransactionStatusTableMetaData.NAME + " from "
				+ TransactionStatusTableMetaData.TABLE_NAME + " order by " + TransactionStatusTableMetaData.SORTORDER;
	}

	String createPaymentMethodQuery() {
		return "select " + PaymentMethodsTableMetaData._ID + ", " + PaymentMethodsTableMetaData.NAME + " from "
				+ PaymentMethodsTableMetaData.TABLE_NAME + " order by " + PaymentMethodsTableMetaData.SORTORDER;
	}
	
	void putValuesToBundle(Bundle outBundle/*, Date fromDatePeriod, Date toDatePeriod, String amountFrom,
			String amountTo, int operationTypeID, int transactionTypeID, 
			boolean[] checkedAccountItems, boolean[] checkedCategoryItems, 
			ArrayList<Integer> checkedAccountIDs, ArrayList<Integer> checkedCategoryIDs*/) {
		if (outBundle == null)
			outBundle = new Bundle();
		Tools.putToBundle(outBundle, "fromDatePeriod", fromDatePeriod);
		Tools.putToBundle(outBundle, "toDatePeriod", toDatePeriod);
		Tools.putToBundle(outBundle, "amountFrom", amountFrom);
		Tools.putToBundle(outBundle, "amountTo", amountTo);
		Tools.putToBundle(outBundle, "operationTypeID", operationTypeID);
		Tools.putToBundle(outBundle, "transactionTypeID", transactionTypeID);
		Tools.putToBundle(outBundle, "checkedCategoryItems", checkedCategoryItems);
		if (accountsList != null)
			Tools.putToBundle(outBundle, "accountsList", Tools.convertCheckBoxListToHashMap(accountsList));
		if (currencyList != null)
			Tools.putToBundle(outBundle, "currencyList", Tools.convertCheckBoxListToHashMap(currencyList));
		if (statusList != null)
			Tools.putToBundle(outBundle, "statusList", Tools.convertCheckBoxListToHashMap(statusList));
		if (paymentMethodList != null)
			Tools.putToBundle(outBundle, "paymentMethodList", Tools.convertCheckBoxListToHashMap(paymentMethodList));
		Tools.putToBundleIntegerArray(outBundle, "checkedCategoryIDs", checkedCategoryIDs);
	}
	
	void getValuesFromBundle(Bundle inBundle) {
		if (!inBundle.isEmpty()) {
			fromDatePeriod = Tools.getDateFromBundle(inBundle, "fromDatePeriod");
			toDatePeriod = Tools.getDateFromBundle(inBundle, "toDatePeriod");
			amountFrom = Tools.getStringFromBundle(inBundle, "amountFrom");
			amountTo = Tools.getStringFromBundle(inBundle, "amountTo");
			operationTypeID = Tools.getIntegerFromBundle0(inBundle, "operationTypeID");
			transactionTypeID = Tools.getIntegerFromBundle0(inBundle, "transactionTypeID");
			checkedCategoryIDs = Tools.getIntegerArrayListFromBundle(inBundle, "checkedCategoryIDs");
			checkedCategoryItems = (HashMap<Integer, Integer>) Tools.getSerializableFromBundle(inBundle, "checkedCategoryItems");

			if ((accountsList == null) && (inBundle.containsKey("accountsList"))) {
				Cursor cursor = DBTools.createCursor(getBaseContext(), createAccountQuery());
				accountsList = Tools.cretaCheckBoxList(cursor, AccountTableMetaData._ID, AccountTableMetaData.NAME);
			}
			accountsList = Tools.getValuesFromHashMap((HashMap<Integer, Integer>) Tools.getSerializableFromBundle(inBundle, "accountsList"), accountsList);

			if ((currencyList == null) && (inBundle.containsKey("currencyList"))) {
				Cursor cursor = DBTools.createCursor(getBaseContext(), createCurrencyQuery());
				currencyList = Tools.cretaCheckBoxList(cursor, CurrencyTableMetaData._ID, CurrencyTableMetaData.NAME);
			}
			currencyList = Tools.getValuesFromHashMap((HashMap<Integer, Integer>) Tools.getSerializableFromBundle(inBundle, "currencyList"), currencyList);

			if ((statusList == null) && (inBundle.containsKey("statusList"))) {
				Cursor cursor = DBTools.createCursor(getBaseContext(), createStatusQuery());
				statusList = Tools.cretaCheckBoxList(cursor, TransactionStatusTableMetaData._ID, TransactionStatusTableMetaData.NAME);
			}
			statusList = Tools.getValuesFromHashMap((HashMap<Integer, Integer>) Tools.getSerializableFromBundle(inBundle, "statusList"), statusList);

			if ((paymentMethodList == null) && (inBundle.containsKey("paymentMethodList"))) {
				Cursor cursor = DBTools.createCursor(getBaseContext(), createPaymentMethodQuery());
				paymentMethodList = Tools.cretaCheckBoxList(cursor, PaymentMethodsTableMetaData._ID, PaymentMethodsTableMetaData.NAME);
			}
			paymentMethodList = Tools.getValuesFromHashMap((HashMap<Integer, Integer>) Tools.getSerializableFromBundle(inBundle, "paymentMethodList"), paymentMethodList);
		}
	}
}
