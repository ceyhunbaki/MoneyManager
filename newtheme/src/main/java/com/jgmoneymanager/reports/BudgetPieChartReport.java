package com.jgmoneymanager.reports;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransactionsTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.BudgetTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CategoryTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.BudgetCategoriesTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.main.R;
import com.jgmoneymanager.services.BudgetSrv;
import com.jgmoneymanager.services.ReportSrv;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.entity.ReportArray;
import com.jgmoneymanager.tools.Tools;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.CategorySeries;
import org.achartengine.model.SeriesSelection;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;

import java.util.Date;

import chart.AbstractDemoChart;

public class BudgetPieChartReport extends AbstractDemoChart {
    int reportType = Constants.TransFTransaction.All.index();

    final int dateIntervalMonthlyDialogID = 3;
    final int dateIntervalCustomDialogID = 5;

    int currentDateInterval = Constants.ReportTimeIntervalBudget2.Monthly.index();
    Date startDate, endDate;
    int minTransDate;
    boolean clearDates = true;
    String years[] = new String[]{};

    Button btIntervaltype;
    Button btDate;

    Spinner spinnerYear;
    Spinner spinnerMonth;

    ReportArray repArray;

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

        TypedArray array = this.obtainStyledAttributes(R.style.Theme_LabelTextSize, new int[]{android.R.attr.textSize});
        mRenderer.setZoomButtonsVisible(false);
        mRenderer.setLabelsTextSize(array.getDimensionPixelSize(0, 25));
        mRenderer.setLegendTextSize(array.getDimensionPixelSize(0, 25));
        mRenderer.setStartAngle(180);
        mRenderer.setDisplayValues(true);
        array.recycle();

        minTransDate = BudgetSrv.getBudgetMinimumYear(this);//TransactionSrv.getMinTransactionYear(BudgetPieChartReport.this);
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

    @Override
    protected void onResume() {
        super.onResume();
        repaintChart();
    }

    void reloadScreen() {
        btIntervaltype = (Button) findViewById(R.id.repBtInterval);
        btIntervaltype.setText(getResources().getTextArray(R.array.ReportTimeIntervalBudget2)[currentDateInterval].toString());
        btDate = (Button) findViewById(R.id.repBtDate);
    }

    void refreshList() {
        generateArray();
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
                            BudgetPieChartReport.this,
                            mSeries.getCategory(seriesSelection.getPointIndex())
                                    + " - " + Tools.round(seriesSelection.getValue()),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        layout.addView(mChartView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
    }

    void generateArray() {
        //String sql = ReportSrv.generateCategorySQL(reportType, startDate, endDate);
        String sql = "select " + CategoryTableMetaData._ID + ", " + CategoryTableMetaData.NAME + ", \nsum(" + BudgetCategoriesTableMetaData.BUDGET + ") "
                + TransactionsTableMetaData.AMOUNT + ", \n"
                + Constants.defaultCurrency + " " + TransactionsTableMetaData.CURRENCYID + ", \n"
                + Tools.DateToDBString(startDate) + " " + TransactionsTableMetaData.TRANSDATE + ", \n"
                + Constants.TransactionTypeIncome + " " + TransactionsTableMetaData.TRANSTYPE + " \n"
                + " from (select ifnull(cm."
                + CategoryTableMetaData.NAME + ", c." + CategoryTableMetaData.NAME + ") " + CategoryTableMetaData.NAME
                + ",\n ifnull(cm." + CategoryTableMetaData._ID + ", c." + CategoryTableMetaData._ID + ") "
                + CategoryTableMetaData._ID + ", \nbc." + BudgetCategoriesTableMetaData.BUDGET + " \nfrom " + BudgetCategoriesTableMetaData.TABLE_NAME
                + " bc \njoin " + BudgetTableMetaData.TABLE_NAME + " b on bc." + BudgetCategoriesTableMetaData.BUDGET_ID + " = b."
                + BudgetTableMetaData._ID + " \njoin " + CategoryTableMetaData.TABLE_NAME + " c on c." + CategoryTableMetaData._ID
                + " = bc." + BudgetCategoriesTableMetaData.CATEGORY_ID + " \nleft join " + CategoryTableMetaData.TABLE_NAME + " cm on cm."
                + CategoryTableMetaData._ID + " = c." + CategoryTableMetaData.MAINID + " \nwhere b." + BudgetTableMetaData.FROM_DATE + " between '"
                + Tools.DateToDBString(startDate) + "' and '" + Tools.DateToDBString(endDate)
                + "') \ngroup by " + CategoryTableMetaData.NAME + ", " + CategoryTableMetaData._ID
                + " \nhaving sum(" + BudgetCategoriesTableMetaData.BUDGET + ") > 0";
        repArray = ReportSrv.generateArray(BudgetPieChartReport.this, reportType, sql, false, true,
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
                    int hideDateArrow = (currentDateInterval != Constants.ReportTimeIntervalBudget2.Custom.index()) ? View.VISIBLE : View.INVISIBLE;
                    findViewById(R.id.repImgDateLeft).setVisibility(hideDateArrow);
                    findViewById(R.id.repImgDateRight).setVisibility(hideDateArrow);
                    btIntervaltype.setText(getResources().getTextArray(R.array.ReportTimeIntervalBudget2)[currentDateInterval].toString());
                    recreateDateInterval(0);
                    refreshList();
                }
                break;
            case R.id.repImgIntRight:
                if (currentDateInterval < Constants.ReportTimeIntervalBudget2.values().length - 1) {
                    currentDateInterval++;
                    int hideDateArrow = (currentDateInterval != Constants.ReportTimeIntervalBudget2.Custom.index()) ? View.VISIBLE : View.INVISIBLE;
                    findViewById(R.id.repImgDateLeft).setVisibility(hideDateArrow);
                    findViewById(R.id.repImgDateRight).setVisibility(hideDateArrow);
                    btIntervaltype.setText(getResources().getTextArray(R.array.ReportTimeIntervalBudget2)[currentDateInterval].toString());
                    recreateDateInterval(0);
                    refreshList();
                }
                break;
            case R.id.repImgDateLeft:
                if (currentDateInterval != Constants.ReportTimeIntervalBudget2.Custom.index()) {
                    startDate = ReportSrv.addPeriod(false, Constants.ReportTimeInterval.Monthly.index(), startDate);
                    endDate = ReportSrv.getEndDate(Constants.ReportTimeInterval.Monthly.index(), startDate);
                    refreshDateButtonText();
                    refreshList();
                }
                break;
            case R.id.repImgDateRight:
                if (currentDateInterval != Constants.ReportTimeIntervalBudget2.Custom.index()) {
                    if (Tools.compareDates(ReportSrv.addPeriod(true, Constants.ReportTimeInterval.Monthly.index(), startDate), Tools.getCurrentDate()) <= 0) {
                        startDate = ReportSrv.addPeriod(true, Constants.ReportTimeInterval.Monthly.index(), startDate);
                        endDate = ReportSrv.getEndDate(Constants.ReportTimeInterval.Monthly.index(), startDate);
                        refreshDateButtonText();
                        refreshList();
                    }
                }
                break;
            case R.id.repBtDate:
                if (currentDateInterval == Constants.ReportTimeIntervalBudget2.Monthly.index()) {
                    showDialog(dateIntervalMonthlyDialogID);
                } else if (currentDateInterval == Constants.ReportTimeIntervalBudget2.Custom.index()) {
                    showDialog(dateIntervalCustomDialogID);
                }
                break;
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
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
                        BudgetPieChartReport.this, cmd, R.string.msgSelectPeriod, view, R.drawable.ic_menu_edit);
                viewDialog.show();
                break;
            case dateIntervalCustomDialogID:

                final int minYear = BudgetSrv.getBudgetMinimumYear(this);
                String[] years = new String[Tools.getCurrentYear() - minYear + 1];
                for (int i = 0; i <= Tools.getCurrentYear() - minYear; i++) {
                    years[i] = String.valueOf(minYear + i);
                }

                li = LayoutInflater.from(this);
                view = li.inflate(R.layout.yearmonthdialog_dual, null);

                final Spinner spinnerYearFrom = (Spinner) view.findViewById(R.id.dmSpYearFrom);
                final Spinner spinnerMonthFrom = (Spinner) view.findViewById(R.id.dmSpMonthFrom);
                final Spinner spinnerYearTo = (Spinner) view.findViewById(R.id.dmSpYearTo);
                final Spinner spinnerMonthTo = (Spinner) view.findViewById(R.id.dmSpMonthTo);

                spinnerArrayAdapter = new ArrayAdapter<String>(this, R.layout.simple_spinner_item_blue, years);
                spinnerArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_blue);
                spinnerYearFrom.setAdapter(spinnerArrayAdapter);
                spinnerYearTo.setAdapter(spinnerArrayAdapter);

