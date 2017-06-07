package com.jgmoneymanager.reports;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jgmoneymanager.SlidingMenu.MyHorizontalScrollView;
import com.jgmoneymanager.SlidingMenu.OnSwipeTouchListener;
import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.BudgetCategoriesTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.BudgetTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CategoryTableMetaData;
import com.jgmoneymanager.dialogs.CheckBoxDialog;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyAbstractDemoChartctivity;
import com.jgmoneymanager.main.R;
import com.jgmoneymanager.services.BudgetSrv;
import com.jgmoneymanager.services.CategorySrv;
import com.jgmoneymanager.services.ChartSrv;
import com.jgmoneymanager.entity.CheckBoxItem;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.LocalTools;
import com.jgmoneymanager.entity.Margins;
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

/**
 * Created by Ceyhun on 21/05/2015.
 */
public class BudgetedAmountByCategories extends MyAbstractDemoChartctivity {
    private MyHorizontalScrollView scrollView;
    private static View menu;
    private View app;
    private ImageView btnSlide;

    private List<String> names = new ArrayList<String>();
    private List<double[]> periodValues = new ArrayList<double[]>();
    private List<double[]> budgetValues = new ArrayList<double[]>();

    private ArrayList<CheckBoxItem> categoriesList;

    double maxValue = 0;
    double minValue = Long.MAX_VALUE;

    private GraphicalView mChartView;
    private XYMultipleSeriesRenderer renderer;
    private XYMultipleSeriesDataset dataset;
    private String btMenuCategoriesTag = "btMenuCategoriesTag";

