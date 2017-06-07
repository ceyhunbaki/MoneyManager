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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jgmoneymanager.SlidingMenu.MyHorizontalScrollView;
import com.jgmoneymanager.SlidingMenu.OnSwipeTouchListener;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.AccountTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CategoryTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.main.R;
import com.jgmoneymanager.main.TransactionList;
import com.jgmoneymanager.services.ReportSrv;
import com.jgmoneymanager.services.TransactionSrv;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.LocalTools;
import com.jgmoneymanager.entity.ReportArray;
import com.jgmoneymanager.entity.ReportArrayItem;
import com.jgmoneymanager.entity.ReportItemAdapter;
import com.jgmoneymanager.tools.Tools;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class ListReport extends MyActivity{
	private MyHorizontalScrollView scrollView;
	private static View menu;
	private View app;
	private ImageView btnSlide;
	static boolean menuOut = false;
	
	int reportType = Constants.TransFTransaction.All.index();	
	
	final int dateIntervalDailyDialogID = 1;
	final int dateIntervalWeeklyDialogID = 2;
	final int dateIntervalMonthlyDialogID = 3;
	final int dateIntervalYearlyDialogID = 4;
	final int dateIntervalCustomDialogID = 5;
	final int fromDateDialogID = 6;
	final int toDateDialogID = 7;

	private String btMenuExpenseTag = "btMenuExpenseTag";
	private String btMenuIncomeTag = "btMenuIncomeTag";
	private String btMenuSummaryTag = "btMenuSummaryTag";
	private String btMenuTransferTag = "btMenuTransferTag";

	boolean clearDates = true;
	boolean includeTransfers = true;
	int currentDateInterval = Constants.ReportTimeInterval.Monthly.index();
	Date startDate, endDate;
	int minTransDate;
	String years[] = new String[] {};
		
	Button btIntervaltype;
	Button btDate;
	TextView tvTitle;
	
	Spinner spinnerYear;
	Spinner spinnerMonth;
	
	ReportArray repArray;
	ArrayList<ReportArrayItem> list;
	ListView listView;

	View dialogView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			clearDates = Tools.getBooleanFromBundle0(savedInstanceState, "clearDates");
			startDate =  Tools.getDateFromBundle(savedInstanceState, "startDate");
			endDate =  Tools.getDateFromBundle(savedInstanceState, "endDate");
			currentDateInterval = Tools.getIntegerFromBundle0(savedInstanceState, "currentDateInterval");
			reportType = Tools.getIntegerFromBundle0(savedInstanceState, "reportType");
			includeTransfers = Tools.getBooleanFromBundle(savedInstanceState, "includeTransfers");
		}

		initializeViews();
		
		minTransDate = TransactionSrv.getMinTransactionYear(ListReport.this);
		int currentYear = Tools.getCurrentDate().getYear() + 1900;
		for (int i=minTransDate; i<=currentYear; i++)
			years = Tools.addElement(years, String.valueOf(i));

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
		boolean viewAccountReports = false;
		try {
			viewAccountReports = getIntent().getAction().equals(Constants.ActionViewAccountReport);
		}
		catch (Exception e) {
		}
		if (viewAccountReports)
			LocalTools.addButtonToMenuList(this, includeTransfers ? R.string.menuExcludeTransfers : R.string.menuIncludeTransfers, btMenuTransferTag);
		//app.setOnTouchListener(mySwipeListener);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Tools.putToBundle(outState, "clearDates", false);
		Tools.putToBundle(outState, "startDate", Tools.DateToString(startDate));
		Tools.putToBundle(outState, "endDate", Tools.DateToString(endDate));
		Tools.putToBundle(outState, "currentDateInterval", currentDateInterval);
		Tools.putToBundle(outState, "reportType", reportType);
		Tools.putToBundle(outState, "includeTransfers", includeTransfers);
		super.onSaveInstanceState(outState);
	}
	
	void reloadScreen() {
		btIntervaltype = (Button) findViewById(R.id.repBtInterval);
		btIntervaltype.setText(getResources().getTextArray(R.array.ReportTimeInterval)[currentDateInterval].toString());
		btDate = (Button) findViewById(R.id.repBtDate);
		tvTitle = (TextView) findViewById(R.id.tvATTitle);
	}
	
	void refreshTitle() {
		String titleString = ReportList.getTitleString(ListReport.this, reportType);
		
		if (getIntent().getAction().equals(Constants.ActionViewAccountReport))
			tvTitle.setText(getResources().getString(R.string.accounts) + " - " + titleString);
		else if (getIntent().getAction().equals(Constants.ActionViewCategoryReport))
			tvTitle.setText(getResources().getString(R.string.categories) + " - " + titleString);
	}
	
	void refreshList() {
		generateArray(currentDateInterval);
		listView = new ListView(this);
		listView.setAdapter(new ReportItemAdapter(this, R.layout.list2columnreportrow, list));
		listView.setScrollingCacheEnabled(true);
		listView.setCacheColorHint(00000000);
		listView.setBackgroundColor(getResources().getColor(R.color.White));
		listView.setDivider(getResources().getDrawable(R.color.newThemeBlue));
		listView.setDividerHeight(Math.round(getResources().getDimension(R.dimen.main_round_button_side) / getResources().getDisplayMetrics().density));
		
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				Intent intent = new Intent(ListReport.this, TransactionList.class);
				intent.setAction(Constants.ActionViewTransactionsFromReport);
				Bundle bundle = new Bundle();
				bundle.putString(Constants.paramFromDate, Tools.DateToDBString(startDate));
				bundle.putString(Constants.paramToDate, Tools.DateToDBString(endDate));
				
				if (getIntent().getAction().equals(Constants.ActionViewAccountReport))
					bundle.putLong(Constants.paramAccountID, repArray.getItem(arg2+1).getItemID());
				else if (getIntent().getAction().equals(Constants.ActionViewCategoryReport))
					bundle.putLong(Constants.paramCategory, repArray.getItem(arg2+1).getItemID());
				
				bundle.putInt(Constants.reportType, reportType);
				intent.putExtras(bundle);
				startActivityForResult(intent, Constants.RequestNONE);
			}
		});
		
		RelativeLayout myLayout = (RelativeLayout) findViewById(R.id.repLayList);

		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.MATCH_PARENT);
		layoutParams.setMargins(Math.round(getResources().getDimension(R.dimen.combined_list_label_left_margin) / getResources().getDisplayMetrics().density), 0,
				Math.round(getResources().getDimension(R.dimen.combined_list_label_right_margin) / getResources().getDisplayMetrics().density), 0);

		listView.setLayoutParams(layoutParams);

		myLayout.addView(listView);
		refreshTitle();
	}

	public class MyListAdapter extends SimpleCursorAdapter {

		Context context;

		public MyListAdapter(Cursor cursor, Context context, int rowId,
				String[] from, int[] to) {
			super(context, rowId, cursor, from, to);
			this.context = context;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = super.getView(position, convertView, parent);
			if (view == null) {
				LayoutInflater inflater = LayoutInflater.from(context);
				view = inflater.inflate(R.layout.list2columnrow, null);
			}
			if (position % 2 == 0)
				view.setBackgroundColor(getResources().getColor(
						R.color.AntiqueWhite));
			else
				view.setBackgroundColor(getResources().getColor(R.color.White));
			return view;
		}
	}
		
	void generateArray(int dateIntervalStart) {
		String sql = "";
		if (getIntent().getAction().equals(Constants.ActionViewAccountReport)) {
			sql = ReportSrv.generateAccountSQL(reportType, startDate, endDate, includeTransfers);
			repArray = ReportSrv.generateArray(ListReport.this, reportType, sql, true, false,
					AccountTableMetaData._ID, AccountTableMetaData.NAME);
		}
		else if (getIntent().getAction().equals(Constants.ActionViewCategoryReport)) {
			sql = ReportSrv.generateCategorySQL(reportType, startDate, endDate);
			repArray = ReportSrv.generateArray(ListReport.this, reportType, sql, true, true,
					CategoryTableMetaData._ID, CategoryTableMetaData.NAME);
		}
		
		list = new ArrayList<ReportArrayItem>(repArray.getItemCount());
		
		for (int i = 1; i < repArray.getItemCount(); i++) {
			list.add(repArray.getItem(i));
		}
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
		} else if (target.getTag() == btMenuTransferTag) {
			includeTransfers = !includeTransfers;
			((Button)target).setText(includeTransfers ? R.string.menuExcludeTransfers : R.string.menuIncludeTransfers);
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
						startDate = ReportSrv.addPeriod(false, currentDateInterval, startDate);
						endDate = ReportSrv.getEndDate(currentDateInterval, startDate);
						refreshDateButtonText();
						refreshList();
					}
					break;
				case R.id.repImgDateRight:
					if (currentDateInterval != Constants.ReportTimeInterval.Custom.index()) {
						if (Tools.compareDates(ReportSrv.addPeriod(true, currentDateInterval, startDate), Tools.getCurrentDate()) <= 0) {
							startDate = ReportSrv.addPeriod(true, currentDateInterval, startDate);
							endDate = ReportSrv.getEndDate(currentDateInterval, startDate);
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
						endDate = ReportSrv.getEndDate(currentDateInterval, startDate);
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
						endDate = ReportSrv.getEndDate(currentDateInterval, startDate);
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

				AlertDialog viewDialog = DialogTools.CustomDialog(ListReport.this, cmd, R.string.msgSelectPeriod, view, R.drawable.ic_menu_edit);
				viewDialog.show();
				break;
			case dateIntervalYearlyDialogID:
				cmd = new Command() {
					@Override
					public void execute() {
						startDate = new Date(spinnerYear.getSelectedItemPosition() + minTransDate - 1900, 0, 1);
						endDate = ReportSrv.getEndDate(currentDateInterval, startDate);
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

				AlertDialog listDialog = DialogTools.RadioListDialog(ListReport.this, cmd, R.string.msgSelectPeriod, view);
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
						} else
							DialogTools.toastDialog(ListReport.this, R.string.msgFromDateIsLessThanTo, Toast.LENGTH_LONG);
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
			startDate = ReportSrv.getCurrentDate(currentDateInterval);
			break;
		case 1:
			startDate = ReportSrv.addPeriod(true, currentDateInterval, startDate);
			break;
		case 2:
			startDate = ReportSrv.addPeriod(false, currentDateInterval, startDate);
			break;
		default:
			break;
		}
		endDate = ReportSrv.getEndDate(currentDateInterval, startDate);
		refreshDateButtonText();
	}
	
	void refreshDateButtonText() {
		btDate.setText(ReportSrv.getDateButtonText(ListReport.this, currentDateInterval, startDate, endDate, true));
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
