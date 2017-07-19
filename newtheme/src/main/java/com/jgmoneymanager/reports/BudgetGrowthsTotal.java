package com.jgmoneymanager.reports;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
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
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.BudgetCategoriesTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.BudgetTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CategoryTableMetaData;
import com.jgmoneymanager.dialogs.CheckBoxDialog;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.CheckBoxItem;
import com.jgmoneymanager.entity.Margins;
import com.jgmoneymanager.main.R;
import com.jgmoneymanager.services.BudgetSrv;
import com.jgmoneymanager.services.CategorySrv;
import com.jgmoneymanager.services.ChartSrv;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import chart.AbstractDemoChart;

/**
 * Created by Ceyhun on 16/04/2015.
 */
public class BudgetGrowthsTotal extends AbstractDemoChart {
    List<double[]> periodValues = new ArrayList<double[]>();
    List<double[]> budgetValues = new ArrayList<double[]>();

    ArrayList<CheckBoxItem> categoriesList;

    double maxValue = 0;
    double minValue = Long.MAX_VALUE;

    GraphicalView mChartView;
    XYMultipleSeriesRenderer renderer;
    XYMultipleSeriesDataset dataset;

    private final int btCategoriesMenuID = Menu.FIRST;

    Date periodStart, periodEnd;
    int currentDateInterval = Constants.ReportTimeIntervalBudget.ThisYear.index();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeViews();

        if (savedInstanceState != null) {
            currentDateInterval = Tools.getIntegerFromBundle0(savedInstanceState, "currentDateInterval");
            periodStart = Tools.getDateFromBundle(savedInstanceState, "periodStart");
            periodEnd = Tools.getDateFromBundle(savedInstanceState, "periodEnd");
        }

        if (Tools.isPreferenceAvialable(this, R.string.budgetGrowthCategIDsKey)) {
            categoriesList = Tools.getValuesFromString(Tools.getPreference(this, R.string.budgetGrowthCategIDsKey), CategorySrv.generateMainCategoryIDs(getBaseContext()));
        } else
            showCategoryDialogWindow();
        setPeriods();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    void reloadScreen() {
        Button btIntervaltype = (Button) findViewById(R.id.repBtInterval);
        btIntervaltype.setText(getResources().getTextArray(R.array.ReportTimeIntervalBudget)[currentDateInterval].toString());
    }

    void setPeriods() {
        final Button btPeriod = (Button) findViewById(R.id.repBtDate);
        if (currentDateInterval == Constants.ReportTimeIntervalBudget.ThisYear.index()) {
            periodStart = Tools.truncDate(this, Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncYear);
            periodEnd = Tools.truncDate(this, Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncMonth);
            btPeriod.setText(Tools.DateToString(periodStart, Constants.DateFormatYear));
            //findViewById(R.id.repImgDateLeft).setVisibility(View.INVISIBLE);
            //findViewById(R.id.repImgDateRight).setVisibility(View.INVISIBLE);
        } else if (currentDateInterval == Constants.ReportTimeIntervalBudget.Last12Month.index()) {
            periodStart = Tools.truncDate(this, Tools.AddMonth(Tools.getCurrentDate(), -12), Constants.DateTruncTypes.dateTruncMonth);
            periodEnd = Tools.truncDate(this, Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncMonth);
            btPeriod.setText(Tools.DateToString(periodStart, Constants.DateFormatReport) + " - " + Tools.DateToString(periodEnd, Constants.DateFormatReport));
            //findViewById(R.id.repImgDateLeft).setVisibility(View.INVISIBLE);
            //findViewById(R.id.repImgDateRight).setVisibility(View.INVISIBLE);
        } else {
            btPeriod.setText(Tools.DateToString(periodStart, Constants.DateFormatReport) + " - " + Tools.DateToString(periodEnd, Constants.DateFormatReport));
        }
        reloadScreen();
        refreshList(categoriesList, periodStart, periodEnd);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Tools.putToBundle(outState, "currentDateInterval", currentDateInterval);
        Tools.putToBundle(outState, "periodStart", periodStart);
        Tools.putToBundle(outState, "periodEnd", periodEnd);
        super.onSaveInstanceState(outState);
    }

