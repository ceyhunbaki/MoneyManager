package com.jgmoneymanager.entity;

import android.database.Cursor;
import android.database.SQLException;
import android.util.Log;

import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.TransactionsTableMetaData;

import java.util.Date;

public class Transaction {
    private long id;
    private long account_id;
    private long category_id;
    private Date trans_date;
    private Double amount;
    private int trans_type;
    private Double balance;
    private String description;
    private long transfer_id;
    private long currency_id;
	private String photoPath;
	private long status;
	private long paymentMethod;
	
	public Transaction(Cursor cursor) {
		try {
			id = DBTools.getCursorColumnValueLong(cursor, TransactionsTableMetaData._ID);
			account_id = DBTools.getCursorColumnValueLong(cursor, TransactionsTableMetaData.ACCOUNTID);
			category_id = DBTools.getCursorColumnValueLong(cursor, TransactionsTableMetaData.CATEGORYID);
			trans_date = DBTools.getCursorColumnValueDate(cursor, TransactionsTableMetaData.TRANSDATE);
			amount = DBTools.getCursorColumnValueDouble(cursor, TransactionsTableMetaData.AMOUNT);
			trans_type = DBTools.getCursorColumnValueInt(cursor, TransactionsTableMetaData.TRANSTYPE);
			balance = DBTools.getCursorColumnValueDouble(cursor, TransactionsTableMetaData.BALANCE);
			description = DBTools.getCursorColumnValue(cursor, TransactionsTableMetaData.DESCRIPTION);
			transfer_id = DBTools.getCursorColumnValueLong(cursor, TransactionsTableMetaData.TRANSFERID);
			currency_id = DBTools.getCursorColumnValueLong(cursor, TransactionsTableMetaData.CURRENCYID);
			photoPath = DBTools.getCursorColumnValue(cursor, TransactionsTableMetaData.PHOTO_PATH);
			status = DBTools.getCursorColumnValueLong(cursor, TransactionsTableMetaData.STATUS);
			paymentMethod = DBTools.getCursorColumnValueLong(cursor, TransactionsTableMetaData.PAYMENT_METHOD);
		}
		catch (SQLException er) {
			Log.e("MM.TransEntity - cursor", er.getMessage());
		}
		catch (Exception ex) {
			Log.e("MM.TransEntity - cursor", ex.getMessage());
		}
	}
	
	public Transaction(long id, long account_id, long category_id,
			Date trans_date, Double amount, int trans_type, Double balance,
			String description, long transfer_id, long currency_id, String photoPath, long status, long paymentMethod) {
		this.id = id;
		this.account_id = account_id;
		this.category_id = category_id;
		this.trans_date = trans_date;
		this.amount = amount;
		this.trans_type = trans_type;
		this.balance = balance;
		this.description = description;
		this.transfer_id = transfer_id;
		this.currency_id = currency_id;
		this.photoPath = photoPath;
		this.status = status;
		this.paymentMethod = paymentMethod;
	}

	public long getCategory_id() {
		return category_id;
	}

	public Date getTrans_date() {
		return trans_date;
	}

	public int getTrans_type() {
		return trans_type;
	}

	public long getTransfer_id() {
		return transfer_id;
	}

	public long getCurrency_id() {
		return currency_id;
	}
	public Double getAmount() {
		return this.amount;
	}
}
