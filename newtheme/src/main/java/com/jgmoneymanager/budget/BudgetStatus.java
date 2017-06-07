package com.jgmoneymanager.budget;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.BudgetCategoriesTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.BudgetTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.main.R;
import com.jgmoneymanager.services.BudgetSrv;
import com.jgmoneymanager.services.CategorySrv;
import com.jgmoneymanager.services.CurrencySrv;
import com.jgmoneymanager.tools.Tools;

public class BudgetStatus extends Fragment {

	//Date selectedMonth;
	View rootView;
	Context context;

	static String overSpentHint = "";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		rootView = inflater.inflate(R.layout.budget_status, container, false);
		context = rootView.getContext();

		refreshValues();

		return rootView;
	}

	@Override
	public void setMenuVisibility(boolean menuVisible) {
		super.setMenuVisibility(menuVisible);
		if (menuVisible && BudgetCategories.hasDataChanged())
			refreshValues();
	}

	public void refreshValues() {
		overSpentHint = "";
		Double income = 0d;
		Double totalBudgeted = 0d;
		Double totalRemaining = 0d;
		Double totalOverspent = 0d;
		//Double totalOldOverspent = 0d;
		String currencySign = CurrencySrv.getDefaultCurrencySign(context);

		//if budget row for selected period exists, we'll get these values
		Cursor cursor = context.getContentResolver().query(BudgetTableMetaData.CONTENT_URI,
				new String[] {BudgetTableMetaData._ID, BudgetTableMetaData.INCOME,
				BudgetTableMetaData.CURRENCY_ID},
				BudgetTableMetaData.FROM_DATE + " = '" + Tools.DateToDBString(((BudgetMain)getActivity()).getSelectedMonth()) + "'", null, null);
		if (cursor.moveToFirst()) {
			currencySign = CurrencySrv.getCurrencySignByID(context,
					DBTools.getCursorColumnValueInt(cursor, BudgetTableMetaData.CURRENCY_ID));

			income = DBTools.getCursorColumnValueDouble(cursor, BudgetTableMetaData.INCOME);

			Cursor cursorOverspent = context.getContentResolver().query(BudgetCategoriesTableMetaData.CONTENT_URI,
					null,
					BudgetCategoriesTableMetaData.BUDGET_ID + " = " + BudgetMain.getSelectedBudgetID(),
					null, null);
			for (cursorOverspent.moveToFirst(); !cursorOverspent.isAfterLast(); cursorOverspent.moveToNext()) {
				Double oldRemaining = DBTools.getCursorColumnValueDouble(cursorOverspent, BudgetCategoriesTableMetaData.REMAINING);
				Double budget = DBTools.getCursorColumnValueDouble(cursorOverspent, BudgetCategoriesTableMetaData.BUDGET);
				Double used = DBTools.getCursorColumnValueDouble(cursorOverspent, BudgetCategoriesTableMetaData.USED_AMOUNT);

				totalBudgeted += budget + (oldRemaining);
				totalRemaining += Tools.negativeToZero(budget + /*Tools.negativeToZero*/(oldRemaining) - used);

				Double currenttOverspent = Tools.positiveToZero(budget + oldRemaining - used);
				if (currenttOverspent.compareTo(0d) < 0) {
					totalOverspent += Tools.positiveToZero(budget + oldRemaining - used);
					overSpentHint += CategorySrv.getCategoryNameByID(context, DBTools.getCursorColumnValueLong(cursorOverspent, BudgetCategoriesTableMetaData.CATEGORY_ID))
							+ ": " + Tools.formatDecimalInUserFormat(-currenttOverspent) + "\n";
				}
				//income -= Tools.positiveToZero(oldRemaining);
			}
		}

		TextView textView = (TextView)rootView.findViewById(R.id.bd_value_income);
		textView.setText(Tools.getFullAmountText(income, currencySign, true));

		textView = (TextView)rootView.findViewById(R.id.bd_value_budgeted);
		textView.setText(Tools.getFullAmountText(totalBudgeted, currencySign, true));

		textView = (TextView)rootView.findViewById(R.id.bd_value_notbudgeted);
		textView.setText(Tools.getFullAmountText(income - totalBudgeted, currencySign, true));

		textView = (TextView)rootView.findViewById(R.id.bd_value_remaining);
		textView.setText(Tools.getFullAmountText(totalRemaining, currencySign, true));

		textView = (TextView)rootView.findViewById(R.id.bd_value_overspent);
		textView.setText(Tools.getFullAmountText(totalOverspent, currencySign, true));
		textView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (overSpentHint.length() > 0) {
					AlertDialog dialog = DialogTools.informationDialog(context, R.string.overspent, overSpentHint);
					dialog.show();
				}
			}
		});

		StringBuilder stNotBudgeted = new StringBuilder();
		StringBuilder stRemaining = new StringBuilder();
		StringBuilder stOverspent = new StringBuilder();
		StringBuilder stBudgeted = new StringBuilder();
		StringBuilder stIncome = new StringBuilder();
		String remainingColumnName = "remainingColumnName";
		BudgetSrv.getBudgetValues(context, Tools.AddMonth(((BudgetMain)getActivity()).getSelectedMonth(), -1),
				Tools.AddDays(((BudgetMain)getActivity()).getSelectedMonth(), -1), stNotBudgeted, stRemaining, stOverspent,
				stBudgeted, stIncome, remainingColumnName);

		textView = (TextView)rootView.findViewById(R.id.bd_value_prev_not_budgeted);
		textView.setText(stNotBudgeted.toString());

		textView = (TextView)rootView.findViewById(R.id.bd_value_prev_remaining);
		textView.setText(stRemaining.toString());

		textView = (TextView)rootView.findViewById(R.id.bd_value_prev_overspent);
		textView.setText(stOverspent.toString());

		if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			try {
				textView = (TextView)rootView.findViewById(R.id.bd_value_prev_budgeted);
				textView.setText(stBudgeted.toString());

				textView = (TextView)rootView.findViewById(R.id.bd_value_prev_income);
				textView.setText(stIncome.toString());
			}
			catch (Exception e) {
			}
		}

		RelativeLayout layout = (RelativeLayout) rootView.findViewById(R.id.bd_Lay_Overspent);
		if (totalOverspent.compareTo(0d) < 0)
			layout.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.cool_budget_background_red));
		else
			layout.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.cool_budget_background));

		layout = (RelativeLayout) rootView.findViewById(R.id.bd_Lay_Budgeted);
		if (totalBudgeted.compareTo(0d) == 0)
			layout.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.cool_budget_background_red));
		else
			layout.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.cool_budget_background));

		layout = (RelativeLayout) rootView.findViewById(R.id.bd_Lay_Remaining);
		if (totalRemaining.compareTo(0d) > 0)
			layout.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.cool_budget_background_green));
		else
			layout.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.cool_budget_background));
	}

}
