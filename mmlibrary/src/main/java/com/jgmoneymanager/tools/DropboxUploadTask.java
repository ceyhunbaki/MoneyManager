package com.jgmoneymanager.tools;

import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

//import com.cloudrail.si.interfaces.CloudStorage;
//import com.cloudrail.si.services.Dropbox;
import com.dropbox.core.v2.DbxClientV2;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.mmlibrary.R;
import com.jgmoneymanager.services.DropboxSrv;

import java.io.File;

/**
 * Here we show uploading a file in a background thread, trying to show
 * typical exception handling and flow of control for an app that uploads a
 * file from Dropbox.
 */
public class DropboxUploadTask extends AsyncTask<Void, Long, Boolean> {

    private DbxClientV2 mService;
    private File mFile;
    private boolean mShowProgressDialog;

    private long mFileLen;
    //private UploadRequest mRequest;
    private Context mContext;
    private ProgressDialog mDialog = null;

    private String mErrorMsg;
    private StringBuilder revision = new StringBuilder();

    private NotificationManager mNotificationManager;

    public DropboxUploadTask(Context context, DbxClientV2 service, File file, Boolean showProgressDialog, NotificationManager notificationManager) {
        // We set the context this way so we don't accidentally leak activities
        mContext = context.getApplicationContext();

        mFileLen = file.length();
        mService = service;
        mFile = file;
        mShowProgressDialog = showProgressDialog;
        mNotificationManager = notificationManager;

        if (mShowProgressDialog) {
            mDialog = new ProgressDialog(context);
            mDialog.setMax(100);
            mDialog.setMessage(context.getString(R.string.uploading));
            mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mDialog.setProgress(0);
            mDialog.show();
        }
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        if (Tools.isInternetAvailable(mContext/*, Tools.getPreferenceBool(mContext, R.string.dropboxAutoSyncWiFiKey, false)*/))
            mErrorMsg = DropboxSrv.Upload(mContext, mFile, revision, mService, mDialog);
        else
            return false;
        return mErrorMsg.length() == 0;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        try {
            mDialog.dismiss();
        }
        catch (Exception e) {

        }
        try {
            if (mNotificationManager != null)
                mNotificationManager.cancelAll();
        }
        catch (Exception e) {

        }
        if (result) {
            Tools.setPreference(mContext, R.string.dropboxBackupRevisonKey, revision.toString(), false);
            Tools.setPreference(mContext, com.jgmoneymanager.mmlibrary.R.string.dropboxBackupLocalRevisonKey, mFile.lastModified());
            if (mShowProgressDialog)
                DialogTools.toastDialog(mContext, R.string.msgBackupSuccessful, Toast.LENGTH_LONG);
        } else {
            if (mShowProgressDialog)
                DialogTools.toastDialog(mContext, mErrorMsg, Toast.LENGTH_LONG);
        }
    }
}
