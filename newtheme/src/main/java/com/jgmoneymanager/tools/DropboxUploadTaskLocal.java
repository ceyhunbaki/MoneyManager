package com.jgmoneymanager.tools;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Dropbox;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.mmlibrary.R;
import com.jgmoneymanager.services.DropboxSrvLocal;

import java.io.File;

/**
 * Here we show uploading a file in a background thread, trying to show
 * typical exception handling and flow of control for an app that uploads a
 * file from Dropbox.
 */
public class DropboxUploadTaskLocal extends AsyncTask<Void, Long, Boolean> {

    private CloudStorage mService;
    private String mPath;
    private File mFile;
    private boolean mShowProgressDialog;

    private long mFileLen;
    //private UploadRequest mRequest;
    private Context mContext;
    private ProgressDialog mDialog = null;

    private String mErrorMsg;
    private StringBuilder revision = new StringBuilder();

    private ProgressInputStream.ProgressListener mProgressListener;

    public DropboxUploadTaskLocal(Context context, CloudStorage service, String dropboxPath, File file, Boolean showProgressDialog) {
        // We set the context this way so we don't accidentally leak activities
        mContext = context.getApplicationContext();

        mFileLen = file.length();
        mService = service;
        mPath = dropboxPath;
        mFile = file;
        mShowProgressDialog = showProgressDialog;


        if (mShowProgressDialog) {
            mDialog = new ProgressDialog(context);
            //mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
	        mDialog.setMax(100);
	        mDialog.setMessage(context.getString(R.string.uploading));
	        mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	        mDialog.setProgress(0);
	        mDialog.show();
            mProgressListener = new ProgressInputStream.ProgressListener() {
                @Override
                public void onProgressChanged(long bytes) {
                    int percent = (int)(100.0*(double)bytes/mFileLen + 0.5);
                    if (mShowProgressDialog)
                        mDialog.setProgress(percent);
                }
            };
        }
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        if (Tools.isInternetAvailable(mContext/*, Tools.getPreferenceBool(mContext, R.string.dropboxAutoSyncWiFiKey, false)*/))
        	mErrorMsg = DropboxSrvLocal.Upload(mContext, mPath, mFile, revision, mService, mProgressListener);
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
        if (result) {
            //DialogTools.toastDialog(mContext, "The uploaded file's rev is: " + revision, Toast.LENGTH_LONG);
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
