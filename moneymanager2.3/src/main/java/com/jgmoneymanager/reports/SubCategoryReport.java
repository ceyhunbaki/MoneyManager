package com.jgmoneymanager.reports;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jgmoneymanager.SlidingMenu.MyHorizontalScrollView;
import com.jgmoneymanager.SlidingMenu.OnSwipeTouchListener;
import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CategoryTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CurrencyTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransactionsTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VRatesToDefaultViewMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.main.R;
import com.jgmoneymanager.main.TransactionList;
import com.jgmoneymanager.services.TransactionSrv;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.LocalTools;
import com.jgmoneymanager.tools.Tools;

import java.util.Calendar;
import java.util.Date;

public class SubCategoryReport extends MyActivity{

	private MyHorizontalScrollView scrollView;
	private static View menu;
	private View app;
	private ImageView btnSlide;
	static boolean menuOut = false;

	private String btMenuExpenseTag = "btMenuExpenseTag";
	private String btMenuIncomeTag = "btMenuIncomeTag";
	private String btMenuSummaryTag = "btMenuSummaryTag";
	
	int reportType = Constants.TransFTransaction.All.index();
	private int mGroupIdColumnIndex = 0;
	String mainSQL;
	
	final int dateIntervalDailyDialogID = 1;
	final int dateIntervalWeeklyDialogID = 2;
	final int dateIntervalMonthlyDialogID = 3;
	final int dateIntervalYearlyDialogID = 4;
	final int dateIntervalCustomDialogID = 5;
	final int fromDateDialogID = 6;
	final int toDateDialogID = 7;

	boolean clearDates = true;
	int currentDateInterval = Constants.ReportTimeInterval.Monthly.index();
	Date startDate, endDate;
	int minTransDate;
	String years[] = new String[] {};
		
	Button btIntervaltype;
	Button btDate;
	TextView tvTitle;
	
	Spinner spinnerYear;
	Spinner spinnerMonth;
	
	ExpandableListView listView;

	View dialogView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initializeViews();
		
		minTransDate = TransactionSrv.getMinTransactionYear(SubCategoryReport.this);
		int currentYear = Tools.getCurrentDate().getYear() + 1900;
		for (int i=minTransDate; i<=currentYear; i++)
			years = Tools.addElement(years, String.valueOf(i));

