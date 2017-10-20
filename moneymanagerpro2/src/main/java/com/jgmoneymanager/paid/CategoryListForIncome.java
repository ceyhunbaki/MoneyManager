package com.jgmoneymanager.paid;

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
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CategoryTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransactionsTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.services.CategorySrv;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

public class CategoryListForIncome extends MyActivity {

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
		View child = inflater.inflate(R.layout.categorylistincome, null);
		mainLayout.addView(child, params);

		if (getIntent().getAction().equals(Intent.ACTION_PICK))
			setTitle(R.string.selectCategory);

		ListView listView = (ListView) findViewById(R.id.catListView);
		Cursor cursor = getContentResolver().query(CategoryTableMetaData.CONTENT_URI, null,
				CategoryTableMetaData.ISINCOME + " = 1 and " + CategoryTableMetaData.MAINID + " is not null ", null, null);
		String[] from = new String[] { CategoryTableMetaData.NAME};
		int[] to = new int[] { R.id.grp_child};
		SimpleCursorAdapter notes = new MyListAdapter(cursor, this, R.layout.list1columnrowcategoryincome, from, to);
		listView.setAdapter(notes);
		listView.setScrollingCacheEnabled(true);
		listView.setCacheColorHint(00000000);
		listView.setBackgroundColor(getResources().getColor(R.color.White));
		registerForContextMenu(listView);
	}

	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		menu.setHeaderIcon(R.drawable.ic_menu_manage);
		menu.add(R.string.menuEdit);
		menu.add(R.string.menuDelete);
		menu.add(R.string.menuSetAsExpense);
		ListView listView = (ListView) findViewById(R.id.catListView);
		Cursor cursor = (Cursor)listView.getAdapter().getItem(info.position);
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
							cmdMainCat, R.string.selectMainCategory, cursor, CategoryTableMetaData.NAME, true, true);
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
