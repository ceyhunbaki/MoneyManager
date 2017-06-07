package com.jgmoneymanager.entity;

import com.jgmoneymanager.tools.Tools;

public class ReportArray {
	
	ReportArrayItem [] array;
	
	public ReportArray(int itemCount) {
		array = new ReportArrayItem[itemCount];
//		for (int i = 0; i <= itemCount; i++)
//			array[i] = new ReportArrayItem(i, name, amount)
	}
	
	public void addItem(int position, ReportArrayItem item) {
		if (array[position] == null)
			array[position] = item;
		else {
			ReportArrayItem currentItem = array[position];
			currentItem.addAmount(item.getAmount());
		}
	}
	
	public int getItemCount() {
		return array.length;
	}
	
	public ReportArrayItem getItem(int index) {
		return array[index];
	}
	
	public void deleteEmtpyItems() {
		int dataCount = 0;
		for (int i = 1; i < array.length; i++) {
			if (array[i] != null)
				dataCount ++;
		}
		ReportArrayItem[] newArray = new ReportArrayItem[dataCount+1];
		int currentID = 1;
		for (int i = 1; i < array.length; i++) {
			if (array[i] != null){
				newArray[currentID] = array[i];
				currentID++;
			}
		}
		array = newArray;
	}
	
	public void roundValues() {
		for (int i = 1; i < array.length; i++) {
			if (array[i] != null)
				array[i].setAmount(Tools.round(array[i].getAmount()));
		}
	}
}
