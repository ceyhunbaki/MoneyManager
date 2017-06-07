package com.jgmoneymanager.reports;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.jgmoneymanager.entity.MyListActivity;
import com.jgmoneymanager.main.R;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

public class ReportList extends MyListActivity {

	String[] valuesArray;

	private AdView adView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.report_list);
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
		/*setListAdapter(new ArrayAdapter<String>(this, R.layout.list1columnrow, R.id.grp_child, valuesArray));*/
		setListAdapter(new MyListAdapter(this, valuesArray, imagesArray));
		getListView().setBackgroundColor(getResources().getColor(R.color.White));
		/*getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
		((TextView)findViewById(R.id.cusTitleText)).setText(R.string.menuReports);
		((Button) findViewById(R.id.cusTitleMenu)).setVisibility(View.GONE);*/
		
		// Create the adView
		if (!Tools.proVersionExists(this)) {
			adView = new AdView(this, AdSize.BANNER, "ca-app-pub-5995868530154544/3468276114"); 
			LinearLayout layout = (LinearLayout)findViewById(R.id.onlyListAdsLayout); 
			layout.addView(adView);  
			AdRequest adRequest = new AdRequest();
			adView.loadAd(adRequest);
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
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
				adView.removeAllViews();
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
			result = context.getResources().getString(R.string.income);
		else if (reportType == Constants.TransFTransaction.Expence.index())
			result = context.getResources().getString(R.string.expense);
		return result;
	}

}
