package com.jgmoneymanager.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

//import com.cloudrail.si.interfaces.CloudStorage;
import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.AccountTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.main.MainScreen;
import com.jgmoneymanager.main.SettingsMain;
import com.jgmoneymanager.mmlibrary.R;

/**
 * Here we show getting metadata for a directory and downloading a file in a
 * background thread, trying to show typical exception handling and flow of
 * control for an app that downloads a file from Dropbox.
 */

public class DropboxDownload extends AsyncTask<Void, Long, Boolean> {


    private Context mContext;
    private final ProgressDialog mDialog;
    private DbxClientV2 mDropbox;
    private File mFile;
    private long mFileLen;

    //private boolean mCanceled;
    private String mErrorMsg;

    MainScreen mMainsScreen = null;

    FileOutputStream fos;

    String revision;

    public DropboxDownload(Context context, DbxClientV2 dropbox, File file, MainScreen mainScreen) {
        mContext = context.getApplicationContext();
        mFileLen = file.length();

        mFile = file;
        mDropbox = dropbox;

        mMainsScreen = mainScreen;

        mDialog = new ProgressDialog(context);
        mDialog.setMax(100);
        mDialog.setMessage(context.getString(R.string.msgDropboxSnycing));
        mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mDialog.setProgress(0);
        mDialog.show();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        File tempFile = null;
        boolean tempFound = false;
        try {
            File tempDir = new File(Constants.backupDirectory + "/temp");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            tempFile = new File(tempDir, mFile.getName());

            fos = new FileOutputStream(tempFile);
            tempFound = true;
        } catch (FileNotFoundException e1) {
            try {
                fos = new FileOutputStream(mFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        DbxDownloader<FileMetadata> dbxDownloader = null;
        long size = 0;
        try {
            dbxDownloader = mDropbox.files().download("/"+mFile.getName());
            size = dbxDownloader.getResult().getSize();
        } catch (DbxException e) {
            e.printStackTrace();
        }

        try {
            dbxDownloader.download(new ProgressOutputStream(size, fos, new ProgressOutputStream.ProgressOutputListener() {
                public void progress(long completed, long totalSize) {
                    mDialog.setProgress(Math.round((100*completed)/totalSize));
                }
            }));
            revision = dbxDownloader.getResult().getRev();
        } catch (DbxException | IOException e) {
            e.printStackTrace();
            mErrorMsg = e.getMessage();
        }

        if (tempFound) {
            SQLiteDatabase checkDB;
            try {
                checkDB = SQLiteDatabase.openDatabase(tempFile.getPath(), null, SQLiteDatabase.OPEN_READONLY);
                checkDB.rawQuery("select * from " + AccountTableMetaData.TABLE_NAME, null);
                checkDB.close();
            } catch (SQLiteException e) {
                mErrorMsg = mContext.getResources().getString(R.string.msgInvalidRestoredFile);
                return false;
            }

            Log.i("DropboxDownload", "Before Copy");
            try {
                Tools.copyFile(tempFile, mFile);
            } catch (IOException e) {
                mErrorMsg = e.getMessage();
                return false;
            }
            Log.i("DropboxDownload", "After copy");
        }

//        if (result == null) {
//            mErrorMsg = mContext.getString(R.string.noBackup);
//            return false;
//        }

        return true;
    }

    @Override
    protected void onProgressUpdate(Long... progress) {
        int percent = (int) (100.0 * (double) progress[0] / mFileLen + 0.5);
        mDialog.setProgress(percent);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        mDialog.dismiss();
        if (result) {
            Log.i("DropboxDownload", "True Result");
            //DialogTools.toastDialog(mContext, "The downloaded file's rev is: " + revision, Toast.LENGTH_LONG);
            Tools.setPreference(mContext, R.string.dropboxBackupRevisonKey, revision, false);
            Tools.setPreference(mContext, R.string.dropboxBackupLocalRevisonKey, mFile.lastModified());

            //DialogTools.toastDialog(mContext, R.string.msgRestoreSuccessful, Toast.LENGTH_LONG);
            try {
                if (mMainsScreen != null) {
                    mMainsScreen.generateAccountsLayout();
                    mMainsScreen.setActiveAccountButton(mMainsScreen.selectedAccountID);
                    mMainsScreen.refreshAccountDetails();
                } else
                    SettingsMain.languageChanged = true;
            } catch (Exception e) {

            }
            try {
                LocalTools.startupActions(mContext);

            } catch (Exception e) {

            }
            // Set the image now that we have it
            //mView.setImageDrawable(mDrawable);
        } else {
            // Couldn't download it, so show an error
            DialogTools.toastDialog(mContext, mErrorMsg, Toast.LENGTH_LONG);
        }
    }
}
