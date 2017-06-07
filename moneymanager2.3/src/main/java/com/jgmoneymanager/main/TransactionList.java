package com.jgmoneymanager.main;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.google.android.vending.licensing.LicenseChecker;
import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CategoryTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VTransactionViewMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyListActivity;
import com.jgmoneymanager.services.CurrRatesSrv;
import com.jgmoneymanager.services.CurrencySrv;
import com.jgmoneymanager.services.TransactionSrv;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.LocalTools;
import com.jgmoneymanager.tools.RefreshTransactionListTotalTask;
import com.jgmoneymanager.tools.Tools;

import java.util.Date;

public class TransactionList extends MyListActivity {

	private Cursor cursor;
	private String currentSelection = null;
	private long transferID;
	String query;
	long selectedAccountID = 0;
	//public static int dateFilterType;
	int defaultCurrency;
	boolean dontRefreshList;//this value is used for if after first this screen's opening on category filter screen 
						//pressed cancel button then don't rferesh list;
	
	Bundle transactionFilterValues;
	
    //private LicenseChecker mLicenseCheckerCallback;
    private LicenseChecker mChecker;
    // A handler on the UI thread.
    //private Handler mHandler;
    boolean isEdited = false;

	private AdView adView;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.transactionlist2);
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
				this.findViewById(R.id.btTRFilter).setVisibility(View.GONE);
				Bundle bundle = getIntent().getExtras();
				//Eger pencere adi gonderilibse ona yerie qoyaq
				if (bundle.containsKey(Constants.paramTitle))
					((TextView)findViewById(R.id.tvATTitle)).setText(bundle.getString(Constants.paramTitle));
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
		if (!Tools.proVersionExists(this)) {
			 adView = new AdView(this, AdSize.BANNER, "ca-app-pub-5995868530154544/6273212514"); 
			 if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
				 LinearLayout layout = (LinearLayout)findViewById(R.id.layTrList); 
				 layout.addView(adView);  
				 AdRequest adRequest = new AdRequest();
				 adView.loadAd(adRequest);
			 }
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
    		mChecker.onDestroy();
			if (adView != null)
				adView.removeAllViews();
				adView.destroy();
    	}
    	catch (Exception ex) {
    		
    	}
    }

	
	public void myClickHandler(View target) {
		switch (target.getId()) {
		case R.id.btTRFilter:
			openFilterScreen(Constants.RequestFilterForTransaction);
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
		if (SettingsScreen.languageChanged) {
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
			
		cursor = TransactionList.this.managedQuery( VTransactionViewMetaData.CONTENT_URI, null,
				currentSelection, null, null);
		String[] from;
		int[] to;
		if (selectedAccountID != 0)
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
		{			
			from = new String[] {
					VTransactionViewMetaData.CATEGORYNAME,
					VTransactionViewMetaData.TRANSDATE,
					VTransactionViewMetaData.DESCRIPTION,
					VTransactionViewMetaData.LBAMOUNT,
					VTransactionViewMetaData.LBALANCE,
					VTransactionViewMetaData.ACCOUNTNAME};
			to = new int[] { R.id.ltTrCategory, R.id.ltTrDate, R.id.ltTrDescription, 
					R.id.ltTrAmount, R.id.ltTrBalance, R.id.ltTrAccount };
		}
		SimpleCursorAdapter notes = new MyListAdapter(cursor, TransactionList.this, R.layout.listtransactionrow, from, to);
		setListAdapter(notes);

		refreshTotalLabel(cursor);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		editListItem(id, Constants.RequestTransactionUpdate);
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
			refreshTotalLabel(cursor);
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
			//if currency is diffrent then add amount in default currency
			Date transDate = DBTools.getCursorColumnValueDate(cursor, VTransactionViewMetaData.TRANSDATE);
			//if (TransactionList.this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
				//int defaultCurrency = CurrencyEdit.getDefaultCurrencyID(TransactionList.this);
				if (DBTools.getCursorColumnValueInt(cursor, VTransactionViewMetaData.CURRID) != defaultCurrency) {
					Double amountInDefault = CurrRatesSrv.convertAmount(context, 
							DBTools.getCursorColumnValueDouble(cursor, VTransactionViewMetaData.AMOUNT), 
							DBTools.getCursorColumnValueInt(cursor, VTransactionViewMetaData.CURRID), 
							defaultCurrency, transDate, true);
					tvAmount.setText(tvAmount.getText().toString() + "(" + Tools.formatDecimal(amountInDefault) + 
							CurrencySrv.getDefaultCurrencySign(TransactionList.this) + ")");
				}
			//}
			TextView tvBalance = (TextView) view.findViewById(R.id.ltTrBalance);
			if (DBTools.getCursorColumnValueFloat(cursor, VTransactionViewMetaData.BALANCE) < 0)
			{
				tvBalance.setTextColor(getResources().getColor(R.color.DarkRed));
			} else {
				tvBalance.setTextColor(getResources().getColor(R.color.Black));
			}
			TextView tvTransDate = (TextView) view.findViewById(R.id.ltTrDate);
			tvTransDate.setText(Tools.DateToString(transDate, Constants.DateFormatUser));			
			return view;
		}
	}
	
	private void refreshTotalLabel(Cursor cursor)
	{
		TextView tvTotal = (TextView)findViewById(R.id.tvTrTotal);
		RefreshTransactionListTotalTask rfTask = new RefreshTransactionListTotalTask(TransactionList.this, 
			currentSelection, tvTotal, selectedAccountID);
		rfTask.execute(" ");
	}
}
