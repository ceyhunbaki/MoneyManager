package com.jgmoneymanager.main;

import java.io.File;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;
import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProvider;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VTransAccountViewMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.VTransferViewMetaData;
import com.jgmoneymanager.dialogs.ChooseFileDialog;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.dialogs.SecurityQuestion;
import com.jgmoneymanager.dialogs.SetPassword;
import com.jgmoneymanager.dialogs.StartupPassword2;
import com.jgmoneymanager.entity.MyPreferenceActivity;
import com.jgmoneymanager.tools.BackupDatabaseFileTask;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Constants.BackupMaxDaysValues;
import com.jgmoneymanager.tools.Constants.BackupMaxSizeValues;
import com.jgmoneymanager.tools.Constants.DateFilterValues;
import com.jgmoneymanager.tools.Constants.LanguageValues;
import com.jgmoneymanager.tools.Constants.StartupScreenValues;
import com.jgmoneymanager.tools.DropboxDownload;
import com.jgmoneymanager.tools.DropboxUploadTaskLocal;
import com.jgmoneymanager.tools.RestoreDatabaseFileTask;
import com.jgmoneymanager.tools.Tools;

public class SettingsScreen extends MyPreferenceActivity {
	//ListPreference listDateFilter;
	//ListPreference listStartupScreen;
	ListPreference listBackupMaxDays;
	ListPreference listBackupMaxSize;
	ListPreference listLanguages;
	ListPreference listHomeScreen;
	CheckBoxPreference checkBoxAutoBackup;
	CheckBoxPreference checkBoxBackupToData;
	CheckBoxPreference checkBoxAskPassword;
	CheckBoxPreference checkBoxDropboxAutoSync;
	Preference btBackupFolder;
	Preference btBackup;
	Preference btDropboxBackup;
	Preference btDropboxRestore;
	Preference btDropboxReset;
	Preference btPassword;
	Preference btForget;

	String[] listDateFilterValues = getDateFilterValues();
	String[] listStartupScreenValues = getStartupScreenValues();
	String[] listBackupMaxDaysValues = getBackupMaxDaysValues();
	String[] listBackupMaxSizeValues = getBackupMaxSizeValues();
	String[] languageValues = getLanguageValues();
	
	String oldPassword;
	boolean passwordExists;
	public static boolean languageChanged = false;
	public static boolean homeScreenVersionChanged = false;
	static final int dropAuthNotRequested = 0;
	static final int dropAuthBackupRequested = 1;
	static final int dropAuthRestoreRequested = 2;
	static int dropboxAuthRequested = dropAuthNotRequested;
	
