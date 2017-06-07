package com.jgmoneymanager.database;

import android.net.Uri;
import android.provider.BaseColumns;

public class MoneyManagerProviderMetaData {
	public static final String AUTHORITY = "com.jgmoneymanager.MoneyManager";
	public static final String DATABASE_NAME = "mmdatabase.db";
	public static final int DATABASE_VERSION = 63;

	private MoneyManagerProviderMetaData() {
	}

	public static final class CurrencyTableMetaData implements BaseColumns {
		private CurrencyTableMetaData() {
		}

		public static final String TABLE_NAME = "currency";
		// uri and MIME type definitions
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/" + TABLE_NAME);
		/*
		 * public static final String CONTENT_TYPE =
		 * "vnd.android.cursor.dir/vnd.androidbook.book"; public static final
		 * String CONTENT_ITEM_TYPE =
		 * "vnd.android.cursor.item/vnd.androidbook.book";
		 */
		// Additional Columns start here.
		public static final String NAME = "name";
		public static final String SIGN = "sign";
		public static final String ISDEFAULT = "isdefault";
		public static final String SORTORDER = "sortorder";
		public static final String RESOURCEID = "resourceID";

		public static final String CREATED_DATE = "created";
		public static final String MODIFIED_DATE = "modified";

		public static final String DEFAULT_SORT_ORDER = ISDEFAULT + " desc, " + SORTORDER + " is null, " + 
				SORTORDER + ", " + NAME;
	}

	public static final class AccountTableMetaData implements BaseColumns {
		private AccountTableMetaData() {
		}

		public static final String TABLE_NAME = "accounts";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);
		
		public static final String NAME = "name";
		public static final String CURRID = "curr_id";
		public static final String DESCRIPTION = "description";
		public static final String INITIALBALANCE = "initial_balance";
		public static final String ISDEFAULT = "isdefault";
		public static final String STATUS = "status";
		public static final String SORTORDER = "sortorder";
		
		public static final String CREATED_DATE = "created";
		public static final String MODIFIED_DATE = "modified";

