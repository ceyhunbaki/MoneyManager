package com.jgmoneymanager.budget;

import android.content.Context;
import android.os.AsyncTask;

import com.jgmoneymanager.services.BudgetSrv;

/**
 * Created by Ceyhun on 01.11.2016.
 */
public class BudgetNewMonthTask extends AsyncTask<String, Void, Boolean> {
    Context mContext;

    public BudgetNewMonthTask(Context context) {
        this.mContext = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(String... params) {
        return BudgetSrv.controlNewMonth(mContext);
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (success) {
            try {
                BudgetStatus budgetStatus = new BudgetStatus();
                budgetStatus.refreshValues();
                BudgetCategories budgetCategories = new BudgetCategories();
                budgetCategories.refreshList();
            }
            catch (Exception e) {

            }
        }
    }
}
