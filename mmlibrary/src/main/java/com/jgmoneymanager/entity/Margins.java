package com.jgmoneymanager.entity;

public class Margins {

	private int leftMargin;
	private int rightMargin;
	private int bottomMargin;
	private int topMargin;

	public Margins(int leftMargin, int rightMargin, int bottomMargin,
			int topMargin) {
		super();
		this.leftMargin = leftMargin;
		this.rightMargin = rightMargin;
		this.bottomMargin = bottomMargin;
		this.topMargin = topMargin;
	}

	public int getLeftMargin() {
		return leftMargin;
	}

	public void setLeftMargin(int leftMargin) {
		this.leftMargin = leftMargin;
	}

	public int getRightMargin() {
		return rightMargin;
	}

	public void setRightMargin(int rightMargin) {
		this.rightMargin = rightMargin;
	}

	public int getBottomMargin() {
		return bottomMargin;
	}

	public void setBottomMargin(int bottomMargin) {
		this.bottomMargin = bottomMargin;
	}

	public int getTopMargin() {
		return topMargin;
	}

	public void setTopMargin(int topMargin) {
		this.topMargin = topMargin;
	}
}
