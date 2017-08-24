package com.jgmoneymanager.budget;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.BudgetGoalsTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CategoryTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.CheckBoxItem;
import com.jgmoneymanager.entity.Group;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.main.CategoryFilter;
import com.jgmoneymanager.main.R;
import com.jgmoneymanager.services.BudgetSrv;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class BudgetGoalsList extends MyActivity {

    private AdView adView;

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
        View child = inflater.inflate(R.layout.budgetgoalslist, null);
        mainLayout.addView(child, params);

        reloadList();

        ListView lv = (ListView) findViewById(R.id.goalsList);
        lv.setScrollingCacheEnabled(true);
        lv.setCacheColorHint(00000000);
        lv.setBackgroundColor(getResources().getColor(R.color.White));
        registerForContextMenu(lv);


        if ((getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                /*&& !Tools.proVersionExists(this)*/) {
            MobileAds.initialize(getApplicationContext(), "ca-app-pub-5995868530154544/1867118510");
            adView = new AdView(this);
            adView.setAdSize(AdSize.SMART_BANNER);
            adView.setAdUnitId("ca-app-pub-5995868530154544/1867118510");
            LinearLayout layout = (LinearLayout) findViewById(R.id.GoalLayout2);
            layout.addView(adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }
    }

    void reloadList() {
        String query = "Select bg." + BudgetGoalsTableMetaData._ID
                + ", ifnull(c2." + CategoryTableMetaData.NAME + ", '') || case when c2." + CategoryTableMetaData.NAME
                + " is not null then ' - ' else '' end || c." + CategoryTableMetaData.NAME + " " + CategoryTableMetaData.NAME
                + ", bg." + BudgetGoalsTableMetaData.CATEGORY_ID + ", bg." + BudgetGoalsTableMetaData.START_MONTH
                + ", bg." + BudgetGoalsTableMetaData.TARGET_MONTH + ", bg." + BudgetGoalsTableMetaData.TARGET_AMOUNT
                + ", bg." + BudgetGoalsTableMetaData.DESCRIPTION
                + " from " + BudgetGoalsTableMetaData.TABLE_NAME + " bg join "
                + CategoryTableMetaData.TABLE_NAME + " c on c." + CategoryTableMetaData._ID + " = bg." + BudgetGoalsTableMetaData.CATEGORY_ID
                + " left join " + CategoryTableMetaData.TABLE_NAME + " c2 on c2." + CategoryTableMetaData._ID + "= c." + CategoryTableMetaData.MAINID;
        Cursor cursor = DBTools.createCursor(this, query);
        String[] from = new String[]{CategoryTableMetaData.NAME, BudgetGoalsTableMetaData.TARGET_MONTH, BudgetGoalsTableMetaData.TARGET_AMOUNT,
                BudgetGoalsTableMetaData.DESCRIPTION};
        int[] to = new int[]{R.id.ed_category_name, R.id.ed_goal_target_month_value, R.id.ed_goal_target_amount_value, R.id.ed_goal_name};
        SimpleCursorAdapter notes = new MyListAdapter(cursor, this, R.layout.listgoalsrow, from, to);
        ((ListView) findViewById(R.id.goalsList)).setAdapter(notes);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (adView != null)
                adView.destroy();
        } catch (Exception ex) {

        }
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        menu.setHeaderIcon(R.drawable.ic_menu_manage);
        menu.add(R.string.menuEdit);
        menu.add(R.string.menuDelete);
        Cursor cursor = (Cursor) ((ListView) findViewById(R.id.goalsList)).getAdapter().getItem(info.position);
        menu.setHeaderTitle(DBTools.getCursorColumnValue(cursor, BudgetGoalsTableMetaData.DESCRIPTION));
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (item.getTitle().toString().equals(getString(R.string.menuEdit))) {

            final Cursor cursor = getContentResolver().query(BudgetGoalsTableMetaData.CONTENT_URI, null, BudgetGoalsTableMetaData._ID + " = " + info.id,
                    null, null);
            cursor.moveToFirst();
            final Date oldDate = DBTools.getCursorColumnValueDate(cursor, BudgetGoalsTableMetaData.TARGET_MONTH);
            final double oldAmount = DBTools.getCursorColumnValueDouble(cursor, BudgetGoalsTableMetaData.TARGET_AMOUNT);
            final String oldDescription = DBTools.getCursorColumnValue(cursor, BudgetGoalsTableMetaData.DESCRIPTION);

            LayoutInflater li = LayoutInflater.from(this);
            View view = li.inflate(R.layout.budget_goal_set_dialog, null);

            ((TextView) view.findViewById(R.id.bgsTitle)).setText(R.string.editGoal);

            final EditText edTargetAmount = (EditText) view.findViewById(R.id.bgsTargetAmount);
            edTargetAmount.addTextChangedListener(new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (edTargetAmount.getText().toString().length() != 0)
                        try {
                            Double.parseDouble(edTargetAmount.getText().toString());
                        } catch (NumberFormatException e) {
                            DialogTools.toastDialog(BudgetGoalsList.this, R.string.msgInvalidNumber, Toast.LENGTH_SHORT);
                        }
                }
            });
            edTargetAmount.setText(Tools.formatDecimal(oldAmount));

            final EditText edDescription = (EditText) view.findViewById(R.id.bgsTargetDescription);
            edDescription.setText(oldDescription);
            final Button edTargetMonth = (Button) view.findViewById(R.id.bgsTargetMonth);
            edTargetMonth.setText(Tools.DateToString(oldDate, Constants.DateFormatUser));
            edTargetMonth.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DatePickerDialog datePickerDialog = new DatePickerDialog(BudgetGoalsList.this, new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                            Calendar cal = Calendar.getInstance();
                            cal.set(year, month, day);
                            Date newDate = cal.getTime();
                            edTargetMonth.setText(Tools.DateToString(newDate, Constants.DateFormatUser));
                        }
                    }, Integer.parseInt(Tools.DateToString(oldDate, "yyyy")),
                            Integer.parseInt(Tools.DateToString(Tools.AddMonth(oldDate, -1), "MM")),
                            Integer.parseInt(Tools.DateToString(oldDate, "dd")));
                    datePickerDialog.show();
                }
            });

            final AlertDialog dialog = DialogTools.CustomDialog(BudgetGoalsList.this, view);

            Command cmdOK = new Command() {
                @Override
                public void execute() {
                    Date targetMonth = Tools.StringToDate(edTargetMonth.getText().toString(), Constants.DateFormatUser);
                    double targetAmount = Tools.stringToDouble(BudgetGoalsList.this, edTargetAmount.getText().toString(), false);
                    String description = edDescription.getText().toString().trim();
                    if ((targetMonth == null) || (Double.compare(targetAmount, 0d) == 0) || (targetMonth.compareTo(Tools.getCurrentDate()) < 0))
                        DialogTools.toastDialog(BudgetGoalsList.this, R.string.msgIncorrectValues, Toast.LENGTH_SHORT);
                    else {
                        if ((oldDate.compareTo(targetMonth) != 0) || (Double.compare(oldAmount, targetAmount) != 0)
                                || !oldDescription.equals(description)) {
                            BudgetSrv.editGoal(BudgetGoalsList.this, DBTools.getCursorColumnValueLong(cursor, BudgetGoalsTableMetaData._ID),
                                    Tools.DateToDBString(targetMonth), Tools.formatDecimal(targetAmount), description);
                            reloadList();
                        }
                        dialog.dismiss();
                    }
                }
            };

            Command cmdCancel = new Command() {
                @Override
                public void execute() {
                    dialog.dismiss();
                }
            };

            DialogTools.setButtonActions(view, R.id.bgsSave, R.id.bgsCancel, cmdOK, cmdCancel);

            dialog.show();
        } else if (item.getTitle().toString().equals(getString(R.string.menuDelete))) {
            Command cmd = new Command() {
                @Override
                public void execute() {
                    getContentResolver().delete(BudgetGoalsTableMetaData.CONTENT_URI, BudgetGoalsTableMetaData._ID + " = " + info.id, null);
                    reloadList();
                }
            };
            AlertDialog dialog = DialogTools.confirmDialog(BudgetGoalsList.this, cmd, R.string.msgConfirm, R.string.msgDeleteItem);
            dialog.show();
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((resultCode == RESULT_OK) && (requestCode == Constants.RequestCategoryForBudgetGoal)) {
            int selectedCategoryID = 0;
            ArrayList<Group> categoriesList = CategoryFilter.group;
            for (int i = 0; i < categoriesList.size(); i++) {
                Group group = categoriesList.get(i);
                if (group.isChecked())
                    selectedCategoryID = group.getID();
                else {
                    List<CheckBoxItem> subCategories = group.getChildren();
                    for (int j = 0; j < subCategories.size(); j++) {
                        CheckBoxItem subCategory = subCategories.get(j);
                        if (subCategory.isSelected())
                            selectedCategoryID = subCategory.getID();
                    }
                }
                if (selectedCategoryID != 0)
                    break;
            }
            addGoal(BudgetGoalsList.this, selectedCategoryID);
        }
    }

    void addGoal(final Context context, final long categoryID) {
        LayoutInflater li = LayoutInflater.from(context);
        View view = li.inflate(R.layout.budget_goal_set_dialog, null);
        final EditText edTargetAmount = (EditText) view.findViewById(R.id.bgsTargetAmount);
        edTargetAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (edTargetAmount.getText().toString().length() != 0)
                    try {
                        Double.parseDouble(edTargetAmount.getText().toString());
                    } catch (NumberFormatException e) {
                        DialogTools.toastDialog(BudgetGoalsList.this, R.string.msgInvalidNumber, Toast.LENGTH_SHORT);
                    }
            }
        });
        final EditText edDescription = (EditText) view.findViewById(R.id.bgsTargetDescription);
        final Button edTargetMonth = (Button) view.findViewById(R.id.bgsTargetMonth);
        edTargetMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePickerDialog datePickerDialog = new DatePickerDialog(context, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                        Calendar cal = Calendar.getInstance();
                        cal.set(year, month, day);
                        Date newDate = cal.getTime();
                        edTargetMonth.setText(Tools.DateToString(newDate, Constants.DateFormatUser));
                    }
                }, Integer.parseInt(Tools.DateToString(Tools.getCurrentDate(), "yyyy")),
                        Integer.parseInt(Tools.DateToString(Tools.getCurrentDate(), "MM")),
                        Integer.parseInt(Tools.DateToString(Tools.getCurrentDate(), "dd")));
                datePickerDialog.show();
            }
        });

        final AlertDialog dialog = DialogTools.CustomDialog(context, view);

        Command cmdOK = new Command() {
            @Override
            public void execute() {
                String targetMonth = edTargetMonth.getText().toString();
                String targetAmount = edTargetAmount.getText().toString();
                String targetDescription = edDescription.getText().toString().trim();
                if ((targetAmount == null) || (targetMonth == null) || (Tools.UserDateToDBDate(targetMonth) == null)
                        || (Tools.stringToDouble(context, targetAmount, false).compareTo(0d) <= 0)
                        || (Tools.StringToDate(targetMonth, Constants.DateFormatUser).compareTo(Tools.getCurrentDate()) < 0))
                    DialogTools.toastDialog(context, R.string.msgIncorrectValues, Toast.LENGTH_SHORT);
                else {
                    BudgetSrv.addGoal(context, categoryID, Tools.DateToDBString(Tools.getCurrentDate()), Tools.UserDateToDBDate(targetMonth),
                            Tools.formatDecimalUser2DB(BudgetGoalsList.this, targetAmount), targetDescription);
                    dialog.dismiss();
                    reloadList();
                }
            }
        };

        Command cmdCancel = new Command() {
            @Override
            public void execute() {
                dialog.dismiss();
            }
        };

        DialogTools.setButtonActions(view, R.id.bgsSave, R.id.bgsCancel, cmdOK, cmdCancel);

        dialog.show();
    }

    public class MyListAdapter extends SimpleCursorAdapter {

        public MyListAdapter(Cursor cursor, Context context, int rowId,
                             String[] from, int[] to) {
            super(context, rowId, cursor, from, to);
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            TextView tvMonthlyMinimum = (TextView) view.findViewById(R.id.ed_goal_minimum_value);
            TextView tvTargetDate = (TextView) view.findViewById(R.id.ed_goal_target_month_value);

            Cursor cursor = (Cursor) super.getItem(position);
            Double targetAmount = DBTools.getCursorColumnValueDouble(cursor, BudgetGoalsTableMetaData.TARGET_AMOUNT);
            ((TextView) view.findViewById(R.id.ed_goal_target_amount_value)).setText(Tools.formatDecimalInUserFormat(targetAmount));
            Date targetMonth = DBTools.getCursorColumnValueDate(cursor, BudgetGoalsTableMetaData.TARGET_MONTH);
            tvTargetDate.setText(Tools.DBDateToUserDate(DBTools.getCursorColumnValue(cursor, BudgetGoalsTableMetaData.TARGET_MONTH)));
            final long categoryID = DBTools.getCursorColumnValueLong(cursor, BudgetGoalsTableMetaData.CATEGORY_ID);
            final Date currentMonth = Tools.truncDate(BudgetGoalsList.this, Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncMonth);
            StringBuilder sbRemainingBudget = new StringBuilder();
            StringBuilder sbUsedBudget = new StringBuilder();
            Double currentBudget = BudgetSrv.getBudget(BudgetGoalsList.this, categoryID, currentMonth, sbRemainingBudget, sbUsedBudget);
            Double usedAmount = Double.valueOf(sbUsedBudget.toString());
            Double remainingBudget = Double.valueOf(sbRemainingBudget.toString());
            StringBuilder sbMonthlyMinimum = new StringBuilder("");
            StringBuilder sbProgressValue = new StringBuilder("");

            ImageButton attentionButton = (ImageButton) view.findViewById(R.id.bt_goal_attention);
            if (!BudgetSrv.getBudgetGoalStatus(targetMonth, targetAmount,
                    currentBudget, usedAmount, remainingBudget, sbMonthlyMinimum, sbProgressValue))
                attentionButton.setVisibility(View.GONE);

            double monthlyMinimum = Double.valueOf(sbMonthlyMinimum.toString());
            tvMonthlyMinimum.setText(Tools.formatDecimalInUserFormat(monthlyMinimum));

            attentionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final EditText input = new EditText(BudgetGoalsList.this);
                    input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    Command cmd = new Command() {
                        @Override
                        public void execute() {
                            if (!Tools.isCorrectNumber(input.getText().toString()))
                                DialogTools.toastDialog(BudgetGoalsList.this, R.string.msgInvalidNumber, Toast.LENGTH_SHORT);
                            else if (Double.compare(Double.parseDouble(input.getText().toString()), 0d) < 0)
                                DialogTools.toastDialog(BudgetGoalsList.this, R.string.msgInvalidNumber, Toast.LENGTH_SHORT);
                            else {
                                BudgetSrv.addBudget(BudgetGoalsList.this, categoryID, currentMonth,
                                        Double.parseDouble(input.getText().toString()), -1);
                            }
                        }
                    };
                    AlertDialog informationDialog = DialogTools.InputDialog(BudgetGoalsList.this, cmd, R.string.msgGoalBudgetHasntAdded,
                            input, R.drawable.ic_menu_add, R.string.addBudget);
                    informationDialog.show();
                }
            });

            ProgressBar pbGoalAmount = (ProgressBar) view.findViewById(R.id.pb_goal_amount);
            pbGoalAmount.setMax(targetAmount.intValue());
            Double progressAmont = Double.valueOf(sbProgressValue.toString());
            pbGoalAmount.setProgress(Math.min(targetAmount.intValue(), progressAmont.intValue()));
            return view;
        }
    }

    public void myClickHandler(View target) {
        switch (target.getId()) {
            case R.id.btGoalAdd:
                Intent intent = new Intent(BudgetGoalsList.this, CategoryFilter.class);
                intent.putExtra(Constants.dontRefreshValues, false);
                intent.putExtra(Constants.disableMultiSelect, true);
                startActivityForResult(intent, Constants.RequestCategoryForBudgetGoal);
                break;
            case R.id.bt_goal_edit:
                openContextMenu(target);
                break;
            default:
                break;
        }
    }
}
