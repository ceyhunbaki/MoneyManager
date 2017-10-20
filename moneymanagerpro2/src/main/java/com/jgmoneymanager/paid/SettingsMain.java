package com.jgmoneymanager.paid;

import android.app.AlertDialog;
import android.app.Dialog;
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

//import com.cloudrail.si.CloudRail;
import com.dropbox.core.android.Auth;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData;
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
import com.jgmoneymanager.tools.DropboxDownload;
import com.jgmoneymanager.tools.DropboxUploadTask;
import com.jgmoneymanager.tools.LocalTools;
import com.jgmoneymanager.tools.RestoreDatabaseFileTask;
import com.jgmoneymanager.tools.Tools;

import java.io.File;

public class SettingsMain extends MyPreferenceActivity {
	ListPreference listBackupMaxDays;
	ListPreference listBackupMaxSize;
	ListPreference listHomeScreen;
	CheckBoxPreference checkBoxAutoBackup;
	CheckBoxPreference checkBoxBackupToData;
	CheckBoxPreference checkBoxAskPassword;
	CheckBoxPreference checkBoxDropboxAutoSync;
	CheckBoxPreference checkBoxDropboxAutoSyncWiFi;
	Preference btBackupFolder;
	Preference btReceiptsFolder;
	Preference btBackup;
	Preference btDropboxBackup;
	Preference btDropboxRestore;
	Preference btDropboxReset;
	Preference btPassword;
	Preference btForget;
	Preference btLocalization;

	String[] listBackupMaxDaysValues = getBackupMaxDaysValues();
	String[] listBackupMaxSizeValues = getBackupMaxSizeValues();

