package com.jgmoneymanager.main;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.jgmoneymanager.budget.BudgetGoalsList;
import com.jgmoneymanager.budget.BudgetMain;
import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VTransAccountViewMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransactionsTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CurrencyTableMetaData;
import com.jgmoneymanager.dialogs.CheckBoxDialog;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.CheckBoxItem;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.reports.ReportList;
import com.jgmoneymanager.services.AccountSrv;
import com.jgmoneymanager.services.CurrencySrv;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.GetNecessaryCurrRatesAndControlBudgetTask;
import com.jgmoneymanager.tools.LocalTools;
import com.jgmoneymanager.tools.RefreshAccountDetailsTask;
import com.jgmoneymanager.tools.Tools;
import com.jgmoneymanager.tools.TranslateDBTask;

import java.util.ArrayList;
import java.util.Date;

public class MainScreen extends MyActivity
        implements NavigationView.OnNavigationItemSelectedListener{
    //View app;
    
    final int buttonFirstId = 1000;
	ArrayList<CheckBoxItem> accountsList;
	Button[] buttons;
	LayoutParams accountButtonParams;
    
    int btAccountMenuID = Menu.FIRST;
    int btTransactionMenuID = btAccountMenuID + 2;
    int btTransferMenuID = btAccountMenuID + 3;
    int btRPTransactionMenuID = btAccountMenuID + 4;
    int btSavingsMenuID = btAccountMenuID + 5;
    int btDebtsMenuID = btAccountMenuID + 6;
    int btCurrencyMenuID = btAccountMenuID + 7;
    int btExCategoryMenuID = btAccountMenuID + 8;
    int btIncCategoryMenuID = btAccountMenuID + 9;
    int btStatusMenuID = btAccountMenuID + 10;
    int btPaymentMethodMenuID = btAccountMenuID + 11;
    int btSettingMenuID = btAccountMenuID + 12;
    int btHelpMenuID = btAccountMenuID + 13;
    int btRemoveAdsMenuID = btAccountMenuID + 14;
    int btAboutMenuID = btAccountMenuID + 15;

	public long selectedAccountID = 0;
    boolean isOldStyle = false;

    Intent intent;
	    
    @Override
	protected void onCreate(Bundle savedInstanceState) {

        Tools.loadSettings(this);
        Tools.loadLanguage(this, null);

        super.onCreate(savedInstanceState);
        
        if (CurrencySrv.getDefaultCurrencyID(this) == 0) {
        	/*final Cursor cursor = getContentResolver().query(CurrencyTableMetaData.CONTENT_URI, null, null, null, CurrencyTableMetaData.NAME);
            Command cmd = new Command() {
                @Override
                public void execute() {
                    cursor.moveToPosition(Constants.cursorPosition);
                    CurrencySrv.changeDefaultCurrency(MainScreen.this, DBTools.getCursorColumnValueLong(cursor, CurrencyTableMetaData._ID), null);
                    if (AccountSrv.getAccountCount(MainScreen.this) == 0) {
                        long accoundID = AccountSrv.insertAccount(MainScreen.this, getBaseContext().getString(R.string.cash), "0", "1");
                        AccountSrv.insertAccount(MainScreen.this, getBaseContext().getString(R.string.bank), "0", "0");
                        selectedAccountID = accoundID;
                        restartActivity();
                    }
                }
            };
            AlertDialog dialog = DialogTools.RadioListDialog(MainScreen.this, cmd, R.string.msgSetDefaultCurrency, cursor, CurrencyTableMetaData.NAME, false, true);
        	dialog.show();*/
        	String query = "select " + CurrencyTableMetaData._ID + ", " + CurrencyTableMetaData.NAME + " from "
                    + CurrencyTableMetaData.TABLE_NAME;
            String queryPart2 = " order by " + CurrencyTableMetaData.DEFAULT_SORT_ORDER;

            intent = new Intent(MainScreen.this, CheckBoxDialog.class);
            Bundle bundle = new Bundle();
            bundle.putBoolean(Constants.dontRefreshValues, false);
            bundle.putString(Constants.query, query);
            bundle.putString(Constants.queryPart2, queryPart2);
            bundle.putString(Constants.paramTitle, CurrencyTableMetaData.NAME);
            bundle.putString(Constants.paramWindowTitle, getString(R.string.msgSetDefaultCurrency));
            bundle.putString(Constants.paramFilterType, CurrencyTableMetaData.TABLE_NAME);
            intent.putExtras(bundle);

            intent.setAction(Intent.ACTION_SEARCH);

            Tools.setPreference(this, R.string.oldversionkey, Tools.getVersionCode(this));

            startActivityForResult(intent, Constants.RequestCurrencyForTransaction);
        }
        else {
            GetNecessaryCurrRatesAndControlBudgetTask getRatesTask = new GetNecessaryCurrRatesAndControlBudgetTask(MainScreen.this, myApp);
            getRatesTask.execute("");

            LocalTools.startupActions(MainScreen.this);
            LocalTools.showWhatsNewDialog(this);
        }

        initializeViews();
        
        if ((savedInstanceState != null) && (savedInstanceState.containsKey("selectedAccountID")))
        	selectedAccountID = Tools.getLongFromBundle0(savedInstanceState, "selectedAccountID");
        if (selectedAccountID == 0)
        	selectedAccountID = AccountSrv.getDefultAccountID(MainScreen.this, null);
        setActiveAccountButton(Integer.parseInt(String.valueOf(selectedAccountID)));
        refreshAccountDetails();


    }

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Tools.putToBundle(outState, "selectedAccountID", selectedAccountID);
		super.onSaveInstanceState(outState);
	}

	public void generateAccountsLayout() {
		//kenardan cagiranda silib elave etmedi, olanlarin ardina atdi;
    	LinearLayout layout = (LinearLayout) findViewById(R.id.llATAccounts);
    	layout.removeAllViewsInLayout();
    	
    	accountsList = AccountSrv.generateAccountsList(MainScreen.this, getResources().getString(R.string.totalAccount));
    	buttons = new Button[accountsList.size()];

    	accountButtonParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        int margin = Math.round(getResources().getDimension(R.dimen.main_account_names_margin) / getResources().getDisplayMetrics().density);
        accountButtonParams.setMargins(margin, 0, margin, 0);
    	
    	for (int j = 0; j < accountsList.size(); j++) {
    		CheckBoxItem item = accountsList.get(j);
    		buttons[j] = new Button(this);
    	    buttons[j].setLayoutParams(accountButtonParams);
    	    buttons[j].setText("  " + item.getName() + "  ");
            buttons[j].setTransformationMethod(null);
            buttons[j].setId(buttonFirstId + Integer.parseInt(String.valueOf(item.getID())));
            buttons[j].setTextAppearance(this, R.style.ThemeNew_Main_DeactiveAccount);
            buttons[j].setTextColor(getResources().getColor(R.color.newThemeBlue));
            buttons[j].setBackgroundColor(getResources().getColor(R.color.White));
            //}
    	    buttons[j].setOnClickListener(myClickListener);
    	    layout.addView(buttons[j]);
    	}
    }
    
    public void setActiveAccountButton(long accountID) {
    	//kohneni bozardib, yenini agardaq
    	try{
	    	Button oldButton = (Button)findViewById(buttonFirstId + Integer.parseInt(String.valueOf(selectedAccountID)));
            oldButton.setTextAppearance(this, R.style.ThemeNew_Main_DeactiveAccount);
    	}
    	catch (Exception e) {
    		selectedAccountID = AccountSrv.getDefultAccountID(getBaseContext(), null);
    		accountID = selectedAccountID;
    	}
    	
    	Button activeButton = (Button)findViewById(buttonFirstId + Integer.parseInt(String.valueOf(accountID)));
        if (activeButton != null) {
            activeButton.setTextAppearance(this, R.style.ThemeNew_Main_ActiveAccount);
            activeButton.requestFocus();

            //buttonu ortaya getirek
    	    HorizontalScrollView scroolView = (HorizontalScrollView)findViewById(R.id.hsATAccounts);
    	    scroolView.scrollTo(activeButton.getLeft() - ((scroolView.getWidth() - activeButton.getWidth()) / 2), 0);
        }
    	
    	selectedAccountID = accountID;
    }

    public void refreshAccountDetails() {
        if (isOldStyle)
            refreshAccountDetailsOld();
        else
            refreshAccountDetailsNew();
    }

	public void refreshAccountDetailsNew() {
		//balance deyerinin teyini
        //TextView balanceCurrencyLabel = (TextView) MainScreen.this.findViewById(R.id.tvATBalanceCurrency);
        TextView balanceAmountLabel = (TextView) MainScreen.this.findViewById(R.id.tvATBalanceAmount);

		StringBuffer currencySign = new StringBuffer();
		float balance = AccountSrv.getAccountBalance(MainScreen.this, selectedAccountID, currencySign);
       // balanceCurrencyLabel.setText(CurrencySrv.getCurrencySymbol(currencySign.toString()));
        balanceAmountLabel.setText(Tools.getFullAmountText(balance, currencySign.toString(), true));
	}

    public void refreshAccountDetailsOld() {
        //balance deyerinin teyini
        TextView balanceAmountLabel = (TextView) MainScreen.this.findViewById(R.id.tvATBalanceAmount);
        StringBuffer currencySign = new StringBuffer();
        float balance = AccountSrv.getAccountBalance(MainScreen.this, selectedAccountID, currencySign);
        balanceAmountLabel.setText(Tools.getFullAmountText(balance, currencySign.toString(), true));
        if (Float.compare(balance, 0f) < 0) {
            //eger balance < 0 olarsa her iki label qirmizi renge cevrilir
            balanceAmountLabel.setBackgroundColor(getResources().getColor(R.color.DarkRed));
            TextView balanceCaptionLabel = (TextView) MainScreen.this.findViewById(R.id.tvATBalance);
            balanceCaptionLabel.setBackgroundColor(getResources().getColor(R.color.DarkRed));
        }
        else {
            //eger balance < 0 olarsa her iki label qirmizi renge cevrilir
            balanceAmountLabel.setBackgroundColor(getResources().getColor(R.color.DarkGreen));
            TextView balanceCaptionLabel = (TextView) MainScreen.this.findViewById(R.id.tvATBalance);
            balanceCaptionLabel.setBackgroundColor(getResources().getColor(R.color.DarkGreen));
        }

        // ((TextView)findViewById(R.id.tvATAccName)).setText(AccountSrv.getAccountNameByID(MainScreen.this, selectedAccountID));
        //diger labellerin yenilenmesi
        TextView tvDayIncome = (TextView) MainScreen.this.findViewById(R.id.btATTodayIncome);
        TextView tvDayExpense = (TextView) MainScreen.this.findViewById(R.id.btATTodayExpence);
        TextView tvMonthIncome = (TextView) MainScreen.this.findViewById(R.id.btATMonthIncome);
        TextView tvMonthExpense = (TextView) MainScreen.this.findViewById(R.id.btATMonthExpence);
        TextView tvYearIncome = (TextView) MainScreen.this.findViewById(R.id.btATYearIncome);
        TextView tvYearExpense = (TextView) MainScreen.this.findViewById(R.id.btATYearExpence);

        RefreshAccountDetailsTask refreshDetailsTask = new RefreshAccountDetailsTask(MainScreen.this,
                selectedAccountID, tvDayIncome, tvDayExpense, tvMonthIncome, tvMonthExpense,
                tvYearIncome, tvYearExpense);
        refreshDetailsTask.execute("");
    }
    
    OnClickListener myClickListener = new OnClickListener() {		
		@Override
		public void onClick(View v) {
			int newAccountID = v.getId() - buttonFirstId;
			if (newAccountID != selectedAccountID) {
				setActiveAccountButton(newAccountID);
				refreshAccountDetails();
			}
		}
	};
	
	protected void onResume() {

		super.onResume();	
		LocalTools.onResumeEvents(this);
		setActiveAccountButton(selectedAccountID);
		if (myApp.isMainDetailsChanged()) {
			refreshAccountDetails();
			myApp.setMainDetailsChanged(false);
		}
	}

	public void myClickHandler(View target) {
        Intent intent;
        Bundle bundle;
        switch (target.getId()) {
            case R.id.btATAccounts:
                final Cursor cursor = this.managedQuery(VTransAccountViewMetaData.CONTENT_URI, null,
                        VTransAccountViewMetaData.STATUS + " =1 ", null, null);
                Command cmd = new Command() {
                    @Override
                    public void execute() {
                        Cursor cursorInt = getContentResolver().query(VTransAccountViewMetaData.CONTENT_URI,
                                new String[]{VTransAccountViewMetaData._ID}, VTransAccountViewMetaData.STATUS + " =1 ",
                                null, null);
                        cursorInt.moveToPosition(Constants.cursorPosition);
                        setActiveAccountButton(DBTools.getCursorColumnValueInt(cursorInt, VTransAccountViewMetaData._ID));
                        refreshAccountDetails();
                    }
                };
                AlertDialog accountList = DialogTools.RadioListDialog(this, cmd, R.string.selectAccount, cursor, VTransAccountViewMetaData.NAME, true, true);
                accountList.show();
                break;
            case R.id.btATBudget:
                intent = new Intent(MainScreen.this, BudgetMain.class);
                startActivityForResult(intent, Constants.RequestNONE);
                break;
            case R.id.btATReports:
                intent = new Intent(MainScreen.this, ReportList.class);
                startActivityForResult(intent, Constants.RequestNONE);
                break;
            case R.id.btATCalculator:
                intent = new Intent(MainScreen.this, Calculator.class);
                startActivityForResult(intent, Constants.RequestNONE);
                break;
            case R.id.btATConverter:
                intent = new Intent(MainScreen.this, Convertor.class);
                startActivityForResult(intent, Constants.RequestNONE);
                break;
            case R.id.btATTransaction:
                intent = new Intent(this, TransactionEdit.class);
                intent.setAction(Intent.ACTION_INSERT);
                bundle = new Bundle();
                bundle.putString(Constants.UpdateMode, Constants.Insert);
                Constants.TransactionType = Constants.TransactionTypeExpence;
                bundle.putInt(TransactionsTableMetaData.TRANSTYPE, Constants.TransactionType);
                if (selectedAccountID != 0)
                    bundle.putLong(Constants.paramAccountID, selectedAccountID);
                intent.putExtras(bundle);
                startActivityForResult(intent, Constants.RequestTransactionInsert);
                break;
            case R.id.btATTransfer:
                intent = new Intent(getBaseContext(), TransferEdit.class);
                intent.setAction(Intent.ACTION_INSERT);
                if (selectedAccountID != 0) {
                    bundle = new Bundle();
                    bundle.putLong(Constants.paramAccountID, selectedAccountID);
                    intent.putExtras(bundle);
                }
                startActivityForResult(intent, Constants.RequestTransferInsert);
                break;
            case R.id.layATBalance:
                callTransactionList(Constants.DateFilterValues.ThisMonth.index(), Constants.TransFTransaction.All.index());
                break;
            case R.id.btATIncome:
                intent = new Intent(this, TransactionEdit.class);
                intent.setAction(Intent.ACTION_INSERT);
                bundle = new Bundle();
                bundle.putString(Constants.UpdateMode, Constants.Insert);
                Constants.TransactionType = Constants.TransactionTypeIncome;
                bundle.putInt(TransactionsTableMetaData.TRANSTYPE, Constants.TransactionType);
                if (selectedAccountID != 0)
                    bundle.putLong(Constants.paramAccountID, selectedAccountID);
                intent.putExtras(bundle);
                startActivityForResult(intent, Constants.RequestTransactionInsert);
                break;
            case R.id.btATExpence:
                intent = new Intent(this, TransactionEdit.class);
                intent.setAction(Intent.ACTION_INSERT);
                bundle = new Bundle();
                bundle.putString(Constants.UpdateMode, Constants.Insert);
                Constants.TransactionType = Constants.TransactionTypeExpence;
                bundle.putInt(TransactionsTableMetaData.TRANSTYPE, Constants.TransactionType);
                if (selectedAccountID != 0)
                    bundle.putLong(Constants.paramAccountID, selectedAccountID);
                intent.putExtras(bundle);
                startActivityForResult(intent, Constants.RequestTransactionInsert);
                break;
            case R.id.btATToday:
                callTransactionList(Constants.DateFilterValues.Today.index(), Constants.TransFTransaction.All.index());
                break;
            case R.id.btATTodayIncome:
                callTransactionList(Constants.DateFilterValues.Today.index(), Constants.TransFTransaction.Income.index());
                break;
            case R.id.btATTodayExpence:
                callTransactionList(Constants.DateFilterValues.Today.index(), Constants.TransFTransaction.Expence.index());
                break;
            case R.id.btATMonth:
                callTransactionList(Constants.DateFilterValues.ThisMonth.index(), Constants.TransFTransaction.All.index());
                break;
            case R.id.btATMonthIncome:
                callTransactionList(Constants.DateFilterValues.ThisMonth.index(), Constants.TransFTransaction.Income.index());
                break;
            case R.id.btATMonthExpence:
                callTransactionList(Constants.DateFilterValues.ThisMonth.index(), Constants.TransFTransaction.Expence.index());
                break;
            case R.id.btATYear:
                callTransactionList(Constants.DateFilterValues.ThisYear.index(), Constants.TransFTransaction.All.index());
                break;
            case R.id.btATYearIncome:
                callTransactionList(Constants.DateFilterValues.ThisYear.index(), Constants.TransFTransaction.Income.index());
                break;
            case R.id.btATYearExpence:
                callTransactionList(Constants.DateFilterValues.ThisYear.index(), Constants.TransFTransaction.Expence.index());
                break;
            case R.id.btATRecurring:
                intent = new Intent(MainScreen.this, RPTransactionEdit.class);
                intent.setAction(Intent.ACTION_INSERT);
                bundle = new Bundle();
                if (selectedAccountID != 0)
                    bundle.putLong(Constants.paramAccountID, selectedAccountID);
                intent.putExtras(bundle);
                startActivityForResult(intent, Constants.RequestNewRPTransactionForTransfer);
                break;
        }

    }
		
	void callTransactionList(int periodType, int transactiontype) {
		Intent intent = new Intent(MainScreen.this, TransactionList.class);
		intent.setAction(Constants.ActionViewTransactionsFromReport);
		String title;
		Bundle bundle = new Bundle();
		Date fromDate;
		if (periodType == Constants.DateFilterValues.ThisMonth.index()) {
			fromDate = Tools.truncDate(this, Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncMonth);
			title = getResources().getString(R.string.thisMonth) + " - ";
		}
		else if (periodType == Constants.DateFilterValues.ThisYear.index()) {
			fromDate = Tools.truncDate(this, Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncYear);
			title = getResources().getString(R.string.thisYear) + " - ";
		}
		else {
			fromDate = Tools.getCurrentDate();
			title = getResources().getString(R.string.today) + " - ";
		}
		bundle.putString(Constants.paramFromDate, Tools.DateToDBString(fromDate));
		bundle.putString(Constants.paramToDate, Tools.DateToDBString(Tools.getCurrentDate()));
		
		bundle.putLong(Constants.paramAccountID, selectedAccountID);		
		bundle.putInt(Constants.reportType, transactiontype);
		switch (transactiontype) {		
		case 0:
			title += getResources().getString(R.string.transactions);
			break;
		case 1:
			title += getResources().getString(R.string.incomes);
			break;
		case 2:
			title += getResources().getString(R.string.expences);
			break;
		default:
			break;
		}
		bundle.putString(Constants.paramTitle, title);
		
		intent.putExtras(bundle);
		startActivityForResult(intent, Constants.RequestTransactionByAccount);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);

		if (SettingsMain.languageChanged || SettingsMain.homeScreenVersionChanged || SettingsMain.loadSettings) {
            if (SettingsMain.languageChanged) {
                TranslateDBTask translateCategoriesTask = new TranslateDBTask(MainScreen.this);
                translateCategoriesTask.execute();
            }

			restartActivity();
			SettingsMain.languageChanged = false;
            SettingsMain.homeScreenVersionChanged = false;
            SettingsMain.loadSettings = false;
		}

		if (requestCode == Constants.RequestPasswordInStartup) { 
			if (resultCode != RESULT_OK)
				finish();
		}
		
		if (resultCode == RESULT_OK) {
			if (requestCode == Constants.RequestAccountSort) {
		        restartActivity();
			}
		}
		if ((requestCode == Constants.RequestAccountSort) && (resultCode == RESULT_CANCELED) && AccountSort.resultOK) 
			restartActivity();

        if ((requestCode == Constants.RequestCurrencyForTransaction)) {
            if (resultCode == RESULT_CANCELED) {
                //finish();
                try {
                    CurrencySrv.changeDefaultCurrency(MainScreen.this, CurrencySrv.getCurrencyIDBySign(MainScreen.this, "USD"), null);
                    defaultCurrencyChangedAction(true);
                }catch (Exception e) {
                    finish();
                }
            }
            else {
                ArrayList<CheckBoxItem> currencyList = CheckBoxDialog.itemsList;
                if ((currencyList != null) && (Tools.getIDsFromCheckBoxList(currencyList).length() != 0)) {
                    try {
                        CurrencySrv.changeDefaultCurrency(MainScreen.this, Tools.getIDFromCheckBoxList(currencyList), null);
                        defaultCurrencyChangedAction(false);
                    }catch (Exception e) {
                        finish();
                    }
                }
                else {
                    finish();
                }
            /*final Cursor cursor = getContentResolver().query(CurrencyTableMetaData.CONTENT_URI, null, null, null, CurrencyTableMetaData.NAME);
            Command cmd = new Command() {
                @Override
                public void execute() {
                    cursor.moveToPosition(Constants.cursorPosition);
                    CurrencySrv.changeDefaultCurrency(MainScreen.this, DBTools.getCursorColumnValueLong(cursor, CurrencyTableMetaData._ID), null);
                    if (AccountSrv.getAccountCount(MainScreen.this) == 0) {
                        long accoundID = AccountSrv.insertAccount(MainScreen.this, getBaseContext().getString(R.string.cash), "0", "1");
                        AccountSrv.insertAccount(MainScreen.this, getBaseContext().getString(R.string.bank), "0", "0");
                        selectedAccountID = accoundID;
                        restartActivity();
                    }
                }
            };*/
            }
        }
	}

	void defaultCurrencyChangedAction(boolean showNotification) {
        if (AccountSrv.getAccountCount(MainScreen.this) == 0) {
            long accoundID = AccountSrv.insertAccount(MainScreen.this, getBaseContext().getString(R.string.cash), "0", "1");
            AccountSrv.insertAccount(MainScreen.this, getBaseContext().getString(R.string.bank), "0", "0");
            selectedAccountID = accoundID;
            if (!showNotification)
                restartActivity();
            else {
                Command cmdRestart = new Command() {
                    @Override
                    public void execute() {
                        restartActivity();
                    }
                };
                AlertDialog dialog = DialogTools.informationDialog(MainScreen.this, R.string.information, R.string.msgUsdDefaultSet, cmdRestart);
                dialog.show();
            }
        }
    }
	
	private void restartActivity() {
	    Intent intent = getIntent();
	    finish();
	    startActivity(intent);
	}

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        Intent intent;
        Bundle bundle;
        if (id == btAccountMenuID) {
            intent = new Intent(MainScreen.this, AccountSort.class);
            startActivityForResult(intent, Constants.RequestAccountSort);
        }
        else if (id == btTransactionMenuID) {
            intent = new Intent(MainScreen.this, TransactionList.class);
            intent.setAction(Constants.ActionViewAllTransactions);
            bundle = new Bundle();
            bundle.putString(Constants.paramTitle, getResources().getString(R.string.allTransactions));
            intent.putExtras(bundle);
            startActivityForResult(intent, Constants.RequestNONE);
        }
        else if (id == btTransferMenuID) {
            intent = new Intent(MainScreen.this, TransferList.class);
            intent.setAction(Constants.ActionViewTransfersByAccount);
            startActivityForResult(intent, Constants.RequestNONE);
        }
        else if (id == btRPTransactionMenuID) {
            intent = new Intent(MainScreen.this, RPTransactionList.class);
            intent.setAction(Constants.ActionViewRPTransactionsByAccount);
            startActivityForResult(intent, Constants.RequestRPTransactionsForAccount);
        }
        else if (id == btSavingsMenuID) {
            startActivity(new Intent(this, BudgetGoalsList.class));
        }
        else if (id == btDebtsMenuID) {
            intent = new Intent(MainScreen.this, DebtsList.class);
            startActivityForResult(intent, Constants.RequestNONE);
        }
        else if (id == btCurrencyMenuID) {
            startActivityForResult(new Intent("com.jgmoneymanager.intent.action.CURRENCYLIST"), Constants.RequestNONE);
        }
        else if (id == btExCategoryMenuID) {
            startActivityForResult(new Intent("com.jgmoneymanager.intent.action.CATEGORYLISTFOREXPENSE"), Constants.RequestNONE);
        }
        else if (id == btIncCategoryMenuID) {
            startActivityForResult(new Intent("com.jgmoneymanager.intent.action.CATEGORYLISTFORINCOME"), Constants.RequestNONE);
        }
        else if (id == btStatusMenuID) {
            intent = new Intent(MainScreen.this, TransactionStatusList.class);
            startActivityForResult(intent, Constants.RequestNONE);
        }
        else if (id == btPaymentMethodMenuID) {
            intent = new Intent(MainScreen.this, PaymentMethodList.class);
            startActivityForResult(intent, Constants.RequestNONE);
        }
        else if (id == btSettingMenuID) {
            intent = new Intent(MainScreen.this, SettingsMain.class);
            startActivityForResult(intent, Constants.RequestSettingsScreen);
        }
        else if (id == btHelpMenuID) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://sites.google.com/site/jgmoneymanager/"));
            startActivityForResult(browserIntent, Constants.RequestNONE);
        }
        else if (id == btRemoveAdsMenuID) {
            //Tools.removeAds(MainScreen.this);
            LocalTools.removeAds(MainScreen.this);
        }
        else if (id == btAboutMenuID) {
            Tools.showAboutDialog(MainScreen.this, R.string.app_name);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

	@Override
	protected void onStart() {
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		EasyTracker.getInstance(this).activityStop(this);
	}

	@Override

	public void onBackPressed() {
        try {
            if (Tools.rateDialogMustShow(getBaseContext())) {
                Tools.showRateDialog(MainScreen.this);
            } else {
                myApp.setAskPassword(true);
                finish();
                moveTaskToBack(true);
                super.onBackPressed();
            }
        }
        catch (Exception e) {

        }
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		myApp.setAskPassword(false);
		
		super.onConfigurationChanged(newConfig);

        if (CurrencySrv.getDefaultCurrencyID(this) != 0) {
            this.restartActivity();
        }
	}
	
	void initializeViews() {

        setContentView(R.layout.main_activity);
        isOldStyle = false;

        isOldStyle = !((Tools.getPreference(this, R.string.setHomeScreenKey).equals("null")) || (Tools.getPreference(this, R.string.setHomeScreenKey).equals(getResources().getString(R.string.version2Key))));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        LinearLayout mainLayout = (LinearLayout) findViewById(R.id.content_main_layout);
        LayoutInflater inflater = (LayoutInflater)      this.getSystemService(LAYOUT_INFLATER_SERVICE);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        View child;
        if (isOldStyle)
            child = inflater.inflate(R.layout.main_screen_adv, null);
        else
            child = inflater.inflate(R.layout.main_screen, null);
        mainLayout.addView(child, params);

        Menu menu = navigationView.getMenu();

        menu.add(0, btAccountMenuID, btAccountMenuID, R.string.accounts);
        menu.add(0, btTransactionMenuID, btTransactionMenuID, R.string.allTransactions);
        menu.add(0, btTransferMenuID, btTransferMenuID, R.string.menuTransfers);
        menu.add(0, btRPTransactionMenuID, btRPTransactionMenuID, R.string.menuRepeatingTransactions);
        menu.add(0, btSavingsMenuID, btSavingsMenuID, R.string.goals);
        menu.add(0, btDebtsMenuID, btDebtsMenuID, R.string.mydebts);
        menu.add(0, btCurrencyMenuID, btCurrencyMenuID, R.string.currencies);
        menu.add(0, btExCategoryMenuID, btExCategoryMenuID, R.string.expenseCategories);
        menu.add(0, btIncCategoryMenuID, btIncCategoryMenuID, R.string.incomeCategories);
        menu.add(0, btStatusMenuID, btStatusMenuID, R.string.transactionStatus);
        menu.add(0, btPaymentMethodMenuID, btPaymentMethodMenuID, R.string.paymentMethod);
        menu.add(0, btSettingMenuID, btSettingMenuID, R.string.settings);
        menu.add(0, btHelpMenuID, btHelpMenuID, R.string.help);
        //if (!Tools.proVersionExists(this))
            menu.add(0, btRemoveAdsMenuID, btRemoveAdsMenuID, R.string.removeAds);
        menu.add(0, btAboutMenuID, btAboutMenuID, R.string.about);

        generateAccountsLayout();
	}
}
