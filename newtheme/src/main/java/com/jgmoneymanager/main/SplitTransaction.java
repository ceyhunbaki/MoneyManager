package com.jgmoneymanager.main;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.services.CategorySrv;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ceyhun on 15/12/2016.
 */

public class SplitTransaction extends MyActivity {

    public class SplitItem {
        int categoryButtonID;
        int layoutID;
        int amountViewID;
        long categoryID;
        double amount;

        SplitItem(int categoryButtonID, int layoutID, int amountViewID, long categoryID, double amount) {
            this.categoryButtonID = categoryButtonID;
            this.layoutID = layoutID;
            this.categoryID = categoryID;
            this.amount = amount;
            this.amountViewID = amountViewID;
        }
    }

    public static List<SplitItem> splitItemsList;
    boolean dontRefreshValue = false;
    int transactionType = Constants.TransactionTypeExpence;
    int activCategoryIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.split_categeories);

        Bundle bundle = getIntent().getExtras();
        if (bundle.containsKey(Constants.dontRefreshValues))
            dontRefreshValue = bundle.getBoolean(Constants.dontRefreshValues);
        if (bundle.containsKey(Constants.paramTransactionType))
            transactionType = bundle.getInt(Constants.paramTransactionType);

        if (savedInstanceState != null)
            dontRefreshValue  = true;

        if (!dontRefreshValue) {
            splitItemsList = new ArrayList<>();
            reloadButtonIDs();
        }
        else {
            if (splitItemsList == null) {
                splitItemsList = new ArrayList<>();
                reloadButtonIDs();
            }
            else
                reloadItems();
        }

    }

    public void myClickHandler(View target) {
        switch (target.getId()) {
            case R.id.btSplitCat1:
                activCategoryIndex = 0;
                openCategoryFilter();
                break;
            case R.id.btSplitCat2:
                activCategoryIndex = 1;
                openCategoryFilter();
                break;
            case R.id.btSplitCat3:
                activCategoryIndex = 2;
                openCategoryFilter();
                break;
            case R.id.btSplitCat4:
                activCategoryIndex = 3;
                openCategoryFilter();
                break;
            case R.id.btSplitCat5:
                activCategoryIndex = 4;
                openCategoryFilter();
                break;
            case R.id.btSplitCat6:
                activCategoryIndex = 5;
                openCategoryFilter();
                break;
            case R.id.btSplitCat7:
                activCategoryIndex = 6;
                openCategoryFilter();
                break;
            case R.id.btSplitCat8:
                activCategoryIndex = 7;
                openCategoryFilter();
                break;
            case R.id.btSplitCat9:
                activCategoryIndex = 8;
                openCategoryFilter();
                break;
            case R.id.btSplitCat10:
                activCategoryIndex = 9;
                openCategoryFilter();
                break;
            case R.id.ibSplitDel1:
                deleteRow(0);
                break;
            case R.id.ibSplitDel2:
                deleteRow(1);
                break;
            case R.id.ibSplitDel3:
                deleteRow(2);
                break;
            case R.id.ibSplitDel4:
                deleteRow(3);
                break;
            case R.id.ibSplitDel5:
                deleteRow(4);
                break;
            case R.id.ibSplitDel6:
                deleteRow(5);
                break;
            case R.id.ibSplitDel7:
                deleteRow(6);
                break;
            case R.id.ibSplitDel8:
                deleteRow(7);
                break;
            case R.id.ibSplitDel9:
                deleteRow(8);
                break;
            case R.id.ibSplitDel10:
                deleteRow(9);
                break;
            case R.id.btSplitAdd:
                if (getVisibleItemsCount() >= 10)
                    DialogTools.toastDialog(this, R.string.min2Max10, Toast.LENGTH_SHORT);
                else
                    for (int i=0; i<splitItemsList.size(); i++) {
                        if ((findViewById(splitItemsList.get(i).layoutID).getVisibility() == View.GONE)) {
                            findViewById(splitItemsList.get(i).layoutID).setVisibility(View.VISIBLE);
                            break;
                        }
                    }
                break;
            case R.id.btSplitCancel:
                setResult(RESULT_CANCELED);
                finish();
                break;
            case R.id.btSplitOk:
                int normalRowsCount = 0;
                for (int i=0; i<splitItemsList.size(); i++) {
                    String amount = ((TextView)findViewById(splitItemsList.get(i).amountViewID)).getText().toString();
                    splitItemsList.get(i).amount = Tools.stringToDouble(SplitTransaction.this, amount, false);
                    SplitItem splitItem = splitItemsList.get(i);
                    if ((splitItem.categoryID != 0) && (Double.compare(splitItem.amount, 0d) != 0))
                        normalRowsCount++;
                }
                if (normalRowsCount < 2)
                    DialogTools.toastDialog(this, R.string.min2Max10, Toast.LENGTH_SHORT);
                else {
                    setResult(RESULT_OK);
                    finish();
                }
                break;
            case R.id.ibSplitCalc1:
                calcButtonClickAction(0);
                break;
            case R.id.ibSplitCalc2:
                calcButtonClickAction(1);
                break;
            case R.id.ibSplitCalc3:
                calcButtonClickAction(2);
                break;
            case R.id.ibSplitCalc4:
                calcButtonClickAction(3);
                break;
            case R.id.ibSplitCalc5:
                calcButtonClickAction(4);
                break;
            case R.id.ibSplitCalc6:
                calcButtonClickAction(5);
                break;
            case R.id.ibSplitCalc7:
                calcButtonClickAction(6);
                break;
            case R.id.ibSplitCalc8:
                calcButtonClickAction(7);
                break;
            case R.id.ibSplitCalc9:
                calcButtonClickAction(8);
                break;
            case R.id.ibSplitCalc10:
                calcButtonClickAction(9);
                break;
            default:
                break;
        }
    }

    /**
     * Amount calculator button onClickAction
     * @param index button index, starts from 0
     */
    void calcButtonClickAction(int index) {
        Intent intent = new Intent(getBaseContext(), Calculator.class);
        Bundle bundle = new Bundle();
        if (Double.compare(splitItemsList.get(0).amount, 0) != 0)
            bundle.putDouble(Calculator.startupAmountKey, splitItemsList.get(index).amount);
        bundle.putInt(Constants.paramSplitTransactionIndex, index);
        intent.putExtras(bundle);
        intent.setAction(Intent.ACTION_PICK);
        if(getCurrentFocus()!=null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
        startActivityForResult(intent, Constants.RequestCalculatorForSplitTransaction);
    }

    void reloadButtonIDs() {
        SplitItem splitItem = new SplitItem(R.id.btSplitCat1, R.id.relSplitRow1, R.id.edSplitAmount1, 0, 0d);
        splitItemsList.add(splitItem);
        splitItem = new SplitItem(R.id.btSplitCat2, R.id.relSplitRow2, R.id.edSplitAmount2, 0, 0d);
        splitItemsList.add(splitItem);
        splitItem = new SplitItem(R.id.btSplitCat3, R.id.relSplitRow3, R.id.edSplitAmount3, 0, 0d);
        splitItemsList.add(splitItem);
        splitItem = new SplitItem(R.id.btSplitCat4, R.id.relSplitRow4, R.id.edSplitAmount4, 0, 0d);
        splitItemsList.add(splitItem);
        splitItem = new SplitItem(R.id.btSplitCat5, R.id.relSplitRow5, R.id.edSplitAmount5, 0, 0d);
        splitItemsList.add(splitItem);
        splitItem = new SplitItem(R.id.btSplitCat6, R.id.relSplitRow6, R.id.edSplitAmount6, 0, 0d);
        splitItemsList.add(splitItem);
        splitItem = new SplitItem(R.id.btSplitCat7, R.id.relSplitRow7, R.id.edSplitAmount7, 0, 0d);
        splitItemsList.add(splitItem);
        splitItem = new SplitItem(R.id.btSplitCat8, R.id.relSplitRow8, R.id.edSplitAmount8, 0, 0d);
        splitItemsList.add(splitItem);
        splitItem = new SplitItem(R.id.btSplitCat9, R.id.relSplitRow9, R.id.edSplitAmount9, 0, 0d);
        splitItemsList.add(splitItem);
        splitItem = new SplitItem(R.id.btSplitCat10, R.id.relSplitRow10, R.id.edSplitAmount10, 0, 0d);
        splitItemsList.add(splitItem);
    }

    int getVisibleItemsCount() {
        int result = 0;
        for (int i=0; i<splitItemsList.size(); i++) {
            if (findViewById(splitItemsList.get(i).layoutID).getVisibility() == View.VISIBLE)
                result++;
        }
        return result;
    }

    void deleteRow(int index) {
        if (getVisibleItemsCount() > 2) {
            findViewById(splitItemsList.get(index).layoutID).setVisibility(View.GONE);
            splitItemsList.get(index).categoryID = 0;
            splitItemsList.get(index).amount = 0d;
            ((Button) findViewById(splitItemsList.get(index).categoryButtonID)).setText(null);
            ((EditText) findViewById(splitItemsList.get(index).amountViewID)).setText(Tools.formatDecimal(0d));
        }
        else
            DialogTools.toastDialog(this, R.string.min2Max10, Toast.LENGTH_SHORT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == Constants.RequestCategoryForSplitTransaction) {
                int categoryID;
                Uri selectedUri = data.getData();
                Cursor cursor = this.managedQuery(selectedUri, null, null, null, null);
                cursor.moveToFirst();
                categoryID = Integer.parseInt(DBTools.getCursorColumnValue(cursor, MoneyManagerProviderMetaData.VCategoriesViewMetaData._ID));
                SplitItem splitItem = null;
                if (splitItemsList.size() > activCategoryIndex)
                    splitItem = splitItemsList.get(activCategoryIndex);
                else
                    splitItemsList.add(activCategoryIndex, splitItem);
                splitItem.categoryID = categoryID;
                splitItemsList.set(activCategoryIndex, splitItem);
                setCategoryButtonTitle(activCategoryIndex);
            }
            else if (requestCode == Constants.RequestCalculatorForSplitTransaction) {
                Bundle bundle = data.getExtras();
                if (bundle.containsKey(Constants.paramSplitTransactionIndex)) {
                    activCategoryIndex = bundle.getInt(Constants.paramSplitTransactionIndex);
                    splitItemsList.get(activCategoryIndex).amount = data.getDoubleExtra(Constants.calculatorValue, 0d);
                    ((EditText)findViewById(splitItemsList.get(activCategoryIndex).amountViewID)).setText(Tools.formatDecimal(splitItemsList.get(activCategoryIndex).amount));
                }
            }
        }
    }

    void openCategoryFilter() {
        Intent intent;
        if (transactionType == Constants.TransactionTypeIncome)
            intent = new Intent(getBaseContext(), CategoryListForIncome.class);
        else
            intent = new Intent(getBaseContext(), CategoryListForExpense.class);
        intent.setAction(Intent.ACTION_PICK);
        startActivityForResult(intent, Constants.RequestCategoryForSplitTransaction);
    }

    void setCategoryButtonTitle(int index) {
        Button button = (Button) findViewById(splitItemsList.get(index).categoryButtonID);
        if (button != null)
            button.setText(CategorySrv.getCategoryNameByID(this, (splitItemsList.get(activCategoryIndex)).categoryID));
    }

    void reloadItems() {
        if (splitItemsList != null)
            for (int i = 0; i < splitItemsList.size(); i++) {
                SplitItem splitItem = splitItemsList.get(i);
                if ((splitItem.categoryID != 0) || (Double.compare(splitItem.amount, 0d) != 0)) {
                    (findViewById(splitItem.layoutID)).setVisibility(View.VISIBLE);
                    ((Button) findViewById(splitItem.categoryButtonID)).setText(CategorySrv.getCategoryNameByID(this, splitItem.categoryID));
                    ((TextView) findViewById(splitItem.amountViewID)).setText(Tools.formatDecimal(splitItem.amount));
                }
            }
    }

}
