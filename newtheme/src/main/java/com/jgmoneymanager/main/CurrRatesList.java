package com.jgmoneymanager.main;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CurrRatesTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CurrencyTableMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VCurrRatesViewMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.services.TransactionSrv;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import java.util.Date;

public class CurrRatesList extends MyActivity {

	Cursor cursor = null;
	long selectedID = 0;
	ListView listView;

	Date periodStart, periodEnd;

	private AdView adView;

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
		View child = inflater.inflate(R.layout.currency_rates_list, null);
		mainLayout.addView(child, params);

		listView = (ListView) findViewById(R.id.curList);
		listView.setScrollingCacheEnabled(true);
		listView.setCacheColorHint(00000000);

		if (savedInstanceState != null) {
			periodEnd = Tools.getDateFromBundle(savedInstanceState, "periodEnd");
			periodStart = Tools.getDateFromBundle(savedInstanceState, "periodStart");
		}

		// Create the adView
		if (!Tools.proVersionExists(this)) {
//			adView = new AdView(this, AdSize.BANNER, "ca-app-pub-5995868530154544/3468276114");
//			LinearLayout layout = (LinearLayout)findViewById(R.id.onlyListAdsLayout);
//			layout.addView(adView);
//			AdRequest adRequest = new AdRequest();
//			adView.loadAd(adRequest);
			MobileAds.initialize(getApplicationContext(), "ca-app-pub-5995868530154544/3468276114");
			adView = new AdView(this);
			adView.setAdSize(AdSize.SMART_BANNER);
			adView.setAdUnitId("ca-app-pub-5995868530154544/3468276114");
			LinearLayout layout = (LinearLayout) findViewById(R.id.onlyListAdsLayout);
			layout.addView(adView);
			AdRequest adRequest = new AdRequest.Builder().build();
			adView.loadAd(adRequest);
		}

		fillData(listView);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_filter, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			final int minYear = TransactionSrv.getMinTransactionYear(CurrRatesList.this);
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

			if (periodStart == null) {
				periodStart = Tools.StringToDate(String.valueOf(minYear), Constants.DateFormatYear);
				periodEnd = Tools.getCurrentDate();
			}

			spinnerYearFrom.setSelection(periodStart.getYear() - minYear + 1900);
			spinnerMonthFrom.setSelection(periodStart.getMonth());
			spinnerYearTo.setSelection(periodEnd.getYear() - minYear + 1900);
			spinnerMonthTo.setSelection(periodEnd.getMonth());

			final AlertDialog viewDialog = DialogTools.CustomDialog(this, view);

