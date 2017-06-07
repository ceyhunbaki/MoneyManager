package com.jgmoneymanager.main;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.AccountTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VAccountsViewMetaData;
import com.jgmoneymanager.entity.MyListActivity;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.LocalTools;
import com.jgmoneymanager.tools.Tools;

public class AccountList extends MyListActivity {

	private Cursor cursor;
	private final static boolean showDisabled = true;

	//MyApplicationLocal myApp;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.accountlist);
		LocalTools.onResumeEvents(this);
		
		if (getIntent().getAction().equals(Intent.ACTION_PICK))
			this.setTitle(R.string.selectAccount);

		refreshList(showDisabled);	
		
		/*myApp = (MyApplicationLocal)getApplication();
		myApp.setAskPassword(false);*/	
	}
	
	/*@Override
	protected void onResume() {
		super.onResume();
		Tools.onResumeEvents(this);
	}*/

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (!getIntent().getAction().equals(Intent.ACTION_PICK))
		{
			//editListItem(id);
			Intent intent = new Intent(getBaseContext(), TransactionList.class);
			intent.setAction(Constants.ActionViewTransactionsByAccount);
			Uri uri = ContentUris.withAppendedId(AccountTableMetaData.CONTENT_URI, id);
			intent.setData(uri);
			startActivityForResult(intent, Constants.RequestTransactionByAccount);
		}
		else {
			Uri uri = ContentUris.withAppendedId(AccountTableMetaData.CONTENT_URI, id);
			setResult(RESULT_OK, new Intent().setData(uri));
			finish();
		}
	}

	public class MyListAdapter extends SimpleCursorAdapter {

		public MyListAdapter(Cursor cursor, Context context, int rowId,
				String[] from, int[] to) {
			super(context, rowId, cursor, from, to);
		}

		@Override
		public void notifyDataSetChanged() {
			super.notifyDataSetChanged();	
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = super.getView(position, convertView, parent);
			Cursor cursor = (Cursor) super.getItem(position);

			TextView tvBalance = (TextView) view.findViewById(R.id.l2column2);
			Double balance = DBTools.getCursorColumnValueDouble(cursor, VAccountsViewMetaData.BALANCE);
			if (balance < 0)
				tvBalance.setTextColor(getResources().getColor(R.color.DarkRed));
			else
				tvBalance.setTextColor(getResources().getColor(R.color.Black));
			tvBalance.setText(Tools.getFullAmountText(balance, DBTools.getCursorColumnValue(cursor, VAccountsViewMetaData.CURRSIGN), true));

			TextView tvName = (TextView) view.findViewById(R.id.l2column1);			
			if (DBTools.getCursorColumnValueInt(cursor, VAccountsViewMetaData.ISDEFAULT) == 1)
				tvName.setText(DBTools.getCursorColumnValue(cursor, VAccountsViewMetaData.ACCOUNTNAME) + "*");
			if (DBTools.getCursorColumnValueInt(cursor, VAccountsViewMetaData.STATUS) == 0)
				tvName.setTextColor(getResources().getColor(R.color.DarkGray));
			else
				tvName.setTextColor(getResources().getColor(R.color.Black));

			return view;
		}
	}

	private void refreshList(boolean showDisabled)
    {
    	String condition = null;
    	if (!showDisabled)
        	condition= VAccountsViewMetaData.STATUS + " = " + String.valueOf(Constants.Status.Enabled.index());
    	cursor = getContentResolver().query(VAccountsViewMetaData.CONTENT_URI,
    			new String[] {VAccountsViewMetaData._ID, VAccountsViewMetaData.ACCOUNTNAME, VAccountsViewMetaData.BALANCE, 
    								VAccountsViewMetaData.CURRID, VAccountsViewMetaData.ISDEFAULT, VAccountsViewMetaData.STATUS, VAccountsViewMetaData.CURRSIGN}, 
    			condition, null, null);		
		String[] from = new String[] { VAccountsViewMetaData.ACCOUNTNAME, VAccountsViewMetaData.BALANCE };
		int[] to = new int[] { R.id.l2column1, R.id.l2column2 };
		SimpleCursorAdapter notes = new MyListAdapter(cursor, this, R.layout.list2columnrow, from, to);
		setListAdapter(notes);   
    }

	/*@Override
	protected void onStart() {
		super.onStart();	
		EasyTracker.getInstance(this).activityStart(this);
	}

	@Override
	protected void onStop() {
		super.onStop();	
		EasyTracker.getInstance(this).activityStop(this);

		myApp.setAskPassword(true);
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		
		if (myApp.getAskPassword() && SetPassword.passwordRequired(AccountList.this)) {
        	Intent intent = new Intent(AccountList.this, StartupPassword.class);
        	intent.setAction(Constants.ActionStartupPassword);
        	startActivityForResult(intent, Constants.RequestPasswordInStartup);
        }       
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		myApp.setAskPassword(false);
	}*/
}