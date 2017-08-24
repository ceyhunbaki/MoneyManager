package com.jgmoneymanager.tools;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.ParseException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;
import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.CheckBoxItem;
import com.jgmoneymanager.entity.Margins;
import com.jgmoneymanager.mmlibrary.R;
import com.jgmoneymanager.services.AccountSrv;
import com.jgmoneymanager.services.CategorySrv;
import com.jgmoneymanager.services.CurrencySrv;
import com.jgmoneymanager.services.TransactionSrv;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static com.jgmoneymanager.services.CurrencySrv.getCurrencySymbol;
import static com.jgmoneymanager.tools.Constants.decimalCountUser;
import static com.jgmoneymanager.tools.Constants.decimalSeparatorSymbol;
import static com.jgmoneymanager.tools.Constants.digitGropingSymbol;

public class Tools {

	public static boolean existsInTable(Context ctx, Uri contentUri,
										String columnName, String value, String condition) {
		String selection = " upper (" + columnName + ") = upper('" + value.replace("'", "")
				+ "')";
		if (condition != null)
			selection += " and " + condition;
		Cursor cursor = ctx.getContentResolver().query(contentUri, null, selection, null, null);
		boolean resultValue = cursor.moveToFirst();
		cursor.close();
		return resultValue;
	}

	public static String formatDecimal(double number) {
		return formatDecimal(number, Constants.decimalCount);
	}

	public static String formatDecimal(double number, String decimalCount) {
		// double epsilon = 0.004d; // 4 tenths of a cent
		// if ( Math.abs(Math.round(number) - number) < epsilon) {
		// return String.format("%.0f", number); // sdb
		// } else {
		return String.format("%." + decimalCount + "f", number).replace(",", "."); // dj_segfault
		// }
	}

	public static String formatDecimal(String number) {
		if ((number == null) || (number.length() == 0))
			return formatDecimal(0d);
		else
			return formatDecimal(Tools.parseDouble(number));
	}

	public static String formatDecimal(String number, String decimalCount) {
		if ((number == null) || (number.length() == 0))
			return formatDecimal(0d);
		else
			return formatDecimal(Tools.parseDouble(number), decimalCount);
	}

	public static String formatDecimalUser2DB(Context context, String number) {
		if ((number == null) || (number.length() == 0))
			return formatDecimal(0d);
		else
			return formatDecimal(Tools.stringToDoubleInUserFormat(context, number, false));
	}

	public static String formatDecimalInUserFormat(Double amount) {
		return formatDecimalInUserFormat(amount, decimalCountUser);
	}

	public static String formatDecimalInUserFormat(Double amount, int decimalCount) {
		/*DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
		decimalFormatSymbols.setDecimalSeparator(decimalSeparatorSymbol);
		if (!String.valueOf(digitGropingSymbol).toLowerCase().equals("n"))
			decimalFormatSymbols.setGroupingSeparator(digitGropingSymbol);
		String formatPattern = "#,##0";
		if (decimalCount > 0)
			formatPattern = formatPattern + "." + new String(new char[decimalCount]).replace("\0", "0");
		DecimalFormat decimalFormat = new DecimalFormat(formatPattern, decimalFormatSymbols);
		if (String.valueOf(digitGropingSymbol).toLowerCase().equals("n"))
			decimalFormat.setGroupingUsed(false);
		return decimalFormat.format(amount);*/
		return formatDecimalInUserFormat(String.valueOf(Tools.round(amount, decimalCount)).replace('.', decimalSeparatorSymbol).replace(',', decimalSeparatorSymbol), true, true, true, decimalCount);
	}

	public static String formatDecimalInUserFormat(Double amount, boolean truncPrecision, boolean addPrecision, boolean deleteLeftZeros, int decimalCount) {
		return formatDecimalInUserFormat(amount.toString().replace('.', decimalSeparatorSymbol).replace(',', decimalSeparatorSymbol), truncPrecision, addPrecision, deleteLeftZeros, decimalCount);
	}

	public static String formatDecimalInUserFormat(String amount, int decimalCount) {
		return formatDecimalInUserFormat(amount, true, true, true, decimalCount);
	}

	/**
	 * String formatda giren reqemi formatlayir
	 * @param amount
	 * @param truncPrecision	Onluq hissədə artıq simvolları kəssin
	 * @param addPrecision	Onluq hissəyə əlavə sıfırlar atsın
	 * @param deleteLeftZeros	solda boş sıfırlar varsa silsin
	 * @return
	 */
	public static String formatDecimalInUserFormat(String amount, boolean truncPrecision, boolean addPrecision, boolean deleteLeftZeros, int decimalCount) {
		//return formatDecimalInUserFormat(stringToDoubleInUserFormat(context, amount, false), decimalCount);
		String text = amount;

		//evvelde - simvolu varsa onu indiden ayrib saxlayaq
		String signSymbol = "";
		if ((text.length() > 0) && (text.substring(0,1).equals("-"))) {
			text = text.replace("-", "");
			signSymbol = "-";
		}

		//Sonu . ile bitirse, lakin ayirici simvol , olarsa cevirib , qoyaq
		String otherSeparatorSymbol = ".";
		if (String.valueOf(decimalSeparatorSymbol).equals(otherSeparatorSymbol))
			otherSeparatorSymbol = ",";
		if (text.endsWith(otherSeparatorSymbol)) {
			text = text.substring(0, text.length() - 1) + Constants.decimalSeparatorSymbol;
		}
		boolean lastSymbolDecimal = text.endsWith(String.valueOf(decimalSeparatorSymbol));

		if (text.length() > 0) {
			//Onluq hisseni kesib saxlayaq
			String decimalPart = null;
			int decimalPointPosition = text.indexOf(decimalSeparatorSymbol);
			if (decimalPointPosition > 0) {
				decimalPart = text.substring(decimalPointPosition + 1);
				text = text.substring(0, decimalPointPosition);
				//decimalPart = decimalSeparatorSymbol + decimalPart.replace(decimalSeparatorSymbol, ' ').replace(" ", "");

				if (truncPrecision && (decimalPart.length() > decimalCount))
					decimalPart = decimalPart.substring(0, decimalCount);
				if (addPrecision && (decimalPart.length() < decimalCount) && (decimalCount > 0)) {
					int i = 0;
					while (i < decimalCount + 1 - decimalPart.length()) {
						decimalPart += '0';
						i++;
					}
				}
			}

			String unFormattedString = text;
			//Soldaki simvollari kesmek lazimdirsa
			if (deleteLeftZeros) {
				if (Pattern.matches("[0]+", unFormattedString) && (decimalPart == null)) {
					return "0";
				} else {
					while ((unFormattedString.indexOf("0") == 0) && (unFormattedString.length() > 1)) {
						unFormattedString = unFormattedString.substring(1);
					}
				}
				//unFormattedString = String.valueOf(Integer.parseInt(unFormattedString));
			}
			String formattedText = formatIntegerPart(unFormattedString);
			if (lastSymbolDecimal || ((decimalPart != null) && (decimalPart.length() > 0)))
				formattedText += decimalSeparatorSymbol + decimalPart;
			if (!formattedText.equals(amount)) {
				return signSymbol+formattedText;
			}
		}
		return amount;
	}

	private static String formatIntegerPart(String integerNumber) {
		String formattedText = "";
		int symbolCount = 1;
		integerNumber = integerNumber.replace(digitGropingSymbol, ' ').replace(" ", "");
		for (int i = integerNumber.length() - 1; i >= 0; i--) {
			formattedText = integerNumber.charAt(i) + formattedText;
			if ((symbolCount == 3) && (i > 0)) {
				formattedText = digitGropingSymbol + formattedText;
				symbolCount = 0;
			}
			symbolCount++;
		}
		return formattedText;
	}

	/*public static String appendNumberInUserFormat(Context context, String valueInUserFormat, String newChar) {
		String resultString;
		if (newChar.equals(String.valueOf(decimalSeparatorSymbol)) || valueInUserFormat.indexOf(decimalSeparatorSymbol) != -1)
			resultString = valueInUserFormat + newChar;
		else {
			resultString = formatDecimalInUserFormat(valueInUserFormat + newChar, false, false, true, Constants.decimalCountUser);
		}
		return resultString;
	}*/

