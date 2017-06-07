package com.jgmoneymanager.reports;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.jgmoneymanager.database.MoneyManagerProviderMetaData.AccountTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CategoryTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.entity.ReportArray;
import com.jgmoneymanager.entity.ReportArrayItem;
import com.jgmoneymanager.entity.ReportItemAdapter;
import com.jgmoneymanager.main.R;
import com.jgmoneymanager.main.TransactionList;
import com.jgmoneymanager.services.ReportSrv;
import com.jgmoneymanager.services.TransactionSrv;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class ListReport extends MyActivity

{
    int reportType = Constants.TransFTransaction.All.index();

    final int dateIntervalDailyDialogID = 1;
    final int dateIntervalWeeklyDialogID = 2;
    final int dateIntervalMonthlyDialogID = 3;
    final int dateIntervalYearlyDialogID = 4;
    final int dateIntervalCustomDialogID = 5;
    final int fromDateDialogID = 6;
    final int toDateDialogID = 7;

    private final int btSummaryMenuID = Menu.FIRST;
    private final int btExpenseMenuID = btSummaryMenuID + 1;
    private final int btIncomeMenuID = btSummaryMenuID + 2;
    private final int btTransferMenuID = btSummaryMenuID + 3;

    boolean clearDates = true;
    boolean includeTransfers = true;
    int currentDateInterval = Constants.ReportTimeInterval.Monthly.index();
    Date startDate, endDate;
    int minTransDate;
    String years[] = new String[]{};

    Button btIntervaltype;
    Button btDate;

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
            startDate = Tools.getDateFromBundle(savedInstanceState, "startDate");
            endDate = Tools.getDateFromBundle(savedInstanceState, "endDate");
            currentDateInterval = Tools.getIntegerFromBundle0(savedInstanceState, "currentDateInterval");
            reportType = Tools.getIntegerFromBundle0(savedInstanceState, "reportType");
            includeTransfers = Tools.getBooleanFromBundle(savedInstanceState, "includeTransfers");
        }

        initializeViews();

        minTransDate = TransactionSrv.getMinTransactionYear(ListReport.this);
        int currentYear = Tools.getCurrentDate().getYear() + 1900;
        for (int i = minTransDate; i <= currentYear; i++)
            years = Tools.addElement(years, String.valueOf(i));

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
        View child = inflater.inflate(R.layout.listreport, null);
        mainLayout.addView(child, params);
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
    }

    void refreshTitle() {
        String titleString = ReportList.getTitleString(ListReport.this, reportType);

        if (getIntent().getAction().equals(Constants.ActionViewAccountReport))
            setTitle(getResources().getString(R.string.accounts) + " - " + titleString);
        else if (getIntent().getAction().equals(Constants.ActionViewCategoryReport))
            setTitle(getResources().getString(R.string.categories) + " - " + titleString);
    }

    void refreshList() {
        generateArray();
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
                    bundle.putLong(Constants.paramAccountID, repArray.getItem(arg2 + 1).getItemID());
                else if (getIntent().getAction().equals(Constants.ActionViewCategoryReport))
                    bundle.putLong(Constants.paramCategory, repArray.getItem(arg2 + 1).getItemID());

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

    /*public class MyListAdapter extends SimpleCursorAdapter {

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
    }*/

    void generateArray() {
        String sql;
        if (getIntent().getAction().equals(Constants.ActionViewAccountReport)) {
            sql = ReportSrv.generateAccountSQL(reportType, startDate, endDate, includeTransfers);
            repArray = ReportSrv.generateArray(ListReport.this, reportType, sql, true, false,
                    AccountTableMetaData._ID, AccountTableMetaData.NAME);
        } else if (getIntent().getAction().equals(Constants.ActionViewCategoryReport)) {
            sql = ReportSrv.generateCategorySQL(reportType, startDate, endDate);
            repArray = ReportSrv.generateArray(ListReport.this, reportType, sql, true, true,
                    CategoryTableMetaData._ID, CategoryTableMetaData.NAME);
        }

        list = new ArrayList<ReportArrayItem>(repArray.getItemCount());

        for (int i = 1; i < repArray.getItemCount(); i++) {
            list.add(repArray.getItem(i));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_empty, menu);
        menu.add(0, btSummaryMenuID, btSummaryMenuID, R.string.menuSummary);
        menu.add(0, btExpenseMenuID, btExpenseMenuID, R.string.expences);
        menu.add(0, btIncomeMenuID, btIncomeMenuID, R.string.incomes);
        boolean viewAccountReports = false;
        try {
            viewAccountReports = getIntent().getAction().equals(Constants.ActionViewAccountReport);
        } catch (Exception e) {
        }
        if (viewAccountReports)
            menu.add(0, btTransferMenuID, btTransferMenuID, includeTransfers ? R.string.menuExcludeTransfers : R.string.menuIncludeTransfers);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == btSummaryMenuID)
            reportType = Constants.TransFTransaction.All.index();
        else if (id == btExpenseMenuID)
            reportType = Constants.TransFTransaction.Expence.index();
        else if (id == btIncomeMenuID)
            reportType = Constants.TransFTransaction.Income.index();
        else if (id == btTransferMenuID) {
            includeTransfers = !includeTransfers;
            item.setTitle(includeTransfers? R.string.menuExcludeTransfers : R.string.menuIncludeTransfers);
        }
        refreshList();
        return super.onOptionsItemSelected(item);
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
                        startDate = Tools.truncDate(ListReport.this, new Date(year - 1900, monthOfYear, dayOfMonth), Constants.DateTruncTypes.dateTruncWeek);
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
                startDate = ReportSrv.getCurrentDate(ListReport.this, currentDateInterval);
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
}
