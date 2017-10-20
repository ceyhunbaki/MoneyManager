package com.jgmoneymanager.paid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;
import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CategoryTableMetaData;
import com.jgmoneymanager.entity.CheckBoxItem;
import com.jgmoneymanager.entity.Group;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.paid.R;
import com.jgmoneymanager.tools.Constants;

import java.util.ArrayList;
import java.util.List;

public class CategoryFilter extends MyActivity {

	public static ArrayList<Group> group;
	public static final String paramList = "list";
	static boolean disableMultiSelect;
	Bundle givenParams;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.categoryfilter);

		try {	
			if (getIntent().hasExtra(Constants.disableMultiSelect)) {	
				disableMultiSelect = getIntent().getBooleanExtra(Constants.disableMultiSelect, false);
				givenParams = getIntent().getExtras();
			}
			else 
				disableMultiSelect = false;
			if (disableMultiSelect) 
				findViewById(R.id.btCatFilterSelAll).setEnabled(false);
		}
		catch (Exception e) {
			
		}

		ExpandableListView expandableListView = (ExpandableListView) findViewById(R.id.catExListView);
		expandableListView.setBackgroundColor(getResources().getColor(R.color.White));
		expandableListView.setScrollingCacheEnabled(true);
		expandableListView.setCacheColorHint(0);

		if (!getIntent().hasExtra(Constants.dontRefreshValues))
			group = generateData(getBaseContext());		
		if (group == null)
			group = generateData(getBaseContext());
		if (getIntent().hasExtra(CategoryFilter.paramList)){
			ArrayList<Integer> givenList = getIntent().getIntegerArrayListExtra(CategoryFilter.paramList);
			for (int i = 0; i < group.size(); i++) {
				Group groupItem = group.get(i);
				//eger qrupun altinda subqruplar varsa, ve hamisi secilibse bunun komeyile qrupu da secilmiw edek
				boolean allChildrenSelected = groupItem.getChildren().size() >0;
				for (int j = 0; j < groupItem.getChildren().size(); j++) {
					CheckBoxItem item = groupItem.getChildren().get(j);
					if (givenList.contains(item.getID()))
						item.setSelected(true);
					else 
						allChildrenSelected = false;
				}
				groupItem.setOnlyGroupChecked(allChildrenSelected);
			}	
		}
			
		ExpandableListAdapter mAdapter = new MyExpandableListAdapter(this, group);
		expandableListView.setAdapter(mAdapter);
	}

	// extending SimpleCursorTreeAdapter
	public class MyExpandableListAdapter extends BaseExpandableListAdapter {

		private final List<Group> groups;
		public LayoutInflater inflater;

		public MyExpandableListAdapter(Activity act, List<Group> groups) {
			this.groups = groups;
			inflater = act.getLayoutInflater();
		}
		
		class ViewHolder {
		    protected TextView text;
		    protected CheckBox checkbox;
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return groups.get(groupPosition).getChildren().get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return 0;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return groups.get(groupPosition).getChildren().size();
		}

		@Override
		public Object getGroup(int groupPosition) {
			return groups.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return groups.size();
		}

		@Override
		public void onGroupCollapsed(int groupPosition) {
			super.onGroupCollapsed(groupPosition);
		}

		@Override
		public void onGroupExpanded(int groupPosition) {
			super.onGroupExpanded(groupPosition);
		}

		@Override
		public long getGroupId(int groupPosition) {
			return 0;
		}

		@Override
		public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {			
			View view;
		    if (convertView == null) {
		    	view = inflater.inflate(R.layout.list1rowcategorycbox, null);
		    	final ViewHolder viewHolder = new ViewHolder();
		    	viewHolder.text = (TextView) view.findViewById(R.id.grp_child);
		    	viewHolder.checkbox = (CheckBox) view.findViewById(R.id.grp_child_check);
		    	viewHolder.checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
		            @Override
		            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		            	CheckBoxItem element = (CheckBoxItem) viewHolder.checkbox.getTag();
		            	element.setSelected(buttonView.isChecked());
		            	if (!disableMultiSelect) {
			            	element.setSelected(buttonView.isChecked());
		            		if (!isChecked) 
		            			((Group)getGroup(groupPosition)).setOnlyGroupChecked(false);
		            	}
		            	else {
		            		if (buttonView.isChecked()) {
		            			CategoryFilter.setGroupState(false);
				            	notifyDataSetChanged();
		            		}
			            	element.setSelected(buttonView.isChecked());
		            	}
		            }
		         });
		    	view.setTag(viewHolder);
		    	viewHolder.checkbox.setTag(getChild(groupPosition, childPosition));
		    } else {
		      view = convertView;
		      ((ViewHolder) view.getTag()).checkbox.setTag(getChild(groupPosition, childPosition));
		    }
		    ViewHolder holder = (ViewHolder) view.getTag();
		    holder.text.setText(((CheckBoxItem)getChild(groupPosition, childPosition)).getName());
		    holder.checkbox.setChecked(((CheckBoxItem)getChild(groupPosition, childPosition)).isSelected());
		    return view;
		}

		@Override
		public View getGroupView(final int groupPosition, final boolean isExpanded,
				View convertView, ViewGroup parent) {
			View view;
		    if (convertView == null) {
		    	view = inflater.inflate(R.layout.group_row_cbox, null);
		    	final ViewHolder viewHolder = new ViewHolder();
		    	viewHolder.text = (TextView) view.findViewById(R.id.grp_main_name);
		    	viewHolder.checkbox = (CheckBox) view.findViewById(R.id.grp_main_check);
		    	viewHolder.checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
		            @Override
		            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		            	Group element = (Group) viewHolder.checkbox.getTag();
		            	if (!disableMultiSelect) {
		            		element.setChecked(buttonView.isChecked());
		            		notifyDataSetChanged();
		            	}
		            	else {
		            		if (buttonView.isChecked()) {
		            			CategoryFilter.setGroupState(false);
				            	notifyDataSetChanged();
		            		}
			            	element.setOnlyGroupChecked(buttonView.isChecked());
		            	}
		            }
		         });
		    	view.setTag(viewHolder);
		    	viewHolder.checkbox.setTag(getGroup(groupPosition));
		    } else {
		      view = convertView;
		      ((ViewHolder) view.getTag()).checkbox.setTag(getGroup(groupPosition));
		    }
	    	view.setOnClickListener(new OnClickListener() {				
				@Override
				public void onClick(View v) {
					if (isExpanded)
						((ExpandableListView) findViewById(R.id.catExListView)).collapseGroup(groupPosition);
					else
						((ExpandableListView) findViewById(R.id.catExListView)).expandGroup(groupPosition);
				}
			});
	    	
		    ViewHolder holder = (ViewHolder) view.getTag();
		    holder.text.setText(((Group)getGroup(groupPosition)).getName());
		    holder.checkbox.setChecked(((Group)getGroup(groupPosition)).isChecked());
		    return view;

		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}			
	}
	
	/**
	 * Generates ArrayList<Group> of Categories
	 */
	public static ArrayList<Group> generateData(Context context) {
		ArrayList<Group> result = new ArrayList<Group>();
		try {
			Cursor mainCursor = context.getContentResolver().query(CategoryTableMetaData.CONTENT_URI, 
					new String[] {CategoryTableMetaData._ID, CategoryTableMetaData.NAME}, 
					CategoryTableMetaData.MAINID + " is null ", null, CategoryTableMetaData.NAME);
			for(mainCursor.moveToFirst(); !mainCursor.isAfterLast(); mainCursor.moveToNext()) {
				Group group = new Group(DBTools.getCursorColumnValueInt(mainCursor, CategoryTableMetaData._ID), 
						DBTools.getCursorColumnValue(mainCursor, CategoryTableMetaData.NAME), false);
				Cursor childCursor = context.getContentResolver().query(CategoryTableMetaData.CONTENT_URI, 
						new String[] {CategoryTableMetaData._ID, CategoryTableMetaData.NAME, CategoryTableMetaData.MAINID}, 
						CategoryTableMetaData.MAINID + " = ? ", 
						new String[] {DBTools.getCursorColumnValue(mainCursor, CategoryTableMetaData._ID)}, 
						CategoryTableMetaData.NAME);
				List<CheckBoxItem> children = new ArrayList<CheckBoxItem>();
				for(childCursor.moveToFirst(); !childCursor.isAfterLast(); childCursor.moveToNext()) {
					CheckBoxItem item = new CheckBoxItem(DBTools.getCursorColumnValueInt(childCursor, CategoryTableMetaData._ID), 
							DBTools.getCursorColumnValue(childCursor, CategoryTableMetaData.NAME));
					children.add(item);
				}
				if (children.size() > 0)
					group.setChildren(children);	
				result.add(group);
			}
			DBTools.closeDatabase();		
		}
		catch (Exception e) {
			Tracker myTracker = EasyTracker.getInstance(context);     // Get a reference to tracker.
			myTracker.set(Fields.SCREEN_NAME, "Category Filter- Gen data");
			myTracker.send(MapBuilder.createAppView().build());			
		}
		return result;
	}
	
	public void myClickHandler(View target) {
		switch (target.getId()) {
		case R.id.btCatFilterOk:
			Intent intent = new Intent();
			try {
				intent.putExtras(givenParams);
			}
			catch (Exception e) {
				
			}
			setResult(RESULT_OK, intent);
			finish();
			break;
		case R.id.btCatFilterCancel:
			setResult(RESULT_CANCELED);
			finish();
			break;
		case R.id.btCatFilterSelAll:
			setGroupState(true);
			setResult(RESULT_OK);
			finish();
			break;
		default:
			break;
		}
	}
	
	public static void setGroupState(boolean state) {
		Group g;
		for (int i = 0; i < group.size(); i++) {
			g = group.get(i);
			g.setChecked(state);
		}
	}
}
