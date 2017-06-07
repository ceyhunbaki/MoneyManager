package com.jgmoneymanager.tools;

import java.util.Date;

import com.dropbox.client2.session.Session.AccessType;

public class Constants {

	public static final String emailSubject = "Money Manager";
	public static final String supportEmail = "sp.moneymanager@gmail.com";
	
	public static String UpdateMode = "UpdateMode";
	public static String Update = "Update";
	public static String Insert = "Insert";
	
	//public static String RPTranscDescription = "Repeating:";
	//public static String TransferDescription = "Transfer:";
	public static final String newLine = "\n";
	public static String tableIsEmpty = "tableIsEmpty";
	public static Date autoBackupDate = Tools.AddDays(Tools.getCurrentDate(), -1);
	
	public static String DateFormatUser = "dd.MM.yyyy";
    public static final String DateFormatDB = "yyyyMMdd";
	public static final String DateFormatShort = "yyyyMM";
	public static final String DateFormatReport = "MM.yyyy";
	public static final String DateFormatYear = "yyyy";
	public static final String DateFormatDBLong = "yyyyMMddHHmmssSS";
	public static final String DateFormatForDescriptions = "yyMMddHHmmss";
	public static final String DateFormatExpMan = "dd.MM.yyyy";
	public static final String DateFormatBackupAuto = "yyyy-MM-dd";
	public static final String DateFormatBackup = "yyyy-MM-dd HHmmss";
	//public static final String DateFormatDropboxRevision = "EEE, dd MMM yyyy HH:mm:ss ZZZZ";
	public static String WeekFirstDay = "monday";
	public static int MonthFirstDate = 1;

	//public static final int RequestAccountForTransaction  = 1;
	public static final int RequestCategoryForTransaction = 2;	
	public static final int RequestCurrencyForTransaction = 13;	
	public static final int RequestAccountForTransferFrom  = 3;
	public static final int RequestAccountForTransferTo = 4;
	public static final int RequestNewTransferForTransfer = 5;
	public static final int RequestCurrencyForTransfer = 14;	
	public static final int RequestAccountInsert = 6;
	public static final int RequestAccountUpdate = 7;
	public static final int RequestTransactionInsert = 8;
	public static final int RequestTransactionUpdate = 9;
	//public static final int RequestTransactionUpdateTransfer = 10;
	public static final int RequestTransactionByAccount = 11;
	public static final int RequestEditTransferForTransfer = 12;
	public static final int RequestCurrencyForAccount = 15;
	public static final int RequestRPTransactionsForAccount = 16;
	public static final int RequestNewRPTransactionForTransfer = 17;
	public static final int RequestEditRPTransactionForTransfer = 18;
	public static final int RequestFilterForTransaction = 19;
	public static final int RequestDialogForImport = 20;
	public static final int RequestSettingsScreen = 21;
	public static final int RequestDialogForRestore = 22;
	//public static final int RequestRatesForCurrency = 23;
	//public static final int RequestRateInsert = 24;
	public static final int RequestCurrencyForFromRate  = 25;
	public static final int RequestCurrencyForToRate  = 26;
	public static final int RequestDialogForBackupFolder = 27;
	public static final int RequestTransferInsert = 28;
	public static final int RequestCalculator = 29;
	public static final int RequestCurrencyForConvertorFrom = 30;
	public static final int RequestCurrencyForConvertorTo = 31;
	public static final int RequestDialogForExport = 32;
	public static final int RequestAccountSort = 33;
	public static final int RequestPassword = 34;
	public static final int RequestPasswordPlusProtection = 35;
	public static final int RequestPasswordForDisabling = 36;
	public static final int RequestPasswordInStartup = 37;
	//public static final int RequestPasswordForEmail = 38;
	public static final int RequestPasswordForQuestion = 39;
	public static final int RequestQuestionDialog = 40;
	public static final int RequestFilterForExport = 41;
	public static final int RequestCategoryForRPTransaction = 42;	
	public static final int RequestDebtsForInsert = 43;		
	public static final int RequestDebtsForEdit = 44;		
	//public static final int RequestBudgetForEdit = 45;
	public static final int RequestCategoryForBudget = 46;
	public static final int RequestTransactionFirstFilter = 47;
	public static final int RequestNONE = 48;
	public static final int RequestCamera = 49;
	public static final int RequestGallery = 50;
	public static final int RequestCalculatorForRate = 51;
	public static final int RequestAccountForTransaction  = 52;
	public static final int RequestStatusForTransaction  = 53;
	public static final int RequestCategoryForReport = 54;
	public static final int RequestMethodForTransaction  = 55;
	public static final int RequestCategoryForBudgetGoal = 56;
	public static final int RequestSplitTransaction = 57;
	public static final int RequestCategoryForSplitTransaction = 58;
	public static final int RequestCalculatorForSplitTransaction = 59;
	public static final int RequestSettingsLocalization = 60;
	public static final int RequestCalculatorForTransFilterAmountFrom = 61;
	public static final int RequestCalculatorForTransFilterAmountTo = 62;
	public static final int RequestDialogForReceiptFolder = 63;

