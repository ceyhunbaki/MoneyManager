package com.jgmoneymanager.reports;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.main.R;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

public class ReportList extends MyActivity {

	String[] valuesArray;

	private AdView adView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

		LinearLayout mainLayout = (LinearLayout) findViewById(R.id.content_main_layout);
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		View child = inflater.inflate(R.layout.report_list, null);
		mainLayout.addView(child, params);

		valuesArray = getResources().getStringArray(R.array.ReportNames);
		Integer[] imagesArray = {
				R.drawable.rep_account_balance,
				R.drawable.rep1_account_2,
				R.drawable.rep2_category_2,
				R.drawable.rep3_subcategory_2,
				R.drawable.rep4_piechart_2,
				R.drawable.rep5_barchart_2,
				R.drawable.rep6_budget_growth_categ_2,
				R.drawable.rep7_budget_growth_total_2,
				R.drawable.rep8_budget_month_categ_2,
				R.drawable.rep9_budget_monthly_total_2,
				R.drawable.rep10_budget_comp_3,
				R.drawable.rep4_piechart_2,
				R.drawable.rep5_barchart_2
		};
		ListView listView = (ListView) findViewById(R.id.repList);
		listView.setAdapter(new MyListAdapter(this, valuesArray, imagesArray));
		listView.setBackgroundColor(getResources().getColor(R.color.White));
		
		// Create the adView
		/*if (!Tools.proVersionExists(this))*/ {
			MobileAds.initialize(getApplicationContext(), "ca-app-pub-5995868530154544/3468276114");
			adView = new AdView(this);
			adView.setAdSize(AdSize.SMART_BANNER);
			adView.setAdUnitId("ca-app-pub-5995868530154544/3468276114");
			LinearLayout layout = (LinearLayout) findViewById(R.id.onlyListAdsLayout);
			layout.addView(adView);
			AdRequest adRequest = new AdRequest.Builder().build();
			adView.loadAd(adRequest);
		}
	}

	public class MyListAdapter extends ArrayAdapter<String>{

		private final Activity context;
		private final String[] items;
		private final Integer[] imageId;
		public MyListAdapter(Activity context, String[] items, Integer[] imageId) {
			super(context, R.layout.list_reportlist_row, items);
			this.context = context;
			this.items = items;
			this.imageId = imageId;

		}
		@Override
		public View getView(final int position, View view, ViewGroup parent) {
			LayoutInflater inflater = context.getLayoutInflater();
			View rowView= inflater.inflate(R.layout.list_reportlist_row, null, true);

			TextView txtTitle = (TextView) rowView.findViewById(R.id.grp_child);
			txtTitle.setText(items[position]);
			txtTitle.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					onItemClickAction(position);
				}
			});

			ImageView imageView = (ImageView) rowView.findViewById(R.id.bt_grp_icon);
			imageView.setImageResource(imageId[position]);

			return rowView;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
    	try {
			if (adView != null)
				adView.destroy();
    	}
    	catch (Exception ex) {
    		
    	}
	}

	public static String getTitleString(Context context, int reportType) {
		String result = "";
		if (reportType == Constants.TransFTransaction.All.index())
			result = context.getResources().getString(R.string.menuSummary);
		else if (reportType == Constants.TransFTransaction.Income.index())
			result = context.getResources().getString(R.string.incomes);
		else if (reportType == Constants.TransFTransaction.Expence.index())
			result = context.getResources().getString(R.string.expences);
		return result;
	}

	private void onItemClickAction(int position) {
		Intent intent;
		if (valuesArray[position].equals(valuesArray[0])) {
			intent = new Intent(ReportList.this, AccountBalanceReport.class);
			startActivityForResult(intent, Constants.RequestNONE);
		} else if (valuesArray[position].equals(valuesArray[1])) {
			intent = new Intent(ReportList.this, ListReport.class);
			intent.setAction(Constants.ActionViewAccountReport);
			startActivityForResult(intent, Constants.RequestNONE);
		} else if (valuesArray[position].equals(valuesArray[2])) {
			intent = new Intent(ReportList.this, ListReport.class);
			intent.setAction(Constants.ActionViewCategoryReport);
			startActivityForResult(intent, Constants.RequestNONE);
		} else if (valuesArray[position].equals(valuesArray[3])) {
			intent = new Intent(ReportList.this, SubCategoryReport.class);
			startActivityForResult(intent, Constants.RequestNONE);
		} else if (valuesArray[position].equals(valuesArray[4])) {
			intent = new Intent(ReportList.this, PieChartReport.class);
			startActivityForResult(intent, Constants.RequestNONE);
		} else if (valuesArray[position].equals(valuesArray[5])) {
			intent = new Intent(ReportList.this, BarChartReport.class);
			startActivityForResult(intent, Constants.RequestNONE);
		} else if (valuesArray[position].equals(valuesArray[6])) {
			intent = new Intent(ReportList.this, BudgetGrowthByCategories.class);
			startActivityForResult(intent, Constants.RequestNONE);
		} else if (valuesArray[position].equals(valuesArray[7])) {
			intent = new Intent(ReportList.this, BudgetGrowthsTotal.class);
			startActivityForResult(intent, Constants.RequestNONE);
		} else if (valuesArray[position].equals(valuesArray[8])) {
			intent = new Intent(ReportList.this, BudgetedAmountByCategories.class);
			startActivityForResult(intent, Constants.RequestNONE);
		} else if (valuesArray[position].equals(valuesArray[9])) {
			intent = new Intent(ReportList.this, BudgetedAmountTotal.class);
			startActivityForResult(intent, Constants.RequestNONE);
		} else if (valuesArray[position].equals(valuesArray[10])) {
			intent = new Intent(ReportList.this, BudgetCompare.class);
			startActivityForResult(intent, Constants.RequestNONE);
		} else if (valuesArray[position].equals(valuesArray[11])) {
			intent = new Intent(ReportList.this, BudgetPieChartReport.class);
			startActivityForResult(intent, Constants.RequestNONE);
		} else if (valuesArray[position].equals(valuesArray[12])) {
			intent = new Intent(ReportList.this, MontlyCategoryReport.class);
			startActivityForResult(intent, Constants.RequestNONE);
		}
	}

}
