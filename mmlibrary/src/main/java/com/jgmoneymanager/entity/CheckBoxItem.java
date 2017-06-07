package com.jgmoneymanager.entity;

public class CheckBoxItem {
	private final int id;
	private final String name;
	private boolean selected;
	
	public CheckBoxItem(int id, String name) {
		this.id = id;
		this.name = name;
        this.selected = false;
	}
	
	public int getID() {
		return this.id;
	}
	
	public String getName() {
		return this.name;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}
}
