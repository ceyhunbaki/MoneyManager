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
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.DebtsTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VDebtsViewMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import java.util.Date;

public class DebtsList extends MyActivity {

	final int menuDeleteAll = Menu.FIRST;
	final int menuEdit = menuDeleteAll + 1;
	final int menuDelete = menuDeleteAll + 2;
	final int menuReturned = menuDeleteAll + 3;
	Uri selectedUri;

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
		View child = inflater.inflate(R.layout.debtslist, null);
		mainLayout.addView(child, params);

		refreshList();

		ListView lv = (ListView) findViewById(R.id.debListView);
		lv.setScrollingCacheEnabled(true);
		lv.setCacheColorHint(00000000);
		lv.setBackgroundColor(getResources().getColor(R.color.White));
		registerForContextMenu(lv);

		// Create the adView
		if (!Tools.proVersionExists(this)) {
//			adView = new AdView(this, AdSize.BANNER, "ca-app-pub-5995868530154544/2578328512");
//			LinearLayout layout = (LinearLayout)findViewById(R.id.onlyListAdsLayout);
//			layout.addView(adView);
//			AdRequest adRequest = new AdRequest();
//			adView.loadAd(adRequest);
			MobileAds.initialize(getApplicationContext(), "ca-app-pub-5995868530154544/2578328512");
			adView = new AdView(this);
			adView.setAdSize(AdSize.SMART_BANNER);
			adView.setAdUnitId("ca-app-pub-5995868530154544/2578328512");
			LinearLayout layout = (LinearLayout) findViewById(R.id.onlyListAdsLayout);
			layout.addView(adView);
			AdRequest adRequest = new AdRequest.Builder().build();
			adView.loadAd(adRequest);
		}
	}

	void refreshList() {
		Cursor cursor;
		cursor = getContentResolver().query(VDebtsViewMetaData.CONTENT_URI, null,
				VDebtsViewMetaData.STATUS + " = ?", new String[] {String.valueOf(Constants.Status.Enabled.index())}, null);
		String[] from = new String[] { VDebtsViewMetaData.DESCRIPTION, VDebtsViewMetaData.AMOUNT,
				VDebtsViewMetaData.TRANSDATE, VDebtsViewMetaData.BACKDATE};
		int[] to = new int[] { R.id.ltDebDescription, R.id.ltDebAmount, R.id.ltDebTransDate, R.id.ltDebRetDate};
		SimpleCursorAdapter notes = new MyListAdapter(cursor, this, R.layout.listdebtsrow, from, to);
		((ListView) findViewById(R.id.debListView)).setAdapter(notes);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.setHeaderIcon(R.drawable.ic_menu_manage);
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		Cursor cursor = (Cursor) ((ListView) findViewById(R.id.debListView)).getAdapter().getItem(info.position);
		menu.setHeaderTitle(DBTools.getCursorColumnValue(cursor, DebtsTableMetaData.DESCRIPTION));
		menu.add(0, menuEdit, 0, R.string.menuEdit);
		menu.add(0, menuDelete, 1, R.string.menuDelete);
		menu.add(0, menuReturned, 1, R.string.menuReturned);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();
		Intent intent;
		switch (item.getItemId()) {
		case menuEdit:
			intent = new Intent(this, DebtsEdit.class);
			intent.setAction(Intent.ACTION_EDIT);
			selectedUri = Uri.withAppendedPath(DebtsTableMetaData.CONTENT_URI, String.valueOf(info.id));
			intent.setData(selectedUri);
			startActivityForResult(intent, Constants.RequestDebtsForEdit);
			break;
		case menuDelete:
			Command deleteCommand = new Command() {
				public void execute() {
					DebtsList.this.getContentResolver().delete(DebtsTableMetaData.CONTENT_URI, DebtsTableMetaData._ID + " = "  
							+ String.valueOf(info.id), null);
					refreshList();
				}
			};
			AlertDialog deleteDialog = DialogTools.confirmDialog(
					DebtsList.this, deleteCommand, R.string.msgConfirm,
					R.string.msgDeleteItem);
			deleteDialog.show();
			break;
		case menuReturned:
			ContentValues values = new ContentValues();
			String today = Tools.DateToDBString(Tools.getCurrentDate());
			values.put(DebtsTableMetaData.BACKDATE, today);
			values.put(DebtsTableMetaData.ISGIVEN, 1);
			getContentResolver().update(DebtsTableMetaData.CONTENT_URI, values, 
					DebtsTableMetaData._ID + " = ? and (" + DebtsTableMetaData.BACKDATE + " > ? or " 
						+ DebtsTableMetaData.BACKDATE + " is null) ", 
					new String[] {String.valueOf(info.id), today});
			refreshList();
		default:
			break;
		}
		return true;
	}

	public void myClickHandler(View target) {
		Intent intent;
		Bundle bundle = new Bundle();
		switch (target.getId()) {
		case R.id.btDebBorrowed:
			intent = new Intent(getBaseContext(), DebtsEdit.class);
			intent.setAction(Intent.ACTION_INSERT);
			bundle.putInt(DebtsEdit.pIsGiven, 0);
			intent.putExtras(bundle);
			startActivityForResult(intent, Constants.RequestDebtsForInsert);
			break;
		case R.id.btDebLend:
			intent = new Intent(getBaseContext(), DebtsEdit.class);
			intent.setAction(Intent.ACTION_INSERT);
			bundle.putInt(DebtsEdit.pIsGiven, 1);
			intent.putExtras(bundle);
			startActivityForResult(intent, Constants.RequestDebtsForInsert);
			break;
		case R.id.igDebMenu:
			openContextMenu(target);
			break;
		case R.id.btDebDeleteAll:
			getContentResolver().delete(DebtsTableMetaData.CONTENT_URI, 
					DebtsTableMetaData.BACKDATE + " is not null and " 
							+ DebtsTableMetaData.BACKDATE + " < '" + Tools.DateToDBString(Tools.getCurrentDate()) + "' ", 
					null);
			refreshList();
			break;
		default:
			break;	
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK)
			refreshList();
	}

	public class MyListAdapter extends SimpleCursorAdapter {

		Context context;

		public MyListAdapter(Cursor cursor, Context context, int rowId,
				String[] from, int[] to) {
			super(context, rowId, cursor, from, to, 0);
			this.context = context;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = super.getView(position, convertView, parent);
			Cursor cursor = (Cursor) super.getItem(position);
			if (view == null) {
				LayoutInflater inflater = LayoutInflater.from(context);
				view = inflater.inflate(R.layout.listdebtsrow, null);
			}
			
			int isGiven = DBTools.getCursorColumnValueInt(cursor, DebtsTableMetaData.ISGIVEN);
			if ((DBTools.getCursorColumnValueDate(cursor, DebtsTableMetaData.BACKDATE) != null) &&
					(Tools.compareDates(DBTools.getCursorColumnValueDate(cursor, DebtsTableMetaData.BACKDATE), Tools.getCurrentDate()) <= 0)) {
				if (Tools.compareDates(DBTools.getCursorColumnValueDate(cursor, DebtsTableMetaData.BACKDATE), Tools.getCurrentDate()) < 0)
					view.setBackgroundColor(getResources().getColor(R.color.DarkGray));
				else if (Tools.compareDates(DBTools.getCursorColumnValueDate(cursor, DebtsTableMetaData.BACKDATE), Tools.getCurrentDate()) == 0)
					view.setBackgroundColor(getResources().getColor(R.color.transparent));
			}
			else {
				if (isGiven == 1)
					view.setBackgroundColor(getResources().getColor(R.color.CustomLightGreen));
				else 
					view.setBackgroundColor(getResources().getColor(R.color.CustomLightRed));
			}
			TextView tvTransDate = (TextView) view.findViewById(R.id.ltDebTransDate);
			tvTransDate.setText(getResources().getString(R.string.date) + ":" + Tools.DBDateToUserDate(DBTools.getCursorColumnValue(cursor, DebtsTableMetaData.TRANSDATE)));

			TextView tvDescription = (TextView) view.findViewById(R.id.ltDebDescription);
			String description = DBTools.getCursorColumnValue(cursor, DebtsTableMetaData.DESCRIPTION);
			tvDescription.setText(((isGiven == 1) ? getResources().getString(R.string.lent) : getResources().getString(R.string.borrowed)) + " - " + description);
			
			TextView tvReturnDate = (TextView) view.findViewById(R.id.ltDebRetDate);
			Date retDate = DBTools.getCursorColumnValueDate(cursor, DebtsTableMetaData.BACKDATE);
			if (retDate != null)
				tvReturnDate.setText(getResources().getString(R.string.returnDate) + ":" + Tools.DateToString(retDate, Constants.DateFormatUser));
			else
				tvReturnDate.setText("");
			
			TextView tvAmount = (TextView) view.findViewById(R.id.ltDebAmount);
			Double amount = DBTools.getCursorColumnValueDouble(cursor, DebtsTableMetaData.AMOUNT);
			String currencySign = DBTools.getCursorColumnValue(cursor, VDebtsViewMetaData.CURRENCY_SIGN);
			tvAmount.setText(Tools.getFullAmountText(amount, currencySign, true));
			
			return view;
		}
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
}