	public static Double parseDouble(String value) {
		try {
			return Double.parseDouble(value);
		}
		catch (Exception e) {
			return 0d;
		}
	}

	public static Double stringToDouble(Context context, String value, boolean throwErrorMessage) {
		if (value == null)
			return 0d;
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			if (throwErrorMessage)
				DialogTools.toastDialog(context, R.string.msgInvalidNumber, Toast.LENGTH_SHORT);
			return 0d;
		} catch (NullPointerException e) {
			return 0d;
		}
	}

	public static Double stringToDoubleInUserFormat(Context context, String value, boolean throwErrorMessage) {
		if (value == null)
			return 0d;
		try {
			value = value.replace(String.valueOf(digitGropingSymbol), "").replace(",", ".");
			return stringToDouble(context, value, throwErrorMessage);
		} catch (NumberFormatException e) {
			if (throwErrorMessage)
				DialogTools.toastDialog(context, R.string.msgInvalidNumber, Toast.LENGTH_SHORT);
			return 0d;
		} catch (NullPointerException e) {
			return 0d;
		}
	}

	/*public static Double controlCorrectNumberInUserFormat(Context context, String value) {
		if (value == null)
			return 0d;
		value = value.replace(String.valueOf(digitGropingSymbol), "").replace(",", ".");
		return stringToDouble(context, value, true);
	}*/

	public static boolean isCorrectNumber(String inputText) {
		try {
			double d = Double.parseDouble(inputText);
			return true;
		} catch (NumberFormatException e) {
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean isGreaterZero(Context context, String value) {
		return (isCorrectNumber(value) && Double.compare(Tools.stringToDouble(context, value, false), 0) > 0);
	}

	public static String cutName(String name) {
		return name.substring(0,
				Math.min(Constants.maxNameLength, name.length()));
	}

	public static int compareDouble(Double d1, Double d2) {
		// hansinda daha az onluq hisse varsa, digerini hemin hisseye qeder
		// yuvarlaqlawdirib sonra muqayise edir
		BigDecimal big1 = new BigDecimal(d1);
		BigDecimal big2 = new BigDecimal(d2);
		String stDouble1 = String.valueOf(d1);
		String stDouble2 = String.valueOf(d2);
		String stDecimal1 = stDouble1.substring(stDouble1.indexOf(".") + 1,
				stDouble1.length());
		String stDecimal2 = stDouble2.substring(stDouble2.indexOf(".") + 1,
				stDouble2.length());
		int precision = 0;
		// if ((stDouble1.indexOf(".") > -1) && (stDouble2.indexOf(".") > -1))
		precision = Math.min(stDecimal1.length(), stDecimal2.length())
				+ (((Integer.parseInt(stDecimal1) == 0) || (Integer
				.parseInt(stDecimal2) == 0)) ? 0 : 1);
		return big1.round(new MathContext(precision)).compareTo(
				big2.round(new MathContext(precision)));
	}

	public static ArrayList<CheckBoxItem> cretaCheckBoxList(Cursor cursor, String idColumn, String nameColumn) {
		ArrayList<CheckBoxItem> itemsList = new ArrayList<CheckBoxItem>();
		CheckBoxItem itemRow;
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			itemRow = new CheckBoxItem(DBTools.getCursorColumnValueInt(cursor, idColumn),
					DBTools.getCursorColumnValue(cursor, nameColumn));
			itemsList.add(itemRow);
		}
		return itemsList;
	}

	public static String getNamesFromCheckBoxList(ArrayList<CheckBoxItem> list) {
		String result = "";
		if (list == null)
			return result;
		for (int i = 0; i < list.size(); i++) {
			CheckBoxItem item = list.get(i);
			if (item.isSelected())
				result += item.getName() + ", ";
		}
		if (result.length() > 2)
			return result.substring(0, result.length()-2);
		else
			return result;
	}

	public static String getIDsFromCheckBoxList(ArrayList<CheckBoxItem> list) {
		String result = "";
		if (list == null)
			return result;
		for (int i = 0; i < list.size(); i++) {
			CheckBoxItem item = list.get(i);
			if (item.isSelected())
				result += item.getID() + ", ";
		}
		if (result.length() > 2)
			return result.substring(0, result.length()-2);
		else
			return result;
	}

	/**
	 * Returns First checked ID
	 * @param list
	 * @return
	 */
	public static Integer getIDFromCheckBoxList(ArrayList<CheckBoxItem> list) {
		Integer result = 0;
		if (list == null)
			return result;
		for (int i = 0; i < list.size(); i++) {
			CheckBoxItem item = list.get(i);
			if (item.isSelected())
				return item.getID();
		}
		return result;
	}

	public static HashMap<Integer, Integer> convertCheckBoxListToHashMap(ArrayList<CheckBoxItem> list) {
		HashMap<Integer, Integer> resultList = new HashMap<>();
		if (list != null)
			for (int i = 0; i < list.size(); i++) {
				CheckBoxItem item = list.get(i);
				if (item.isSelected())
					resultList.put(item.getID(), item.getID());
			}
		return resultList;
	}

	public static String convertListToString(ArrayList<CheckBoxItem> itemsList) {
		String result = "";
		for (int i=0; i<itemsList.size(); i++) {
			CheckBoxItem item = itemsList.get(i);
			if (item.isSelected())
				result += item.getID() + ",";
		}
		return result;
	}

	public static ArrayList<CheckBoxItem> getValuesFromHashMap(HashMap<Integer, Integer> hashMap, ArrayList<CheckBoxItem> list) {
		if (list != null)
			for (int i = 0; i < list.size(); i++) {
				CheckBoxItem item = list.get(i);
				item.setSelected(hashMap.containsKey(item.getID()));
			}
		return list;
	}

	public static ArrayList<CheckBoxItem> getValuesFromString(String values, ArrayList<CheckBoxItem> list) {
		values = "," + values + ",";
		if (list != null)
			for (int i = 0; i < list.size(); i++) {
				CheckBoxItem item = list.get(i);
				item.setSelected(values.indexOf("," + item.getID() + ",") != -1);
			}
		return list;
	}

	// Date methods
	public static String DateToString(Date inDate, String format) {
		if (inDate == null)
			return null;
		else {
			try {
				SimpleDateFormat formatter = new SimpleDateFormat(format);
				return formatter.format(inDate);
			} catch (ParseException ps) {
				return null;
			}
		}
	}

	public static String DateToString(Date inDate) {
		return DateToString(inDate, Constants.DateFormatUser);
	}

	public static String DateToDBString(Date inDate) {
		return DateToString(inDate, Constants.DateFormatDB);
	}

	public static Date StringToDate(String inDate, String format) {
		try {
			SimpleDateFormat formatter = new SimpleDateFormat(format);
			return formatter.parse(inDate);
		} catch (java.text.ParseException e) {
			return null;
		}
	}

	public static Date StringToDate(String inDate) {
		return StringToDate(inDate, Constants.DateFormatUser);
	}

	public static String LongDateToString(Long value, String format) {
		//"MM/dd/yyyy HH:mm:ss"
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(value);
	}

	public static String UserDateToDBDate(String inDate) {
		return DateToDBString(StringToDate(inDate, Constants.DateFormatUser));
	}

	public static String DBDateToUserDate(String inDate) {
		return DateToString(StringToDate(inDate, Constants.DateFormatDB),
				Constants.DateFormatUser);
	}

	public static Date AddDays(Date inDate, int dayCount) {
		Calendar cal = Calendar.getInstance();
		cal.set(inDate.getYear() + 1900, inDate.getMonth(), inDate.getDate(),
				0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.add(Calendar.DATE, dayCount);
		return cal.getTime();
	}

	public static Date AddMonth(Date inDate, int monthCount) {
		Calendar cal = Calendar.getInstance();
		cal.set(inDate.getYear() + 1900, inDate.getMonth(), inDate.getDate(),
				0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.add(Calendar.MONTH, monthCount);
		return cal.getTime();
	}

	/**
	 * Returns last day of the month
	 *
	 * @param inDate
	 * @return
	 */
	public static Date lastDay(Context context, Date inDate) {
		//return AddDays(AddMonth(inDate, 1), -1);
		return AddDays(AddMonth(truncDate(context, inDate, Constants.DateTruncTypes.dateTruncMonth), 1), -1);
	}

	/**
	 * Calculates next date
	 *
	 * @param repeatType     period-{@link Constants.TransferType}
	 * @param currentDate
	 * @param customInterval interval count on days for Custom Period
	 * @return next date
	 */
	public static Date getNextDate(int repeatType, Date currentDate, int customInterval) {
		if (repeatType == Constants.TransferType.Daily.index())
			return Tools.AddDays(currentDate, 1);
		if (repeatType == Constants.TransferType.Weekly.index())
			return Tools.AddDays(currentDate, 7);
		if (repeatType == Constants.TransferType.Monthly.index())
			return Tools.AddMonth(currentDate, 1);
		if (repeatType == Constants.TransferType.Quarterly.index())
			return Tools.AddMonth(currentDate, 3);
		if (repeatType == Constants.TransferType.Yearly.index())
			return Tools.AddMonth(currentDate, 12);
		if (repeatType == Constants.TransferType.Custom.index())
			return Tools.AddDays(currentDate, customInterval);
		return currentDate;
	}

	/**
	 * Calculates next date from Fromdate
	 *
	 * @param repeatType     period-{@link Constants.TransferType}
	 * @param currentDate    start of the period
	 * @param customInterval interval count on days for Custom Period
	 * @param fromDate       requires date from this date
	 * @return next date
	 */
	public static Date getNextDateFrom(int repeatType, Date currentDate, int customInterval, Date fromDate) {
		Date nextDate = currentDate;
		while (nextDate.compareTo(fromDate) <= 0) {
			nextDate = getNextDate(repeatType, nextDate, customInterval);
		}
		return nextDate;
	}

	public static int getCurrentYear() {
		Calendar cal = Calendar.getInstance();
		return cal.get(Calendar.YEAR);
	}

	public static Date getCurrentDate() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	public static Date getCurrentDateTime() {
		Calendar cal = Calendar.getInstance();
		return cal.getTime();
	}

	/**
	 * @param inDate
	 * @param type   {@link Constants.DateTruncTypes}
	 * @return
	 */
	public static Date truncDate(Context context, Date inDate, String type) {
		Date result = new Date();
		if (type.equals(Constants.DateTruncTypes.dateTruncWeek)) {
			result = AddDays(inDate, -getDay(inDate) + 1 + Arrays.asList(context.getResources().getStringArray(R.array.weeksArrayKey)).indexOf(
					Constants.WeekFirstDay
			) );
			if (result.compareTo(inDate) > 0)
				result = Tools.AddDays(result, -7);
		} else if (type.equals(Constants.DateTruncTypes.dateTruncMonth)) {
			result = new Date(inDate.getYear(), inDate.getMonth(), Constants.MonthFirstDate);
			if (result.compareTo(inDate) > 0)
				result = Tools.AddMonth(result, -1);
		} else if (type.equals(Constants.DateTruncTypes.dateTruncYear)) {
			result = new Date(inDate.getYear(), 0, 1);
		}
		return result;
	}

	private static int getDay(Date inDate) {
		if (inDate.getDay() == 0)
			return 7;
		else
			return inDate.getDay();
	}

	public static int compareDates(Date date1, Date date2) {
		if (date1 == null && date2 == null)
			return 0; // make null==null
		else if (date1 == null)
			return -1; // this null < other not null
		else if (date2 == null)
			return 1; // this not null > other null
		else
			return date1.compareTo(date2);
	}

	private static boolean compareDates(Date dateIn, Date firstDate, Date secondDate) {
		return ((compareDates(dateIn, firstDate) >= 0) && (compareDates(dateIn, secondDate) <= 0));
	}

	public static Date getLeastDate(Date date1, Date date2) {
		if (date1 == null && date2 == null)
			return null; // make null==null
		else if (date1 == null)
			return date2; // this null < other not null
		else if (date2 == null)
			return date1; // this not null > other null
		else if (date1.compareTo(date2) >= 0)
			return date2;
		else
			return date1;
	}

	public static long getDateDifference(Date date1, Date date2) {
		long diff = date2.getTime() - date1.getTime();
		return diff / (24 * 60 * 60 * 1000);
	}

	public static int monthsBetween(Date startDate, Date endDate) {
		Calendar startCalendar = new GregorianCalendar();
		startCalendar.setTime(startDate);
		Calendar endCalendar = new GregorianCalendar();
		endCalendar.setTime(endDate);

		int diffYear = endCalendar.get(Calendar.YEAR) - startCalendar.get(Calendar.YEAR);
		return (int) negativeToZero((double) (diffYear * 12 + endCalendar.get(Calendar.MONTH) - startCalendar.get(Calendar.MONTH)));
	}

	public static void importExpenceManagerCSV(Context context, String filePath) {
		//CurrencyEdit.controlCurrencies(context);
		try {
			// csv file containing data
			// String strFile = Environment.getExternalStorageDirectory() +
			// "/expensemanager.csv";

			// create BufferedReader to read csv file
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			String line;

			int dateColumn = -1;
			int amountColumn = -1;
			int categoryColumn = -1;
			int subCategoryColumn = -1;
			int descriptionColumn = -1;
			int accountColumn = -1;
			int currencyColumn = -1;
			int foundAll = 0;

			if ((line = br.readLine()) != null) {
				String[] RowData = line.split(",");
				for (int i = 0; i < RowData.length; i++) {
					if (RowData[i].equals(Constants.ImpExpColNames.date)
							&& (dateColumn == -1)) {
						dateColumn = i;
						foundAll++;
					} else if (RowData[i].equals(Constants.ImpExpColNames.amount)
							&& (amountColumn == -1)) {
						amountColumn = i;
						foundAll++;
					} else if (RowData[i]
							.equals(Constants.ImpExpColNames.category)
							&& (categoryColumn == -1)) {
						categoryColumn = i;
						foundAll++;
					} else if (RowData[i].equals(Constants.ImpExpColNames.subcategory)
							&& (subCategoryColumn == -1)) {
						subCategoryColumn = i;
						foundAll++;
					} else if (RowData[i].equals(Constants.ImpExpColNames.description)
							&& (descriptionColumn == -1)) {
						descriptionColumn = i;
						foundAll++;
					} else if (RowData[i].equals(Constants.ImpExpColNames.account)
							&& (accountColumn == -1)) {
						accountColumn = i;
						foundAll++;
					} else if (RowData[i].equals(Constants.ImpExpColNames.currency)
							&& (currencyColumn == -1)) {
						currencyColumn = i;
					}
				}
			}
			if (foundAll < 6)
				throw (new Exception());

			long accountID;
			long categoryID;
			long currencyID = Constants.defaultCurrency;
			double amount;
			int transactionType;
			String amountSt;
			String date;
			String description;
			String categoryName;
			String subCategoryName;
			String accountName;
			String currencyName;

			// int rowNum = 0;

			while ((line = br.readLine()) != null) {
				// rowNum++;
				String[] RowData = line.split(",");
				// for (int i = 0; i < RowData.length; i++) {
				amountSt = RowData[amountColumn];
				date = RowData[dateColumn];
				description = RowData[descriptionColumn];
				categoryName = RowData[categoryColumn];
				subCategoryName = RowData[subCategoryColumn];
				accountName = RowData[accountColumn];

				accountID = AccountSrv.getAccountIDByName(context, accountName);
				if (accountID == 0) {// eger account yoxdursa insert edek
					if (subCategoryName.equals("Initial Balance"))
						accountID = AccountSrv.insertAccount(context,
								accountName, amountSt, "0");
					else
						accountID = AccountSrv.insertAccount(context,
								accountName, "0.00", "0");
				}

				if (currencyColumn != -1) {
					currencyName = RowData[currencyColumn];
					currencyID = CurrencySrv.getCurrencyIDBySign(context,
							currencyName);
					if (currencyID == 0) // eger currency yoxdursa insert edek
						currencyID = CurrencySrv.insertCurrency(context,
								currencyName, currencyName, 0, 0);
				}

				if (!subCategoryName.equals("Initial Balance")) {
					categoryID = CategorySrv.getCategoryIDBySubName(context,
							categoryName, subCategoryName);
					amount = Tools.stringToDouble(context, amountSt, false);
					if (categoryID == 0) {// eger category yoxdursa insert edek
						categoryID = CategorySrv.insertSubCategory(context, categoryName, subCategoryName, amount > 0, null);
					}
					if (amount < 0) {
						amount = Math.abs(amount);
						transactionType = -1;
					} else
						transactionType = 1;
					TransactionSrv.insertTransaction(context, accountID,
							categoryID, Tools.StringToDate(date,
									Constants.DateFormatExpMan), amount,
							transactionType, description, 0, currencyID, 0, null, 0, 0);
				}
				// }
			}

		} catch (Exception e) {
			DialogTools.toastDialog(context,
					"Exception while reading csv file", Toast.LENGTH_LONG);
			System.out.println("Exception while reading csv file: " + e);
		}
	}

	public static void exportToCSV(Context context, String exportDir, String conditions) {
		String query = "select at.name, "
				+ "ifnull(case when ca2._id is not null then ca2.name else ca3.name end, 'Uncategorized') category_name, "
				+ "ca1.name subcategory_name,tr.trans_date, tr.trans_type * tr.amount amount, tr.description, cur.sign "
				+ "from transactions tr "
				+ "join accounts at on at._id = tr.account_id "
				+ "join currency cur on cur._id = tr.curr_id "
				+ "left join category ca1 on ca1._id = tr.category_id and ca1.main_id is not null "
				+ "left join category ca2 on ca2._id = ca1.main_id "
				+ "left join category ca3 on ca3._id = tr.category_id and ca3.main_id is null ";
		if (!conditions.equals(""))
			query += " where 1=1 " + conditions;
		Cursor cursor = DBTools.createCursor(context, query);

		BufferedWriter fwrite;
		try {
			File file = new File(exportDir, Constants.exportFileName
					+ Tools.DateToString(Tools.getCurrentDateTime(),
					Constants.DateFormatDBLong) + ".csv");
			fwrite = new BufferedWriter(new FileWriter(file));
			String comma = ",";

			fwrite.write(Constants.ImpExpColNames.account + comma
					+ Constants.ImpExpColNames.category + comma
					+ Constants.ImpExpColNames.subcategory + comma
					+ Constants.ImpExpColNames.date + comma
					+ Constants.ImpExpColNames.description + comma
					+ Constants.ImpExpColNames.amount + comma
					+ Constants.ImpExpColNames.currency);
			fwrite.newLine();

			for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor
					.moveToNext()) {
				fwrite.write(cursor.getString(0) + comma + cursor.getString(1)
						+ comma + cursor.getString(2) + comma
						+ Tools.DateToString(Tools.StringToDate(
								cursor.getString(3), Constants.DateFormatDB),
						Constants.DateFormatExpMan) + comma
						+ cursor.getString(5) + comma + cursor.getString(4)
						+ comma + cursor.getString(6));
				fwrite.newLine();
			}
			fwrite.close();
			DialogTools.toastDialog(
					context, context.getResources().getString(
							R.string.msgExportSuccessful), Toast.LENGTH_SHORT);
		} catch (IOException e) {
			Log.e("MoneyManager.export", e.getMessage());
			DialogTools.toastDialog(context, R.string.msgExportFailed, Toast.LENGTH_LONG);
		}
		DBTools.closeDatabase();
	}

	public static boolean isInternetAvailable(Context context, boolean controlOnlyWifi) {
		if (controlOnlyWifi) {
			ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			//NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

			//For 3G check
			//boolean is3g = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnectedOrConnecting();
			//For WiFi Check
            return connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnectedOrConnecting();
		} else
			return
					isInternetAvailable(context);
	}

	public static boolean isInternetAvailable(Context context) {
		ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null;
	}

	public static void copyFile(File src, File dst) throws IOException {
		FileChannel inChannel = new FileInputStream(src).getChannel();
		FileChannel outChannel = new FileOutputStream(dst).getChannel();
		try {
			inChannel.transferTo(0, inChannel.size(), outChannel);
		} finally {
			if (inChannel != null)
				inChannel.close();
			outChannel.close();
		}
	}

	/*public static void autoBackup(Context context) {
		if (Constants.autoBackupDate.compareTo(Tools.getCurrentDate()) < 0) {
			if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.autoBackupKey), true)) {
				File dbFile = new File(Environment.getDataDirectory() + "/data/com.jgmoneymanager.main/databases/"
						+ MoneyManagerProviderMetaData.DATABASE_NAME);

				File exportDir = new File(Constants.backupDirectory);
				if (!exportDir.exists()) {
					if (!exportDir.mkdirs()) {
						AlertDialog warning = DialogTools.warningDialog(context, R.string.msgWarning,
								context.getString(R.string.msgChooseBackupFolder));
						warning.show();
					}
				}
				File file = new File(exportDir, Tools.DateToString(getCurrentDate(), Constants.DateFormatBackupAuto));
				//File file = new File(exportDir, Constants.backupFileName + Tools.DateToString(Tools.getCurrentDateTime(), Constants.DateFormatDB));
				if (!file.exists())
					try {
						controlBkpFiles();
						file.createNewFile();
						copyFile(dbFile, file);
					} catch (IOException e) {
						DialogTools.toastDialog(context,
								context.getString(R.string.msgBackupFailed),
								Toast.LENGTH_SHORT);
						Log.e("MoneyManager.backup", e.getMessage(), e);
					}
			}
			Constants.autoBackupDate = Tools.getCurrentDate();
		}
	}*/

	public static void addFolder(String path, String name) {
		File file = new File(path, name);
		if (!file.exists())
			file.mkdirs();
	}

	public static void ediFile(String path, String oldName, String newName) {
		File file = new File(path, oldName);
		File newFile = new File(path, newName.trim());
		if (file.exists())
			file.renameTo(newFile);
	}

	public static void deleteFolder(File fileOrDirectory) {
		if (fileOrDirectory.isDirectory())
			for (File child : fileOrDirectory.listFiles())
				deleteFolder(child);

		fileOrDirectory.delete();
	}

	public static boolean isFirstLaunch(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getBoolean(context.getString(R.string.isFirstLaunch), true);
	}

	public static void loadSettings(Context context) {
		loadSettings(context, true);
	}

    /**
     *
     * @param context
     * @param doFirstLaunchActions if true then inserts initial database values
     */
	public static void loadSettings(Context context, boolean doFirstLaunchActions) {

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		if (prefs.getBoolean(context.getString(R.string.backupToDataFolderKey), false))
            //Constants.backupDirectory = Environment.getDataDirectory() + "/data/com.jgmoneymanager.paid/";
            Constants.backupDirectory = Environment.getDataDirectory() + "/data/" + context.getPackageName() + "/";
		else
			Constants.backupDirectory = prefs.getString(context.getResources().getString(R.string.backupFolderKey),
					context.getResources().getString(R.string.backupFolderDefaultValue));
		Constants.receiptDirectory = prefs.getString(context.getResources().getString(R.string.receiptFolderKey),
				context.getResources().getString(R.string.receiptFolderDefaultValue));

		Constants.DateFormatUser = Tools.getPreference(context, R.string.setDateFormatKey, R.string.dfk_ddmmyyyy_pointKey);
		Constants.currencySignPosition = Tools.getPreference(context, R.string.setCurSignKey).equals(context.getResources().getString(R.string.setCurSignLeftKey))
				?Constants.currencySignPositionLeft:Constants.currencySignPositionRight;
		decimalSeparatorSymbol = getPreference(context, R.string.setDecimalSymbolKey, R.string.setCommaKey).charAt(0);
		decimalCountUser = Integer.parseInt(getPreference(context, R.string.setDecimalDigitsCountKey, R.string.setDecimalDigitsCountDefaultKey));
		Constants.rateDecimalCount = getPreference(context, R.string.setDecimalDigitsCountCurrencyKey, R.string.setDecimalDigitsCountCurrencyDefaultKey);
		Constants.decimalCount = String.valueOf(decimalCountUser);
		if (getPreference(context, R.string.setDigitsGroupingSymbolKey, R.string.setNoneKey).toUpperCase().equals(context.getString(R.string.setNoneKey).toUpperCase()))
			digitGropingSymbol = Character.MIN_VALUE;
		else
			digitGropingSymbol = getPreference(context, R.string.setDigitsGroupingSymbolKey, R.string.setNoneKey).charAt(0);

		Constants.WeekFirstDay = getPreference(context, R.string.setWeekFirstDayKey, R.string.setMondayKey);
		Constants.MonthFirstDate = Integer.parseInt(getPreference(context, R.string.setMonthFirstDateKey, R.string.digitCountValueKey1));

		int newVersionCode = 0;
		try {
			PackageInfo packInfo = context.getPackageManager().getPackageInfo("com.jgmoneymanager.paid", 0);
			newVersionCode = packInfo.versionCode;
		} catch (NameNotFoundException e) {
			Tracker myTracker = EasyTracker.getInstance(context);
			myTracker.set(Fields.SCREEN_NAME, "loadSettings- Get versionCode");
			myTracker.send(MapBuilder.createAppView().build());
		}

		if (isFirstLaunch(context) && doFirstLaunchActions) {
			loadLanguage(context, null);
			DBTools.insertFirstItems(context);
			Editor editor = prefs.edit();
			editor.putBoolean(context.getString(R.string.isFirstLaunch), false);
			//editor.putString(context.getString(R.string.emailKey), getPrimaryEmailAccount(context));
			editor.putInt(context.getString(R.string.rateAppCurrentOpenCountKey), 0);
			//editor.putInt(context.getString(R.string.oldversionkey), newVersionCode);
			editor.commit();
			Tools.resetFormats(context);
		}

		/*if ((prefs.getString(context.getString(R.string.emailKey), null) == null) ||
				(prefs.getString(context.getString(R.string.emailKey), "").length() == 0)) {
			Editor editor = prefs.edit();
			editor.putString(context.getString(R.string.emailKey), getPrimaryEmailAccount(context));
			editor.commit();
		}*/

		int index = Integer.parseInt(prefs.getString(context.getResources().getString(R.string.backupMaxDateKey),context.getResources().getString(R.string.backupMaxDateDefValue)));
		Constants.backupDaysCount = Constants.BackupMaxDaysValues.getValue(index);

		index = Integer.parseInt(prefs.getString(context.getResources().getString(R.string.backupMaxSizeKey),context.getResources().getString(R.string.backupMaxSizeDefValue)));
		Constants.backupMaxSizeMB = Constants.BackupMaxSizeValues.getValue(index);
	}

	public static void resetFormats(Context context) {
		resetFormats(context, Tools.getPreference(context, R.string.setLanguageKey));
	}

	public static void resetFormats(Context context, String language) {
		Tools.setPreference(context, R.string.setDecimalDigitsCountKey, context.getString(R.string.setDecimalDigitsCountDefaultKey), false);
		Tools.setPreference(context, R.string.setDecimalDigitsCountCurrencyKey, context.getString(R.string.setDecimalDigitsCountCurrencyDefaultKey), false);
		Tools.setPreference(context, R.string.setWeekFirstDayKey, context.getString(R.string.setMondayKey), false);
		Tools.setPreference(context, R.string.setMonthFirstDateKey, context.getString(R.string.digitCountValueKey1), false);
		if (language.equals(Constants.LanguageValues.getValue(Constants.LanguageValues.Default.index()))) {
			Tools.setPreference(context, R.string.setDateFormatKey, context.getString(R.string.dfk_mmddyyyy_slashKey), false);
			Tools.setPreference(context, R.string.setCurSignKey, context.getString(R.string.setCurSignLeftKey), false);
			Tools.setPreference(context, R.string.setDecimalSymbolKey, context.getString(R.string.setDotKey), false);
			Tools.setPreference(context, R.string.setDigitsGroupingSymbolKey, context.getString(R.string.setCommaKey), false);
		}
		else if (language.equals(Constants.LanguageValues.getValue(Constants.LanguageValues.Azeri.index()))) {
			Tools.setPreference(context, R.string.setDateFormatKey, context.getString(R.string.dfk_ddmmyyyy_pointKey), false);
			Tools.setPreference(context, R.string.setCurSignKey, context.getString(R.string.setCurSignLeftKey), false);
			Tools.setPreference(context, R.string.setDecimalSymbolKey, context.getString(R.string.setCommaKey), false);
			Tools.setPreference(context, R.string.setDigitsGroupingSymbolKey, context.getString(R.string.setDotKey), false);
			Tools.setPreference(context, R.string.setWeekFirstDayKey, context.getString(R.string.setMondayKey), false);
		}
		else if (language.equals(Constants.LanguageValues.getValue(Constants.LanguageValues.Indonesian.index()))) {
			Tools.setPreference(context, R.string.setDateFormatKey, context.getString(R.string.dfk_ddmmyyyy_slashKey), false);
			Tools.setPreference(context, R.string.setCurSignKey, context.getString(R.string.setCurSignLeftKey), false);
			Tools.setPreference(context, R.string.setDecimalSymbolKey, context.getString(R.string.setCommaKey), false);
			Tools.setPreference(context, R.string.setDigitsGroupingSymbolKey, context.getString(R.string.setDotKey), false);
			Tools.setPreference(context, R.string.setWeekFirstDayKey, context.getString(R.string.setMondayKey), false);
		}
		else if (language.equals(Constants.LanguageValues.getValue(Constants.LanguageValues.Deutch.index()))) {
			Tools.setPreference(context, R.string.setDateFormatKey, context.getString(R.string.dfk_ddmmyyyy_pointKey), false);
			Tools.setPreference(context, R.string.setCurSignKey, context.getString(R.string.setCurSignRightKey), false);
			Tools.setPreference(context, R.string.setDecimalSymbolKey, context.getString(R.string.setCommaKey), false);
			Tools.setPreference(context, R.string.setDigitsGroupingSymbolKey, context.getString(R.string.setDotKey), false);
			Tools.setPreference(context, R.string.setWeekFirstDayKey, context.getString(R.string.setMondayKey), false);
		}
		else if (language.equals(Constants.LanguageValues.getValue(Constants.LanguageValues.English.index()))) {
			Tools.setPreference(context, R.string.setDateFormatKey, context.getString(R.string.dfk_ddmmyyyy_slashKey), false);
			Tools.setPreference(context, R.string.setCurSignKey, context.getString(R.string.setCurSignLeftKey), false);
			Tools.setPreference(context, R.string.setDecimalSymbolKey, context.getString(R.string.setCommaKey), false);
			Tools.setPreference(context, R.string.setDigitsGroupingSymbolKey, context.getString(R.string.setDotKey), false);
			Tools.setPreference(context, R.string.setWeekFirstDayKey, context.getString(R.string.setSundayKey), false);
		}
		else if (language.equals(Constants.LanguageValues.getValue(Constants.LanguageValues.Espanol.index()))) {
			Tools.setPreference(context, R.string.setDateFormatKey, context.getString(R.string.dfk_ddmmyyyy_slashKey), false);
			Tools.setPreference(context, R.string.setCurSignKey, context.getString(R.string.setCurSignRightKey), false);
			Tools.setPreference(context, R.string.setDecimalSymbolKey, context.getString(R.string.setCommaKey), false);
			Tools.setPreference(context, R.string.setDigitsGroupingSymbolKey, context.getString(R.string.setDotKey), false);
			Tools.setPreference(context, R.string.setWeekFirstDayKey, context.getString(R.string.setMondayKey), false);
		}
		else if (language.equals(Constants.LanguageValues.getValue(Constants.LanguageValues.French.index()))) {
			Tools.setPreference(context, R.string.setDateFormatKey, context.getString(R.string.dfk_ddmmyyyy_slashKey), false);
			Tools.setPreference(context, R.string.setCurSignKey, context.getString(R.string.setCurSignRightKey), false);
			Tools.setPreference(context, R.string.setDecimalSymbolKey, context.getString(R.string.setCommaKey), false);
			Tools.setPreference(context, R.string.setDigitsGroupingSymbolKey, context.getString(R.string.setSpaceKey), false);
			Tools.setPreference(context, R.string.setWeekFirstDayKey, context.getString(R.string.setMondayKey), false);
		}
		else if (language.equals(Constants.LanguageValues.getValue(Constants.LanguageValues.Italian.index()))) {
			Tools.setPreference(context, R.string.setDateFormatKey, context.getString(R.string.dfk_ddmmyyyy_slashKey), false);
			Tools.setPreference(context, R.string.setCurSignKey, context.getString(R.string.setCurSignRightKey), false);
			Tools.setPreference(context, R.string.setDecimalSymbolKey, context.getString(R.string.setCommaKey), false);
			Tools.setPreference(context, R.string.setDigitsGroupingSymbolKey, context.getString(R.string.setDotKey), false);
			Tools.setPreference(context, R.string.setWeekFirstDayKey, context.getString(R.string.setMondayKey), false);
		}
		else if (language.equals(Constants.LanguageValues.getValue(Constants.LanguageValues.Portugal.index()))) {
			Tools.setPreference(context, R.string.setDateFormatKey, context.getString(R.string.dfk_ddmmyyyy_slashKey), false);
			Tools.setPreference(context, R.string.setCurSignKey, context.getString(R.string.setCurSignLeftKey), false);
			Tools.setPreference(context, R.string.setDecimalSymbolKey, context.getString(R.string.setCommaKey), false);
			Tools.setPreference(context, R.string.setDigitsGroupingSymbolKey, context.getString(R.string.setDotKey), false);
			Tools.setPreference(context, R.string.setWeekFirstDayKey, context.getString(R.string.setMondayKey), false);
		}
		else if (language.equals(Constants.LanguageValues.getValue(Constants.LanguageValues.Russian.index()))) {
			Tools.setPreference(context, R.string.setDateFormatKey, context.getString(R.string.dfk_ddmmyyyy_pointKey), false);
			Tools.setPreference(context, R.string.setCurSignKey, context.getString(R.string.setCurSignRightKey), false);
			Tools.setPreference(context, R.string.setDecimalSymbolKey, context.getString(R.string.setCommaKey), false);
			Tools.setPreference(context, R.string.setDigitsGroupingSymbolKey, context.getString(R.string.setSpaceKey), false);
			Tools.setPreference(context, R.string.setWeekFirstDayKey, context.getString(R.string.setMondayKey), false);
		}
		else if (language.equals(Constants.LanguageValues.getValue(Constants.LanguageValues.Ukrainian.index()))) {
			Tools.setPreference(context, R.string.setDateFormatKey, context.getString(R.string.dfk_ddmmyyyy_pointKey), false);
			Tools.setPreference(context, R.string.setCurSignKey, context.getString(R.string.setCurSignRightKey), false);
			Tools.setPreference(context, R.string.setDecimalSymbolKey, context.getString(R.string.setCommaKey), false);
			Tools.setPreference(context, R.string.setDigitsGroupingSymbolKey, context.getString(R.string.setSpaceKey), false);
			Tools.setPreference(context, R.string.setWeekFirstDayKey, context.getString(R.string.setMondayKey), false);
		}
	}

	@SuppressLint("NewApi")
	public static String getPrimaryEmailAccount(Context context) {
		String email = null;
		try {
			Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
			Account[] accounts = AccountManager.get(context).getAccountsByType("com.google");
			for (Account account : accounts) {
				if (emailPattern.matcher(account.name).matches()) {
					email = account.name;
				}
			}
		} catch (Exception e) {
			Log.e("getPrimaryEmailAccount", e.getMessage());
			Tracker myTracker = EasyTracker.getInstance(context);
			myTracker.set(Fields.SCREEN_NAME, e.getMessage());
			myTracker.send(MapBuilder.createAppView().build());
		}
		return email;
	}

	/**
	 * Show rate dialog after 15 times open
	 * @param context
	 * @return
     */
	public static boolean rateDialogMustShow(final Context context) {
		int rateDefaultAppOpenCount = 15;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		int controlValue = prefs.getInt(context.getResources().getString(R.string.rateShouldControlKey), Constants.RateShouldControlValues.Control.index());
		if (controlValue == Constants.RateShouldControlValues.Control.index()) {
			int rateCurrentOpenCount = prefs.getInt(context.getResources().getString(R.string.rateAppCurrentOpenCountKey), 0);
			rateDefaultAppOpenCount = prefs.getInt(context.getResources().getString(R.string.rateAppDefaultOpenCountKey), rateDefaultAppOpenCount);
			Tools.setPreference(context, R.string.rateAppCurrentOpenCountKey, rateCurrentOpenCount+1);
			if (rateCurrentOpenCount > rateDefaultAppOpenCount)
				return true;
		}
		return false;
	}

	public static Configuration loadLanguage(Context context, String languageToLoad) {
		if (languageToLoad == null) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			languageToLoad = prefs.getString(
					context.getResources().getString(R.string.setLanguageKey),
					context.getResources().getString(R.string.setLanguageDefaultValue));
		}
		Configuration config;
		if (!languageToLoad.equals(Constants.LanguageValues.getValue(0))) {
			Locale locale = new Locale(languageToLoad);
			//Locale.setDefault(locale);
			config = new Configuration();
			config.locale = locale;
			context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
		} else {
			//Locale.getDefault().getDisplayLanguage();
			//Locale locale = context.getResources().getConfiguration().locale; 
			//Locale.setDefault(locale);
			Locale locale = Locale.getDefault();
			config = new Configuration();
			config.locale = locale;
			context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
		}

		Locale locale = context.getResources().getConfiguration().locale;
		Locale.setDefault(locale);

		return config;
	}

	public static Locale getLocale(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String languageToLoad = prefs.getString(
				context.getResources().getString(R.string.setLanguageKey),
				context.getResources().getString(R.string.setLanguageDefaultValue));
		return new Locale(languageToLoad);
	}

	public static boolean sendEmail(Context context, String recipient, String subject, String body) {
		if (!isInternetAvailable(context)) {
			DialogTools.toastDialog(context, R.string.msgInternetUnavailable, Toast.LENGTH_SHORT);
			return false;
		}
		Mail m = new Mail(Constants.supportEmail, "android23");

		m.setTo(new String[]{recipient});
		Log.i("Email recipient", recipient);
		m.setFrom("sp.moneymanager@gmail.com");
		m.setSubject(subject);
		m.setBody(body);

		try {
			// m.addAttachment("/sdcard/filelocation");
			if (m.send()) {
				Toast.makeText(context, "Email was sent successfully.", Toast.LENGTH_SHORT).show();
				return true;
			} else {
				//if (!recipient.equals(getPrimaryEmailAccount(context))) {
				sendEmail(context, getPrimaryEmailAccount(context), subject, body);
				//}
				//Toast.makeText(context, "Email was not sent.", Toast.LENGTH_SHORT).show();
				return false;
			}
		} catch (Exception e) {
			//if (!recipient.equals(getPrimaryEmailAccount(context))) {
			//sendEmail(context, getPrimaryEmailAccount(context), subject, body);
			//}
			Toast.makeText(context, "There was a problem sending the email.", Toast.LENGTH_LONG).show();
			Tracker myTracker = EasyTracker.getInstance(context);
			myTracker.set(Fields.SCREEN_NAME, "Email:" + recipient + ";" + body);
			myTracker.send(MapBuilder.createAppView().build());
			return false;
		}
	}

	public static String encrypt(String plaintext) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA");
		} catch (NoSuchAlgorithmException e) {
			Log.i("MoneyManager.Encrypt", "SHA algorithm not found for encrypting passwords");
		}
		try {
			md.update("CSAASADM".getBytes("UTF-8"));
			md.update(plaintext.getBytes("UTF-8"));
			md.update("CSAASADM".getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			Log.i("MoneyManager.Encrypt", "UnsupportedEncodingException UTF-8 while encrypting password");
		}
		byte raw[] = md.digest();
		return Base64.encodeBytes(raw);
	}

	public static boolean isPreferenceAvialable(Context context, int keyId) {
		return PreferenceManager.getDefaultSharedPreferences(context).contains(context.getString(keyId));
	}

	public static String getPreference(Context context, int keyId) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(keyId), "null");
	}

	public static String getPreference(Context context, int keyId, int defaultValue) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(keyId), context.getString(defaultValue));
	}

	public static long getPreferenceLong(Context context, int keyId) {
		return PreferenceManager.getDefaultSharedPreferences(context).getLong(context.getString(keyId), 0);
	}

	public static int getPreferenceInt(Context context, int keyId) {
		return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(keyId), 0);
	}

	public static Boolean getPreferenceBool(Context context, int keyId, Boolean defaultValue) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(keyId), defaultValue);
	}

	public static void setPreference(Context context, int keyId, String value, boolean encrypt) {
		SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = myPrefs.edit();
		if (encrypt)
			editor.putString(context.getString(keyId), encrypt(value));
		else
			editor.putString(context.getString(keyId), value);
		editor.commit();
	}

	public static void setPreference(Context context, int keyId, Boolean value) {
		SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = myPrefs.edit();
		editor.putBoolean(context.getString(keyId), value);
		editor.commit();
	}

	public static void setPreference(Context context, int keyId, long value) {
		SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = myPrefs.edit();
		editor.putLong(context.getString(keyId), value);
		editor.commit();
	}

	public static void setPreference(Context context, int keyId, int value) {
		SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = myPrefs.edit();
		editor.putInt(context.getString(keyId), value);
		editor.commit();
	}

	public static void removePreferense(Context context, int keyId) throws Exception {
		SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = myPrefs.edit();
		editor.remove(context.getString(keyId));
		editor.commit();
	}

	public static String getApplicationVersion(Context ctx) {
		String result = "";
		try {
			PackageInfo pInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
			result = pInfo.versionName;
		} catch (NameNotFoundException e) {
		}
		return result;
	}

	public static void controlBkpFiles() {
		try {
			File file = new File(Constants.backupDirectory);
			File[] files = file.listFiles();
			Arrays.sort(files, new Comparator<File>() {
				public int compare(File f1, File f2) {
					String f1Name = f1.isDirectory() ? "/" + f1.getName() : f1.getName();
					String f2Name = f2.isDirectory() ? "/" + f2.getName() : f2.getName();
					return String.valueOf(f1Name).compareTo(f2Name);
				}
			});
			//en azi biri istisna olmaqla digerlerini settingse uygun olaraq silmeli
			int i = files.length - 2;
			boolean endOfLoop = false;
			long size = 0;//files[files.length - 1].length() / 1024;
			do {
				try {
					String fileName = files[i].getName().substring(0, Constants.DateFormatBackupAuto.length());
					//eger backupin adi backup ucun max gun sayindan evvele duwurse dovrden cixsin
					if (!compareDates(StringToDate(fileName, Constants.DateFormatBackupAuto), AddDays(getCurrentDate(),
							-Constants.backupDaysCount + 1), getCurrentDate()))
						endOfLoop = true;
				} catch (Exception e) {
				}
				if (!endOfLoop) {
					//eger evvelkilerin cem size-i limiti kecibse dovrden cixsin
					size += files[i].length() / 1024 / 1024;
					if (size > Constants.backupMaxSizeMB)
						endOfLoop = true;
				}
				if (!endOfLoop)
					i--;
			}
			while ((i >= 0) && !endOfLoop);
			if (endOfLoop) {
				//eger wert odenibse qalanlari silinsin
				for (int j = 0; j <= i; j++) {
					files[j].delete();
				}
			}
		} catch (Exception e) {
		}
	}

	public static String[] addElement(String[] list, String added) {
		String[] result = new String[list.length + 1];
		System.arraycopy(list, 0, result, 0, list.length);
		result[list.length] = added;
		return result;
	}
	
	/*public static int getItemIndex(String[] array, String item) {
		int index = -1;

	    for (int i = 0; (i < array.length) && (index == -1); i++) {
	        if (array[i] == item) {
	            index = i;
	        }
	    }
	    return index;
	}*/

	public static double round(double value) {
		return Tools.round(value, Integer.parseInt(Constants.decimalCount));
	}

	private static double round(double value, int decimalCount) {
		BigDecimal bd = new BigDecimal(value).setScale(
				decimalCount, RoundingMode.HALF_EVEN);
		return bd.doubleValue();
	}

	/**
	 * Create Margins from given style
	 *
	 * @param context
	 * @param styleID
	 * @return
	 */
	public static Margins getMarginsFromStyle(Context context, int styleID) {
		/*hamisini birden goturmek alinmadi*/
		Margins margin = new Margins(0, 0, 0, 0);
		TypedArray array = context.obtainStyledAttributes(styleID, new int[]{android.R.attr.layout_marginTop});
		margin.setTopMargin(array.getDimensionPixelSize(0, 0));
		array.recycle();
		array = context.obtainStyledAttributes(styleID, new int[]{android.R.attr.layout_marginBottom});
		margin.setBottomMargin(array.getDimensionPixelSize(0, 0));
		array.recycle();
		array = context.obtainStyledAttributes(styleID, new int[]{android.R.attr.layout_marginLeft});
		margin.setLeftMargin(array.getDimensionPixelSize(0, 0));
		array.recycle();
		array = context.obtainStyledAttributes(styleID, new int[]{android.R.attr.layout_marginRight});
		margin.setRightMargin(array.getDimensionPixelSize(0, 0));
		array.recycle();
		return margin;
	}

	//Function for onSave and onResume Instances
	public static void putToBundle(Bundle bundle, String key, Long value) {
		if (value != null)
			bundle.putLong(key, value);
	}

	public static Long getLongFromBundle(Bundle bundle, String key) {
		try {
			if (bundle.containsKey(key))
				return bundle.getLong(key);
			else return null;
		}
		catch (Exception e) {
			return null;
		}
	}

	public static long getLongFromBundle0(Bundle bundle, String key) {
		Long resultValue = getLongFromBundle(bundle, key);
		if (resultValue == null)
			return 0;
		else
			return resultValue;
	}

	public static void putToBundle(Bundle bundle, String key, Double value) {
		if (value != null)
			bundle.putDouble(key, value);
	}

	public static Double getDoubleFromBundle(Bundle bundle, String key) {
		try{
			if (bundle.containsKey(key))
				return bundle.getDouble(key);
			else return null;
		}
		catch (Exception e) {
			return null;
		}
	}

	public static double getDoubleFromBundle0(Bundle bundle, String key) {
		Double resultValue = getDoubleFromBundle(bundle, key);
		if (resultValue == null)
			return 0;
		else
			return resultValue;
	}

	public static void putToBundle(Bundle bundle, String key, Integer value) {
		if (value != null)
			bundle.putInt(key, value);
	}

	public static Integer getIntegerFromBundle(Bundle bundle, String key) {
		try {
			if (bundle.containsKey(key))
				return bundle.getInt(key);
			else return null;
		}
		catch (Exception e) {
			return null;
		}
	}

	public static int getIntegerFromBundle0(Bundle bundle, String key) {
		Integer resultValue = getIntegerFromBundle(bundle, key);
		if (resultValue == null)
			return 0;
		else
			return resultValue;
	}

	public static void putToBundle(Bundle bundle, String key, String value) {
		if (value != null)
			bundle.putString(key, value);
	}

	public static String getStringFromBundle(Bundle bundle, String key) {
		if (bundle.containsKey(key))
			return bundle.getString(key);
		else return null;
	}

	public static void putToBundle(Bundle bundle, String key, Date value) {
		if (value != null)
			bundle.putString(key, DateToString(value));
	}

	public static Date getDateFromBundle(Bundle bundle, String key) {
		if (bundle.containsKey(key))
			return StringToDate(bundle.getString(key));
		else return null;
	}

	public static void putToBundle(Bundle bundle, String key, Boolean value) {
		if (value != null)
			bundle.putBoolean(key, value);
	}

	public static Boolean getBooleanFromBundle(Bundle bundle, String key) {
		try {
			if (bundle.containsKey(key))
				return bundle.getBoolean(key);
			else return null;
		}
		catch (Exception e) {
			return null;
		}
	}

	public static boolean getBooleanFromBundle0(Bundle bundle, String key) {
		Boolean resultValue = getBooleanFromBundle(bundle, key);
		if (resultValue == null)
			return false;
		else
			return resultValue;
	}

	public static void putToBundle(Bundle bundle, String key, ArrayList<String> value) {
		if (value != null)
			bundle.putStringArrayList(key, value);
	}

	public static ArrayList<String> getStringArrayListFromBundle(Bundle bundle, String key) {
		if (bundle.containsKey(key))
			return bundle.getStringArrayList(key);
		else return null;
	}

	public static void putToBundle(Bundle bundle, String key, boolean[] value) {
		if (value != null)
			bundle.putBooleanArray(key, value);
	}

	public static boolean[] getBooleanArrayFromBundle(Bundle bundle, String key) {
		if (bundle.containsKey(key))
			return bundle.getBooleanArray(key);
		else return null;
	}

	public static void putToBundleIntegerArray(Bundle bundle, String key, ArrayList<Integer> value) {
		if (value != null)
			bundle.putIntegerArrayList(key, value);
	}

	public static ArrayList<Integer> getIntegerArrayListFromBundle(Bundle bundle, String key) {
		if (bundle.containsKey(key))
			return bundle.getIntegerArrayList(key);
		else return null;
	}

	public static void putToBundle(Bundle bundle, String key, Serializable value) {
		if (value != null)
			bundle.putSerializable(key, value);
	}

	public static Serializable getSerializableFromBundle(Bundle bundle, String key) {
		if (bundle.containsKey(key))
			return bundle.getSerializable(key);
		else return null;
	}

	public static void putToBundle(Bundle bundle, String key, Bundle value) {
		if (value != null)
			bundle.putBundle(key, value);
	}

	public static Bundle getBundleFromBundle(Bundle bundle, String key) {
		if (bundle.containsKey(key))
			return bundle.getBundle(key);
		else return null;
	}

	//old version
	public static boolean proVersionExists(Context context) {
		/*if (Tools.isPreferenceAvialable(context, R.string.proInstalledKey))
			return Tools.getPreferenceBool(context, R.string.proInstalledKey, false);
		else {*/
			boolean result = false;
			final PackageManager pm = context.getPackageManager();
			//get a list of installed apps.
			List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

			for (ApplicationInfo packageInfo : packages) {
				if (packageInfo.packageName.equals("com.jgmoneymanager.paid")) {
					Tools.setPreference(context, R.string.proInstalledKey, true);
					result = true;
				}
			}
			Tools.setPreference(context, R.string.proInstalledKey, result);
			return result;
		//}
	}

	public static double negativeToZero(Double value) {
		if (value.compareTo(0d) < 0)
			return 0d;
		else
			return value;
	}

	public static double positiveToZero(Double value) {
		if (value.compareTo(0d) > 0)
			return 0d;
		else
			return value;
	}

	public static void showRateDialog(final Context mContext) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		final Editor editor = prefs.edit();

		AlertDialog dialog = null;
		Command rateCommand = new Command() {
			@Override
			public void execute() {
				editor.putInt(mContext.getResources().getString(R.string.rateShouldControlKey), Constants.RateShouldControlValues.Never.index());
				editor.commit();
				mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + mContext.getPackageName())));
			}
		};
		Command laterCommand = new Command() {
			@Override
			public void execute() {
				editor.putInt(mContext.getResources().getString(R.string.rateAppDefaultOpenCountKey), Tools.getPreferenceInt(mContext, R.string.rateAppDefaultOpenCountKey) + 10);
				editor.commit();
			}
		};
		Command neverDialog = new Command() {
			@Override
			public void execute() {
				if (editor != null) {
					editor.putInt(mContext.getResources().getString(R.string.rateShouldControlKey), Constants.RateShouldControlValues.Never.index());
					editor.commit();
				}
			}
		};
		dialog = DialogTools.confirmWithCancelDialog(mContext, neverDialog, laterCommand, rateCommand, R.string.information,
				mContext.getString(R.string.rateDialogTitle),
				new String[] {mContext.getString(R.string. neverAsk), mContext.getString(R.string.later), mContext.getString(R.string.appRate)});
		dialog.show();
	}

	public static void showAboutDialog(Context context, int appNameID) {
		String aboutMessage = context.getResources().getString(appNameID) + " " + Tools.getApplicationVersion(context);
		aboutMessage += "\n MM Group";
		aboutMessage += "\n 2013";
		AlertDialog dialog = DialogTools.informationDialog(context, R.string.about, aboutMessage);
		dialog.show();
		TextView messageText = (TextView) dialog.findViewById(android.R.id.message);
		messageText.setGravity(Gravity.CENTER);
	}

	public static void setAccountSpinnerValue(Cursor cursor, Spinner spinner, long value) {
		int position = 0;
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			if (DBTools.getCursorColumnValueLong(cursor, MoneyManagerProviderMetaData.AccountTableMetaData._ID) == value) {
				spinner.setSelection(position);
				break;
			}
			position++;
		}
	}

	public static String getFullAmountText(double amount, String currencySign, boolean addSpace) {
		String spaceSymbol = addSpace?" ":"";
		if (Constants.currencySignPosition == Constants.currencySignPositionRight)
			return formatDecimalInUserFormat(amount) + spaceSymbol + getCurrencySymbol(currencySign);
		else
			return getCurrencySymbol(currencySign) + spaceSymbol + formatDecimalInUserFormat(amount);
	}

	public static String getValueFromKeyArray(Context context, int keysArrayId, int valuesArrayID, String key) {
		try {
			String[] keysArray = context.getResources().getStringArray(keysArrayId);
			String[] valuesArray = context.getResources().getStringArray(valuesArrayID);
			return valuesArray[Arrays.asList(keysArray).indexOf(key)];
		}
		catch (Exception e) {
			return "null";
		}
	}

	public static void removeAds(final Context context) {
				/*1.Settingsden silek
				* 2.lisenziyani yoxlayaq
				* 3.Eger lisenziya yoxdursa, pronun sehifesini acaq getsin alsin*/
		try {
			Tools.removePreferense(context, R.string.proInstalledKey);
		} catch (Exception e) {
		}
		if (!Tools.proVersionExists(context)) {
			try {
				Tools.removePreferense(context, R.string.proInstalledKey);
			} catch (Exception e) {
			}
			try {
				context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + "com.jgmoneymanager.paid")));
			} catch (android.content.ActivityNotFoundException anfe) {
				Log.i("LICENSE", "checkLicense - open store error");
				context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + "com.jgmoneymanager.paid")));
			}
		}
		else
			DialogTools.toastDialog(context, R.string.adsRemoveSummaryRemoved, Toast.LENGTH_SHORT);
	}

	public static void backupToMemory(final Context context) {
		File file = new File(Constants.backupDirectory);
		if (!file.exists())
			if (!file.mkdirs()) {
				AlertDialog warning = DialogTools.warningDialog(context, R.string.msgWarning, context.getString(R.string.msgChooseBackupFolder));
				warning.show();
				return;
			}
		final EditText input = new EditText(context);
		input.setText(Tools.DateToString(Tools.getCurrentDateTime(), Constants.DateFormatBackup));
		Command cmd = new Command() {
			@Override
			public void execute() {
				BackupDatabaseFileTask backupDBTask = new BackupDatabaseFileTask(context, input.getText().toString());
				backupDBTask.execute("");
			}
		};
		AlertDialog inputDialog = DialogTools.InputDialog(context, cmd, R.string.msgBckFileName, input, R.drawable.ic_menu_manage);
		inputDialog.show();
		inputDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(input.getText().toString().trim().length() != 0);
	}

	public static String getApplicationName(Context context) {
		ApplicationInfo applicationInfo = context.getApplicationInfo();
		int stringId = applicationInfo.labelRes;
		return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString(stringId);
	}

	public static int getVersionCode(Context context) {
		try {
			PackageInfo packInfo = context.getPackageManager().getPackageInfo("com.jgmoneymanager.main", 0);
			return packInfo.versionCode;
		} catch (PackageManager.NameNotFoundException e) {
			Tracker myTracker = EasyTracker.getInstance(context);
			myTracker.set(Fields.SCREEN_NAME, "loadSettings- Get versionCode");
			myTracker.send(MapBuilder.createAppView().build());
			return 0;
		}
	}
}