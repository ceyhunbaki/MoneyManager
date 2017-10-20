/*
 * Copyright (c) 2011 Dropbox, Inc.
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */


package com.jgmoneymanager.tools;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.main.MainScreen;
import com.jgmoneymanager.mmlibrary.R;

import java.io.File;

/**
 * Here we show uploading a file in a background thread, trying to show
 * typical exception handling and flow of control for an app that uploads a
 * file from Dropbox.
 */
public class DropboxCheckRevisionForDownload extends AsyncTask<Void, Long, Boolean> {

    private String mCurrRevision;
    private File mFile;

    private Context mContext;
    private Context mGivenContext;

    private String mErrorMsg;
    String newRevision;
    
    private boolean backupDeleted = false;
    
    MainScreen mMainScreen;
    DbxClientV2 mDropboxClient;

    public DropboxCheckRevisionForDownload(Context context, DbxClientV2 dropboxClient, File file, String currRevision, MainScreen mainScreen) {
        // We set the context this way so we don't accidentally leak activities
        mContext = context.getApplicationContext();
        mGivenContext = context;

        mCurrRevision = currRevision;
        mDropboxClient = dropboxClient;
        mFile = file;
        
        mMainScreen = mainScreen;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            StringBuilder revision = new StringBuilder();
            backupDeleted = !Tools.getDropboxRevision(mDropboxClient, mFile, revision);
            if (backupDeleted) {
                return false;
            }
            else {
                newRevision = revision.toString();
                return (!mCurrRevision.equals(newRevision));
            }
        } catch (DbxException e) {
            mErrorMsg = e.getMessage();
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (result) {
            try {
                DropboxDownload dDownload = new DropboxDownload(mGivenContext, mDropboxClient, mFile, mMainScreen);
                dDownload.execute();
            }
            catch (Exception e) {

            }
        } 
        else if (backupDeleted)
        	DialogTools.toastDialog(mContext, R.string.msgDropboxBackupDeleted, Toast.LENGTH_LONG);
        else if (mErrorMsg != null)
            DialogTools.toastDialog(mContext, mErrorMsg, Toast.LENGTH_LONG);
    }
}
