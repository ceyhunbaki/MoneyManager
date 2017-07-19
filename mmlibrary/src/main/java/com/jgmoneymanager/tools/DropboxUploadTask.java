package com.jgmoneymanager.tools;

import java.io.File;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.view.WindowManager;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.ProgressListener;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.mmlibrary.R;
import com.jgmoneymanager.services.DropboxSrv;

/**
 * Here we show uploading a file in a background thread, trying to show
 * typical exception handling and flow of control for an app that uploads a
 * file from Dropbox.
 */
public class DropboxUploadTask extends AsyncTask<Void, Long, Boolean> {

    private DropboxAPI<?> mApi;
    private String mPath;
    private File mFile;
    private boolean mShowProgressDialog;

    private long mFileLen;
    //private UploadRequest mRequest;
    private Context mContext;
    private ProgressDialog mDialog = null;

    private String mErrorMsg;
    private StringBuilder revision = new StringBuilder();

    public DropboxUploadTask(Context context, DropboxAPI<?> api, String dropboxPath, File file, Boolean showProgressDialog) {
        // We set the context this way so we don't accidentally leak activities
        mContext = context.getApplicationContext();

        mFileLen = file.length();
        mApi = api;
        mPath = dropboxPath;
        mFile = file;
        mShowProgressDialog = showProgressDialog;

        if (mShowProgressDialog) {
            mDialog = new ProgressDialog(context);
            mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
	        mDialog.setMax(100);
	        mDialog.setMessage(context.getString(R.string.uploading));
	        mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	        mDialog.setProgress(0);
	        mDialog.show();
        }
    }

    @Override
    protected Boolean doInBackground(Void... params) {
    	ProgressListener progressListener = new ProgressListener() {
            @Override
            public long progressInterval() {
                // Update the progress bar every half-second or so
                return 500;
            }

            @Override
            public void onProgress(long bytes, long total) {
                publishProgress(bytes);
            }
        };
        if (Tools.isInternetAvailable(mContext, Tools.getPreferenceBool(mContext, R.string.dropboxAutoSyncWiFiKey, false)))
        	mErrorMsg = DropboxSrv.Upload(mContext, mPath, mFile, revision, mApi, progressListener);
        else
        	return false;
        return mErrorMsg.length() == 0;
    }

    @Override
    protected void onProgressUpdate(Long... progress) {
        int percent = (int)(100.0*(double)progress[0]/mFileLen + 0.5);
        if (mShowProgressDialog) 
        	mDialog.setProgress(percent);
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
            if (mShowProgressDialog)
        	    DialogTools.toastDialog(mContext, R.string.msgBackupSuccessful, Toast.LENGTH_LONG);
        } else {
            if (mShowProgressDialog)
            	DialogTools.toastDialog(mContext, mErrorMsg, Toast.LENGTH_LONG);
        }
    }
}
