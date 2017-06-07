package com.jgmoneymanager.entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.jgmoneymanager.entity.CheckBoxItem;

import java.util.ArrayList;
import java.util.List;

public class Group implements Parcelable {
	private String name;
	public Group(int iD, String name, boolean isChecked) {
		super();
		this.name = name;
		ID = iD;
		this.isChecked = isChecked;
	}

	private final int ID;
	private boolean isChecked;
	
	private List<CheckBoxItem> children = new ArrayList<>();

	public List<CheckBoxItem> getChildren() {
		return children;
	}

	public void setChildren(List<CheckBoxItem> children) {
		this.children = children;
	}
	
	/*public void addChildren(CheckBoxItem child) {
		children.add(child);
	}*/

	public String getName() {
		return name;
	}

	/*public void setName(String name) {
		this.name = name;
	}*/

	public int getID() {
		return ID;
	}

	/*public void setID(int iD) {
		ID = iD;
	}*/

	public boolean isChecked() {
		return isChecked;
	}

	public void setChecked(boolean isChecked) {
		this.isChecked = isChecked;
		
		for (int i = 0; i < children.size(); i++) {
			children.get(i).setSelected(isChecked);
		}
	}

	public void setOnlyGroupChecked(boolean isChecked) {
		this.isChecked = isChecked;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
	}
}
