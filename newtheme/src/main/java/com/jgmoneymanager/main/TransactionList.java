package com.jgmoneymanager.main;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CategoryTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VTransactionViewMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.services.CurrRatesSrv;
import com.jgmoneymanager.services.CurrencySrv;
import com.jgmoneymanager.services.TransactionSrv;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.LocalTools;
import com.jgmoneymanager.tools.RefreshTransactionListTotalTask;
import com.jgmoneymanager.tools.Tools;

import java.util.Date;

public class TransactionList extends MyActivity {

	private String currentSelection = null;
	private long transferID;
	String query;
	long selectedAccountID = 0;
	int defaultCurrency;
	boolean dontRefreshList;//this value is used for if after first this screen's opening on category filter screen 
						//pressed cancel button then don't rferesh list;
	
	Bundle transactionFilterValues;
    boolean isEdited = false;

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
		View child = inflater.inflate(R.layout.transactionlist2, null);
		mainLayout.addView(child, params);

		LocalTools.onResumeEvents(this);
		
		defaultCurrency = CurrencySrv.getDefaultCurrencyID(TransactionList.this);
		
		if (savedInstanceState != null) {
			transferID = Tools.getLongFromBundle0(savedInstanceState, "transferID");
			selectedAccountID = Tools.getLongFromBundle0(savedInstanceState, "selectedAccountID");
			transactionFilterValues = Tools.getBundleFromBundle(savedInstanceState, "transactionFilterValues");
			query = Tools.getStringFromBundle(savedInstanceState, "query");
			dontRefreshList = Tools.getBooleanFromBundle0(savedInstanceState, "dontRefreshList");
			if (!dontRefreshList)
				refreshTransactionList(selectedAccountID, query);
		}
		else {
			if (!getIntent().getAction().equals(Constants.ActionViewAllTransactions)) {
				dontRefreshList = false;
				//this.findViewById(R.id.btTRFilter).setVisibility(View.GONE);
				Bundle bundle = getIntent().getExtras();
				//Eger pencere adi gonderilibse ona yerie qoyaq
				if (bundle.containsKey(Constants.paramTitle))
					setTitle(bundle.getString(Constants.paramTitle));
				//filtrleri yoxlayaq
				if (bundle.containsKey(Constants.paramTransferID))
					transferID = bundle.getLong(Constants.paramTransferID);
				if (bundle.containsKey(Constants.paramAccountID)) {
					selectedAccountID = bundle.getLong(Constants.paramAccountID);
				}
				if (bundle.containsKey(Constants.paramFromDate)) {
					String fromDate = bundle.getString(Constants.paramFromDate);
					String toDate = bundle.getString(Constants.paramToDate);
					query = " and " + VTransactionViewMetaData.TRANSDATE + " >= '" + fromDate
							+ "' and " + VTransactionViewMetaData.TRANSDATE + " <= '" + toDate + "'";
				}
				if (bundle.containsKey(Constants.paramCategory)) {
					//Category Report olarsa
					selectedAccountID = 0;
					String categid = String.valueOf(bundle.getLong(Constants.paramCategory));
					query += " and " + VTransactionViewMetaData.CATEGORYID + " in (select " 
							+ CategoryTableMetaData._ID + " from " + CategoryTableMetaData.TABLE_NAME 
							+ " where " + CategoryTableMetaData._ID + " = " + categid + " or "
							+ CategoryTableMetaData.MAINID + " = " + categid + ") ";
				}
				if (bundle.containsKey(Constants.reportType)) {
					if (bundle.getInt(Constants.reportType) == Constants.TransFTransaction.Income.index())
						query += " and " + VTransactionViewMetaData.TRANSTYPE + " = '1' ";
					else if (bundle.getInt(Constants.reportType) == Constants.TransFTransaction.Expence.index())
						query += " and " + VTransactionViewMetaData.TRANSTYPE + " = '-1' ";
					/*query += " and not exists (select 1 from " + TransferTableMetaData.TABLE_NAME 
							+ " ta where ta." + TransferTableMetaData._ID + " = " + VTransactionViewMetaData.TRANSFERID 
							+ " and " + TransferTableMetaData.FIRSTACCOUNTID + " is not null and " 
							+ TransferTableMetaData.SECONDACCOUNTID + " is not null)";*/
				}
				refreshTransactionList(selectedAccountID, query);
			}
			else {
				dontRefreshList = true;
				openFilterScreen(Constants.RequestTransactionFirstFilter);
			}
		}

		
		// Create the adView 
		if (/*!Tools.proVersionExists(this) &&*/ (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)) {
			MobileAds.initialize(getApplicationContext(), "ca-app-pub-5995868530154544/6273212514");
			adView = new AdView(this);
			adView.setAdSize(AdSize.SMART_BANNER);
			adView.setAdUnitId("ca-app-pub-5995868530154544/6273212514");
			LinearLayout layout = (LinearLayout) findViewById(R.id.layTrList);
			layout.addView(adView);
			AdRequest adRequest = new AdRequest.Builder().build();
			adView.loadAd(adRequest);
//			 }
		}
	}

    @Override
	protected void onSaveInstanceState(Bundle outState) {
    	Tools.putToBundle(outState, "transactionFilterValues", transactionFilterValues);
    	Tools.putToBundle(outState, "transferID", transferID);
    	Tools.putToBundle(outState, "selectedAccountID", selectedAccountID);
    	Tools.putToBundle(outState, "query", query);
    	Tools.putToBundle(outState, "dontRefreshList", dontRefreshList);
		super.onSaveInstanceState(outState);
	}
    

	@Override
    protected void onDestroy() {
		super.onDestroy();
    	try {
			if (adView != null)
				adView.destroy();
    	}
    	catch (Exception ex) {
    		
    	}
    }

	void openFilterScreen(int request) {
		Intent intent = new Intent(getBaseContext(), TransactionFilter.class);
		intent.setAction(Intent.ACTION_PICK);
		Bundle bundle = new Bundle();
		if (transactionFilterValues != null)
			bundle = transactionFilterValues;
		//bundle.putLong(Constants.selectedAccount, selectedAccountID);
		intent.putExtras(bundle);
		startActivityForResult(intent, request);
	}
	
	@Override
	public void onBackPressed() {
		if (isEdited)
			setResult(RESULT_OK);
		super.onBackPressed();
	}

	@Override	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (SettingsMain.languageChanged) {
			restartActivity();
		}
		if (resultCode == RESULT_OK)
		{
			dontRefreshList = false;
			if ((requestCode == Constants.RequestFilterForTransaction)
					|| (requestCode == Constants.RequestTransactionFirstFilter))
			{		
				transactionFilterValues = data.getExtras();
				query = transactionFilterValues.getString(Constants.query);
			} else if (requestCode == Constants.RequestTransactionUpdate)
				isEdited = true;
			refreshTransactionList(selectedAccountID, query);
		}
		if ((resultCode == RESULT_CANCELED) && (requestCode == Constants.RequestTransactionFirstFilter))
			this.finish();
	}
	
	private void restartActivity() {
	    Intent intent = getIntent();
	    finish();
	    startActivity(intent);
	}

	private void refreshTransactionList(long accountId, String query)
	{
		currentSelection = null;
		/*if ((accountId == 0) && (transferID == 0) && (!AccountList.showDisabled)
				&& (!getIntent().getAction().equals(Constants.ActionViewTransactionsFromReport)))
			currentSelection = VTransactionViewMetaData.ACCOUNTSTATUS + " = 1 ";
		else 
		{*/
			currentSelection = " 1=1 ";	
			if (accountId != 0)
				currentSelection += " and " + VTransactionViewMetaData.ACCOUNTID + " = " + String.valueOf(accountId);
			else if (getIntent().getAction().equals(Constants.ActionViewTransactionsFromReport))
				currentSelection += " and " + VTransactionViewMetaData.ISTRANSFER + " =0 ";
			if (transferID != 0)
				currentSelection += " and " + VTransactionViewMetaData.TRANSFERID + " = " + String.valueOf(transferID);
		//}
		
		if (query != null)
		{
			if (currentSelection.length() > 0)
				currentSelection += query;
			else 
				currentSelection = query;
		}
			
		Cursor cursor = TransactionList.this.managedQuery( VTransactionViewMetaData.CONTENT_URI, null,
				currentSelection, null, null);
		String[] from;
		int[] to;
		/*if (selectedAccountID != 0)
		{
			from = new String[] {
					VTransactionViewMetaData.CATEGORYNAME,
					VTransactionViewMetaData.TRANSDATE,
					VTransactionViewMetaData.DESCRIPTION,
					VTransactionViewMetaData.LBAMOUNT,
					VTransactionViewMetaData.LBALANCE };
			to = new int[] { R.id.ltTrCategory, R.id.ltTrDate, R.id.ltTrDescription, R.id.ltTrAmount, R.id.ltTrBalance };
		}
		else
		{*/
			from = new String[] {
					VTransactionViewMetaData.CATEGORYNAME,
					VTransactionViewMetaData.TRANSDATE,
					VTransactionViewMetaData.DESCRIPTION,
					VTransactionViewMetaData.AMOUNT,
					VTransactionViewMetaData.BALANCE,
					VTransactionViewMetaData.ACCOUNTNAME,
					VTransactionViewMetaData.CURRENCYSIGN};
			to = new int[] { R.id.ltTrCategory, R.id.ltTrDate, R.id.ltTrDescription, 
					R.id.ltTrAmount, R.id.ltTrBalance, R.id.ltTrAccount };
		//}
		SimpleCursorAdapter notes = new MyListAdapter(cursor, TransactionList.this, R.layout.listtransactionrow, from, to);
		//cursor.close();

		ListView listView = (ListView) findViewById(R.id.trListView);
		listView.setAdapter(notes);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				editListItem(id, Constants.RequestTransactionUpdate);
			}
		});

		refreshTotalLabel();
	}

	public void editListItem(long id, int requestCode) {
		int transactionType = TransactionSrv.getTransactionType(TransactionList.this, id, true);
		if (transactionType == Constants.TransFOperType.Transfer.index())
			DialogTools.toastDialog(TransactionList.this, R.string.msgUpdateFromTransfer, Toast.LENGTH_LONG);
		else if (transactionType == Constants.TransFOperType.RpTransaction.index())
			DialogTools.toastDialog(TransactionList.this, R.string.msgUpdateFromRpTransactions, Toast.LENGTH_LONG);
		else {
			Intent intent = new Intent(getBaseContext(), TransactionEdit.class);
			intent.setAction(Intent.ACTION_EDIT);
			Bundle bundle = new Bundle();
			bundle.putString(VTransactionViewMetaData._ID, String.valueOf(id));
			intent.putExtras(bundle);
			startActivityForResult(intent, requestCode);
		}
	}

	public class MyListAdapter extends SimpleCursorAdapter {

		Context context;

		public MyListAdapter(Cursor cursor, Context context, int rowId,
				String[] from, int[] to) {
			super(context, rowId, cursor, from, to);
			this.context = context;
		}

		@Override
		public void notifyDataSetChanged() {
			super.notifyDataSetChanged();
			refreshTotalLabel();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = super.getView(position, convertView, parent);
			Cursor cursor = (Cursor) super.getItem(position);
			if (view == null) {
				LayoutInflater inflater = LayoutInflater.from(context);
				view = inflater.inflate(R.layout.listtransactionrow, null);
			}
			if (position % 2 == 0)
				view.setBackgroundColor(getResources().getColor(
						R.color.AntiqueWhite));
			else/**/
				view.setBackgroundColor(getResources().getColor(R.color.White));
			TextView tvAmount = (TextView) view.findViewById(R.id.ltTrAmount);
			if (DBTools.getCursorColumnValueInt(cursor, VTransactionViewMetaData.TRANSTYPE) == Constants.TransactionTypeExpence) {
				tvAmount.setTextColor(getResources().getColor(R.color.Red));
			} else {
				tvAmount.setTextColor(getResources().getColor(R.color.Green));
			}
			Double amount = DBTools.getCursorColumnValueDouble(cursor, VTransactionViewMetaData.AMOUNT);
			String currSign = DBTools.getCursorColumnValue(cursor, VTransactionViewMetaData.CURRENCYSIGN);
			tvAmount.setText(Tools.getFullAmountText(amount, currSign, true));

			//if currency is diffrent then add amount in default currency
			Date transDate = DBTools.getCursorColumnValueDate(cursor, VTransactionViewMetaData.TRANSDATE);
			//if (TransactionList.this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
				//int defaultCurrency = CurrencyEdit.getDefaultCurrencyID(TransactionList.this);
				if (DBTools.getCursorColumnValueInt(cursor, VTransactionViewMetaData.CURRID) != defaultCurrency) {
					Double amountInDefault = CurrRatesSrv.convertAmount(context, 
							DBTools.getCursorColumnValueDouble(cursor, VTransactionViewMetaData.AMOUNT), 
							DBTools.getCursorColumnValueInt(cursor, VTransactionViewMetaData.CURRID), 
							defaultCurrency, transDate, true);
					tvAmount.setText(tvAmount.getText().toString() + "(" + Tools.getFullAmountText(amountInDefault, CurrencySrv.getDefaultCurrencySign(TransactionList.this), false) + ")");
				}
			//}
			TextView tvBalance = (TextView) view.findViewById(R.id.ltTrBalance);
			Double balance = DBTools.getCursorColumnValueDouble(cursor, VTransactionViewMetaData.BALANCE);
			if (balance.compareTo(0d) < 0)
			{
				tvBalance.setTextColor(getResources().getColor(R.color.DarkRed));
			} else {
				tvBalance.setTextColor(getResources().getColor(R.color.Black));
			}
			tvBalance.setText("(" + Tools.formatDecimal(balance) + ")");

			TextView tvTransDate = (TextView) view.findViewById(R.id.ltTrDate);
			tvTransDate.setText(Tools.DateToString(transDate, Constants.DateFormatUser));
			//cursor.close();
			return view;
		}
	}
	
	private void refreshTotalLabel()
	{
		TextView tvTotal = (TextView)findViewById(R.id.tvTrTotal);
		RefreshTransactionListTotalTask rfTask = new RefreshTransactionListTotalTask(TransactionList.this, 
			currentSelection, tvTotal, selectedAccountID);
		rfTask.execute(" ");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (getIntent().getAction().equals(Constants.ActionViewAllTransactions))
			getMenuInflater().inflate(R.menu.menu_filter, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			openFilterScreen(Constants.RequestFilterForTransaction);
		}
		return super.onOptionsItemSelected(item);
	}
}
