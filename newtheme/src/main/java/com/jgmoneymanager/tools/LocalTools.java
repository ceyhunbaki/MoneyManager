package com.jgmoneymanager.tools;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;
import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.ServerManagedPolicy;
import com.jgmoneymanager.budget.BudgetNewMonthTask;
import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProvider;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.main.FileExplorer;
import com.jgmoneymanager.main.MainScreen;
import com.jgmoneymanager.main.R;
import com.jgmoneymanager.main.RPTransactionEdit;
import com.jgmoneymanager.main.SettingsLanguage;
import com.jgmoneymanager.services.CurrencySrv;
import com.jgmoneymanager.services.DebtsSrv;
import com.jgmoneymanager.services.PaymentMethodsSrv;
import com.jgmoneymanager.services.RPTransactionSrv;
import com.jgmoneymanager.services.TransferSrv;

import java.io.File;
import java.io.IOException;

public class LocalTools {
	
	public static void startupActions(Context context) {
        if (CurrencySrv.getDefaultCurrencyID(context) != 0) {
            CurrencySrv.refreshDefaultCurrency(context);
            LocalTools.autoBackup(context);
            TransferSrv.controlTransfers(context);
            RPTransactionEdit.generateRPTransNotification(context);
            DebtsSrv.generateUnpaidDebtsNotification(context);
            RPTransactionSrv.controlRPTransactions(context);
            controlDropBoxRevision(context);
            BudgetNewMonthTask budgetNewMonthTask = new BudgetNewMonthTask(context);
            budgetNewMonthTask.execute();
        }
	}
	
	public static void controlDropBoxRevision(Context context) {
		if (!Tools.getPreference(context, R.string.dropboxTokenKey).equals("null") && Tools.isInternetAvailable(context)) {
			AppKeyPair appKeys = new AppKeyPair(Constants.dropboxKey, Constants.dropboxSecret);
	        AndroidAuthSession session = new AndroidAuthSession(appKeys, Constants.dropboxAccessType);
	        DropboxAPI<AndroidAuthSession> mDBApi = new DropboxAPI<AndroidAuthSession>(session);
			mDBApi.getSession().setOAuth2AccessToken(Tools.getPreference(context, R.string.dropboxTokenKey));
			File file = new File(Environment.getDataDirectory() + "/data/com.jgmoneymanager.main/databases/"
					+ MoneyManagerProviderMetaData.DATABASE_NAME);
			MainScreen ms = null;
			if (context.getClass() == MainScreen.class)
				ms = (MainScreen) context;
			DropboxCheckRevisionForDownload dUpload = new DropboxCheckRevisionForDownload(context, mDBApi, "/", file, Tools.getPreference(context, R.string.dropboxBackupRevisonKey), ms);
			dUpload.execute();
		}
	}

	public static void onResumeEvents(Context context) {
		if (Constants.defaultCurrency == -1) {
			Tools.loadSettings(context);
			startupActions(context);
		}
	}

