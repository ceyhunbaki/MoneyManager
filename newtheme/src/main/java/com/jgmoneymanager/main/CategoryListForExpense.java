package com.jgmoneymanager.main;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.BudgetCategoriesTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CategoryTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransactionsTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VCategoriesViewMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.services.CategorySrv;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

public class CategoryListForExpense extends MyActivity {

	private int mGroupIdColumnIndex;

	private AdView adView;

	@Override
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
		View child = inflater.inflate(R.layout.categorylist, null);
		mainLayout.addView(child, params);

		if (getIntent().getAction().equals(Intent.ACTION_PICK))
			this.setTitle(R.string.selectCategory);

		//getExpandableListView().setGroupIndicator(getResources().getDrawable(R.drawable.expander_group));
		ExpandableListView expandableListView = (ExpandableListView) findViewById(R.id.catExListView);
		expandableListView.setBackgroundColor(getResources().getColor(R.color.White));
		expandableListView.setScrollingCacheEnabled(true);
		expandableListView.setCacheColorHint(0);

		Cursor mGroupsCursor = this.managedQuery(CategoryTableMetaData.CONTENT_URI, null, 
				CategoryTableMetaData.MAINID + " is null and " + CategoryTableMetaData.ISINCOME + " = 0 ", 
				null, null);
		mGroupIdColumnIndex = mGroupsCursor.getColumnIndexOrThrow(CategoryTableMetaData._ID);
		ExpandableListAdapter mAdapter = new MyExpandableListAdapter(mGroupsCursor, this,
				R.layout.group_row, R.layout.list1columnrowcategory,
				new String[] { CategoryTableMetaData.NAME },
				new int[] { R.id.row_name },
				new String[] { CategoryTableMetaData.NAME},
				new int[] { R.id.grp_child});
		expandableListView.setAdapter(mAdapter);
		
