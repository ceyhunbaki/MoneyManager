package com.jgmoneymanager.budget;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CursorTreeAdapter;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.BudgetCategoriesTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.BudgetTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CategoryTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.main.CategoryFilter;
import com.jgmoneymanager.main.R;
import com.jgmoneymanager.main.TransactionList;
import com.jgmoneymanager.services.BudgetSrv;
import com.jgmoneymanager.services.CategorySrv;
import com.jgmoneymanager.entity.CheckBoxItem;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.entity.Group;
import com.jgmoneymanager.tools.Tools;

import java.util.ArrayList;
import java.util.List;

public class BudgetBalances extends MyActivity {

	private int mGroupIdColumnIndex = 0;
	String totalRemaining = "totalRemaining";
	public static final String budgetIDCol = "budgetID";
	ExpandableListView listView;
	//Date selectedMonth;
	ExpandableListAdapter mAdapter;
	static Boolean dataHasChanged;
	
	final String paramBudget = "budget";
	final String paramCategoryID = "catID";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.budget_balance);
		
		//selectedMonth = Tools.truncDate(Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncMonth);
		if (dataHasChanged == null)
			dataHasChanged = false;
		
		refreshList();
	}
	
	void refreshList() {
		Cursor mGroupsCursor = BudgetSrv.generateGroupCursor(totalRemaining, BudgetStatus.getSelectedMonth(), this, 0, 0);
		mGroupIdColumnIndex = mGroupsCursor.getColumnIndexOrThrow(CategoryTableMetaData._ID);
		mAdapter = new MyExpandableListAdapter(mGroupsCursor, this,
				R.layout.group_budgetrow, R.layout.listbudgetitem,
				new String[] { CategoryTableMetaData.NAME, totalRemaining },
				new int[] { R.id.row2_name, R.id.row2_value },
				new String[] { CategoryTableMetaData.NAME, totalRemaining },
				new int[] { R.id.tvCategory, R.id.tvRemaining });
		
		listView = new ExpandableListView(this);
		listView.setAdapter(mAdapter);
		listView.setGroupIndicator(getResources().getDrawable(R.drawable.expander_group));
		listView.setScrollingCacheEnabled(true);
		listView.setCacheColorHint(00000000);
		listView.setBackgroundColor(getResources().getColor(R.color.White));				
		
		RelativeLayout myLayout = (RelativeLayout) findViewById(R.id.lay_budget_balance);
		listView.setLayoutParams(new RelativeLayout.LayoutParams(
		                                     RelativeLayout.LayoutParams.FILL_PARENT,
		                                     RelativeLayout.LayoutParams.FILL_PARENT));
		myLayout.addView(listView);
		
		registerForContextMenu(listView);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		//getActivity().getMenuInflater().inflate(R.menu.fragment_menu, menu);
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
		int type = ExpandableListView.getPackedPositionType(info.packedPosition);
		menu.setHeaderIcon(R.drawable.ic_menu_manage);
		menu.add(R.string.editBudget);
		menu.add(R.string.addBudget);
		menu.add(R.string.moveBudget);
		menu.add(R.string.menuGoTotransactions);
        menu.add(R.string.status);
		//menu.add(R.string.editRemainingBudget);
		Cursor cursor;
		if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP)
			cursor = (Cursor) listView.getExpandableListAdapter().getGroup(ExpandableListView.getPackedPositionGroup(info.packedPosition));
		else
			cursor = (Cursor) listView.getExpandableListAdapter().getChild(ExpandableListView.getPackedPositionGroup(info.packedPosition), ExpandableListView.getPackedPositionChild(info.packedPosition));
		menu.setHeaderTitle(DBTools.getCursorColumnValue(cursor, CategoryTableMetaData.NAME));
	}
	
	public void myClickHandler(View target) {
		switch (target.getId()) {
		case R.id.bt_group_edit:
			openContextMenu(target);
			break;
		case R.id.bt_child_edit:
			openContextMenu(target);
			break;
		default:
			break;
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
		final int type = ExpandableListView.getPackedPositionType(info.packedPosition);
		final Cursor cursor;
		if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) 
			cursor = (Cursor) listView.getExpandableListAdapter().getChild(ExpandableListView.getPackedPositionGroup(info.packedPosition), ExpandableListView.getPackedPositionChild(info.packedPosition));
		else 
			cursor = (Cursor) listView.getExpandableListAdapter().getGroup(ExpandableListView.getPackedPositionGroup(info.packedPosition));
		if (item.getTitle().toString() == getString(R.string.editBudget)) {
			final long categoryID = DBTools.getCursorColumnValueLong(cursor, CategoryTableMetaData._ID);
			final double oldBudget;
			if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) 
				oldBudget = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.BUDGET);
			else 
				oldBudget = DBTools.getCursorColumnValueDouble(cursor, BudgetSrv.groupBudget);
			final EditText input = new EditText(this);
			input.setText(String.valueOf(oldBudget));
			input.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
			Command command = new Command() {				
				@Override
				public void execute() {
					BudgetSrv.addBudget(BudgetBalances.this, categoryID, BudgetStatus.getSelectedMonth(), Double.parseDouble(input.getText().toString()) - oldBudget);
					cursor.requery();
					if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
						Cursor groupCursor = (Cursor) listView.getExpandableListAdapter().getGroup(ExpandableListView.getPackedPositionGroup(info.packedPosition));
						groupCursor.requery();
					}
					dataHasChanged = true;
				}
			};
			AlertDialog inputDialog = DialogTools.InputDialog(this, command,
                    getResources().getString(R.string.menuEdit) + " " + DBTools.getCursorColumnValue(cursor, CategoryTableMetaData.NAME),
                    input, R.drawable.ic_menu_edit);
			inputDialog.show();
			inputDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(input.getText().toString().trim().length() != 0);
		}
		else if (item.getTitle().toString() == getString(R.string.addBudget)) {
			final long categoryID = DBTools.getCursorColumnValueLong(cursor, CategoryTableMetaData._ID);
			final EditText input = new EditText(this);
			input.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
			Command command = new Command() {				
				@Override
				public void execute() {
					BudgetSrv.addBudget(BudgetBalances.this, categoryID, BudgetStatus.getSelectedMonth(), Double.parseDouble(input.getText().toString()));
					cursor.requery();
					if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
						Cursor groupCursor = (Cursor) listView.getExpandableListAdapter().getGroup(ExpandableListView.getPackedPositionGroup(info.packedPosition));
						groupCursor.requery();
					}
					dataHasChanged = true;
				}
			};
			AlertDialog inputDialog = DialogTools.InputDialog(this, command, 
					getResources().getString(R.string.addBudget), input, R.drawable.ic_menu_add);
			inputDialog.show();
		}
		else if (item.getTitle().toString() == getString(R.string.moveBudget)) {
			final EditText input = new EditText(this);
			input.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
			final Double budget = BudgetSrv.getBudget(this, 
					DBTools.getCursorColumnValueLong(cursor, CategoryTableMetaData._ID), BudgetStatus.getSelectedMonth(), null);
					/*DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.BUDGET)
					+ DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.REMAINING);*/
			final long selectedCategoryID = DBTools.getCursorColumnValueLong(cursor, CategoryTableMetaData._ID);
			Command command = new Command() {				
				@Override
				public void execute() {
					if (!Tools.isCorrectNumber(input.getText().toString()))
						DialogTools.toastDialog(BudgetBalances.this, R.string.msgInvalidNumber, Toast.LENGTH_SHORT);
					else if (Double.compare(Double.parseDouble(input.getText().toString()), budget) > 0) {
						DialogTools.toastDialog(BudgetBalances.this, R.string.movedValueIsTooBig, Toast.LENGTH_SHORT);
					}
					else {
						Intent intent = new Intent(BudgetBalances.this, CategoryFilter.class);
						intent.putExtra(Constants.dontRefreshValues, true);
						intent.putExtra(Constants.disableMultiSelect, true);
						Bundle values = new Bundle();
						values.putDouble(paramBudget, Double.parseDouble(input.getText().toString()));
						values.putLong(paramCategoryID, selectedCategoryID);
						intent.putExtras(values);
						startActivityForResult(intent, Constants.RequestCategoryForBudget);
					}
				}
			};
			input.setText(Tools.formatDecimal(budget, "0"));
			AlertDialog valueDialog = DialogTools.InputDialog(this, command, R.string.selectValue, 
					input, R.drawable.ic_input_add);
			valueDialog.show();
			valueDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(input.getText().toString().trim().length() != 0);
		}
		else if (item.getTitle().toString() == getString(R.string.menuGoTotransactions)) {
			Intent intent = new Intent(this, TransactionList.class);
			intent.setAction(Constants.ActionViewTransactionsFromReport);
			Bundle bundle = new Bundle();
			bundle.putString(Constants.paramFromDate, Tools.DateToDBString(BudgetStatus.getSelectedMonth()));
			bundle.putString(Constants.paramToDate, Tools.DateToDBString(Tools.lastDay(BudgetStatus.getSelectedMonth())));				
			bundle.putLong(Constants.paramCategory, DBTools.getCursorColumnValueLong(cursor, CategoryTableMetaData._ID));				
			bundle.putInt(Constants.reportType, Constants.TransFTransaction.Expence.index());
			intent.putExtras(bundle);
			startActivityForResult(intent, Constants.RequestNONE);
		}
        else if (item.getTitle().toString() == getString(R.string.status)) {
            String categoryName = DBTools.getCursorColumnValue(cursor, CategoryTableMetaData.NAME);
            if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                Double budget = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.BUDGET);
                Double remaining = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.REMAINING);
                Double usedAmount = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.USED_AMOUNT);
                BudgetSrv.showBudgetUsageDialog(this, BudgetStatus.getSelectedBudgetID(), categoryName, usedAmount, remaining, budget, 0, 0, false);
            }
            if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
                Double budget = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.BUDGET);
                Double groupBudget = DBTools.getCursorColumnValueDouble(cursor, BudgetSrv.groupBudget);
                Double remaining = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.REMAINING);
                Double groupRemaining = DBTools.getCursorColumnValueDouble(cursor, BudgetSrv.groupRemaining);
                Double usedAmount = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.USED_AMOUNT);
                BudgetSrv.showBudgetUsageDialog(this, BudgetStatus.getSelectedBudgetID(), categoryName, usedAmount, remaining, budget,
                        groupBudget, groupRemaining, true);
            }
        }
		/*else if (item.getTitle().toString() == getString(R.string.editRemainingBudget)) {
			final double remainingBudget;
			if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) 
				remainingBudget = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.REMAINING);
			else 
				remainingBudget = DBTools.getCursorColumnValueDouble(cursor, BudgetSrv.groupRemaining);
			if (BudgetSrv.haveNextMonthBudget(this, BudgetStatus.getSelectedMonth())) {
				AlertDialog dialog = DialogTools.informationDialog(this, R.string.msgWarning, R.string.msgLastMonthRemaining);
				dialog.show();				
			}
			else if (Double.compare(remainingBudget, 0d) < 0) {
				AlertDialog dialog = DialogTools.informationDialog(this, R.string.msgWarning, R.string.msgDontChangeOverspent);
				dialog.show();
			} 
			else {
				final EditText input = new EditText(this);
				input.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
				input.setText(Tools.formatDecimal(remainingBudget, "0"));
				final long selectedCategoryID = DBTools.getCursorColumnValueLong(cursor, CategoryTableMetaData._ID);				
				Command command = new Command() {				
					@Override
					public void execute() {
						if (!Tools.isCorrectNumber(input.getText().toString()))
							DialogTools.toastDialog(BudgetBalances.this, R.string.msgInvalidNumber, Toast.LENGTH_SHORT);
						else if (Double.compare(Double.parseDouble(input.getText().toString()), remainingBudget) > 0) {
							DialogTools.toastDialog(BudgetBalances.this, R.string.msgYouCannotAddRemaining, Toast.LENGTH_SHORT);
						}
						else if (Double.compare(Double.parseDouble(input.getText().toString()), 0d) < 0) {
							DialogTools.toastDialog(BudgetBalances.this, R.string.msgValueIsLessZero, Toast.LENGTH_SHORT);
						}
						else {
							ContentValues values = new ContentValues();
							values.put(BudgetCategoriesTableMetaData.REMAINING, Tools.formatDecimal(input.getText().toString()));
							BudgetBalances.this.getContentResolver().update(BudgetCategoriesTableMetaData.CONTENT_URI, values, 
									BudgetCategoriesTableMetaData.BUDGET_ID + " = " + BudgetStatus.getSelectedBudgetID()
									+ " and " + BudgetCategoriesTableMetaData.CATEGORY_ID + " = " 
									+ selectedCategoryID, null);
							cursor.requery();
							if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
								Cursor groupCursor = (Cursor) listView.getExpandableListAdapter().getGroup(ExpandableListView.getPackedPositionGroup(info.packedPosition));
								groupCursor.requery();
							}
							dataHasChanged = true;
						}
					}
				};
				
				AlertDialog valueDialog = DialogTools.InputDialog(this, command, R.string.selectValue, 
						input, R.drawable.ic_input_add);
				valueDialog.show();
			}
		}*/
		return false;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == Constants.RequestCategoryForBudget) {
				int selectedCategoryID = 0;
				ArrayList<Group> categoriesList = CategoryFilter.group;
				for (int i = 0; i < categoriesList.size(); i++) {
					Group group = categoriesList.get(i);
					if (group.isChecked())
						selectedCategoryID = group.getID();
					else {
						List<CheckBoxItem> subCategories = group.getChildren();
						for (int j = 0; j < subCategories.size(); j++) {
							CheckBoxItem subCategory = subCategories.get(j);
							if (subCategory.isSelected())
								selectedCategoryID = subCategory.getID();
						}
					}
					if (selectedCategoryID != 0)
						break;
				}
				try {
					Bundle values = data.getExtras();
					BudgetSrv.moveBudget(this, BudgetStatus.getSelectedMonth(), values.getDouble(paramBudget), 
							values.getLong(paramCategoryID), selectedCategoryID);
					long fromCatMainID = CategorySrv.getMainCategoryID(this, values.getLong(paramCategoryID));
					long toCatMainID = CategorySrv.getMainCategoryID(this, selectedCategoryID);
					((CursorTreeAdapter) mAdapter).notifyDataSetChanged(true);
					for (int position=0; position<mAdapter.getGroupCount(); position++)
						if ((mAdapter.getGroupId(position) == fromCatMainID) ||
								mAdapter.getGroupId(position) == toCatMainID) {
							Cursor cursor = (Cursor) listView.getExpandableListAdapter().getGroup(ExpandableListView.getPackedPositionGroup(position));
							Log.i("Group cursor", DBTools.getCursorColumnValue(cursor, CategoryTableMetaData.NAME));
							cursor.requery();
						}
				}
				catch (Exception e) {
					
				}
				dataHasChanged = true;
			}
		}
	}

	public class MyExpandableListAdapter extends SimpleCursorTreeAdapter {

		public MyExpandableListAdapter(Cursor cursor, Context context,
				int groupLayout, int childLayout, String[] groupFrom,
				int[] groupTo, String[] childrenFrom, int[] childrenTo) {
			super(context, cursor, groupLayout, groupFrom, groupTo, childLayout, childrenFrom, childrenTo);
		}		

		@Override
		public void notifyDataSetChanged(boolean releaseCursors) {
			super.notifyDataSetChanged(releaseCursors);
		}

		// returns cursor with subitems for given group cursor
		@Override
		protected Cursor getChildrenCursor(Cursor groupCursor) {
			return generateChildCursor(groupCursor.getLong(mGroupIdColumnIndex));
		}

		// I needed to process click on click of the button on child item
		public View getChildView(final int groupPosition,
				final int childPosition, boolean isLastChild, View convertView,
				ViewGroup parent) {
			View rowView = super.getChildView(groupPosition, childPosition,
					isLastChild, convertView, parent);
			Cursor cursor = (Cursor) super.getChild(groupPosition, childPosition);
			Double budget = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.BUDGET);
			Double remaining = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.REMAINING);
			Double usedAmount = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.USED_AMOUNT);
			
			Integer percent = 0;
			if (usedAmount.compareTo(0d)!= 0)
				if (Tools.negativeToZero(budget + remaining) != 0)
					percent = (int) Math.round(usedAmount * 100 / (Tools.negativeToZero(budget + remaining)));
				else 
					percent = 100;

			TextView tvPercent = (TextView)rowView.findViewById(R.id.tvProgressText);
            tvPercent.setText(Tools.formatDecimal(percent) + "%");
            /*tvPercent.setText(Tools.formatDecimal(percent) + "% ("
                    + getResources().getString(R.string.usedShort) + ":"
                    + Tools.formatDecimal(usedAmount, "0")
                    + ";" + getResources().getString(R.string.budgetShort)
                    + ":" + Tools.formatDecimal(budget, "0")
                    + ";" + getResources().getString(R.string.remainingShort)
                    + ":" + Tools.formatDecimal(remaining, "0") + ")");*/
			
			ProgressBar pBar = (ProgressBar)rowView.findViewById(R.id.progressBar1);
			pBar.setProgress(percent);
			return rowView;
		}

		public View getGroupView(final int groupPosition, final boolean isExpanded,
				View convertView, ViewGroup parent) {
			View rowView = super.getGroupView(groupPosition, isExpanded, convertView, parent);
			rowView.setOnClickListener(new OnClickListener() {				
				@Override
				public void onClick(View v) {
					if (isExpanded)
						listView.collapseGroup(groupPosition);
					else
						listView.expandGroup(groupPosition);
				}
			});
			
			Cursor cursor = (Cursor) super.getGroup(groupPosition);
			Double budget = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.BUDGET);
			//Double groupBudget = DBTools.getCursorColumnValueDouble(cursor, BudgetSrv.groupBudget);
			Double remaining = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.REMAINING);
			//Double groupRemaining = DBTools.getCursorColumnValueDouble(cursor, BudgetSrv.groupRemaining);
			Double usedAmount = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.USED_AMOUNT);
			
			Integer percent = 0;
			if ((budget + remaining <= 0) && (Double.compare(usedAmount, 0) != 0))
				percent = 100;
			else if ((budget + remaining != 0) && (Double.compare(usedAmount, 0) != 0)) 
				percent = (int) Math.round(usedAmount * 100 / (budget + remaining));

			TextView tvPercent = (TextView)rowView.findViewById(R.id.tvProgressText);
            tvPercent.setText(Tools.formatDecimal(percent) + "%");
            /*tvPercent.setText(Tools.formatDecimal(percent) + "% ("
                    + getResources().getString(R.string.usedShort)
                    + ":" + Tools.formatDecimal(usedAmount, "0")
                    + ";" + getResources().getString(R.string.totalBudgetShort)
                    + ":" + Tools.formatDecimal(budget, "0") + ";"
                    + getResources().getString(R.string.budgetShort) + ":"
                    + Tools.formatDecimal(groupBudget, "0")
                    + ";" + getResources().getString(R.string.totalRemainingShort) + ":"
                    + Tools.formatDecimal(remaining, "0") + ";"
                    + getResources().getString(R.string.remainingShort) + ":"
                    + Tools.formatDecimal(groupRemaining, "0") + ")");*/
			
			ProgressBar pBar = (ProgressBar)rowView.findViewById(R.id.progressBar1);
			pBar.setProgress(percent);
			return rowView;
		}

	}

	/*private Cursor generateGroupCursor() {
		String categMainIDColName = "catMainID";
		String categIDColName = "catID";
		String mainSQL = "select c2." + CategoryTableMetaData._ID + ", c2." + CategoryTableMetaData.NAME 
			+ ", ifnull(sum(" + BudgetCategoriesTableMetaData.BUDGET + "),0) " + BudgetCategoriesTableMetaData.BUDGET
			+ ", ifnull(sum(" + BudgetCategoriesTableMetaData.USED_AMOUNT + "),0) " + BudgetCategoriesTableMetaData.USED_AMOUNT 
			+ ", ifnull(sum(" + BudgetCategoriesTableMetaData.BUDGET + " - " + BudgetCategoriesTableMetaData.USED_AMOUNT 
			+ "),0) " + remainingColumn + " from (select " + CategoryTableMetaData._ID + " " + categMainIDColName 
			+ ", " + CategoryTableMetaData._ID + " " + categIDColName + " from " 
			+ CategoryTableMetaData.TABLE_NAME + " where " + CategoryTableMetaData.MAINID + " is null "
			+ "union all select " + CategoryTableMetaData.MAINID + ", " + CategoryTableMetaData._ID + " from "
			+ CategoryTableMetaData.TABLE_NAME + " where " + CategoryTableMetaData.MAINID 
			+ " is not null) c1 join " + CategoryTableMetaData.TABLE_NAME + " c2 on c2." 
			+ CategoryTableMetaData._ID + " = c1." + categMainIDColName + " left join (select b1.* from " 
			+ BudgetCategoriesTableMetaData.TABLE_NAME + " b1 join " + BudgetTableMetaData.TABLE_NAME + " b2 on b2."
			+ BudgetTableMetaData._ID + " = b1." + BudgetCategoriesTableMetaData.BUDGET_ID + " where " 
			+ BudgetTableMetaData.FROM_DATE + " = '" + Tools.DateToDBString(selectedMonth) 				
			+ "')b on b." + BudgetCategoriesTableMetaData.CATEGORY_ID 
			+ " = c1." + categIDColName+ " group by c2." + CategoryTableMetaData._ID + ", c2." 
			+ CategoryTableMetaData.NAME + " order by " + CategoryTableMetaData.NAME ;
		return DBTools.createCursor(context, mainSQL);
	}*/

	private Cursor generateChildCursor(long mainCategoryID) {
		String mainSQL = "select c." + CategoryTableMetaData._ID + ", c." + CategoryTableMetaData.NAME + ", b."
				+ BudgetCategoriesTableMetaData._ID + " " + budgetIDCol + ", " 
				+ BudgetCategoriesTableMetaData.BUDGET + ", " + BudgetCategoriesTableMetaData.USED_AMOUNT 
				+ ", round(" + BudgetCategoriesTableMetaData.BUDGET + " + " + BudgetCategoriesTableMetaData.REMAINING 
				+ " - " + BudgetCategoriesTableMetaData.USED_AMOUNT + ", " + Constants.decimalCount + ") " + totalRemaining + ", "
				+ BudgetCategoriesTableMetaData.REMAINING + ", " + CategoryTableMetaData.MAINID 
				+ " from " + CategoryTableMetaData.TABLE_NAME 
				+ " c left join (select b1.* from " + BudgetCategoriesTableMetaData.TABLE_NAME + " b1 join "
				+ BudgetTableMetaData.TABLE_NAME + " b2 on b1." + BudgetCategoriesTableMetaData.BUDGET_ID  
				+ " = b2." + BudgetTableMetaData._ID + " where b2." + BudgetTableMetaData.FROM_DATE + " = '"
				+ Tools.DateToDBString(BudgetStatus.getSelectedMonth()) + "') b on c." 
				+ CategoryTableMetaData._ID + " = " + BudgetCategoriesTableMetaData.CATEGORY_ID 
				+ " where " + CategoryTableMetaData.MAINID + " = " + mainCategoryID
				+ " order by " + CategoryTableMetaData.NAME;
		return DBTools.createCursor(this, mainSQL);
	}
	
	public static boolean hasDataChanged() {
		if ((dataHasChanged == null) || !dataHasChanged)
			return false;
		else {
			dataHasChanged = false;
			return true;
		}
	}
}