	//final static private String APP_KEY = "INSERT_APP_KEY";
	//final static private String APP_SECRET = "INSERT_APP_SECRET";
    private DropboxAPI<AndroidAuthSession> mDBApi;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.settings_screen);

		addPreferencesFromResource(R.xml.mmoptions);

		AppKeyPair appKeys = new AppKeyPair(Constants.dropboxKey, Constants.dropboxSecret);
		AndroidAuthSession session = new AndroidAuthSession(appKeys, Constants.dropboxAccessType);
		mDBApi = new DropboxAPI<AndroidAuthSession>(session);

		/*listDateFilter = (ListPreference) findPreference(getResources().getString(R.string.setTransactionListDFKey));
		listDateFilter.setEntryValues(listDateFilterValues);

		listStartupScreen = (ListPreference) findPreference(getResources().getString(R.string.setStartupScreenKey));
		listStartupScreen.setEntryValues(listStartupScreenValues);*/

		listBackupMaxDays = (ListPreference) findPreference(getResources().getString(R.string.backupMaxDateKey));
		listBackupMaxDays.setEntryValues(listBackupMaxDaysValues);

		listBackupMaxSize = (ListPreference) findPreference(getResources().getString(R.string.backupMaxSizeKey));
		listBackupMaxSize.setEntryValues(listBackupMaxSizeValues);

		listLanguages = (ListPreference) findPreference(getResources().getString(R.string.setLanguageKey));
		listLanguages.setEntryValues(languageValues);
		listLanguages.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Tools.loadLanguage(SettingsScreen.this, newValue.toString());
				DBTools.execQuery(SettingsScreen.this, "Drop view if exists " + VTransAccountViewMetaData.VIEW_NAME);
				DBTools.execQuery(SettingsScreen.this,
						MoneyManagerProvider.DatabaseHelper.DATABASE_CREATE_VIEW_VTRANSACCOUNTS.replace("'ALL'",
								"'" + getResources().getString(R.string.all) + "'"));
				DBTools.execQuery(SettingsScreen.this, "Drop view if exists " + VTransferViewMetaData.VIEW_NAME);
				DBTools.execQuery(SettingsScreen.this,
						MoneyManagerProvider.DatabaseHelper.DATABASE_CREATE_VIEW_VTRANSFER.
								replace("income", getResources().getString(R.string.income).toLowerCase()).
								replace("expense", getResources().getString(R.string.expense).toLowerCase()));
				languageChanged = true;
				restartActivity();
				return true;
			}
		});

		listHomeScreen = (ListPreference) findPreference(getResources().getString(R.string.setHomeScreenKey));
		String summary;
		if (Tools.getPreference(this, R.string.setHomeScreenKey).equals(getResources().getString(R.string.version1Key)))
			summary = getString(R.string.version1);
		else
			summary = getString(R.string.version2);
		listHomeScreen.setSummary(summary);
		listHomeScreen.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				homeScreenVersionChanged = true;
				if (newValue.equals(getResources().getString(R.string.version1Key)))
					listHomeScreen.setSummary(getString(R.string.version1));
				else
					listHomeScreen.setSummary(getString(R.string.version2));
				return true;
			}
		});

		checkBoxAutoBackup = (CheckBoxPreference) findPreference(getResources().getString(R.string.autoBackupKey));

		btBackup = findPreference(getString(R.string.backupKey));
		btBackup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				File file = new File(Constants.backupDirectory);
				if (!file.exists())
					if (!file.mkdirs()) {
						AlertDialog warning = DialogTools.warningDialog(SettingsScreen.this, R.string.msgWarning, SettingsScreen.this.getString(R.string.msgChooseBackupFolder));
						warning.show();
						return true;
					}
				final EditText input = new EditText(SettingsScreen.this);
				input.setText(Tools.DateToString(Tools.getCurrentDateTime(), Constants.DateFormatBackup));
				Command cmd = new Command() {
					@Override
					public void execute() {
						BackupDatabaseFileTask backupDBTask = new BackupDatabaseFileTask(getBaseContext(), input.getText().toString());
						backupDBTask.execute("");
					}
				};
				AlertDialog inputDialog = DialogTools.InputDialog(SettingsScreen.this, cmd, R.string.msgBckFileName, input, R.drawable.ic_menu_manage);
				inputDialog.show();
				inputDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(input.getText().toString().trim().length() != 0);

				return true;
			}
		});

		Preference btRestore = findPreference(getString(R.string.restoreKey));
		btRestore.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				Command cmd = new Command() {
					@Override
					public void execute() {
						backupDB(Tools.DateToString(Tools.getCurrentDateTime(), Constants.DateFormatBackup));
						Intent intent = new Intent(getBaseContext(), ChooseFileDialog.class);
						Bundle bundle = new Bundle();
						bundle.putString(Constants.title, getString(R.string.menuRestore));
						intent.setAction(Constants.ActionChooseFileForRestore);
						intent.putExtras(bundle);
						startActivityForResult(intent, Constants.RequestDialogForRestore);
					}
				};
				String[] buttonNames = new String[] {getString(R.string.Continue), getString(R.string.Cancel)};
				AlertDialog warning = DialogTools.confirmDialog(SettingsScreen.this, cmd, R.string.msgConfirm,
						R.string.restoreWarning, buttonNames);
				warning.show();
				return true;
			}
		});

		btBackupFolder = findPreference(getResources().getString(R.string.backupFolderKey));
		btBackupFolder.setEnabled(!PreferenceManager.getDefaultSharedPreferences(SettingsScreen.this).
				getBoolean(SettingsScreen.this.getString(R.string.backupToDataFolderKey), false));
		btBackupFolder.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				Intent intent = new Intent(getBaseContext(), FileExplorer.class);
				intent.setAction(Constants.ActionViewFolders);
				Bundle bundle = new Bundle();
				bundle.putString(Constants.title, getString(R.string.msgChooseFolder));
				bundle.putInt(Constants.dialogType, FileExplorer.DialogOpenFolderID);
				bundle.putString(Constants.folderKey, Constants.backupDirectory);
				intent.putExtras(bundle);
				startActivityForResult(intent, Constants.RequestDialogForBackupFolder);
				return true;
			}
		});

		checkBoxBackupToData = (CheckBoxPreference) findPreference(getResources().getString(R.string.backupToDataFolderKey));
		checkBoxBackupToData.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				btBackupFolder.setEnabled(!(Boolean)newValue);
				if ((Boolean)newValue)
					Constants.backupDirectory = Environment.getDataDirectory() + "/data/com.jgmoneymanager.main/";
				else
					Constants.backupDirectory = getString(R.string.backupFolderDefaultValue);
				refreshBackupFolderNames();
				return true;
			}
		});

		checkBoxDropboxAutoSync = (CheckBoxPreference) findPreference(getResources().getString(R.string.dropboxAutoSyncKey));
		checkBoxDropboxAutoSync.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object o) {
				if (!Tools.isPreferenceAvialable(SettingsScreen.this, R.string.dropboxTokenKey) && ((Boolean)o)) {
					Command backupCommand = new Command() {
						@Override
						public void execute() {
							dropboxBackupAction();
						}
					};
					Command restoreCommand = new Command() {
						@Override
						public void execute() {
							dropboxRestoreAction();
						}
					};
					Command cancelCommand = new Command() {
						@Override
						public void execute() {
							Tools.setPreference(SettingsScreen.this, R.string.dropboxAutoSyncKey, false);
							checkBoxDropboxAutoSync.setChecked(false);
						}
					};

					AlertDialog dialog = DialogTools.confirmWithCancelDialog(SettingsScreen.this, backupCommand, restoreCommand, cancelCommand,
							R.string.msgWarning, getResources().getString(R.string.msgDropboxNotAutorised),
							new String[] {getResources().getString(R.string.menuBackup),
									getResources().getString(R.string.menuRestore),
									getResources().getString(R.string.Cancel)});
					dialog.show();
				}
				return true;
			}
		});

		refreshBackupFolderNames();

		Preference btExport = findPreference("export");
		btExport.setSummary(getResources().getString(R.string.btExportSummary));
		btExport.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				Intent intent = new Intent(getBaseContext(), FileExplorer.class);
				intent.setAction(Constants.ActionViewFolders);
				Bundle bundle = new Bundle();
				bundle.putString(Constants.title, getString(R.string.msgChooseFolder));
				bundle.putInt(Constants.dialogType, FileExplorer.DialogOpenFolderID);
				bundle.putString(Constants.folderKey, Constants.backupDirectory);
				intent.putExtras(bundle);
				startActivityForResult(intent, Constants.RequestDialogForExport);
				return true;
			}
		});

		Preference btImport = findPreference("import");
		btImport.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				Intent intent = new Intent(getBaseContext(), ChooseFileDialog.class);
				Bundle bundle = new Bundle();
				bundle.putString(Constants.title, getString(R.string.menuImport));
				intent.putExtras(bundle);
				intent.setAction(Intent.ACTION_PICK);
				startActivityForResult(intent, Constants.RequestDialogForImport);
				return true;
			}
		});

		btPassword = findPreference(getResources().getString(R.string.setPasswordKey));
		/*btPassword.setEnabled(Tools.isPreferenceAvialable(SettingsScreen.this, R.string.emailKey) ||
				Tools.isPreferenceAvialable(SettingsScreen.this, R.string.securityAnswerKey));*/
		oldPassword = Tools.getPreference(SettingsScreen.this, R.string.setPasswordKey);
		passwordExists = !oldPassword.equals("null");
		if (passwordExists)
			btPassword.setTitle(getString(R.string.changePassword));
		btPassword.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				if (!Tools.isPreferenceAvialable(getBaseContext(), R.string.securityAnswerKey)) {
					DialogTools.toastDialog(getBaseContext(), R.string.securityQuestionSummary, Toast.LENGTH_LONG);
					return false;
				}
				Intent intent = new Intent(getBaseContext(), SetPassword.class);
				intent.putExtra(Constants.passwordExists, passwordExists);
				intent.putExtra(Constants.password, oldPassword);
				startActivityForResult(intent, Constants.RequestPassword);
				return true;
			}
		});

		btForget = findPreference(getResources().getString(R.string.forgetPasswordKey));
		btForget.setEnabled(passwordExists);
		btForget.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				SetPassword.forgetPassword(SettingsScreen.this);
				return true;
			}
		});

		/*final Preference btEmail = (Preference) findPreference(getResources().getString(R.string.emailKey));
		if (PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.emailKey), "null") != "null")
			btEmail.setTitle(getString(R.string.changeEmail));
		btEmail.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				if (Tools.isPreferenceAvialable(SettingsScreen.this, R.string.setPasswordKey)) {
		        	Intent intent = new Intent(SettingsScreen.this, StartupPassword.class);
		        	intent.setAction(Constants.ActionControlPassword);
		        	startActivityForResult(intent, Constants.RequestPasswordForEmail);
				}
				else
					setEmailPrefrence(SettingsScreen.this);
				return true;
			}
		});	*/

		Preference btSeqQuestion = findPreference(getResources().getString(R.string.securityQuestionKey));
		btSeqQuestion.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				if (Tools.isPreferenceAvialable(SettingsScreen.this, R.string.setPasswordKey)) {
					Intent intent = new Intent(SettingsScreen.this, StartupPassword2.class);
					intent.setAction(Constants.ActionControlPassword);
					startActivityForResult(intent, Constants.RequestPasswordForQuestion);
				}
				else
					startActivityForResult(new Intent(getBaseContext(), SecurityQuestion.class), Constants.RequestQuestionDialog);
				return true;
			}
		});

		checkBoxAskPassword = (CheckBoxPreference) findPreference(getResources().getString(R.string.askpasswordkey));
		checkBoxAskPassword.setEnabled(/*Tools.isPreferenceAvialable(SettingsScreen.this, R.string.emailKey) ||*/
				Tools.isPreferenceAvialable(SettingsScreen.this, R.string.setPasswordKey));
		checkBoxAskPassword.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if ((Boolean) newValue) {
					if (!Tools.isPreferenceAvialable(getBaseContext(), R.string.securityAnswerKey)) {
						DialogTools.toastDialog(getBaseContext(), R.string.securityQuestionSummary, Toast.LENGTH_LONG);
						return false;
					}
					if (!passwordExists) {
						Intent intent = new Intent(getBaseContext(), SetPassword.class);
						intent.putExtra(Constants.passwordExists, passwordExists);
						intent.putExtra(Constants.password, oldPassword);
						startActivityForResult(intent, Constants.RequestPasswordPlusProtection);
						return false;
					}
				} else if (passwordExists) {
					Intent intent = new Intent(getBaseContext(), StartupPassword2.class);
					intent.setAction(Constants.ActionDisablePassword);
					startActivityForResult(intent, Constants.RequestPasswordForDisabling);
					return false;
				}
				return true;
			}
		});

		Preference btAbout = findPreference(getResources().getString(R.string.pAbout));
		btAbout.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Tools.showAboutDialog(SettingsScreen.this);
				return false;
			}
		});

		Preference btAdsFree = findPreference(getResources().getString(R.string.pAdsFree));
		btAdsFree.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				if (Tools.getPreferenceBool(SettingsScreen.this, R.string.proInstalledKey, false))
					DialogTools.toastDialog(SettingsScreen.this, R.string.adsRemoveSummaryRemoved, Toast.LENGTH_LONG);
				else {
					Tools.removeAds(SettingsScreen.this);
				}
				return true;
			}
		});

		Preference btHelp = findPreference(getResources().getString(R.string.pHelp));
		btHelp.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://sites.google.com/site/jgmoneymanager/"));
				startActivityForResult(browserIntent, Constants.RequestNONE);
				return true;
			}
		});

		Preference btSendEmail = findPreference(getResources().getString(R.string.sendEmailKey));
		btSendEmail.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent emailIntent=new Intent(Intent.ACTION_SEND);
				String[] recipients = new String[]{Constants.supportEmail};
				emailIntent.putExtra(Intent.EXTRA_EMAIL, recipients);
				emailIntent.putExtra(Intent.EXTRA_SUBJECT, Constants.emailSubject + ", v" + Tools.getApplicationVersion(SettingsScreen.this));
				emailIntent.setType("message/rfc822");
				startActivityForResult(emailIntent, Constants.RequestNONE);
				return false;
			}
		});

		btDropboxBackup = findPreference(getString(R.string.dropboxBackupKey));
		btDropboxBackup.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				dropboxBackupAction();
				return true;
			}
		});

		btDropboxRestore = findPreference(getString(R.string.dropboxRestoreKey));
		btDropboxRestore.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				dropboxRestoreAction();
				return true;
			}
		});

		btDropboxReset = findPreference(getString(R.string.dropboxResetUserKey));
		btDropboxReset.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				try {
					mDBApi.getSession().unlink();
					Tools.removePreferense(SettingsScreen.this, R.string.dropboxTokenKey);
					DialogTools.toastDialog(SettingsScreen.this, R.string.dropboxUserAccess, Toast.LENGTH_LONG);
					return true;
				} catch (Exception e) {
					return false;
				}
			}
		});
	}

	String[] getDateFilterValues() {
		String[] values = new String[Constants.DateFilterValues.values().length];
		int i = 0;
		for (DateFilterValues value : Constants.DateFilterValues.values()) {
			values[i] = String.valueOf(value.index());
			i++;
		}
		return values;
	}

	String[] getStartupScreenValues() {
		String[] values = new String[Constants.StartupScreenValues.values().length];
		int i = 0;
		for (StartupScreenValues value : Constants.StartupScreenValues.values()) {
			values[i] = String.valueOf(value.index());
			i++;
		}
		return values;
	}

	String[] getBackupMaxDaysValues() {
		String[] values = new String[Constants.BackupMaxDaysValues.values().length];
		int i = 0;
		for (BackupMaxDaysValues value : Constants.BackupMaxDaysValues.values()) {
			values[i] = String.valueOf(value.index());
			i++;
		}
		return values;
	}

	String[] getBackupMaxSizeValues() {
		String[] values = new String[Constants.BackupMaxSizeValues.values().length];
		int i = 0;
		for (BackupMaxSizeValues value : Constants.BackupMaxSizeValues.values()) {
			values[i] = String.valueOf(value.index());
			i++;
		}
		return values;
	}

	String[] getLanguageValues() {
		String[] values = new String[Constants.LanguageValues.values().length];
		int i = 0;
		for (LanguageValues value : Constants.LanguageValues.values()) {
			values[i] = LanguageValues.getValue(value.index());
			i++;
		}
		return values;
	}
	
	private void restartActivity() {
	    Intent intent = getIntent();
	    finish();
	    startActivity(intent);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (mDBApi.getSession().authenticationSuccessful() && (dropboxAuthRequested != dropAuthNotRequested)) {
	        try {
	            mDBApi.getSession().finishAuthentication();
	            String accessToken = mDBApi.getSession().getOAuth2AccessToken();
	            Tools.setPreference(SettingsScreen.this, R.string.dropboxTokenKey, accessToken, false);
	            dropBoxBackupRestore(dropboxAuthRequested);
	        } catch (IllegalStateException e) {
	            Log.i("DbAuthLog", "Error authenticating", e);
	        }
	    }
	}
	
	void dropBoxBackupRestore(int type) {
		File file = new File(Environment.getDataDirectory() + "/data/com.jgmoneymanager.main/databases/"
				+ MoneyManagerProviderMetaData.DATABASE_NAME);
		if (type == dropAuthBackupRequested) {
			DropboxUploadTaskLocal dUpload = new DropboxUploadTaskLocal(SettingsScreen.this, mDBApi, "", file, true);
			dUpload.execute();
		}
		else if (type == dropAuthRestoreRequested) {
			DropboxDownload dDownload = new DropboxDownload(SettingsScreen.this, mDBApi, "", file, null);
			dDownload.execute();
		}
		dropboxAuthRequested = dropAuthNotRequested;
	}

	void dropboxBackupAction() {
		if (!Tools.isInternetAvailable(SettingsScreen.this)) {
			DialogTools.toastDialog(SettingsScreen.this, R.string.msgInternetUnavailable, Toast.LENGTH_LONG);
		}
		else {
			Command cmdBackupConfirm = new Command() {
				@Override
				public void execute() {
					if (Tools.getPreference(SettingsScreen.this, R.string.dropboxTokenKey).equals("null")) {
						/*Command cmd = new Command() {
							@Override
							public void execute() {*/
								dropboxAuthRequested = dropAuthBackupRequested;
								mDBApi.getSession().startOAuth2Authentication(SettingsScreen.this);
						/*	}
						};
						AlertDialog dialog = DialogTools.informationDialog(SettingsScreen.this, R.string.msgAttention,
								R.string.msgDropBoxAttention, cmd);
						dialog.show();*/
					}
					else {
						mDBApi.getSession().setOAuth2AccessToken(Tools.getPreference(SettingsScreen.this, R.string.dropboxTokenKey));
						dropBoxBackupRestore(dropAuthBackupRequested);
					}
				}
			};
			AlertDialog confirmBackupDialog = new DialogTools().confirmDialog(SettingsScreen.this, cmdBackupConfirm, R.string.msgConfirm, R.string.btBackupDropboxSummary);
			confirmBackupDialog.show();
		}
	}

	void dropboxRestoreAction() {
		if (!Tools.isInternetAvailable(SettingsScreen.this)) {
			DialogTools.toastDialog(SettingsScreen.this, R.string.msgInternetUnavailable, Toast.LENGTH_LONG);
		}
		else {
			final Command cmdRestoreConfirm = new Command() {
				@Override
				public void execute() {
					if (Tools.getPreference(SettingsScreen.this, R.string.dropboxTokenKey).equals("null")) {
						/*Command cmd = new Command() {
							@Override
							public void execute() {*/
								dropboxAuthRequested = dropAuthRestoreRequested;
								mDBApi.getSession().startOAuth2Authentication(SettingsScreen.this);
							/*}
						};
						AlertDialog dialog = DialogTools.informationDialog(SettingsScreen.this, R.string.msgAttention,
								R.string.msgDropBoxAttention, cmd);
						dialog.show();*/
					}
					else {
						mDBApi.getSession().setOAuth2AccessToken(Tools.getPreference(SettingsScreen.this, R.string.dropboxTokenKey));
						dropBoxBackupRestore(dropAuthRestoreRequested);
					}
				}
			};
			String[] buttonNames = new String[] {getString(R.string.Continue), getString(R.string.Cancel)};
			AlertDialog confirmRestoreDailog = DialogTools.confirmDialog(SettingsScreen.this, cmdRestoreConfirm,
					R.string.msgConfirm, R.string.restoreWarning, buttonNames);
			confirmRestoreDailog.show();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data); 
		if (resultCode == RESULT_OK) {
			if (requestCode == Constants.RequestDialogForRestore)
			{
				RestoreDatabaseFileTask restoreDBTask = new RestoreDatabaseFileTask(getBaseContext(), ChooseFileDialog.filePath);
				restoreDBTask.execute("");
				languageChanged = true;
			}
			else if (requestCode == Constants.RequestDialogForImport)
				Tools.importExpenceManagerCSV(getBaseContext(), ChooseFileDialog.filePath);
			else if (requestCode == Constants.RequestDialogForBackupFolder) {
				Tools.setPreference(getBaseContext(), R.string.backupFolderKey, FileExplorer.filePath, false);
				Constants.backupDirectory = FileExplorer.filePath;
				refreshBackupFolderNames();
			}
			else if (requestCode == Constants.RequestDialogForExport) {
				Intent intent = new Intent(getBaseContext(), TransactionFilter.class);
				intent.setAction(Constants.ActionAddFilterForExport);
				startActivityForResult(intent, Constants.RequestFilterForExport);
				//Tools.exportToCSV(getBaseContext(), FileExplorer.filePath);
			}
			else if ((requestCode == Constants.RequestPassword) || (requestCode == Constants.RequestPasswordPlusProtection)) {
				oldPassword = data.getStringExtra(Constants.password);
				if (!passwordExists) {
					passwordExists = true;
					btPassword.setTitle(getString(R.string.changePassword));
					btForget.setEnabled(passwordExists);
					checkBoxAskPassword.setEnabled(passwordExists);
				}
				if (requestCode == Constants.RequestPasswordPlusProtection) 
					checkBoxAskPassword.setChecked(true);
			}
			else if (requestCode == Constants.RequestPasswordForDisabling) 
				checkBoxAskPassword.setChecked(false);
			//else if (requestCode == Constants.RequestPasswordForEmail) 
			//	setEmailPrefrence(SettingsScreen.this);
			else if (requestCode == Constants.RequestPasswordForQuestion) 
				startActivityForResult(new Intent(getBaseContext(), SecurityQuestion.class), Constants.RequestQuestionDialog);
			else if (requestCode == Constants.RequestQuestionDialog)
				enablePasswordButtons();
			else if (requestCode == Constants.RequestFilterForExport) {
				Bundle bundle = data.getExtras();
				String conditions = bundle.getString(Constants.query);
				Tools.exportToCSV(getBaseContext(), FileExplorer.filePath, conditions);
			}
		}
	}
	
	void backupDB(String fileName) {
		File file = new File(Constants.backupDirectory);
		if (file.exists()) {
			BackupDatabaseFileTask backupDBTask = new BackupDatabaseFileTask(getBaseContext(), fileName);
			backupDBTask.execute("");
		}
	}
	
	void setEmailPrefrence(final Context context) {
		final EditText edEmail = new EditText(context);
		if (Tools.isPreferenceAvialable(context, R.string.emailKey))
			edEmail.setText(Tools.getPreference(context, R.string.emailKey));
		Command cmd = new Command() {						
			@Override
			public void execute() {
				Tools.setPreference(context, R.string.emailKey, edEmail.getText().toString(), false);
				enablePasswordButtons();				
			}
		};
		AlertDialog emailDialog = DialogTools.InputDialog(context, cmd, R.string.setEmail, edEmail, R.drawable.ic_menu_edit);
		emailDialog.show();
		emailDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(edEmail.getText().toString().trim().length() != 0);
	}
	
	void enablePasswordButtons() {
		if (!btPassword.isEnabled()) {
			btPassword.setEnabled(true);
			checkBoxAskPassword.setEnabled(true);
		}
	}
	
	void refreshBackupFolderNames() {
		btBackupFolder.setSummary(getString(R.string.backupFolderSummary) + " " + Constants.backupDirectory);
		checkBoxAutoBackup.setSummary(getResources().getString(R.string.autoBackupSummary) + " " + 
				Constants.backupDirectory);
		btBackup.setSummary(getResources().getString(R.string.btBackupSummary) + " " + Constants.backupDirectory);
	}
}