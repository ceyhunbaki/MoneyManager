package com.jgmoneymanager.budget;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.main.R;
import com.jgmoneymanager.main.SettingsMain;
import com.jgmoneymanager.services.BudgetSrv;
import com.jgmoneymanager.services.ReportSrv;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import java.util.Date;

public class BudgetMain extends MyActivity implements NavigationView.OnNavigationItemSelectedListener{
	private ViewPager viewPager;
	public static TabsPagerAdapter mAdapter;

	private static Date selectedMonth;
	private static long selectedBudgetID;
	final int dateIntervalMonthlyDialogID = 3;
	Spinner spinnerYear;
	Spinner spinnerMonth;
	int minYear;
	String years[] = new String[] {};

	int btGoalsMenuID = Menu.FIRST;
	int btChangeTotalMenuID = btGoalsMenuID + 1;
	int btResetBudgetMenuID = btGoalsMenuID + 2;
	int btValuesMenuID = btGoalsMenuID + 3;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initializeViews();
		if (!Tools.getPreferenceBool(BudgetMain.this, R.string.enablebudgetkey, true))
		{
			Command cmdOpenSettings = new Command() {
				@Override
				public void execute() {
					Intent intent = new Intent(BudgetMain.this, SettingsMain.class);
					startActivityForResult(intent, Constants.RequestNONE);
				}
			};
			AlertDialog dialog = DialogTools.confirmDialog(BudgetMain.this, cmdOpenSettings,
					R.string.msgWarning, R.string.msgBudgetDisabled,
					new String[] {getResources().getString(R.string.settings), getResources().getString(R.string.Cancel)});
			dialog.show();
		}

		if (savedInstanceState != null) {
			selectedMonth = Tools.getDateFromBundle(savedInstanceState, "selectedMonth");
		}
		else
			selectedMonth = Tools.truncDate(this, Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncMonth);
		minYear = BudgetSrv.getBudgetMinimumYear(this);
		int currentYear = Tools.getCurrentDate().getYear() + 1900;
		for (int i=minYear; i<=currentYear; i++)
			years = Tools.addElement(years, String.valueOf(i));

		mAdapter = new TabsPagerAdapter(getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		viewPager = (ViewPager) findViewById(R.id.pager);
		viewPager.setAdapter(mAdapter);

		refreshDateButtonText(false);

		if (Tools.getPreferenceBool(this, R.string.showBudgetRealValueKey, true)) {
			BudgetRealAmountTask bTask = new BudgetRealAmountTask(this, BudgetSrv.getBudgetTotalAmount(BudgetMain.this, getSelectedMonth()),
					getSelectedMonth());
			bTask.execute();
		}

//		if (savedInstanceState == null) {
//			// The Activity is NOT being re-created so we can instantiate a new Fragment
//			// and add it to the Activity
//			BudgetStatus budgetStatus= new BudgetStatus();
//
//			getSupportFragmentManager()
//					.beginTransaction()
//					// It's almost always a good idea to use .replace instead of .add so that
//					// you never accidentally layer multiple Fragments on top of each other
//					// unless of course that's your intention
//					.replace(R.id.pager, budgetStatus)
//					.commit();
//		} else {
//			// The Activity IS being re-created so we don't need to instantiate the Fragment or add it,
//			// but if we need a reference to it, we can use the tag we passed to .replace
//			mFragment = (MyFragment) getSupportFragmentManager().findFragmentByTag(TAG_MY_FRAGMENT);
//		}
	}

	private void initializeViews() {
		setContentView(R.layout.main_activity);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
				this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
		drawer.setDrawerListener(toggle);
		toggle.syncState();

		NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(this);

		LinearLayout mainLayout = (LinearLayout) findViewById(R.id.content_main_layout);
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		View child = inflater.inflate(R.layout.budgetmain, null);
		mainLayout.addView(child, params);

		Menu menu = navigationView.getMenu();
		menu.add(0, btGoalsMenuID, btGoalsMenuID, R.string.goals);
		menu.add(0, btChangeTotalMenuID, btChangeTotalMenuID, R.string.menuChangeTotalAmount);
		menu.add(0, btResetBudgetMenuID, btResetBudgetMenuID, R.string.menuResetBudget);
		menu.add(0, btValuesMenuID, btValuesMenuID, R.string.menuSetValues);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Tools.putToBundle(outState, "selectedMonth", selectedMonth);
	}

