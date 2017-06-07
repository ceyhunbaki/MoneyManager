package com.jgmoneymanager.budget;

import android.content.Context;
import android.os.AsyncTask;

import com.jgmoneymanager.services.BudgetSrv;

/**
 * Created by Ceyhun on 07.04.2017.
 */
public class BudgetUpdateMonthFirstDateTask extends AsyncTask<String, Void, Boolean> {
    Context mContext;
    String mNewDate;

    public BudgetUpdateMonthFirstDateTask(Context context, String newDate) {
        this.mContext = context;
        this.mNewDate = newDate;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(String... params) {
        BudgetSrv.updateMonthStartDate(mContext, mNewDate);
        return true;
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
