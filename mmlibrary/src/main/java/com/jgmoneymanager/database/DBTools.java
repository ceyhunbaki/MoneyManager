package com.jgmoneymanager.database;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.StaleDataException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.jgmoneymanager.database.MoneyManagerProvider.DatabaseHelper;
import com.jgmoneymanager.mmlibrary.R;
import com.jgmoneymanager.services.CategorySrv;
import com.jgmoneymanager.services.CurrencySrv;
import com.jgmoneymanager.services.PaymentMethodsSrv;
import com.jgmoneymanager.services.TransactionStatusSrv;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import java.util.Date;

public class DBTools {
    private static DatabaseHelper dbHelper;
	private static SQLiteDatabase database;
	
	public static void execQuery(Context context, String query) {
		try {
			dbHelper = new DatabaseHelper(context);
			database = dbHelper.getWritableDatabase();
			database.execSQL(query);
		}
		catch (SQLException ex)
		{
		}
		/*finally
		{
			if (database.isOpen())
				database.close();
		}*/
	}

	public static String execQueryWithReturn(Context context, String query) {
		String result = null;
		try {
			dbHelper = new DatabaseHelper(context);
			database = dbHelper.getWritableDatabase();
			Cursor cursor = database.rawQuery(query, null);
			if (cursor.moveToFirst())
				result = cursor.getString(0);
			cursor.close();
		}
		catch (SQLException ex)
		{
		}
		/*finally
		{
			if (database.isOpen())
				database.close();
		}*/
		return result;
	}

	public static Cursor createCursor(Context context, String query) {
		Cursor cursor = null;
		try {
			dbHelper = new DatabaseHelper(context);
			database = dbHelper.getWritableDatabase();
			cursor = database.rawQuery(query, null);
		}
		catch (SQLException ex)
		{
			/*if (database.isOpen())
				database.close();*/
		}
		return cursor;
	}
	
	public static void closeDatabase() {
		try {
			/*if (database.isOpen())
				database.close();*/
		}
		catch (Exception e) {
			Log.e("Dbtools.closeDatabase", e.getMessage());
		}
	}
	
	//Cursor methods

	// Cursor methods
	public static String getCursorColumnValue(Cursor cursor, String columnName) {
		try {
			return cursor.getString(cursor.getColumnIndex(columnName));
		} catch (StaleDataException sd) {
			int position = cursor.getPosition();
			cursor.requery();
			cursor.moveToPosition(position);
			return cursor.getString(cursor.getColumnIndex(columnName));
		} catch (Exception ex) {
			return null;
		}
	}

	public static Integer getCursorColumnValueInt(Cursor cursor,
			String columnName) {
		try {
			return Integer.parseInt(getCursorColumnValue(cursor, columnName));
		} catch (Exception ex) {
			return 0;
		}
	}

	public static Long getCursorColumnValueLong(Cursor cursor, String columnName) {
		try {
			return Long.parseLong(getCursorColumnValue(cursor, columnName));
		} catch (Exception ex) {
			return (long) 0;
		}
	}

	public static Float getCursorColumnValueFloat(Cursor cursor,
			String columnName) {
		try {
			return Float.parseFloat(getCursorColumnValue(cursor, columnName));
		} catch (Exception ex) {
			return 0f;
		}
	}

	public static Double getCursorColumnValueDouble(Cursor cursor,
			String columnName) {
		try {
			return Double.parseDouble(getCursorColumnValue(cursor, columnName));
		} catch (Exception ex) {
			return 0d;
		}
	}

	public static Date getCursorColumnValueDate(Cursor cursor,
			String columnName, String format) {
		try {
			String value = getCursorColumnValue(cursor, columnName);
			if (value == null)
				return null;
			else 
				return Tools.StringToDate(value, format);
		} catch (Exception ex) {
			return Tools.getCurrentDate();
		}
	}

