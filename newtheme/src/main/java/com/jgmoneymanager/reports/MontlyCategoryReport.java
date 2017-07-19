package com.jgmoneymanager.reports;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CategoryTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransactionsTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VRatesToDefaultViewMetaData;
import com.jgmoneymanager.dialogs.CheckBoxDialog;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.dialogs.LocalDialog;
import com.jgmoneymanager.entity.CheckBoxItem;
import com.jgmoneymanager.entity.Margins;
import com.jgmoneymanager.entity.ReportArray;
import com.jgmoneymanager.main.R;
import com.jgmoneymanager.services.CategorySrv;
import com.jgmoneymanager.services.ReportSrv;
import com.jgmoneymanager.services.TransactionSrv;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
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

import chart.AbstractDemoChart;

public class MontlyCategoryReport extends AbstractDemoChart {

	private final int dateIntervalCustomDialogID = 1;
	private final int fromDateDialogID = 2;
	private final int toDateDialogID = 3;

	private final int btCategoriesMenuID = Menu.FIRST;

	private int currentDateInterval = Constants.ReportTimeIntervalBudget.Last12Month.index();
	private Date startDate, endDate;

	private Button btIntervaltype;

	private ReportArray repArray;

	private View dialogView;

	private GraphicalView chartView;

	ArrayList<CheckBoxItem> categoriesList;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initializeViews();

		if (savedInstanceState != null) {
			startDate =  Tools.getDateFromBundle(savedInstanceState, "startDate");
			endDate =  Tools.getDateFromBundle(savedInstanceState, "endDate");
			currentDateInterval = Tools.getIntegerFromBundle0(savedInstanceState, "currentDateInterval");
		}
		reloadScreen();
		setPeriods();
	}

	private void initializeViews() {
		setContentView(R.layout.main_activity);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

		LinearLayout mainLayout = (LinearLayout) findViewById(R.id.content_main_layout);
		LayoutInflater inflater = (LayoutInflater)      this.getSystemService(LAYOUT_INFLATER_SERVICE);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		View child = inflater.inflate(R.layout.piechartreport, null);
		mainLayout.addView(child, params);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, btCategoriesMenuID, btCategoriesMenuID, R.string.categories);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == btCategoriesMenuID)
			showCategoryDialogWindow();
		return super.onOptionsItemSelected(item);
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
		XYMultipleSeriesRenderer renderer = getBarRenderer();
		XYMultipleSeriesDataset dataset = getBarDataset();
		checkParameters(dataset, renderer);
		
		LinearLayout layChart = (LinearLayout)findViewById(R.id.chart);
		if (chartView != null)
			layChart.removeView(chartView);
		chartView = ChartFactory.getBarChartView(getBaseContext(), dataset, renderer, Type.DEFAULT);		
		layChart.addView(chartView);
	}

	private void generateArray() {
		String monthAlias = "monthAlias";
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

	void showCategoryDialogWindow() {
		if (categoriesList == null)
			categoriesList = CategorySrv.generateMainCategoryIDs(this);

		Bundle bundle = new Bundle();
		bundle.putBoolean(Constants.dontRefreshValues, true);
		CheckBoxDialog.itemsList = categoriesList;
		bundle.putString(Constants.query, CategorySrv.mainCategoriesQueryForReports);
		bundle.putString(Constants.paramTitle, CategoryTableMetaData.NAME);
		//bundle.putSerializable(Constants.paramValues, Tools.convertCheckBoxListToHashMap(categoriesList));
		Intent intent = new Intent(MontlyCategoryReport.this, CheckBoxDialog.class);
		intent.putExtras(bundle);
		startActivityForResult(intent, Constants.RequestCategoryForReport);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
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
			startDate = Tools.truncDate(this, Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncYear);
			endDate = Tools.getCurrentDate();
		}
		else if (currentDateInterval == Constants.ReportTimeIntervalBudget.Last12Month.index()) {
			startDate = Tools.truncDate(this, Tools.AddMonth(Tools.getCurrentDate(), -12), Constants.DateTruncTypes.dateTruncMonth);
			endDate = Tools.AddDays(Tools.truncDate(this, Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncMonth), -1);
		}
		btPeriod.setText(Tools.DateToString(startDate, Constants.DateFormatUser) + " - " + Tools.DateToString(endDate, Constants.DateFormatUser));
		reloadScreen();
		refreshList();
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public String getDesc() {
		return null;
	}

	@Override
	public Intent execute(Context context) {
		return null;
	}
}