    private void refreshList(ArrayList<CheckBoxItem> categoriesList, Date periodStart, Date periodEnd) {
        if (categoriesList != null) {
            periodValues.clear();
            budgetValues.clear();
            maxValue = 0;
            minValue = Long.MAX_VALUE;
            String allCategoryIDs = "";
            int monthsCount = Tools.monthsBetween(periodStart, periodEnd) + 1;
            for (int i = 0; i < categoriesList.size(); i++) {
                CheckBoxItem category = categoriesList.get(i);
                if (category.isSelected())
                    allCategoryIDs += category.getID() + ",";
            }
            if (allCategoryIDs.length() > 0)
                allCategoryIDs = allCategoryIDs.substring(0, allCategoryIDs.length() - 1);
            String query = "select " + BudgetTableMetaData.FROM_DATE + ", " +
                    " sum(" + BudgetCategoriesTableMetaData.BUDGET + "+" + BudgetCategoriesTableMetaData.REMAINING +
                    "-" + BudgetCategoriesTableMetaData.USED_AMOUNT + ")" + BudgetCategoriesTableMetaData.BUDGET +
                    " from (\n" +
                    " select " + CategoryTableMetaData._ID + ", (select " + CategoryTableMetaData.NAME + " from " + CategoryTableMetaData.TABLE_NAME +
                    " where " + CategoryTableMetaData._ID + " = ifnull(c1." + CategoryTableMetaData.MAINID + ", c1." + CategoryTableMetaData._ID + ")) category\n" +
                    " from  " + CategoryTableMetaData.TABLE_NAME + "  c1 where " + CategoryTableMetaData._ID + " in (" + allCategoryIDs + ") " +
                    " or  " + CategoryTableMetaData.MAINID + "  in (" + allCategoryIDs + "))cat\n" +
                    " join " + BudgetCategoriesTableMetaData.TABLE_NAME + " bc on bc." + BudgetCategoriesTableMetaData.CATEGORY_ID + " = cat." + CategoryTableMetaData._ID + "\n" +
                    " join " + BudgetTableMetaData.TABLE_NAME + " bud on bud. " + BudgetTableMetaData._ID + " = bc. " + BudgetCategoriesTableMetaData.BUDGET_ID + "\n" +
                    " where " + BudgetTableMetaData.FROM_DATE + " between '" + Tools.DateToDBString(periodStart) + "' and '" + Tools.DateToDBString(periodEnd) +
                    "' group by " + BudgetTableMetaData.FROM_DATE + "\n" +
                    " order by 1";
            Cursor cursor = DBTools.createCursor(BudgetGrowthsTotal.this, query);

            double[] currentPeriods = new double[monthsCount + 1];
            for (int m = 0; m < monthsCount + 1; m++)
                currentPeriods[m] = m;//Tools.AddMonth(periodStart, m);

            double[] currentBudget = new double[monthsCount + 1];
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                Date budgetMonth = DBTools.getCursorColumnValueDate(cursor, BudgetTableMetaData.FROM_DATE, Constants.DateFormatDB);
                //currentPeriods[Tools.monthsBetween(periodStart, budgetMonth)] = Long.parseLong(Tools.DateToString(budgetMonth, Constants.DateFormatDB));
                double budget = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.BUDGET);
                currentBudget[Tools.monthsBetween(periodStart, budgetMonth) + 1] = budget;
                maxValue = Math.max(maxValue, budget);
                minValue = Math.min(minValue, budget);
            }
            periodValues.add(currentPeriods);
            budgetValues.add(currentBudget);

            maxValue += Math.round(maxValue / 10);
            minValue -= Math.round(maxValue / 10);

            LinearLayout layout = (LinearLayout) findViewById(R.id.chart);
            if (mChartView != null) {
                try {
                    layout.removeView(mChartView);
                    renderer.removeAllRenderers();
                    dataset.clear();
                } catch (Exception e) {

                }
            }
            mChartView = repaintChart(minValue, maxValue, periodValues, budgetValues, periodStart, periodEnd);
            layout.addView(mChartView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }


    private GraphicalView repaintChart(double minValue, double maxValue, List<double[]> periodValues, List<double[]> budgetValues, Date periodStart, Date periodEnd) {
        int[] colors = ChartSrv.generateRandomColorsList(1);
        PointStyle[] styles = ChartSrv.generateRandomStyleList(1);
        renderer = buildRenderer(colors, styles);
        int length = renderer.getSeriesRendererCount();
        for (int i = 0; i < length; i++) {
            ((XYSeriesRenderer) renderer.getSeriesRendererAt(i)).setFillPoints(true);
        }
        int monthCount = Tools.monthsBetween(periodStart, periodEnd) + 1;
        setChartSettings(renderer, null, null, getResources().getString(R.string.totalBudget),
                1, monthCount,
                minValue, maxValue, Color.LTGRAY, Color.LTGRAY);
        renderer.setXLabels(0);
        for (int i = 0; i <= monthCount; i++) {
            renderer.addXTextLabel(i + 1, Tools.DateToString(Tools.AddMonth(periodStart, i), Constants.DateFormatShort));
        }
        renderer.setXRoundedLabels(false);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            renderer.setYLabels(6);
        else
            renderer.setYLabels(10);
        renderer.setShowGrid(false);
        renderer.setXLabelsAlign(Paint.Align.RIGHT);
        renderer.setXLabelsAngle(270);
        renderer.setYLabelsAlign(Paint.Align.RIGHT);
        renderer.setZoomButtonsVisible(false);
        renderer.setPanLimits(new double[]{-10, 20, -10, 40});
        renderer.setZoomLimits(new double[]{-10, 20, -10, 40});
        Margins margins = Tools.getMarginsFromStyle(this, R.style.LineReportMargins);
        int leftMargin = (String.valueOf(Math.round(maxValue)).length() + 2) * margins.getLeftMargin();
        renderer.setMargins(new int[]{margins.getTopMargin(), leftMargin, margins.getBottomMargin(), margins.getRightMargin()});

        dataset = buildDataset(new String[]{getResources().getString(R.string.total)}, periodValues, budgetValues);
        return ChartFactory.getLineChartView(this, dataset, renderer);
    }