			final Command cmdPeriod = new Command() {
				@Override
				public void execute() {
					periodStart = new Date(spinnerYearFrom.getSelectedItemPosition() + minYear - 1900, spinnerMonthFrom.getSelectedItemPosition(), 1);
					periodEnd = Tools.lastDay(CurrRatesList.this, new Date(spinnerYearTo.getSelectedItemPosition() + minYear - 1900, spinnerMonthTo.getSelectedItemPosition(), 1));

					if (periodStart.compareTo(periodEnd) <= 0) {
						fillData(listView);
						viewDialog.dismiss();
					}
					else
						DialogTools.toastDialog(CurrRatesList.this, R.string.msgFromDateIsLessThanTo, Toast.LENGTH_LONG);
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
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Tools.putToBundle(outState, "periodEnd", periodEnd);
		Tools.putToBundle(outState, "periodStart", periodStart);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onDestroy() {
		try {
			super.onDestroy();
			if (adView != null)
				adView.destroy();
		}
		catch (Exception ex) {

		}
	}

	private void fillData(ListView listView) {
		String condition = null;
		//eger CurrencyListden cagirilibsa ona aidler cixsin
		if ((getIntent().getAction() != null) && getIntent().getAction().equals(Intent.ACTION_PICK))
		{
			Bundle bundle = getIntent().getExtras();
			if (bundle.containsKey(CurrencyTableMetaData._ID))
			{
				selectedID = bundle.getLong(CurrencyTableMetaData._ID);
				condition = "(" + VCurrRatesViewMetaData.FIRSTCURRID + " = " + selectedID +
						" or " + VCurrRatesViewMetaData.SECONDCURRID + " = " + selectedID + ")";
			}						
		}
		if (periodEnd != null) {
			if (condition != null)
				condition += " and ";
			else
				condition = " ";
			condition += VCurrRatesViewMetaData.RATEDATE + " <= '" + Tools.DateToDBString(periodEnd) + "' ";
		}
		if (periodStart != null) {
			if (condition != null)
				condition += " and ";
			else
				condition = " ";
			condition += VCurrRatesViewMetaData.RATEDATE + " >= '" + Tools.DateToDBString(periodStart) + "' ";
		}
		cursor = this.managedQuery(VCurrRatesViewMetaData.CONTENT_URI, null, condition, null, null);
		String[] from = new String[] { VCurrRatesViewMetaData.FIRSTCURRSIGN,
				VCurrRatesViewMetaData.SECONDCURRSIGN, VCurrRatesViewMetaData.VALUE, VCurrRatesViewMetaData.RATEDATE};
		int[] to = new int[] { R.id.lbCRFirstSign, R.id.lbCRSecondSign, R.id.lbCRValue, R.id.lbCRRateDate};

		// Now create an array adapter and set it to display using our row
		SimpleCursorAdapter notes = new MyListAdapter(cursor, this, R.layout.currency_rates_row, from, to);
		listView.setAdapter(notes);
	}

	private void editListItem(Long id)
	{
		Intent intent = new Intent(this, CurrRatesEdit.class);
		intent.setData(Uri.withAppendedPath(CurrRatesTableMetaData.CONTENT_URI, id.toString()));
		intent.setAction(Intent.ACTION_EDIT);
		startActivityForResult(intent, Constants.RequestNONE);
	}
	
	public class MyListAdapter extends SimpleCursorAdapter {

		Context context;
		
		public MyListAdapter(Cursor cursor, Context context, int rowId, String[] from, int[] to) {
			super(context, rowId, cursor, from, to);
			this.context = context;
		}
		
		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View view = super.getView(position, convertView, parent);
			final Cursor cursor1 = (Cursor) super.getItem(position);
			if (view == null) {
				LayoutInflater inflater = LayoutInflater.from(context);
				view = inflater.inflate(R.layout.listtransactionrow, null);
			}
			TextView tvRateDate = (TextView) view.findViewById(R.id.lbCRRateDate);
			tvRateDate.setText(Tools.DBDateToUserDate(DBTools.getCursorColumnValue(cursor1, VCurrRatesViewMetaData.RATEDATE)));

			tvRateDate = (TextView) view.findViewById(R.id.lbCRValue);
			tvRateDate.setText(Tools.formatDecimalInUserFormat(DBTools.getCursorColumnValueDouble(cursor1, VCurrRatesViewMetaData.VALUE), Integer.parseInt(Constants.rateDecimalCount)));

			ImageButton btEdit = (ImageButton) view.findViewById(R.id.igCREdit);
			btEdit.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					cursor.moveToPosition(position);
					editListItem(DBTools.getCursorColumnValueLong(cursor, CurrRatesTableMetaData._ID));
				}
			});
			
			return view;
		}
	}
	
	public void myClickHandler(View target) {
		switch (target.getId()) {
			case R.id.btCurrRatesAdd:
				Intent intent = new Intent(CurrRatesList.this, CurrRatesEdit.class);
				intent.setAction(Intent.ACTION_INSERT);
				if (selectedID != 0) {
					Bundle bundle = new Bundle();
					bundle.putLong(CurrRatesEdit.fromCurrencyIDTag, selectedID);
					intent.putExtras(bundle);
				}
				startActivityForResult(intent, Constants.RequestNONE);
				break;
			default:
				break;
		}
	}
		
}