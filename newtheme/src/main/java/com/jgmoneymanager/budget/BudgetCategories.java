package com.jgmoneymanager.budget;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorTreeAdapter;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ProgressBar;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.BudgetCategoriesTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.BudgetTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CategoryTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.CheckBoxItem;
import com.jgmoneymanager.entity.Group;
import com.jgmoneymanager.main.CategoryFilter;
import com.jgmoneymanager.main.R;
import com.jgmoneymanager.main.TransactionList;
import com.jgmoneymanager.services.BudgetSrv;
import com.jgmoneymanager.services.CategorySrv;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import java.util.ArrayList;
import java.util.List;

public class BudgetCategories extends Fragment {

	View rootView;
	Context context;
	private int mGroupIdColumnIndex = 0;
	String totalRemaining = "totalRemaining";
	String budgetIDCol = "budgetID";
	ExpandableListView listView;
	//Date selectedMonth;
	ExpandableListAdapter mAdapter;
	static Boolean dataHasChanged;
	
	final String paramBudget = "budget";
	final String paramCategoryID = "catID";

	boolean includeSubCategories;
	String lastBudget = "0";
	String averageBudget = "0";
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		rootView = inflater.inflate(R.layout.budget_remainings, container, false);
		context = rootView.getContext();		

		//selectedMonth = Tools.truncDate(Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncMonth);
		if (dataHasChanged == null)
			dataHasChanged = false;
		
		refreshList();

		includeSubCategories = Tools.getPreferenceBool(context, R.string.includeSubCategoriesKey, true);