	public static final String ActionViewTransactionsByAccount = "ActionViewTransactionsByAccount";
	public static final String ActionViewTransactionsFromReport = "ActionViewTransactionsFromReport";
	public static final String ActionViewRPTransactionsByAccount = "ActionViewRPTransactionsByAccount";
	public static final String ActionViewTransactionsByTransfer = "ActionViewTransactionsByTransfer";
	public static final String ActionViewTransfersByAccount = "ActionViewTransfersByAccount";
	//public static final String ActionViewTransactions = "ActionViewTransactions";
	public static final String ActionViewFolders = "ActionViewFolders";
	public static final String ActionStartupPassword = "ActionStartupPassword";
	public static final String ActionControlPassword = "ActionControlPassword";
	public static final String ActionDisablePassword = "ActionDisablePassword";
	public static final String ActionAddFilterForExport = "ActionAddFilterForExport";
	public static final String ActionChooseFileForRestore = "ActionChooseFileForRestore";
	//public static final String ActionControlLicence = "ActionControlLicence";
	public static final String ActionViewAccountReport = "ActionViewAccountReport";
	public static final String ActionViewCategoryReport = "ActionViewCategoryReport";
	public static final String ActionViewAllTransactions = "ActionViewAllTransactions";
	
	public static int TransactionType = 0;
	public static final int TransactionTypeIncome = 1;
	public static final int TransactionTypeExpence = -1;
	
	public static int cursorPosition;

	public static String decimalCount = "2";
	public static String rateDecimalCount = "4";
	//public static final String rateDecimalCountLong = "6";

	public static int decimalCountUser = 2;
	//public static int decimalCountCurrencyUser = 4;
	public static char decimalSeparatorSymbol = '.';
	public static char digitGropingSymbol = ',';
	public static final int currencySignPositionLeft = 1;
	public static final int currencySignPositionRight = 2;
	public static int currencySignPosition = currencySignPositionLeft;

	public static final char MINUS = '\u2212';
	public static final char POWER_PLACEHOLDER = '\u200B';
	public static final char POWER = '^';
	public static final char MATRIX_SEPARATOR = ' ';

	public static final int maxNameLength = 30;
	public static final int maxButtonTextLength = 150;
	public static int defaultCurrency = -1;
	//public final int defaultBcpFileDateCount = 5;
	
	public static String query = "query";
	public static String title = "title";
	public static String dialogType = "DilaogType";
	public static String folderKey = "folder";
	public static String reportType = "reportType";
	//public static String hideButtons = "hideButtons";
	//public static String selectedAccount = "selectedAccount";
	
	//public static String backupFileName = "mmdb";
	public static final String baseDirectory = "mnt/sdcard/MoneyManager/";
	public static String backupDirectory = baseDirectory + "Backup";
	public static String receiptDirectory = baseDirectory + "Receipts";
	public static int backupDaysCount = 5;
	public static int backupMaxSizeMB = 2;
	//public static String exportDirectory = baseDirectory + "Export";
	public static final String exportFileName = "MoneyManager";
	
	public static String calculatorValue = "CalculatorValue";
	public static final String password = "password";
	public static final String passwordExists = "PasswordExists";
	
	//public static String dateFilterType;
	
	public static String paramFromDate = "fromDate";
	public static String paramToDate = "toDate";
	public static String paramAccountID = "accountID";
	public static String paramTransferID = "transferID";
	public static String paramCategory = "category";
	public static String paramTitle = "title";
	public static String paramValues = "values";
	public static String paramTransactionType = "transactionType";
	public static String paramSplitTransactionIndex = "splitTransactionIndex";
	public static String dontRefreshValues = "dontRefreshValues";
	public static String disableMultiSelect = "disableMultiSelect";
	
	public final static AccessType dropboxAccessType = AccessType.APP_FOLDER;
	public final static String dropboxKey = "9dn8ox451k4vbsg";
	public final static String dropboxSecret = "l3ca4gir0jom8ul";
	
	public static class DateTruncTypes {
		public static final String dateTruncWeek = "dateTruncWeek";
		public static final String dateTruncMonth = "dateTruncMonth";
		public static final String dateTruncYear = "dateTruncYear";
	}
	
	public static class ImpExpColNames {
		public static final String date = "Date";
		public static final String amount = "Amount";
		public static final String category = "Category";
		public static final String subcategory = "Subcategory";
		public static final String description = "Description";
		public static final String account = "Account";
		public static final String currency = "Currency";
	}
	
	public enum TransferType {
		 Once (0),
	     Daily (1),
	     Weekly (2),
	     Monthly (3),
	     Quarterly (4),
	     Yearly (5),
	     Custom (6);

	    private final int index;   

	    TransferType(int index) {
	        this.index = index;
	    }

	    public int index() { 
	        return index; 
	    }

	}

	public enum Status {
		 Disabled (0),
	     Enabled (1);

	    private final int index;   

	    Status(int index) {
	        this.index = index;
	    }

	    public int index() { 
	        return index; 
	    }

	}

