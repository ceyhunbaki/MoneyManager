package com.jgmoneymanager.main;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.ericharlow.DragNDrop.DragListener;
import com.ericharlow.DragNDrop.DragNDropAdapter;
import com.ericharlow.DragNDrop.DragNDropListView;
import com.ericharlow.DragNDrop.DropListener;
import com.ericharlow.DragNDrop.RemoveListener;
import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.PaymentMethodsTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyListActivity;
import com.jgmoneymanager.services.PaymentMethodsSrv;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import java.util.ArrayList;

public class PaymentMethodList extends MyListActivity {

	ArrayList<String> content;

	static boolean resultOK = false;
	/**
	 * If order of items changed it changes to true
	 */
	boolean orderChanged = false;

	Cursor cursor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.accountsort);
		super.setCustomTitle(R.string.paymentMethod);

        if (savedInstanceState == null) {
	        reloadContentList();
        }
        else {
			cursor = getContentResolver().query(PaymentMethodsTableMetaData.CONTENT_URI, null, null, null, PaymentMethodsTableMetaData.SORTORDER);
        	content = Tools.getStringArrayListFromBundle(savedInstanceState, "content");
        	orderChanged = Tools.getBooleanFromBundle0(savedInstanceState, "orderChanged");
        	resultOK = Tools.getBooleanFromBundle0(savedInstanceState, "resultOK");
        }

        refreshList();

		registerForContextMenu(getListView());
    }

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Tools.putToBundle(outState, "content", content);
		Tools.putToBundle(outState, "orderChanged", orderChanged);
		Tools.putToBundle(outState, "resultOK", resultOK);
		super.onSaveInstanceState(outState);
	}

	void refreshList() {
        setListAdapter(new DragNDropAdapter(this, new int[]{R.layout.dragitem_new}, new int[]{R.id.drText}, content));
        ListView listView = getListView();

        if (listView instanceof DragNDropListView) {
        	((DragNDropListView) listView).setDropListener(mDropListener);
        	((DragNDropListView) listView).setRemoveListener(mRemoveListener);
        	((DragNDropListView) listView).setDragListener(mDragListener);
        }

        listView.setBackgroundColor(getResources().getColor(R.color.White));
	}

	void reloadContentList() {
		cursor = getContentResolver().query(PaymentMethodsTableMetaData.CONTENT_URI, null, null, null, PaymentMethodsTableMetaData.SORTORDER);
		content = new ArrayList<String>(cursor.getCount());
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			content.add(DBTools.getCursorColumnValue(cursor, PaymentMethodsTableMetaData.NAME));
		}
	}

	public void restartActivity() {
	    Intent intent = getIntent();
	    finish();
	    startActivity(intent);
	}

	private void sortAZ() {
		Cursor cursor = getContentResolver().query(PaymentMethodsTableMetaData.CONTENT_URI, null, null, null, PaymentMethodsTableMetaData.NAME);
        content = new ArrayList<String>(cursor.getCount());
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
        	content.add(DBTools.getCursorColumnValue(cursor, PaymentMethodsTableMetaData.NAME));
        }
        refreshList();
		//ardicilliq deyiwiklik olubsa cixiwda save soruwaq
        orderChanged = true;
	}

	private void saveToDB(Context context, ArrayList<String> content){
		ContentValues values = new ContentValues();
		for (int i = 0; i < content.size(); i++) {
			values.clear();
			values.put(PaymentMethodsTableMetaData.SORTORDER, i);
			context.getContentResolver().update(PaymentMethodsTableMetaData.CONTENT_URI, values,
					PaymentMethodsTableMetaData.NAME + " = '" + content.get(i) + "'", null);
		}
	}

	private DropListener mDropListener =
		new DropListener() {
        public void onDrop(int from, int to) {
        	ListAdapter adapter = getListAdapter();
        	if (adapter instanceof DragNDropAdapter) {
        		((DragNDropAdapter)adapter).onDrop(from, to);
        		getListView().invalidateViews();
        	}
        }
    };

    private RemoveListener mRemoveListener =
        new RemoveListener() {
        public void onRemove(int which) {
        	ListAdapter adapter = getListAdapter();
        	if (adapter instanceof DragNDropAdapter) {
        		((DragNDropAdapter)adapter).onRemove(which);
        		getListView().invalidateViews();
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
			final long rowId = DBTools.getCursorColumnValueLong(cursor, PaymentMethodsTableMetaData._ID);
			final EditText input = new EditText(PaymentMethodList.this);
			input.setText(String.valueOf(DBTools.getCursorColumnValue(cursor, PaymentMethodsTableMetaData.NAME)));

			Command cmd = new Command() {
				@Override
				public void execute() {
					String oldName = PaymentMethodsSrv.getNameByID(PaymentMethodList.this, rowId);
					PaymentMethodsSrv.updateMethod(PaymentMethodList.this, rowId, input.getText().toString(), 0, !oldName.equals(input.getText().toString()));
					reloadContentList();
					refreshList();
				}
			};

			AlertDialog dialog = DialogTools.InputDialog(PaymentMethodList.this, cmd, R.string.msgChangeName, input, R.drawable.edit_new);
			dialog.show();
			dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(input.getText().toString().trim().length() != 0);
		}
		else if (item.getTitle().toString().equals(getResources().getString(R.string.menuDelete))) {
			final long rowId = DBTools.getCursorColumnValueLong(cursor, PaymentMethodsTableMetaData._ID);
			Command deleteCommand = new Command() {
				@Override
				public void execute() {
					PaymentMethodsSrv.deleteMethod(PaymentMethodList.this, rowId);
					reloadContentList();
					refreshList();
				}
			};
			AlertDialog confirmDialog = DialogTools.confirmDialog(PaymentMethodList.this, deleteCommand, R.string.msgConfirm, R.string.msgDeleteItem);
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
		menu.setHeaderTitle(DBTools.getCursorColumnValue(cursor, PaymentMethodsTableMetaData.NAME));

		menu.add(R.string.menuEdit);
		menu.add(R.string.menuDelete);
	}

	public void myClickHandler(View target) {
		switch (target.getId()) {
		case R.id.btDRAddNew:
			/*Intent intent = new Intent(TransactionStatusList.this, AccountEdit.class);
			intent.setAction(Intent.ACTION_INSERT);
			startActivityForResult(intent, Constants.RequestAccountInsert);*/
			final EditText input = new EditText(PaymentMethodList.this);
			Command cmd = new Command() {
				@Override
				public void execute() {
					PaymentMethodsSrv.insertMethod(PaymentMethodList.this, input.getText().toString(), 0, null);
					restartActivity();
				}
			};
			AlertDialog dialog = DialogTools.InputDialog(PaymentMethodList.this, cmd, R.string.msgEnter, input, R.drawable.ic_menu_add);
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
			PaymentMethodList.resultOK = true;
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
					saveToDB(PaymentMethodList.this, content);
					PaymentMethodList.this.setResult(RESULT_OK);
					PaymentMethodList.this.finish();
				}
			};
			Command noCmd = new Command() {
				@Override
				public void execute() {
					PaymentMethodList.this.setResult(RESULT_CANCELED);
					PaymentMethodList.this.finish();
				}
			};
			AlertDialog dialog = DialogTools.confirmDialog(PaymentMethodList.this, yesCmd, noCmd,
					R.string.msgConfirm, getResources().getString(R.string.msgSaveSorting),
					new String[] {getResources().getString(R.string.Yes), getResources().getString(R.string.No)});
			dialog.show();
		}
		else {
			PaymentMethodList.this.setResult(RESULT_CANCELED);
			PaymentMethodList.this.finish();
		}
	}
}