	public static Date getCursorColumnValueDate(Cursor cursor, String columnName) {
		return getCursorColumnValueDate(cursor, columnName, Constants.DateFormatDB);
	}
	
	public static void insertFirstItems(final Context context) {
		//Locale locale = context.getResources().getConfiguration().locale;
		/*if (locale.getLanguage().equals("az")) {
			CurrencySrv.insertCurrency(context, context.getString(R.string.usd), "USD", 0);
			CurrencySrv.insertCurrency(context, context.getString(R.string.eur), "EUR", 0);
			CurrencySrv.insertCurrency(context, context.getString(R.string.azn), "AZN", 1);
			CurrencySrv.insertCurrency(context, context.getString(R.string.rub), "RUB", 0);			
		}
		else if (locale.getLanguage().equals("ru")) {
			CurrencySrv.insertCurrency(context, context.getString(R.string.usd), "USD", 0);
			CurrencySrv.insertCurrency(context, context.getString(R.string.eur), "EUR", 0);
			CurrencySrv.insertCurrency(context, context.getString(R.string.azn), "AZN", 0);
			CurrencySrv.insertCurrency(context, context.getString(R.string.rub), "RUB", 1);			
		}
		else if (locale.getCountry().equals("GB") || locale.getCountry().equals("DE") ||
				locale.getCountry().equals("FR") || locale.getCountry().equals("IT")) {
			CurrencySrv.insertCurrency(context, context.getString(R.string.usd), "USD", 0);
			CurrencySrv.insertCurrency(context, context.getString(R.string.eur), "EUR", 1);
			CurrencySrv.insertCurrency(context, context.getString(R.string.azn), "AZN", 0);
			CurrencySrv.insertCurrency(context, context.getString(R.string.rub), "RUB", 0);			
		}
		else {
			CurrencySrv.insertCurrency(context, context.getString(R.string.usd), "USD", 1);
			CurrencySrv.insertCurrency(context, context.getString(R.string.eur), "EUR", 0);
			CurrencySrv.insertCurrency(context, context.getString(R.string.azn), "AZN", 0);
			CurrencySrv.insertCurrency(context, context.getString(R.string.rub), "RUB", 0);			
		}*/

		CurrencySrv.insertCurrency(context, context.getString(R.string.usd), "USD", 0, R.string.usd);
		CurrencySrv.insertCurrency(context, context.getString(R.string.eur), "EUR", 0, R.string.eur);
		CurrencySrv.insertCurrency(context, context.getString(R.string.azn), "AZN", 0, R.string.azn);
		CurrencySrv.insertCurrency(context, context.getString(R.string.rub), "RUB", 0, R.string.rub);
		CurrencySrv.insertCurrency(context, context.getString(R.string.gbp), "GBP", 0, R.string.gbp);
		CurrencySrv.insertCurrency(context, context.getString(R.string.aed), "AED", 0, R.string.aed);
		CurrencySrv.insertCurrency(context, context.getString(R.string.afn), "AFN", 0, R.string.afn);
		//ALL sözü converterdə başda çıxır və ALL sözü ilə qarışır
		//CurrencySrv.insertCurrency(context, context.getString(R.string.allCur), "ALL", 0, R.string.allCur);
		CurrencySrv.insertCurrency(context, context.getString(R.string.ang), "ANG", 0, R.string.ang);
		CurrencySrv.insertCurrency(context, context.getString(R.string.aoa), "AOA", 0, R.string.aoa);
		CurrencySrv.insertCurrency(context, context.getString(R.string.ars), "ARS", 0, R.string.ars);
		CurrencySrv.insertCurrency(context, context.getString(R.string.aud), "AUD", 0, R.string.aud);
		CurrencySrv.insertCurrency(context, context.getString(R.string.awg), "AWG", 0, R.string.awg);
		CurrencySrv.insertCurrency(context, context.getString(R.string.bam), "BAM", 0, R.string.bam);
		CurrencySrv.insertCurrency(context, context.getString(R.string.bbd), "BBD", 0, R.string.bbd);
		CurrencySrv.insertCurrency(context, context.getString(R.string.bdt), "BDT", 0, R.string.bdt);
		CurrencySrv.insertCurrency(context, context.getString(R.string.bgn), "BGN", 0, R.string.bgn);
		CurrencySrv.insertCurrency(context, context.getString(R.string.bhd), "BHD", 0, R.string.bhd);
		CurrencySrv.insertCurrency(context, context.getString(R.string.bif), "BIF", 0, R.string.bif);
		CurrencySrv.insertCurrency(context, context.getString(R.string.bmd), "BMD", 0, R.string.bmd);
		CurrencySrv.insertCurrency(context, context.getString(R.string.bnd), "BND", 0, R.string.bnd);
		CurrencySrv.insertCurrency(context, context.getString(R.string.bob), "BOB", 0, R.string.bob);
		CurrencySrv.insertCurrency(context, context.getString(R.string.brl), "BRL", 0, R.string.brl);
		CurrencySrv.insertCurrency(context, context.getString(R.string.bsd), "BSD", 0, R.string.bsd);
		CurrencySrv.insertCurrency(context, context.getString(R.string.btn), "BTN", 0, R.string.btn);
		CurrencySrv.insertCurrency(context, context.getString(R.string.bwp), "BWP", 0, R.string.bwp);
		CurrencySrv.insertCurrency(context, context.getString(R.string.byr), "BYR", 0, R.string.byr);
		CurrencySrv.insertCurrency(context, context.getString(R.string.bzd), "BZD", 0, R.string.bzd);
		CurrencySrv.insertCurrency(context, context.getString(R.string.cad), "CAD", 0, R.string.cad);
		CurrencySrv.insertCurrency(context, context.getString(R.string.cdf), "CDF", 0, R.string.cdf);
		CurrencySrv.insertCurrency(context, context.getString(R.string.chf), "CHF", 0, R.string.chf);
		CurrencySrv.insertCurrency(context, context.getString(R.string.clp), "CLP", 0, R.string.clp);
		CurrencySrv.insertCurrency(context, context.getString(R.string.cny), "CNY", 0, R.string.cny);
		CurrencySrv.insertCurrency(context, context.getString(R.string.cop), "COP", 0, R.string.cop);
		CurrencySrv.insertCurrency(context, context.getString(R.string.crc), "CRC", 0, R.string.crc);
		CurrencySrv.insertCurrency(context, context.getString(R.string.cuc), "CUC", 0, R.string.cuc);
		CurrencySrv.insertCurrency(context, context.getString(R.string.cup), "CUP", 0, R.string.cup);
		CurrencySrv.insertCurrency(context, context.getString(R.string.cve), "CVE", 0, R.string.cve);
		CurrencySrv.insertCurrency(context, context.getString(R.string.czk), "CZK", 0, R.string.czk);
		CurrencySrv.insertCurrency(context, context.getString(R.string.djf), "DJF", 0, R.string.djf);
		CurrencySrv.insertCurrency(context, context.getString(R.string.dkk), "DKK", 0, R.string.dkk);
		CurrencySrv.insertCurrency(context, context.getString(R.string.dop), "DOP", 0, R.string.dop);
		CurrencySrv.insertCurrency(context, context.getString(R.string.dzd), "DZD", 0, R.string.dzd);
		CurrencySrv.insertCurrency(context, context.getString(R.string.egp), "EGP", 0, R.string.egp);
		CurrencySrv.insertCurrency(context, context.getString(R.string.ern), "ERN", 0, R.string.ern);
		CurrencySrv.insertCurrency(context, context.getString(R.string.etb), "ETB", 0, R.string.etb);
		CurrencySrv.insertCurrency(context, context.getString(R.string.fjd), "FJD", 0, R.string.fjd);
		CurrencySrv.insertCurrency(context, context.getString(R.string.fkp), "FKP", 0, R.string.fkp);
		CurrencySrv.insertCurrency(context, context.getString(R.string.gel), "GEL", 0, R.string.gel);
		CurrencySrv.insertCurrency(context, context.getString(R.string.ggp), "GGP", 0, R.string.ggp);
		CurrencySrv.insertCurrency(context, context.getString(R.string.ghs), "GHS", 0, R.string.ghs);
		CurrencySrv.insertCurrency(context, context.getString(R.string.gip), "GIP", 0, R.string.gip);
		CurrencySrv.insertCurrency(context, context.getString(R.string.gmd), "GMD", 0, R.string.gmd);
		CurrencySrv.insertCurrency(context, context.getString(R.string.gnf), "GNF", 0, R.string.gnf);
		CurrencySrv.insertCurrency(context, context.getString(R.string.gtq), "GTQ", 0, R.string.gtq);
		CurrencySrv.insertCurrency(context, context.getString(R.string.gyd), "GYD", 0, R.string.gyd);
		CurrencySrv.insertCurrency(context, context.getString(R.string.hkd), "HKD", 0, R.string.hkd);
		CurrencySrv.insertCurrency(context, context.getString(R.string.hnl), "HNL", 0, R.string.hnl);
		CurrencySrv.insertCurrency(context, context.getString(R.string.hrk), "HRK", 0, R.string.hrk);
		CurrencySrv.insertCurrency(context, context.getString(R.string.htg), "HTG", 0, R.string.htg);
		CurrencySrv.insertCurrency(context, context.getString(R.string.huf), "HUF", 0, R.string.huf);
		CurrencySrv.insertCurrency(context, context.getString(R.string.idr), "IDR", 0, R.string.idr);
		CurrencySrv.insertCurrency(context, context.getString(R.string.ils), "ILS", 0, R.string.ils);
		CurrencySrv.insertCurrency(context, context.getString(R.string.imp), "IMP", 0, R.string.imp);
		CurrencySrv.insertCurrency(context, context.getString(R.string.inr), "INR", 0, R.string.inr);
		CurrencySrv.insertCurrency(context, context.getString(R.string.iqd), "IQD", 0, R.string.iqd);
		CurrencySrv.insertCurrency(context, context.getString(R.string.irr), "IRR", 0, R.string.irr);
		CurrencySrv.insertCurrency(context, context.getString(R.string.isk), "ISK", 0, R.string.isk);
		CurrencySrv.insertCurrency(context, context.getString(R.string.jep), "JEP", 0, R.string.jep);
		CurrencySrv.insertCurrency(context, context.getString(R.string.jmd), "JMD", 0, R.string.jmd);
		CurrencySrv.insertCurrency(context, context.getString(R.string.jod), "JOD", 0, R.string.jod);
		CurrencySrv.insertCurrency(context, context.getString(R.string.jpy), "JPY", 0, R.string.jpy);
		CurrencySrv.insertCurrency(context, context.getString(R.string.kes), "KES", 0, R.string.kes);
		CurrencySrv.insertCurrency(context, context.getString(R.string.kgs), "KGS", 0, R.string.kgs);
		CurrencySrv.insertCurrency(context, context.getString(R.string.khr), "KHR", 0, R.string.khr);
		CurrencySrv.insertCurrency(context, context.getString(R.string.kmf), "KMF", 0, R.string.kmf);
		CurrencySrv.insertCurrency(context, context.getString(R.string.kpw), "KPW", 0, R.string.kpw);
		CurrencySrv.insertCurrency(context, context.getString(R.string.krw), "KRW", 0, R.string.krw);
		CurrencySrv.insertCurrency(context, context.getString(R.string.kwd), "KWD", 0, R.string.kwd);
		CurrencySrv.insertCurrency(context, context.getString(R.string.kyd), "KYD", 0, R.string.kyd);
		CurrencySrv.insertCurrency(context, context.getString(R.string.kzt), "KZT", 0, R.string.kzt);
		CurrencySrv.insertCurrency(context, context.getString(R.string.lak), "LAK", 0, R.string.lak);
		CurrencySrv.insertCurrency(context, context.getString(R.string.lbp), "LBP", 0, R.string.lbp);
		CurrencySrv.insertCurrency(context, context.getString(R.string.lkr), "LKR", 0, R.string.lkr);
		CurrencySrv.insertCurrency(context, context.getString(R.string.lrd), "LRD", 0, R.string.lrd);
		CurrencySrv.insertCurrency(context, context.getString(R.string.lsl), "LSL", 0, R.string.lsl);
		CurrencySrv.insertCurrency(context, context.getString(R.string.ltl), "LTL", 0, R.string.ltl);
		CurrencySrv.insertCurrency(context, context.getString(R.string.lvl), "LVL", 0, R.string.lvl);
		CurrencySrv.insertCurrency(context, context.getString(R.string.lyd), "LYD", 0, R.string.lyd);
		CurrencySrv.insertCurrency(context, context.getString(R.string.mad), "MAD", 0, R.string.mad);
		CurrencySrv.insertCurrency(context, context.getString(R.string.mdl), "MDL", 0, R.string.mdl);
		CurrencySrv.insertCurrency(context, context.getString(R.string.mga), "MGA", 0, R.string.mga);
		CurrencySrv.insertCurrency(context, context.getString(R.string.mkd), "MKD", 0, R.string.mkd);
		CurrencySrv.insertCurrency(context, context.getString(R.string.mmk), "MMK", 0, R.string.mmk);
		CurrencySrv.insertCurrency(context, context.getString(R.string.mnt), "MNT", 0, R.string.mnt);
		CurrencySrv.insertCurrency(context, context.getString(R.string.mop), "MOP", 0, R.string.mop);
		CurrencySrv.insertCurrency(context, context.getString(R.string.mro), "MRO", 0, R.string.mro);
		CurrencySrv.insertCurrency(context, context.getString(R.string.mur), "MUR", 0, R.string.mur);
		CurrencySrv.insertCurrency(context, context.getString(R.string.mvr), "MVR", 0, R.string.mvr);
		CurrencySrv.insertCurrency(context, context.getString(R.string.mwk), "MWK", 0, R.string.mwk);
		CurrencySrv.insertCurrency(context, context.getString(R.string.mxn), "MXN", 0, R.string.mxn);
		CurrencySrv.insertCurrency(context, context.getString(R.string.myr), "MYR", 0, R.string.myr);
		CurrencySrv.insertCurrency(context, context.getString(R.string.mzn), "MZN", 0, R.string.mzn);
		CurrencySrv.insertCurrency(context, context.getString(R.string.nad), "NAD", 0, R.string.nad);
		CurrencySrv.insertCurrency(context, context.getString(R.string.ngn), "NGN", 0, R.string.ngn);
		CurrencySrv.insertCurrency(context, context.getString(R.string.nio), "NIO", 0, R.string.nio);
		CurrencySrv.insertCurrency(context, context.getString(R.string.nok), "NOK", 0, R.string.nok);
		CurrencySrv.insertCurrency(context, context.getString(R.string.npr), "NPR", 0, R.string.npr);
		CurrencySrv.insertCurrency(context, context.getString(R.string.nzd), "NZD", 0, R.string.nzd);
		CurrencySrv.insertCurrency(context, context.getString(R.string.omr), "OMR", 0, R.string.omr);
		CurrencySrv.insertCurrency(context, context.getString(R.string.pab), "PAB", 0, R.string.pab);
		CurrencySrv.insertCurrency(context, context.getString(R.string.pen), "PEN", 0, R.string.pen);
		CurrencySrv.insertCurrency(context, context.getString(R.string.pgk), "PGK", 0, R.string.pgk);
		CurrencySrv.insertCurrency(context, context.getString(R.string.php), "PHP", 0, R.string.php);
		CurrencySrv.insertCurrency(context, context.getString(R.string.pkr), "PKR", 0, R.string.pkr);
		CurrencySrv.insertCurrency(context, context.getString(R.string.pln), "PLN", 0, R.string.pln);
		CurrencySrv.insertCurrency(context, context.getString(R.string.pyg), "PYG", 0, R.string.pyg);
		CurrencySrv.insertCurrency(context, context.getString(R.string.qar), "QAR", 0, R.string.qar);
		CurrencySrv.insertCurrency(context, context.getString(R.string.ron), "RON", 0, R.string.ron);
		CurrencySrv.insertCurrency(context, context.getString(R.string.rsd), "RSD", 0, R.string.rsd);
		CurrencySrv.insertCurrency(context, context.getString(R.string.rwf), "RWF", 0, R.string.rwf);
		CurrencySrv.insertCurrency(context, context.getString(R.string.sar), "SAR", 0, R.string.sar);
		CurrencySrv.insertCurrency(context, context.getString(R.string.sbd), "SBD", 0, R.string.sbd);
		CurrencySrv.insertCurrency(context, context.getString(R.string.scr), "SCR", 0, R.string.scr);
		CurrencySrv.insertCurrency(context, context.getString(R.string.sdg), "SDG", 0, R.string.sdg);
		CurrencySrv.insertCurrency(context, context.getString(R.string.sek), "SEK", 0, R.string.sek);
		CurrencySrv.insertCurrency(context, context.getString(R.string.sgd), "SGD", 0, R.string.sgd);
		CurrencySrv.insertCurrency(context, context.getString(R.string.shp), "SHP", 0, R.string.shp);
		CurrencySrv.insertCurrency(context, context.getString(R.string.sll), "SLL", 0, R.string.sll);
		CurrencySrv.insertCurrency(context, context.getString(R.string.sos), "SOS", 0, R.string.sos);
		CurrencySrv.insertCurrency(context, context.getString(R.string.spl), "SPL", 0, R.string.spl);
		CurrencySrv.insertCurrency(context, context.getString(R.string.srd), "SRD", 0, R.string.srd);
		CurrencySrv.insertCurrency(context, context.getString(R.string.std), "STD", 0, R.string.std);
		CurrencySrv.insertCurrency(context, context.getString(R.string.svc), "SVC", 0, R.string.svc);
		CurrencySrv.insertCurrency(context, context.getString(R.string.syp), "SYP", 0, R.string.syp);
		CurrencySrv.insertCurrency(context, context.getString(R.string.szl), "SZL", 0, R.string.szl);
		CurrencySrv.insertCurrency(context, context.getString(R.string.thb), "THB", 0, R.string.thb);
		CurrencySrv.insertCurrency(context, context.getString(R.string.tjs), "TJS", 0, R.string.tjs);
		CurrencySrv.insertCurrency(context, context.getString(R.string.tmt), "TMT", 0, R.string.tmt);
		CurrencySrv.insertCurrency(context, context.getString(R.string.tnd), "TND", 0, R.string.tnd);
		CurrencySrv.insertCurrency(context, context.getString(R.string.top), "TOP", 0, R.string.top);
		CurrencySrv.insertCurrency(context, context.getString(R.string.tryCur), "TRY", 0, R.string.tryCur);
		CurrencySrv.insertCurrency(context, context.getString(R.string.ttd), "TTD", 0, R.string.ttd);
		CurrencySrv.insertCurrency(context, context.getString(R.string.tvd), "TVD", 0, R.string.tvd);
		CurrencySrv.insertCurrency(context, context.getString(R.string.twd), "TWD", 0, R.string.twd);
		CurrencySrv.insertCurrency(context, context.getString(R.string.tzs), "TZS", 0, R.string.tzs);
		CurrencySrv.insertCurrency(context, context.getString(R.string.uah), "UAH", 0, R.string.uah);
		CurrencySrv.insertCurrency(context, context.getString(R.string.ugx), "UGX", 0, R.string.ugx);
		CurrencySrv.insertCurrency(context, context.getString(R.string.uyu), "UYU", 0, R.string.uyu);
		CurrencySrv.insertCurrency(context, context.getString(R.string.uzs), "UZS", 0, R.string.uzs);
		CurrencySrv.insertCurrency(context, context.getString(R.string.vef), "VEF", 0, R.string.vef);
		CurrencySrv.insertCurrency(context, context.getString(R.string.vnd), "VND", 0, R.string.vnd);
		CurrencySrv.insertCurrency(context, context.getString(R.string.vuv), "VUV", 0, R.string.vuv);
		CurrencySrv.insertCurrency(context, context.getString(R.string.wst), "WST", 0, R.string.wst);
		CurrencySrv.insertCurrency(context, context.getString(R.string.xcd), "XCD", 0, R.string.xcd);
		CurrencySrv.insertCurrency(context, context.getString(R.string.xof), "XOF", 0, R.string.xof);
		CurrencySrv.insertCurrency(context, context.getString(R.string.xpf), "XPF", 0, R.string.xpf);
		CurrencySrv.insertCurrency(context, context.getString(R.string.yer), "YER", 0, R.string.yer);
		CurrencySrv.insertCurrency(context, context.getString(R.string.zar), "ZAR", 0, R.string.zar);
		CurrencySrv.insertCurrency(context, context.getString(R.string.zmw), "ZMW", 0, R.string.zmw);
		CurrencySrv.insertCurrency(context, context.getString(R.string.zwd), "ZWD", 0, R.string.zwd);
		
		long mainCategoryId = CategorySrv.insertMainCategory(context, context.getResources().getString(R.string.incomeCategories), true, R.string.incomeCategories);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.salary), true, R.string.salary);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.equities), true, R.string.equities);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.otherincome), true, R.string.otherincome);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.pensions), true, R.string.pensions);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.personalsavings), true, R.string.personalsavings);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.rents), true, R.string.rents);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.parttimework), true, R.string.parttimework);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.sosialsecurity), true, R.string.sosialsecurity);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.debts), true, R.string.debts);
		mainCategoryId = CategorySrv.insertMainCategory(context, context.getResources().getString(R.string.utilities), false, R.string.utilities);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.electric), false, R.string.electric);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.telephone), false, R.string.telephone);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.internet), false, R.string.internet);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.cabletv), false, R.string.cabletv);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.garbage), false, R.string.garbage);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.gas), false, R.string.gas);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.water), false, R.string.water);
		mainCategoryId = CategorySrv.insertMainCategory(context, context.getResources().getString(R.string.home), false, R.string.home);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.shopping), false, R.string.shopping);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.furniture), false, R.string.furniture);
		mainCategoryId = CategorySrv.insertMainCategory(context, context.getResources().getString(R.string.healthcare), false, R.string.healthcare);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.medical), false, R.string.medical);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.doctor), false, R.string.doctor);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.analysis), false, R.string.analysis);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.dental), false, R.string.dental);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.insurance), false, R.string.insurance);
		mainCategoryId = CategorySrv.insertMainCategory(context, context.getResources().getString(R.string.automobile), false, R.string.automobile);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.fuel), false, R.string.fuel);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.parts), false, R.string.parts);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.service), false, R.string.service);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.parking), false, R.string.parking);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.insurance), false, R.string.insurance);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.other), false, R.string.other);
		mainCategoryId = CategorySrv.insertMainCategory(context, context.getResources().getString(R.string.travel), false, R.string.travel);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.hotel), false, R.string.hotel);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.airplane), false, R.string.airplane);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.carrental), false, R.string.carrental);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.ticket), false, R.string.ticket);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.shopping), false, R.string.shopping);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.food), false, R.string.food);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.other), false, R.string.other);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.taxi), false, R.string.taxi);
		mainCategoryId = CategorySrv.insertMainCategory(context, context.getResources().getString(R.string.vacation), false, R.string.vacation);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.hotel), false, R.string.hotel);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.airplane), false, R.string.airplane);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.carrental), false, R.string.carrental);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.ticket), false, R.string.ticket);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.shopping), false, R.string.shopping);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.food), false, R.string.food);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.other), false, R.string.other);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.taxi), false, R.string.taxi);
		mainCategoryId = CategorySrv.insertMainCategory(context, context.getResources().getString(R.string.child), false, R.string.child);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.clothing), false, R.string.clothing);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.medical), false, R.string.medical);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.childcare), false, R.string.childcare);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.kids), false, R.string.kids);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.education), false, R.string.education);
		mainCategoryId = CategorySrv.insertMainCategory(context, context.getResources().getString(R.string.entertainment), false, R.string.entertainment);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.concert), false, R.string.concert);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.cinema), false, R.string.cinema);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.other), false, R.string.other);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.sports), false, R.string.sports);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.party), false, R.string.party);
		mainCategoryId = CategorySrv.insertMainCategory(context, context.getResources().getString(R.string.food), false, R.string.food);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.restaurant), false, R.string.restaurant);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.groceries), false, R.string.groceries);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.snack), false, R.string.snack);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.other), false, R.string.other);
		mainCategoryId = CategorySrv.insertMainCategory(context, context.getResources().getString(R.string.insurance), false, R.string.insurance);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.auto), false, R.string.auto);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.home), false, R.string.home);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.health), false, R.string.health);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.life), false, R.string.life);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.other), false, R.string.other);
		mainCategoryId = CategorySrv.insertMainCategory(context, context.getResources().getString(R.string.loans), false, R.string.loans);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.auto), false, R.string.auto);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.homeequity), false, R.string.homeequity);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.mortgage), false, R.string.mortgage);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.other), false, R.string.other);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.student), false, R.string.student);
		mainCategoryId = CategorySrv.insertMainCategory(context, context.getResources().getString(R.string.tax), false, R.string.tax);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.other), false, R.string.other);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.propertytax), false, R.string.propertytax);
		mainCategoryId = CategorySrv.insertMainCategory(context, context.getResources().getString(R.string.other), false, R.string.other);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.other), false, R.string.other);
		CategorySrv.insertSubCategory(context, mainCategoryId, context.getResources().getString(R.string.debts), false, R.string.debts);

		TransactionStatusSrv.insertStatus(context, context.getResources().getString(R.string.cleared), 1, R.string.cleared);
		TransactionStatusSrv.insertStatus(context, context.getResources().getString(R.string.uncleared), 2, R.string.uncleared);
		TransactionStatusSrv.insertStatus(context, context.getResources().getString(R.string.void_), 3, R.string.void_);

		PaymentMethodsSrv.insertMethod(context, context.getResources().getString(R.string.cash), 1, R.string.cash);
		PaymentMethodsSrv.insertMethod(context, context.getResources().getString(R.string.card), 2, R.string.card);
		PaymentMethodsSrv.insertMethod(context, context.getResources().getString(R.string.check), 3, R.string.check);

		/*final Cursor cursor = context.getContentResolver().query(CurrencyTableMetaData.CONTENT_URI, null, null, null, CurrencyTableMetaData.NAME);
		Command cmd = new Command() {				
			@Override
			public void execute() {
				cursor.moveToPosition(Constants.cursorPosition);
				CurrencySrv.setDefaultCurrency(context, DBTools.getCursorColumnValueLong(cursor, CurrencyTableMetaData._ID));
				AccountEdit.insertAccount(context, context.getString(R.string.cash), "0", "1");
				AccountEdit.insertAccount(context, context.getString(R.string.bank), "0", "0");
			}
		};
		AlertDialog dialog = DialogTools.RadioListDialog(context, cmd, R.string.msgSetDefaultCurrency, cursor, CurrencyTableMetaData.NAME, false);
		dialog.show();*/
	}

}