	public enum DateFilterValues {
		 Today (0),
	     ThisWeek (1),
	     ThisMonth (2),
	     Last30Days (3),
	     ThisYear (4),
	     All (5);

	    private final int index;   

	    DateFilterValues(int index) {
	        this.index = index;
	    }

	    public int index() { 
	        return index; 
	    }

	}

	public enum ReportTimeInterval {
		Daily (0),
		Weekly (1),
		Monthly (2),
		Yearly (3),
		Custom (4);

		private final int index;

		ReportTimeInterval(int index) {
			this.index = index;
		}

		public int index() {
			return index;
		}
	}

	public enum ReportTimeIntervalBudget {
		ThisYear (0),
		Last12Month (1),
		Custom (2);

		private final int index;

		ReportTimeIntervalBudget(int index) {
			this.index = index;
		}

		public int index() {
			return index;
		}
	}

	public enum ReportTimeIntervalBudget2 {
		Monthly (0),
		Custom (1);

		private final int index;

		ReportTimeIntervalBudget2(int index) {
			this.index = index;
		}

		public int index() {
			return index;
		}
	}

	public enum StartupScreenValues {
		 Accounts (0),
	     Transactions (1);

	    private final int index;   

	    StartupScreenValues(int index) {
	        this.index = index;
	    }

	    public int index() { 
	        return index; 
	    }

	}
	
	public enum RPTransRemindValues {
		 Never (0),
	     SameDay (1),
	     OneDayBefore (2),
	     TwoDaysBefore (3),
	     ThreeDaysBefore (4),
	     SevenDaysBefore (5);

	    private final int index;   

	    RPTransRemindValues(int index) {
	        this.index = index;
	    }

	    public int index() { 
	        return index; 
	    }
	}		
	
	public enum TransFOperType {
		 All (0),
		 Transaction (1),
		 Transfer (2),
		 RpTransaction (3);

	    private final int index;   

	    TransFOperType(int index) {
	        this.index = index;
	    }

	    public int index() { 
	        return index; 
	    }
	}			
	
	public enum TransFTransaction {
		 All (0),
		 Income (1),
		 Expence (2);

	    private final int index;   

	    TransFTransaction(int index) {
	        this.index = index;
	    }

	    public int index() { 
	        return index; 
	    }
	}				
	
	public enum RateShouldControlValues {
		 Never (0),
		 Control (1);

	    private final int index;   

	    RateShouldControlValues(int index) {
	        this.index = index;
	    }

	    public int index() { 
	        return index; 
	    }
	}		
	
	public enum DBOperationType {
		 Insert (0),
		 Update (1),
		 Delete (2);

	    private final int index;   

	    DBOperationType(int index) {
	        this.index = index;
	    }

	    public int index() { 
	        return index; 
	    }
	}		
	
	public enum BackupMaxDaysValues {
		 OneDay (0),
	     ThreeDay (1),
	     OneWeek (2),
	     TwoWeek (3),
	     OneMonth (4),
	     TwoMonth (5);

	    private final int index;   

	    BackupMaxDaysValues(int index) {
	        this.index = index;
	    }

	    public int index() { 
	        return index; 
	    }
	    
	    public static int getValue(int index) {
	    	switch (index) {
			case 0:
				return 1;
			case 1:
				return 3;
			case 2:
				return 7;
			case 3:
				return 14;
			case 4:
				return 30;
			case 5:
				return 60;
			default:
				return 3;
			}
	    }
	}			
	
	public enum LanguageValues {
		Default(0),
		Azeri(1),
		Indonesian(2),
		Deutch(3),
		English(4),
		Espanol(5),
		French(6),
		Italian(7),
		Portugal(8),
		Russian(9),
		Ukrainian(10);

		private final int index;

		LanguageValues(int index) {
			this.index = index;
		}

		public int index() {
			return index;
		}

		public static String getValue(int index) {
			switch (index) {
				case 0:
					return "def";
				case 1:
					return "az";
				case 2:
					return "in";
				case 3:
					return "de";
				case 4:
					return "en";
				case 5:
					return "es";
				case 6:
					return "fr";
				case 7:
					return "it";
				case 8:
					return "pt";
				case 9:
					return "ru";
				case 10:
					return "uk";
				default:
					return "def";
			}
		}

		/*public static int getIndexByValue(String value) {
			switch (value) {
				case "en":
					return 1;
				case "ru":
					return 2;
				case "az":
					return 3;
				default:
					return 0;
			}
		}*/
	}		
	
	public enum BackupMaxSizeValues {
		 TwoMB (0),
	     TenMB (1),
	     OneHundredMB (2),
	     FiveHundredMB (3),
	     OneGB (4);

	    private final int index;   

	    BackupMaxSizeValues(int index) {
	        this.index = index;
	    }

	    public int index() { 
	        return index; 
	    }
	    
	    public static int getValue(int index) {
	    	switch (index) {
			case 0:
				return 2;
			case 1:
				return 10;
			case 2:
				return 100;
			case 3:
				return 500;
			case 4:
				return 1024;
			default:
				return 2;
			}
	    }
	}
}
