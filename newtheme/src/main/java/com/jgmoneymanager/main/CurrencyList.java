package com.jgmoneymanager.main;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
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
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CurrencyTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.services.CurrencySrv;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import static android.widget.AdapterView.AdapterContextMenuInfo;
import static android.widget.AdapterView.OnClickListener;
import static android.widget.AdapterView.OnItemClickListener;

public class CurrencyList extends MyActivity
		implements NavigationView.OnNavigationItemSelectedListener {
	
	ListView listView;

	private final int menuEdit = Menu.FIRST;
	private final int menuDelete = menuEdit + 1;
	private final int menuSetDefault = menuEdit + 2;
	private final int menuRates = menuEdit + 3;

	int btRatesMenuID = menuEdit + 4;
	int btConverterMenuID = btRatesMenuID + 1;
	int btAddMenuID = btRatesMenuID + 2;
	int btDeleteAllMenuID = btRatesMenuID + 3;
	int btSetDefaultMenuID = btRatesMenuID + 4;

	private AdView adView;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initializeViews();
		//requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		
		if (getIntent().getAction().equals(Intent.ACTION_PICK)) {
			setTitle(R.string.selectCurrency);
		}

		listView = (ListView) findViewById(R.id.curList);
		refreshList(CurrencyList.this, listView);

		listView.setScrollingCacheEnabled(true);
		listView.setCacheColorHint(00000000);
		listView.setBackgroundColor(getResources().getColor(R.color.White));
		listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (getIntent().getAction().equals(Intent.ACTION_PICK)) {
					CurrencyEdit.updateSortOrder(CurrencyList.this, id);
					Uri uri = ContentUris.withAppendedId(CurrencyTableMetaData.CONTENT_URI, id);
					setResult(RESULT_OK, new Intent().setData(uri));
					finish();
				} else
					editListItem(id);
			}
		});
		
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

		registerForContextMenu(listView);
	}

	void refreshList(Context context, ListView listView) {
		Cursor cursor = context.getContentResolver().query(CurrencyTableMetaData.CONTENT_URI, null, null, null, null);
		String[] from = new String[] { CurrencyTableMetaData.NAME, CurrencyTableMetaData.SIGN };
		int[] to = new int[] { R.id.l2column1, R.id.l2column2 };
		SimpleCursorAdapter notes = new MyListAdapter(cursor, context, R.layout.currency_list_row, from, to);
		listView.setAdapter(notes);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK)
			refreshList(this, listView);
	}

	private void initializeViews() {
		setContentView(R.layout.main_activity);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
				this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
		drawer.setDrawerListener(toggle);
		toggle.syncState();

		NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(this);

		LinearLayout mainLayout = (LinearLayout) findViewById(R.id.content_main_layout);
		LayoutInflater inflater = (LayoutInflater)      this.getSystemService(LAYOUT_INFLATER_SERVICE);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		View child = inflater.inflate(R.layout.currency_list, null);
		mainLayout.addView(child, params);

		generateMenuItemNames(navigationView);
	}

	@Override
	public boolean onNavigationItemSelected(MenuItem item) {
		int id = item.getItemId();
		Intent intent;
		if (id == btRatesMenuID) {
			intent = new Intent(CurrencyList.this, CurrRatesList.class);
			startActivityForResult(intent, Constants.RequestNONE);
		}
		else if (id == btConverterMenuID) {
			intent = new Intent(CurrencyList.this, Convertor.class);
			startActivityForResult(intent, Constants.RequestNONE);
		}
		else if (id == btAddMenuID) {
			intent = new Intent(CurrencyList.this, CurrencyEdit.class);
			intent.setAction(Intent.ACTION_INSERT);
			Bundle bundle = new Bundle();
			bundle.putString(Constants.UpdateMode, Constants.Insert);
			intent.putExtras(bundle);
			startActivityForResult(intent, Constants.RequestNONE);
		}
		else if (id == btDeleteAllMenuID) {
			if (((ListView) findViewById(R.id.curList)).getCount() > 0) {
				Command cmd = new Command() {
					@Override
					public void execute() {
						CurrencyEdit.deleteCurrency(CurrencyList.this, 0);
						refreshList(CurrencyList.this, listView);
					}
				};
				AlertDialog deleteAllDialog = DialogTools.confirmDialog(CurrencyList.this, cmd, R.string.msgConfirm, R.string.msgDeleteAll);
				deleteAllDialog.show();
			}
		}
		else if (id == btSetDefaultMenuID) {
			final Cursor cursor = CurrencyList.this.getContentResolver().query(CurrencyTableMetaData.CONTENT_URI, null, null, null, CurrencyTableMetaData.NAME);

			final Command refreshCurrencyCommand = new Command() {
				@Override
				public void execute() {
					refreshList(CurrencyList.this, listView);
				}
			};

			Command cmd = new Command() {
				@Override
				public void execute() {
					cursor.moveToPosition(Constants.cursorPosition);
					CurrencySrv.changeDefaultCurrency(CurrencyList.this, DBTools.getCursorColumnValueLong(cursor, CurrencyTableMetaData._ID),
							refreshCurrencyCommand);
					cursor.close();
				}
			};
			AlertDialog dialog = DialogTools.RadioListDialog(CurrencyList.this, cmd, R.string.msgSetDefaultCurrency, cursor, CurrencyTableMetaData.NAME, true, true);
			dialog.show();
		}
		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.closeDrawer(GravityCompat.START);
		return true;
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
	
	void generateMenuItemNames(NavigationView navigationView) {
		Menu menu = navigationView.getMenu();

		if (!getIntent().getAction().equals(Intent.ACTION_PICK)) {
			menu.add(0, btRatesMenuID, btRatesMenuID, R.string.menuRates);
			menu.add(0, btConverterMenuID, btConverterMenuID, R.string.convertor);
			menu.add(0, btAddMenuID, btAddMenuID, R.string.menuAdd);
			menu.add(0, btDeleteAllMenuID, btDeleteAllMenuID, R.string.menuDeleteAll);
			menu.add(0, btSetDefaultMenuID, btSetDefaultMenuID, R.string.menuSetDefault);
		}
		else {
			menu.add(0, btAddMenuID, btAddMenuID, R.string.menuAdd);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		Cursor cursor = (Cursor) ((ListView) findViewById(R.id.curList)).getAdapter().getItem(info.position);
		menu.setHeaderTitle(DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData.NAME));
		menu.setHeaderIcon(R.drawable.ic_menu_manage);
		menu.add(0, menuEdit, 1, R.string.menuEdit);
		menu.add(0, menuDelete, 2, R.string.menuDelete);
		menu.add(0, menuSetDefault, 4, R.string.menuSetDefault);
		if (!getIntent().getAction().equals(Intent.ACTION_PICK))
			menu.add(0, menuRates, 3, R.string.menuRates);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case menuEdit:
			editListItem(info.id);			
			break;
		case menuDelete:
			final Cursor cursor = (Cursor) ((ListView) findViewById(R.id.curList)).getAdapter().getItem(info.position);
			if (DBTools.getCursorColumnValueInt(cursor, CurrencyTableMetaData.ISDEFAULT) == 1)
				DialogTools.toastDialog(this, getResources().getString(R.string.msgDeleteDefaultCurrency), Toast.LENGTH_LONG);
			else
			{
				Command cmd = new Command() {					
					@Override
					public void execute() {
						CurrencyEdit.deleteCurrency(CurrencyList.this, info.id);
						refreshList(CurrencyList.this, listView);
					}
				};
				AlertDialog deleteDialog = DialogTools.confirmDialog(CurrencyList.this, cmd, R.string.msgConfirm, R.string.msgDeleteItem);
				deleteDialog.show();
			}
			break;
		case menuSetDefault:
			/*CurrencyEdit.setDefaultCurrency(CurrencyList.this, info.id);
			CurrencyEdit.refreshDefaultCurrency(CurrencyList.this);*/
			final Command refreshCurrencyCommand = new Command() {
				@Override
				public void execute() {
					refreshList(CurrencyList.this, listView);
				}
			};
			CurrencySrv.changeDefaultCurrency(CurrencyList.this, info.id, refreshCurrencyCommand);
			break;
		case menuRates:
			Intent intent = new Intent(this, CurrRatesList.class);
			intent.setAction(Intent.ACTION_PICK);
			Bundle bundle = new Bundle();
			bundle.putLong(CurrencyTableMetaData._ID, info.id);
			intent.putExtras(bundle);
			startActivityForResult(intent, Constants.RequestNONE);
			break;
		default:
			break;
		}
		return true;
	}
	
	public void editListItem(long id)
	{
		Intent intent = new Intent(this, CurrencyEdit.class);
		intent.setAction(Intent.ACTION_EDIT);
		Bundle bundle = new Bundle();
		
		Uri uri = Uri.withAppendedPath(CurrencyTableMetaData.CONTENT_URI, String.valueOf(id));
		Cursor cursor = getContentResolver().query(uri, null, null, null, null);

		if (cursor.moveToFirst()) {
			bundle.putString(CurrencyTableMetaData.NAME, cursor.getString(cursor.getColumnIndex(CurrencyTableMetaData.NAME)));
			bundle.putString(CurrencyTableMetaData.SIGN, cursor.getString(cursor.getColumnIndex(CurrencyTableMetaData.SIGN)));
			bundle.putString(CurrencyTableMetaData._ID, cursor.getString(cursor.getColumnIndex(CurrencyTableMetaData._ID)));
			cursor.close();
			intent.putExtras(bundle);
			startActivityForResult(intent, Constants.RequestNONE);
		}
	}
		
	class MyListAdapter extends SimpleCursorAdapter {

		public MyListAdapter(Cursor cursor, Context context, int rowId,
				String[] from, int[] to) {
			super(context, rowId, cursor, from, to, 0);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = super.getView(position, convertView, parent);
			TextView tvName = (TextView) view.findViewById(R.id.l2column1);
			Cursor cursor = (Cursor) super.getItem(position);
			final long id = DBTools.getCursorColumnValueLong(cursor, CurrencyTableMetaData._ID);
			if (DBTools.getCursorColumnValueInt(cursor, CurrencyTableMetaData.ISDEFAULT) == 1)
				tvName.setText(DBTools.getCursorColumnValue(cursor, CurrencyTableMetaData.NAME) + " - " + getResources().getString(R.string.msgDefault));

			OnClickListener myClickListener = new OnClickListener() {
				@Override
				public void onClick(View view) {
					if (getIntent().getAction().equals(Intent.ACTION_PICK)) {
						CurrencyEdit.updateSortOrder(CurrencyList.this, id);
						Uri uri = ContentUris.withAppendedId(CurrencyTableMetaData.CONTENT_URI, id);
						setResult(RESULT_OK, new Intent().setData(uri));
						finish();
					}
				}
			};

			tvName.setOnClickListener(myClickListener);
			(view.findViewById(R.id.l2column2)).setOnClickListener(myClickListener);

			return view;
		}
	}

	public void myClickHandler(View target) {
		if (target.getId() == R.id.igDebMenu)
				openContextMenu(target);
	}
	
}