package com.jgmoneymanager.budget;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.BudgetCategoriesTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.BudgetTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.main.R;
import com.jgmoneymanager.main.SettingsScreen;
import com.jgmoneymanager.services.BudgetSrv;
import com.jgmoneymanager.services.CurrencySrv;
import com.jgmoneymanager.services.ReportSrv;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import java.util.Date;

public class BudgetStatus extends MyActivity {
	
    private static Date selectedMonth;
    private static long selectedBudgetID;
	final int dateIntervalMonthlyDialogID = 3;
	Spinner spinnerYear;
	Spinner spinnerMonth;
	int minYear;
	String years[] = new String[] {};
	
	final int menuResetBudget = Menu.FIRST;
	final int menuChangeTotal = menuResetBudget + 1;
	final int menuValues      = menuResetBudget + 2;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		setContentView(R.layout.budget_status);
		
		if (Tools.getPreferenceBool(BudgetStatus.this, R.string.enablebudgetkey, true))
			BudgetSrv.controlNewMonth(getBaseContext());
		else {
			Command cmdOpenSettings = new Command() {				
				@Override
				public void execute() {
					Intent intent = new Intent(BudgetStatus.this, SettingsScreen.class);
					startActivityForResult(intent, Constants.RequestNONE);				
				}
			};
			AlertDialog dialog = DialogTools.confirmDialog(BudgetStatus.this, cmdOpenSettings,
                    R.string.msgWarning, R.string.msgBudgetDisabled,
                    new String[]{getResources().getString(R.string.settings), getResources().getString(R.string.Cancel)});
			dialog.show();
		}
		
		if (savedInstanceState != null)
			selectedMonth = Tools.getDateFromBundle(savedInstanceState, "selectedMonth");
		else 
			selectedMonth = Tools.truncDate(Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncMonth);
		minYear = BudgetSrv.getBudgetMinimumYear(getBaseContext());
		int currentYear = Tools.getCurrentDate().getYear() + 1900;
		for (int i=minYear; i<=currentYear; i++)
			years = Tools.addElement(years, String.valueOf(i));
        