		public static final String DEFAULT_SORT_ORDER = ISDEFAULT + " DESC, " + SORTORDER;
	}

	public static final class CategoryTableMetaData implements BaseColumns {
		private CategoryTableMetaData() {
		}

		public static final String TABLE_NAME = "category";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);
		
		public static final String NAME = "name";
		public static final String MAINID = "main_id";
		public static final String ISINCOME = "is_income";
		public static final String RESOURCEID = "resourceID";
		
		public static final String CREATED_DATE = "created";
		public static final String MODIFIED_DATE = "modified";

		public static final String DEFAULT_SORT_ORDER = NAME;
	}

	public static final class CurrRatesTableMetaData implements BaseColumns {
		private CurrRatesTableMetaData() {
		}

		public static final String TABLE_NAME = "curr_rates";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);
		
		public static final String FIRSTCURRID = "first_curr_id";
		public static final String SECONDCURRID = "second_curr_id";
		public static final String VALUE= "value";
		public static final String RATEDATE= "rate_date";
		public static final String NEXTRATEDATE= "next_rate_date";
		
		public static final String CREATED_DATE = "created";
		public static final String MODIFIED_DATE = "modified";

		public static final String DEFAULT_SORT_ORDER = FIRSTCURRID;
	}

	public static final class VCurrRatesViewMetaData implements BaseColumns {
		private VCurrRatesViewMetaData() {
		}

		public static final String VIEW_NAME = "vcurrates";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + VIEW_NAME);
		
		public static final String FIRSTCURRID = "first_curr_id";
		public static final String FIRSTCURRSIGN = "first_curr_sign";
		public static final String SECONDCURRID = "second_curr_id";
		public static final String SECONDCURRSIGN = "second_curr_sign";
		public static final String VALUE= "value";
		public static final String RATEDATE= "rate_date";
		public static final String NEXTRATEDATE= "next_rate_date";
		public static final String FIRSTISDEFAULT= "firstisdefault";
		public static final String SECONDISDEFAULT= "secondisdefault";

		public static final String DEFAULT_SORT_ORDER = RATEDATE + " desc " + ", " + FIRSTISDEFAULT + " desc, " + SECONDISDEFAULT + " desc, " +
				FIRSTCURRSIGN + ", " + SECONDCURRSIGN;
	}

	public static final class TransactionsTableMetaData implements BaseColumns {
		private TransactionsTableMetaData() {
		}

		public static final String TABLE_NAME = "transactions";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);
		
		public static final String ACCOUNTID = "account_id";
		public static final String CATEGORYID = "category_id";
		public static final String TRANSDATE= "trans_date";
		public static final String AMOUNT= "amount";
		public static final String TRANSTYPE= "trans_type";
		public static final String BALANCE= "balance";
		public static final String DESCRIPTION= "description";
		public static final String TRANSFERID= "transfer_id";
		public static final String CURRENCYID= "curr_id";
		public static final String PHOTO_PATH= "photo_path";
		public static final String STATUS= "status";
		public static final String PAYMENT_METHOD= "payment_method";

		public static final String CREATED_DATE = "created";
		public static final String MODIFIED_DATE = "modified";

		public static final String DEFAULT_SORT_ORDER = TRANSDATE + " DESC ";
	}

	public static final class TransferTableMetaData implements BaseColumns {
		private TransferTableMetaData() {
		}

		public static final String TABLE_NAME = "transfer";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);
		
		public static final String FIRSTACCOUNTID = "first_account_id";
		public static final String SECONDACCOUNTID = "second_account_id";
		public static final String CATEGORYID = "category_id";
		public static final String TRANSDATE= "trans_date";
		public static final String AMOUNT= "amount";
		public static final String REPEATTYPE= "repeat_type";
		public static final String DESCRIPTION= "description";
		public static final String PERIODEND= "period_end";
		public static final String NEXTPAYMENT= "next_payment";
		public static final String CUSTOMINTERVAL= "custom_interval";
		public static final String CURRENCYID= "curr_id";
		public static final String REMINDER= "reminder";
		public static final String STATUS= "status";
		public static final String TRANSACTION_STATUS= "transaction_status";
		public static final String TRANSACTION_PAYMENT_METHOD= "transaction_payment_method";
		
		public static final String CREATED_DATE = "created";
		public static final String MODIFIED_DATE = "modified";

		public static final String DEFAULT_SORT_ORDER = REPEATTYPE + ", " + TRANSDATE;
	}

	public static final class DebtsTableMetaData implements BaseColumns {
		private DebtsTableMetaData() {
		}

		public static final String TABLE_NAME = "debts";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);
		
		public static final String ISGIVEN = "is_given";
		public static final String TRANSDATE= "trans_date";
		public static final String AMOUNT= "amount";
		public static final String DESCRIPTION= "description";
		public static final String BACKDATE= "back_date";
		public static final String REMINDME= "remind_me";
		public static final String CURRENCY_ID = "currency_id";
		public static final String STATUS= "status";
		
		public static final String CREATED_DATE = "created";
		public static final String MODIFIED_DATE = "modified";

		public static final String DEFAULT_SORT_ORDER = TRANSDATE + ", " + BACKDATE /*+ ", " + ACCOUNTID*/;
	}

	public static final class BudgetTableMetaData implements BaseColumns {
		private BudgetTableMetaData() {
		}

		public static final String TABLE_NAME = "budget";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);
		
		public static final String FROM_DATE= "from_date";
		public static final String TO_DATE= "to_date";
		public static final String INCOME= "income";
		public static final String CURRENCY_ID = "currency_id";
		public static final String STATUS= "status";
		
		public static final String CREATED_DATE = "created";
		public static final String MODIFIED_DATE = "modified";

		public static final String DEFAULT_SORT_ORDER = FROM_DATE;
	}

	public static final class BudgetCategoriesTableMetaData implements BaseColumns {
		private BudgetCategoriesTableMetaData() {
		}

		public static final String TABLE_NAME = "budget_categories";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);
		
		public static final String BUDGET_ID= "budget_id";
		public static final String CATEGORY_ID= "category_id";
		public static final String BUDGET= "budget";
		public static final String USED_AMOUNT= "used_amount";
		public static final String REMAINING= "remaining";
		public static final String REPEAT= "repeat";
		
		public static final String CREATED_DATE = "created";
		public static final String MODIFIED_DATE = "modified";

		public static final String DEFAULT_SORT_ORDER = BUDGET_ID;
	}

	public static final class TransactionStatusTableMetaData implements BaseColumns {
		private TransactionStatusTableMetaData() {
		}

		public static final String TABLE_NAME = "transaction_status";
		// uri and MIME type definitions
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/" + TABLE_NAME);

		public static final String NAME = "name";
		public static final String SORTORDER = "sortorder";
		public static final String RESOURCEID = "resourceID";

		public static final String CREATED_DATE = "created";
		public static final String MODIFIED_DATE = "modified";

		public static final String DEFAULT_SORT_ORDER = SORTORDER + " is null, " +
				SORTORDER + ", " + NAME;
	}

	public static final class PaymentMethodsTableMetaData implements BaseColumns {
		private PaymentMethodsTableMetaData() {
		}

		public static final String TABLE_NAME = "payment_methods";
		// uri and MIME type definitions
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/" + TABLE_NAME);

		public static final String NAME = "name";
		public static final String SORTORDER = "sortorder";
		public static final String RESOURCEID = "resourceID";

		public static final String CREATED_DATE = "created";
		public static final String MODIFIED_DATE = "modified";

		public static final String DEFAULT_SORT_ORDER = SORTORDER + " is null, " +
				SORTORDER + ", " + NAME;
	}

	public static final class BudgetGoalsTableMetaData implements BaseColumns {
		private BudgetGoalsTableMetaData() {
		}

		public static final String TABLE_NAME = "budget_goals";
		// uri and MIME type definitions
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);

		public static final String CATEGORY_ID   = "category_id";
		public static final String START_MONTH   = "start_month";
		public static final String TARGET_MONTH  = "target_month";
		public static final String TARGET_AMOUNT = "target_amount";
		public static final String DESCRIPTION = "description";

		public static final String CREATED_DATE = "created";
		public static final String MODIFIED_DATE = "modified";

		public static final String DEFAULT_SORT_ORDER = TARGET_MONTH;
	}

	public static final class VTransactionViewMetaData implements BaseColumns {
		private VTransactionViewMetaData() {
		}

		public static final String VIEW_NAME = "vtransaction";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + VIEW_NAME);
		
		public static final String ACCOUNTID = "account_id";
		public static final String ACCOUNTNAME = "account_name";
		public static final String CATEGORYID = "category_id";
		public static final String CATEGORYNAME = "category_name";
		public static final String TRANSDATE = "trans_date";
		public static final String AMOUNT = "amount";
		public static final String LBAMOUNT= "lbamount";
		public static final String TRANSTYPE= "trans_type";
		public static final String BALANCE = "balance";
		public static final String LBALANCE = "lbalance";
		public static final String DESCRIPTION = "description";
		public static final String TRANSFERID = "transfer_id";
		public static final String ISTRANSFER = "is_transfer";
		public static final String ACCOUNTSTATUS = "account_status";
		public static final String CURRID = "curr_id";
		public static final String ACCOUNTCURRID = "account_curr_id";
		public static final String PHOTO_PATH = "photo_path";
		public static final String STATUS = "status";
		public static final String PAYMENT_METHOD= "payment_method";
		public static final String CURRENCYSIGN = "currency_sign";

		public static final String DEFAULT_SORT_ORDER = TRANSDATE + " DESC, " + _ID + " DESC ";
	}

	public static final class VAccountsViewMetaData implements BaseColumns {
		private VAccountsViewMetaData() {
		}

		public static final String VIEW_NAME = "vaccounts";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + VIEW_NAME);
		
		public static final String ACCOUNTNAME = "account_name";
		public static final String CURRID = "curr_id";
		public static final String CURRNAME = "currency_name";
		public static final String DESCRIPTION = "description";
		public static final String INITIALBALANCE = "initial_balance";
		public static final String ISDEFAULT = "isdefault";
		public static final String BALANCE = "balance";
		public static final String CURRSIGN = "currency_sign";
		public static final String STATUS = "status";
		public static final String SORTORDER = "sortorder";

		public static final String DEFAULT_SORT_ORDER = ISDEFAULT + " DESC, " + SORTORDER + ", " + ACCOUNTNAME;
	}

	public static final class VTransAccountViewMetaData implements BaseColumns {
		private VTransAccountViewMetaData() {
		}

		public static final String VIEW_NAME = "vtrans_accounts";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + VIEW_NAME);
		
		public static final String NAME = "account_name";
		public static final String ISDEFAULT = "isdefault";
		public static final String STATUS = "status";
		public static final String SORTORDER = "sortorder";
		
		public static final String DEFAULT_SORT_ORDER = ISDEFAULT + " DESC, " + NAME;
	}

	public static final class VTransferViewMetaData implements BaseColumns {
		private VTransferViewMetaData() {
		}

		public static final String VIEW_NAME = "vtransfer";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + VIEW_NAME);
		
		public static final String FROMACCOUNTID = "from_account_id";
		public static final String FROMACCOUNTNAME = "from_account_name";
		public static final String TOACCOUNTID = "to_account_id";
		public static final String TOACCOUNTNAME = "to_account_name";
		public static final String ACCOUNTLABEL = "account_label";
		public static final String CATEGORYID = "category_id";
		public static final String CATEGORYNAME = "category_name";
		public static final String AMOUNT = "amount";
		public static final String LBAMOUNT = "lbamount";
		public static final String TRANSDATE = "trans_date";
		public static final String REPEATTYPE = "repeat_type";
		public static final String DESCRIPTION = "description";
		public static final String PERIODEND= "period_end";
		public static final String NEXTPAYMENT= "next_payment";
		public static final String CUSTOMINTERVAL= "custom_interval";
		public static final String CURRENCYID= "curr_id";
		public static final String FROMACCCURRID = "from_account_curr_id";
		public static final String TOACCCURRID = "to_account_curr_id";
		public static final String ISENABLED = "is_enabled";
		public static final String REMINDER = "reminder";
		public static final String TRANSACTION_STATUS= "transaction_status";
		public static final String TRANSACTION_PAYMENT_METHOD= "transaction_payment_method";
		public static final String CURRENCY_SIGN = "currency_sign";
		
		public static final String DEFAULT_SORT_ORDER = ISENABLED + " desc, " + REPEATTYPE + ", " + TRANSDATE;
	}

	public static final class VCategoriesViewMetaData implements BaseColumns {
		private VCategoriesViewMetaData() {
		}

		public static final String VIEW_NAME = "vcategories";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + VIEW_NAME);
		
		public static final String NAME = "name";
		public static final String MAINID = "main_id";
		
		public static final String DEFAULT_SORT_ORDER = NAME;
	}

	public static final class VDebtsViewMetaData implements BaseColumns {
		private VDebtsViewMetaData() {
		}

		public static final String VIEW_NAME = "vdebts";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + VIEW_NAME);
		
		public static final String ISGIVEN = "is_given";
		public static final String TRANSDATE= "trans_date";
		public static final String AMOUNT= "amount";
		public static final String DESCRIPTION= "description";
		public static final String BACKDATE= "back_date";
		public static final String REMINDME= "remind_me";
		public static final String CURRENCY_ID = "currency_id";
		public static final String STATUS= "status";
		public static final String CURRENCY_NAME= "currency_name";
		public static final String CURRENCY_SIGN= "currency_sign";
		
		public static final String DEFAULT_SORT_ORDER = "";
	}

	public static final class VRatesToDefaultViewMetaData implements BaseColumns {
		private VRatesToDefaultViewMetaData() {
		}

		public static final String VIEW_NAME = "vratestodefault";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + VIEW_NAME);
		
		public static final String CURRENCY_ID = "curr_id";
		public static final String RATE_DATE = "rate_date";
		public static final String NEXT_RATE_DATE = "next_rate_date";
		public static final String VALUE = "value";
		
		public static final String DEFAULT_SORT_ORDER = "";
	}

}