    public void myClickHandler(View target) {
        switch (target.getId()) {
            case R.id.repImgIntLeft:
                if (currentDateInterval > 0) {
                    currentDateInterval--;
                    setPeriods();
                }
                break;
            case R.id.repImgIntRight:
                if (currentDateInterval < 2) {
                    currentDateInterval++;
                    setPeriods();
                }
                break;
            case R.id.repBtDate:
                if (currentDateInterval == Constants.ReportTimeIntervalBudget.Custom.index()) {
                    final Button btPeriod = (Button) findViewById(R.id.repBtDate);

                    final int minYear = BudgetSrv.getBudgetMinimumYear(this);
                    String[] years = new String[Tools.getCurrentYear() - minYear + 1];
                    for (int i = 0; i <= Tools.getCurrentYear() - minYear; i++) {
                        years[i] = String.valueOf(minYear + i);
                    }

                    LayoutInflater li = LayoutInflater.from(this);
                    final View view = li.inflate(R.layout.yearmonthdialog_dual, null);

                    final Spinner spinnerYearFrom = (Spinner) view.findViewById(R.id.dmSpYearFrom);
                    final Spinner spinnerMonthFrom = (Spinner) view.findViewById(R.id.dmSpMonthFrom);
                    final Spinner spinnerYearTo = (Spinner) view.findViewById(R.id.dmSpYearTo);
                    final Spinner spinnerMonthTo = (Spinner) view.findViewById(R.id.dmSpMonthTo);

                    ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, R.layout.simple_spinner_item_blue, years);
                    spinnerArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_blue);
                    spinnerYearFrom.setAdapter(spinnerArrayAdapter);
                    spinnerYearTo.setAdapter(spinnerArrayAdapter);

                    String[] months = getResources().getStringArray(R.array.Months);
                    ArrayAdapter<String> spinnerMonthArrayAdapter = new ArrayAdapter<String>(this, R.layout.simple_spinner_item_blue, months);
                    spinnerMonthArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_blue);
                    spinnerMonthFrom.setAdapter(spinnerMonthArrayAdapter);
                    spinnerMonthTo.setAdapter(spinnerMonthArrayAdapter);

                    spinnerYearFrom.setSelection(periodStart.getYear() - minYear + 1900);
                    spinnerMonthFrom.setSelection(periodStart.getMonth());
                    spinnerYearTo.setSelection(periodEnd.getYear() - minYear + 1900);
                    spinnerMonthTo.setSelection(periodEnd.getMonth());

                    final AlertDialog viewDialog = DialogTools.CustomDialog(this, view);

                    final Command cmdPeriod = new Command() {
                        @Override
                        public void execute() {
                            periodStart = new Date(spinnerYearFrom.getSelectedItemPosition() + minYear - 1900, spinnerMonthFrom.getSelectedItemPosition(), 1);
                            periodEnd = new Date(spinnerYearTo.getSelectedItemPosition() + minYear - 1900, spinnerMonthTo.getSelectedItemPosition(), 1);

                            if (periodStart.compareTo(periodEnd) < 0) {
                                btPeriod.setText(Tools.DateToString(periodStart, Constants.DateFormatReport) + " - " + Tools.DateToString(periodEnd, Constants.DateFormatReport));
                                reloadScreen();
                                refreshList(categoriesList, periodStart, periodEnd);
                                viewDialog.dismiss();
                            } else
                                DialogTools.toastDialog(BudgetGrowthsTotal.this, R.string.msgFromDateIsLessThanTo, Toast.LENGTH_LONG);
                        }
                    };

                    Command cmdCancel = new Command() {
                        @Override
                        public void execute() {
                            viewDialog.dismiss();
                        }
                    };

                    DialogTools.setButtonActions(view, R.id.dmBtOK, R.id.dmBtCancel, cmdPeriod, cmdCancel);

                    viewDialog.show();
                }
                break;

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK)
            if (requestCode == Constants.RequestCategoryForReport) {
                refreshList(categoriesList, periodStart, periodEnd);
                Tools.setPreference(BudgetGrowthsTotal.this, R.string.budgetGrowthCategIDsKey, Tools.convertListToString(categoriesList), false);
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
        Intent intent = new Intent(BudgetGrowthsTotal.this, CheckBoxDialog.class);
        intent.putExtras(bundle);
        startActivityForResult(intent, Constants.RequestCategoryForReport);
    }

    void initializeViews() {
        setContentView(R.layout.main_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        LinearLayout mainLayout = (LinearLayout) findViewById(R.id.content_main_layout);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        View child = inflater.inflate(R.layout.report_window, null);
        mainLayout.addView(child, params);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_empty, menu);
        menu.add(0, btCategoriesMenuID, btCategoriesMenuID, R.string.categories);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == btCategoriesMenuID) {
            showCategoryDialogWindow();
        }

        return super.onOptionsItemSelected(item);
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
