package com.jgmoneymanager.reports;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CategoryTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.ReportArray;
import com.jgmoneymanager.paid.R;
import com.jgmoneymanager.services.ReportSrv;
import com.jgmoneymanager.services.TransactionSrv;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.CategorySeries;
import org.achartengine.model.SeriesSelection;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;

import java.util.Calendar;
import java.util.Date;

import chart.AbstractDemoChart;

public class PieChartReport extends AbstractDemoChart {
    private final int btExpenseMenuID = Menu.FIRST;
    private final int btIncomeMenuID = btExpenseMenuID + 1;

    int reportType = Constants.TransFTransaction.Expence.index();

    final int dateIntervalDailyDialogID = 1;
    final int dateIntervalWeeklyDialogID = 2;
    final int dateIntervalMonthlyDialogID = 3;
    final int dateIntervalYearlyDialogID = 4;
    final int dateIntervalCustomDialogID = 5;
    final int fromDateDialogID = 6;
    final int toDateDialogID = 7;

    int currentDateInterval = Constants.ReportTimeInterval.Monthly.index();
    Date startDate, endDate;
    int minTransDate;
    boolean clearDates = true;
    String years[] = new String[]{};

    Button btIntervaltype;
    Button btDate;

    Spinner spinnerYear;
    Spinner spinnerMonth;

    ReportArray repArray;

    View dialogView;

    /**
     * Colors to be used for the pie slices.
     */
    private static int[] COLORS = new int[]{Color.GREEN, Color.BLUE, Color.MAGENTA, Color.CYAN, Color.RED, Color.YELLOW};
    /**
     * The main series that will include all the data.
     */
    private CategorySeries mSeries = new CategorySeries("");
    /**
     * The main renderer for the main dataset.
     */
    private DefaultRenderer mRenderer = new DefaultRenderer();
    /**
     * The chart view that displays the data.
     */
    private GraphicalView mChartView;

