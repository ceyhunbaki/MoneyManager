package com.jgmoneymanager.paid;

import android.app.AlertDialog;
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
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransferTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VTransferViewMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.paid.R;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

public class RPTransactionList extends MyActivity {

	final int menuAdd = Menu.FIRST;
	final int menuDeleteAll = menuAdd + 1;
	final int menuEdit = menuAdd + 2;
	final int menuDelete = menuAdd + 3;
	final int menuTransactions = menuAdd + 4;
	final int menuRemind = menuAdd + 5;
	final int menuDeleteAllFinished = menuAdd + 6;

	String[] repeatType = null;
	Uri selectedUri;
	ListView listView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

		LinearLayout mainLayout = (LinearLayout) findViewById(R.id.content_main_layout);
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		View child = inflater.inflate(R.layout.transfer_list, null);
		mainLayout.addView(child, params);

		repeatType = getResources().getStringArray(R.array.TransferTypes);

		Cursor cursor;

		cursor = this.managedQuery(VTransferViewMetaData.CONTENT_URI, null,
				VTransferViewMetaData.FROMACCOUNTID + " is null or "
				+ VTransferViewMetaData.TOACCOUNTID + " is null ",
				null, null);
		String[] from = new String[] { VTransferViewMetaData.DESCRIPTION,
				VTransferViewMetaData.AMOUNT,
				VTransferViewMetaData.REPEATTYPE,
				VTransferViewMetaData.ACCOUNTLABEL,
				VTransferViewMetaData.NEXTPAYMENT,
				VTransferViewMetaData.PERIODEND,
				VTransferViewMetaData.CATEGORYNAME,
				VTransferViewMetaData.CURRENCY_SIGN};
		int[] to = new int[] { R.id.ltRpDescription, R.id.ltRpAmount,
				R.id.ltRpType, R.id.ltRpAccounts, R.id.ltRpDate, R.id.ltRpLatest, R.id.ltRpCategory };
		SimpleCursorAdapter notes = new MyListAdapter(cursor, this, R.layout.listrptransrow, from, to);
		listView = (ListView) findViewById(R.id.trListView);
		listView.setAdapter(notes);
		listView.setScrollingCacheEnabled(true);
		listView.setCacheColorHint(00000000);
		listView.setBackgroundColor(getResources().getColor(R.color.White));
		registerForContextMenu(listView);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.setHeaderIcon(R.drawable.ic_menu_manage);
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		Cursor cursor = (Cursor) ((ListView) findViewById(R.id.trListView)).getAdapter().getItem(info.position);
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

			Cursor cursor = (Cursor) ((ListView) findViewById(R.id.trListView)).getAdapter().getItem(info.position);
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

			TextView tvAmount = (TextView) view.findViewById(R.id.ltRpAmount);
			Double amount = DBTools.getCursorColumnValueDouble(cursor, VTransferViewMetaData.AMOUNT);
			String currSign = DBTools.getCursorColumnValue(cursor, VTransferViewMetaData.CURRENCY_SIGN);
			tvAmount.setText(Tools.getFullAmountText(amount, currSign, true));

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
				if ((listView != null) && (listView.getCount() > 0)) {
					Command deleteAllCommand;
					deleteAllCommand = new Command() {
						public void execute() {
							RPTransactionEdit.deleteAll(RPTransactionList.this);
						}
					};
					AlertDialog deleteAllDialog = DialogTools.confirmDialog(RPTransactionList.this, deleteAllCommand, R.string.msgConfirm, R.string.msgDeleteAll);
					deleteAllDialog.show();
				}
				break;
			case R.id.btTrDeleteFinished:
				if ((listView != null) && (listView.getCount() > 0)) {
					Command deleteAllFinishedCommand;
					deleteAllFinishedCommand = new Command() {
						public void execute() {
							RPTransactionEdit.deleteAllFinished(RPTransactionList.this);
						}
					};
					AlertDialog deleteAllFinishedDialog = DialogTools.confirmDialog(RPTransactionList.this, deleteAllFinishedCommand, R.string.msgConfirm,
							R.string.msgDeleteAllFinished);
					deleteAllFinishedDialog.show();
				}
				break;
			default:
				break;
		}
	}
}
