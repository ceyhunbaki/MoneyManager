package com.jgmoneymanager.reports;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jgmoneymanager.SlidingMenu.MyHorizontalScrollView;
import com.jgmoneymanager.SlidingMenu.OnSwipeTouchListener;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VRatesToDefaultViewMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransactionsTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CategoryTableMetaData;
import com.jgmoneymanager.dialogs.CheckBoxDialog;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.dialogs.LocalDialog;
import com.jgmoneymanager.entity.CheckBoxItem;
import com.jgmoneymanager.entity.Margins;
import com.jgmoneymanager.entity.MyAbstractDemoChartctivity;
import com.jgmoneymanager.entity.ReportArray;
import com.jgmoneymanager.main.R;
import com.jgmoneymanager.services.CategorySrv;
import com.jgmoneymanager.services.ReportSrv;
import com.jgmoneymanager.services.TransactionSrv;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.LocalTools;
import com.jgmoneymanager.tools.Tools;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart.Type;
import org.achartengine.model.CategorySeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer.Orientation;
import org.achartengine.util.MathHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class MontlyCategoryReport extends MyAbstractDemoChartctivity {
	private MyHorizontalScrollView scrollView;
	private static View menu;
	private View app;
	private ImageView btnSlide;

	private final int dateIntervalCustomDialogID = 1;
	private final int fromDateDialogID = 2;
	private final int toDateDialogID = 3;

	String btMenuCategoriesTag = "btMenuCategoriesTag";

	private int currentDateInterval = Constants.ReportTimeIntervalBudget.Last12Month.index();
	private Date startDate, endDate;

	private Button btIntervaltype;

	private ReportArray repArray;

	private View dialogView;

	private XYMultipleSeriesRenderer renderer;
	private XYMultipleSeriesDataset dataset;
	private GraphicalView chartView;

	private String monthAlias = "monthAlias";

	ArrayList<CheckBoxItem> categoriesList;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initializeViews();

		((TextView) findViewById(R.id.tvATTitle)).setText(R.string.repExpensesCompareTitle);

		if (savedInstanceState != null) {
			startDate =  Tools.getDateFromBundle(savedInstanceState, "startDate");
			endDate =  Tools.getDateFromBundle(savedInstanceState, "endDate");
			currentDateInterval = Tools.getIntegerFromBundle0(savedInstanceState, "currentDateInterval");
		}
		reloadScreen();
		setPeriods();
	}

	private void initializeViews() {
		LayoutInflater inflater = LayoutInflater.from(this);
		scrollView = (MyHorizontalScrollView) inflater.inflate(R.layout.horz_scroll_with_list_menu, null);

		setContentView(scrollView);

		menu = inflater.inflate(R.layout.horz_scroll_menu, null);
		app = inflater.inflate(R.layout.piechartreport, null);
		ViewGroup tabBar = (ViewGroup) app.findViewById(R.id.relATTop);

		btnSlide = (ImageView) tabBar.findViewById(R.id.btATMenu);
		btnSlide.setOnClickListener(new ClickListenerForScrolling(scrollView, menu));

		final View[] children = new View[] { menu, app };

		// Scroll to app (view[1]) when layout finished.
		int scrollToViewIdx = 1;
		scrollView.initViews(children, scrollToViewIdx, new SizeCallbackForMenu(btnSlide));

		menu.setOnTouchListener(mySwipeListener);

		LocalTools.addButtonToMenuList(this, R.string.categories, btMenuCategoriesTag);

		findViewById(R.id.repImgDateLeft).setVisibility(View.GONE);
		findViewById(R.id.repImgDateRight).setVisibility(View.GONE);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		Tools.putToBundle(outState, "startDate", Tools.DateToString(startDate));
		Tools.putToBundle(outState, "endDate", Tools.DateToString(endDate));
		Tools.putToBundle(outState, "currentDateInterval", currentDateInterval);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		repaintChart();
	}

	private void reloadScreen() {
		btIntervaltype = (Button) findViewById(R.id.repBtInterval);
		btIntervaltype.setText(getResources().getTextArray(R.array.ReportTimeIntervalBudget)[currentDateInterval].toString());
	}

	private void refreshList() {
		generateArray();
		repaintChart();
	}

	private void repaintChart() {
		renderer = getBarRenderer();
		dataset = getBarDataset();
		checkParameters(dataset, renderer);
		
		LinearLayout layChart = (LinearLayout)findViewById(R.id.chart);
		if (chartView != null)
			layChart.removeView(chartView);
		chartView = ChartFactory.getBarChartView(getBaseContext(), dataset, renderer, Type.DEFAULT);		
		layChart.addView(chartView);
	}

	private void generateArray() {
		//String sql = ReportSrv.generateCategorySQL(reportType, startDate, endDate);
		String sql = "Select substr(" + TransactionsTableMetaData.TRANSDATE + ", 1, 6) " + monthAlias + ",\n" +
				" sum(case when tr." + TransactionsTableMetaData.CURRENCYID + " = " + Constants.defaultCurrency + " \n" +
				"\t\tthen tr." + TransactionsTableMetaData.AMOUNT + " \n" +
				"\t\telse tr." + TransactionsTableMetaData.AMOUNT + " " +
				"* (select " + VRatesToDefaultViewMetaData.VALUE + " from " + VRatesToDefaultViewMetaData.VIEW_NAME +
				" where " + VRatesToDefaultViewMetaData.CURRENCY_ID + " = tr." + TransactionsTableMetaData.CURRENCYID
				+ " and tr." + TransactionsTableMetaData.TRANSDATE + " between " + VRatesToDefaultViewMetaData.RATE_DATE + " and " + VRatesToDefaultViewMetaData.NEXT_RATE_DATE + ") \n" +
				"\tend) " + TransactionsTableMetaData.AMOUNT + " \n" +
				"From " + TransactionsTableMetaData.TABLE_NAME + " tr \n" +
				" Join " + CategoryTableMetaData.TABLE_NAME + " ca On tr." + TransactionsTableMetaData.CATEGORYID + " = ca." + CategoryTableMetaData._ID + " \n" +
				" join " + CategoryTableMetaData.TABLE_NAME + " ca2 on ca2." + CategoryTableMetaData._ID + " = ca." + CategoryTableMetaData.MAINID + " \n" +
				" where tr." + TransactionsTableMetaData.TRANSDATE + " between '" + Tools.DateToDBString(startDate) +
				"' and '" + Tools.DateToDBString(endDate) + "'  and " + TransactionsTableMetaData.TRANSTYPE + " =-1 ";
		if (categoriesList != null) {
			String categoryIDs = "";
			for (int i = 0; i < categoriesList.size(); i++) {
				CheckBoxItem category = categoriesList.get(i);
				if (category.isSelected()) {
					categoryIDs += category.getID() + ",";
				}
			}
			if (categoryIDs.length() > 0)
				sql += " and ca2." + CategoryTableMetaData._ID + " in(" + categoryIDs.substring(0, categoryIDs.length()-1) + ") ";
		}
		sql += " group by substr(" + TransactionsTableMetaData.TRANSDATE + ", 1, 6) order by 1";
		repArray = ReportSrv.generateArrayUniversal(MontlyCategoryReport.this, sql, monthAlias, monthAlias, TransactionsTableMetaData.AMOUNT,
				Tools.monthsBetween(startDate, endDate)+1);
	}

	private XYMultipleSeriesRenderer getBarRenderer() {
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
		TypedArray array = this.obtainStyledAttributes(R.style.Theme_ChartLegendTextSize, new int[] { android.R.attr.textSize });
		renderer.setAxisTitleTextSize(array.getDimensionPixelSize(0, 25));
		renderer.setChartTitleTextSize(array.getDimensionPixelSize(0, 25));
		renderer.setLabelsTextSize(array.getDimensionPixelSize(0, 25));
		renderer.setLegendTextSize(array.getDimensionPixelSize(0, 25));
		
		renderer.setXLabels(0);
		renderer.setXLabelsAngle(270);
		renderer.setXLabelsAlign(Align.RIGHT);
		renderer.setYLabels(10);
		renderer.setYLabelsPadding(10);
		renderer.setYLabelsAlign(Align.RIGHT);
	    renderer.setPanEnabled(true, true);
		renderer.setAxesColor(Color.GRAY);
		renderer.setLabelsColor(Color.LTGRAY);
		renderer.setZoomButtonsVisible(false);
		renderer.setOrientation(Orientation.HORIZONTAL);
		renderer.setBarSpacing(1);
	    double maxValue = 0;
		for (int i = 1; i < repArray.getItemCount(); i++) {
			renderer.addXTextLabel(i, repArray.getItem(i).getName());
			maxValue = Math.max(maxValue, repArray.getItem(i).getAmount());
		}
		renderer.addXTextLabel(repArray.getItemCount(), "");

		Margins margins = Tools.getMarginsFromStyle(this, R.style.BarChartMargins);
		renderer.setMargins(new int[] {margins.getTopMargin(), 
				String.valueOf(maxValue).length() * margins.getLeftMargin() + 10, margins.getBottomMargin(), 
				margins.getRightMargin()});
		array.recycle();
		
		renderer.setXAxisMin(0);
		renderer.setYAxisMin(0);
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
			renderer.setYAxisMax(maxValue + maxValue / (renderer.getYLabels()));
		else 
			renderer.setYAxisMax(maxValue + maxValue / (2 * renderer.getYLabels()));
		
		SimpleSeriesRenderer r = new SimpleSeriesRenderer();
		r.setColor(Color.GREEN);
		r.setDisplayChartValues(true);
		r.setShowLegendItem(true);
		array = this.obtainStyledAttributes(R.style.Theme_ChartValuesTextSize, new int[] { android.R.attr.textSize });
		r.setChartValuesTextSize(array.getDimensionPixelSize(0, 25));
		r.setChartValuesSpacing(3);
		renderer.addSeriesRenderer(r);
		return renderer;
	}

	private XYMultipleSeriesDataset getBarDataset() {
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		CategorySeries series = new CategorySeries("");
		for (int i = 1; i < repArray.getItemCount(); i++) {
			series.add(repArray.getItem(i).getAmount());
		}
		series.add(MathHelper.NULL_VALUE);
		dataset.addSeries(series.toXYSeries());
		return dataset;
	}

	private static void checkParameters(XYMultipleSeriesDataset dataset, XYMultipleSeriesRenderer renderer) {
		if (dataset == null || renderer == null || dataset.getSeriesCount() != renderer.getSeriesRendererCount()) {
			throw new IllegalArgumentException(
					"Dataset and renderer should be not null and should have the same number of series");
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if ((resultCode == RESULT_OK)&&(requestCode == Constants.RequestCategoryForReport))
			refreshList();
	}

	public void myClickHandler(View target) {
		if (target.getTag() == btMenuCategoriesTag) {
			showCategoryDialogWindow();
			if (menu.getVisibility() == View.VISIBLE) {
				menu.setVisibility(View.INVISIBLE);
				scrollView.smoothScrollTo(menu.getMeasuredWidth(), 0);
				menuOut = false;
			}
		}
		else {
			switch (target.getId()) {
				case R.id.repImgIntLeft:
					if (currentDateInterval > 0) {
						currentDateInterval--;
						btIntervaltype.setText(getResources().getTextArray(R.array.ReportTimeIntervalBudget)[currentDateInterval].toString());
						setPeriods();
					}
					break;
				case R.id.repImgIntRight:
					if (currentDateInterval < Constants.ReportTimeIntervalBudget.values().length - 1) {
						currentDateInterval++;
						btIntervaltype.setText(getResources().getTextArray(R.array.ReportTimeIntervalBudget)[currentDateInterval].toString());
						setPeriods();
					}
					break;
				case R.id.repBtDate:
					if (currentDateInterval == Constants.ReportTimeIntervalBudget.Custom.index())
						showDialog(dateIntervalCustomDialogID);
					break;
				case R.id.dmDateFrom:
					showDialog(fromDateDialogID);
					break;
				case R.id.dmDateTo:
					showDialog(toDateDialogID);
					break;
			}
		}
	}

	void showCategoryDialogWindow() {
		if (categoriesList == null)
			categoriesList = CategorySrv.generateMainCategoryIDs(this);

		Bundle bundle = new Bundle();
		bundle.putBoolean(Constants.dontRefreshValues, true);
		CheckBoxDialog.itemsList = categoriesList;
		bundle.putString(Constants.query, CategorySrv.mainCategoriesQueryForReports);
		bundle.putString(Constants.paramTitle, CategoryTableMetaData.NAME);
		bundle.putSerializable(Constants.paramValues, Tools.convertCheckBoxListToHashMap(categoriesList));
		Intent intent = new Intent(MontlyCategoryReport.this, CheckBoxDialog.class);
		intent.putExtras(bundle);
		startActivityForResult(intent, Constants.RequestCategoryForReport);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		final Button dpStartDate;
		final Button dpEndDate;
		switch (id) {
			case dateIntervalCustomDialogID:
				int minYear = TransactionSrv.getMinTransactionYear(this);
				Calendar minCal = Calendar.getInstance();
				minCal.set(minYear, 1, 1);

				LayoutInflater li = LayoutInflater.from(this);
				dialogView = li.inflate(R.layout.datedialog_dual, null);
				final Command cmdPeriod = new Command() {
					@Override
					public void execute() {
						if (startDate.compareTo(endDate) < 0) {
							setPeriods();
						} else
							DialogTools.toastDialog(MontlyCategoryReport.this, R.string.msgFromDateIsLessThanTo, Toast.LENGTH_LONG);
					}
				};
				AlertDialog dialog = LocalDialog.DualDateDialog(this, dialogView, startDate, endDate, cmdPeriod);
				dialog.show();
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

	private void setDialogDate(int buttonID, Date date) {
		((Button) dialogView.findViewById(buttonID)).setText(Tools.DateToString(date, Constants.DateFormatUser));
	}


	private void setPeriods() {
		final Button btPeriod = (Button) findViewById(R.id.repBtDate);
		if (currentDateInterval == Constants.ReportTimeIntervalBudget.ThisYear.index()) {
			startDate = Tools.truncDate(Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncYear);
			endDate = Tools.getCurrentDate();
		}
		else if (currentDateInterval == Constants.ReportTimeIntervalBudget.Last12Month.index()) {
			startDate = Tools.truncDate(Tools.AddMonth(Tools.getCurrentDate(), -12), Constants.DateTruncTypes.dateTruncMonth);
			endDate = Tools.AddDays(Tools.truncDate(Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncMonth), -1);
		}
		btPeriod.setText(Tools.DateToString(startDate, Constants.DateFormatUser) + " - " + Tools.DateToString(endDate, Constants.DateFormatUser));
		reloadScreen();
		refreshList();
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