    @Override
    public void onRestoreInstanceState(Bundle savedState) {
        super.onRestoreInstanceState(savedState);
        Tools.loadLanguage(this, null);
        mSeries = (CategorySeries) savedState.getSerializable("current_series");
        mRenderer = (DefaultRenderer) savedState
                .getSerializable("current_renderer");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("current_series", mSeries);
        outState.putSerializable("current_renderer", mRenderer);
        Tools.putToBundle(outState, "clearDates", false);
        Tools.putToBundle(outState, "startDate", Tools.DateToString(startDate));
        Tools.putToBundle(outState, "endDate", Tools.DateToString(endDate));
        Tools.putToBundle(outState, "currentDateInterval", currentDateInterval);
        Tools.putToBundle(outState, "reportType", reportType);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initializeViews();

        TypedArray array = this.obtainStyledAttributes(R.style.Theme_LabelTextSize, new int[]{android.R.attr.textSize});
        mRenderer.setZoomButtonsVisible(false);
        mRenderer.setLabelsTextSize(array.getDimensionPixelSize(0, 25));
        mRenderer.setLegendTextSize(array.getDimensionPixelSize(0, 25));
        mRenderer.setStartAngle(180);
        mRenderer.setDisplayValues(true);
        array.recycle();

        minTransDate = TransactionSrv.getMinTransactionYear(PieChartReport.this);
        int currentYear = Tools.getCurrentDate().getYear() + 1900;
        for (int i = minTransDate; i <= currentYear; i++)
            years = Tools.addElement(years, String.valueOf(i));

        if (savedInstanceState != null) {
            clearDates = Tools.getBooleanFromBundle0(savedInstanceState, "clearDates");
            startDate = Tools.getDateFromBundle(savedInstanceState, "startDate");
            endDate = Tools.getDateFromBundle(savedInstanceState, "endDate");
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
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
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
    protected void onResume() {
        super.onResume();
        repaintChart();
    }

    void reloadScreen() {
        btIntervaltype = (Button) findViewById(R.id.repBtInterval);
        btIntervaltype.setText(getResources().getTextArray(R.array.ReportTimeInterval)[currentDateInterval].toString());
        btDate = (Button) findViewById(R.id.repBtDate);
    }

    void refreshTitle() {
        String titleString = ReportList.getTitleString(PieChartReport.this, reportType);
        setTitle(getResources().getString(R.string.categories) + " - " + titleString);
    }

    void refreshList() {
        generateArray();
        refreshTitle();
    }

    void repaintChart() {
        //if (mChartView == null) {
        LinearLayout layout = (LinearLayout) findViewById(R.id.chart);
        if (mChartView != null)
            layout.removeView(mChartView);
        mChartView = ChartFactory.getPieChartView(this, mSeries, mRenderer);
        // mChartView.setBackgroundColor(getResources().getColor(R.color.White));
        mRenderer.setClickEnabled(true);
        mChartView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SeriesSelection seriesSelection = mChartView.getCurrentSeriesAndPoint();
                if (seriesSelection != null) {
                    for (int i = 0; i < mSeries.getItemCount(); i++) {
                        mRenderer.getSeriesRendererAt(i).setHighlighted(i == seriesSelection.getPointIndex());
                    }
                    mChartView.repaint();
                    Toast.makeText(
                            PieChartReport.this,
                            mSeries.getCategory(seriesSelection.getPointIndex())
                                    + " - " + Tools.round(seriesSelection.getValue()),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        layout.addView(mChartView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        /*} else {
			mChartView.repaint();
		}*/

    }

    void generateArray() {
        String sql = ReportSrv.generateCategorySQL(reportType, startDate, endDate);
        repArray = ReportSrv.generateArray(PieChartReport.this, reportType, sql, false, true,
                CategoryTableMetaData._ID, CategoryTableMetaData.NAME);

        mRenderer.removeAllRenderers();
        mSeries.clear();
        for (int i = 1; i < repArray.getItemCount(); i++) {
            mSeries.add(repArray.getItem(i).getName(), repArray.getItem(i).getAmount());
            SimpleSeriesRenderer renderer = new SimpleSeriesRenderer();
            renderer.setColor(COLORS[(mSeries.getItemCount() - 1) % COLORS.length]);
            mRenderer.addSeriesRenderer(renderer);
        }
        repaintChart();
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

    @Override
    protected Dialog onCreateDialog(int id) {
        final Button dpStartDate;
        final Button dpEndDate;
        switch (id) {
            case dateIntervalDailyDialogID:
                return new DatePickerDialog(this, new OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year,
                                          int monthOfYear, int dayOfMonth) {
                        startDate = new Date(year - 1900, monthOfYear, dayOfMonth);
                        endDate = startDate;
                        refreshList();
                        refreshDateButtonText();
                    }
                }, startDate.getYear() + 1900, startDate.getMonth(),
                        startDate.getDate());
            case dateIntervalWeeklyDialogID:
                return new DatePickerDialog(this, new OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year,
                                          int monthOfYear, int dayOfMonth) {
                        startDate = Tools.truncDate(PieChartReport.this, new Date(year - 1900, monthOfYear, dayOfMonth),
                                Constants.DateTruncTypes.dateTruncWeek);
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

                ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(
                        this, R.layout.simple_spinner_item_blue, years);
                spinnerArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_blue);
                spinnerYear.setAdapter(spinnerArrayAdapter);

                spinnerArrayAdapter = new ArrayAdapter<String>(
                        this, R.layout.simple_spinner_item_blue, getResources().getStringArray(R.array.Months));
                spinnerArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_blue);
                spinnerMonth.setAdapter(spinnerArrayAdapter);

                spinnerYear.setSelection(startDate.getYear() - minTransDate + 1900);
                spinnerMonth.setSelection(startDate.getMonth());

                AlertDialog viewDialog = DialogTools.CustomDialog(
                        PieChartReport.this, cmd, R.string.msgSelectPeriod, view, R.drawable.ic_menu_edit);
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

                spinnerArrayAdapter = new ArrayAdapter<String>(this,
                        R.layout.simple_spinner_item_blue, years);
                spinnerArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_blue);
                spinnerYear.setAdapter(spinnerArrayAdapter);

                spinnerYear.setSelection(startDate.getYear() - minTransDate + 1900);

                AlertDialog listDialog = DialogTools.RadioListDialog(PieChartReport.this, cmd, R.string.msgSelectPeriod, view);
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
                            DialogTools.toastDialog(PieChartReport.this, R.string.msgFromDateIsLessThanTo, Toast.LENGTH_LONG);
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
        // addPeriod = 1-add, 2-subtract, 0-go to current date
        switch (addPeriod) {
            case 0:
                startDate = ReportSrv.getCurrentDate(PieChartReport.this, currentDateInterval);
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
        btDate.setText(ReportSrv.getDateButtonText(PieChartReport.this, currentDateInterval, startDate, endDate, true));
    }

    void setDialogDate(int buttonID, Date date) {
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
