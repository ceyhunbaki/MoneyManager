package com.jgmoneymanager.services;

import android.content.Context;
import android.database.Cursor;

import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CategoryTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransactionsTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.AccountTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VTransactionViewMetaData;
import com.jgmoneymanager.mmlibrary.R;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.entity.ReportArray;
import com.jgmoneymanager.entity.ReportArrayItem;
import com.jgmoneymanager.tools.Tools;

import java.util.Date;

/**
 * Created by Ceyhun on 23.09.2015.
 */
public class ReportSrv {

    public static String generateCategorySQL(int reportType, Date startDate, Date endDate) {
        String fromPart,
                conditionPart;
        if (reportType == Constants.TransFTransaction.Income.index()) {
            fromPart = "c1." + CategoryTableMetaData._ID + ", c1." + CategoryTableMetaData.NAME;
            conditionPart = " and tr." + TransactionsTableMetaData.TRANSTYPE + " = " + String.valueOf(Constants.TransactionTypeIncome)
                    + " order by c1." + CategoryTableMetaData.DEFAULT_SORT_ORDER;;
        } else if (reportType == Constants.TransFTransaction.Expence.index()) {
            fromPart = "c2." + CategoryTableMetaData._ID + ", c2." + CategoryTableMetaData.NAME;
            conditionPart = " and tr." + TransactionsTableMetaData.TRANSTYPE + " = " + String.valueOf(Constants.TransactionTypeExpence)
                + " order by c2." + CategoryTableMetaData.DEFAULT_SORT_ORDER;;
        } else {
            fromPart = "c2." + CategoryTableMetaData._ID + ", c2." + CategoryTableMetaData.NAME;
            conditionPart = " order by c2." + CategoryTableMetaData.DEFAULT_SORT_ORDER;;
        }


        String sql = "select " + fromPart + ",  tr."
                + TransactionsTableMetaData.CURRENCYID + ", " + TransactionsTableMetaData.TRANSTYPE + ", "
                + TransactionsTableMetaData.TRANSDATE + ", " + TransactionsTableMetaData.AMOUNT
                + " from " + TransactionsTableMetaData.TABLE_NAME
                + " tr join " + CategoryTableMetaData.TABLE_NAME
                + " c1 on c1." + CategoryTableMetaData._ID + " = tr." + TransactionsTableMetaData.CATEGORYID
                + " join " + CategoryTableMetaData.TABLE_NAME + " c2 on c2." + CategoryTableMetaData._ID
                + " = c1." + CategoryTableMetaData.MAINID;
        sql += " where "
                + TransactionsTableMetaData.TRANSDATE + " >= '" + Tools.DateToDBString(startDate) + "' and "
                + TransactionsTableMetaData.TRANSDATE + " <= '" + Tools.DateToDBString(endDate) + "' " + conditionPart;

        return sql;
    }

    public static String generateAccountSQL(int reportType, Date startDate, Date endDate, boolean includeTransfers) {
        String sql = "select ac." + AccountTableMetaData._ID + " " + AccountTableMetaData._ID + ",ac."
                + AccountTableMetaData.NAME + " " + AccountTableMetaData.NAME + " , "
                + VTransactionViewMetaData.ACCOUNTID + " , tr."
                + VTransactionViewMetaData.CURRID + " " + TransactionsTableMetaData.CURRENCYID + " , "
                + VTransactionViewMetaData.TRANSTYPE + " , "
                + VTransactionViewMetaData.TRANSDATE + " , " + VTransactionViewMetaData.AMOUNT
                + " from " + VTransactionViewMetaData.VIEW_NAME
                + " tr join " + AccountTableMetaData.TABLE_NAME
                + " ac on ac." + AccountTableMetaData._ID + " = tr." + VTransactionViewMetaData.ACCOUNTID;
		/*if (!AccountList.showDisabled)
			sql += " and ac." + AccountTableMetaData.STATUS + " = 1 ";*/
        sql += " where 1=1 /*" + VTransactionViewMetaData.ISTRANSFER + " = 0 and*/ ";
        if (!includeTransfers)
            sql += " and " + VTransactionViewMetaData.ISTRANSFER + " = 0 ";
        sql += " and " + VTransactionViewMetaData.TRANSDATE + " >= '" + Tools.DateToDBString(startDate) + "' and "
                + VTransactionViewMetaData.TRANSDATE + " <= '" + Tools.DateToDBString(endDate) + "' ";

        if (reportType == Constants.TransFTransaction.Expence.index()) {
            sql += " and tr." + VTransactionViewMetaData.TRANSTYPE + " = " + String.valueOf(Constants.TransactionTypeExpence);
        }
        else if (reportType == Constants.TransFTransaction.Income.index()) {
            sql += " and tr." + VTransactionViewMetaData.TRANSTYPE + " = " + String.valueOf(Constants.TransactionTypeIncome);
        }
        sql += " order by " + AccountTableMetaData.DEFAULT_SORT_ORDER;
        return sql;
    }

