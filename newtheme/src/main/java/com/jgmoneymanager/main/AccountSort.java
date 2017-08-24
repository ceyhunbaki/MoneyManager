/*
 * Copyright (C) 2013 Jeyhun Gasimov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jgmoneymanager.main;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ericharlow.DragNDrop.DragListener;
import com.ericharlow.DragNDrop.DragNDropAdapter;
import com.ericharlow.DragNDrop.DragNDropListView;
import com.ericharlow.DragNDrop.DropListener;
import com.ericharlow.DragNDrop.RemoveListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.AccountTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransactionsTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.services.AccountSrv;
import com.jgmoneymanager.services.TransferSrv;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import java.util.ArrayList;

public class AccountSort extends MyActivity {

	private ArrayList<String> content;
	Cursor cursor;

	private long defaultAccountID;
	static boolean resultOK = false;
	/**
	 * If order of accounts changed it changes to true
	 */
	boolean orderChanged = false;

	private AdView adView;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.accountsort);
		//super.setCustomTitle(R.string.accounts);

		setContentView(R.layout.main_activity);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

		LinearLayout mainLayout = (LinearLayout) findViewById(R.id.content_main_layout);
		LayoutInflater inflater = (LayoutInflater)      this.getSystemService(LAYOUT_INFLATER_SERVICE);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		View child = inflater.inflate(R.layout.accountsort, null);
		mainLayout.addView(child, params);


		if (savedInstanceState == null) {
			reloadContentList();
			defaultAccountID = AccountSrv.getDefultAccountID(AccountSort.this, null);
        }
        else {
			cursor = getContentResolver().query(AccountTableMetaData.CONTENT_URI, null, null, null, AccountTableMetaData.ISDEFAULT + " desc, " + AccountTableMetaData.SORTORDER);
			content = Tools.getStringArrayListFromBundle(savedInstanceState, "content");
        	defaultAccountID = Tools.getLongFromBundle0(savedInstanceState, "defaultAccountID");
        	orderChanged = Tools.getBooleanFromBundle0(savedInstanceState, "orderChanged");
        	resultOK = Tools.getBooleanFromBundle0(savedInstanceState, "resultOK");
        }
        
        refreshList();

		// Create the adView
		/*if (!Tools.proVersionExists(this))*/ {
			MobileAds.initialize(getApplicationContext(), "ca-app-pub-5995868530154544/3468276114");
			adView = new AdView(this);
			adView.setAdSize(AdSize.SMART_BANNER);
			adView.setAdUnitId("ca-app-pub-5995868530154544/3468276114");
			LinearLayout layout = (LinearLayout) findViewById(R.id.onlyListAdsLayout);
			layout.addView(adView);
			AdRequest adRequest = new AdRequest.Builder().build();
			adView.loadAd(adRequest);
		}

		registerForContextMenu(findViewById(R.id.drListView));
    }

	@Override
	protected void onDestroy() {
		try {
			super.onDestroy();
			if (adView != null)
				adView.destroy();
		}
		catch (Exception ex) {

		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Tools.putToBundle(outState, "content", content);
		Tools.putToBundle(outState, "defaultAccountID", defaultAccountID);
		Tools.putToBundle(outState, "orderChanged", orderChanged);
		Tools.putToBundle(outState, "resultOK", resultOK);
		super.onSaveInstanceState(outState);
	}

	private void refreshList() {
		ListView listView = (ListView) findViewById(R.id.drListView);

		listView.setAdapter(new MyListAdapter(this, new int[]{R.layout.dragitem_new}, new int[]{R.id.drText}, content));

        if (listView instanceof DragNDropListView) {
        	((DragNDropListView) listView).setDropListener(mDropListener);
        	((DragNDropListView) listView).setRemoveListener(mRemoveListener);
        	((DragNDropListView) listView).setDragListener(mDragListener);
        }
        
        listView.setBackgroundColor(getResources().getColor(R.color.White));
	}
	
	class MyListAdapter extends DragNDropAdapter {

		//int[] itemIDs; 
		
		public MyListAdapter(Context context, int[] itemLayouts, int[] itemIDs,
				ArrayList<String> content) {
			super(context, itemLayouts, itemIDs, content);
			//this.itemIDs = itemIDs;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = super.getView(position, convertView, parent);
			final long accountId = AccountSrv.getAccountIDByName(AccountSort.this, content.get(position));
			
			TextView accountName = (TextView) view.findViewById(R.id.drText);
			if (AccountSrv.isAccountVisible(AccountSort.this, accountId))
				accountName.setTextColor(getResources().getColor(R.color.Black));
			else
				accountName.setTextColor(getResources().getColor(R.color.DarkGray));
			
			return view;
		}
		
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		menu.setHeaderIcon(R.drawable.ic_menu_manage);
		menu.add(R.string.menuEdit);
		menu.add(R.string.menuDelete);
		menu.add(R.string.menuSetDefault);
		cursor.moveToPosition(info.position);
		menu.setHeaderTitle(DBTools.getCursorColumnValue(cursor, AccountTableMetaData.NAME));
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		if (item.getTitle().toString().equals(getResources().getString(R.string.menuEdit))) {
			editListItem(info.position);
		}
		else if (item.getTitle().toString().equals(getResources().getString(R.string.menuDelete))) {
			if (AccountSrv.isDefultAccountID(getBaseContext(), info.id))
			{
				DialogTools.toastDialog(getBaseContext(), getResources().getString(R.string.msgDeleteDefaultAccount), Toast.LENGTH_LONG);
			}
			else
			{
				Command cmd = new Command() {
					@Override
					public void execute() {
						cursor.moveToPosition(info.position);
						String accountID = DBTools.getCursorColumnValue(cursor, AccountTableMetaData._ID);
						getContentResolver().delete(Uri.withAppendedPath(AccountTableMetaData.CONTENT_URI, accountID), null, null);
						getContentResolver().delete(TransactionsTableMetaData.CONTENT_URI, TransactionsTableMetaData.ACCOUNTID + " = " + accountID, null);
						TransferSrv.deleteTransfersFromAccount(AccountSort.this, accountID);
						AccountSort.resultOK = true;
						reloadContentList();
						refreshList();
					}
				};
				AlertDialog deleteDialog = DialogTools.confirmDialog(AccountSort.this, cmd, R.string.msgConfirm, R.string.msgDeleteAccount);
				deleteDialog.show();
			}
		}
		else if (item.getTitle().toString().equals(getResources().getString(R.string.menuSetDefault))) {
			if (info.id == defaultAccountID) {
				DialogTools.toastDialog(AccountSort.this, R.string.msgChooseAnotherDefaultAccount, Toast.LENGTH_LONG);
			}
			else {
				AccountEdit.setDefaultAccount(getBaseContext(), info.id);
				AccountSort.resultOK = true;
				reloadContentList();
				refreshList();
			}
		}
		return false;
	}

	void reloadContentList() {
		cursor = getContentResolver().query(AccountTableMetaData.CONTENT_URI, null, null, null, AccountTableMetaData.ISDEFAULT + " desc, " + AccountTableMetaData.SORTORDER);
		content = new ArrayList<String>(cursor.getCount());
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			content.add(DBTools.getCursorColumnValue(cursor, AccountTableMetaData.NAME));
		}
	}
	
	private void sortAZ() {
		Cursor cursor = getContentResolver().query(AccountTableMetaData.CONTENT_URI, null, null, null,
				AccountTableMetaData.ISDEFAULT + " desc, " + AccountTableMetaData.STATUS + " desc, " + AccountTableMetaData.NAME);
        content = new ArrayList<String>(cursor.getCount());
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
        	content.add(DBTools.getCursorColumnValue(cursor, AccountTableMetaData.NAME));
        }
        refreshList();
		//ardicilliq deyiwiklik olubsa cixiwda save soruwaq
        orderChanged = true;
	}
	
	private void saveToDB(Context context, ArrayList<String> content){
		ContentValues values = new ContentValues();		
		for (int i = 0; i < content.size(); i++) {
			values.clear();
			values.put(AccountTableMetaData.SORTORDER, i);
			context.getContentResolver().update(AccountTableMetaData.CONTENT_URI, values, 
					AccountTableMetaData.NAME + " = '" + content.get(i) + "'", null);
		}
	}

	private DropListener mDropListener = 
		new DropListener() {
        public void onDrop(int from, int to) {
			ListView listView = (ListView) findViewById(R.id.drListView);
        	ListAdapter adapter = listView.getAdapter();
        	if (adapter instanceof DragNDropAdapter) {
        		((DragNDropAdapter)adapter).onDrop(from, to);
				listView.invalidateViews();
        	}
        }
    };
    
    private RemoveListener mRemoveListener =
        new RemoveListener() {
        public void onRemove(int which) {
			ListView listView = (ListView) findViewById(R.id.drListView);
        	ListAdapter adapter = listView.getAdapter();
        	if (adapter instanceof DragNDropAdapter) {
        		((DragNDropAdapter)adapter).onRemove(which);
				listView.invalidateViews();
        	}
        }
    };
    
    private DragListener mDragListener =
    	new DragListener() {

			public void onStartDrag(View itemView) {
				itemView.setVisibility(View.INVISIBLE);
				itemView.setBackgroundColor(getResources().getColor(R.color.LightBlue));
				ImageView iv = (ImageView)itemView.findViewById(R.id.drImage);
				if (iv != null) iv.setVisibility(View.INVISIBLE);
				//ardicilliq deyiwiklik olubsa cixiwda save soruwaq
		        orderChanged = true;
			}

			public void onStopDrag(View itemView) {
				itemView.setVisibility(View.VISIBLE);
				itemView.setBackgroundColor(getResources().getColor(R.color.White));//defaultBackgroundColor);
				ImageView iv = (ImageView)itemView.findViewById(R.id.drImage);
				if (iv != null) iv.setVisibility(View.VISIBLE);
			}

			@Override
			public void onDrag(int x, int y, ListView listView) {
				
			}
    	
    };
    
	public void myClickHandler(View target) {
		switch (target.getId()) {
			case R.id.drEdit:
			openContextMenu(target);
			break;
		case R.id.btDRAddNew:
			Intent intent = new Intent(AccountSort.this, AccountEdit.class);
			intent.setAction(Intent.ACTION_INSERT);
			startActivityForResult(intent, Constants.RequestAccountInsert);
			break;
		case R.id.btDRAZ:
			sortAZ();
			break;
		default:
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if ((resultCode == RESULT_OK) && 
				((requestCode == Constants.RequestAccountUpdate) || (requestCode == Constants.RequestAccountInsert))) {
			AccountSort.resultOK = true;
			reloadContentList();
			refreshList();
		}
	}
	
	private void editListItem(int cursorPosition) {
		cursor.moveToPosition(cursorPosition);
		Intent intent = new Intent(AccountSort.this, AccountEdit.class);
		Bundle bundle = new Bundle();
		bundle.putString(Constants.UpdateMode, Constants.Update);
		bundle.putString(AccountTableMetaData.NAME, DBTools.getCursorColumnValue(cursor, AccountTableMetaData.NAME));
		bundle.putString(AccountTableMetaData.INITIALBALANCE, DBTools.getCursorColumnValue(cursor, AccountTableMetaData.INITIALBALANCE));
		bundle.putString(AccountTableMetaData.DESCRIPTION, DBTools.getCursorColumnValue(cursor, AccountTableMetaData.DESCRIPTION));
		bundle.putString(AccountTableMetaData._ID, DBTools.getCursorColumnValue(cursor, AccountTableMetaData._ID));
		bundle.putLong(AccountTableMetaData.CURRID, DBTools.getCursorColumnValueLong(cursor, AccountTableMetaData.CURRID));
		bundle.putString(AccountTableMetaData.ISDEFAULT, DBTools.getCursorColumnValue(cursor, AccountTableMetaData.ISDEFAULT));
		bundle.putString(AccountTableMetaData.CREATED_DATE, DBTools.getCursorColumnValue(cursor, AccountTableMetaData.CREATED_DATE));
		bundle.putInt(AccountTableMetaData.STATUS, DBTools.getCursorColumnValueInt(cursor, AccountTableMetaData.STATUS));
		intent.putExtras(bundle);
		intent.setAction(Intent.ACTION_EDIT);
		startActivityForResult(intent, Constants.RequestAccountUpdate);
	}

	/**
	 * Opens Dialog to ask user if he want to save sorting or not
	 */
	@Override
	public void onBackPressed() {
		if (orderChanged) {
			Command yesCmd = new Command() {				
				@Override
				public void execute() {
					saveToDB(AccountSort.this, content);
					AccountSort.this.setResult(RESULT_OK);
					AccountSort.this.finish();
				}
			};
			Command noCmd = new Command() {				
				@Override
				public void execute() {
					AccountSort.this.setResult(RESULT_CANCELED);
					AccountSort.this.finish();
				}
			};
			AlertDialog dialog = DialogTools.confirmDialog(AccountSort.this, yesCmd, noCmd, 
					R.string.msgConfirm, getResources().getString(R.string.msgSaveSorting), 
					new String[] {getResources().getString(R.string.Yes), getResources().getString(R.string.No)});
			dialog.show();
		}
		else {
			AccountSort.this.setResult(RESULT_CANCELED);
			AccountSort.this.finish();
		}
	}
}