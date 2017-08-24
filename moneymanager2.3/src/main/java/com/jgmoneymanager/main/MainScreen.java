package com.jgmoneymanager.main;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.jgmoneymanager.SlidingMenu.MyHorizontalScrollView;
import com.jgmoneymanager.SlidingMenu.MyHorizontalScrollView.SizeCallback;
import com.jgmoneymanager.SlidingMenu.OnSwipeTouchListener;
import com.jgmoneymanager.budget.BudgetStatus;
import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CurrencyTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.reports.ReportList;
import com.jgmoneymanager.services.AccountSrv;
import com.jgmoneymanager.services.CurrencySrv;
import com.jgmoneymanager.entity.CheckBoxItem;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.GetNecessaryCurrRatesAndControlBudgetTask;
import com.jgmoneymanager.tools.LocalTools;
import com.jgmoneymanager.tools.RefreshAccountDetailsTask;
import com.jgmoneymanager.tools.Tools;
import com.jgmoneymanager.tools.TranslateDBTask;

import java.util.ArrayList;
import java.util.Date;

public class MainScreen extends MyActivity{
    MyHorizontalScrollView scrollView;
    static View menu;
    View app;
    ImageView btnSlide;
    static boolean menuOut = false;
    
    final int buttonFirstId = 1000;
	ArrayList<CheckBoxItem> accountsList;
	Button[] buttons;
	//float deactiveButtonTextSize;
	//float activeButtonTextSize;
	LayoutParams accountButtonParams;

    String btAccountTag = "btAccountTag";
    String btTransactionTag = "btTransactionTag";
    String btTransferTag = "btTransferTag";
    String btRPTransactionTag = "btRPTransactionTag";
    //String btBudget;
    String btDebtsTag = "btDebtsTag";
    String btCurrencyTag = "btCurrencyTag";
    String btExCategoryTag = "btExCategoryTag";
    String btIncCategoryTag = "btIncCategoryTag";
    String btStatusTag = "btStatusTag";
    String btPaymentMethodTag = "btPaymentMethodTag";
    String btSettingTag = "btSettingTag";
    String btHelpTag = "btHelpTag";
    String btRemoveAdsTag = "btRemoveAdsTag";
    String btAboutTag = "btAboutTag";

	public long selectedAccountID = 0;
    boolean isOldStyle = false;
	//static boolean isDetailsChanged = false;
	
	//static boolean askPassword = true;
	//MyApplicationLocal myApp;
	    
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		
		Tools.loadSettings(this);
        Tools.loadLanguage(this, null);
        LocalTools.showWhatsNewDialog(this);

        //Tools.getDb`  FileModifiedDate();

        super.onCreate(savedInstanceState);
        
        if (CurrencySrv.getDefaultCurrencyID(this) == 0) {
        	final Cursor cursor = getContentResolver().query(CurrencyTableMetaData.CONTENT_URI, null, null, null, CurrencyTableMetaData.NAME);
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
            AlertDialog dialog = DialogTools.RadioListDialog(MainScreen.this, cmd, R.string.msgSetDefaultCurrency, cursor, CurrencyTableMetaData.NAME, false);
        	dialog.show();        	
        }
        
        GetNecessaryCurrRatesAndControlBudgetTask getRatesTask = new GetNecessaryCurrRatesAndControlBudgetTask(MainScreen.this, myApp);
        getRatesTask.execute("");

        initializeViews();
        
        if ((savedInstanceState != null) && (savedInstanceState.containsKey("selectedAccountID")))
        	selectedAccountID = Tools.getLongFromBundle0(savedInstanceState, "selectedAccountID");
        if (selectedAccountID == 0)
        	selectedAccountID = AccountSrv.getDefultAccountID(MainScreen.this, null);
        setActiveAccountButton(Integer.parseInt(String.valueOf(selectedAccountID)));
        refreshAccountDetails(selectedAccountID);
        
        menu.setOnTouchListener(mySwipeListener);
        app.setOnTouchListener(mySwipeListener);
        