    private Date periodStart, periodEnd;
    private int currentDateInterval = Constants.ReportTimeIntervalBudget.ThisYear.index();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report_window);

        if (savedInstanceState != null) {
            currentDateInterval = Tools.getIntegerFromBundle0(savedInstanceState, "currentDateInterval");
            periodStart = Tools.getDateFromBundle(savedInstanceState, "periodStart");
            periodEnd = Tools.getDateFromBundle(savedInstanceState, "periodEnd");
        }

        initializeViews();
        if (Tools.isPreferenceAvialable(this, R.string.budgetGrowthCategIDsKey)) {
            categoriesList = Tools.getValuesFromString(Tools.getPreference(this, R.string.budgetGrowthCategIDsKey), CategorySrv.generateMainCategoryIDs(getBaseContext()));
        }
        else
            showCategoryDialogWindow();
        setPeriods();
        //reloadScreen();

        ((TextView)findViewById(R.id.tvATTitle)).setText(R.string.repBudgetAmount);

        //refreshList(categoriesList, checkedCategoryIDs, periodStart, periodEnd);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void reloadScreen() {
        Button btIntervaltype = (Button) findViewById(R.id.repBtInterval);
        btIntervaltype.setText(getResources().getTextArray(R.array.ReportTimeIntervalBudget)[currentDateInterval].toString());
    }

    private void setPeriods() {
        final Button btPeriod = (Button) findViewById(R.id.repBtDate);
        if (currentDateInterval == Constants.ReportTimeIntervalBudget.ThisYear.index()) {
            periodStart = Tools.truncDate(Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncYear);
            periodEnd = Tools.truncDate(Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncMonth);
            btPeriod.setText(Tools.DateToString(periodStart, Constants.DateFormatYear));
            //findViewById(R.id.repImgDateLeft).setVisibility(View.INVISIBLE);
            //findViewById(R.id.repImgDateRight).setVisibility(View.INVISIBLE);
        }
        else if (currentDateInterval == Constants.ReportTimeIntervalBudget.Last12Month.index()) {
            periodStart = Tools.truncDate(Tools.AddMonth(Tools.getCurrentDate(), -12), Constants.DateTruncTypes.dateTruncMonth);
            periodEnd = Tools.truncDate(Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncMonth);
            btPeriod.setText(Tools.DateToString(periodStart, Constants.DateFormatReport) + " - " + Tools.DateToString(periodEnd, Constants.DateFormatReport));
            //findViewById(R.id.repImgDateLeft).setVisibility(View.INVISIBLE);
            //findViewById(R.id.repImgDateRight).setVisibility(View.INVISIBLE);
        }
        else {
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
        if (this.categoriesList != null) {
            names.clear();
            periodValues.clear();
            budgetValues.clear();
            maxValue = 0;
            minValue = Long.MAX_VALUE;
            int monthsCount = Tools.monthsBetween(periodStart, periodEnd) + 1;
            for (int i = 0; i < this.categoriesList.size(); i++) {
                CheckBoxItem category = categoriesList.get(i);
                if (category.isSelected()) {
                    names.add(category.getName());
                    String colCategory = "category";
                    String query = "select " + colCategory + ", " + BudgetTableMetaData.FROM_DATE + ", " +
                            " sum(" + BudgetCategoriesTableMetaData.BUDGET + /*"+" + BudgetCategoriesTableMetaData.REMAINING +
                            "-" + BudgetCategoriesTableMetaData.USED_AMOUNT +*/ ")" + BudgetCategoriesTableMetaData.BUDGET +
                            " from (\n" +
                            " select " + CategoryTableMetaData._ID + ", (select " + CategoryTableMetaData.NAME + " from " + CategoryTableMetaData.TABLE_NAME +
                            " where " + CategoryTableMetaData._ID + " = ifnull(c1." + CategoryTableMetaData.MAINID + ", c1." + CategoryTableMetaData._ID + ")) category\n" +
                            " from  " + CategoryTableMetaData.TABLE_NAME + "  c1 where " + CategoryTableMetaData._ID + " in (" + category.getID() + ") " +
                            " or  " + CategoryTableMetaData.MAINID + "  in (" + category.getID() + "))cat\n" +
                            " join " + BudgetCategoriesTableMetaData.TABLE_NAME + " bc on bc." + BudgetCategoriesTableMetaData.CATEGORY_ID + " = cat." + CategoryTableMetaData._ID + "\n" +
                            " join " + BudgetTableMetaData.TABLE_NAME + " bud on bud. " + BudgetTableMetaData._ID + " = bc. " + BudgetCategoriesTableMetaData.BUDGET_ID + "\n" +
                            " where " + BudgetTableMetaData.FROM_DATE + " between '" + Tools.DateToDBString(periodStart) + "' and '" + Tools.DateToDBString(periodEnd) +
                            "' group by " + colCategory + ", " + BudgetTableMetaData.FROM_DATE + "\n" +
                            " order by 2";
                    Cursor cursor = DBTools.createCursor(BudgetedAmountByCategories.this, query);

                    double[] currentPeriods = new double[monthsCount+1];
                    for (int m=0; m<monthsCount+1; m++)
                        currentPeriods[m] = m;//Tools.AddMonth(periodStart, m);

                    double[] currentBudget = new double[monthsCount+1];
                    double oldBudget = 0;
                    for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                        Date budgetMonth = DBTools.getCursorColumnValueDate(cursor, BudgetTableMetaData.FROM_DATE, Constants.DateFormatDB);
                        //currentPeriods[Tools.monthsBetween(periodStart, budgetMonth)] = Long.parseLong(Tools.DateToString(budgetMonth, Constants.DateFormatDB));
                        double budget = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.BUDGET);
                        double budgetDiffrence = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.BUDGET) - oldBudget;
                        currentBudget[Tools.monthsBetween(periodStart, budgetMonth)+1] = budget;//Diffrence;
                        oldBudget = budget;
                        maxValue = Math.max(maxValue, budgetDiffrence);
                        minValue = Math.min(minValue, budgetDiffrence);
                    }
                    periodValues.add(currentPeriods);
                    budgetValues.add(currentBudget);
                }
            }

            maxValue += Math.round(maxValue/10);
            minValue -= Math.round(maxValue/10);

            LinearLayout layout = (LinearLayout) findViewById(R.id.chart);
            if (mChartView != null) {
                try {
                    layout.removeView(mChartView);
                    renderer.removeAllRenderers();
                    dataset.clear();
                }
                catch (Exception e) {

                }
            }
            mChartView = repaintChart(minValue, maxValue, names, periodValues, budgetValues, periodStart, periodEnd);
            layout.addView(mChartView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }


    private GraphicalView repaintChart(double minValue, double maxValue, List<String> names, List<double[]> periodValues, List<double[]> budgetValues, Date periodStart, Date periodEnd) {
        int[] colors = ChartSrv.generateRandomColorsList(names.size());
        PointStyle[] styles = ChartSrv.generateRandomStyleList(names.size());
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
        for (int i = 0; i <= monthCount; i++)
        {
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
        renderer.setMargins(new int[]{margins.getTopMargin(),
                (String.valueOf(Math.round(maxValue)).length() + 2) * margins.getLeftMargin(), margins.getBottomMargin(),
                margins.getRightMargin()});

        String[] namesArray = new String[names.size()];
        namesArray = names.toArray(namesArray);

        dataset = buildDataset(namesArray, periodValues, budgetValues);
        //XYSeries series = dataset.getSeriesAt(0);
        return ChartFactory.getLineChartView(this, dataset, renderer);
    }

    public void myClickHandler(View target) {
        if (target.getTag() == btMenuCategoriesTag) {
            showCategoryDialogWindow();
            hideMenu();
        }
        else {
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
                                }
                                else
                                    DialogTools.toastDialog(BudgetedAmountByCategories.this, R.string.msgFromDateIsLessThanTo, Toast.LENGTH_LONG);
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
    }

    void hideMenu() {
        if (menu.getVisibility() == View.VISIBLE) {
            menu.setVisibility(View.INVISIBLE);
            scrollView.smoothScrollTo(menu.getMeasuredWidth(), 0);
            menuOut = false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK)
            if (requestCode == Constants.RequestCategoryForReport) {
                refreshList(categoriesList, periodStart, periodEnd);
                Tools.setPreference(BudgetedAmountByCategories.this, R.string.budgetGrowthCategIDsKey, Tools.convertListToString(categoriesList), false);
            }
    }

    private void showCategoryDialogWindow() {
        if (categoriesList == null)
            categoriesList = CategorySrv.generateMainCategoryIDs(this);
        Bundle bundle = new Bundle();
        bundle.putBoolean(Constants.dontRefreshValues, true);
        CheckBoxDialog.itemsList = categoriesList;
        bundle.putString(Constants.query, CategorySrv.mainCategoriesQueryForReports);
        bundle.putString(Constants.paramTitle, CategoryTableMetaData.NAME);
        bundle.putSerializable(Constants.paramValues, Tools.convertCheckBoxListToHashMap(categoriesList));
        Intent intent = new Intent(BudgetedAmountByCategories.this, CheckBoxDialog.class);
        intent.putExtras(bundle);
        startActivityForResult(intent, Constants.RequestCategoryForReport);
    }

    private void initializeViews() {
        LayoutInflater inflater = LayoutInflater.from(this);
        scrollView = (MyHorizontalScrollView) inflater.inflate(R.layout.horz_scroll_with_list_menu, null);

        setContentView(scrollView);

        //myApp = (MyApplicationLocal)getApplication();

        menu = inflater.inflate(R.layout.horz_scroll_menu, null);
        //app = inflater.inflate(R.layout.activitytest, null);
        app = inflater.inflate(R.layout.report_window, null);
        ViewGroup tabBar = (ViewGroup) app.findViewById(R.id.relATTop);

        btnSlide = (ImageView) tabBar.findViewById(R.id.btATMenu);
        btnSlide.setOnClickListener(new ClickListenerForScrolling(scrollView, menu));

        final View[] children = new View[] { menu, app };

        // Scroll to app (view[1]) when layout finished.
        int scrollToViewIdx = 1;
        scrollView.initViews(children, scrollToViewIdx, new SizeCallbackForMenu(btnSlide));

        menu.setOnTouchListener(mySwipeListener);

        LocalTools.addButtonToMenuList(this, R.string.categories, btMenuCategoriesTag);
        //app.setOnTouchListener(mySwipeListener);
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
            BudgetedAmountByCategories.menu.setVisibility(View.INVISIBLE);
            scrollView.smoothScrollTo(BudgetedAmountByCategories.menu.getMeasuredWidth(), 0);
            menuOut = false;
        }
        else {
            BudgetedAmountByCategories.menu.setVisibility(View.VISIBLE);
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