		if ((getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
				&& !Tools.proVersionExists(this)) {
//			// Create the adView
//			adView = new AdView(this, AdSize.BANNER, "ca-app-pub-5995868530154544/1867118510");
//			// Lookup your LinearLayout assuming it's been given // the attribute android:id="@+id/mainLayout"
//			LinearLayout layout = (LinearLayout)findViewById(R.id.CatLayout2);
//			layout.addView(adView); // Initiate a generic request to load it with an ad
//			AdRequest adRequest = new AdRequest();
//			adView.loadAd(adRequest);
			MobileAds.initialize(getApplicationContext(), "ca-app-pub-5995868530154544/1867118510");
			adView = new AdView(this);
			adView.setAdSize(AdSize.SMART_BANNER);
			adView.setAdUnitId("ca-app-pub-5995868530154544/1867118510");
			LinearLayout layout = (LinearLayout) findViewById(R.id.CatLayout2);
			layout.addView(adView);
			AdRequest adRequest = new AdRequest.Builder().build();
			adView.loadAd(adRequest);
		}

		registerForContextMenu(expandableListView);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
    	try {
    		super.onDestroy();
			if (adView != null)
				adView.destroy();
    	}
    	catch (Exception ex) {
    		
    	}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
		int type = ExpandableListView.getPackedPositionType(info.packedPosition);
		//menu.setHeaderTitle(R.string.menuContextTitle);
		menu.setHeaderIcon(R.drawable.ic_menu_manage);
		menu.add(R.string.menuEdit);
		menu.add(R.string.menuDelete);
		Cursor cursor;
		ExpandableListView expandableListView = (ExpandableListView) findViewById(R.id.catExListView);
		if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP)
		{
			menu.add(R.string.addSubCategory);
			cursor = (Cursor) expandableListView.getExpandableListAdapter().getGroup(ExpandableListView.getPackedPositionGroup(info.packedPosition));
		}
		else
		{
			menu.add(R.string.menuChangeGroup);
			cursor = (Cursor) expandableListView.getExpandableListAdapter().getChild(ExpandableListView.getPackedPositionGroup(info.packedPosition), ExpandableListView.getPackedPositionChild(info.packedPosition));
		}
		menu.add(R.string.menuSetAsIncome);
		menu.setHeaderTitle(DBTools.getCursorColumnValue(cursor, CategoryTableMetaData.NAME));
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
		if (item.getTitle().toString().equals(getString(R.string.addSubCategory))) {
			addSubCategory(info.id);
			return true;
		}
		else if (item.getTitle().toString().equals(getString(R.string.menuEdit))) {
			final EditText input = new EditText(this);
			final Cursor cursor = this.managedQuery(Uri.withAppendedPath(CategoryTableMetaData.CONTENT_URI, String.valueOf(info.id)), null, null, null, null);
			final int type = ExpandableListView.getPackedPositionType(info.packedPosition);
			cursor.moveToFirst();
			input.setText(cursor.getString(cursor.getColumnIndex(CategoryTableMetaData.NAME)));
			input.selectAll();
			Command update = new Command() {
				public void execute() {
					String name = Tools.cutName(input.getText().toString());
					if ((type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) &&
							(Tools.existsInTable(getBaseContext(), CategoryTableMetaData.CONTENT_URI, CategoryTableMetaData.NAME, name, 
									CategoryTableMetaData.MAINID + " = " + cursor.getString(cursor.getColumnIndex(CategoryTableMetaData.MAINID)) +
									" and " + CategoryTableMetaData._ID + " <> " + String.valueOf(info.id))))
						DialogTools.toastDialog(getBaseContext(), getResources().getString(R.string.msgSubCategoryExists), Toast.LENGTH_LONG);
					else if ((type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) &&
							(Tools.existsInTable(getBaseContext(), CategoryTableMetaData.CONTENT_URI, CategoryTableMetaData.NAME, name, 
									CategoryTableMetaData.MAINID + " is null and " + CategoryTableMetaData._ID + " <> " + String.valueOf(info.id))))
						DialogTools.toastDialog(getBaseContext(), getResources().getString(R.string.msgCategoryExists), Toast.LENGTH_LONG);
					else 
					{
						CategorySrv.changeCategoryName(CategoryListForExpense.this, info.id, name);
						/*ContentValues values = new ContentValues();
						values.put(CategoryTableMetaData.NAME, name);
						values.put(CategoryTableMetaData.MAINID, cursor.getString(cursor.getColumnIndex(CategoryTableMetaData.MAINID)));
						getContentResolver().update(Uri.withAppendedPath(CategoryTableMetaData.CONTENT_URI, String.valueOf(info.id)), values, null, null);*/
					}
				}
			};
			AlertDialog updateDialog = DialogTools.InputDialog(this, update,
					getResources().getString(R.string.menuEdit) + " " + DBTools.getCursorColumnValue(cursor, CategoryTableMetaData.NAME),
					input, R.drawable.ic_menu_edit);
			updateDialog.show();
			updateDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(input.getText().toString().trim().length() != 0);
			return true;
		}
		else if (item.getTitle().toString().equals(getString(R.string.menuDelete))) {
			int type = ExpandableListView.getPackedPositionType(info.packedPosition);
			int message;
			if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD)
				message = R.string.msgDeleteItem;
			else if (this.managedQuery(CategoryTableMetaData.CONTENT_URI, null, 
					CategoryTableMetaData.MAINID + " = " + String.valueOf(info.id), null, null).getCount() > 0)
				message = R.string.msgDeleteGroupAndChild;
			else 
				message = R.string.msgDeleteGroup;
			Command delete = new Command() {
				@Override
				public void execute() {
					Command update = new Command() {					
						@Override
						public void execute() {
							DBTools.execQuery(CategoryListForExpense.this, "update " + TransactionsTableMetaData.TABLE_NAME + " set " + TransactionsTableMetaData.CATEGORYID +
									" = 0 where " + TransactionsTableMetaData.CATEGORYID + " in (select _id from " + CategoryTableMetaData.TABLE_NAME +
									" where " + CategoryTableMetaData._ID + " = " + String.valueOf(info.id) + " or " +
									CategoryTableMetaData.MAINID + " = " + String.valueOf(info.id) + ")");
							DBTools.execQuery(CategoryListForExpense.this, "delete from " + BudgetCategoriesTableMetaData.TABLE_NAME 
									+ " where " + BudgetCategoriesTableMetaData.CATEGORY_ID + " in (select _id from " 
									+ CategoryTableMetaData.TABLE_NAME + " where " + CategoryTableMetaData._ID + " = " 
									+ String.valueOf(info.id) + " or " + CategoryTableMetaData.MAINID + " = " + String.valueOf(info.id) + ")");
							getContentResolver().delete(Uri.withAppendedPath(CategoryTableMetaData.CONTENT_URI, String.valueOf(info.id)), null, null);
						}
					};
					AlertDialog updateDialog = DialogTools.confirmDialog(CategoryListForExpense.this, update, R.string.msgConfirm,
							R.string.msgUpdateThisCategoryTransactions,
							new String[]{CategoryListForExpense.this.getResources().getString(R.string.ok), CategoryListForExpense.this.getResources().getString(R.string.Cancel)});
					updateDialog.show();
				}
			};
			AlertDialog deleteDialog = DialogTools.confirmDialog(this, delete, R.string.msgConfirm, message);
			deleteDialog.show();
		}
		else if (item.getTitle().toString().equals(getString(R.string.menuChangeGroup))) {
			final ListView listView = new ListView(this);

			Cursor cursor = this.managedQuery(CategoryTableMetaData.CONTENT_URI, null, 
					CategoryTableMetaData.MAINID + " is null ", null, null);
			//startManagingCursor(cursor);
			String[] from = new String[] { CategoryTableMetaData.NAME };
			int[] to = new int[] { R.id.l2column1};
			SimpleCursorAdapter notes = new SimpleCursorAdapter(this, R.layout.list2columnrow, cursor, from, to);
			listView.setAdapter(notes);
								
			Command change = new Command() {
				public void execute() {
					Cursor cursor = getContentResolver().query(CategoryTableMetaData.CONTENT_URI, null, 
							CategoryTableMetaData.MAINID + " is null ", null, null);
					cursor.moveToPosition(Constants.cursorPosition);
					Cursor subCursor = getContentResolver().query(Uri.withAppendedPath(CategoryTableMetaData.CONTENT_URI, String.valueOf(info.id)), 
							new String[] {CategoryTableMetaData.NAME}, null, null, null);
					subCursor.moveToFirst();
					if (Tools.existsInTable(getBaseContext(), CategoryTableMetaData.CONTENT_URI, CategoryTableMetaData.NAME, 
							DBTools.getCursorColumnValue(subCursor, CategoryTableMetaData.NAME), 
							CategoryTableMetaData.MAINID + " = " + DBTools.getCursorColumnValue(cursor, CategoryTableMetaData._ID)))
						DialogTools.toastDialog(getBaseContext(), getResources().getString(R.string.msgSubCategoryExists), Toast.LENGTH_LONG);
					else {
						ContentValues values = new ContentValues();
						values.put(CategoryTableMetaData.MAINID, cursor.getString(cursor.getColumnIndex(CategoryTableMetaData._ID)));
						getContentResolver().update(Uri.withAppendedPath(CategoryTableMetaData.CONTENT_URI, String.valueOf(info.id)), values, null, null);
					}
				}
			};
			AlertDialog changeDialog = DialogTools.RadioListDialog(this, change, R.string.msgSelect, cursor, CategoryTableMetaData.NAME, true, true);
			changeDialog.show();
		}
		else if (item.getTitle().toString().equals(getString(R.string.menuSetAsIncome))) {
			final int type = ExpandableListView.getPackedPositionType(info.packedPosition);
			int message;
			if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD)
				message = R.string.msgSetCategoryAsIncome;
			else 
				message = R.string.msgSetSubCategoriesAsIncome;
			Command cmdConfirm = new Command() {				
				@Override
				public void execute() {
					CategorySrv.changeCategoryStatusToIncome(CategoryListForExpense.this, info.id);
					if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) 
						CategoryListForExpense.this.getContentResolver().delete(CategoryTableMetaData.CONTENT_URI, 
								CategoryTableMetaData._ID + " = " + info.id, null);
				}
			};
			AlertDialog confirmDialog = DialogTools.confirmDialog(CategoryListForExpense.this, cmdConfirm, R.string.msgConfirm, 
					message, new String[] {getResources().getString(R.string.Yes), getResources().getString(R.string.No)});
			confirmDialog.show();
		}
		return false;
	}
	
	void addSubCategory (final long mainId) {
		final EditText input = new EditText(this);
		Command insert = new Command() {
			public void execute() {
				String name = Tools.cutName(input.getText().toString());
				if (Tools.existsInTable(getBaseContext(), CategoryTableMetaData.CONTENT_URI, CategoryTableMetaData.NAME, 
						name, CategoryTableMetaData.MAINID + " = " + String.valueOf(mainId)))
				{
					DialogTools.toastDialog(getBaseContext(), getResources().getString(R.string.msgSubCategoryExists), Toast.LENGTH_LONG);
					return;
				}
				ContentValues values = new ContentValues();
				values.put(CategoryTableMetaData.NAME, name);
				values.put(CategoryTableMetaData.MAINID, String.valueOf(mainId));
				values.put(CategoryTableMetaData.ISINCOME, 0);
				getContentResolver().insert(CategoryTableMetaData.CONTENT_URI, values);
			}
		};
		AlertDialog inputDialog = DialogTools.InputDialog(this, insert, R.string.addSubCategory, input, R.drawable.ic_menu_add);
		inputDialog.show();	
		inputDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(input.getText().toString().trim().length() != 0);
	}	
	
	void addMainCategory() {
		final EditText input = new EditText(CategoryListForExpense.this);
		Command insert = new Command() {
			public void execute() {
				String name = Tools.cutName(input.getText().toString());
				if (Tools.existsInTable(getBaseContext(), CategoryTableMetaData.CONTENT_URI, CategoryTableMetaData.NAME, 
						name, CategoryTableMetaData.MAINID + " is null "))
					DialogTools.toastDialog(getBaseContext(), getResources().getString(R.string.msgCategoryExists), Toast.LENGTH_LONG);
				else 
				{
					ContentValues cv = new ContentValues();
					cv.put(CategoryTableMetaData.NAME, name);
					cv.put(CategoryTableMetaData.ISINCOME, 0);
					getContentResolver().insert(CategoryTableMetaData.CONTENT_URI, cv);
				}
			}
		};
		AlertDialog inputDialog = DialogTools.InputDialog(CategoryListForExpense.this, insert, R.string.msgAddCategory, input, R.drawable.ic_menu_add);
		inputDialog.show();
		inputDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(input.getText().toString().trim().length() != 0);
	}

	// extending SimpleCursorTreeAdapter
	public class MyExpandableListAdapter extends SimpleCursorTreeAdapter {

		public MyExpandableListAdapter(Cursor cursor, Context context,
				int groupLayout, int childLayout, String[] groupFrom,
				int[] groupTo, String[] childrenFrom, int[] childrenTo) {
			super(context, cursor, groupLayout, groupFrom, groupTo, childLayout, childrenFrom, childrenTo);
		}

		// returns cursor with subitems for given group cursor
		@Override
		protected Cursor getChildrenCursor(Cursor groupCursor) {
			return getContentResolver().query(CategoryTableMetaData.CONTENT_URI, null,
					CategoryTableMetaData.MAINID + " = " + groupCursor.getLong(mGroupIdColumnIndex) , null, null);
		}

		// I needed to process click on click of the button on child item
		public View getChildView(final int groupPosition,
				final int childPosition, boolean isLastChild, View convertView,
				ViewGroup parent) {
			View rowView = super.getChildView(groupPosition, childPosition, isLastChild, convertView, parent);
			rowView.setOnClickListener(new OnClickListener() {			
				@Override
				public void onClick(View v) {
					if (getIntent().getAction().equals(Intent.ACTION_PICK))
					{
						Uri uri = ContentUris.withAppendedId(VCategoriesViewMetaData.CONTENT_URI, getChildId(groupPosition, childPosition));
						setResult(RESULT_OK, new Intent().setData(uri));
						finish();
					}
				}
			});
			return rowView;
		}

		public View getGroupView(final int groupPosition, final boolean isExpanded,
				View convertView, ViewGroup parent) {
			View v = super.getGroupView(groupPosition, isExpanded, convertView, parent);
			v.setOnClickListener(new OnClickListener() {				
				@Override
				public void onClick(View v) {
					if (isExpanded)
						((ExpandableListView)findViewById(R.id.catExListView)).collapseGroup(groupPosition);
					else
						((ExpandableListView)findViewById(R.id.catExListView)).expandGroup(groupPosition);
				}
			});
			return v;
		}

	}
	
	public void myClickHandler(View target) {
		switch (target.getId()) {
		case R.id.btCatAddMain:
			addMainCategory();
			break;
		case R.id.btCatAddSubCategory:
			final Cursor cursor = getContentResolver().query(CategoryTableMetaData.CONTENT_URI, null, 
					CategoryTableMetaData.MAINID + " is null ", null, null);
			Command cmd = new Command() {				
				@Override
				public void execute() {
					cursor.moveToPosition(Constants.cursorPosition);
					addSubCategory(DBTools.getCursorColumnValueLong(cursor, CategoryTableMetaData._ID));
				}
			};
			AlertDialog dialog = DialogTools.RadioListDialog(CategoryListForExpense.this, cmd, R.string.selectMainCategory, 
					cursor, CategoryTableMetaData.NAME, true, true);
			dialog.show();
			break;
		case R.id.bt_grp_edit:
			openContextMenu(target);
			break;
		default:
			break;
		}
	}
}
