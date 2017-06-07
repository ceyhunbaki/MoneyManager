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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CategoryTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.dialogs.LocalDialog;
import com.jgmoneymanager.entity.Margins;
import com.jgmoneymanager.entity.ReportArray;
import com.jgmoneymanager.main.R;
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

import java.util.Calendar;
import java.util.Date;

import chart.AbstractDemoChart;

public class BarChartReport extends AbstractDemoChart {
	private int reportType = Constants.TransFTransaction.Expence.index();

	private final int dateIntervalDailyDialogID = 1;
	private final int dateIntervalWeeklyDialogID = 2;
	private final int dateIntervalMonthlyDialogID = 3;
	private final int dateIntervalYearlyDialogID = 4;
	private final int dateIntervalCustomDialogID = 5;
	private final int fromDateDialogID = 6;
	private final int toDateDialogID = 7;

	private final int btExpenseMenuID = Menu.FIRST;
	private final int btIncomeMenuID = btExpenseMenuID + 1;

	private int currentDateInterval = Constants.ReportTimeInterval.Monthly.index();
	private Date startDate, endDate;
	private int minTransDate;
	private boolean clearDates = true;
	private String years[] = new String[] {};

	private Button btIntervaltype;
	private Button btDate;

	private Spinner spinnerYear;
	private Spinner spinnerMonth;

	private ReportArray repArray;

	private View dialogView;

	private GraphicalView chartView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initializeViews();

