package com.jgmoneymanager.reports;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.BudgetCategoriesTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.BudgetTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CategoryTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.main.R;
import com.jgmoneymanager.services.BudgetSrv;
import com.jgmoneymanager.services.ReportSrv;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import java.util.Date;

public class BudgetCompare extends MyActivity {

    public static boolean menuOut = false;

    ExpandableListView listView;
    ExpandableListAdapter mAdapter;

    Spinner spinnerYear;
    Spinner spinnerMonth;
    String years[] = new String[]{};

    Date selectedMonth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.budget_comp_rep);


        if (savedInstanceState != null) {
            selectedMonth = Tools.getDateFromBundle(savedInstanceState, "selectedMonth");
        } else
            selectedMonth = Tools.truncDate(Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncMonth);

        refreshDateButtonText(true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Tools.putToBundle(outState, "selectedMonth", selectedMonth);
    }

    public void refreshList() {
        String sql = "select c2." + CategoryTableMetaData._ID + ", c2." + CategoryTableMetaData.NAME + ", " +
                "ifnull(sum(" + BudgetCategoriesTableMetaData.BUDGET + "),0) " + BudgetCategoriesTableMetaData.BUDGET + " \n" +
                "from (select " + CategoryTableMetaData._ID + " catMainID, " + CategoryTableMetaData._ID + " catID, 1 is_main \n" +
                "\tfrom " + CategoryTableMetaData.TABLE_NAME + " where " + CategoryTableMetaData.MAINID + " is null  and " + CategoryTableMetaData.ISINCOME + " = 0 \n" +
                "\tunion all \n" +
                "\tselect " + CategoryTableMetaData.MAINID + ", " + CategoryTableMetaData._ID + ", 0 is_main \n" +
                "\tfrom " + CategoryTableMetaData.TABLE_NAME + " where " + CategoryTableMetaData.MAINID + " is not null and " + CategoryTableMetaData.ISINCOME + " = 0) c1 \n" +
                "join " + CategoryTableMetaData.TABLE_NAME + " c2 on c2." + CategoryTableMetaData._ID + " = c1.catMainID \n" +
                "left join (select b1.* from " + BudgetCategoriesTableMetaData.TABLE_NAME + " b1 \n" +
                "\tjoin " + BudgetTableMetaData.TABLE_NAME + " b2 on b2." + BudgetTableMetaData._ID + " = b1." + BudgetCategoriesTableMetaData.BUDGET_ID + " \n" +
                "\twhere b2." + BudgetTableMetaData.FROM_DATE + " = '" + Tools.DateToDBString(selectedMonth) + "')b on b." + BudgetCategoriesTableMetaData.CATEGORY_ID + " = c1.catID \n" +
                "group by c2." + CategoryTableMetaData._ID + ", c2." + CategoryTableMetaData.NAME + " order by " + CategoryTableMetaData.NAME;

        Cursor mGroupsCursor = DBTools.createCursor(BudgetCompare.this, sql);
        mAdapter = new MyExpandableListAdapter(mGroupsCursor, BudgetCompare.this,
                R.layout.group_budget_comp_row, R.layout.group_budget_comp_row,
                new String[]{CategoryTableMetaData.NAME, BudgetCategoriesTableMetaData.BUDGET},
                new int[]{R.id.tvCategory, R.id.tvCurrentValue},
                new String[]{CategoryTableMetaData.NAME, BudgetCategoriesTableMetaData.BUDGET/*, lastMonthColumn, averageColumn */},
                new int[]{R.id.tvCategory, R.id.tvCurrentValue/*, R.id.tvLastMonthValue, R.id.tvAverageValue */});

        listView = new ExpandableListView(BudgetCompare.this);
        listView.setAdapter(mAdapter);
        //listView.setGroupIndicator(context.getResources().getDrawable(R.drawable.expander_group));
        listView.setScrollingCacheEnabled(true);
        listView.setCacheColorHint(00000000);
        listView.setBackgroundColor(getResources().getColor(R.color.White));
        listView.setDivider(getResources().getDrawable(R.color.newThemeBlue));
        listView.setDividerHeight(Math.round(getResources().getDimension(R.dimen.main_round_button_side) / getResources().getDisplayMetrics().density));
        listView.setChildDivider(getResources().getDrawable(R.color.newThemeBlue));

        RelativeLayout myLayout = (RelativeLayout) findViewById(R.id.lay_budget_balance);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.FILL_PARENT,
                RelativeLayout.LayoutParams.FILL_PARENT);
        layoutParams.setMargins(Math.round(getResources().getDimension(R.dimen.combined_list_label_left_margin) / getResources().getDisplayMetrics().density), 0,
                Math.round(getResources().getDimension(R.dimen.combined_list_label_right_margin) / getResources().getDisplayMetrics().density), 0);
        listView.setLayoutParams(layoutParams);

        myLayout.addView(listView);

        registerForContextMenu(listView);
    }

    public class MyExpandableListAdapter extends SimpleCursorTreeAdapter {

        public MyExpandableListAdapter(Cursor cursor, Context context,
                                       int groupLayout, int childLayout, String[] groupFrom,
                                       int[] groupTo, String[] childrenFrom, int[] childrenTo) {
            super(BudgetCompare.this, cursor, groupLayout, groupFrom, groupTo, childLayout, childrenFrom, childrenTo);
        }

        @Override
        protected Cursor getChildrenCursor(Cursor cursor) {
            String sql = "select c." + CategoryTableMetaData._ID + ", c." +
                    CategoryTableMetaData.NAME + ", ifnull(" +
                    BudgetCategoriesTableMetaData.BUDGET + ",0) " + BudgetCategoriesTableMetaData.BUDGET + " \n" +
                    "from " + CategoryTableMetaData.TABLE_NAME + " c \n" +
                    "left join (select b1." + BudgetCategoriesTableMetaData.BUDGET + ", b1." + BudgetCategoriesTableMetaData.CATEGORY_ID + "\n from " +
                    BudgetCategoriesTableMetaData.TABLE_NAME +
                    " b1 \njoin " + BudgetTableMetaData.TABLE_NAME + " b on b1." + BudgetCategoriesTableMetaData.BUDGET_ID + " = b." +
                    BudgetTableMetaData._ID + " and b." + BudgetTableMetaData.FROM_DATE + " = '" + Tools.DateToDBString(selectedMonth) +
                    "') bc on c." + CategoryTableMetaData._ID + " = bc." + BudgetCategoriesTableMetaData.CATEGORY_ID + " \n" +
                    "where c." + CategoryTableMetaData.MAINID + " = " + DBTools.getCursorColumnValue(cursor, CategoryTableMetaData._ID) +
                    " order by c." + CategoryTableMetaData.NAME;
            return DBTools.createCursor(BudgetCompare.this, sql);
        }

        @Override
        public void notifyDataSetChanged(boolean releaseCursors) {
            super.notifyDataSetChanged(releaseCursors);
        }

        // I needed to process click on click of the button on child item
        public View getChildView(final int groupPosition,
                                 final int childPosition, boolean isLastChild, View convertView,
                                 ViewGroup parent) {
            View rowView = super.getChildView(groupPosition, childPosition,
                    isLastChild, convertView, parent);
            Cursor cursor = super.getChild(groupPosition, childPosition);
            long categoryID = DBTools.getCursorColumnValueLong(cursor, CategoryTableMetaData._ID);
            long defaultCurrencyID = BudgetSrv.getBudgetCurrencyID(BudgetCompare.this, selectedMonth);
            double currentBudget = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.BUDGET);

            TextView tvAverage = (TextView) rowView.findViewById(R.id.tvAverageValue);
            tvAverage.setText(Tools.formatDecimalInUserFormat(BudgetSrv.getCategoryAverageBudget(BudgetCompare.this, Tools.AddMonth(selectedMonth, -11),
                    selectedMonth, categoryID, defaultCurrencyID)));

            tvAverage = (TextView) rowView.findViewById(R.id.tvLastMonthValue);
            double lastBudgetValue = BudgetSrv.getBudget(BudgetCompare.this, categoryID, Tools.AddMonth(selectedMonth, -1), defaultCurrencyID);
            tvAverage.setText(Tools.formatDecimal(lastBudgetValue, "0"));

            ImageView imgStatus = (ImageView) rowView.findViewById(R.id.img_status);
            if (Double.compare(currentBudget, lastBudgetValue) < 0) {
                imgStatus.setImageResource(R.drawable.arrow_down_rep);
            } else if (Double.compare(currentBudget, lastBudgetValue) == 0) {
                imgStatus.setImageResource(R.drawable.minus);
            } else if (Double.compare(currentBudget, lastBudgetValue) > 0) {
                imgStatus.setImageResource(R.drawable.arrow_up_rep);
            }

            return rowView;
        }

        public View getGroupView(final int groupPosition, final boolean isExpanded,
                                 View convertView, ViewGroup parent) {
            View rowView = super.getGroupView(groupPosition, isExpanded, convertView, parent);
            rowView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (isExpanded)
                        listView.collapseGroup(groupPosition);
                    else
                        listView.expandGroup(groupPosition);
                }
            });

            Cursor cursor = super.getGroup(groupPosition);
            long categoryID = DBTools.getCursorColumnValueLong(cursor, CategoryTableMetaData._ID);
            long defaultCurrencyID = BudgetSrv.getBudgetCurrencyID(BudgetCompare.this, selectedMonth);
            double currentBudget = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.BUDGET);

            TextView tvAverage = (TextView) rowView.findViewById(R.id.tvAverageValue);
            tvAverage.setText(Tools.formatDecimalInUserFormat(BudgetSrv.getGroupAverageBudget(BudgetCompare.this, Tools.AddMonth(selectedMonth, -11),
                    selectedMonth, categoryID, defaultCurrencyID)));

            tvAverage = (TextView) rowView.findViewById(R.id.tvLastMonthValue);
            String lastBudgetValue = Tools.formatDecimalInUserFormat(BudgetSrv.getGroupBudget(BudgetCompare.this, Tools.AddMonth(selectedMonth, -1), categoryID, defaultCurrencyID));
            tvAverage.setText(lastBudgetValue);

            ImageView imgStatus = (ImageView) rowView.findViewById(R.id.img_status);
            if (Double.compare(currentBudget, Tools.parseDouble(lastBudgetValue)) < 0) {
                imgStatus.setImageResource(R.drawable.arrow_down_rep);
            } else if (Double.compare(currentBudget, Tools.parseDouble(lastBudgetValue)) == 0) {
                imgStatus.setImageResource(R.drawable.minus);
            } else if (Double.compare(currentBudget, Tools.parseDouble(lastBudgetValue)) > 0) {
                imgStatus.setImageResource(R.drawable.arrow_up_rep);
            }

            return rowView;
        }
    }

    public void myClickHandler(View target) {
        switch (target.getId()) {
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
                showDialog(0);
                break;
            default:
                break;
        }
    }

    protected Dialog onCreateDialog(int id) {
        final int minYear = BudgetSrv.getBudgetMinimumYear(this);
        int currentYear = Tools.getCurrentDate().getYear() + 1900;
        for (int i = minYear; i <= currentYear; i++)
            years = Tools.addElement(years, String.valueOf(i));
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

    void refreshDateButtonText(boolean refreshLists) {
        ((Button) findViewById(R.id.repBtDate)).setText(ReportSrv.getDateButtonText(this, Constants.ReportTimeInterval.Monthly.index(), selectedMonth, selectedMonth, false));
        if (refreshLists) {
            try {
                refreshList();
            } catch (Exception e) {
                //Log.e(e);
            }
        }
    }

}
