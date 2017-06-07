package com.jgmoneymanager.dialogs;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;
import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.CategoryTableMetaData;
import com.jgmoneymanager.entity.CheckBoxItem;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.main.R;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import java.util.ArrayList;
import java.util.HashMap;

public class CheckBoxDialog extends MyActivity {

    public static ArrayList<CheckBoxItem> itemsList;
    //public static final String paramList = "list";
    boolean dontRefreshValues = false;
    String query = null;
    Cursor mainCursor;
    String itemNameColumn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.check_box_filter);

        ListView listView = (ListView) findViewById(R.id.catListView);
        listView.setBackgroundColor(getResources().getColor(R.color.White));
        listView.setScrollingCacheEnabled(true);
        listView.setCacheColorHint(0);

        Bundle bundle = getIntent().getExtras();
        if (bundle.containsKey(Constants.dontRefreshValues))
            dontRefreshValues = bundle.getBoolean(Constants.dontRefreshValues, false);
        this.query = bundle.getString(Constants.query);
        itemNameColumn = bundle.getString(Constants.paramTitle);
        itemsList = generateData(getBaseContext(), query, dontRefreshValues);
        if (bundle.containsKey(Constants.paramValues)) {
            Tools.getValuesFromHashMap((HashMap<Integer, Integer>) Tools.getSerializableFromBundle(bundle, Constants.paramValues), itemsList);
        }
        String[] from = new String[]{itemNameColumn};
        int[] to = new int[]{R.id.grp_child};
        listView.setAdapter(new MyListAdapter(getBaseContext(), R.layout.list1rowcategorycbox, mainCursor, from, to));
    }

    /**
     * Generates ArrayList<CheckBoxItem> from given query
     */
    private ArrayList<CheckBoxItem> generateData(Context context, String query, boolean dontRefreshValues) {
        ArrayList<CheckBoxItem> result = new ArrayList<CheckBoxItem>();
        try {
            mainCursor = DBTools.createCursor(context, query);
            if (!dontRefreshValues || itemsList == null)
                for (mainCursor.moveToFirst(); !mainCursor.isAfterLast(); mainCursor.moveToNext()) {
                    CheckBoxItem item = new CheckBoxItem(DBTools.getCursorColumnValueInt(mainCursor, CategoryTableMetaData._ID),
                            DBTools.getCursorColumnValue(mainCursor, CategoryTableMetaData.NAME));
                    result.add(item);
                }
            //DBTools.closeDatabase();
        } catch (Exception e) {
            Tracker myTracker = EasyTracker.getInstance(context);     // Get a reference to tracker.
            myTracker.set(Fields.SCREEN_NAME, "Category Filter- Gen data");
            myTracker.send(MapBuilder.createAppView().build());
        }
        if (dontRefreshValues)
            return itemsList;
        else
            return result;
    }

    public void myClickHandler(View target) {
        switch (target.getId()) {
            case R.id.btCatFilterOk:
                setResult(RESULT_OK);
                finish();
                break;
            case R.id.btCatFilterCancel:
                setResult(RESULT_CANCELED);
                finish();
                break;
            case R.id.btCatFilterSelAll:
                //setGroupState(true);
                setResult(RESULT_OK);
                finish();
                break;
            default:
                break;
        }
    }

    class MyListAdapter extends SimpleCursorAdapter {

        public MyListAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
            super(context, layout, c, from, to);
        }

        class ViewHolder {
            protected TextView text;
            protected CheckBox checkbox;
        }

        @Override
        public Object getItem(int position) {
            return itemsList.get(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = super.getView(position, convertView, parent);
                //view = inflater.inflate(R.layout.list1rowcategorycbox, null);
                final ViewHolder viewHolder = new ViewHolder();
                viewHolder.text = (TextView) view.findViewById(R.id.grp_child);
                viewHolder.checkbox = (CheckBox) view.findViewById(R.id.grp_child_check);
                viewHolder.checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        CheckBoxItem element = (CheckBoxItem) viewHolder.checkbox.getTag();
                        element.setSelected(buttonView.isChecked());
                        notifyDataSetChanged();
                    }
                });
                view.setTag(viewHolder);
                viewHolder.checkbox.setTag(getItem(position));
            } else {
                view = convertView;
                ((ViewHolder) view.getTag()).checkbox.setTag(getItem(position));
            }

            ViewHolder holder = (ViewHolder) view.getTag();
            holder.text.setText(((CheckBoxItem) getItem(position)).getName());
            holder.checkbox.setChecked(((CheckBoxItem) getItem(position)).isSelected());
            return view;

        }

        @Override
        public boolean hasStableIds() {
            return false;
        }
    }
}