    public static String getDateButtonText(Context context, int dateInterval, Date startDate, Date endDate, boolean twoLinesForLandscape) {
        String text = "";
        if (dateInterval == Constants.ReportTimeInterval.Daily.index())
            /*if (twoLinesForLandscape && (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE))
                text = Tools.DateToString(startDate, "dd") + ", \n" +
                        context.getResources().getStringArray(R.array.Months)[startDate.getMonth()] + ", \n" +
                        Tools.DateToString(startDate, "yyyy");
            else*/
                text = Tools.DateToString(startDate, Constants.DateFormatUser);
        else if (dateInterval == Constants.ReportTimeInterval.Weekly.index() || dateInterval == Constants.ReportTimeInterval.Custom.index())
            /*if (twoLinesForLandscape && (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE))
                text = Tools.DateToString(startDate, "dd") + "-\n" + Tools.DateToString(endDate, "dd") + "\n" +
                        context.getResources().getStringArray(R.array.Months)[endDate.getMonth()] + ", \n" +
                        Tools.DateToString(endDate, "yyyy");
            else*/
                text = Tools.DateToString(startDate, Constants.DateFormatUser) + "-" + Tools.DateToString(endDate, Constants.DateFormatUser);
        else if (dateInterval == Constants.ReportTimeInterval.Monthly.index())
            /*if (twoLinesForLandscape && (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE))
                text = context.getResources().getStringArray(R.array.Months)[endDate.getMonth()] + ", \n" +
                        Tools.DateToString(startDate, "yyyy");
            else*/
                text = context.getResources().getStringArray(R.array.Months)[endDate.getMonth()] + ", " +
                        Tools.DateToString(startDate, "yyyy");
        else if (dateInterval == Constants.ReportTimeInterval.Yearly.index())
            text = Tools.DateToString(startDate, "yyyy");
        return text;
    }

    public static Date getCurrentDate(Context context, int dateInterval) {
        if (dateInterval == Constants.ReportTimeInterval.Weekly.index())
            return Tools.truncDate(context, Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncWeek);
        else if (dateInterval == Constants.ReportTimeInterval.Monthly.index())
            return Tools.truncDate(context, Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncMonth);
        else if (dateInterval == Constants.ReportTimeInterval.Yearly.index())
            return Tools.truncDate(context, Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncYear);
        else
            return Tools.getCurrentDate();
    }

    public static Date addPeriod(boolean add, int dateInterval, Date startDate) {
        int value = add ? 1 : -1;
        if (dateInterval == Constants.ReportTimeInterval.Weekly.index())
            return Tools.AddDays(startDate, value * 7);
        else if (dateInterval == Constants.ReportTimeInterval.Monthly.index())
            return Tools.AddMonth(startDate, value);
        else if (dateInterval == Constants.ReportTimeInterval.Yearly.index())
            return Tools.AddMonth(startDate, value * 12);
        else if (dateInterval == Constants.ReportTimeInterval.Daily.index())
            return Tools.AddDays(startDate, value);
        else return startDate;
    }

