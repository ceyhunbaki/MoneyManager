package com.jgmoneymanager.main;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.ericharlow.DragNDrop.DragListener;
import com.ericharlow.DragNDrop.DragNDropAdapter;
import com.ericharlow.DragNDrop.DragNDropListView;
import com.ericharlow.DragNDrop.DropListener;
import com.ericharlow.DragNDrop.RemoveListener;
import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransactionStatusTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.services.TransactionStatusSrv;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import java.util.ArrayList;

public class TransactionStatusList extends MyActivity {

	ArrayList<String> content;
	
	static boolean resultOK = false;
	/**
	 * If order of status changed it changes to true
	 */
	boolean orderChanged = false;

	Cursor cursor;
	
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
		View child = inflater.inflate(R.layout.accountsort, null);
		mainLayout.addView(child, params);


		if (savedInstanceState == null) {
	        reloadContentList();
        }
        else {
			cursor = getContentResolver().query(TransactionStatusTableMetaData.CONTENT_URI, null, null, null, TransactionStatusTableMetaData.SORTORDER);
        	content = Tools.getStringArrayListFromBundle(savedInstanceState, "content");
        	orderChanged = Tools.getBooleanFromBundle0(savedInstanceState, "orderChanged");
        	resultOK = Tools.getBooleanFromBundle0(savedInstanceState, "resultOK");
        }
        
        refreshList();

		registerForContextMenu(findViewById(R.id.drListView));
    }
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Tools.putToBundle(outState, "content", content);
		Tools.putToBundle(outState, "orderChanged", orderChanged);
		Tools.putToBundle(outState, "resultOK", resultOK);
		super.onSaveInstanceState(outState);
	}

	void refreshList() {
		ListView listView = (ListView) findViewById(R.id.drListView);
		listView.setAdapter(new DragNDropAdapter(this, new int[]{R.layout.dragitem_new}, new int[]{R.id.drText}, content));

        
        if (listView instanceof DragNDropListView) {
        	((DragNDropListView) listView).setDropListener(mDropListener);
        	((DragNDropListView) listView).setRemoveListener(mRemoveListener);
        	((DragNDropListView) listView).setDragListener(mDragListener);
        }
        
        listView.setBackgroundColor(getResources().getColor(R.color.White));
	}

	void reloadContentList() {
		cursor = getContentResolver().query(TransactionStatusTableMetaData.CONTENT_URI, null, null, null, TransactionStatusTableMetaData.SORTORDER);
		content = new ArrayList<String>(cursor.getCount());
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			content.add(DBTools.getCursorColumnValue(cursor, TransactionStatusTableMetaData.NAME));
		}
	}
	
	public void restartActivity() {
	    Intent intent = getIntent();
	    finish();
	    startActivity(intent);
	}
	
	private void sortAZ() {
		Cursor cursor = getContentResolver().query(TransactionStatusTableMetaData.CONTENT_URI, null, null, null, TransactionStatusTableMetaData.NAME);
        content = new ArrayList<String>(cursor.getCount());
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
        	content.add(DBTools.getCursorColumnValue(cursor, TransactionStatusTableMetaData.NAME));
        }
        refreshList();
		//ardicilliq deyiwiklik olubsa cixiwda save soruwaq
        orderChanged = true;
	}
	
	private void saveToDB(Context context, ArrayList<String> content){
		ContentValues values = new ContentValues();		
		for (int i = 0; i < content.size(); i++) {
			values.clear();
			values.put(TransactionStatusTableMetaData.SORTORDER, i);
			context.getContentResolver().update(TransactionStatusTableMetaData.CONTENT_URI, values,
					TransactionStatusTableMetaData.NAME + " = '" + content.get(i) + "'", null);
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

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		//Cursor cursor = cursor (Cursor) getListAdapter().getItem(info.position);
		cursor.moveToPosition(info.position);
		if (item.getTitle().toString().equals(getResources().getString(R.string.menuEdit))) {
			final long rowId = DBTools.getCursorColumnValueLong(cursor, TransactionStatusTableMetaData._ID);
			final EditText input = new EditText(TransactionStatusList.this);
			input.setText(String.valueOf(DBTools.getCursorColumnValue(cursor, TransactionStatusTableMetaData.NAME)));

			Command cmd = new Command() {
				@Override
				public void execute() {
					String oldName = TransactionStatusSrv.getNameByID(TransactionStatusList.this, rowId);
					TransactionStatusSrv.updateStatus(TransactionStatusList.this, rowId, input.getText().toString(), 0, !oldName.equals(input.getText().toString()));
					reloadContentList();
					refreshList();
				}
			};

			AlertDialog dialog = DialogTools.InputDialog(TransactionStatusList.this, cmd, R.string.msgChangeName, input, R.drawable.edit_new);
			dialog.show();
			dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(input.getText().toString().trim().length() != 0);
		}
		else if (item.getTitle().toString().equals(getResources().getString(R.string.menuDelete))) {
			final long rowId = DBTools.getCursorColumnValueLong(cursor, TransactionStatusTableMetaData._ID);
			Command deleteCommand = new Command() {
				@Override
				public void execute() {
					TransactionStatusSrv.deleteStatus(TransactionStatusList.this, rowId);
					reloadContentList();
					refreshList();
				}
			};
			AlertDialog confirmDialog = DialogTools.confirmDialog(TransactionStatusList.this, deleteCommand, R.string.msgConfirm, R.string.msgDeleteItem);
			confirmDialog.show();
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.setHeaderIcon(R.drawable.edit_new);

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		//Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
		cursor.moveToPosition(info.position);
		menu.setHeaderTitle(DBTools.getCursorColumnValue(cursor, TransactionStatusTableMetaData.NAME));

		menu.add(R.string.menuEdit);
		menu.add(R.string.menuDelete);
	}

	public void myClickHandler(View target) {
		switch (target.getId()) {
		case R.id.btDRAddNew:
			/*Intent intent = new Intent(TransactionStatusList.this, AccountEdit.class);
			intent.setAction(Intent.ACTION_INSERT);
			startActivityForResult(intent, Constants.RequestAccountInsert);*/
			final EditText input = new EditText(TransactionStatusList.this);
			Command cmd = new Command() {
				@Override
				public void execute() {
					TransactionStatusSrv.insertStatus(TransactionStatusList.this, input.getText().toString(), 0, null);
					restartActivity();
				}
			};
			AlertDialog dialog = DialogTools.InputDialog(TransactionStatusList.this, cmd, R.string.msgEnter, input, R.drawable.ic_menu_add);
			dialog.show();
			break;
		case R.id.btDRAZ:
			sortAZ();
			break;
		case R.id.drEdit:
			openContextMenu(target);
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
			TransactionStatusList.resultOK = true;
			restartActivity();
		}
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
					saveToDB(TransactionStatusList.this, content);
					TransactionStatusList.this.setResult(RESULT_OK);
					TransactionStatusList.this.finish();
				}
			};
			Command noCmd = new Command() {				
				@Override
				public void execute() {
					TransactionStatusList.this.setResult(RESULT_CANCELED);
					TransactionStatusList.this.finish();
				}
			};
			AlertDialog dialog = DialogTools.confirmDialog(TransactionStatusList.this, yesCmd, noCmd,
					R.string.msgConfirm, getResources().getString(R.string.msgSaveSorting), 
					new String[] {getResources().getString(R.string.Yes), getResources().getString(R.string.No)});
			dialog.show();
		}
		else {
			TransactionStatusList.this.setResult(RESULT_CANCELED);
			TransactionStatusList.this.finish();
		}
	}
}