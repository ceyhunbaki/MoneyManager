package com.jgmoneymanager.main;

import android.app.AlertDialog;
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
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransferTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VTransferViewMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyListActivity;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

public class RPTransactionList extends MyListActivity {

	final int menuAdd = Menu.FIRST;
	final int menuDeleteAll = menuAdd + 1;
	final int menuEdit = menuAdd + 2;
	final int menuDelete = menuAdd + 3;
	final int menuTransactions = menuAdd + 4;
	final int menuRemind = menuAdd + 5;
	final int menuDeleteAllFinished = menuAdd + 6;

	String[] repeatType = null;
	Uri selectedUri;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.transfer_list);

		((TextView)findViewById(R.id.tvATTitle)).setText(R.string.menuRepeatingTransactions);

		repeatType = getResources().getStringArray(R.array.TransferTypes);

		Cursor cursor;

		cursor = this.managedQuery(VTransferViewMetaData.CONTENT_URI, null,
				VTransferViewMetaData.FROMACCOUNTID + " is null or "
				+ VTransferViewMetaData.TOACCOUNTID + " is null ",
				null, null);
		String[] from = new String[] { VTransferViewMetaData.DESCRIPTION,
				VTransferViewMetaData.LBAMOUNT,
				VTransferViewMetaData.REPEATTYPE,
				VTransferViewMetaData.ACCOUNTLABEL,
				VTransferViewMetaData.NEXTPAYMENT,
				VTransferViewMetaData.PERIODEND,
				VTransferViewMetaData.CATEGORYNAME};
		int[] to = new int[] { R.id.ltRpDescription, R.id.ltRpAmount,
				R.id.ltRpType, R.id.ltRpAccounts, R.id.ltRpDate, R.id.ltRpLatest, R.id.ltRpCategory };
		SimpleCursorAdapter notes = new MyListAdapter(cursor, this, R.layout.listrptransrow, from, to);
		setListAdapter(notes);

		ListView lv = getListView();
		lv.setScrollingCacheEnabled(true);
		lv.setCacheColorHint(00000000);
		lv.setBackgroundColor(getResources().getColor(R.color.White));
		registerForContextMenu(lv);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.setHeaderIcon(R.drawable.ic_menu_manage);
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
		menu.setHeaderTitle(DBTools.getCursorColumnValue(cursor,
				VTransferViewMetaData.DESCRIPTION));
		menu.add(0, menuEdit, 0, R.string.menuEdit);
		menu.add(0, menuDelete, 1, R.string.menuDelete);
		menu.add(1, menuTransactions, 2, R.string.menuGoTotransactions);
		menu.add(2, menuRemind, 3, R.string.menuRemind);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();
		Intent intent;
		switch (item.getItemId()) {
		case menuEdit:
			intent = new Intent(this, RPTransactionEdit.class);
			intent.setAction(Intent.ACTION_EDIT);
			selectedUri = Uri.withAppendedPath(VTransferViewMetaData.CONTENT_URI, String.valueOf(info.id));
			intent.setData(selectedUri);
			startActivityForResult(intent, Constants.RequestEditRPTransactionForTransfer);
			break;
		case menuDelete:
			Command deleteCommand = new Command() {
				public void execute() {
					TransferEdit.deleteTransfer(RPTransactionList.this, info.id, true);
				}
			};
			AlertDialog deleteDialog = DialogTools.confirmDialog(
					RPTransactionList.this, deleteCommand, R.string.msgConfirm,
					R.string.msgDeleteItem);
			deleteDialog.show();
			break;
		case menuTransactions:
			intent = new Intent(this, TransactionList.class);
			intent.setAction(Constants.ActionViewTransactionsByTransfer);
			Bundle bundle = new Bundle();
			bundle.putLong(Constants.paramTransferID, info.id);
			intent.putExtras(bundle);
			startActivityForResult(intent, 1);
			break;
		case menuRemind:
			/*final Spinner spValues = new Spinner(RPTransactionList.this);
			ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(
					RPTransactionList.this, R.layout.spinner_item, R.id.spinneritem,
					getResources().getStringArray(R.array.RPTransRemindValues));
			spValues.setAdapter(spinnerAdapter);
			
			Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
			int selectedItem = DBTools.getCursorColumnValueInt(cursor, VTransferViewMetaData.REMINDER);
			if (selectedItem > 10) 
				selectedItem = selectedItem / 10;
			spValues.setSelection(selectedItem);
			
			Command cmd = new Command() {				
				@Override
				public void execute() {
					TransferEdit.updateTransferReminder(RPTransactionList.this, info.id, spValues.getSelectedItemId());					
				}
			};
			AlertDialog reminderDialog = DialogTools.InputDialog(RPTransactionList.this, cmd, R.string.menuRemind, spValues, R.drawable.ic_menu_edit);
			reminderDialog.show();*/
			LayoutInflater li = LayoutInflater.from(this);
			View view = li.inflate(R.layout.radiolistdialog, null);

			final Spinner spValues = (Spinner) view.findViewById(R.id.rdSpinner);

			ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, R.layout.simple_spinner_item_blue, getResources().getStringArray(R.array.RPTransRemindValues));
			spinnerAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_blue);
			spValues.setAdapter(spinnerAdapter);

			Command cmd = new Command() {
				@Override
				public void execute() {
					TransferEdit.updateTransferReminder(RPTransactionList.this, info.id, spValues.getSelectedItemId());
				}
			};

			Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
			int selectedItem = DBTools.getCursorColumnValueInt(cursor, VTransferViewMetaData.REMINDER);
			if (selectedItem > 10)
				selectedItem = selectedItem / 10;
			spValues.setSelection(selectedItem);

			AlertDialog listDialog = DialogTools.RadioListDialog(RPTransactionList.this, cmd, R.string.menuRemind, view);
			listDialog.show();
			break;
		default:
			break;
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case menuAdd:
			Intent intent = new Intent(this, RPTransactionEdit.class);
			intent.setAction(Intent.ACTION_INSERT);
			startActivityForResult(intent, Constants.RequestNewRPTransactionForTransfer);
			break;
		case menuDeleteAll:
			Command deleteAllCommand;
			deleteAllCommand = new Command() {
				public void execute() {
					RPTransactionEdit.deleteAll(RPTransactionList.this);
				}};
			AlertDialog deleteAllDialog = DialogTools.confirmDialog(
					RPTransactionList.this, deleteAllCommand, R.string.msgConfirm,
					R.string.msgDeleteAll);
			deleteAllDialog.show();
			break;
		case menuDeleteAllFinished:
			Command deleteAllFinishedCommand;
			deleteAllFinishedCommand = new Command() {
				public void execute() {
					RPTransactionEdit.deleteAllFinished(RPTransactionList.this);
				}};
			AlertDialog deleteAllFinishedDialog = DialogTools.confirmDialog(RPTransactionList.this, deleteAllFinishedCommand, R.string.msgConfirm,
					R.string.msgDeleteAllFinished);
			deleteAllFinishedDialog.show();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public class MyListAdapter extends SimpleCursorAdapter {

		Context context;

		public MyListAdapter(Cursor cursor, Context context, int rowId,
				String[] from, int[] to) {
			super(context, rowId, cursor, from, to);
			this.context = context;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = super.getView(position, convertView, parent);
			Cursor cursor = (Cursor) super.getItem(position);
			if (view == null) {
				LayoutInflater inflater = LayoutInflater.from(context);
				view = inflater.inflate(R.layout.listrptransrow, null);
			}

			int currenctRepeatType = DBTools.getCursorColumnValueInt(cursor,
					VTransferViewMetaData.REPEATTYPE);
			String nextPayment = DBTools.getCursorColumnValue(cursor,
					VTransferViewMetaData.NEXTPAYMENT);
			Integer isEnabled = DBTools.getCursorColumnValueInt(cursor,
					VTransferViewMetaData.ISENABLED);

			if (isEnabled == Constants.Status.Disabled.index()) {
				view.setBackgroundColor(getResources().getColor(
						R.color.LightGrey));
			} else if (position % 2 == 0)
				view.setBackgroundColor(getResources().getColor(
						R.color.AntiqueWhite));
			else
				view.setBackgroundColor(getResources().getColor(R.color.White));

			TextView tvType = (TextView) view.findViewById(R.id.ltRpType);
			tvType.setText(repeatType[DBTools.getCursorColumnValueInt(cursor,
					VTransferViewMetaData.REPEATTYPE)]);
			if (DBTools.getCursorColumnValueLong(cursor, VTransferViewMetaData.REMINDER) != Constants.RPTransRemindValues.Never.index())
				tvType.append(" " + getResources().getString(R.string.reminderIcon));

			TextView tvNextDate = (TextView) view.findViewById(R.id.ltRpDate);
			TextView tvLatestDate = (TextView) view
					.findViewById(R.id.ltRpLatest);
			if (currenctRepeatType == Constants.TransferType.Once.index()) {
				tvNextDate.setText(getString(R.string.date)
						+ ":"
						+ Tools.DateToString(DBTools.getCursorColumnValueDate(
								cursor, VTransferViewMetaData.TRANSDATE),
								Constants.DateFormatUser));
			}
			else {
				if (nextPayment != null)
					tvNextDate.setText(getString(R.string.next) + ":"
							+ Tools.DBDateToUserDate(nextPayment));
				else
					tvNextDate.setText(getString(R.string.first)
							+ ":"
							+ Tools.DBDateToUserDate(DBTools
									.getCursorColumnValue(cursor,
											TransferTableMetaData.TRANSDATE)));

				tvLatestDate.setText(getString(R.string.latest)
						+ ":"
						+ Tools.DBDateToUserDate(DBTools.getCursorColumnValue(
								cursor, VTransferViewMetaData.PERIODEND)));
			}
			return view;
		}
	}
	
	public void myClickHandler(View target) {
		switch (target.getId()) {
			case R.id.igRpMenu:
				openContextMenu(target);
				break;
			case R.id.btTrAdd:
				Intent intent = new Intent(RPTransactionList.this, RPTransactionEdit.class);
				intent.setAction(Intent.ACTION_INSERT);
				startActivityForResult(intent, Constants.RequestNewRPTransactionForTransfer);
				break;
			case R.id.btTrDeleteAll:
				Command deleteAllCommand;
				deleteAllCommand = new Command() {
					public void execute() {
						RPTransactionEdit.deleteAll(RPTransactionList.this);
					}};
				AlertDialog deleteAllDialog = DialogTools.confirmDialog(RPTransactionList.this, deleteAllCommand, R.string.msgConfirm, R.string.msgDeleteAll);
				deleteAllDialog.show();
				break;
			case R.id.btTrDeleteFinished:
				Command deleteAllFinishedCommand;
				deleteAllFinishedCommand = new Command() {
					public void execute() {
						RPTransactionEdit.deleteAllFinished(RPTransactionList.this);
					}};
				AlertDialog deleteAllFinishedDialog = DialogTools.confirmDialog(RPTransactionList.this, deleteAllFinishedCommand, R.string.msgConfirm,
						R.string.msgDeleteAllFinished);
				deleteAllFinishedDialog.show();
				break;
			default:
				break;
		}
	}
}
