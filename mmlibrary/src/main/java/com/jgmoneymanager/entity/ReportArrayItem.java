package com.jgmoneymanager.entity;

public class ReportArrayItem {
	
	private String name = null;
	private Double amount = 0d;
	private long id = 0;
	
	public ReportArrayItem(long id, String name, Double amount) {
		this.id = id;
		this.name = name;
		this.amount = amount;
	}
	
	public void addAmount(Double amount) {
		this.amount += amount;
	}
	
	public Double getAmount() {
		return this.amount;
	}
	
	public String getName() {
		return this.name;
	}
	
	public long getItemID() {
		return this.id;
	}
	
	public void setAmount(Double amount){
		this.amount = amount;
	}
}
