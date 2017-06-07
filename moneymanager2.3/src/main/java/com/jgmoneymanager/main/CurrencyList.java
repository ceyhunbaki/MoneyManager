package com.jgmoneymanager.main;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.jgmoneymanager.SlidingMenu.MyHorizontalScrollView;
import com.jgmoneymanager.SlidingMenu.OnSwipeTouchListener;
import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CurrencyTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyAbstractDemoChartctivity;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.services.CurrencySrv;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.LocalTools;
import com.jgmoneymanager.tools.Tools;

import static android.widget.AdapterView.*;

public class CurrencyList extends MyActivity {
	
	ListView listView;

	private MyHorizontalScrollView scrollView;
	private static View menu;
	private View app;
	private ImageView btnSlide;
	public static boolean menuOut = false;
	
	private final int menuEdit = Menu.FIRST;
	private final int menuDelete = menuEdit + 1;
	private final int menuSetDefault = menuEdit + 2;
	private final int menuRates = menuEdit + 3;

	String btRatesTag = "btRatesTag";
	String btConverterTag = "btConverterTag";
	String btAddTag = "btAddTag";
	String btDeleteAllTag = "btDeleteAllTag";
	String btSetDefaultTag = "btSetDefaultTag";

	private AdView adView;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initializeViews();
		//requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		
		if (getIntent().getAction().equals(Intent.ACTION_PICK)) {
			((TextView)findViewById(R.id.tvATTitle)).setText(R.string.selectCurrency);
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

	    generateMenuItemNames();
		
		// Create the adView 
		if (!Tools.proVersionExists(this)) {
			adView = new AdView(this, AdSize.BANNER, "ca-app-pub-5995868530154544/3468276114"); 
			LinearLayout layout = (LinearLayout)findViewById(R.id.onlyListAdsLayout); 
			layout.addView(adView);  
			AdRequest adRequest = new AdRequest();
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

	private void initializeViews() {
		LayoutInflater inflater = LayoutInflater.from(this);
		scrollView = (MyHorizontalScrollView) inflater.inflate(R.layout.horz_scroll_with_list_menu, null);

		setContentView(scrollView);

		//myApp = (MyApplicationLocal)getApplication();

		menu = inflater.inflate(R.layout.horz_scroll_menu, null);
		//app = inflater.inflate(R.layout.activitytest, null);
		app = inflater.inflate(R.layout.currency_list, null);
		ViewGroup tabBar = (ViewGroup) app.findViewById(R.id.relATTop);

		btnSlide = (ImageView) tabBar.findViewById(R.id.btATMenu);
		btnSlide.setOnClickListener(new MyAbstractDemoChartctivity.ClickListenerForScrolling(scrollView, menu));

		final View[] children = new View[] { menu, app };

		// Scroll to app (view[1]) when layout finished.
		int scrollToViewIdx = 1;
		scrollView.initViews(children, scrollToViewIdx, new MyAbstractDemoChartctivity.SizeCallbackForMenu(btnSlide));

		menu.setOnTouchListener(mySwipeListener);
	}

	//menu el ile geriye cekmek ucun
	private OnSwipeTouchListener mySwipeListener = new OnSwipeTouchListener() {
		public void onSwipeLeft() {
			if (menu.getVisibility() == View.VISIBLE) {
				menu.setVisibility(View.INVISIBLE);
				scrollView.smoothScrollTo(menu.getMeasuredWidth(), 0);
				menuOut = false;
			}
		}
		public void onSwipeRight() {
			if (menu.getVisibility() == View.INVISIBLE) {
				menu.setVisibility(View.VISIBLE);
				scrollView.smoothScrollTo(0, 0);
				menuOut = true;
			}
		}
	};

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		if (menuOut) {
			hideMenu();
		}
		else {
			CurrencyList.menu.setVisibility(View.VISIBLE);
			scrollView.smoothScrollTo(0, 0);
			menuOut = true;
		}
		return super.onMenuOpened(featureId, menu);
	}

	void hideMenu() {
		CurrencyList.menu.setVisibility(View.INVISIBLE);
		scrollView.smoothScrollTo(CurrencyList.menu.getMeasuredWidth(), 0);
		menuOut = false;
	}

	/**
	 * Helper for examples with a HSV that should be scrolled by a menu View's width.
	 */
	static class ClickListenerForScrolling implements View.OnClickListener {
		HorizontalScrollView scrollView;
		View menu;

		public ClickListenerForScrolling(HorizontalScrollView scrollView, View menu) {
			super();
			this.scrollView = scrollView;
			this.menu = menu;
		}

		@Override
		public void onClick(View v) {
			// Ensure menu is visible
			if (!menuOut) {
				scrollView.smoothScrollTo(0, 0);
				menu.setVisibility(View.VISIBLE);
			} else {
				scrollView.smoothScrollTo(menu.getMeasuredWidth(), 0);
				menu.setVisibility(View.INVISIBLE);
			}
			menuOut = !menuOut;
		}
	}

	/**
	 * Helper that remembers the width of the 'slide' button, so that the 'slide' button remains in view, even when the menu is
	 * showing.
	 */
	static class SizeCallbackForMenu implements MyHorizontalScrollView.SizeCallback {
		int btnWidth;
		View btnSlide;

		public SizeCallbackForMenu(View btnSlide) {
			super();
			this.btnSlide = btnSlide;
		}

		@Override
		public void onGlobalLayout() {
			btnWidth = btnSlide.getMeasuredWidth();
			System.out.println("btnWidth=" + btnWidth);
		}

		@Override
		public void getViewSize(int idx, int w, int h, int[] dims) {
			dims[0] = w;
			dims[1] = h;
			final int menuIdx = 0;
			if (idx == menuIdx) {
				dims[0] = w - btnWidth;
			}
		}
	}

	@Override
	protected void onDestroy() {
    	try {
    		super.onDestroy();
			if (adView != null)
				adView.removeAllViews();
				adView.destroy();
    	}
    	catch (Exception ex) {
    		
    	}
	}
	
	void generateMenuItemNames() {
		if (!getIntent().getAction().equals(Intent.ACTION_PICK)) {
			LocalTools.addButtonToMenuList(this,R.string.menuRates, btRatesTag);
			LocalTools.addButtonToMenuList(this,R.string.convertor, btConverterTag);
			LocalTools.addButtonToMenuList(this,R.string.menuAdd, btAddTag);
			LocalTools.addButtonToMenuList(this,R.string.menuDeleteAll, btDeleteAllTag);
			LocalTools.addButtonToMenuList(this,R.string.menuSetDefault, btSetDefaultTag);
		}
		else {
			LocalTools.addButtonToMenuList(this, R.string.menuAdd, btAddTag);
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
			super(context, rowId, cursor, from, to);
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
		hideMenu();
		Intent intent;
		if (target.getTag() == btRatesTag) {
			intent = new Intent(CurrencyList.this, CurrRatesList.class);
			startActivityForResult(intent, Constants.RequestNONE);
		}
		else if (target.getTag() == btConverterTag) {
			intent = new Intent(CurrencyList.this, Convertor.class);
			startActivityForResult(intent, Constants.RequestNONE);
		}
		else if (target.getTag() == btAddTag) {
			intent = new Intent(CurrencyList.this, CurrencyEdit.class);
			intent.setAction(Intent.ACTION_INSERT);
			Bundle bundle = new Bundle();
			bundle.putString(Constants.UpdateMode, Constants.Insert);
			intent.putExtras(bundle);
			startActivityForResult(intent, Constants.RequestNONE);
		}
		else if (target.getTag() == btDeleteAllTag) {
			if (((ListView) findViewById(R.id.curList)).getCount() > 0) {
				Command cmd = new Command() {
					@Override
					public void execute() {
						CurrencyEdit.deleteCurrency(CurrencyList.this, 0);
					}
				};
				AlertDialog deleteAllDialog = DialogTools.confirmDialog(CurrencyList.this, cmd, R.string.msgConfirm, R.string.msgDeleteAll);
				deleteAllDialog.show();
			}
		}
		else if (target.getTag() == btSetDefaultTag) {
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
				}
			};
			AlertDialog dialog = DialogTools.RadioListDialog(CurrencyList.this, cmd, R.string.msgSetDefaultCurrency, cursor, CurrencyTableMetaData.NAME, true);
			dialog.show();
		}
		else {
			switch (target.getId()) {
				case R.id.igDebMenu:
					openContextMenu(target);
					break;
				default:
					break;
			}
		}
	}
	
}