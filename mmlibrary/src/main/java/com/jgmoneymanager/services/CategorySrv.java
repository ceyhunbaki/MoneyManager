package com.jgmoneymanager.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.AccountTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CategoryTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VCategoriesViewMetaData;
import com.jgmoneymanager.mmlibrary.R;
import com.jgmoneymanager.entity.CheckBoxItem;

import java.util.ArrayList;

public class CategorySrv {

	public static final String mainCategoriesQueryForReports = "select " + CategoryTableMetaData._ID + ", " + CategoryTableMetaData.NAME + " from "
			+ CategoryTableMetaData.TABLE_NAME + " where " + CategoryTableMetaData.MAINID + " is null and " + CategoryTableMetaData.ISINCOME
			+ " =0 order by " + CategoryTableMetaData.DEFAULT_SORT_ORDER;

	public static long getCategoryIDBySubName(Context context, String mainName, String subCategoryName) {
		String id = DBTools.execQueryWithReturn(context, "select " + CategoryTableMetaData._ID + " from " + CategoryTableMetaData.TABLE_NAME + " where " + 
				CategoryTableMetaData.NAME + " = '" + subCategoryName + "' and " + CategoryTableMetaData.MAINID + " = (select " + 
				CategoryTableMetaData._ID + " from " + CategoryTableMetaData.TABLE_NAME + " where " + CategoryTableMetaData.NAME + 
				" = '" + mainName + "' and " + CategoryTableMetaData.MAINID + " is null)");
		if (id != null)
			return Long.parseLong(id);
		else 
			return 0;
	}
	
	public static String getCategoryNameByID(Context context, long categoryID) {
		Cursor cursor = context.getContentResolver().query(VCategoriesViewMetaData.CONTENT_URI,
				new String[]{VCategoriesViewMetaData.NAME},
				VCategoriesViewMetaData._ID + " = " + String.valueOf(categoryID), null, null);
		if (cursor.moveToFirst())
			return DBTools.getCursorColumnValue(cursor, VCategoriesViewMetaData.NAME);
		else
			return null;
	}
	
	/**
	 * If main Category doesn't exists inserts new one and returns Id,
	 * else returns old one's ID
	 * @param context
	 * @param name
	 * Main category Name
	 * @return
	 * Category ID
	 */
	public static long insertMainCategory(Context context, String name, Boolean isIncome, Integer resourceID) {
		long mainID;
		Cursor cursor = context.getContentResolver().query(CategoryTableMetaData.CONTENT_URI, 
				new String[] {CategoryTableMetaData._ID}, 
				CategoryTableMetaData.NAME + " = '" + name + "' and " + 
				CategoryTableMetaData.MAINID + " is null ", null, null);
		if (cursor.moveToFirst())
		{
			mainID = DBTools.getCursorColumnValueLong(cursor, CategoryTableMetaData._ID);
			cursor.close();
		}
		else 
		{
			ContentValues values = new ContentValues();
			values.put(CategoryTableMetaData.NAME, name);
			values.put(CategoryTableMetaData.ISINCOME, isIncome ? 1 : 0);
			if (resourceID != null)
				values.put(CategoryTableMetaData.RESOURCEID, resourceID);
			else
				values.putNull(CategoryTableMetaData.RESOURCEID);
			values.putNull(CategoryTableMetaData.MAINID);
			Uri insertedUri = context.getContentResolver().insert(CategoryTableMetaData.CONTENT_URI, values);
			Cursor insCursor = context.getContentResolver().query(insertedUri, null, null, null, null);
			insCursor.moveToFirst();
			mainID = DBTools.getCursorColumnValueLong(insCursor, CategoryTableMetaData._ID);
			insCursor.close();
		}
		return mainID;
	}
	
	/**
	 * Inserts subCategory with main ID
	 * @param context
	 * @param mainID
	 * @param subName
	 * @return
	 * if subName is not null then returns new ID, else returns mainID
	 */
	public static long insertSubCategory(Context context, long mainID, String subName, Boolean isIncome, Integer resourceID) {
		if (subName == null)
			return mainID;
		else {
			ContentValues values = new ContentValues();
			values.put(CategoryTableMetaData.NAME, subName);
			values.put(CategoryTableMetaData.MAINID, mainID);
			values.put(CategoryTableMetaData.ISINCOME, isIncome ? 1 : 0);
			if (resourceID != null)
				values.put(CategoryTableMetaData.RESOURCEID, resourceID);
			else
				values.putNull(CategoryTableMetaData.RESOURCEID);
			Uri insertedUri = context.getContentResolver().insert(CategoryTableMetaData.CONTENT_URI, values);
			Cursor insCursor = context.getContentResolver().query(insertedUri, null, null, null, null);
			insCursor.moveToFirst();
			long subID = DBTools.getCursorColumnValueLong(insCursor, CategoryTableMetaData._ID);
			insCursor.close();
			return subID;
		}
	}
	
