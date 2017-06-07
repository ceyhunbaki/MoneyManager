package com.jgmoneymanager.budget;

import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.main.R;
import com.jgmoneymanager.services.BudgetSrv;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import java.util.Date;

/**
 * Created by Ceyhun on 02.01.2016.
 */
public class BudgetRealAmountTask extends AsyncTask<String, Void, Boolean> {
    Context context;
    Double realBudget = 0d;
    Double currentBudget = 0d;
    Date selectedMonth;

    public BudgetRealAmountTask(Context context, Double currentBudget, Date selectedMonth) {
        this.context = context;
        this.currentBudget = currentBudget;
        this.selectedMonth = selectedMonth;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(String... params) {
        realBudget = Tools.round(BudgetSrv.getTotalAmountForBudget(context, selectedMonth, Tools.lastDay(context, selectedMonth)));
        return (realBudget.compareTo(currentBudget) != 0);
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (success) {
            Command realBudgetYesCommand = new Command() {
                @Override
                public void execute() {
                    BudgetSrv.changeBudgetTotalAmount(context, Tools.truncDate(context, Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncMonth), realBudget);
                    BudgetStatus fragment = (BudgetStatus) BudgetMain.mAdapter.getItem(0);
                    fragment.refreshValues();
                }
            };
            Command realBudgetNoCommand = new Command() {
                @Override
                public void execute() {
                    Tools.setPreference(context, R.string.showBudgetRealValueKey, false);
                }
            };
            Command realBudgetCancelCommand = new Command() {
                public void execute() {

                }
            };
            AlertDialog realBudgetDialog = DialogTools.confirmWithCancelDialog(context, realBudgetYesCommand,
                    realBudgetNoCommand, realBudgetCancelCommand, R.string.menuChangeTotalAmount,
                    context.getResources().getString(R.string.msgBudgetRealValue) + " " + Tools.formatDecimalInUserFormat(realBudget),
                    new String[]{context.getResources().getString(R.string.applyThis),
                            context.getResources().getString(R.string.neverAsk), context.getResources().getString(R.string.Cancel)});
            try {
                realBudgetDialog.show();
            }
            catch (Exception e) {

            }
        }
    }
}