		minTransDate = TransactionSrv.getMinTransactionYear(BarChartReport.this);
		int currentYear = Tools.getCurrentDate().getYear() + 1900;
		for (int i = minTransDate; i <= currentYear; i++)
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
		getMenuInflater().inflate(R.menu.menu_empty, menu);
		menu.add(0, btExpenseMenuID, btExpenseMenuID, R.string.expences);
		menu.add(0, btIncomeMenuID, btIncomeMenuID, R.string.incomes);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == btExpenseMenuID)
			reportType = Constants.TransFTransaction.Expence.index();
		else if (id == btIncomeMenuID)
			reportType = Constants.TransFTransaction.Income.index();
		refreshList();
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		Tools.putToBundle(outState, "clearDates", false);
		Tools.putToBundle(outState, "startDate", Tools.DateToString(startDate));
		Tools.putToBundle(outState, "endDate", Tools.DateToString(endDate));
		Tools.putToBundle(outState, "currentDateInterval", currentDateInterval);
		Tools.putToBundle(outState, "reportType", reportType);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		repaintChart();
	}

	private void reloadScreen() {
		btIntervaltype = (Button) findViewById(R.id.repBtInterval);
		btIntervaltype.setText(getResources().getTextArray(
				R.array.ReportTimeInterval)[currentDateInterval].toString());
		btDate = (Button) findViewById(R.id.repBtDate);
	}

	private void refreshTitle() {
		String titleString = ReportList.getTitleString(BarChartReport.this,
				reportType);
		setTitle(getResources().getString(R.string.categories) + " - " + titleString);
	}

	private void refreshList() {
		generateArray();
		refreshTitle();
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
		String sql = ReportSrv.generateCategorySQL(reportType, startDate, endDate);
		repArray = ReportSrv.generateArray(BarChartReport.this, reportType, sql, false, true,
				CategoryTableMetaData._ID, CategoryTableMetaData.NAME);
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
		//renderer.setXAxisMax(10 + 10 / (2 * renderer.getXLabels()));
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

	public void myClickHandler(View target) {
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
					if (Tools.compareDates(
							ReportSrv.addPeriod(true, currentDateInterval, startDate), Tools.getCurrentDate()) <= 0) {
						startDate = ReportSrv.addPeriod(true, currentDateInterval, startDate);
						endDate = ReportSrv.getEndDate(currentDateInterval, startDate);
						refreshDateButtonText();
						refreshList();
					}
				}
				break;
			case R.id.repBtDate:
				if (currentDateInterval == Constants.ReportTimeInterval.Daily
						.index()) {
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

	@Override
	protected Dialog onCreateDialog(int id) {
        switch (id) {
            case dateIntervalDailyDialogID:
                return new DatePickerDialog(this, new OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year,
                                          int monthOfYear, int dayOfMonth) {
                        startDate = new Date(year - 1900, monthOfYear, dayOfMonth);
                        endDate = startDate;
                        refreshDateButtonText();
                        refreshList();
                        //restartActivity();
                    }
                }, startDate.getYear() + 1900, startDate.getMonth(),
                        startDate.getDate());
            case dateIntervalWeeklyDialogID:
                return new DatePickerDialog(this, new OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year,
                                          int monthOfYear, int dayOfMonth) {
                        startDate = Tools.truncDate(BarChartReport.this, new Date(year - 1900, monthOfYear, dayOfMonth), Constants.DateTruncTypes.dateTruncWeek);
                        endDate = ReportSrv.getEndDate(currentDateInterval, startDate);
                        refreshDateButtonText();
                        refreshList();
                        //restartActivity();
                    }
                }, startDate.getYear() + 1900, startDate.getMonth(),
                        startDate.getDate());
            case dateIntervalMonthlyDialogID:
                Command cmd = new Command() {
                    @Override
                    public void execute() {
                        startDate = new Date(spinnerYear.getSelectedItemPosition() + minTransDate - 1900,
                                spinnerMonth.getSelectedItemPosition(), 1);
                        endDate = ReportSrv.getEndDate(currentDateInterval, startDate);
                        refreshList();
                        refreshDateButtonText();
                        //restartActivity();
                    }
                };
                LayoutInflater li = LayoutInflater.from(this);
                View view = li.inflate(R.layout.yearmonthdialog, null);

                spinnerYear = (Spinner) view.findViewById(R.id.dmSpYear);
                spinnerMonth = (Spinner) view.findViewById(R.id.dmSpMonth);

                ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(
                        this, R.layout.simple_spinner_item_blue, years);
                spinnerArrayAdapter
                        .setDropDownViewResource(R.layout.simple_spinner_dropdown_item_blue);
                spinnerYear.setAdapter(spinnerArrayAdapter);

				spinnerArrayAdapter = new ArrayAdapter<String>(
						this, R.layout.simple_spinner_item_blue, getResources().getStringArray(R.array.Months));
				spinnerArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_blue);
				spinnerMonth.setAdapter(spinnerArrayAdapter);

                spinnerYear.setSelection(startDate.getYear() - minTransDate + 1900);
                spinnerMonth.setSelection(startDate.getMonth());

                AlertDialog viewDialog = DialogTools.CustomDialog(
                        BarChartReport.this, cmd, R.string.msgSelectPeriod, view,
                        R.drawable.ic_menu_edit);
                viewDialog.show();
                break;
            case dateIntervalYearlyDialogID:
                cmd = new Command() {
                    @Override
                    public void execute() {
                        startDate = new Date(spinnerYear.getSelectedItemPosition() + minTransDate - 1900, 0, 1);
                        endDate = ReportSrv.getEndDate(currentDateInterval, startDate);
                        refreshDateButtonText();
                        refreshList();
                        //restartActivity();
                    }
                };
                li = LayoutInflater.from(this);
                view = li.inflate(R.layout.radiolistdialog, null);

                spinnerYear = (Spinner) view.findViewById(R.id.rdSpinner);

                spinnerArrayAdapter = new ArrayAdapter<String>(this, R.layout.simple_spinner_item_blue, years);
                spinnerArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_blue);
                spinnerYear.setAdapter(spinnerArrayAdapter);

                spinnerYear.setSelection(startDate.getYear() - minTransDate + 1900);

                AlertDialog listDialog = DialogTools.RadioListDialog(
                        BarChartReport.this, cmd, R.string.msgSelectPeriod, view);
                listDialog.show();
                break;
            case dateIntervalCustomDialogID:
                int minYear = TransactionSrv.getMinTransactionYear(this);
                Calendar minCal = Calendar.getInstance();
                minCal.set(minYear, 1, 1);

                li = LayoutInflater.from(this);
                dialogView = li.inflate(R.layout.datedialog_dual, null);
				final Command cmdPeriod = new Command() {
					@Override
					public void execute() {
						if (startDate.compareTo(endDate) < 0) {
							refreshList();
							refreshDateButtonText();
						} else
							DialogTools.toastDialog(BarChartReport.this, R.string.msgFromDateIsLessThanTo, Toast.LENGTH_LONG);
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

	private void recreateDateInterval(int addPeriod) {
		// addPeriod = 1-add, 2-subtract, 0-go to current date
		switch (addPeriod) {
		case 0:
			startDate = ReportSrv.getCurrentDate(BarChartReport.this, currentDateInterval);
			break;
		case 1:
			startDate = ReportSrv.addPeriod(true, currentDateInterval,
					startDate);
			break;
		case 2:
			startDate = ReportSrv.addPeriod(false, currentDateInterval,
					startDate);
			break;
		default:
			break;
		}
		endDate = ReportSrv.getEndDate(currentDateInterval, startDate);
		refreshDateButtonText();
	}

	private void refreshDateButtonText() {
		btDate.setText(ReportSrv.getDateButtonText(BarChartReport.this, currentDateInterval, startDate, endDate, true));
	}

	private void setDialogDate(int buttonID, Date date) {
        ((Button) dialogView.findViewById(buttonID)).setText(Tools.DateToString(date, Constants.DateFormatUser));
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