		return rootView;
	}
	
	void refreshList() {
		Cursor mGroupsCursor = BudgetSrv.generateGroupCursor(totalRemaining, ((BudgetMain)getActivity()).getSelectedMonth(), context, 0, 0);
		mGroupIdColumnIndex = mGroupsCursor.getColumnIndexOrThrow(CategoryTableMetaData._ID);
		mAdapter = new MyExpandableListAdapter(mGroupsCursor, context,
				R.layout.group_budgetrow, R.layout.listbudgetitem,
				new String[] { CategoryTableMetaData.NAME, totalRemaining },
				new int[] { R.id.row2_name, R.id.row2_value },
				new String[] { CategoryTableMetaData.NAME, totalRemaining },
				new int[] { R.id.tvCategory, R.id.tvRemaining });

		listView = (ExpandableListView) rootView.findViewById(R.id.list_budget_balance);
		listView.setAdapter(mAdapter);
		
		registerForContextMenu(listView);
		
		//dataHasChanged = true;
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
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
		final int type = ExpandableListView.getPackedPositionType(info.packedPosition);
		final Cursor cursor;
		if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) 
			cursor = (Cursor) listView.getExpandableListAdapter().getChild(ExpandableListView.getPackedPositionGroup(info.packedPosition), ExpandableListView.getPackedPositionChild(info.packedPosition));
		else 
			cursor = (Cursor) listView.getExpandableListAdapter().getGroup(ExpandableListView.getPackedPositionGroup(info.packedPosition));
		if (item.getTitle().toString().equals(getString(R.string.editBudget))) {
			final long categoryID = DBTools.getCursorColumnValueLong(cursor, CategoryTableMetaData._ID);
			final double oldBudget;
			if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD)
				oldBudget = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.BUDGET);
			else
				oldBudget = DBTools.getCursorColumnValueDouble(cursor, BudgetSrv.groupBudget);

			LayoutInflater li = LayoutInflater.from(context);
			View view = li.inflate(R.layout.budget_add_dialog, null);

			TextView tvTitle = (TextView) view.findViewById(R.id.badTitle);
			tvTitle.setText(R.string.editBudget);

			final EditText input = (EditText) view.findViewById(R.id.badValue);
			input.addTextChangedListener(new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}

				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void afterTextChanged(Editable s) {
					if (input.getText().toString().length() != 0)
						try {
							Double.parseDouble(input.getText().toString());
						} catch (NumberFormatException e) {
							DialogTools.toastDialog(context, R.string.msgInvalidNumber, Toast.LENGTH_SHORT);
						}
				}
			});
			input.setText(Tools.formatDecimal(oldBudget));

			final TextView tvLastMonth = (TextView) view.findViewById(R.id.badLastValue);
			final TextView tvAverValue = (TextView) view.findViewById(R.id.badAverValue);
			reloadForDialogBudgetValues(ExpandableListView.PACKED_POSITION_TYPE_CHILD, categoryID, tvLastMonth, tvAverValue);

			view.findViewById(R.id.badSubCategLayout).setVisibility(View.GONE);
			//}

			final int[] budgetRepeat = {DBTools.getCursorColumnValueInt(cursor, BudgetCategoriesTableMetaData.REPEAT)};
			CheckBox checkBox = (CheckBox) view.findViewById(R.id.badCbRepeat);
			checkBox.setChecked(budgetRepeat[0] == 1);
			checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean newValue) {
					budgetRepeat[0] = (newValue ? 1 : 0);
				}
			});

			final AlertDialog dialog = DialogTools.CustomDialog(context, view);

			Command cmOK = new Command() {
				@Override
				public void execute() {
					BudgetSrv.addBudget(context, categoryID, ((BudgetMain)getActivity()).getSelectedMonth(), Tools.stringToDouble(context, input.getText().toString(), false) - oldBudget,
							budgetRepeat[0]);
					cursor.requery();
					if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
						Cursor groupCursor = (Cursor) listView.getExpandableListAdapter().getGroup(ExpandableListView.getPackedPositionGroup(info.packedPosition));
						groupCursor.requery();
					}
					dataHasChanged = true;
					dialog.dismiss();
				}
			};

			Command cmdCancel = new Command() {
				@Override
				public void execute() {
					dialog.dismiss();
				}
			};

			DialogTools.setButtonActions(view, R.id.badOK, R.id.badCancel, cmOK, cmdCancel);

			dialog.show();
		}
		else if (item.getTitle().toString().equals(getString(R.string.addBudget))) {
			final long categoryID = DBTools.getCursorColumnValueLong(cursor, CategoryTableMetaData._ID);

			LayoutInflater li = LayoutInflater.from(context);
			View view = li.inflate(R.layout.budget_add_dialog, null);
			final EditText input = (EditText) view.findViewById(R.id.badValue);

			input.addTextChangedListener(new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}

				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void afterTextChanged(Editable s) {
					if (input.getText().toString().length() != 0)
						try {
							Double.parseDouble(input.getText().toString());
						} catch (NumberFormatException e) {
							DialogTools.toastDialog(context, R.string.msgInvalidNumber, Toast.LENGTH_SHORT);
						}
				}
			});

			final TextView tvLastMonth = (TextView) view.findViewById(R.id.badLastValue);
			final TextView tvAverValue = (TextView) view.findViewById(R.id.badAverValue);
			reloadForDialogBudgetValues(type, categoryID, tvLastMonth, tvAverValue);

			if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
				view.findViewById(R.id.badSubCategLayout).setVisibility(View.GONE);
			}

			CheckBox checkBox = (CheckBox) view.findViewById(R.id.badCbIncludeSub);
			checkBox.setChecked(includeSubCategories);
			checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean newValue) {
					includeSubCategories = newValue;
					Tools.setPreference(context, R.string.includeSubCategoriesKey, includeSubCategories);
					reloadForDialogBudgetValues(type, categoryID, tvLastMonth, tvAverValue);
				}
			});

			final int[] budgetRepeat = {0};
			CheckBox checkBoxRepeat = (CheckBox) view.findViewById(R.id.badCbRepeat);
			checkBoxRepeat.setChecked(false);
			checkBoxRepeat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean newValue) {
					budgetRepeat[0] = newValue ? 1 : 0;
				}
			});

			final AlertDialog dialog = DialogTools.CustomDialog(context, view);

			Command cmdOK = new Command() {
				@Override
				public void execute() {
					BudgetSrv.addBudget(context, categoryID, ((BudgetMain)getActivity()).getSelectedMonth(),
							Tools.stringToDouble(context, input.getText().toString(), false),
							budgetRepeat[0]);
					cursor.requery();
					if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
						Cursor groupCursor = (Cursor) listView.getExpandableListAdapter().getGroup(ExpandableListView.getPackedPositionGroup(info.packedPosition));
						groupCursor.requery();
					}
					dataHasChanged = true;
					dialog.dismiss();
				}
			};

			Command cmdCancel = new Command() {
				@Override
				public void execute() {
					dialog.dismiss();
				}
			};

			DialogTools.setButtonActions(view, R.id.badOK, R.id.badCancel, cmdOK, cmdCancel);

			dialog.show();
		}
		else if (item.getTitle().toString().equals(getString(R.string.moveBudget))) {
			final EditText input = new EditText(context);
			input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
			final Double budget;
			if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
				budget = DBTools.getCursorColumnValueDouble(cursor, BudgetSrv.groupBudget)
						+ DBTools.getCursorColumnValueDouble(cursor, BudgetSrv.groupRemaining);
			}
			else {
				budget = Tools.round(DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.BUDGET)
						+ DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.REMAINING)
				 		/*- DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.USED_AMOUNT)*/);
			}
			input.addTextChangedListener(new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}

				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void afterTextChanged(Editable s) {
					if (input.getText().toString().length() != 0)
						try {
							Double.parseDouble(input.getText().toString());
						} catch (NumberFormatException e) {
							DialogTools.toastDialog(context, R.string.msgInvalidNumber, Toast.LENGTH_SHORT);
						}
				}
			});
            input.setText(Tools.formatDecimal(budget));
			final long selectedCategoryID = DBTools.getCursorColumnValueLong(cursor, CategoryTableMetaData._ID);
			Command command = new Command() {				
				@Override
				public void execute() {
					/*if (!Tools.isCorrectNumber(input.getText().toString()))
						DialogTools.toastDialog(context, R.string.msgInvalidNumber, Toast.LENGTH_SHORT);
					else*/ if (Double.compare(Tools.stringToDouble(context, input.getText().toString(), false), budget) > 0) {
						DialogTools.toastDialog(context, R.string.movedValueIsTooBig, Toast.LENGTH_SHORT);
					}
					else {
						Intent intent = new Intent(context, CategoryFilter.class);
						intent.putExtra(Constants.dontRefreshValues, true);
						intent.putExtra(Constants.disableMultiSelect, true);
						Bundle values = new Bundle();
						values.putDouble(paramBudget, Tools.stringToDouble(context, input.getText().toString(), false));
						values.putLong(paramCategoryID, selectedCategoryID);
						intent.putExtras(values);
						startActivityForResult(intent, Constants.RequestCategoryForBudget);
					}
				}
			};
			AlertDialog valueDialog = DialogTools.InputDialog(context, command, R.string.selectValue, 
					input, R.drawable.ic_input_add);
			valueDialog.show();
			valueDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(input.getText().toString().trim().length() != 0);
		}
		else if (item.getTitle().toString().equals(getString(R.string.menuGoTotransactions))) {
			Intent intent = new Intent(getActivity(), TransactionList.class);
			intent.setAction(Constants.ActionViewTransactionsFromReport);
			Bundle bundle = new Bundle();
			bundle.putString(Constants.paramFromDate, Tools.DateToDBString(((BudgetMain)getActivity()).getSelectedMonth()));
			bundle.putString(Constants.paramToDate, Tools.DateToDBString(Tools.lastDay(context, ((BudgetMain)getActivity()).getSelectedMonth())));
			bundle.putLong(Constants.paramCategory, DBTools.getCursorColumnValueLong(cursor, CategoryTableMetaData._ID));				
			bundle.putInt(Constants.reportType, Constants.TransFTransaction.Expence.index());
			intent.putExtras(bundle);
			startActivityForResult(intent, Constants.RequestNONE);
		}
        else if (item.getTitle().toString().equals(getString(R.string.status))) {
            String categoryName = DBTools.getCursorColumnValue(cursor, CategoryTableMetaData.NAME);
            if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                Double budget = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.BUDGET);
                Double remaining = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.REMAINING);
                Double usedAmount = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.USED_AMOUNT);
                BudgetSrv.showBudgetUsageDialog(context, BudgetMain.getSelectedBudgetID(), categoryName, usedAmount, remaining, budget, 0, 0, false);
            }
            if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
                Double budget = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.BUDGET);
                Double groupBudget = DBTools.getCursorColumnValueDouble(cursor, BudgetSrv.groupBudget);
                Double remaining = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.REMAINING);
                Double groupRemaining = DBTools.getCursorColumnValueDouble(cursor, BudgetSrv.groupRemaining);
                Double usedAmount = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.USED_AMOUNT);
                BudgetSrv.showBudgetUsageDialog(context, BudgetMain.getSelectedBudgetID(), categoryName, usedAmount, remaining, budget,
                        groupBudget, groupRemaining, true);
            }
        }
		return false;
	}

	void reloadForDialogBudgetValues(int dialogType, long categoryID, TextView tvLastMonth, TextView tvAverBudget) {
		if ((dialogType == ExpandableListView.PACKED_POSITION_TYPE_GROUP) && (includeSubCategories)) {
			lastBudget = Tools.formatDecimalInUserFormat(BudgetSrv.getGroupBudget(context, Tools.AddMonth(((BudgetMain)getActivity()).getSelectedMonth(), -1), categoryID, Constants.defaultCurrency));
			averageBudget = Tools.formatDecimalInUserFormat(BudgetSrv.getGroupAverageBudget(context, Tools.AddMonth(((BudgetMain)getActivity()).getSelectedMonth(), -13),
					Tools.AddMonth(((BudgetMain)getActivity()).getSelectedMonth(), -1), categoryID, Constants.defaultCurrency));
		}
		else {
			lastBudget = Tools.formatDecimalInUserFormat(BudgetSrv.getBudget(context, categoryID, Tools.AddMonth(((BudgetMain)getActivity()).getSelectedMonth(), -1), null, null));
			averageBudget = Tools.formatDecimalInUserFormat(BudgetSrv.getCategoryAverageBudget(context, Tools.AddMonth(((BudgetMain)getActivity()).getSelectedMonth(), -13),
					Tools.AddMonth(((BudgetMain)getActivity()).getSelectedMonth(), -1), categoryID, Constants.defaultCurrency));
		}
		tvLastMonth.setText(lastBudget);
		tvAverBudget.setText(averageBudget);
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
					BudgetSrv.moveBudget(context, ((BudgetMain)getActivity()).getSelectedMonth(), values.getDouble(paramBudget),
							values.getLong(paramCategoryID), selectedCategoryID);
					long fromCatMainID = CategorySrv.getMainCategoryID(context, values.getLong(paramCategoryID));
					long toCatMainID = CategorySrv.getMainCategoryID(context, selectedCategoryID);
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
			View rowView = super.getChildView(groupPosition, childPosition, isLastChild, convertView, parent);
			Cursor cursor = super.getChild(groupPosition, childPosition);
			Double budget = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.BUDGET);
			Double remaining = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.REMAINING);
			Double usedAmount = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.USED_AMOUNT);
			
			Integer percent = 0;
			if (usedAmount.compareTo(0d)!= 0)
				if (Tools.negativeToZero(budget + remaining) != 0)
					percent = (int) Math.round(usedAmount * 100 / (Tools.negativeToZero(budget + remaining)));
				else 
					percent = 100;

			//TextView tvPercent = (TextView)rowView.findViewById(R.id.tvProgressText);
            //tvPercent.setText(Tools.formatDecimal(percent) + "% ");
			
			ProgressBar pBar = (ProgressBar)rowView.findViewById(R.id.progressBar1);
			pBar.setProgress(percent);

			Double totalRemainingValue = DBTools.getCursorColumnValueDouble(cursor, totalRemaining);
			((TextView) rowView.findViewById(R.id.tvRemaining)).setText(Tools.formatDecimalInUserFormat(totalRemainingValue));

			return rowView;
		}

		public View getGroupView(final int groupPosition, final boolean isExpanded,
				View convertView, ViewGroup parent) {
			View rowView = super.getGroupView(groupPosition, isExpanded, convertView, parent);
			rowView.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					if (isExpanded)
						listView.collapseGroup(groupPosition);
					else
						listView.expandGroup(groupPosition);
				}
			});

			Cursor cursor = super.getGroup(groupPosition);
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

			//TextView tvPercent = (TextView)rowView.findViewById(R.id.tvProgressText);
            //tvPercent.setText(Tools.formatDecimal(percent) + "%");
			
			ProgressBar pBar = (ProgressBar)rowView.findViewById(R.id.progressBar1);
			pBar.setProgress(percent);

			Double totalRemainingValue = DBTools.getCursorColumnValueDouble(cursor, totalRemaining);
			((TextView) rowView.findViewById(R.id.row2_value)).setText(Tools.formatDecimalInUserFormat(totalRemainingValue));

			return rowView;
		}

	}

	private Cursor generateChildCursor(long mainCategoryID) {
		String mainSQL = "select c." + CategoryTableMetaData._ID + ", c." + CategoryTableMetaData.NAME + ", b."
				+ BudgetCategoriesTableMetaData._ID + " " + budgetIDCol + ", " 
				+ BudgetCategoriesTableMetaData.BUDGET + ", " + BudgetCategoriesTableMetaData.USED_AMOUNT 
				+ ", " + BudgetCategoriesTableMetaData.BUDGET + " + " + BudgetCategoriesTableMetaData.REMAINING
				+ " - " + BudgetCategoriesTableMetaData.USED_AMOUNT + " " + totalRemaining + ", "
				+ BudgetCategoriesTableMetaData.REMAINING + ", " + CategoryTableMetaData.MAINID
				+ ", " + BudgetCategoriesTableMetaData.REPEAT
				+ " from " + CategoryTableMetaData.TABLE_NAME 
				+ " c left join (select b1.* from " + BudgetCategoriesTableMetaData.TABLE_NAME + " b1 join "
				+ BudgetTableMetaData.TABLE_NAME + " b2 on b1." + BudgetCategoriesTableMetaData.BUDGET_ID  
				+ " = b2." + BudgetTableMetaData._ID + " where b2." + BudgetTableMetaData.FROM_DATE + " = '"
				+ Tools.DateToDBString(((BudgetMain)getActivity()).getSelectedMonth()) + "') b on c."
				+ CategoryTableMetaData._ID + " = " + BudgetCategoriesTableMetaData.CATEGORY_ID 
				+ " where " + CategoryTableMetaData.MAINID + " = " + mainCategoryID
				+ " order by " + CategoryTableMetaData.NAME;
		return DBTools.createCursor(context, mainSQL);
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