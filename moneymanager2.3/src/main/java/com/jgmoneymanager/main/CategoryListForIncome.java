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
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CategoryTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransactionsTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyListActivity;
import com.jgmoneymanager.services.CategorySrv;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

public class CategoryListForIncome extends MyListActivity {

	private AdView adView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.categorylistincome);
		
		if (getIntent().getAction().equals(Intent.ACTION_PICK))
			((TextView)findViewById(R.id.tvATTitle)).setText(R.string.selectCategory);


		Cursor cursor;
		cursor = getContentResolver().query(CategoryTableMetaData.CONTENT_URI, null, 
				CategoryTableMetaData.ISINCOME + " = 1 and " 
						+ CategoryTableMetaData.MAINID + " is not null ", null, null);
		String[] from = new String[] { CategoryTableMetaData.NAME};
		int[] to = new int[] { R.id.grp_child};
		SimpleCursorAdapter notes = new MyListAdapter(cursor, this, R.layout.list1columnrowcategoryincome, from, to);
		setListAdapter(notes);

		ListView lv = getListView();
		lv.setScrollingCacheEnabled(true);
		lv.setCacheColorHint(00000000);
		lv.setBackgroundColor(getResources().getColor(R.color.White));
		registerForContextMenu(lv);		
		
		
		if ((getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
				&& !Tools.proVersionExists(this)) {
			// Create the adView 
			adView = new AdView(this, AdSize.BANNER, "ca-app-pub-5995868530154544/1867118510"); 
			// Lookup your LinearLayout assuming it's been given // the attribute android:id="@+id/mainLayout" 
			LinearLayout layout = (LinearLayout)findViewById(R.id.CatLayout2); 
			layout.addView(adView); // Initiate a generic request to load it with an ad 
			AdRequest adRequest = new AdRequest();
			adView.loadAd(adRequest);
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			if (adView != null)
				adView.removeAllViews();
				adView.destroy();
    	}
    	catch (Exception ex) {
    		
    	}
	}

	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		menu.setHeaderIcon(R.drawable.ic_menu_manage);
		menu.add(R.string.menuEdit);
		menu.add(R.string.menuDelete);
		menu.add(R.string.menuSetAsExpense);
		Cursor cursor = (Cursor)getListAdapter().getItem(info.position);
		menu.setHeaderTitle(DBTools.getCursorColumnValue(cursor, CategoryTableMetaData.NAME));
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		if (item.getTitle().toString().equals(getString(R.string.menuEdit))) {
			final EditText input = new EditText(this);
			final Cursor cursor = getContentResolver().query(CategoryTableMetaData.CONTENT_URI, null, 
					CategoryTableMetaData._ID + " = " + String.valueOf(info.id), null, null);
			cursor.moveToFirst();
			input.setText(cursor.getString(cursor.getColumnIndex(CategoryTableMetaData.NAME)));
			input.selectAll();
			Command update = new Command() {
				public void execute() {
					String name = Tools.cutName(input.getText().toString());
					if (Tools.existsInTable(getBaseContext(), CategoryTableMetaData.CONTENT_URI, CategoryTableMetaData.NAME, name, 
							CategoryTableMetaData.ISINCOME + " = 1 and " + CategoryTableMetaData._ID + " <> " + String.valueOf(info.id)))
						DialogTools.toastDialog(getBaseContext(), getResources().getString(R.string.msgCategoryExists), Toast.LENGTH_LONG);
					else 
					{
						CategorySrv.changeCategoryName(CategoryListForIncome.this, info.id, name);
						/*ContentValues values = new ContentValues();
						values.put(CategoryTableMetaData.NAME, name);
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
			int message = R.string.msgDeleteItem;
			Command delete = new Command() {
				@Override
				public void execute() {
					Command update = new Command() {					
						@Override
						public void execute() {
							DBTools.execQuery(CategoryListForIncome.this, "update " + TransactionsTableMetaData.TABLE_NAME + " set " + TransactionsTableMetaData.CATEGORYID +
									" = 0 where " + TransactionsTableMetaData.CATEGORYID + " in (select _id from " + CategoryTableMetaData.TABLE_NAME +
									" where " + CategoryTableMetaData._ID + " = " + String.valueOf(info.id) + ")");
							getContentResolver().delete(Uri.withAppendedPath(CategoryTableMetaData.CONTENT_URI, String.valueOf(info.id)), null, null);
						}
					};
					AlertDialog updateDialog = DialogTools.confirmDialog(CategoryListForIncome.this, update, R.string.msgConfirm,
							R.string.msgUpdateThisCategoryTransactions,
							new String[]{CategoryListForIncome.this.getResources().getString(R.string.ok), CategoryListForIncome.this.getResources().getString(R.string.Cancel)});
					updateDialog.show();
				}
			};
			AlertDialog deleteDialog = DialogTools.confirmDialog(this, delete , R.string.msgConfirm, message);
			deleteDialog.show();
		}
		else if (item.getTitle().toString().equals(getString(R.string.menuSetAsExpense))) {
			final Cursor cursor = CategoryListForIncome.this.getContentResolver().query(CategoryTableMetaData.CONTENT_URI, 
					new String[] {CategoryTableMetaData._ID, CategoryTableMetaData.NAME}, 
					CategoryTableMetaData.MAINID + " is null and " + CategoryTableMetaData.ISINCOME + " = 0 ", 
					null, null);
			Command cmdConfirm = new Command() {				
				@Override
				public void execute() {
					Command cmdMainCat = new Command() {						
						@Override
						public void execute() {
							cursor.moveToPosition(Constants.cursorPosition);
							CategorySrv.changeCategoryStatusToExpense(CategoryListForIncome.this, info.id, 
									DBTools.getCursorColumnValueLong(cursor, CategoryTableMetaData._ID));
						}
					};
					AlertDialog mainCatDialog = DialogTools.RadioListDialog(CategoryListForIncome.this, 
							cmdMainCat, R.string.selectMainCategory, cursor, CategoryTableMetaData.NAME, true);
					mainCatDialog.show();
				}
			};
			AlertDialog confirmDialog = DialogTools.confirmDialog(CategoryListForIncome.this, cmdConfirm, 
					R.string.msgConfirm, R.string.msgSetCategoryAsExpense, 
					new String[] {CategoryListForIncome.this.getResources().getString(R.string.Yes), 
							CategoryListForIncome.this.getResources().getString(R.string.No)});
			confirmDialog.show();
		}
		return false;
	}
	
	void addCategory() {
		final EditText input = new EditText(CategoryListForIncome.this);
		Command insert = new Command() {
			public void execute() {
				String name = Tools.cutName(input.getText().toString());
				if (Tools.existsInTable(getBaseContext(), CategoryTableMetaData.CONTENT_URI, CategoryTableMetaData.NAME, 
						name, CategoryTableMetaData.ISINCOME + " = 1 "))
					DialogTools.toastDialog(getBaseContext(), getResources().getString(R.string.msgCategoryExists), Toast.LENGTH_LONG);
				else 
				{
					ContentValues cv = new ContentValues();
					cv.put(CategoryTableMetaData.NAME, name);
					cv.put(CategoryTableMetaData.MAINID, CategorySrv.getMainIncomeCategoryID(getBaseContext()));
					cv.put(CategoryTableMetaData.ISINCOME, 1);
					getContentResolver().insert(CategoryTableMetaData.CONTENT_URI, cv);
				}
			}
		};
		AlertDialog inputDialog = DialogTools.InputDialog(CategoryListForIncome.this, insert, R.string.msgAddCategory, input, R.drawable.ic_menu_add);
		inputDialog.show();
		inputDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(input.getText().toString().trim().length() != 0);
	}
	
	public class MyListAdapter extends SimpleCursorAdapter {

		public MyListAdapter(Cursor cursor, Context context, int rowId,
				String[] from, int[] to) {
			super(context, rowId, cursor, from, to);
		}

		public View getView(final int position, View convertView, ViewGroup parent) {
			View view = super.getView(position, convertView, parent);
			//Cursor cursor = (Cursor) super.getItem(position);
			view.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (getIntent().getAction().equals(Intent.ACTION_PICK)) {
						Uri uri = ContentUris.withAppendedId(CategoryTableMetaData.CONTENT_URI, getItemId(position));
						setResult(RESULT_OK, new Intent().setData(uri));
						finish();
					}
				}
			});
			return view;
		}
			

	}
	
	public void myClickHandler(View target) {
		switch (target.getId()) {
		case R.id.btCatAdd:
			addCategory();
			break;
		case R.id.bt_grp_edit:
			openContextMenu(target);
			break;
		default:
			break;
		}
	}
}