                String[] months = getResources().getStringArray(R.array.Months);
                ArrayAdapter<String> spinnerMonthArrayAdapter = new ArrayAdapter<String>(this, R.layout.simple_spinner_item_blue, months);
                spinnerMonthArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_blue);
                spinnerMonthFrom.setAdapter(spinnerMonthArrayAdapter);
                spinnerMonthTo.setAdapter(spinnerMonthArrayAdapter);

                spinnerYearFrom.setSelection(startDate.getYear() - minYear + 1900);
                spinnerMonthFrom.setSelection(startDate.getMonth());
                spinnerYearTo.setSelection(endDate.getYear() - minYear + 1900);
                spinnerMonthTo.setSelection(endDate.getMonth());

                final AlertDialog viewDialogCustom = DialogTools.CustomDialog(this, view);

                final Command cmdPeriod = new Command() {
                    @Override
                    public void execute() {
                        startDate = new Date(spinnerYearFrom.getSelectedItemPosition() + minYear - 1900, spinnerMonthFrom.getSelectedItemPosition(), 1);
                        endDate = new Date(spinnerYearTo.getSelectedItemPosition() + minYear - 1900, spinnerMonthTo.getSelectedItemPosition(), 1);

                        if (startDate.compareTo(endDate) < 0) {
                            refreshList();
                            refreshDateButtonText();
                            viewDialogCustom.dismiss();
                        } else
                            DialogTools.toastDialog(BudgetPieChartReport.this, R.string.msgFromDateIsLessThanTo, Toast.LENGTH_LONG);
                    }
                };

                Command cmdCancel = new Command() {
                    @Override
                    public void execute() {
                        viewDialogCustom.dismiss();
                    }
                };

                DialogTools.setButtonActions(view, R.id.dmBtOK, R.id.dmBtCancel, cmdPeriod, cmdCancel);

                viewDialogCustom.show();
                break;
        }
        return null;
    }

    void recreateDateInterval(int addPeriod) {
        // addPeriod = 1-add, 2-subtract, 0-go to current date
        switch (addPeriod) {
            case 0:
                startDate = Tools.truncDate(this, ReportSrv.getCurrentDate(this, currentDateInterval), Constants.DateTruncTypes.dateTruncMonth);
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
        endDate = ReportSrv.getEndDate(Constants.ReportTimeInterval.Monthly.index(), startDate);
        refreshDateButtonText();
    }

    void refreshDateButtonText() {
        btDate.setText(ReportSrv.getDateButtonText(BudgetPieChartReport.this, currentDateInterval, startDate, endDate, true));
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