    public static void showWhatsNewDialog(Context context) {
        if (!Tools.isFirstLaunch(context)) {
            {
                int newVersionCode = 0;
                try {
                    PackageInfo packInfo = context.getPackageManager().getPackageInfo("com.jgmoneymanager.main", 0);
                    newVersionCode = packInfo.versionCode;
                } catch (PackageManager.NameNotFoundException e) {
                    Tracker myTracker = EasyTracker.getInstance(context);
                    myTracker.set(Fields.SCREEN_NAME, "loadSettings- Get versionCode");
                    myTracker.send(MapBuilder.createAppView().build());
                }

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                int oldVersion = prefs.getInt(context.getResources().getString(R.string.oldversionkey), 33);
                if (oldVersion < newVersionCode) {
                    Tools.loadLanguage(context, null);
                    String message = "";
                    for (int i = oldVersion+1; i <= newVersionCode; i++)
                        switch (i) {
                            case 34:
                                message += "v 2.1 \n" + context.getResources().getString(R.string.v2_1);
                                break;
                            case 36:
                                message += "\n v 2.2 \n" + context.getResources().getString(R.string.v2_2);
                                break;
                            case 37:
                                message += "\n v 2.2.1 \n" + context.getResources().getString(R.string.v2_2_1);
                                break;
                            case 38:
                                message += "\n v 2.2.2";
                                try {
                                    if (Tools.isPreferenceAvialable(context, R.string.setPasswordKey)) {
                                        message += "\n" + context.getResources().getString(R.string.msgSetPasswordAgain).toUpperCase();
                                    }
                                    Tools.removePreferense(context, R.string.setPasswordKey);
                                    Tools.removePreferense(context, R.string.securityAnswerKey);
                                    Tools.removePreferense(context, R.string.securityQuestionKey);
                                    Tools.removePreferense(context, R.string.askpasswordkey);
                                    //Tools.removePreferense(context, R.string.emailKey);
                                } catch (Exception e) {
                                }
                                message += "\n" + context.getResources().getString(R.string.v2_2_2);
                                break;
                            case 39:
                                message += "\n v 2.2.3 \n" + context.getResources().getString(R.string.v2_2_3);
                                break;
                            case 40:
                                message += "\n v 2.2.4 \n" + context.getResources().getString(R.string.v2_2_4);
                                break;
                            case 45:
                                message += "\n v 3.0 \n" + context.getResources().getString(R.string.v3_0);
                                break;
                            case 46:
                                message += "\n v 3.0.1 \n" + context.getResources().getString(R.string.v3_0_1);
                                break;
                            case 48:
                                message += "\n v 3.1.0 \n" + context.getResources().getString(R.string.v3_1_0);
                                break;
                            case 53:
                                message += "\n v 3.1.2 \n" + context.getResources().getString(R.string.v3_1_2);
                                break;
                            case 54:
                                try {
                                    if (Tools.isPreferenceAvialable(context, R.string.setPasswordKey)) {
                                        DialogTools.toastDialog(context, R.string.msgPasswordReset, Toast.LENGTH_LONG);
                                    }
                                    Tools.removePreferense(context, R.string.setPasswordKey);
                                    Tools.removePreferense(context, R.string.askpasswordkey);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                message += "\n v 3.2.0 \n" + context.getResources().getString(R.string.v3_2);
                                break;
                            case 55:
                                message += "\n v 3.3.0 \n" + context.getResources().getString(R.string.v3_3);
                                break;
                            case 56:
                                message += "\n v 3.3.1 \n" + context.getResources().getString(R.string.v3_3_1);
                                break;
                            case 57:
                                message += "\n v 3.3.2 \n" + context.getResources().getString(R.string.v3_3_2);
                                break;
                            case 58:
                                message += "\n v 3.3.3 \n" + context.getResources().getString(R.string.v3_3_3);
                                break;
                            case 59:
                                message += "\n v 3.3.4 \n" + context.getResources().getString(R.string.v3_3_4);
                                break;
                            case 62:
                                message += "\n v 3.3.6 \n" + context.getResources().getString(R.string.v3_3_6);
                                break;
                            case 66:
                                PaymentMethodsSrv.controlFirstItems(context);
                                break;
                            case 67:
                                message += "\n v 3.4.1 \n" + context.getResources().getString(R.string.v3_4_1);
                                break;
                            case 68:
                                message += "\n v 3.5.0 \n" + context.getResources().getString(R.string.v3_5_0);
                                break;
                            case 69:
                                message += "\n v 3.5.1 \n" + context.getResources().getString(R.string.v3_5_1);
                                break;
                            case 70:
                                message += "\n v 3.5.2 \n" + context.getResources().getString(R.string.v3_5_2);
                                break;
                            case 71:
                                message += "\n v 3.5.3 \n" + context.getResources().getString(R.string.v3_5_3);
                                break;
                            case 72:
                                message += "\n v 3.5.4 \n" + context.getResources().getString(R.string.v3_5_4);
                                break;
                            case 74:
                                message += "\n v 3.5.6 \n" + context.getResources().getString(R.string.v3_5_6);
                                break;
                            case 75:
                                message += "\n v 3.6.0 \n" + context.getResources().getString(R.string.v3_6_0);
                                break;
                            case 76:
                                Tools.resetFormats(context);
                                message += "\n v 3.6.1 \n" + context.getResources().getString(R.string.v3_6_1);
                                break;
                            case 78:
                                message += "\n v 3.6.3 \n" + context.getResources().getString(R.string.v3_6_3);
                                break;
                            case 80:
                                DBTools.execQuery(context, "Drop view if exists " + MoneyManagerProviderMetaData.VTransAccountViewMetaData.VIEW_NAME);
                                DBTools.execQuery(context,
                                        MoneyManagerProvider.DatabaseHelper.DATABASE_CREATE_VIEW_VTRANSACCOUNTS.replace("'ALL'",
                                                "'" + context.getResources().getString(R.string.totalAccount) + "'"));
                                break;
                            default:
                                break;
                        }
                    if (message.length() > 0) {
                        AlertDialog dialog = DialogTools.informationDialog(context, R.string.whatsnew, message);
                        dialog.show();
                    }
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt(context.getString(R.string.oldversionkey), newVersionCode);
                    editor.apply();
                }
            }
        }
    }

    public static void fillSpinner(Spinner spinner, Context context, Cursor cursor, String columnName) {
        String[] from = new String[] { columnName, columnName };
        int[] to = new int[] { R.id.spinItem, R.id.spinneritem };
        SimpleCursorAdapter mAdapter = new SimpleCursorAdapter(context, R.layout.simple_spinner_item, cursor, from, to);
        mAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(mAdapter);
    }

    public static void autoBackup(final Context context) {
        if (Constants.autoBackupDate.compareTo(Tools.getCurrentDate()) < 0) {
            if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(com.jgmoneymanager.mmlibrary.R.string.autoBackupKey), true)) {
                File dbFile = new File(Environment.getDataDirectory() + "/data/com.jgmoneymanager.main/databases/"
                        + MoneyManagerProviderMetaData.DATABASE_NAME);

                File exportDir = new File(Constants.backupDirectory);
                if (!exportDir.exists()) {
                    if (!exportDir.mkdirs()) {
                        LayoutInflater li = LayoutInflater.from(context);
                        final View dialogView = li.inflate(R.layout.custom_3button_dialog, null);

                        TextView tvTitle = (TextView) dialogView.findViewById(R.id.dmTvTitle);
                        tvTitle.setText(R.string.msgSetBackupFolderDialog);

                        final AlertDialog warningDialog = DialogTools.CustomDialog(context, dialogView);

                        Command disableCommand = new Command() {
                            @Override
                            public void execute() {
                                Tools.setPreference(context, R.string.autoBackupKey, false);
                                warningDialog.dismiss();
                            }
                        };

                        Command laterCommand = new Command() {
                            @Override
                            public void execute() {
                                warningDialog.dismiss();
                            }
                        };

                        Command setNowCommand = new Command() {
                            @Override
                            public void execute() {
                                warningDialog.dismiss();
                                Intent intent = new Intent(context, FileExplorer.class);
                                intent.setAction(Constants.ActionViewFolders);
                                Bundle bundle = new Bundle();
                                bundle.putString(Constants.title, context.getResources().getString(R.string.msgChooseFolder));
                                bundle.putInt(Constants.dialogType, FileExplorer.DialogOpenFolderID);
                                bundle.putString(Constants.folderKey, Constants.backupDirectory);
                                intent.putExtras(bundle);
                                context.startActivity(intent);
                            }
                        };

                        DialogTools.setButtonActions(dialogView, R.id.dmBtDisable, disableCommand);
                        DialogTools.setButtonActions(dialogView, R.id.dmBtLater, laterCommand);
                        DialogTools.setButtonActions(dialogView, R.id.dmBtSet, setNowCommand);
                        warningDialog.show();
                    }
                }
                File file = new File(exportDir, Tools.DateToString(Tools.getCurrentDate(), Constants.DateFormatBackupAuto));
                //File file = new File(exportDir, Constants.backupFileName + Tools.DateToString(Tools.getCurrentDateTime(), Constants.DateFormatDB));
                if (!file.exists())
                    try {
                        Tools.controlBkpFiles();
                        file.createNewFile();
                        Tools.copyFile(dbFile, file);
                    } catch (IOException e) {
                        DialogTools.toastDialog(context,
                                context.getString(com.jgmoneymanager.mmlibrary.R.string.msgBackupFailed),
                                Toast.LENGTH_SHORT);
                        Log.e("MoneyManager.backup", e.getMessage(), e);
                    }
            }
            Constants.autoBackupDate = Tools.getCurrentDate();
        }
    }

    public static boolean proVersionExists(final Context context) {
        final boolean[] isLicensed = {false};
        final String BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAumkuedRktfc6n9KVQvFabohZZ2bBG0BgQTqeUixdanhe1Y/FfRY9F/g++0AzROQIYmW1jfg0UroSC5UPVDpogzAK/xDhoIHBXXEpTQdsIj3gnFr0eHZczmwoZBcq3oDAxxkSU0tzjviNDQy0X3m8xUX8DYnjNAovP0vckzXPr6pNO+cDPmGkxzzHAltoSWx52OKTo38q0MzmcqucAZn4Ae61iiDFSHfkem8ei/O6KQgg1di55aXUeMCIiGsOAX6jWO5p9t5ZendBqePhSYF0ZAKJ4mKqCyfddh2dZmJ/pv9HmGuxVN2lQydTeU/CC79bAlmXfBQAVhA7CQ2L18+uAwIDAQAB";
        final byte[] SALT = new byte[]{-46, 65, 30, -23, -103, -57, 74, -64, 51, 21, -95, -45, 77, -117, -55, -105, -11, 32, -64, 89};
        LicenseCheckerCallback mLicenseCheckerCallback;
        LicenseChecker mChecker;

        // Try to use more data here. ANDROID_ID is a single point of attack.
        String deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        // Construct the LicenseChecker with a policy.
        mChecker = new LicenseChecker(
                context, new ServerManagedPolicy(context,
                new AESObfuscator(SALT, "com.jgmoneymanager.pro", deviceId)),
                BASE64_PUBLIC_KEY);
        mChecker.checkAccess(new LicenseCheckerCallback() {
            @Override
            public void allow(int reason) {
                isLicensed[0] = true;
                DialogTools.toastDialog(context, "allowed", Toast.LENGTH_LONG);
            }

            @Override
            public void dontAllow(int reason) {
                isLicensed[0] = false;
                DialogTools.toastDialog(context, "don't allowed", Toast.LENGTH_LONG);
            }

            @Override
            public void applicationError(int errorCode) {
                isLicensed[0] = false;
                DialogTools.toastDialog(context, "error", Toast.LENGTH_LONG);
            }
        });
        return isLicensed[0];
    }
}
