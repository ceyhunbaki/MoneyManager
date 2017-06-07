package com.jgmoneymanager.tools;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.TextView;

import com.jgmoneymanager.services.TransactionSrv;

public class RefreshAccountDetailsTask extends AsyncTask<String, Void, Boolean> {
    private final Context ctx;
    private final long selectedAccountID;
    private double dayIncome, dayExpense, monthIncome, monthExpense, yearIncome, yearExpense;
    private final TextView tvDayIncome, tvDayExpense, tvMonthIncome, tvMonthExpense, tvYearIncome, tvYearExpense;
	
	public RefreshAccountDetailsTask(Context context, long selectedAccountID,
			TextView tvDayIncome, TextView tvDayExpense, TextView tvMonthIncome, TextView tvMonthExpense,
			TextView tvYearIncome, TextView tvYearExpense) {
		ctx = context;
		this.selectedAccountID = selectedAccountID;
		this.tvDayIncome = tvDayIncome;
		this.tvDayExpense = tvDayExpense;
		this.tvMonthIncome = tvMonthIncome;
		this.tvMonthExpense = tvMonthExpense;
		this.tvYearIncome = tvYearIncome;
		this.tvYearExpense = tvYearExpense;
	}

    // can use UI thread here
    protected void onPreExecute() {
    	tvDayIncome.setText("...");
		tvDayExpense.setText("...");
		tvMonthIncome.setText("...");
		tvMonthExpense.setText("...");
		tvYearIncome.setText("...");
		tvYearExpense.setText("...");
    }

    // automatically done on worker thread (separate from UI thread)
    protected Boolean doInBackground(final String... args) {
    	dayIncome = 0d; 
    	dayExpense = 0d; 
    	monthIncome = 0d; 
    	monthExpense = 0d; 
    	yearIncome = 0d; 
    	yearExpense = 0d;
    	try {
    		dayIncome = TransactionSrv.getTransactionsSum(ctx, selectedAccountID, 
    				Constants.DateFilterValues.Today.index(), Constants.TransactionTypeIncome);
    		dayExpense = TransactionSrv.getTransactionsSum(ctx, selectedAccountID, 
    				Constants.DateFilterValues.Today.index(), Constants.TransactionTypeExpence);
    		monthIncome = TransactionSrv.getTransactionsSum(ctx, selectedAccountID, 
    				Constants.DateFilterValues.ThisMonth.index(), Constants.TransactionTypeIncome);
    		monthExpense = TransactionSrv.getTransactionsSum(ctx, selectedAccountID, 
    				Constants.DateFilterValues.ThisMonth.index(), Constants.TransactionTypeExpence);
    		yearIncome = TransactionSrv.getTransactionsSum(ctx, selectedAccountID, 
    				Constants.DateFilterValues.ThisYear.index(), Constants.TransactionTypeIncome);
    		yearExpense = TransactionSrv.getTransactionsSum(ctx, selectedAccountID, 
    				Constants.DateFilterValues.ThisYear.index(), Constants.TransactionTypeExpence);
    		return true;
    	}
    	catch (Exception e) {
    		return false;
    	}
    }

    // can use UI thread here
    protected void onPostExecute(final Boolean success) {
       if (success) {
    	   try {
    		   tvDayIncome.setText(Tools.formatDecimalInUserFormat(dayIncome,0));
    		   tvDayExpense.setText(Tools.formatDecimalInUserFormat(dayExpense,0));
    		   tvMonthIncome.setText(Tools.formatDecimalInUserFormat(monthIncome,0));
    		   tvMonthExpense.setText(Tools.formatDecimalInUserFormat(monthExpense,0));
    		   tvYearIncome.setText(Tools.formatDecimalInUserFormat(yearIncome,0));
    		   tvYearExpense.setText(Tools.formatDecimalInUserFormat(yearExpense,0));
    	   }
    	   catch (Exception e) {
    		   
    	   }
       }
    }

 }