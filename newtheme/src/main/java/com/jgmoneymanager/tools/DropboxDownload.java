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
import android.widget.Toast;

import com.cloudrail.si.interfaces.CloudStorage;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.AccountTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.main.MainScreen;
import com.jgmoneymanager.main.SettingsMain;
import com.jgmoneymanager.mmlibrary.R;

import org.jcodec.common.IOUtils;

/**
 * Here we show getting metadata for a directory and downloading a file in a
 * background thread, trying to show typical exception handling and flow of
 * control for an app that downloads a file from Dropbox.
 */

public class DropboxDownload extends AsyncTask<Void, Long, Boolean> {


    private Context mContext;
    private final ProgressDialog mDialog;
    private CloudStorage mDropbox;
    private String mPath;
    private File mFile;
    private long mFileLen;

    //private boolean mCanceled;
    private String mErrorMsg;

    MainScreen mMainsScreen = null;

    FileOutputStream fos;

    String revision;
    private ProgressInputStream.ProgressListener mProgressListener;

    // Note that, since we use a single file name here for simplicity, you
    // won't be able to use this code for two simultaneous downloads.
    //private final static String fileName = MoneyManagerProviderMetaData.DATABASE_NAME;

    public DropboxDownload(Context context, CloudStorage dropbox, String dropboxPath, File file, MainScreen mainScreen) {
        // We set the context this way so we don't accidentally leak activities
        mContext = context.getApplicationContext();
        mFileLen = file.length();

        mPath = dropboxPath;
        mFile = file;
        mDropbox = dropbox;

        mMainsScreen = mainScreen;

        mDialog = new ProgressDialog(context);
        mDialog.setMax(100);
        mDialog.setMessage(context.getString(R.string.msgDropboxSnycing));
        mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mDialog.setProgress(0);
        mProgressListener = new ProgressInputStream.ProgressListener() {
            @Override
            public void onProgressChanged(long bytes) {
                int percent = (int)(100.0*(double)bytes/mFileLen + 0.5);
                mDialog.setProgress(percent);
            }
        };
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
        mDropbox.login();
        ProgressInputStream result = new ProgressInputStream(mDropbox.download("/" + mFile.getName()), mProgressListener);
        try {
            IOUtils.copy(result, fos);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
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

            try {
                Tools.copyFile(tempFile, mFile);
            } catch (IOException e) {
                return false;
            }
        }

        if (result == null) {
            mErrorMsg = mContext.getString(R.string.noBackup);
            return false;
        }

        // TODO dropbox comment
        //revision = mDropbox.get;
        // We must have a legitimate picture
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
            //DialogTools.toastDialog(mContext, "The downloaded file's rev is: " + revision, Toast.LENGTH_LONG);
            Tools.setPreference(mContext, R.string.dropboxBackupRevisonKey, revision, false);

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