        /*if (myApp.getAskPassword() && SetPassword.passwordRequired(MainScreen.this)) {
        	Intent intent = new Intent(MainScreen.this, StartupPassword.class);
        	intent.setAction(Constants.ActionStartupPassword);
        	startActivityForResult(intent, Constants.RequestPasswordInStartup);
        }*/ 
        
        LocalTools.startupActions(MainScreen.this);

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
    	
    	accountsList = AccountSrv.generateAccountsList(MainScreen.this, getResources().getString(R.string.all));
    	buttons = new Button[accountsList.size()];
    	
    	//activeButtonTextSize = ((Button)findViewById(R.id.btATIncome)).getTextSize() - 3;
    	//deactiveButtonTextSize = activeButtonTextSize - 4;

    	accountButtonParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        int margin = Math.round(getResources().getDimension(R.dimen.main_account_names_margin) / getResources().getDisplayMetrics().density);
        accountButtonParams.setMargins(margin, 0, margin, 0);
    	
    	for (int j = 0; j < accountsList.size(); j++) {
    		CheckBoxItem item = accountsList.get(j);
    		buttons[j] = new Button(this);    	        
    	    buttons[j].setLayoutParams(accountButtonParams);
    	    buttons[j].setText("  " + item.getName() + "  ");
            buttons[j].setId(buttonFirstId + Integer.parseInt(String.valueOf(item.getID())));
            /*if (isOldStyle) {
                buttons[j].setTextAppearance(this, R.style.Theme_MainBigLabel_DeactiveAccount);
                buttons[j].setTextColor(getResources().getColor(R.color.DarkGray));
                buttons[j].setBackgroundResource(R.drawable.account_button_first);
            }
            else {*/
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
            /*if (isOldStyle) {
                oldButton.setTextColor(getResources().getColor(R.color.DarkGray));
                oldButton.setTextAppearance(this, R.style.Theme_MainBigLabel_DeactiveAccount);
            }
            else*/
	    	    oldButton.setTextAppearance(this, R.style.ThemeNew_Main_DeactiveAccount);
    	}
    	catch (Exception e) {
    		selectedAccountID = AccountSrv.getDefultAccountID(getBaseContext(), null);
    		accountID = selectedAccountID;
    	}
    	
    	Button activeButton = (Button)findViewById(buttonFirstId + Integer.parseInt(String.valueOf(accountID)));
        /*if (isOldStyle) {
            activeButton.setTextColor(getResources().getColor(R.color.White));
            activeButton.setTextAppearance(this, R.style.Theme_MainBigLabel_ActiveAccount);
        }
        else*/
    	    activeButton.setTextAppearance(this, R.style.ThemeNew_Main_ActiveAccount);
    	activeButton.requestFocus();
    	
    	//buttonu ortaya getirek
    	HorizontalScrollView scroolView = (HorizontalScrollView)findViewById(R.id.hsATAccounts);
    	scroolView.scrollTo(activeButton.getLeft() - ((scroolView.getWidth() - activeButton.getWidth()) / 2), 0);
    	