	public void myClickHandler(View target) {
		switch (target.getId()) {
			case R.id.bt_group_edit:
				if (BudgetSrv.isBudgetAvialable(BudgetMain.this, selectedMonth))
					openContextMenu(target);
				else
					DialogTools.toastDialog(BudgetMain.this, R.string.msgBudgetNotAvialable, Toast.LENGTH_SHORT);
				break;
			case R.id.bt_child_edit:
				if (BudgetSrv.isBudgetAvialable(BudgetMain.this, selectedMonth))
					openContextMenu(target);
				else
					DialogTools.toastDialog(BudgetMain.this, R.string.msgBudgetNotAvialable, Toast.LENGTH_SHORT);
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

	void showChangeBudgetDialog(Double currentValue) {
		final EditText inputField = new EditText(BudgetMain.this);
		inputField.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
		inputField.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				if (inputField.getText().toString().length() != 0)
					try {
						Double.parseDouble(inputField.getText().toString());
					} catch (NumberFormatException e) {
						DialogTools.toastDialog(BudgetMain.this, R.string.msgInvalidNumber, Toast.LENGTH_SHORT);
					}
			}
		});
		inputField.setText(Tools.formatDecimal(currentValue));
		Command cmdChangeManually = new Command() {
			@Override
			public void execute() {
				BudgetSrv.changeBudgetTotalAmount(BudgetMain.this, getSelectedMonth(),
						Tools.stringToDouble(BudgetMain.this, inputField.getText().toString(), false));
				BudgetStatus fragment = (BudgetStatus)mAdapter.getItem(0);
				getSupportFragmentManager().beginTransaction().detach(fragment).attach(fragment).commit();
				fragment.refreshValues();
			}
		};
		AlertDialog inputDialog = DialogTools.InputDialog(BudgetMain.this, cmdChangeManually, R.string.menuChangeTotalAmount, inputField, R.drawable.ic_menu_edit);
		inputDialog.show();
		inputDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(inputField.getText().toString().trim().length() != 0);
	}

	void refreshDateButtonText(boolean refreshLists) {
		((Button)findViewById(R.id.repBtDate)).setText(ReportSrv.getDateButtonText(this, Constants.ReportTimeInterval.Monthly.index(), selectedMonth, selectedMonth, false));
		selectedBudgetID = BudgetSrv.getBudgetId(BudgetMain.this, selectedMonth);
		if (refreshLists) {
			try {
				mAdapter.getBudgetStatus().refreshValues();
			}
			catch (Exception e) {
				//Log.e(e);
			}
			try {
				mAdapter.getBudgetCategories().refreshList();
			}
			catch (Exception e) {
				//Log.e(e);
			}
		}
	}

	public Date getSelectedMonth() {
		if (selectedMonth == null)
			selectedMonth = Tools.truncDate(this, Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncMonth);
		return selectedMonth;
	}

	public static long getSelectedBudgetID() {
		return selectedBudgetID;
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

		ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, R.layout.simple_spinner_item_blue, years);
		spinnerArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_blue);
		spinnerYear.setAdapter(spinnerArrayAdapter);

		spinnerArrayAdapter = new ArrayAdapter<String>(this, R.layout.simple_spinner_item_blue, getResources().getStringArray(R.array.Months));
		spinnerArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_blue);
		spinnerMonth.setAdapter(spinnerArrayAdapter);

		spinnerYear.setSelection(selectedMonth.getYear() - minYear + 1900);
		spinnerMonth.setSelection(selectedMonth.getMonth());

		AlertDialog viewDialog = DialogTools.CustomDialog(this, cmd, R.string.msgSelectPeriod, view, R.drawable.ic_menu_edit);
		viewDialog.show();
		return null;
	}
	
	/*void correctBudgetValues() {
		Cursor cursorMainCategories = getBaseContext().getContentResolver().query(CategoryTableMetaData.CONTENT_URI, 
				null, CategoryTableMetaData.MAINID + " is null ", null, null);
		for (cursorMainCategories.moveToFirst(); !cursorMainCategories.isAfterLast(); cursorMainCategories.moveToNext()) {
			String mainCatId = DBTools.getCursorColumnValue(cursorMainCategories, CategoryTableMetaData._ID);
			Cursor cursorMainCategoryBudget = getBaseContext().getContentResolver().query(
					BudgetCategoriesTableMetaData.CONTENT_URI, null, 
					BudgetCategoriesTableMetaData.CATEGORY_ID + " = " + mainCatId, 
							null, null);
			if (cursorMainCategoryBudget.moveToFirst()) {				
				Double mainCatBudgetValue = DBTools.getCursorColumnValueDouble(cursorMainCategoryBudget, BudgetCategoriesTableMetaData.BUDGET);
				if (mainCatBudgetValue.compareTo(0d) > 0) {
					Cursor cursorSubCategoris = getBaseContext().getContentResolver().query(
							CategoryTableMetaData.CONTENT_URI, null, 
							CategoryTableMetaData.MAINID + " = " + mainCatId, null, CategoryTableMetaData.NAME);
					for (cursorSubCategoris.moveToFirst(); !cursorSubCategoris.isAfterLast(); cursorSubCategoris.moveToNext()) {
						String subCatID = DBTools.getCursorColumnValue(cursorSubCategoris, CategoryTableMetaData._ID);
						Cursor cursorSubBudget = getContentResolver().query(BudgetCategoriesTableMetaData.CONTENT_URI, 
								null, BudgetCategoriesTableMetaData.CATEGORY_ID + " = " + subCatID, null, null);
						if (cursorSubBudget.moveToFirst()) {
							Double subUsedValue = DBTools.getCursorColumnValueDouble(cursorSubBudget, BudgetCategoriesTableMetaData.USED_AMOUNT);
							Double subBudget = DBTools.getCursorColumnValueDouble(cursorSubBudget, BudgetCategoriesTableMetaData.BUDGET);
							if (subUsedValue.compareTo(subBudget))
						}
					}
				}
			}
		}
	}*/

	@Override
	public boolean onNavigationItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == btGoalsMenuID) {
			startActivity(new Intent(this, BudgetGoalsList.class));
		}
		else if (id == btChangeTotalMenuID) {
			final Double currentValue = BudgetSrv.getBudgetTotalAmount(BudgetMain.this, getSelectedMonth());
			final Double realAmount = BudgetSrv.getTotalAmountForBudget(BudgetMain.this, getSelectedMonth(),
					Tools.lastDay(BudgetMain.this, getSelectedMonth()));
			if (currentValue.compareTo(Tools.round(realAmount)) != 0) {
				Command realAmountYesCommand = new Command() {
					@Override
					public void execute() {
						BudgetSrv.changeBudgetTotalAmount(BudgetMain.this, getSelectedMonth(), realAmount);
						BudgetStatus fragment = (BudgetStatus) mAdapter.getItem(0);
						fragment.refreshValues();
					}
				};
				Command realAmountNoCommand = new Command() {
					@Override
					public void execute() {
						showChangeBudgetDialog(currentValue);
					}
				};
				Command realAmountCancelCommand = new Command() {
					public void execute() {

					}
				};
				AlertDialog realAmountDialog = DialogTools.confirmWithCancelDialog(BudgetMain.this, realAmountYesCommand,
						realAmountNoCommand, realAmountCancelCommand, R.string.menuChangeTotalAmount,
						BudgetMain.this.getResources().getString(R.string.msgBudgetRealValue) + " " + Tools.formatDecimalInUserFormat(realAmount),
						new String[]{BudgetMain.this.getResources().getString(R.string.applyThis),
								BudgetMain.this.getResources().getString(R.string.change), BudgetMain.this.getResources().getString(R.string.Cancel)});
				realAmountDialog.show();
			}
			else
				showChangeBudgetDialog(currentValue);
		}
		else if (id == btResetBudgetMenuID) {
			Command resetCommand = new Command() {
				@Override
				public void execute() {
					BudgetSrv.resetBudget(BudgetMain.this, selectedBudgetID, selectedMonth);
					BudgetStatus fragment = (BudgetStatus)mAdapter.getItem(0);
					fragment.refreshValues();
					BudgetCategories fragment1 = (BudgetCategories)mAdapter.getItem(1);
					fragment1.refreshList();
				}
			};
			AlertDialog resetDialog = DialogTools.confirmDialog(BudgetMain.this, resetCommand, R.string.msgConfirm,
					R.string.msgResetBudget);
			resetDialog.show();
		}
		else if (id == btValuesMenuID) {
			if (viewPager.getCurrentItem() == 0)
				viewPager.setCurrentItem(1);
			else
				viewPager.setCurrentItem(0);
		}

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.closeDrawer(GravityCompat.START);

		return true;
	}


}