	String oldPassword;
	boolean passwordExists;
	public static boolean languageChanged = false;
	public static boolean homeScreenVersionChanged = false;
	public static boolean loadSettings = false;
	static final int dropAuthNotRequested = 0;
	static final int dropAuthBackupRequested = 1;
	static final int dropAuthRestoreRequested = 2;
	static int dropboxAuthRequested = dropAuthNotRequested;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.settings_screen);

		addPreferencesFromResource(R.xml.mmoptions);

		listBackupMaxDays = (ListPreference) findPreference(getResources().getString(R.string.backupMaxDateKey));
		listBackupMaxDays.setEntryValues(listBackupMaxDaysValues);

		listBackupMaxSize = (ListPreference) findPreference(getResources().getString(R.string.backupMaxSizeKey));
		listBackupMaxSize.setEntryValues(listBackupMaxSizeValues);

		/*listLanguages = (ListPreference) findPreference(getResources().getString(R.string.setLanguageKey));
		listLanguages.setEntryValues(languageValues);
		listLanguages.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Tools.loadLanguage(SettingsMain.this, newValue.toString());
				DBTools.execQuery(SettingsMain.this, "Drop view if exists " + VTransAccountViewMetaData.VIEW_NAME);
				DBTools.execQuery(SettingsMain.this,
						MoneyManagerProvider.DatabaseHelper.DATABASE_CREATE_VIEW_VTRANSACCOUNTS.replace("'ALL'",
								"'" + getResources().getString(R.string.all) + "'"));
				DBTools.execQuery(SettingsMain.this, "Drop view if exists " + VTransferViewMetaData.VIEW_NAME);
				DBTools.execQuery(SettingsMain.this,
						MoneyManagerProvider.DatabaseHelper.DATABASE_CREATE_VIEW_VTRANSFER.
								replace("income", getResources().getString(R.string.income).toLowerCase()).
								replace("expense", getResources().getString(R.string.expense).toLowerCase()));
				languageChanged = true;
				restartActivity();
				return true;
			}
		});*/

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

		btLocalization = findPreference(getString(R.string.setLanguageOptsKey));
		btLocalization.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent intent = new Intent(SettingsMain.this, SettingsLanguage.class);
				startActivityForResult(intent, Constants.RequestSettingsLocalization);
				return true;
			}
		});

		btReceiptsFolder = findPreference(getResources().getString(R.string.receiptFolderKey));
		btReceiptsFolder.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				Intent intent = new Intent(getBaseContext(), FileExplorer.class);
				intent.setAction(Constants.ActionViewFolders);
				Bundle bundle = new Bundle();
				bundle.putString(Constants.title, getString(R.string.msgChooseFolder));
				bundle.putInt(Constants.dialogType, FileExplorer.DialogOpenFolderID);
				bundle.putString(Constants.folderKey, Constants.receiptDirectory);
				intent.putExtras(bundle);
				startActivityForResult(intent, Constants.RequestDialogForReceiptFolder);
				return true;
			}
		});

		checkBoxAutoBackup = (CheckBoxPreference) findPreference(getResources().getString(R.string.autoBackupKey));

		btBackup = findPreference(getString(R.string.backupKey));
		btBackup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				/*File file = new File(Constants.backupDirectory);
				if (!file.exists())
					if (!file.mkdirs()) {
						AlertDialog warning = DialogTools.warningDialog(SettingsMain.this, R.string.msgWarning, SettingsMain.this.getString(R.string.msgChooseBackupFolder));
						warning.show();
						return true;
					}
				final EditText input = new EditText(SettingsMain.this);
				input.setText(Tools.DateToString(Tools.getCurrentDateTime(), Constants.DateFormatBackup));
				Command cmd = new Command() {
					@Override
					public void execute() {
						BackupDatabaseFileTask backupDBTask = new BackupDatabaseFileTask(getBaseContext(), input.getText().toString());
						backupDBTask.execute("");
					}
				};
				AlertDialog inputDialog = DialogTools.InputDialog(SettingsMain.this, cmd, R.string.msgBckFileName, input, R.drawable.ic_menu_manage);
				inputDialog.show();
				inputDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(input.getText().toString().trim().length() != 0);*/
				Tools.backupToMemory(SettingsMain.this);
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
				AlertDialog warning = DialogTools.confirmDialog(SettingsMain.this, cmd, R.string.msgConfirm,
						R.string.restoreWarning, buttonNames);
				warning.show();
				return true;
			}
		});

		btBackupFolder = findPreference(getResources().getString(R.string.backupFolderKey));
		btBackupFolder.setEnabled(!PreferenceManager.getDefaultSharedPreferences(SettingsMain.this).
				getBoolean(SettingsMain.this.getString(R.string.backupToDataFolderKey), false));
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
					Constants.backupDirectory = Environment.getDataDirectory() + "/data/" + getPackageName() + "/";
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
				if (!Tools.isPreferenceAvialable(SettingsMain.this, R.string.dropboxTokenKey) && ((Boolean)o)) {
					Tools.setPreference(SettingsMain.this, R.string.dropboxAutoSyncKey, false);
                    checkBoxDropboxAutoSync.setChecked(false);
					o = false;
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
							Tools.setPreference(SettingsMain.this, R.string.dropboxAutoSyncKey, false);
						}
					};

					AlertDialog dialog = DialogTools.confirmWithCancelDialog(SettingsMain.this, backupCommand, restoreCommand, cancelCommand,
							R.string.msgWarning, getResources().getString(R.string.msgDropboxNotAutorised),
							new String[] {getResources().getString(R.string.menuBackup),
									getResources().getString(R.string.menuRestore),
									getResources().getString(R.string.Cancel)});
					dialog.show();
					return false;
				}
				refreshDropboxUploadWiFiButtonState((Boolean)o);
				return true;
			}
		});

		refreshDropboxUploadWiFiButtonState(null);

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
		/*btPassword.setEnabled(Tools.isPreferenceAvialable(SettingsMain.this, R.string.emailKey) ||
				Tools.isPreferenceAvialable(SettingsMain.this, R.string.securityAnswerKey));*/
		oldPassword = Tools.getPreference(SettingsMain.this, R.string.setPasswordKey);
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
				SetPassword.forgetPassword(SettingsMain.this);
				return true;
			}
		});

		Preference btSeqQuestion = findPreference(getResources().getString(R.string.securityQuestionKey));
		btSeqQuestion.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				if (Tools.isPreferenceAvialable(SettingsMain.this, R.string.setPasswordKey)) {
					Intent intent = new Intent(SettingsMain.this, StartupPassword2.class);
					intent.setAction(Constants.ActionControlPassword);
					startActivityForResult(intent, Constants.RequestPasswordForQuestion);
				}
				else
					startActivityForResult(new Intent(getBaseContext(), SecurityQuestion.class), Constants.RequestQuestionDialog);
				return true;
			}
		});

		checkBoxAskPassword = (CheckBoxPreference) findPreference(getResources().getString(R.string.askpasswordkey));
		checkBoxAskPassword.setEnabled(/*Tools.isPreferenceAvialable(SettingsMain.this, R.string.emailKey) ||*/
				Tools.isPreferenceAvialable(SettingsMain.this, R.string.setPasswordKey));
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
				Tools.showAboutDialog(SettingsMain.this, R.string.app_name_pro);
				return false;
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
				emailIntent.putExtra(Intent.EXTRA_SUBJECT, Constants.emailSubject + ", v" + Tools.getApplicationVersion(SettingsMain.this));
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
					Tools.removePreferense(SettingsMain.this, R.string.dropboxTokenKey);
					DialogTools.toastDialog(SettingsMain.this, R.string.dropboxUserAccess, Toast.LENGTH_LONG);
					return true;
				} catch (Exception e) {
					return false;
				}
			}
		});
	}

	void refreshDropboxUploadWiFiButtonState(Boolean state) {
		if (checkBoxDropboxAutoSyncWiFi == null)
			checkBoxDropboxAutoSyncWiFi = (CheckBoxPreference) findPreference(getResources().getString(R.string.dropboxAutoSyncWiFiKey));
		if (state == null)
			checkBoxDropboxAutoSyncWiFi.setEnabled(checkBoxDropboxAutoSync.isChecked());
		else
			checkBoxDropboxAutoSyncWiFi.setEnabled(state);
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
	
	@Override
	protected void onResume() {
		super.onResume();
		if (Auth.getOAuth2Token() != null)
		{
			Log.i("SettingsMain-OnRESUME", "Girdi");
			Tools.setPreference(SettingsMain.this, R.string.dropboxTokenKey, Auth.getOAuth2Token().toString(), false);
			dropBoxBackupRestore(dropboxAuthRequested);
		}
	}

	void dropBoxBackupRestore(int type) {
		if (type == dropAuthBackupRequested) {
			DropboxUploadTask dUpload = new DropboxUploadTask(SettingsMain.this, Tools.getDropboxClient(SettingsMain.this), Tools.getDatabaseFile(SettingsMain.this), true, null);
			dUpload.execute();
		}
		else if (type == dropAuthRestoreRequested) {
			DropboxDownload dDownload = new DropboxDownload(SettingsMain.this, Tools.getDropboxClient(SettingsMain.this), Tools.getDatabaseFile(SettingsMain.this), null);
			dDownload.execute();
		}
		dropboxAuthRequested = dropAuthNotRequested;
	}

	void dropboxBackupAction() {
		if (!Tools.isInternetAvailable(SettingsMain.this)) {
			DialogTools.toastDialog(SettingsMain.this, R.string.msgInternetUnavailable, Toast.LENGTH_LONG);
		}
		else {
			Command cmdBackupConfirm = new Command() {
				@Override
				public void execute() {
					if (Tools.getPreference(SettingsMain.this, R.string.dropboxTokenKey).equals("null")) {
						dropboxAuthRequested = dropAuthBackupRequested;
						Auth.startOAuth2Authentication(SettingsMain.this, Constants.dropboxKey);
					}
					else {
						dropboxAuthRequested = dropAuthBackupRequested;
						dropBoxBackupRestore(dropAuthBackupRequested);
					}
				}
			};
			AlertDialog confirmBackupDialog = new DialogTools().confirmDialog(SettingsMain.this, cmdBackupConfirm, R.string.msgConfirm, R.string.btBackupDropboxSummary);
			confirmBackupDialog.show();
		}
	}

	void dropboxRestoreAction() {
		if (!Tools.isInternetAvailable(SettingsMain.this)) {
			DialogTools.toastDialog(SettingsMain.this, R.string.msgInternetUnavailable, Toast.LENGTH_LONG);
		}
		else {
			final Command cmdRestoreConfirm = new Command() {
				@Override
				public void execute() {
					dropboxAuthRequested = dropAuthRestoreRequested;
					if (Tools.getPreference(SettingsMain.this, R.string.dropboxTokenKey).equals("null")) {
						Auth.startOAuth2Authentication(SettingsMain.this, Constants.dropboxKey);
					}
					else {
						dropBoxBackupRestore(dropboxAuthRequested);
					}
				}
			};
			String[] buttonNames = new String[] {getString(R.string.Continue), getString(R.string.Cancel)};
			AlertDialog confirmRestoreDailog = DialogTools.confirmDialog(SettingsMain.this, cmdRestoreConfirm,
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
			else if (requestCode == Constants.RequestDialogForReceiptFolder) {
				Tools.setPreference(getBaseContext(), R.string.receiptFolderKey, FileExplorer.filePath, false);
				Constants.receiptDirectory = FileExplorer.filePath;
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
			//	setEmailPrefrence(SettingsMain.this);
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
		if (SettingsMain.languageChanged)
			restartActivity();
	}
	
	void backupDB(String fileName) {
		File file = new File(Constants.backupDirectory);
		if (file.exists()) {
			BackupDatabaseFileTask backupDBTask = new BackupDatabaseFileTask(getBaseContext(), fileName);
			backupDBTask.execute("");
		}
	}

	private void restartActivity() {
		Intent intent = getIntent();
		finish();
		startActivity(intent);
	}
	
	/*void setEmailPrefrence(final Context context) {
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
	}*/
	
	void enablePasswordButtons() {
		if (!btPassword.isEnabled()) {
			btPassword.setEnabled(true);
			checkBoxAskPassword.setEnabled(true);
		}
	}
	
	void refreshBackupFolderNames() {
        btBackupFolder.setSummary(Constants.backupDirectory);
        btReceiptsFolder.setSummary(Constants.receiptDirectory);
		checkBoxAutoBackup.setSummary(getResources().getString(R.string.autoBackupSummary) + " " + 
				Constants.backupDirectory);
		btBackup.setSummary(getResources().getString(R.string.btBackupSummary) + " " + Constants.backupDirectory);
	}
}