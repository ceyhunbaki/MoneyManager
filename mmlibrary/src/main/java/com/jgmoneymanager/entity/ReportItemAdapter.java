package com.jgmoneymanager.entity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.jgmoneymanager.mmlibrary.R;
import com.jgmoneymanager.tools.Tools;

import java.util.ArrayList;

public class ReportItemAdapter extends ArrayAdapter<ReportArrayItem> {

	// declaring our ArrayList of items
	private ArrayList<ReportArrayItem> objects;

	/* here we must override the constructor for ArrayAdapter
	* the only variable we care about now is ArrayList<Item> objects,
	* because it is the list of objects we want to display.
	*/
	public ReportItemAdapter(Context context, int layoutID, ArrayList<ReportArrayItem> objects) {
		super(context, layoutID, objects);
		this.objects = objects;
	}

	/*
	 * we are overriding the getView method here - this is what defines how each
	 * list item will look.
	 */
	public View getView(int position, View convertView, ViewGroup parent){

		// assign the view we are converting to a local variable
		View v = convertView;

		// first check to see if the view is null. if so, we have to inflate it.
		// to inflate it basically means to render, or show, the view.
		if (v == null) {
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = inflater.inflate(R.layout.list2columnrow, null);
		}

		/*
		 * Recall that the variable position is sent in as an argument to this method.
		 * The variable simply refers to the position of the current object in the list. (The ArrayAdapter
		 * iterates through the list we sent it)
		 * 
		 * Therefore, i refers to the current Item object.
		 */
		ReportArrayItem item = objects.get(position);

		if (item != null) {

			// This is how you obtain a reference to the TextViews.
			// These TextViews are created in the XML files we defined.

			TextView column1 = (TextView) v.findViewById(R.id.l2column1);
			TextView column2 = (TextView) v.findViewById(R.id.l2column2);
			
			// check to see if each individual textview is null.
			// if not, assign some text!
			if (column1 != null){
				column1.setText(item.getName());
			}
			if (column2 != null){
				column2.setText(Tools.formatDecimalInUserFormat(item.getAmount()));
			}
		}

		// the view must be returned to our activity
		return v;

	}

}