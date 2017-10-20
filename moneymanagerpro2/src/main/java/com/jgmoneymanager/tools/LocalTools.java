package com.jgmoneymanager.tools;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.ServerManagedPolicy;
import com.jgmoneymanager.budget.BudgetNewMonthTask;
import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProvider;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.paid.FileExplorer;
import com.jgmoneymanager.paid.MainScreen;
import com.jgmoneymanager.paid.R;
import com.jgmoneymanager.paid.RPTransactionEdit;
import com.jgmoneymanager.services.CurrencySrv;
import com.jgmoneymanager.services.DebtsSrv;
import com.jgmoneymanager.services.PaymentMethodsSrv;
import com.jgmoneymanager.services.RPTransactionSrv;
import com.jgmoneymanager.services.TransferSrv;

import java.io.File;
import java.io.IOException;

public class LocalTools {
	
	public static void startupActions(Context context) {
        if (CurrencySrv.getDefaultCurrencyID(context, false) != 0) {
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
            File file = new File(Environment.getDataDirectory() + "/data/" + context.getPackageName() + "/databases/"
                    + MoneyManagerProviderMetaData.DATABASE_NAME);
            MainScreen ms = null;
            if (context.getClass() == MainScreen.class)
                ms = (MainScreen) context;
            DropboxCheckRevisionForDownload dUpload = new DropboxCheckRevisionForDownload(context, Tools.getDropboxClient(context), file, Tools.getPreference(context, R.string.dropboxBackupRevisonKey), ms);
            dUpload.execute();
		}
	}

    public static void onResumeEvents(Context context) {
        onResumeEvents(context, true);
    }

    /**
     *
     * @param context
     * @param doFirstLaunchActions if true then inserts initial database values
     */
    public static void onResumeEvents(Context context, boolean doFirstLaunchActions) {
        if (Constants.defaultCurrency == -1) {
            Tools.loadSettings(context, doFirstLaunchActions);
            startupActions(context);
        }
    }

    public static void showWhatsNewDialog(Context context) {
        if (!Tools.isFirstLaunch(context)) {
            {
                int newVersionCode = Tools.getVersionCode(context);

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                int oldVersion = prefs.getInt(context.getResources().getString(R.string.oldversionkey), 33);
                if (oldVersion < newVersionCode) {
                    Tools.loadLanguage(context, null);
                    String message = "";
                    for (int i = oldVersion+1; i <= newVersionCode; i++)
                        switch (i) {
                            case 2:
                                message += "v 1.0.1 \n" + context.getResources().getString(R.string.v1_0_1);
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
                File dbFile = new File(Environment.getDataDirectory() + "/data/" + context.getPackageName() + "/databases/"
                        + MoneyManagerProviderMetaData.DATABASE_NAME);

                File exportDir = new File(Constants.backupDirectory);

                int permissionCheck = ContextCompat.checkSelfPermission(context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (permissionCheck == PackageManager.PERMISSION_DENIED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        Command askPemissionCmd = new Command() {
                            @Override
                            public void execute() {
                                int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = PackageManager.PERMISSION_GRANTED;
                                ActivityCompat.requestPermissions((Activity) context,
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                            }
                        };
                        AlertDialog warningDialog = DialogTools.warningDialog(context, R.string.msgWarning,
                                askPemissionCmd, context.getString(R.string.msgAppNeedsPermission));
                        warningDialog.show();
                        // Show an explanation to the user *asynchronously* -- don't block
                        // this thread waiting for the user's response! After the user
                        // sees the explanation, try again to request the permission.

                    } else {
                        int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = PackageManager.PERMISSION_GRANTED;
                        ActivityCompat.requestPermissions((Activity) context,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                    }
                }
                else if (!exportDir.exists()) {
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
                                bundle.putString(FileExplorer.paramSelBackupFolder, "1");
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
                new AESObfuscator(SALT, "com.jgmoneymanager.paid", deviceId)),
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