    public static Date getEndDate(int dateInterval, Date startDate) {
        if (dateInterval == Constants.ReportTimeInterval.Weekly.index())
            return Tools.AddDays(startDate, 6);
        else if (dateInterval == Constants.ReportTimeInterval.Monthly.index())
            return Tools.AddDays(Tools.AddMonth(startDate, 1), -1);
        else if (dateInterval == Constants.ReportTimeInterval.Yearly.index())
            return Tools.AddDays(Tools.AddMonth(startDate, 12), -1);
        else if (dateInterval == Constants.ReportTimeInterval.Daily.index())
            return startDate;
        else
            return startDate;
    }

    public static ReportArray generateArray(Context context, int reportType, String sql,
                                            boolean addSum, boolean forCategory, String idColumn, String nameColumn) {
        Cursor cursor = DBTools.createCursor(context, sql);
        int repArraySize;
        if (forCategory)
            repArraySize = CategorySrv.getMainCategoryCount(context) + 2;
        else
            repArraySize = AccountSrv.getAccountCount(context) + 2;
        ReportArray repArray = new ReportArray(repArraySize);
        int position = 0;
        long oldID = 0;
        double sum = 0d;
        long defaultCurrencyID = CurrencySrv.getDefaultCurrencyID(context);
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            long currentID = DBTools.getCursorColumnValueLong(cursor, idColumn);
            if (oldID != currentID) {
                position++;
                oldID = currentID;
            }
            long currentCurrencyID = DBTools.getCursorColumnValueLong(cursor, TransactionsTableMetaData.CURRENCYID);
            double amount = DBTools.getCursorColumnValueDouble(cursor, TransactionsTableMetaData.AMOUNT);
            if (currentCurrencyID != defaultCurrencyID)
                amount = CurrRatesSrv.convertAmount(context, amount,
                        currentCurrencyID, defaultCurrencyID,
                        DBTools.getCursorColumnValueDate(cursor, TransactionsTableMetaData.TRANSDATE));
            if ((DBTools.getCursorColumnValueInt(cursor, TransactionsTableMetaData.TRANSTYPE) == Constants.TransactionTypeIncome) &&
                    (reportType != Constants.TransFTransaction.Expence.index())) {
                repArray.addItem(position, new ReportArrayItem(currentID,
                        DBTools.getCursorColumnValue(cursor, nameColumn), amount));
                sum += amount;
            }
            else if (DBTools.getCursorColumnValueInt(cursor, TransactionsTableMetaData.TRANSTYPE) == Constants.TransactionTypeExpence) {
                if (reportType == Constants.TransFTransaction.All.index()) {
                    repArray.addItem(position, new ReportArrayItem(currentID,
                            DBTools.getCursorColumnValue(cursor, nameColumn), -amount));
                    sum -= amount;
                }
                else if (reportType == Constants.TransFTransaction.Expence.index()) {
                    repArray.addItem(position, new ReportArrayItem(currentID,
                            DBTools.getCursorColumnValue(cursor, nameColumn), amount));
                    sum += amount;
                }
            }
        }
        DBTools.closeDatabase();
        if (addSum)
            repArray.addItem(repArray.getItemCount()-1, new ReportArrayItem(0, context.getResources().getString(R.string.Sum), sum));
        repArray.deleteEmtpyItems();

        repArray.roundValues();
        return repArray;
    }

    public static ReportArray generateArrayUniversal(Context context, String sql,
                                                     String idColumnAlias, String nameColumnAlias, String valueColumnAlias, int arraySize) {
        Cursor cursor = DBTools.createCursor(context, sql);
        ReportArray repArray = new ReportArray(arraySize+1);
        int position = 1;
        long oldID = 0;
        double sum = 0d;
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            repArray.addItem(position, new ReportArrayItem(DBTools.getCursorColumnValueLong(cursor, idColumnAlias),
                    DBTools.getCursorColumnValue(cursor, nameColumnAlias),
                    DBTools.getCursorColumnValueDouble(cursor, valueColumnAlias)));
            position++;
        }
        DBTools.closeDatabase();
        repArray.deleteEmtpyItems();

        repArray.roundValues();
        return repArray;
    }
}