		refreshDateButtonText(false);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Tools.putToBundle(outState, "selectedMonth", selectedMonth);
	}

	@Override
	public boolean  onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, menuChangeTotal, menuChangeTotal, R.string.menuChangeTotalAmount);
		menu.add(0, menuResetBudget, menuResetBudget, R.string.menuResetBudget);
		menu.add(0, menuValues,  menuValues,  R.string.menuSetValues);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == menuChangeTotal) {
			final Double currentValue = BudgetSrv.getBudgetTotalAmount(BudgetStatus.this, getSelectedMonth());
			final Double realAmount = BudgetSrv.getTotalAmountForBudget(BudgetStatus.this, getSelectedMonth(), 
					Tools.lastDay(getSelectedMonth()));
			Command realAmountYesCommand = new Command() {				
				@Override
				public void execute() {
					BudgetSrv.changeBudgetTotalAmount(BudgetStatus.this, getSelectedMonth(), realAmount);
					refreshValues();
				}
			};
			Command realAmountNoCommand = new Command() {				
				@Override
				public void execute() {
					final EditText inputField = new EditText(BudgetStatus.this);
					inputField.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
					inputField.setText(Tools.formatDecimal(currentValue));
					Command cmdChangeManually = new Command() {						
						@Override
						public void execute() {
							BudgetSrv.changeBudgetTotalAmount(BudgetStatus.this, getSelectedMonth(), 
									Double.parseDouble(inputField.getText().toString()));
							refreshValues();
						}
					};
					AlertDialog inputDialog = DialogTools.InputDialog(BudgetStatus.this, cmdChangeManually, R.string.menuChangeTotalAmount, inputField, R.drawable.ic_menu_edit);
					inputDialog.show();
					inputDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(inputField.getText().toString().trim().length() != 0);
				}
			};
			AlertDialog realAmountDialog = DialogTools.confirmDialog(BudgetStatus.this, realAmountYesCommand, 
					realAmountNoCommand, R.string.menuChangeTotalAmount, 
					getResources().getString(R.string.msgBudgetRealValue) + " " + Tools.formatDecimal(realAmount), 
					new String[] {getResources().getString(R.string.applyThis), getResources().getString(R.string.change)});
			realAmountDialog.show();
		}
		else if (item.getItemId() == menuResetBudget) {
			Command resetCommand = new Command() {				
				@Override
				public void execute() {
					BudgetSrv.resetBudget(BudgetStatus.this, selectedBudgetID, selectedMonth);
					refreshValues();
				}
			};
			AlertDialog resetDialog = DialogTools.confirmDialog(BudgetStatus.this, resetCommand, R.string.msgConfirm, 
					R.string.msgResetBudget);
			resetDialog.show();
		}
		else if (item.getItemId() == menuValues) {
			Intent intent = new Intent(BudgetStatus.this, BudgetBalances.class);
			Bundle bundle = new Bundle();
			bundle.putLong(BudgetBalances.budgetIDCol, getSelectedBudgetID());
			intent.putExtras(bundle);
			startActivityForResult(intent, Constants.RequestNONE);
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void myClickHandler(View target) {
		switch (target.getId()) {
		case R.id.bt_group_edit:
			openContextMenu(target);
			break;
		case R.id.bt_child_edit:
			openContextMenu(target);
			break;
		case R.id.repImgDateLeft:
			selectedMonth = ReportSrv.addPeriod(false, Constants.ReportTimeInterval.Monthly.index(), selectedMonth);
			refreshDateButtonText(true);
			break;
		case R.id.repImgDateRight:
			if (Tools.compareDates(ReportSrv.addPeriod(true, Constants.ReportTimeInterval.Monthly.index(), selectedMonth), Tools.AddMonth(Tools.getCurrentDate(), 1)) <= 0) {
				selectedMonth = ReportSrv.addPeriod(true, Constants.ReportTimeInterval.Monthly.index(), selectedMonth);
				refreshDateButtonText(true);
			}
			break;
		case R.id.repBtDate:
			showDialog(dateIntervalMonthlyDialogID);
			break;
		default:
			break;
		}
	}

	protected Dialog onCreateDialog(int id) {
		Command cmd = new Command() {				
			@Override
			public void execute() {
				selectedMonth = new Date(spinnerYear.getSelectedItemPosition() + minYear - 1900, 
					spinnerMonth.getSelectedItemPosition(), 1);
				refreshDateButtonText(true);
			}
		};
		LayoutInflater li = LayoutInflater.from(this);
		View view = li.inflate(R.layout.yearmonthdialog, null);

		spinnerYear = (Spinner) view.findViewById(R.id.dmSpYear);
		spinnerMonth = (Spinner) view.findViewById(R.id.dmSpMonth);

		ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, R.layout.simple_spinner_item, years);
		spinnerArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
		spinnerYear.setAdapter(spinnerArrayAdapter);
			
		spinnerYear.setSelection(selectedMonth.getYear() - minYear + 1900);
		spinnerMonth.setSelection(selectedMonth.getMonth());

		AlertDialog viewDialog = DialogTools.CustomDialog(this, cmd, R.string.msgSelectPeriod, view, R.drawable.ic_menu_edit);
		viewDialog.show();
		return null;
	}

	void refreshDateButtonText(boolean refreshLists) {
		((Button)findViewById(R.id.repBtDate)).setText(ReportSrv.getDateButtonText(this, Constants.ReportTimeInterval.Monthly.index(), selectedMonth, selectedMonth, false));
		selectedBudgetID = BudgetSrv.getBudgetId(BudgetStatus.this, selectedMonth);
		refreshValues();
	}	
	
	public static Date getSelectedMonth() {
		return selectedMonth;
	}	
	
	public static void setSelectedMonth(Date selectedMonth) {
		BudgetStatus.selectedMonth = selectedMonth;
	}

	public static long getSelectedBudgetID() {
		return selectedBudgetID;
	}

	public void refreshValues() {
		Double income = 0d;
		Double totalBudgeted = 0d;
		Double totalRemaining = 0d;
		Double totalOverspent = 0d;
		//Double totalOldOverspent = 0d;
		String currencySign = CurrencySrv.getDefaultCurrencySign(this);
		
		//if budget row for selected period exists, we'll get these values
		Cursor cursor = getContentResolver().query(BudgetTableMetaData.CONTENT_URI, 
				new String[] {BudgetTableMetaData._ID, BudgetTableMetaData.INCOME,
				BudgetTableMetaData.CURRENCY_ID}, 
				BudgetTableMetaData.FROM_DATE + " = '" + Tools.DateToDBString(getSelectedMonth()) + "'", null, null);
		if (cursor.moveToFirst()) {
			currencySign = CurrencySrv.getCurrencySignByID(this,
					DBTools.getCursorColumnValueInt(cursor, BudgetTableMetaData.CURRENCY_ID));
			
			income = DBTools.getCursorColumnValueDouble(cursor, BudgetTableMetaData.INCOME);
			
			Cursor cursorOverspent = getContentResolver().query(BudgetCategoriesTableMetaData.CONTENT_URI, 
					null, 
					BudgetCategoriesTableMetaData.BUDGET_ID + " = " + getSelectedBudgetID(), 
					null, null);
			for (cursorOverspent.moveToFirst(); !cursorOverspent.isAfterLast(); cursorOverspent.moveToNext()) {
				Double oldRemaining = DBTools.getCursorColumnValueDouble(cursorOverspent, BudgetCategoriesTableMetaData.REMAINING);
				Double budget = DBTools.getCursorColumnValueDouble(cursorOverspent, BudgetCategoriesTableMetaData.BUDGET);
				Double used = DBTools.getCursorColumnValueDouble(cursorOverspent, BudgetCategoriesTableMetaData.USED_AMOUNT);
				
				totalBudgeted += budget + (oldRemaining);
				totalRemaining += budget + Tools.negativeToZero(oldRemaining) - used;
				totalOverspent += Tools.positiveToZero(budget + oldRemaining - used);
				//income -= Tools.positiveToZero(oldRemaining);
			}
		}
		
		TextView textView = (TextView)findViewById(R.id.bd_value_income);
		textView.setText(Tools.formatDecimal(income) + currencySign);
		
		textView = (TextView)findViewById(R.id.bd_value_budgeted);
		textView.setText(Tools.formatDecimal(totalBudgeted) + currencySign);
		
		textView = (TextView)findViewById(R.id.bd_value_notbudgeted);
		textView.setText(Tools.formatDecimal(income - totalBudgeted) + currencySign);
		
		textView = (TextView)findViewById(R.id.bd_value_remaining);
		textView.setText(Tools.formatDecimal(totalRemaining) + currencySign);
		
		textView = (TextView)findViewById(R.id.bd_value_overspent);
		textView.setText(Tools.formatDecimal(totalOverspent) + currencySign);
		
		StringBuilder stNotBudgeted = new StringBuilder();
		StringBuilder stRemaining = new StringBuilder();
		StringBuilder stOverspent = new StringBuilder();
		StringBuilder stBudgeted = new StringBuilder();
		StringBuilder stIncome = new StringBuilder();
		String remainingColumnName = "remainingColumnName";
		BudgetSrv.getBudgetValues(this, Tools.AddMonth(getSelectedMonth(), -1), 
				Tools.AddDays(getSelectedMonth(), -1), stNotBudgeted, stRemaining, stOverspent, 
				stBudgeted, stIncome, remainingColumnName);
			
		textView = (TextView)findViewById(R.id.bd_value_prev_not_budgeted);
		textView.setText(stNotBudgeted.toString());

		textView = (TextView)findViewById(R.id.bd_value_prev_remaining);
		textView.setText(stRemaining.toString());
			
		textView = (TextView)findViewById(R.id.bd_value_prev_overspent);
		textView.setText(stOverspent.toString());
		
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			try {				
				textView = (TextView)findViewById(R.id.bd_value_prev_budgeted);
				textView.setText(stBudgeted.toString());

				textView = (TextView)findViewById(R.id.bd_value_prev_income);
				textView.setText(stIncome.toString());
			}
			catch (Exception e) {				
			}
		}
		
		RelativeLayout layout = (RelativeLayout) findViewById(R.id.bd_Lay_Overspent);
		if (totalOverspent.compareTo(0d) < 0) 
			layout.setBackgroundDrawable(getResources().getDrawable(R.drawable.cool_budget_background_red));
		else
			layout.setBackgroundDrawable(getResources().getDrawable(R.drawable.cool_budget_background));
					
		layout = (RelativeLayout) findViewById(R.id.bd_Lay_Budgeted);
		if (totalBudgeted.compareTo(0d) == 0) 
			layout.setBackgroundDrawable(getResources().getDrawable(R.drawable.cool_budget_background_red));			
		else 
			layout.setBackgroundDrawable(getResources().getDrawable(R.drawable.cool_budget_background));			
					
		layout = (RelativeLayout) findViewById(R.id.bd_Lay_Remaining);
		if (totalRemaining.compareTo(0d) > 0)
			layout.setBackgroundDrawable(getResources().getDrawable(R.drawable.cool_budget_background_green));						
		else 
			layout.setBackgroundDrawable(getResources().getDrawable(R.drawable.cool_budget_background));						
	}
	
}