	public static long insertSubCategory(Context context, String mainName, String subCategoryName, Boolean isIncome, Integer resourceID) {
		long mainID;
		mainID = insertMainCategory(context, mainName, isIncome, resourceID);
		if ((subCategoryName == null) || (subCategoryName.length() == 0))
			return mainID;
		else {
			return insertSubCategory(context, mainID, subCategoryName, isIncome, resourceID);
		}
	}
	
	public static int getMainCategoryCount(Context context) {
		Cursor cursor = context.getContentResolver().query(CategoryTableMetaData.CONTENT_URI, 
				new String[] {AccountTableMetaData._ID}, CategoryTableMetaData.MAINID + " is null ", null, null);
		int resultValue = cursor.getCount();
		cursor.close();
		return resultValue;
	}
	
	public static long getMainCategoryID(Context context, long subCategoryID) {
		Cursor cursor = context.getContentResolver().query(CategoryTableMetaData.CONTENT_URI, 
				new String[] {CategoryTableMetaData.MAINID}, 
				CategoryTableMetaData._ID + " = " + String.valueOf(subCategoryID), null, null);
		if (cursor.moveToFirst())
			if (DBTools.getCursorColumnValueLong(cursor, CategoryTableMetaData.MAINID) != 0)
				return DBTools.getCursorColumnValueLong(cursor, CategoryTableMetaData.MAINID);
			else
				return subCategoryID;
		else
			return 0;
	}
	
	/**
	 * Change category as income or expense category
	 * @param context
	 * @param categoryID Category or main category ID
	 */
	public static void changeCategoryStatusToIncome(Context context, long categoryID) {
		ContentValues values = new ContentValues();
		values.put(CategoryTableMetaData.ISINCOME, 1);
		values.put(CategoryTableMetaData.MAINID, CategorySrv.getMainIncomeCategoryID(context));
		context.getContentResolver().update(CategoryTableMetaData.CONTENT_URI, values, 
				"(" + CategoryTableMetaData._ID + " = " + String.valueOf(categoryID) 
				+ " or " + CategoryTableMetaData.MAINID + " = " + String.valueOf(categoryID)
				+ ") and " + CategoryTableMetaData.MAINID + " is not null ", null);
	}
	
	public static void changeCategoryStatusToExpense(Context context, long categoryID, long mainCategoryID) {
		ContentValues values = new ContentValues();
		values.put(CategoryTableMetaData.ISINCOME, 0);
		values.put(CategoryTableMetaData.MAINID, mainCategoryID);
		context.getContentResolver().update(CategoryTableMetaData.CONTENT_URI, values, 
				CategoryTableMetaData._ID + " = " + String.valueOf(categoryID), null);
	}

	public static long getMainIncomeCategoryID(Context context) {
		Cursor cursor = context.getContentResolver().query(CategoryTableMetaData.CONTENT_URI, 
				new String[] {CategoryTableMetaData._ID}, 
				CategoryTableMetaData.MAINID + " is null and " + CategoryTableMetaData.ISINCOME + " = 1 ", null, null);
		if (cursor.moveToFirst())
			return DBTools.getCursorColumnValueLong(cursor, CategoryTableMetaData._ID);
		else {
            return CategorySrv.insertMainCategory(context, context.getResources().getString(R.string.incomeCategories), true, R.string.incomeCategories);
		}			
	}

    public static ArrayList<CheckBoxItem> generateMainCategoryIDs(Context context){
        ArrayList<CheckBoxItem> categories = new ArrayList<>(0);
        Cursor cursor = DBTools.createCursor(context, CategorySrv.mainCategoriesQueryForReports);
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            categories.add(new CheckBoxItem(DBTools.getCursorColumnValueInt(cursor, CategoryTableMetaData._ID),
                    DBTools.getCursorColumnValue(cursor, CategoryTableMetaData.NAME)));
        }
        return categories;
    }

	public static void changeCategoryName(Context context, long id, String newName) {
		changeCategoryName(context, id, newName, true);
	}

	public static void changeCategoryName(Context context, long id, String newName, boolean deleteResourceID) {
		ContentValues values = new ContentValues();
		values.put(CategoryTableMetaData.NAME, newName);
		if (deleteResourceID)
			values.putNull(CategoryTableMetaData.RESOURCEID);
		context.getContentResolver().update(CategoryTableMetaData.CONTENT_URI, values,
				CategoryTableMetaData._ID + " = " + String.valueOf(id), null);
	}

	/**
	 * if id is group _id then returns true
	 * @param context
	 * @param id {@link CategoryTableMetaData._ID}
	 * @return boolean
     */
	public static boolean isGroupCategoryID(Context context, long id) {
		Cursor cursor = context.getContentResolver().query(CategoryTableMetaData.CONTENT_URI, null,
				CategoryTableMetaData._ID + " = " + String.valueOf(id) + " and " + CategoryTableMetaData.MAINID + " is null", null, null);
		return cursor.moveToFirst();
	}
}