    	selectedAccountID = accountID;
    }

    public void refreshAccountDetails(long accountID) {
        if (isOldStyle)
            refreshAccountDetailsOld(accountID);
        else
            refreshAccountDetailsNew(accountID);
    }

	public void refreshAccountDetailsNew(long accountID) {
		//balance deyerinin teyini
        TextView balanceCurrencyLabel = (TextView) MainScreen.this.findViewById(R.id.tvATBalanceCurrency);
        TextView balanceAmountLabel = (TextView) MainScreen.this.findViewById(R.id.tvATBalanceAmount);

		StringBuffer currencySign = new StringBuffer();
		float balance = AccountSrv.getAccountBalance(MainScreen.this, selectedAccountID, currencySign);
		balanceCurrencyLabel.setText(currencySign.toString());
        balanceAmountLabel.setText(Tools.formatDecimal(balance));
	}

    public void refreshAccountDetailsOld(long accountID) {
        //balance deyerinin teyini
        TextView balanceAmountLabel = (TextView) MainScreen.this.findViewById(R.id.tvATBalanceAmount);
        StringBuffer currencySign = new StringBuffer();
        float balance = AccountSrv.getAccountBalance(MainScreen.this, selectedAccountID, currencySign);
        balanceAmountLabel.setText(Tools.formatDecimal(balance) + " " + currencySign.toString());
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
    
	//menu el ile geriye cekmek ucun
    OnSwipeTouchListener mySwipeListener = new OnSwipeTouchListener() {
        public void onSwipeLeft() {
    		if (menu.getVisibility() == View.VISIBLE) {
    			menu.setVisibility(View.INVISIBLE);
    			scrollView.smoothScrollTo(menu.getMeasuredWidth(), 0);
    			menuOut = false;
    		}
        }
        public void onSwipeRight() {
    		if (menu.getVisibility() == View.INVISIBLE) {
    			menu.setVisibility(View.VISIBLE);
    			scrollView.smoothScrollTo(0, 0);
    			menuOut = true;
    		}
        }    	
    };
    
    OnClickListener myClickListener = new OnClickListener() {		
		@Override
		public void onClick(View v) {
			int newAccountID = v.getId() - buttonFirstId;
			if (newAccountID != selectedAccountID) {
				setActiveAccountButton(newAccountID);
				refreshAccountDetails(newAccountID);
			}
		}
	};
	
	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		//if (MainScreen.menu.getVisibility() == View.VISIBLE) {
		if (menuOut) {
			MainScreen.menu.setVisibility(View.INVISIBLE);
			scrollView.smoothScrollTo(MainScreen.menu.getMeasuredWidth(), 0);
			menuOut = false;
		}
		else {
			MainScreen.menu.setVisibility(View.VISIBLE);
			scrollView.smoothScrollTo(0, 0);
			menuOut = true;
		}
		return super.onMenuOpened(featureId, menu);
	}
	
	@Override
	protected void onResume() {
		super.onResume();	
		LocalTools.onResumeEvents(this);
		if (menu.getVisibility() == View.VISIBLE) {
			menu.setVisibility(View.INVISIBLE);
			scrollView.smoothScrollTo(menu.getMeasuredWidth(), 0);
		}
		setActiveAccountButton(selectedAccountID);
		if (myApp.isMainDetailsChanged()) {
			refreshAccountDetails(selectedAccountID);
			myApp.setMainDetailsChanged(false);
		}
	    
	    /*if (dropboxAutoUpload == null) {
	    	IBinder binder = null;
		    DropboxAutoUpload.MyBinder b = (DropboxAutoUpload.MyBinder) binder;
		    dropboxAutoUpload = b.getService();
	    }*/
	}

	public void myClickHandler(View target) {
		Intent intent;
		Bundle bundle;
        if (target.getTag() == btAccountTag) {
            intent = new Intent(MainScreen.this, AccountSort.class);
            startActivityForResult(intent, Constants.RequestAccountSort);
        }
        else if (target.getTag() == btTransactionTag) {
            intent = new Intent(MainScreen.this, TransactionList.class);
            intent.setAction(Constants.ActionViewAllTransactions);
            bundle = new Bundle();
            bundle.putString(Constants.paramTitle, getResources().getString(R.string.allTransactions));
            intent.putExtras(bundle);
            startActivityForResult(intent, Constants.RequestNONE);
        }
        else if (target.getTag() == btTransferTag) {
            intent = new Intent(MainScreen.this, TransferList.class);
            intent.setAction(Constants.ActionViewTransfersByAccount);
            startActivityForResult(intent, Constants.RequestNONE);
        }
        else if (target.getTag() == btRPTransactionTag) {
            intent = new Intent(MainScreen.this, RPTransactionList.class);
            intent.setAction(Constants.ActionViewRPTransactionsByAccount);
            startActivityForResult(intent, Constants.RequestRPTransactionsForAccount);
        }
        else if (target.getTag() == btDebtsTag) {
            intent = new Intent(MainScreen.this, DebtsList.class);
            startActivityForResult(intent, Constants.RequestNONE);
        }
        else if (target.getTag() == btCurrencyTag) {
            startActivityForResult(new Intent("com.jgmoneymanager.intent.action.CURRENCYLIST"), Constants.RequestNONE);
        }
        else if (target.getTag() == btExCategoryTag) {
            startActivityForResult(new Intent("com.jgmoneymanager.intent.action.CATEGORYLISTFOREXPENSE"), Constants.RequestNONE);
        }
        else if (target.getTag() == btIncCategoryTag) {
            startActivityForResult(new Intent("com.jgmoneymanager.intent.action.CATEGORYLISTFORINCOME"), Constants.RequestNONE);
        }
        else if (target.getTag() == btStatusTag) {
            intent = new Intent(MainScreen.this, TransactionStatusList.class);
            startActivityForResult(intent, Constants.RequestNONE);
        }
        else if (target.getTag() == btPaymentMethodTag) {
            intent = new Intent(MainScreen.this, PaymentMethodList.class);
            startActivityForResult(intent, Constants.RequestNONE);
        }
        else if (target.getTag() == btSettingTag) {
            intent = new Intent(MainScreen.this, SettingsScreen.class);
            startActivityForResult(intent, Constants.RequestSettingsScreen);
        }
        else if (target.getTag() == btHelpTag) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://sites.google.com/site/jgmoneymanager/"));
            startActivityForResult(browserIntent, Constants.RequestNONE);
        }
        else if (target.getTag() == btRemoveAdsTag) {
            Tools.removeAds(MainScreen.this);
            this.menu.setVisibility(View.INVISIBLE);
            scrollView.smoothScrollTo(this.menu.getMeasuredWidth(), 0);
            menuOut = false;
        }
        else if (target.getTag() == btAboutTag) {
            Tools.showAboutDialog(MainScreen.this, R.string.app_name);
        }
        else {
            switch (target.getId()) {
                case R.id.btATAccounts:
                    Cursor cursor = this.managedQuery(MoneyManagerProviderMetaData.VTransAccountViewMetaData.CONTENT_URI, null,
                            MoneyManagerProviderMetaData.VTransAccountViewMetaData.STATUS + " =1 ", null, null);
                    Command cmd = new Command() {
                        @Override
                        public void execute() {
                            Cursor cursorInt = getContentResolver().query(MoneyManagerProviderMetaData.VTransAccountViewMetaData.CONTENT_URI,
                                    new String[] {MoneyManagerProviderMetaData.VTransAccountViewMetaData._ID}, MoneyManagerProviderMetaData.VTransAccountViewMetaData.STATUS + " =1 ",
                                    null, null);
                            cursorInt.moveToPosition(Constants.cursorPosition);
                            //selectedAccountID = DBTools.getCursorColumnValueInt(cursorInt, VTransAccountViewMetaData._ID);
                            setActiveAccountButton(DBTools.getCursorColumnValueInt(cursorInt, MoneyManagerProviderMetaData.VTransAccountViewMetaData._ID));
                            refreshAccountDetails(selectedAccountID);
                        }
                    };
                    AlertDialog accountList = DialogTools.RadioListDialog(this, cmd, R.string.selectAccount, cursor, MoneyManagerProviderMetaData.VTransAccountViewMetaData.NAME, true);
                    accountList.show();
                    break;
                case R.id.btATBudget:
                    intent = new Intent(MainScreen.this, BudgetStatus.class);
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
                    bundle.putInt(MoneyManagerProviderMetaData.TransactionsTableMetaData.TRANSTYPE, Constants.TransactionType);
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
                    bundle.putInt(MoneyManagerProviderMetaData.TransactionsTableMetaData.TRANSTYPE, Constants.TransactionType);
                    if (selectedAccountID != 0)
                        bundle.putLong(Constants.paramAccountID, selectedAccountID);
                    intent.putExtras(bundle);
                    startActivityForResult(intent, Constants.RequestTransactionInsert);
                    break;
                case R.id.btATExpence:

                    //dropboxAutoUpload.upload();

                    intent = new Intent(this, TransactionEdit.class);
                    intent.setAction(Intent.ACTION_INSERT);
                    bundle = new Bundle();
                    bundle.putString(Constants.UpdateMode, Constants.Insert);
                    Constants.TransactionType = Constants.TransactionTypeExpence;
                    bundle.putInt(MoneyManagerProviderMetaData.TransactionsTableMetaData.TRANSTYPE, Constants.TransactionType);
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
	}	
		
	void callTransactionList(int periodType, int transactiontype) {
		Intent intent = new Intent(MainScreen.this, TransactionList.class);
		intent.setAction(Constants.ActionViewTransactionsFromReport);
		String title;
		Bundle bundle = new Bundle();
		Date fromDate;
		if (periodType == Constants.DateFilterValues.ThisMonth.index()) {
			fromDate = Tools.truncDate(Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncMonth);
			title = getResources().getString(R.string.thisMonth) + " - ";
		}
		else if (periodType == Constants.DateFilterValues.ThisYear.index()) {
			fromDate = Tools.truncDate(Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncYear);
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

		if (SettingsScreen.languageChanged || SettingsScreen.homeScreenVersionChanged) {
            if (SettingsScreen.languageChanged) {
                TranslateDBTask translateCategoriesTask = new TranslateDBTask(MainScreen.this);
                translateCategoriesTask.execute();
            }

			restartActivity();
			SettingsScreen.languageChanged = false;
            SettingsScreen.homeScreenVersionChanged = false;
		}
		//myApp.setAskPassword(false);

		if (requestCode == Constants.RequestPasswordInStartup) { 
			if (resultCode != RESULT_OK)
				finish();
			/*else 
				myApp.setAskPassword(false);*/
		}
		
		if (resultCode == RESULT_OK) {
			/*if (requestCode == Constants.RequestTransactionInsert)
				refreshAccountDetails(selectedAccountID);
			else if (requestCode == Constants.RequestTransferInsert)
				refreshAccountDetails(selectedAccountID);
			else if (requestCode == Constants.RequestTransactionByAccount)
				refreshAccountDetails(selectedAccountID);
			else*/ 
			if (requestCode == Constants.RequestAccountSort) {
		        restartActivity();
			}
		}
		if ((requestCode == Constants.RequestAccountSort) && (resultCode == RESULT_CANCELED) && AccountSort.resultOK) 
			restartActivity();
		
		/*if (myApp.getFinishApplication())
			finish();*/
	}
	
	private void restartActivity() {
	    Intent intent = getIntent();
	    finish();
	    startActivity(intent);
	}

	/**
     * Helper for examples with a HSV that should be scrolled by a menu View's width.
     */
    static class ClickListenerForScrolling implements OnClickListener {
        HorizontalScrollView scrollView;
        View menu;

        public ClickListenerForScrolling(HorizontalScrollView scrollView, View menu) {
            super();
            this.scrollView = scrollView;
            this.menu = menu;
        }

        @Override
        public void onClick(View v) {
            // Ensure menu is visible
            if (!menuOut) {
                scrollView.smoothScrollTo(0, 0);
                menu.setVisibility(View.VISIBLE);
            } else {
                scrollView.smoothScrollTo(menu.getMeasuredWidth(), 0);
                menu.setVisibility(View.INVISIBLE);
            }
            menuOut = !menuOut;
        }
    }

    /**
     * Helper that remembers the width of the 'slide' button, so that the 'slide' button remains in view, even when the menu is
     * showing.
     */
    static class SizeCallbackForMenu implements SizeCallback {
        int btnWidth;
        View btnSlide;

        public SizeCallbackForMenu(View btnSlide) {
            super();
            this.btnSlide = btnSlide;
        }

        @Override
        public void onGlobalLayout() {
            btnWidth = btnSlide.getMeasuredWidth();
            System.out.println("btnWidth=" + btnWidth);
        }

        @Override
        public void getViewSize(int idx, int w, int h, int[] dims) {
            dims[0] = w;
            dims[1] = h;
            final int menuIdx = 0;
            if (idx == menuIdx) {
                dims[0] = w - btnWidth;
            }
        }
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
		//myApp.setAskPassword(true);

		/*try {
			dropboxAutoUpload.upload();
		}
		catch (Exception e) {
			Log.e("dropboxAutoUpload", "xeta");
		}*/
	}

    @Override
	protected void onPause() {	    
		super.onPause();
	}

	@Override

	public void onBackPressed() {
    	if (Tools.rateDialogMustShow(getBaseContext())) {
    		Tools.showRateDialog(MainScreen.this);
    		return;
    	}
    	else {
    		myApp.setAskPassword(true);
    		finish();
    		moveTaskToBack(true);
    		super.onBackPressed();
    	}
	}

	/*public static void refreshDetails() {
    	isDetailsChanged = isDetailsChanged || true;
    }*/
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		myApp.setAskPassword(false);
		
		super.onConfigurationChanged(newConfig);
		
		this.restartActivity();
	}
	
	void initializeViews() {
		LayoutInflater inflater = LayoutInflater.from(this);		
        scrollView = (MyHorizontalScrollView) inflater.inflate(R.layout.horz_scroll_with_list_menu, null);

        setContentView(scrollView);
        
        //myApp = (MyApplicationLocal)getApplication();
        
        menu = inflater.inflate(R.layout.horz_scroll_menu, null);

        if ((Tools.getPreference(this, R.string.setHomeScreenKey).equals("null")) || (Tools.getPreference(this, R.string.setHomeScreenKey).equals(getResources().getString(R.string.version2Key)))) {
            app = inflater.inflate(R.layout.main_screen, null);
            isOldStyle = false;
        }
        else {
            app = inflater.inflate(R.layout.main_screen_adv, null);
            isOldStyle = true;
        }

        ViewGroup tabBar = (ViewGroup) app.findViewById(R.id.relATTop);

        btnSlide = (ImageView) tabBar.findViewById(R.id.btATMenu);
        btnSlide.setOnClickListener(new ClickListenerForScrolling(scrollView, menu));

        final View[] children = new View[] { menu, app };

        // Scroll to app (view[1]) when layout finished.
        int scrollToViewIdx = 1;
        scrollView.initViews(children, scrollToViewIdx, new SizeCallbackForMenu(btnSlide));

        LocalTools.addButtonToMenuList(this, R.string.accounts, btAccountTag);
        LocalTools.addButtonToMenuList(this, R.string.allTransactions, btTransactionTag);
        LocalTools.addButtonToMenuList(this, R.string.menuTransfers, btTransferTag);
        LocalTools.addButtonToMenuList(this, R.string.menuRepeatingTransactions, btRPTransactionTag);
        ///**/btBudget = LocalTools.addButtonToMenuList(this, R.string.budget, R.id.hmLayOpen);
        LocalTools.addButtonToMenuList(this, R.string.mydebts, btDebtsTag);
        LocalTools.addButtonToMenuList(this, R.string.currencies, btCurrencyTag);
        LocalTools.addButtonToMenuList(this, R.string.expenseCategories, btExCategoryTag);
        LocalTools.addButtonToMenuList(this, R.string.incomeCategories, btIncCategoryTag);
        LocalTools.addButtonToMenuList(this, R.string.transactionStatus, btStatusTag);
        LocalTools.addButtonToMenuList(this, R.string.paymentMethod, btPaymentMethodTag);
        LocalTools.addButtonToMenuList(this, R.string.settings, btSettingTag);
        LocalTools.addButtonToMenuList(this, R.string.help, btHelpTag);
        if (!Tools.proVersionExists(this))
            LocalTools.addButtonToMenuList(this, R.string.removeAds, btRemoveAdsTag);
        LocalTools.addButtonToMenuList(this, R.string.about, btAboutTag);

        generateAccountsLayout();
                
        menu.setOnTouchListener(mySwipeListener);
        app.setOnTouchListener(mySwipeListener);

        //((TextView)findViewById(R.id.tvATTitle)).setText(R.string.testString);
	}
	
}
