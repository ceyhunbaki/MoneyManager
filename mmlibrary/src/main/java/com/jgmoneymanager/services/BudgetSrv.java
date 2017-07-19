package com.jgmoneymanager.services;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.BudgetCategoriesTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.BudgetGoalsTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.BudgetTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CategoryTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.mmlibrary.R;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import java.util.Calendar;
import java.util.Date;

public class BudgetSrv {
	
	public static final String groupBudget = "groupbudget";
    private static final String positiveRemaining = "positiveremaining";
	public static final String groupRemaining = "groupremaining";
    private static final String childOverspent = "childOverspent";
	
	public static int getBudgetId(Context context, Date filterDate) {
		Cursor cursor = context.getContentResolver().query(BudgetTableMetaData.CONTENT_URI, 
				new String[] {BudgetTableMetaData._ID}, 
				BudgetTableMetaData.FROM_DATE + " = '" + Tools.DateToDBString(filterDate) + "'", null, null);
		if (cursor.moveToFirst())
			return DBTools.getCursorColumnValueInt(cursor, BudgetTableMetaData._ID);
		else
			return 0;
	}

	public static void updateBudgetUsedAmount(Context context, long categoryID, Date transDate, 
			Double amount, long currencyID) {
		if (categoryID != 0) {
			Cursor cursor = context.getContentResolver().query(BudgetTableMetaData.CONTENT_URI, 
					new String[] {BudgetTableMetaData._ID}, 
					"'" + Tools.DateToDBString(transDate) + "' between " + BudgetTableMetaData.FROM_DATE 
						+ " and " + BudgetTableMetaData.TO_DATE, null, null);
			if (cursor.moveToFirst()) {
				int budgetID = DBTools.getCursorColumnValueInt(cursor, BudgetTableMetaData._ID);
				addOrUpdateBudgetCategoriesUsedAmount(context, budgetID, categoryID, amount, currencyID, transDate);
				updateBudgetRemainingValues(context, categoryID, amount, currencyID, transDate);
			}
		}
		else {
			//eger kateqoriya yoxdursa onun umumi budcenin ustune gelek ve ya cixaq
			BudgetSrv.updateBudgetIncome(context, transDate, -amount, currencyID);
		}
	}
	
