package com.jgmoneymanager.tools;

import android.content.Context;
import android.os.AsyncTask;

import com.jgmoneymanager.entity.Transaction;
import com.jgmoneymanager.mmlibrary.R;
import com.jgmoneymanager.services.BudgetSrv;

public class AfterTransactionOperationsTask extends AsyncTask<String, Void, Boolean> {
	private final Context ctx;
    private final Transaction oldTransactionEntity, newTransactionEntity;
    private final int operationType;
	
	/**
	 * Async task for after transaction operations
	 * @param context
	 * @param oldTransactionEntity
	 * @param newTransactionEntity
	 * @param operationType {@link Constants.DBOperationType}
	 */
	public AfterTransactionOperationsTask(Context context, Transaction oldTransactionEntity, 
			Transaction newTransactionEntity, int operationType) {
		ctx = context;
		this.oldTransactionEntity = oldTransactionEntity;
		this.newTransactionEntity = newTransactionEntity;
		this.operationType = operationType;
	}

    // can use UI thread here
    protected void onPreExecute() {
    }

    // automatically done on worker thread (separate from UI thread)
    protected Boolean doInBackground(final String... args) {
    	if (Tools.getPreferenceBool(ctx, R.string.enablebudgetkey, true))
	    	if (newTransactionEntity.getTransfer_id() == 0) {
		    	if (newTransactionEntity.getTrans_type() == Constants.TransactionTypeExpence) {
			    	if (operationType == Constants.DBOperationType.Delete.index()) {
			    		BudgetSrv.updateBudgetUsedAmount(ctx, 
			    				newTransactionEntity.getCategory_id(), newTransactionEntity.getTrans_date(), 
			    				-newTransactionEntity.getAmount(), newTransactionEntity.getCurrency_id());
			    		BudgetSrv.updateBudgetIncome(ctx, Tools.AddMonth(newTransactionEntity.getTrans_date(), 1), 
			    				newTransactionEntity.getAmount(), newTransactionEntity.getCurrency_id());
			    	}
			    	else if (operationType == Constants.DBOperationType.Insert.index()) {
			    		BudgetSrv.updateBudgetUsedAmount(ctx, 
			    				newTransactionEntity.getCategory_id(), newTransactionEntity.getTrans_date(), 
			    				newTransactionEntity.getAmount(), newTransactionEntity.getCurrency_id());
			    		BudgetSrv.updateBudgetIncome(ctx, Tools.AddMonth(newTransactionEntity.getTrans_date(), 1), 
			    				-newTransactionEntity.getAmount(), newTransactionEntity.getCurrency_id());
			    	}
			    	else if (operationType == Constants.DBOperationType.Update.index()) {
			    		BudgetSrv.updateBudgetUsedAmount(ctx, 
		    				oldTransactionEntity.getCategory_id(), oldTransactionEntity.getTrans_date(),  
			    				-oldTransactionEntity.getAmount(), oldTransactionEntity.getCurrency_id());
			    		BudgetSrv.updateBudgetUsedAmount(ctx, 
			    				newTransactionEntity.getCategory_id(), newTransactionEntity.getTrans_date(), 
			    				newTransactionEntity.getAmount(), newTransactionEntity.getCurrency_id());
			    		BudgetSrv.updateBudgetIncome(ctx, Tools.AddMonth(oldTransactionEntity.getTrans_date(), 1), 
			    				oldTransactionEntity.getAmount(), oldTransactionEntity.getCurrency_id());
			    		BudgetSrv.updateBudgetIncome(ctx, Tools.AddMonth(newTransactionEntity.getTrans_date(), 1), 
			    				-newTransactionEntity.getAmount(), newTransactionEntity.getCurrency_id());
			    	}
		    	}
		    	else if(newTransactionEntity.getTrans_type() == Constants.TransactionTypeIncome) {
			    	if (operationType == Constants.DBOperationType.Delete.index()) {
			    		BudgetSrv.updateBudgetIncome(ctx, newTransactionEntity.getTrans_date(), 
			    				-newTransactionEntity.getAmount(), newTransactionEntity.getCurrency_id());
			    	}
			    	else if (operationType == Constants.DBOperationType.Insert.index()) {
			    		BudgetSrv.updateBudgetIncome(ctx, newTransactionEntity.getTrans_date(), 
			    				newTransactionEntity.getAmount(), newTransactionEntity.getCurrency_id());
			    	}
			    	else if (operationType == Constants.DBOperationType.Update.index()) {
			    		BudgetSrv.updateBudgetIncome(ctx, oldTransactionEntity.getTrans_date(),  
			    				-oldTransactionEntity.getAmount(), oldTransactionEntity.getCurrency_id());
			    		BudgetSrv.updateBudgetIncome(ctx, newTransactionEntity.getTrans_date(), 
			    				newTransactionEntity.getAmount(), newTransactionEntity.getCurrency_id());
			    	}
		    	}
	    	}
    	return true;
    }

    // can use UI thread here
    protected void onPostExecute(final Boolean success) {
    	//if (success)
    		
    }

 }