		if (savedInstanceState != null) {
			clearDates = Tools.getBooleanFromBundle0(savedInstanceState, "clearDates");
			startDate =  Tools.getDateFromBundle(savedInstanceState, "startDate");
			endDate =  Tools.getDateFromBundle(savedInstanceState, "endDate");
			currentDateInterval = Tools.getIntegerFromBundle0(savedInstanceState, "currentDateInterval");
			reportType = Tools.getIntegerFromBundle0(savedInstanceState, "reportType");
		}
		reloadScreen();
		if (clearDates)
			recreateDateInterval(0);
		else
			refreshDateButtonText();
		refreshList();		
	}

	private void initializeViews() {
		LayoutInflater inflater = LayoutInflater.from(this);
		scrollView = (MyHorizontalScrollView) inflater.inflate(R.layout.horz_scroll_with_list_menu, null);

		setContentView(scrollView);

		//myApp = (MyApplicationLocal)getApplication();

		menu = inflater.inflate(R.layout.horz_scroll_menu, null);
		app = inflater.inflate(R.layout.listreport, null);
		ViewGroup tabBar = (ViewGroup) app.findViewById(R.id.relATTop);

		btnSlide = (ImageView) tabBar.findViewById(R.id.btATMenu);
		btnSlide.setOnClickListener(new ClickListenerForScrolling(scrollView, menu));

		final View[] children = new View[] { menu, app };

		// Scroll to app (view[1]) when layout finished.
		int scrollToViewIdx = 1;
		scrollView.initViews(children, scrollToViewIdx, new SizeCallbackForMenu(btnSlide));

		menu.setOnTouchListener(mySwipeListener);

		LocalTools.addButtonToMenuList(this, R.string.menuSummary, btMenuSummaryTag);
		LocalTools.addButtonToMenuList(this, R.string.expense, btMenuExpenseTag);
		LocalTools.addButtonToMenuList(this, R.string.income, btMenuIncomeTag);
		//app.setOnTouchListener(mySwipeListener);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Tools.putToBundle(outState, "clearDates", false);
		Tools.putToBundle(outState, "startDate", Tools.DateToString(startDate));
		Tools.putToBundle(outState, "endDate", Tools.DateToString(endDate));
		Tools.putToBundle(outState, "currentDateInterval", currentDateInterval);
		Tools.putToBundle(outState, "reportType", reportType);
		super.onSaveInstanceState(outState);
	}
	
	void reloadScreen() {
		btIntervaltype = (Button) findViewById(R.id.repBtInterval);
		btIntervaltype.setText(getResources().getTextArray(R.array.ReportTimeInterval)[currentDateInterval].toString());
		btDate = (Button) findViewById(R.id.repBtDate);
		tvTitle = (TextView) findViewById(R.id.tvATTitle);
	}
	
	void refreshTitle() {
		int stringID = 0;
		if (reportType == Constants.TransFTransaction.All.index()) 
			stringID = R.string.menuSummary;
		else if (reportType == Constants.TransFTransaction.Income.index()) 
			stringID = R.string.income;
		else if (reportType == Constants.TransFTransaction.Expence.index()) 
			stringID = R.string.expense;
		
		tvTitle.setText(getResources().getString(R.string.categories) + " - " + getResources().getString(stringID));
	}
	
	void refreshList() {
		Cursor mGroupsCursor = generateGroupCursor();
		mGroupIdColumnIndex = mGroupsCursor.getColumnIndexOrThrow(CategoryTableMetaData._ID);
		ExpandableListAdapter mAdapter = new MyExpandableListAdapter(mGroupsCursor, this,
				R.layout.group_2row, R.layout.list2columnrowcategoryrep,
				new String[] { CategoryTableMetaData.NAME, TransactionsTableMetaData.AMOUNT },
				new int[] { R.id.row2_name, R.id.row2_value },
				new String[] { CategoryTableMetaData.NAME, TransactionsTableMetaData.AMOUNT },
				new int[] { R.id.l2column1, R.id.l2column2 });

		listView = new ExpandableListView(this);
		listView.setAdapter(mAdapter);
		//listView.setGroupIndicator(getResources().getDrawable(R.drawable.expander_group));
		listView.setScrollingCacheEnabled(true);
		listView.setCacheColorHint(00000000);
		listView.setBackgroundColor(getResources().getColor(R.color.White));
		
		listView.setOnChildClickListener(new OnChildClickListener() {
			
			@Override
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {
				Intent intent = new Intent(SubCategoryReport.this, TransactionList.class);
				intent.setAction(Constants.ActionViewTransactionsFromReport);
				Bundle bundle = new Bundle();
				bundle.putString(Constants.paramFromDate, Tools.DateToDBString(startDate));
				bundle.putString(Constants.paramToDate, Tools.DateToDBString(endDate));				
				bundle.putLong(Constants.paramCategory, id);				
				bundle.putInt(Constants.reportType, reportType);
				intent.putExtras(bundle);
				startActivityForResult(intent, Constants.RequestNONE);
				return false;
			}
		});
				
		
		RelativeLayout myLayout = (RelativeLayout) findViewById(R.id.repLayList);

		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.MATCH_PARENT);
		layoutParams.setMargins(Math.round(getResources().getDimension(R.dimen.combined_list_label_left_margin) / getResources().getDisplayMetrics().density), 0,
				Math.round(getResources().getDimension(R.dimen.combined_list_label_right_margin) / getResources().getDisplayMetrics().density), 0);

		listView.setLayoutParams(layoutParams);
		listView.setDivider(getResources().getDrawable(R.color.newThemeBlue));
		listView.setDividerHeight(Math.round(getResources().getDimension(R.dimen.main_round_button_side) / getResources().getDisplayMetrics().density));

		myLayout.addView(listView);
		refreshTitle();
	}

	// extending SimpleCursorTreeAdapter
	public class MyExpandableListAdapter extends SimpleCursorTreeAdapter {

		public MyExpandableListAdapter(Cursor cursor, Context context,
				int groupLayout, int childLayout, String[] groupFrom,
				int[] groupTo, String[] childrenFrom, int[] childrenTo) {
			super(context, cursor, groupLayout, groupFrom, groupTo, childLayout, childrenFrom, childrenTo);
		}

		// returns cursor with subitems for given group cursor
		@Override
		protected Cursor getChildrenCursor(Cursor groupCursor) {
			return generateChildCursor(groupCursor.getLong(mGroupIdColumnIndex));
		}

		// I needed to process click on click of the button on child item
		public View getChildView(final int groupPosition,
				final int childPosition, boolean isLastChild, View convertView,
				ViewGroup parent) {
			return super.getChildView(groupPosition, childPosition,
					isLastChild, convertView, parent);
		}

		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			return super.getGroupView(groupPosition, isExpanded, convertView, parent);
			/*View ind = v.findViewById(R.id.explist_indicator2);
			if (ind != null) {
				ImageView indicator = (ImageView) ind;
				if (getChildrenCount(groupPosition) == 0) {
					indicator.setVisibility(View.INVISIBLE);
				} else {
					indicator.setVisibility(View.VISIBLE);
					int stateSetIndex = (isExpanded ? 1 : 0);
					Drawable drawable = indicator.getDrawable();
					Log.i("state", String.valueOf(GROUP_STATE_SETS[stateSetIndex]));
					drawable.setState(GROUP_STATE_SETS[stateSetIndex]);
				}
			}*/
		}

	}

	private Cursor generateGroupCursor() {
		mainSQL = " Select ca2." + CategoryTableMetaData._ID + ", ca2." + CategoryTableMetaData.NAME + ", " +
				" sum(case when tr." + TransactionsTableMetaData.CURRENCYID + " = " + Constants.defaultCurrency + 
				" then " + TransactionsTableMetaData.TRANSTYPE + " * tr." + TransactionsTableMetaData.AMOUNT+
				" else " + TransactionsTableMetaData.TRANSTYPE + " * tr." + TransactionsTableMetaData.AMOUNT + " * (select " +
				VRatesToDefaultViewMetaData.VALUE + " from " + VRatesToDefaultViewMetaData.VIEW_NAME + 
				" where " + VRatesToDefaultViewMetaData.CURRENCY_ID + " = tr." + 
				TransactionsTableMetaData.CURRENCYID + " and tr." + TransactionsTableMetaData.TRANSDATE + 
				" between " + VRatesToDefaultViewMetaData.RATE_DATE + " and " + VRatesToDefaultViewMetaData.NEXT_RATE_DATE + ") end) " + 
				TransactionsTableMetaData.AMOUNT + 
				" From " + TransactionsTableMetaData.TABLE_NAME + " tr " +
				" Join " + CategoryTableMetaData.TABLE_NAME + " ca On tr." + TransactionsTableMetaData.CATEGORYID 
				+ " = ca." + CategoryTableMetaData._ID + 
				" join " + CategoryTableMetaData.TABLE_NAME + " ca2 on ca2." + CategoryTableMetaData._ID + 
				" = ca." + CategoryTableMetaData.MAINID + 
				" join " + CurrencyTableMetaData.TABLE_NAME + " cy on cy." + CurrencyTableMetaData._ID + 
				" = tr." + TransactionsTableMetaData.CURRENCYID;
		/*if (!AccountList.showDisabled)
			mainSQL += " join " + AccountTableMetaData.TABLE_NAME + " ac on ac." + AccountTableMetaData._ID +
				" = tr." + TransactionsTableMetaData.ACCOUNTID + " and ac." + AccountTableMetaData.STATUS + " = 1 ";*/
		mainSQL += "  where tr." + TransactionsTableMetaData.TRANSDATE + " >= '" + Tools.DateToDBString(startDate) + "' and tr." + 
				TransactionsTableMetaData.TRANSDATE + " <= '" + Tools.DateToDBString(endDate) + "' ";
		if (reportType == Constants.TransFTransaction.Expence.index()) {
			mainSQL += " and tr." + TransactionsTableMetaData.TRANSTYPE + " = " + String.valueOf(Constants.TransactionTypeExpence);
		}
		else if (reportType == Constants.TransFTransaction.Income.index()) {
			mainSQL += " and tr." + TransactionsTableMetaData.TRANSTYPE + " = " + String.valueOf(Constants.TransactionTypeIncome);
		} 
		mainSQL += " group by ca2." + CategoryTableMetaData._ID + ", ca2." + CategoryTableMetaData.NAME +
				" order by ca2." + CategoryTableMetaData.NAME;
		return DBTools.createCursor(SubCategoryReport.this, mainSQL);
	}

	private Cursor generateChildCursor(long mainCategoryID) {
		mainSQL = " Select ca." + CategoryTableMetaData._ID + ", ca." + CategoryTableMetaData.NAME + ", " +
				" sum(case when tr." + TransactionsTableMetaData.CURRENCYID + " = " + Constants.defaultCurrency + 
				" then " + TransactionsTableMetaData.TRANSTYPE + " * tr." + TransactionsTableMetaData.AMOUNT +
				" else " + TransactionsTableMetaData.TRANSTYPE + " * tr." + TransactionsTableMetaData.AMOUNT + " * (select " +
				VRatesToDefaultViewMetaData.VALUE + " from " + VRatesToDefaultViewMetaData.VIEW_NAME + 
				" where " + VRatesToDefaultViewMetaData.CURRENCY_ID + " = tr." + 
				TransactionsTableMetaData.CURRENCYID + " and tr." + TransactionsTableMetaData.TRANSDATE + 
				" between " + VRatesToDefaultViewMetaData.RATE_DATE + " and " + VRatesToDefaultViewMetaData.NEXT_RATE_DATE + ") end) " + 
				TransactionsTableMetaData.AMOUNT + 
				" From " + TransactionsTableMetaData.TABLE_NAME + " tr " +
				" Join " + CategoryTableMetaData.TABLE_NAME + " ca On tr." + TransactionsTableMetaData.CATEGORYID 
				+ " = ca." + CategoryTableMetaData._ID + 
				" join " + CurrencyTableMetaData.TABLE_NAME + " cy on cy." + CurrencyTableMetaData._ID + 
				" = tr." + TransactionsTableMetaData.CURRENCYID;
		/*if (!AccountList.showDisabled)
			mainSQL += " join " + AccountTableMetaData.TABLE_NAME + " ac on ac." + AccountTableMetaData._ID +
				" = tr." + TransactionsTableMetaData.ACCOUNTID + " and ac." + AccountTableMetaData.STATUS + " = 1 ";*/
		mainSQL += " where tr." + TransactionsTableMetaData.TRANSDATE + " >= '" + Tools.DateToDBString(startDate) + "' and tr." + 
				TransactionsTableMetaData.TRANSDATE + " <= '" + Tools.DateToDBString(endDate) + "' ";
		mainSQL += " and ca." + CategoryTableMetaData.MAINID + " = '" + String.valueOf(mainCategoryID) + "' ";
		if (reportType == Constants.TransFTransaction.Expence.index()) {
			mainSQL += " and tr." + TransactionsTableMetaData.TRANSTYPE + " = " + String.valueOf(Constants.TransactionTypeExpence);
		}
		else if (reportType == Constants.TransFTransaction.Income.index()) {
			mainSQL += " and tr." + TransactionsTableMetaData.TRANSTYPE + " = " + String.valueOf(Constants.TransactionTypeIncome);
		} 
		mainSQL += " group by ca." + CategoryTableMetaData._ID + ", ca." + CategoryTableMetaData.NAME +
				" order by ca." + CategoryTableMetaData.NAME;
		return DBTools.createCursor(SubCategoryReport.this, mainSQL);
	}
	
	public void myClickHandler(View target) {
		if (target.getTag() == btMenuExpenseTag) {
			reportType = Constants.TransFTransaction.Expence.index();
			hideMenu();
			refreshList();
		} else if (target.getTag() == btMenuIncomeTag) {
			reportType = Constants.TransFTransaction.Income.index();
			hideMenu();
			refreshList();
		} else if (target.getTag() == btMenuSummaryTag) {
			reportType = Constants.TransFTransaction.All.index();
			hideMenu();
			refreshList();
		} else
			switch (target.getId()) {
				case R.id.repImgIntLeft:
					if (currentDateInterval > 0) {
						currentDateInterval--;
						int hideDateArrow = (currentDateInterval != Constants.ReportTimeInterval.Custom.index()) ? View.VISIBLE : View.INVISIBLE;
						findViewById(R.id.repImgDateLeft).setVisibility(hideDateArrow);
						findViewById(R.id.repImgDateRight).setVisibility(hideDateArrow);
						btIntervaltype.setText(getResources().getTextArray(R.array.ReportTimeInterval)[currentDateInterval].toString());
						recreateDateInterval(0);
						refreshList();
					}
					break;
				case R.id.repImgIntRight:
					if (currentDateInterval < Constants.ReportTimeInterval.values().length - 1) {
						currentDateInterval++;
						int hideDateArrow = (currentDateInterval != Constants.ReportTimeInterval.Custom.index()) ? View.VISIBLE : View.INVISIBLE;
						findViewById(R.id.repImgDateLeft).setVisibility(hideDateArrow);
						findViewById(R.id.repImgDateRight).setVisibility(hideDateArrow);
						btIntervaltype.setText(getResources().getTextArray(R.array.ReportTimeInterval)[currentDateInterval].toString());
						recreateDateInterval(0);
						refreshList();
					}
					break;
				case R.id.repImgDateLeft:
					if (currentDateInterval != Constants.ReportTimeInterval.Custom.index()) {
						startDate = addPeriod(false);
						endDate = getEndDate();
						refreshDateButtonText();
						refreshList();
					}
					break;
				case R.id.repImgDateRight:
					if (currentDateInterval != Constants.ReportTimeInterval.Custom.index()) {
						if (Tools.compareDates(addPeriod(true), Tools.getCurrentDate()) <= 0) {
							startDate = addPeriod(true);
							endDate = getEndDate();
							refreshDateButtonText();
							refreshList();
						}
					}
					break;
				case R.id.repBtDate:
					if (currentDateInterval == Constants.ReportTimeInterval.Daily.index()) {
						showDialog(dateIntervalDailyDialogID);
					} else if (currentDateInterval == Constants.ReportTimeInterval.Weekly.index()) {
						showDialog(dateIntervalWeeklyDialogID);
					} else if (currentDateInterval == Constants.ReportTimeInterval.Monthly.index()) {
						showDialog(dateIntervalMonthlyDialogID);
					} else if (currentDateInterval == Constants.ReportTimeInterval.Yearly.index()) {
						showDialog(dateIntervalYearlyDialogID);
					} else if (currentDateInterval == Constants.ReportTimeInterval.Custom.index()) {
						showDialog(dateIntervalCustomDialogID);
					}
					break;
				case R.id.dmDateFrom:
					showDialog(fromDateDialogID);
					break;
				case R.id.dmDateTo:
					showDialog(toDateDialogID);
					break;
			}

	}

	void hideMenu() {
		this.menu.setVisibility(View.INVISIBLE);
		scrollView.smoothScrollTo(this.menu.getMeasuredWidth(), 0);
		menuOut = false;
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		final Button dpStartDate;
		final Button dpEndDate;
		switch (id) {
			case dateIntervalDailyDialogID:
				return new DatePickerDialog(this, new OnDateSetListener() {
					@Override
					public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
						startDate = new Date(year - 1900, monthOfYear, dayOfMonth);
						endDate = startDate;
						refreshList();
						refreshDateButtonText();
					}
				}, startDate.getYear() + 1900, startDate.getMonth(), startDate.getDate());
			case dateIntervalWeeklyDialogID:
				return new DatePickerDialog(this, new OnDateSetListener() {
					@Override
					public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
						startDate = Tools.truncDate(new Date(year - 1900, monthOfYear, dayOfMonth), Constants.DateTruncTypes.dateTruncWeek);
						endDate = getEndDate();
						refreshList();
						refreshDateButtonText();
					}
				}, startDate.getYear() + 1900, startDate.getMonth(), startDate.getDate());
			case dateIntervalMonthlyDialogID:
				Command cmd = new Command() {
					@Override
					public void execute() {
						startDate = new Date(spinnerYear.getSelectedItemPosition() + minTransDate - 1900,
								spinnerMonth.getSelectedItemPosition(), 1);
						endDate = getEndDate();
						refreshList();
						refreshDateButtonText();
					}
				};
				LayoutInflater li = LayoutInflater.from(this);
				View view = li.inflate(R.layout.yearmonthdialog, null);

				spinnerYear = (Spinner) view.findViewById(R.id.dmSpYear);
				spinnerMonth = (Spinner) view.findViewById(R.id.dmSpMonth);

				ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, R.layout.simple_spinner_item_blue, years);
				spinnerArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_blue);
				spinnerYear.setAdapter(spinnerArrayAdapter);

				spinnerArrayAdapter = new ArrayAdapter<String>(
						this, R.layout.simple_spinner_item_blue, getResources().getStringArray(R.array.Months));
				spinnerArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_blue);
				spinnerMonth.setAdapter(spinnerArrayAdapter);

				spinnerYear.setSelection(startDate.getYear() - minTransDate + 1900);
				spinnerMonth.setSelection(startDate.getMonth());

				AlertDialog viewDialog = DialogTools.CustomDialog(SubCategoryReport.this, cmd, R.string.msgSelectPeriod, view, R.drawable.ic_menu_edit);
				viewDialog.show();
				break;
			case dateIntervalYearlyDialogID:
				cmd = new Command() {
					@Override
					public void execute() {
						startDate = new Date(spinnerYear.getSelectedItemPosition() + minTransDate - 1900, 0, 1);
						endDate = getEndDate();
						refreshList();
						refreshDateButtonText();
					}
				};
				li = LayoutInflater.from(this);
				view = li.inflate(R.layout.radiolistdialog, null);

				spinnerYear = (Spinner) view.findViewById(R.id.rdSpinner);

				spinnerArrayAdapter = new ArrayAdapter<String>(this, R.layout.simple_spinner_item_blue, years);
				spinnerArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_blue);
				spinnerYear.setAdapter(spinnerArrayAdapter);

				spinnerYear.setSelection(startDate.getYear() - minTransDate + 1900);

				AlertDialog listDialog = DialogTools.RadioListDialog(SubCategoryReport.this, cmd, R.string.msgSelectPeriod, view);
				listDialog.show();
				break;
			case dateIntervalCustomDialogID:
				int minYear = TransactionSrv.getMinTransactionYear(this);
				Calendar minCal = Calendar.getInstance();
				minCal.set(minYear, 1, 1);

				li = LayoutInflater.from(this);
				dialogView = li.inflate(R.layout.datedialog_dual, null);

				dpStartDate = (Button) dialogView.findViewById(R.id.dmDateFrom);
				dpStartDate.setText(Tools.DateToString(startDate, Constants.DateFormatUser));
				dpEndDate = (Button) dialogView.findViewById(R.id.dmDateTo);
				dpEndDate.setText(Tools.DateToString(endDate, Constants.DateFormatUser));

				final AlertDialog viewDialogCustom = DialogTools.CustomDialog(this, dialogView);

				final Command cmdPeriod = new Command() {
					@Override
					public void execute() {
						if (startDate.compareTo(endDate) < 0) {
							refreshList();
							refreshDateButtonText();
							viewDialogCustom.dismiss();
						}
						else
							DialogTools.toastDialog(SubCategoryReport.this, R.string.msgFromDateIsLessThanTo, Toast.LENGTH_LONG);
					}
				};

				Command cmdCancel = new Command() {
					@Override
					public void execute() {
						viewDialogCustom.dismiss();
					}
				};

				DialogTools.setButtonActions(dialogView, R.id.dmBtOK, R.id.dmBtCancel, cmdPeriod, cmdCancel);

				viewDialogCustom.show();
				break;
			case fromDateDialogID:
				return new DatePickerDialog(this, new OnDateSetListener() {
					@Override
					public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
						Calendar calendar = Calendar.getInstance();
						calendar.set(year, monthOfYear, dayOfMonth);
						startDate = calendar.getTime();
						setDialogDate(R.id.dmDateFrom, startDate);
					}
				}, startDate.getYear() + 1900, startDate.getMonth(), startDate.getDate());
			case toDateDialogID:
				return new DatePickerDialog(this, new OnDateSetListener() {
					@Override
					public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
						Calendar calendar = Calendar.getInstance();
						calendar.set(year, monthOfYear, dayOfMonth);
						endDate = calendar.getTime();
						setDialogDate(R.id.dmDateTo, endDate);
					}
				}, endDate.getYear() + 1900, endDate.getMonth(), endDate.getDate());
		}
		return null;
	}

	void recreateDateInterval(int addPeriod) {
		//addPeriod = 1-add, 2-subtract, 0-go to current date
		switch (addPeriod) {
		case 0:
			startDate = getCurrentDate();
			break;
		case 1:
			startDate = addPeriod(true);
			break;
		case 2:
			startDate = addPeriod(false);
			break;
		default:
			break;
		}
		endDate = getEndDate();
		refreshDateButtonText();
	}
	
	void refreshDateButtonText() {
		if (currentDateInterval == Constants.ReportTimeInterval.Daily.index())
			btDate.setText(Tools.DateToString(startDate, Constants.DateFormatUser));
		else if (currentDateInterval == Constants.ReportTimeInterval.Weekly.index() || currentDateInterval == Constants.ReportTimeInterval.Custom.index())
			btDate.setText(Tools.DateToString(startDate, Constants.DateFormatUser) + " - " + Tools.DateToString(endDate, Constants.DateFormatUser));
		else if (currentDateInterval == Constants.ReportTimeInterval.Monthly.index())
			btDate.setText(getBaseContext().getResources().getStringArray(R.array.Months)[startDate.getMonth()] + ", " +
					Tools.DateToString(startDate, "yyyy"));
		else if (currentDateInterval == Constants.ReportTimeInterval.Yearly.index())
			btDate.setText(Tools.DateToString(startDate, "yyyy"));
	}
	
	Date addPeriod(boolean add) {
		int value = add ? 1 : -1;
		if (currentDateInterval == Constants.ReportTimeInterval.Weekly.index())
			return Tools.AddDays(startDate, value * 7);
		else if (currentDateInterval == Constants.ReportTimeInterval.Monthly.index())
			return Tools.AddMonth(startDate, value);
		else if (currentDateInterval == Constants.ReportTimeInterval.Yearly.index())
			return Tools.AddMonth(startDate, value * 12);
		else
			return Tools.AddDays(startDate, value);
	}
	
	Date getCurrentDate() {
		if (currentDateInterval == Constants.ReportTimeInterval.Weekly.index())
			return Tools.truncDate(Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncWeek);
		else if (currentDateInterval == Constants.ReportTimeInterval.Monthly.index())
			return Tools.truncDate(Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncMonth);
		else if (currentDateInterval == Constants.ReportTimeInterval.Yearly.index())
			return Tools.truncDate(Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncYear);
		else
			return Tools.getCurrentDate();		
	}
	
	Date getEndDate() {
		if (currentDateInterval == Constants.ReportTimeInterval.Weekly.index())
			return Tools.AddDays(startDate, 6);
		else if (currentDateInterval == Constants.ReportTimeInterval.Monthly.index())
			return Tools.AddDays(Tools.AddMonth(startDate, 1), -1);
		else if (currentDateInterval == Constants.ReportTimeInterval.Yearly.index())
			return Tools.AddDays(Tools.AddMonth(startDate, 12), -1);
		else
			return startDate;				
	}

	void setDialogDate(int buttonID, Date date) {
		((Button) dialogView.findViewById(buttonID)).setText(Tools.DateToString(date, Constants.DateFormatUser));
	}

	//menu el ile geriye cekmek ucun
	private OnSwipeTouchListener mySwipeListener = new OnSwipeTouchListener() {
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


	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		if (menuOut) {
			this.menu.setVisibility(View.INVISIBLE);
			scrollView.smoothScrollTo(this.menu.getMeasuredWidth(), 0);
			menuOut = false;
		}
		else {
			this.menu.setVisibility(View.VISIBLE);
			scrollView.smoothScrollTo(0, 0);
			menuOut = true;
		}
		return super.onMenuOpened(featureId, menu);
	}

	/**
	 * Helper for examples with a HSV that should be scrolled by a menu View's width.
	 */
	static class ClickListenerForScrolling implements View.OnClickListener {
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
	static class SizeCallbackForMenu implements MyHorizontalScrollView.SizeCallback {
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
}