	/**
	 * If budget for transDate month exists, then we'll update all next months
	 * @param context
	 * @param transDate
	 * @param amount
	 * @param currencyID transaction's currency ID
	 */
	public static void updateBudgetIncome(Context context, Date transDate, Double amount, long currencyID) {
		Cursor cursor = context.getContentResolver().query(BudgetTableMetaData.CONTENT_URI,
				new String[]{BudgetTableMetaData._ID, BudgetTableMetaData.INCOME, BudgetTableMetaData.CURRENCY_ID},
				BudgetTableMetaData.FROM_DATE + " = '"
						+ Tools.DateToDBString(Tools.truncDate(context, transDate, Constants.DateTruncTypes.dateTruncMonth))
						+ "' ", null, null);
		if (cursor.moveToFirst()) {
			cursor.close();
			cursor = context.getContentResolver().query(BudgetTableMetaData.CONTENT_URI, 
					new String[] {BudgetTableMetaData._ID, BudgetTableMetaData.INCOME, BudgetTableMetaData.CURRENCY_ID}, 
					BudgetTableMetaData.TO_DATE + " >= '" + Tools.DateToDBString(transDate) + "' ", null, null);
			Double addedAmount;
			for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
				int budgetID = DBTools.getCursorColumnValueInt(cursor, BudgetTableMetaData._ID);
				long budgetCurrency = DBTools.getCursorColumnValueLong(cursor, BudgetTableMetaData.CURRENCY_ID);
				Double budgetIncome = DBTools.getCursorColumnValueDouble(cursor, BudgetTableMetaData.INCOME);
				if (currencyID != budgetCurrency) 
					addedAmount = budgetIncome + CurrRatesSrv.convertAmount(context, amount, currencyID, budgetCurrency, transDate);
				else 
					addedAmount = budgetIncome + amount;
				ContentValues values = new ContentValues();
				values.put(BudgetTableMetaData.INCOME, Tools.formatDecimal(addedAmount));
				context.getContentResolver().update(BudgetTableMetaData.CONTENT_URI, values, 
						BudgetTableMetaData._ID + " = " + String.valueOf(budgetID), null);
			}
		}
	}

    private static void addOrUpdateBudgetCategoriesUsedAmount(Context context, int budgetID, long categoryID,
			Double amount, long currencyID, Date transDate)
	{
		Cursor cursor = context.getContentResolver().query(BudgetCategoriesTableMetaData.CONTENT_URI, 
				new String[] {BudgetCategoriesTableMetaData._ID,  BudgetCategoriesTableMetaData.USED_AMOUNT,
					BudgetCategoriesTableMetaData.REMAINING}, 
				BudgetCategoriesTableMetaData.BUDGET_ID + " = " + budgetID + " and " 
					+ BudgetCategoriesTableMetaData.CATEGORY_ID + " = " + categoryID, null, null);
		Double addedAmount = amount;
		Long budgetCategTableId;
		int budgetCurrencyID = getBudgetCurrencyID(context, budgetID);
		if (budgetCurrencyID != currencyID) 
			addedAmount = CurrRatesSrv.convertAmount(context, amount, currencyID, budgetCurrencyID, transDate, true);
		if (cursor.moveToFirst()) {
			Double currentUsedAmount = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.USED_AMOUNT);
			ContentValues values = new ContentValues();
			values.put(BudgetCategoriesTableMetaData.USED_AMOUNT, currentUsedAmount + addedAmount);			
			budgetCategTableId = DBTools.getCursorColumnValueLong(cursor, BudgetCategoriesTableMetaData._ID);
			context.getContentResolver().update(BudgetCategoriesTableMetaData.CONTENT_URI, values, 
					BudgetCategoriesTableMetaData._ID + " = " + budgetCategTableId.toString(), null);
			controlMainCategoryBudget(context, categoryID, budgetID, transDate);
		}
		else {
			ContentValues values = new ContentValues();
			values.put(BudgetCategoriesTableMetaData.USED_AMOUNT, addedAmount);
			values.put(BudgetCategoriesTableMetaData.BUDGET_ID, budgetID);
			values.put(BudgetCategoriesTableMetaData.CATEGORY_ID, categoryID);
			values.put(BudgetCategoriesTableMetaData.REMAINING, Tools.formatDecimal(0d));
			values.put(BudgetCategoriesTableMetaData.BUDGET, 0);
			/*Uri insertedUri = */context.getContentResolver().insert(BudgetCategoriesTableMetaData.CONTENT_URI, values);
			//Cursor cursor1 = context.getContentResolver().query(insertedUri, null, null, null, null);
			//cursor1.moveToFirst();
			controlMainCategoryBudget(context, categoryID, budgetID, transDate);
		}
	}
	
	/**
	 * If the budget for current category is not enough, moves missing amount from main category budget
	 * @param context
	 * @param categoryID
	 * @param budgetID
	 */
    private static void controlMainCategoryBudget(Context context, long categoryID, int budgetID, Date operDate) {
		//yoxlayaq gorek bu kateqoriya budce qalmayibsa, amma main category uzre nese varsa kecirek buna
		Cursor cursor = context.getContentResolver().query(BudgetCategoriesTableMetaData.CONTENT_URI, 
				new String[] {BudgetCategoriesTableMetaData.BUDGET, BudgetCategoriesTableMetaData.REMAINING,
								BudgetCategoriesTableMetaData.USED_AMOUNT}, 
				BudgetCategoriesTableMetaData.BUDGET_ID + " = " + String.valueOf(budgetID)
					+ " and " + BudgetCategoriesTableMetaData.CATEGORY_ID + " = " + String.valueOf(categoryID), 
					null, null);
		cursor.moveToFirst();
		Double currentBudget = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.BUDGET)
				+ DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.REMAINING);
		Double currentUsedAmount = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.USED_AMOUNT);
		Double missingAmount = currentBudget - currentUsedAmount;
		if (missingAmount.compareTo(0d) < 0) {
			long mainCategoryID = CategorySrv.getMainCategoryID(context, categoryID);
			StringBuilder sbMainCategoryBudgetRowID = new StringBuilder();
			Double mainRemainingBudget = getRemainingBudgetByCategoryID(context, budgetID, mainCategoryID, sbMainCategoryBudgetRowID, false);
			if (mainRemainingBudget.compareTo(0d) > 0) {
				Double transferedAmount = mainRemainingBudget.compareTo(-missingAmount) >= 0 ? -missingAmount : mainRemainingBudget;
				//moveBudget(context, budgetID, transferedAmount, mainCategoryID, categoryID, operDate);

				BudgetSrv.moveBudget(context, Tools.truncDate(context, operDate, Constants.DateTruncTypes.dateTruncMonth), transferedAmount, mainCategoryID, categoryID);
			}
		}
	}

	/**
	 * Returns {@link BudgetCategoriesTableMetaData.BUDGET + REMAINING - USED_AMOUNT}
	 * @param context
	 * @param budgetID {@link BudgetCategoriesTableMetaData.BUDGET_ID}
	 * @param categoryID
	 * @param returnOnlyRemaining if true then returns only {@link BudgetCategoriesTableMetaData.REMAINING}
	 * @return
	 */
	private static Double getRemainingBudgetByCategoryID(Context context, int budgetID, long categoryID, StringBuilder rowID, boolean returnOnlyRemaining) {
		Cursor cursor = context.getContentResolver().query(BudgetCategoriesTableMetaData.CONTENT_URI,
				new String[] {BudgetCategoriesTableMetaData.BUDGET, BudgetCategoriesTableMetaData.REMAINING, BudgetCategoriesTableMetaData.USED_AMOUNT},
				BudgetCategoriesTableMetaData.BUDGET_ID + " = " + String.valueOf(budgetID)
						+ " and " + BudgetCategoriesTableMetaData.CATEGORY_ID + " = " + String.valueOf(categoryID),
				null, null);
		if (cursor.moveToFirst()) {
			if (rowID != null)
				rowID.append(DBTools.getCursorColumnValue(cursor, BudgetCategoriesTableMetaData._ID));
			double result;
			if (returnOnlyRemaining)
				result = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.REMAINING);
			else
				result = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.BUDGET)
						+ DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.REMAINING)
						- DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.USED_AMOUNT);
			return result;
		}
		else
			return 0d;
	}

	/*private static Double getRemainingBudgetByGroupCatID(Context context, int budgetID, long categoryGroupID, boolean returnOnlyRemaining) {
		Cursor cursor = context.getContentResolver().query(BudgetCategoriesTableMetaData.CONTENT_URI,
				new String[] {BudgetCategoriesTableMetaData.BUDGET, BudgetCategoriesTableMetaData.REMAINING, BudgetCategoriesTableMetaData.USED_AMOUNT},
				BudgetCategoriesTableMetaData.BUDGET_ID + " = " + String.valueOf(budgetID)
						+ " and (" + BudgetCategoriesTableMetaData.CATEGORY_ID + " in (select "
						+ CategoryTableMetaData._ID  + " from " + CategoryTableMetaData.TABLE_NAME + " where "
						+ CategoryTableMetaData.MAINID + " = " + String.valueOf(categoryGroupID) + ") or "
						+ BudgetCategoriesTableMetaData.CATEGORY_ID + " = " + categoryGroupID + ")",
				null, null);
		double result = 0d;
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			if (returnOnlyRemaining)
				result += DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.REMAINING);
			else
				result += DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.BUDGET)
						+ DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.REMAINING)
						- DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.USED_AMOUNT);
		}
		return result;
	}*/

	/**
	 * Returns {@link BUDGET + REMAINING - USED_AMOUNT}
	 * @param context
	 * @param budgetMonth {@link BudgetCategoriesTableMetaData.FROM_DATE}
	 * @param categoryID
	 * @return
	 */
	public static Double getRemainingBudgetByCategoryID(Context context, Date budgetMonth, long categoryID, StringBuilder rowID, boolean returnOnlyRemaining) {
		return getRemainingBudgetByCategoryID(context, getBudgetId(context, budgetMonth), categoryID, rowID, returnOnlyRemaining);
	}

	/**
	 * If transaction inserted updates remaining values of budgets greater than trans_date
	 * @param context
	 * @param categoryID
	 * @param amount updated value
	 * @param currencyID
	 * @param transDate
	 */
    private static void updateBudgetRemainingValues(Context context, long categoryID, Double amount,
			long currencyID, Date transDate) {
		Cursor cursor = context.getContentResolver().query(BudgetTableMetaData.CONTENT_URI, 
				new String[] {BudgetTableMetaData._ID, BudgetTableMetaData.CURRENCY_ID}, 
				BudgetTableMetaData.FROM_DATE + " > '" + Tools.DateToDBString(transDate) + "' ", null, 
				BudgetTableMetaData.FROM_DATE);
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			long budgetCurrencyID = DBTools.getCursorColumnValueLong(cursor, BudgetTableMetaData.CURRENCY_ID);
			subtractFromRemainingValues(context, categoryID, amount, currencyID, transDate, 
					DBTools.getCursorColumnValue(cursor, BudgetTableMetaData._ID), budgetCurrencyID);
		}
	}
	
	/**
	 * tapilmiw budce cedvelinden olan setre uygun verilmiw kateqoriya uzre budce teyin edilibse onun 
	 * qaliqigini update edek
	 * @param context
	 * @param categoryID
	 * @param amount
	 * @param currencyID
	 * @param transDate
	 * @param updatedBudgetRowID
	 * @param budgetCurrencyID
	 * @param isFromPrevMonth if true, then if amount is greater than zero will add to budget column, 
	 * else to remainig column. This option is for previous month remaining budgets.
	 */
    private static void subtractFromRemainingValues(Context context, long categoryID, Double amount,
			long currencyID, Date transDate, String updatedBudgetRowID, long budgetCurrencyID) {
		Double updatedAmount = amount;
		if (currencyID != budgetCurrencyID)
			updatedAmount = CurrRatesSrv.convertAmount(context, amount, currencyID, budgetCurrencyID, transDate, true);
		Cursor cursorCateg = context.getContentResolver().query(BudgetCategoriesTableMetaData.CONTENT_URI, 
				new String[] {BudgetCategoriesTableMetaData._ID, BudgetCategoriesTableMetaData.REMAINING}, 
				BudgetCategoriesTableMetaData.BUDGET_ID + " = " + updatedBudgetRowID + " and "
						+ BudgetCategoriesTableMetaData.CATEGORY_ID + " = " + String.valueOf(categoryID), 
				null, null);
		ContentValues values = new ContentValues();
		if (cursorCateg.moveToFirst()) {
			/*if (isFromPrevMonth && (amount.compareTo(0d) > 0))
				values.put(BudgetCategoriesTableMetaData.BUDGET, 
						Tools.formatDecimal(DBTools.getCursorColumnValueDouble(cursorCateg, BudgetCategoriesTableMetaData.BUDGET)
							+ updatedAmount));
			else*/ 
			values.put(BudgetCategoriesTableMetaData.REMAINING, 
				Tools.formatDecimal(DBTools.getCursorColumnValueDouble(cursorCateg, BudgetCategoriesTableMetaData.REMAINING)
					- updatedAmount));
			context.getContentResolver().update(BudgetCategoriesTableMetaData.CONTENT_URI, values, 
				BudgetCategoriesTableMetaData._ID + " = " 
					+ DBTools.getCursorColumnValue(cursorCateg, BudgetCategoriesTableMetaData._ID), null);
		}
		else {
			/*if (amount.compareTo(0d) < 0) {
				values.put(BudgetCategoriesTableMetaData.BUDGET, Tools.formatDecimal(-updatedAmount));
				values.put(BudgetCategoriesTableMetaData.REMAINING, Tools.formatDecimal(0d));
			}
			else {*/
				values.put(BudgetCategoriesTableMetaData.REMAINING, Tools.formatDecimal(-updatedAmount));
				values.put(BudgetCategoriesTableMetaData.BUDGET, Tools.formatDecimal(0d));
			//}
			values.put(BudgetCategoriesTableMetaData.BUDGET_ID, updatedBudgetRowID);
			values.put(BudgetCategoriesTableMetaData.CATEGORY_ID, categoryID);
			values.put(BudgetCategoriesTableMetaData.USED_AMOUNT, Tools.formatDecimal(0d));
			context.getContentResolver().insert(BudgetCategoriesTableMetaData.CONTENT_URI, values);
		}
	}

	public static int getBudgetCurrencyID(Context context, long budgetID) {
		Cursor cursor = context.getContentResolver().query(BudgetTableMetaData.CONTENT_URI,
				new String[] {BudgetTableMetaData.CURRENCY_ID},
				BudgetTableMetaData._ID + " = " + String.valueOf(budgetID), null, null);
		if (cursor.moveToFirst())
			return DBTools.getCursorColumnValueInt(cursor, BudgetTableMetaData.CURRENCY_ID);
		else
			return CurrencySrv.getDefaultCurrencyID(context);
	}

	public static int getBudgetCurrencyID(Context context, Date fromDate) {
		Cursor cursor = context.getContentResolver().query(BudgetTableMetaData.CONTENT_URI,
				new String[] {BudgetTableMetaData.CURRENCY_ID},
				BudgetTableMetaData.FROM_DATE + " = '" + Tools.DateToDBString(fromDate) + "'", null, null);
		if (cursor.moveToFirst())
			return DBTools.getCursorColumnValueInt(cursor, BudgetTableMetaData.CURRENCY_ID);
		else
			return CurrencySrv.getDefaultCurrencyID(context);
	}

	/**
	 * {@link BudgetCategoriesTableMetaData.BUDGET} + {@link BudgetCategoriesTableMetaData.REMAINING}
	 * 	- {@link BudgetCategoriesTableMetaData.USED_AMOUNT} for fiven parametres
	 * @param context
	 * @param categoryId {@link CategoryTableMetaData._ID}
	 * @param inDate
	 * @return
	 */
	public static Boolean getCategoryRemainingBudget(Context context, long categoryId, Date inDate, 
			StringBuilder resultValue, StringBuilder sbBudget, StringBuilder sbUsed) {
		if (categoryId == 0)
			return false;
		Double result = 0d;
		Double budgetValue = 0d;
		Double usedValue = 0d;
		Boolean hasBudget = false;
		String currencySign = "";
		Cursor cursorBudget = context.getContentResolver().query(BudgetTableMetaData.CONTENT_URI, 
				new String[] {BudgetTableMetaData._ID, BudgetTableMetaData.CURRENCY_ID}, 
				"'" + Tools.DateToDBString(inDate) + "' between " + BudgetTableMetaData.FROM_DATE 
					+ " and " + BudgetTableMetaData.TO_DATE, null, null);
		if (cursorBudget.moveToFirst()) {
			currencySign = CurrencySrv.getCurrencySignByID(context, 
					DBTools.getCursorColumnValueLong(cursorBudget, BudgetTableMetaData.CURRENCY_ID));
			Cursor cursor = context.getContentResolver().query(BudgetCategoriesTableMetaData.CONTENT_URI, 
					new String[] {BudgetCategoriesTableMetaData.BUDGET, BudgetCategoriesTableMetaData.USED_AMOUNT,
						BudgetCategoriesTableMetaData.REMAINING}, 
					BudgetCategoriesTableMetaData.BUDGET_ID + " = " 
						+ DBTools.getCursorColumnValue(cursorBudget, BudgetTableMetaData._ID)
						+ " and " + BudgetCategoriesTableMetaData.CATEGORY_ID + " = " + String.valueOf(categoryId), 
					null, null);
			if (cursor.moveToFirst()) {
				for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
					result += DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.BUDGET) 
						+ DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.REMAINING)
						- DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.USED_AMOUNT);
					budgetValue += DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.BUDGET) 
						+ DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.REMAINING);
					usedValue += DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.USED_AMOUNT);
				}
				hasBudget = true;
			}
			if (result.compareTo(0d) == 0) {
				String remainingColumn = "remainingColumn";
				cursor = generateGroupCursor(remainingColumn, 
						Tools.truncDate(context, inDate, Constants.DateTruncTypes.dateTruncMonth), context,
						CategorySrv.getMainCategoryID(context, categoryId), categoryId);
				if (cursor.moveToFirst()) {
					Double groupRemainingVal = DBTools.getCursorColumnValueDouble(cursor, groupBudget)
							+ DBTools.getCursorColumnValueDouble(cursor, groupRemaining);
					if (groupRemainingVal.compareTo(0d) > 0) {
						budgetValue += groupRemainingVal;
						result += groupRemainingVal;
					}
					hasBudget = true;
				}
			}
		}
		if ((resultValue != null) && (hasBudget)) {
			resultValue.append(Tools.formatDecimalInUserFormat(result)).append(currencySign);
			sbBudget.append(Tools.formatDecimalInUserFormat(budgetValue));
			sbUsed.append(Tools.formatDecimalInUserFormat(usedValue));
		}
		return hasBudget;
	}
			
	/**
	 * 
	 * @param remainingColumn column alias
	 * @param selectedMonth
	 * @param context
	 * @param mainCategoryID if this parametre added it return cursor for only this id
	 * @param excludedCategoryID if this parametre added it will not added to summary
	 * @return
	 */
	public static Cursor generateGroupCursor(String remainingColumn, Date selectedMonth, Context context, 
			long mainCategoryID, long excludedCategoryID) {
		String categMainIDColName = "catMainID";
		String categIDColName = "catID";
		String mainSQL = "select c2." + CategoryTableMetaData._ID + ", c2." + CategoryTableMetaData.NAME
			+ ", max(b." + BudgetCategoriesTableMetaData.REPEAT + ") " + BudgetCategoriesTableMetaData.REPEAT
			+ ", ifnull(sum(" + BudgetCategoriesTableMetaData.BUDGET + "),0) " + BudgetCategoriesTableMetaData.BUDGET
			+ ", ifnull(sum(case when is_main = 1 then " + BudgetCategoriesTableMetaData.BUDGET 
				+ " else 0 end),0) " + groupBudget
			+ ", ifnull(sum(" + BudgetCategoriesTableMetaData.USED_AMOUNT + "),0) " + BudgetCategoriesTableMetaData.USED_AMOUNT 
			+ ", ifnull(sum(" + BudgetCategoriesTableMetaData.REMAINING + "),0) " + BudgetCategoriesTableMetaData.REMAINING 
			+ ", ifnull(sum(case when " + BudgetCategoriesTableMetaData.REMAINING + " > 0 then " 
			+ BudgetCategoriesTableMetaData.REMAINING + " else 0 end),0) " + positiveRemaining 
			+ ", ifnull(sum(case when is_main = 1 then " + BudgetCategoriesTableMetaData.REMAINING 
				+ " else 0 end),0) " + groupRemaining 
			+ ", ifnull(sum(case when is_main = 0 and cast(" + BudgetCategoriesTableMetaData.USED_AMOUNT + " as integer) > " 
				+ BudgetCategoriesTableMetaData.BUDGET + " + " + BudgetCategoriesTableMetaData.REMAINING + " then " 
				+ BudgetCategoriesTableMetaData.BUDGET + " - " 
				+ BudgetCategoriesTableMetaData.USED_AMOUNT + " + " + BudgetCategoriesTableMetaData.REMAINING 
				+ " else 0 end),0) " + childOverspent 
			+ ", ifnull(sum(" + BudgetCategoriesTableMetaData.BUDGET + " + " + BudgetCategoriesTableMetaData.REMAINING
			+ " - " + BudgetCategoriesTableMetaData.USED_AMOUNT + "),0) " + remainingColumn
			+ " from (select " + CategoryTableMetaData._ID + " " + categMainIDColName 
			+ ", " + CategoryTableMetaData._ID + " " + categIDColName + ", 1 is_main from " 
			+ CategoryTableMetaData.TABLE_NAME + " where " + CategoryTableMetaData.MAINID + " is null "
			+ " and " + CategoryTableMetaData.ISINCOME + " = 0 "
			+ "union all select " + CategoryTableMetaData.MAINID + ", " + CategoryTableMetaData._ID + ", 0 is_main from "
			+ CategoryTableMetaData.TABLE_NAME + " where " + CategoryTableMetaData.MAINID 
			+ " is not null and " + CategoryTableMetaData.ISINCOME + " = 0) c1 join " 
			+ CategoryTableMetaData.TABLE_NAME + " c2 on c2." + CategoryTableMetaData._ID + " = c1." + categMainIDColName;
		if (mainCategoryID != 0) 
			mainSQL += " and c2." + CategoryTableMetaData._ID + " = " + String.valueOf(mainCategoryID);
		if (excludedCategoryID != 0) 
			mainSQL += " and c1." + categIDColName + " != " + String.valueOf(excludedCategoryID);
		mainSQL += " left join (select b1.* from " 
			+ BudgetCategoriesTableMetaData.TABLE_NAME + " b1 join " + BudgetTableMetaData.TABLE_NAME + " b2 on b2."
			+ BudgetTableMetaData._ID + " = b1." + BudgetCategoriesTableMetaData.BUDGET_ID + " where " 
			+ BudgetTableMetaData.FROM_DATE + " = '" + Tools.DateToDBString(selectedMonth) 				
			+ "')b on b." + BudgetCategoriesTableMetaData.CATEGORY_ID 
			+ " = c1." + categIDColName+ " group by c2." + CategoryTableMetaData._ID + ", c2." 
			+ CategoryTableMetaData.NAME + " order by " + CategoryTableMetaData.NAME ;
		return DBTools.createCursor(context, mainSQL);
	}
	
	public static boolean getBudgetValues(Context context, Date fromDate, Date toDate, 
			StringBuilder notBudgeted, StringBuilder remaining, StringBuilder overspent,
			StringBuilder budgeted, StringBuilder income,
			String remainingColumnName) {
		boolean result = false;
		Double remainingValue = 0d;
		Double overspentValue = 0d;
		Double notBudgetedValue = 0d;
		Double incomeValue = 0d;
		Double budget = 0d;
		Cursor cursor = context.getContentResolver().query(BudgetTableMetaData.CONTENT_URI, 
				new String[] {BudgetTableMetaData._ID, BudgetTableMetaData.INCOME}, 
				BudgetTableMetaData.FROM_DATE + " >= '" + Tools.DateToDBString(fromDate) 
					+ "' and " + BudgetTableMetaData.TO_DATE + " <= '" + Tools.DateToDBString(toDate) + "' ", null, 
				BudgetTableMetaData._ID + " desc ");
		if (cursor.moveToFirst()) {
			result = true;
			incomeValue = DBTools.getCursorColumnValueDouble(cursor, BudgetTableMetaData.INCOME);

			Cursor cursorOverspent = BudgetSrv.generateGroupCursor(remainingColumnName, fromDate, context, 0, 0);
			for (cursorOverspent.moveToFirst(); !cursorOverspent.isAfterLast(); cursorOverspent.moveToNext()) {
				//Double currentRemaining = DBTools.getCursorColumnValueDouble(cursorOverspent, remainingColumnName);
				budget += DBTools.getCursorColumnValueDouble(cursorOverspent, BudgetCategoriesTableMetaData.BUDGET)
						+ DBTools.getCursorColumnValueDouble(cursorOverspent, BudgetCategoriesTableMetaData.REMAINING);
				overspentValue += DBTools.getCursorColumnValueDouble(cursorOverspent, childOverspent);	
				remainingValue += Tools.negativeToZero(DBTools.getCursorColumnValueDouble(cursorOverspent, BudgetCategoriesTableMetaData.BUDGET)
						+ DBTools.getCursorColumnValueDouble(cursorOverspent, BudgetCategoriesTableMetaData.REMAINING)
						- DBTools.getCursorColumnValueDouble(cursorOverspent, BudgetCategoriesTableMetaData.USED_AMOUNT));
						//budget + Tools.negativeToZero(oldRemaining) - used;
				/*if (Double.compare(currentRemaining, 0d) < 0d)
					overspentValue += currentRemaining;
				else if (Double.compare(currentRemaining, 0d) > 0d)
					remainingValue += currentRemaining;*/
			}
			notBudgetedValue = incomeValue - budget;
		}
		budgeted.append(Tools.formatDecimalInUserFormat(budget));
		income.append(Tools.formatDecimalInUserFormat(incomeValue));
		remaining.append(Tools.formatDecimalInUserFormat(remainingValue));
		overspent.append(Tools.formatDecimalInUserFormat(overspentValue));
		notBudgeted.append(Tools.formatDecimalInUserFormat(notBudgetedValue));
		return result;
	}

	public static int getBudgetMinimumYear(Context context) {
		try {
			return getBudgetMinimumDate(context).getYear() + 1900;
		}
		catch (Exception e) {
			return Integer.parseInt(Tools.DateToString(Tools.getCurrentDate(), Constants.DateFormatYear));
		}
	}

	public static Date getBudgetMinimumDate(Context context) {
		Cursor cursor = DBTools.createCursor(context, "Select min(" + BudgetTableMetaData.FROM_DATE + ") "
				+ BudgetTableMetaData.FROM_DATE + " from " + BudgetTableMetaData.TABLE_NAME);
		if (cursor.moveToFirst()) {
			Date minDate = DBTools.getCursorColumnValueDate(cursor, BudgetTableMetaData.FROM_DATE);
			if (minDate != null)
				return minDate;
			else return Calendar.getInstance().getTime();
		}
		else
			return Calendar.getInstance().getTime();
	}

	public static boolean isBudgetAvialable(Context context, Date monthStartDate) {
		Cursor cursor = context.getContentResolver().query(BudgetTableMetaData.CONTENT_URI, null,
				BudgetTableMetaData.FROM_DATE + " = '" + Tools.DateToDBString(monthStartDate) + "'", null, null);
		boolean resultValue = cursor.moveToFirst();
		cursor.close();
		return resultValue;
	}

	/**
	 * Returns account balance for current date + expense transactions sum for selected period
     */
	public static Double getTotalAmountForBudget(Context context, Date monthStart, Date monthEnd) {
		/*return AccountSrv.getBalanceForDate(context, 0, Tools.getCurrentDate(), Constants.defaultCurrency)
				+ TransactionSrv.getTransactionSum(context, "0", "0", monthStart, monthEnd,
				Constants.TransactionTypeExpence, Constants.defaultCurrency, false);*/
		return AccountSrv.getBalanceForDate(context, 0, Tools.AddDays(monthStart, -1), Constants.defaultCurrency, Tools.AddDays(monthStart, -1))
				+ TransactionSrv.getTransactionSum(context, "0", "0", monthStart, monthEnd, 
						Constants.TransactionTypeIncome, Constants.defaultCurrency, true)
				- TransactionSrv.getUnCategorizedTransactionsSum(context, monthStart, monthEnd, 
						Constants.TransactionTypeExpence, Constants.defaultCurrency);
	}
	
	/**
	 * This will work in first update and insertes first rows of budget
	 * @param context
	 */
    private static void insertFirstBudgetValues(Context context) {
		Date monthStart = Tools.truncDate(context, Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncMonth);
		Date monthEnd = Tools.lastDay(context, Tools.getCurrentDate());
		double totalBalance = getTotalAmountForBudget(context, monthStart, monthEnd);
		ContentValues values = new ContentValues();
		values.put(BudgetTableMetaData.CURRENCY_ID, Constants.defaultCurrency);
		values.put(BudgetTableMetaData.FROM_DATE, Tools.DateToDBString(monthStart));
		values.put(BudgetTableMetaData.TO_DATE, Tools.DateToDBString(monthEnd));
		values.put(BudgetTableMetaData.INCOME, Tools.formatDecimal(totalBalance));
		Uri insertedUri = context.getContentResolver().insert(BudgetTableMetaData.CONTENT_URI, values);
		
		Cursor cursor = context.getContentResolver().query(insertedUri, null, null, null, null);
		cursor.moveToFirst();
		long budgetID = DBTools.getCursorColumnValueLong(cursor, BudgetTableMetaData._ID);
		
		insertExpenses(context, budgetID, monthStart, monthEnd, Constants.defaultCurrency);		
	}
	
	/**
	 * insertes expense rows to {@link BudgetCategoriesTableMetaData}
	 * @param context
	 * @param budgetID {@link BudgetTableMetaData._ID}
	 * @param startPeriod
	 * @param endPeriod
	 * @param currencyID budget currency
	 */
    private static void insertExpenses(Context context, long budgetID, Date startPeriod, Date endPeriod, long currencyID) {
		Cursor cursor = context.getContentResolver().query(CategoryTableMetaData.CONTENT_URI,
				new String[]{CategoryTableMetaData._ID}, null, null, null);
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			Double expenseSum = TransactionSrv.getTransactionSum(context, "0", 
					DBTools.getCursorColumnValue(cursor, CategoryTableMetaData._ID), 
					startPeriod, endPeriod, Constants.TransactionTypeExpence, currencyID, false);
			if (expenseSum.compareTo(0d) != 0) {
				ContentValues values = new ContentValues();
				values.put(BudgetCategoriesTableMetaData.BUDGET, Tools.formatDecimal(0d));
				values.put(BudgetCategoriesTableMetaData.BUDGET_ID, budgetID);
				values.put(BudgetCategoriesTableMetaData.CATEGORY_ID, DBTools.getCursorColumnValue(cursor, CategoryTableMetaData._ID));
				values.put(BudgetCategoriesTableMetaData.REMAINING, Tools.formatDecimal(0d));
				values.put(BudgetCategoriesTableMetaData.USED_AMOUNT, Tools.formatDecimal(expenseSum));
				context.getContentResolver().insert(BudgetCategoriesTableMetaData.CONTENT_URI, values);
			}
		}
	}
	
	/**
	 * if budget for currenct month or in general not inserted yet, inserts remaining values and incomes.
	 * @param context
	 */
	public static boolean controlNewMonth(Context context) {
		boolean result = false;
		if (Constants.defaultCurrency > 0) {
			Date newMonth = Tools.truncDate(context, Tools.getCurrentDate(), Constants.DateTruncTypes.dateTruncMonth);
			Cursor cursor = context.getContentResolver().query(BudgetTableMetaData.CONTENT_URI, null,
					null, null, BudgetTableMetaData.FROM_DATE + " desc ");
			if (!cursor.moveToFirst()) {
				BudgetSrv.insertFirstBudgetValues(context);
				result = true;
			} else if (DBTools.getCursorColumnValue(cursor, BudgetTableMetaData.TO_DATE).compareTo(
					Tools.DateToDBString(Tools.getCurrentDate())) < 0) {
				Date lastBudgetedMonth = DBTools.getCursorColumnValueDate(cursor, BudgetTableMetaData.FROM_DATE);
				int monthsCount = Tools.monthsBetween(lastBudgetedMonth, newMonth);
				for (int i = 1; i <= monthsCount; i++) {
					Date currentNewMonth = Tools.AddMonth(lastBudgetedMonth, i);
					Date monthLastDay = Tools.lastDay(context, currentNewMonth);
					Double totalValue = getTotalAmountForBudget(context, currentNewMonth, monthLastDay);

					ContentValues values = new ContentValues();
					values.put(BudgetTableMetaData.CURRENCY_ID, Constants.defaultCurrency);
					values.put(BudgetTableMetaData.FROM_DATE, Tools.DateToDBString(currentNewMonth));
					values.put(BudgetTableMetaData.TO_DATE, Tools.DateToDBString(monthLastDay));
					values.put(BudgetTableMetaData.INCOME, totalValue);
					Uri insertedUri = context.getContentResolver().insert(BudgetTableMetaData.CONTENT_URI, values);

					cursor = context.getContentResolver().query(insertedUri, null, null, null, null);
					cursor.moveToFirst();
					long budgetID = DBTools.getCursorColumnValueLong(cursor, BudgetTableMetaData._ID);

					insertExpenses(context, budgetID, currentNewMonth, monthLastDay, Constants.defaultCurrency);
					movePrevRemainingsToCurMonth(context, currentNewMonth, budgetID);
				}
				result = true;
			}
		}
		return result;
	}
	
	/**
	 * Subtract amount from fromCategoryID and adds to toCategoryID. If amount is greater than current month, necessary amount will be moved from previous months
	 * @param context
	 * @param selectedMonth
	 * @param amount
	 * @param fromCategoryID
	 * @param toCategoryID
	 */
	public static void moveBudget(Context context, Date selectedMonth, Double amount, long fromCategoryID, 
			long toCategoryID) {
		/*addBudget(context, fromCategoryID, selectedMonth, -amount);
		addBudget(context, toCategoryID, selectedMonth, amount);*/
		Double remainingAmount = amount;

		String query = "select " + BudgetCategoriesTableMetaData.BUDGET + ", " + BudgetTableMetaData.FROM_DATE
				+ " from " + BudgetCategoriesTableMetaData.TABLE_NAME + " bc "
				+ " join " + BudgetTableMetaData.TABLE_NAME + " b1 on b1." + BudgetTableMetaData._ID + " = bc." + BudgetCategoriesTableMetaData.BUDGET_ID
				+ " where b1." + BudgetTableMetaData.FROM_DATE + " = '" + Tools.DateToDBString(selectedMonth) + "' and bc."
				//+ " where b1." + BudgetTableMetaData.FROM_DATE + " <= '" + Tools.DateToDBString(selectedMonth) + "' and bc."
				+ BudgetCategoriesTableMetaData.CATEGORY_ID + " = " + fromCategoryID
				+ " order by " + BudgetTableMetaData.FROM_DATE + " desc ";
		Cursor cursor = DBTools.createCursor(context, query);
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			Double currentBudget = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.BUDGET);
			Date currentMonth = DBTools.getCursorColumnValueDate(cursor, BudgetTableMetaData.FROM_DATE);
			if (remainingAmount.compareTo(0d) <= 0)
				break;
			//if (currentBudget.compareTo(remainingAmount) >= 0) {
				addBudget(context, fromCategoryID, currentMonth, -remainingAmount, -1);
				addBudget(context, toCategoryID, currentMonth, remainingAmount, -1);
			/*	break;
			}
			else if (currentBudget.compareTo(0d) > 0){
				addBudget(context, fromCategoryID, currentMonth, -currentBudget, -1);
				addBudget(context, toCategoryID, currentMonth, currentBudget, -1);
				remainingAmount = remainingAmount - currentBudget;
			}*/
		}
	}

	/**
	 * Returns only budget values for selected month
	 */
	public static Double getBudget(Context context, long categoryID, Date selectedMonth, StringBuilder remainingBudget, StringBuilder usedBudget) {
		Double result = 0d;
		Cursor cursor = context.getContentResolver().query(BudgetTableMetaData.CONTENT_URI,
				new String[] {BudgetTableMetaData._ID},
				BudgetTableMetaData.FROM_DATE + " = '" + Tools.DateToDBString(selectedMonth) + "'", null, null);
		if (cursor.moveToFirst()) {
			String budgetID = DBTools.getCursorColumnValue(cursor, BudgetTableMetaData._ID);
			cursor = context.getContentResolver().query(BudgetCategoriesTableMetaData.CONTENT_URI,
					new String[] {BudgetCategoriesTableMetaData._ID, BudgetCategoriesTableMetaData.BUDGET, BudgetCategoriesTableMetaData.REMAINING,
								BudgetCategoriesTableMetaData.USED_AMOUNT},
					BudgetCategoriesTableMetaData.CATEGORY_ID + " = " + categoryID + " and "
							+ BudgetCategoriesTableMetaData.BUDGET_ID + " = " + budgetID, null, null);
			if (cursor.moveToFirst()) {
				result = DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.BUDGET);
				if (remainingBudget != null)
					remainingBudget.append(String.valueOf(DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.REMAINING)));
				if (usedBudget != null)
					usedBudget.append(String.valueOf(DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.USED_AMOUNT)));
			}
			else {
				if (remainingBudget != null)
					remainingBudget.append("0");
				if (usedBudget != null)
					usedBudget.append("0");
			}
		}
		return result;
	}

	/**
	 * Returns only budget value for selected month in selected currency
     */
	public static Double getBudget(Context context, long categoryID, Date selectedMonth, long defaultCurrencyID) {
		Double result = 0d;
		Cursor cursor = context.getContentResolver().query(BudgetTableMetaData.CONTENT_URI,
				new String[] {BudgetTableMetaData._ID, BudgetTableMetaData.CURRENCY_ID},
				BudgetTableMetaData.FROM_DATE + " = '" + Tools.DateToDBString(selectedMonth) + "'", null, null);
		if (cursor.moveToFirst()) {
			String budgetID = DBTools.getCursorColumnValue(cursor, BudgetTableMetaData._ID);
			Long currencyID = DBTools.getCursorColumnValueLong(cursor, BudgetTableMetaData.CURRENCY_ID);
			cursor = context.getContentResolver().query(BudgetCategoriesTableMetaData.CONTENT_URI,
					new String[] {BudgetCategoriesTableMetaData._ID, BudgetCategoriesTableMetaData.BUDGET},
					BudgetCategoriesTableMetaData.CATEGORY_ID + " = " + categoryID + " and "
							+ BudgetCategoriesTableMetaData.BUDGET_ID + " = " + budgetID, null, null);
			if (cursor.moveToFirst())
				result = CurrRatesSrv.convertAmount(context, DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.BUDGET), currencyID, defaultCurrencyID, selectedMonth);
		}
		return result;
	}

	public static void changeBudgetTotalAmount(Context context, Date month, Double newTotalValue) {
		ContentValues values = new ContentValues();
		values.put(BudgetTableMetaData.INCOME, Tools.formatDecimal(newTotalValue));
		context.getContentResolver().update(BudgetTableMetaData.CONTENT_URI, values, 
				BudgetTableMetaData.FROM_DATE + " = '" + Tools.DateToDBString(month) + "' ", null);
	}

	/**
	 * returns the total budget for month
	 * @param context
	 * @param month
     * @return
     */
	public static Double getBudgetTotalAmount(Context context, Date month) {
		Double result = 0d;
		Cursor cursor = context.getContentResolver().query(BudgetTableMetaData.CONTENT_URI, 
				new String[] {BudgetTableMetaData.INCOME}, 
				BudgetTableMetaData.FROM_DATE + " = '" + Tools.DateToDBString(month) + "' ", null, null);
		if (cursor.moveToFirst())
			result = DBTools.getCursorColumnValueDouble(cursor, BudgetTableMetaData.INCOME);
		return result;
	}
	
	private static void movePrevRemainingsToCurMonth(Context context, Date currentMonth, long currentBudgetRowID) {
		Cursor cursor = context.getContentResolver().query(BudgetTableMetaData.CONTENT_URI, 
				new String[] {BudgetTableMetaData._ID, BudgetTableMetaData.CURRENCY_ID},
				BudgetTableMetaData.FROM_DATE + " = '" + Tools.DateToDBString(Tools.AddMonth(currentMonth, -1)) + "' ", 
				null, null);
		if (cursor.moveToFirst()) {
			String oldBudgetRowID = DBTools.getCursorColumnValue(cursor, BudgetTableMetaData._ID);
			long oldBudgetCurrencyID = DBTools.getCursorColumnValueLong(cursor, BudgetTableMetaData.CURRENCY_ID);
			String remainingColumn = "remCol";
			String query = "select " + BudgetCategoriesTableMetaData.CATEGORY_ID + ", " + BudgetCategoriesTableMetaData.REPEAT + ", "
					+ BudgetCategoriesTableMetaData.BUDGET + " + " + BudgetCategoriesTableMetaData.REMAINING 
					+ " - " + BudgetCategoriesTableMetaData.USED_AMOUNT + " " + remainingColumn 
					+ " from " + BudgetCategoriesTableMetaData.TABLE_NAME + " where "
					+ BudgetCategoriesTableMetaData.BUDGET_ID + " = " + oldBudgetRowID + " and ("
					+ BudgetCategoriesTableMetaData.BUDGET + " + " + BudgetCategoriesTableMetaData.REMAINING 
					+ " - " + BudgetCategoriesTableMetaData.USED_AMOUNT + " <> 0 or "
					+ BudgetCategoriesTableMetaData.REPEAT + " = 1)";
			cursor = DBTools.createCursor(context, query);
			for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
				long categoryID = DBTools.getCursorColumnValueLong(cursor, BudgetCategoriesTableMetaData.CATEGORY_ID);
				Double remaininValue = DBTools.getCursorColumnValueDouble(cursor, remainingColumn);
				subtractFromRemainingValues(context, categoryID, -remaininValue, oldBudgetCurrencyID, 
						currentMonth, String.valueOf(currentBudgetRowID), Constants.defaultCurrency);
				if (DBTools.getCursorColumnValueInt(cursor, BudgetCategoriesTableMetaData.REPEAT) == 1) {
					double newBudget;
					Date prevMonth = Tools.AddMonth(currentMonth, -1);
					if (CategorySrv.isGroupCategoryID(context, categoryID)) {
						newBudget = BudgetSrv.getGroupBudget(context, prevMonth, categoryID, Constants.defaultCurrency)
								/*+ Tools.negativeToZero(BudgetSrv.getRemainingBudgetByGroupCatID(context, getBudgetId(context, prevMonth), categoryID, true))*/;

					}
					else {
						newBudget = BudgetSrv.getBudget(context, categoryID, prevMonth, null, null)
								/*+ Tools.negativeToZero(BudgetSrv.getRemainingBudgetByCategoryID(context, prevMonth, categoryID, null, true))*/;
					}
					addBudget(context, categoryID, currentMonth, newBudget, 1);
				}
			}
		}
	}
	
	public static void resetBudget(Context context, long budgetID, Date selectedMonth) {
		if (budgetID != 0) {
			ContentValues values = new ContentValues();
			values.put(BudgetTableMetaData.INCOME, getTotalAmountForBudget(context, selectedMonth, Tools.lastDay(context, selectedMonth)));
			values.put(BudgetTableMetaData.CURRENCY_ID, Constants.defaultCurrency);
			context.getContentResolver().update(BudgetTableMetaData.CONTENT_URI, values, 
					BudgetTableMetaData._ID + " = " + String.valueOf(budgetID), null);
			context.getContentResolver().delete(BudgetCategoriesTableMetaData.CONTENT_URI, 
					BudgetCategoriesTableMetaData.BUDGET_ID + " = " + String.valueOf(budgetID), null);
			insertExpenses(context, budgetID, selectedMonth, Tools.lastDay(context, selectedMonth), Constants.defaultCurrency);
		}
	}

	/*public static void editBudget(Context context, long categoryID, Date selectedMonth, double budgetValue) {
		Cursor cursor = context.getContentResolver().query(BudgetTableMetaData.CONTENT_URI, 
				new String[] {BudgetTableMetaData._ID, BudgetTableMetaData.FROM_DATE}, 
				BudgetTableMetaData.FROM_DATE + " >= '" + Tools.DateToDBString(selectedMonth) + "'", 
				null, null);
		ContentValues values = new ContentValues();
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			long budgetID = DBTools.getCursorColumnValueLong(cursor, BudgetTableMetaData._ID);
			Cursor cursor2 = context.getContentResolver().query(BudgetCategoriesTableMetaData.CONTENT_URI, 
					new String[] {BudgetCategoriesTableMetaData._ID, BudgetCategoriesTableMetaData.BUDGET,
								BudgetCategoriesTableMetaData.REMAINING}, 
					BudgetCategoriesTableMetaData.BUDGET_ID + " = " + String.valueOf(budgetID) 
						+ " and " + BudgetCategoriesTableMetaData.CATEGORY_ID + " = " + String.valueOf(categoryID), 
						null, null);
			if (cursor2.moveToFirst()) {
				long budgetCategoryTableID = DBTools.getCursorColumnValueLong(cursor2, BudgetCategoriesTableMetaData._ID);
				if (selectedMonth.compareTo(DBTools.getCursorColumnValueDate(cursor, BudgetTableMetaData.FROM_DATE)) == 0) {
					Double oldBudget = DBTools.getCursorColumnValueDouble(cursor2, BudgetCategoriesTableMetaData.BUDGET);
					values.put(BudgetCategoriesTableMetaData.BUDGET, budgetValue);
				}
				else {
					Double oldRemaining = DBTools.getCursorColumnValueDouble(cursor2, BudgetCategoriesTableMetaData.REMAINING);
					values.put(BudgetCategoriesTableMetaData.REMAINING, oldRemaining + addedBudget);					
				}
				cursor2.close();
				context.getContentResolver().update(BudgetCategoriesTableMetaData.CONTENT_URI, values, 
					BudgetCategoriesTableMetaData._ID + " = " + String.valueOf(budgetCategoryTableID), null);
			}
			else {
				boolean setToRemaining = (selectedMonth.compareTo(DBTools.getCursorColumnValueDate(cursor, BudgetTableMetaData.FROM_DATE)) != 0);
				setBudget(context, categoryID, budgetID, addedBudget, setToRemaining);
			}
		}
	}*/

	/**
	 *
	 * @param context
	 * @param categoryID
	 * @param selectedMonth
	 * @param addedBudget
	 * @param budgetRepeat @{@link BudgetCategoriesTableMetaData.REPEAT} if value=-1 then it will not affect
     */
	public static void addBudget(Context context, long categoryID, Date selectedMonth, double addedBudget, int budgetRepeat) {
		Cursor cursor = context.getContentResolver().query(BudgetTableMetaData.CONTENT_URI, 
				new String[] {BudgetTableMetaData._ID, BudgetTableMetaData.FROM_DATE}, 
				BudgetTableMetaData.FROM_DATE + " >= '" + Tools.DateToDBString(selectedMonth) + "'", 
				null, null);
		ContentValues values = new ContentValues();
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			long budgetID = DBTools.getCursorColumnValueLong(cursor, BudgetTableMetaData._ID);
			Cursor cursor2 = context.getContentResolver().query(BudgetCategoriesTableMetaData.CONTENT_URI, 
					new String[] {BudgetCategoriesTableMetaData._ID, BudgetCategoriesTableMetaData.BUDGET,
								BudgetCategoriesTableMetaData.REMAINING}, 
					BudgetCategoriesTableMetaData.BUDGET_ID + " = " + String.valueOf(budgetID) 
						+ " and " + BudgetCategoriesTableMetaData.CATEGORY_ID + " = " + String.valueOf(categoryID), 
						null, null);
			if (cursor2.moveToFirst()) {
				values.clear();
				if (budgetRepeat != -1)
					values.put(BudgetCategoriesTableMetaData.REPEAT, budgetRepeat);
				long budgetCategoryTableID = DBTools.getCursorColumnValueLong(cursor2, BudgetCategoriesTableMetaData._ID);
				if (selectedMonth.compareTo(DBTools.getCursorColumnValueDate(cursor, BudgetTableMetaData.FROM_DATE)) == 0) {
					Double oldBudget = DBTools.getCursorColumnValueDouble(cursor2, BudgetCategoriesTableMetaData.BUDGET);
					values.put(BudgetCategoriesTableMetaData.BUDGET, Tools.formatDecimal(oldBudget + addedBudget));
				}
				else {
					Double oldRemaining = DBTools.getCursorColumnValueDouble(cursor2, BudgetCategoriesTableMetaData.REMAINING);
					values.put(BudgetCategoriesTableMetaData.REMAINING, Tools.formatDecimal(oldRemaining + addedBudget));
				}
				cursor2.close();
				context.getContentResolver().update(BudgetCategoriesTableMetaData.CONTENT_URI, values, 
					BudgetCategoriesTableMetaData._ID + " = " + String.valueOf(budgetCategoryTableID), null);
			}
			else {
				boolean setToRemaining = (selectedMonth.compareTo(DBTools.getCursorColumnValueDate(cursor, BudgetTableMetaData.FROM_DATE)) != 0);
				setBudget(context, categoryID, budgetID, addedBudget, setToRemaining, budgetRepeat);
			}
		}
	}
	
	/**
	 * updates {@link BudgetCategoriesTableMetaData.BUDGET_ID} or {@link BudgetCategoriesTableMetaData.REMAINING} value
	 * @param context
	 * @param categoryID
	 * @param budgetID
	 * @param budgetValue
	 * @param setToRemaining if true set then budgetValue will set as {@link BudgetCategoriesTableMetaData.REMAINING} else {@link BudgetCategoriesTableMetaData.BUDGET}
	 */
    private static void setBudget(Context context, long categoryID, long budgetID, double budgetValue, boolean setToRemaining, int budgetRepeat) {
		ContentValues values = new ContentValues();
		values.put(BudgetCategoriesTableMetaData.BUDGET_ID, budgetID);
		values.put(BudgetCategoriesTableMetaData.CATEGORY_ID, categoryID);
		values.put(BudgetCategoriesTableMetaData.REPEAT, Math.max(budgetRepeat, 0));
		if (!setToRemaining) {
			values.put(BudgetCategoriesTableMetaData.BUDGET, Tools.formatDecimal(budgetValue));
			values.put(BudgetCategoriesTableMetaData.REMAINING, Tools.formatDecimal(0d));
		}
		else {
			values.put(BudgetCategoriesTableMetaData.BUDGET, Tools.formatDecimal(0d));
			values.put(BudgetCategoriesTableMetaData.REMAINING, Tools.formatDecimal(budgetValue));
		}
		values.put(BudgetCategoriesTableMetaData.USED_AMOUNT, Tools.formatDecimal(0d));
		context.getContentResolver().insert(BudgetCategoriesTableMetaData.CONTENT_URI, values);
	}
	
	/*public static boolean haveNextMonthBudget(Context context, Date controlMonth) {
		Cursor cursor = context.getContentResolver().query(BudgetTableMetaData.CONTENT_URI, 
				new String[] {BudgetTableMetaData._ID}, 
				BudgetTableMetaData.FROM_DATE + " > '" + Tools.DateToDBString(controlMonth) + "'", 
				null, null);
		return cursor.moveToFirst();
	}*/

    public static void showBudgetUsageDialog(Context context, long budgetID, String categoryName, double usedAmount, double remaining, double currentBudget,
                                             double groupBudget, double groupRemaining, boolean isGroup) {
        String text = categoryName + Constants.newLine;
        String currencySign = getBudgetCurrencySign(context, budgetID);
        if (!isGroup) {
            text += context.getResources().getString(R.string.used) + ": " + Tools.getFullAmountText(usedAmount, currencySign, true) + ";" + Constants.newLine;
            text += context.getResources().getString(R.string.budget) + ": " + Tools.getFullAmountText(currentBudget, currencySign, true) + ";" + Constants.newLine;
            text += context.getResources().getString(R.string.remainingFromPrevMonth) + ": " + Tools.getFullAmountText(remaining, currencySign, true) + ";" + Constants.newLine;
            text += context.getResources().getString(R.string.remainingCurrent) + ": " + Tools.getFullAmountText(currentBudget + remaining - usedAmount, currencySign, true) + ";";
        }
        else {
            text += context.getResources().getString(R.string.used) + ": " + Tools.getFullAmountText(usedAmount, currencySign, true) + ";" + Constants.newLine;
            text += context.getResources().getString(R.string.budgetMainGroup) + ": " + Tools.getFullAmountText(groupBudget, currencySign, true) + ";" + Constants.newLine;
            text += context.getResources().getString(R.string.totalBudget) + ": " + Tools.getFullAmountText(currentBudget, currencySign, true) + ";" + Constants.newLine;
            text += context.getResources().getString(R.string.remainingFromPrevMonthMain) + ": " + Tools.getFullAmountText(groupRemaining, currencySign, true) + ";" + Constants.newLine;
            text += context.getResources().getString(R.string.remainingFromPrevMonthTotal) + ": " + Tools.getFullAmountText(remaining, currencySign, true) + ";" + Constants.newLine;
            text += context.getResources().getString(R.string.remainingCurrent) + ": " + Tools.getFullAmountText(currentBudget + remaining - usedAmount, currencySign, true) + ";";
        }
        AlertDialog dialog = DialogTools.informationDialog(context, R.string.status, text);
        dialog.show();
    }

    private static String getBudgetCurrencySign(Context context, long budgetID) {
        Cursor cursor = context.getContentResolver().query(BudgetTableMetaData.CONTENT_URI, new String[]{BudgetTableMetaData.CURRENCY_ID},
                BudgetTableMetaData._ID + " = " + String.valueOf(budgetID), null, null);
        if (cursor.moveToFirst())
            return CurrencySrv.getCurrencySignByID(context, DBTools.getCursorColumnValueLong(cursor, BudgetTableMetaData.CURRENCY_ID));
        else
            return CurrencySrv.getDefaultCurrencySign(context);
    }

	public static Double getGroupAverageBudget(Context context, Date fromDate, Date toDate, long categoryID, long defaulCurrencyID) {
		double result = 0;
		int rowCount = 0;
		String colulmSay = "say";
		String sql = "select " + CategoryTableMetaData._ID + ", " + CategoryTableMetaData.NAME + ", " + BudgetTableMetaData.CURRENCY_ID +
				", count(*) " + colulmSay + ", ifnull(sum(" + BudgetCategoriesTableMetaData.BUDGET + "),0) " + BudgetCategoriesTableMetaData.BUDGET + " \n" +
				"from(\n" +
				"select  c2." + CategoryTableMetaData._ID + ", c2." + CategoryTableMetaData.NAME + ", " + BudgetTableMetaData.CURRENCY_ID +
				", " + BudgetTableMetaData.FROM_DATE + ", ifnull(sum(" + BudgetCategoriesTableMetaData.BUDGET + "),0) " +
				BudgetCategoriesTableMetaData.BUDGET + " \n" +
				"from (select " + CategoryTableMetaData._ID + " catMainID, " + CategoryTableMetaData._ID + " catID, 1 is_main \n" +
				"\tfrom " + CategoryTableMetaData.TABLE_NAME + " where " + CategoryTableMetaData.MAINID + " is null and " +
				CategoryTableMetaData.ISINCOME + " = 0 \n" +
				"\tunion all \n" +
				"\tselect " + CategoryTableMetaData.MAINID + ", _id, 0 is_main \n" +
				"\tfrom " + CategoryTableMetaData.TABLE_NAME + " where " + CategoryTableMetaData.MAINID + " is not null and " +
				CategoryTableMetaData.ISINCOME + " = 0) c1 \n" +
				"join " + CategoryTableMetaData.TABLE_NAME + " c2 on c2." + CategoryTableMetaData._ID + " = c1.catMainID and c2." +
				CategoryTableMetaData._ID + " = " + categoryID + " \n" +
				"join (select " + BudgetTableMetaData.CURRENCY_ID + ", " + BudgetTableMetaData.FROM_DATE + ", b1.* from " +
				BudgetCategoriesTableMetaData.TABLE_NAME + " b1 \n" +
				"\tjoin " + BudgetTableMetaData.TABLE_NAME + " b2 on b2." + BudgetTableMetaData._ID + " = b1." + BudgetCategoriesTableMetaData.BUDGET_ID + " \n" +
				"\twhere b2." + BudgetTableMetaData.FROM_DATE + " between '" + Tools.DateToDBString(fromDate) + "' and '" + Tools.DateToDBString(toDate) +
				"')b on b." + BudgetCategoriesTableMetaData.CATEGORY_ID + " = c1.catID \n" +
				"group by c2." + CategoryTableMetaData._ID + ", c2." + CategoryTableMetaData.NAME + ", " + BudgetTableMetaData.CURRENCY_ID +
				", " + BudgetTableMetaData.FROM_DATE + " \n" +
				")group by " + CategoryTableMetaData._ID + ", " + CategoryTableMetaData.NAME + ", " + BudgetTableMetaData.CURRENCY_ID +
				" order by " + CategoryTableMetaData.NAME;
		Cursor cursor = DBTools.createCursor(context, sql);
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			result += CurrRatesSrv.convertAmount(context, DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.BUDGET),
					DBTools.getCursorColumnValueLong(cursor, BudgetTableMetaData.CURRENCY_ID), defaulCurrencyID, toDate);
			rowCount += DBTools.getCursorColumnValueInt(cursor, colulmSay);
 		}
		if (rowCount == 0)
			return 0d;
		else
			return result/rowCount;
	}

	public static Double getCategoryAverageBudget(Context context, Date fromDate, Date toDate, long categoryID, long defaulCurrencyID) {
		double result = 0;
		int rowCount = 0;
		String colulmSay = "say";
		String sql = "select " + BudgetTableMetaData.CURRENCY_ID + ", sum(" + BudgetCategoriesTableMetaData.BUDGET + ") " +
				BudgetCategoriesTableMetaData.BUDGET + ", count(*) " + colulmSay + " \n" +
				"from " + BudgetTableMetaData.TABLE_NAME + " b \n" +
				"join " + BudgetCategoriesTableMetaData.TABLE_NAME + " bc on bc." + BudgetCategoriesTableMetaData.BUDGET_ID + " = b." +
				BudgetTableMetaData._ID + " \n" +
				"join " + CategoryTableMetaData.TABLE_NAME + " c on c." + CategoryTableMetaData._ID + " = bc." +
				BudgetCategoriesTableMetaData.CATEGORY_ID + " \n" +
				"where " + BudgetTableMetaData.FROM_DATE + " between '" + Tools.DateToDBString(fromDate) +
				"' and '" + Tools.DateToDBString(toDate) + "' and c." + CategoryTableMetaData._ID + " = " + categoryID + " \n" +
				"group by " + BudgetTableMetaData.CURRENCY_ID;
		Cursor cursor = DBTools.createCursor(context, sql);
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			result += CurrRatesSrv.convertAmount(context, DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.BUDGET),
					DBTools.getCursorColumnValueLong(cursor, BudgetTableMetaData.CURRENCY_ID), defaulCurrencyID, toDate);
			rowCount += DBTools.getCursorColumnValueInt(cursor, colulmSay);
		}
		if (rowCount == 0)
			return 0d;
		else
			return result/rowCount;
	}

	public static Double getGroupBudget(Context context, Date date, long categoryID, long defaultCurrencyID) {
		String sql = "select b." + BudgetTableMetaData.CURRENCY_ID + ", sum(" + BudgetCategoriesTableMetaData.BUDGET + ")" + BudgetCategoriesTableMetaData.BUDGET + " \n" +
				"from (select " + CategoryTableMetaData._ID + " catMainID, " + CategoryTableMetaData._ID + " catID, 1 is_main \n" +
				"\tfrom " + CategoryTableMetaData.TABLE_NAME + " where " + CategoryTableMetaData.MAINID + " is null and " +
				CategoryTableMetaData.ISINCOME + " = 0 \n" +
				"\tunion all \n" +
				"\tselect " + CategoryTableMetaData.MAINID + ", " + CategoryTableMetaData._ID + ", 0 is_main \n" +
				"\tfrom " + CategoryTableMetaData.TABLE_NAME + " where " + CategoryTableMetaData.MAINID + " is not null and " +
				CategoryTableMetaData.ISINCOME + " = 0) c1 \n" +
				"join " + BudgetCategoriesTableMetaData.TABLE_NAME + " bc on bc." + BudgetCategoriesTableMetaData.CATEGORY_ID +
				" = c1.catID and c1.catMainID = " + categoryID + " \n" +
				"join " + BudgetTableMetaData.TABLE_NAME + " b on b." + BudgetTableMetaData._ID + " = bc." +
				BudgetCategoriesTableMetaData.BUDGET_ID + " and b." + BudgetTableMetaData.FROM_DATE + " = '" + Tools.DateToDBString(date) + "'";
		Cursor cursor = DBTools.createCursor(context, sql);
		if (cursor.moveToFirst())
			return CurrRatesSrv.convertAmount(context, DBTools.getCursorColumnValueDouble(cursor, BudgetCategoriesTableMetaData.BUDGET),
					DBTools.getCursorColumnValueLong(cursor, BudgetTableMetaData.CURRENCY_ID), defaultCurrencyID, Tools.AddMonth(date, 1));
		else
			return 0d;
	}

	/*public static boolean budgetGoalExists(Context context, long categoryID, Date controlMonth, StringBuilder targetMonth, StringBuilder targetAmount) {
		boolean result = false;
		Cursor cursor = context.getContentResolver().query(BudgetGoalsTableMetaData.CONTENT_URI, null,
				BudgetGoalsTableMetaData.START_MONTH + " <= '" + Tools.DateToDBString(controlMonth) + "' and "
						+ BudgetGoalsTableMetaData.TARGET_MONTH + " >= '" + Tools.DateToDBString(controlMonth) + "' and "
						+ BudgetGoalsTableMetaData.CATEGORY_ID + " = " + categoryID, null, null );
		if (cursor.moveToFirst()) {
			result = true;
			if (targetMonth != null) targetMonth.append(DBTools.getCursorColumnValue(cursor, BudgetGoalsTableMetaData.TARGET_MONTH));
			if (targetAmount != null) targetAmount.append(DBTools.getCursorColumnValue(cursor, BudgetGoalsTableMetaData.TARGET_AMOUNT));
		}
		return result;
	}*/

	public static void addGoal(Context context, long categoryID, String startMonth, String targetMonth, String targetAmount, String description) {
		ContentValues values = new ContentValues();
		values.put(BudgetGoalsTableMetaData.CATEGORY_ID, categoryID);
		values.put(BudgetGoalsTableMetaData.START_MONTH, startMonth);
		values.put(BudgetGoalsTableMetaData.TARGET_MONTH, targetMonth);
		values.put(BudgetGoalsTableMetaData.TARGET_AMOUNT, targetAmount);
		values.put(BudgetGoalsTableMetaData.DESCRIPTION, description);
		context.getContentResolver().insert(BudgetGoalsTableMetaData.CONTENT_URI, values);
	}

	public static void editGoal(Context context, long rowID, String targetMonth, String targetAmount, String description) {
		ContentValues values = new ContentValues();
		values.put(BudgetGoalsTableMetaData.TARGET_MONTH, targetMonth);
		values.put(BudgetGoalsTableMetaData.TARGET_AMOUNT, targetAmount);
		values.put(BudgetGoalsTableMetaData.DESCRIPTION, description);
		context.getContentResolver().update(BudgetGoalsTableMetaData.CONTENT_URI, values, BudgetGoalsTableMetaData._ID + " = " + rowID, null);
	}

	/**
	 * If first date of the month parameter changed then this method will update all budget rows
	 * @param context
	 * @param newDate New month start date
	 */
	public static void updateMonthStartDate (Context context, String newDate) {
		String newValue = newDate;
		if (newValue.length() == 1)
			newValue = "0" + newValue;
		String updateQuery = "update " + BudgetTableMetaData.TABLE_NAME + " set " + BudgetTableMetaData.FROM_DATE +
				" = substr(" + BudgetTableMetaData.FROM_DATE + ", 1, 6)||'" + newValue + "'";
		DBTools.execQuery(context, updateQuery);

		String newEndValue;
		//if (newValue.equals("01")) {
//		updateQuery = "update " + BudgetTableMetaData.TABLE_NAME + " set " + BudgetTableMetaData.TO_DATE +
//				" = replace(cast(date(DATE(substr(" + BudgetTableMetaData.FROM_DATE + ",1,4)\n" +
//				"||'-'||substr(" + BudgetTableMetaData.FROM_DATE + ",5,2)\n" +
//				"||'-'||substr(" + BudgetTableMetaData.FROM_DATE + ",7,2)),'start of month','+1 month','-1 day') as text), '-', '')";
		updateQuery = "update " + BudgetTableMetaData.TABLE_NAME + " set " + BudgetTableMetaData.TO_DATE +
				" = replace(cast(date(DATE(substr(" + BudgetTableMetaData.FROM_DATE + ",1,4)\n" +
				"||'-'||substr(" + BudgetTableMetaData.FROM_DATE + ",5,2)\n" +
				"||'-'||substr(" + BudgetTableMetaData.FROM_DATE + ",7,2)),'+1 month','-1 day') as text), '-', '')";
			DBTools.execQuery(context, updateQuery);
		/*}
		else {
			newEndValue = String.valueOf(Integer.valueOf(newValue) - 1);
			if (newEndValue.length() == 1)
				newEndValue = "0" + newEndValue;
			updateQuery = "update " + BudgetTableMetaData.TABLE_NAME + " set " + BudgetTableMetaData.TO_DATE +
					" = substr(" + BudgetTableMetaData.TO_DATE + ", 1, 6)||'" + newEndValue + "'";
			DBTools.execQuery(context, updateQuery);
		}*/
	}

	public static boolean getBudgetGoalStatus(Date targetMonth, Double targetAmount,
											  Double currentBudget, Double usedBudget, Double remainingBudget,
											  StringBuilder sbMonthlyMinimum, StringBuilder sbProgressValue) {
        int monthsCount = Math.max(Tools.monthsBetween(Tools.AddMonth(Tools.getCurrentDate(), -1), targetMonth), 1);
        boolean result = false;
        Double monthlyMinimum = 0d;
        Double progressValue = 0d;
        System.out.println("monthsCount" + monthsCount);
        if (targetMonth.compareTo(Tools.getCurrentDate()) >= 0) {
            monthlyMinimum = Tools.negativeToZero((targetAmount - (remainingBudget + currentBudget - usedBudget)) / monthsCount);
            progressValue = Tools.negativeToZero(currentBudget + remainingBudget - usedBudget);
            if (progressValue.compareTo(targetAmount) > 0)
                progressValue = targetAmount;
            result = monthlyMinimum > (currentBudget - usedBudget);
        }
        sbMonthlyMinimum.append(monthlyMinimum.toString());
        sbProgressValue.append(progressValue.toString());
        return result;
	}
}
