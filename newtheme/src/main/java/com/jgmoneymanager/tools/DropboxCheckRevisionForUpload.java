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

import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.mmlibrary.R;

import java.io.File;

public class DropboxCheckRevisionForUpload extends AsyncTask<Void, Long, Boolean> {

    private DropboxAPI<AndroidAuthSession> mApi;
    private File mFile;

    private Context mContext;
    //private Context mGivenContext;

    AlertDialog dialog;

    public DropboxCheckRevisionForUpload(Context context, File file) {
        // We set the context this way so we don't accidentally leak activities
        mContext = context.getApplicationContext();
        //mGivenContext = context;

        mFile = file;

        AppKeyPair appKeys = new AppKeyPair(Constants.dropboxKey, Constants.dropboxSecret);
        AndroidAuthSession session = new AndroidAuthSession(appKeys, Constants.dropboxAccessType);
        mApi = new DropboxAPI<AndroidAuthSession>(session);

        Command cmd = new Command() {
            @Override
            public void execute() {
                if (!Tools.getPreference(mContext, com.jgmoneymanager.main.R.string.dropboxTokenKey).equals("null")) {
                    mApi.getSession().setOAuth2AccessToken(Tools.getPreference(mContext, com.jgmoneymanager.main.R.string.dropboxTokenKey));
                    DropboxUploadTaskLocal dUpload = new DropboxUploadTaskLocal(mContext, mApi, "", mFile, true);
                    dUpload.execute();

                    Tools.setPreference(mContext, R.string.dropboxBackupLocalRevisonKey, mFile.lastModified());
                }
            }
        };
        dialog = DialogTools.confirmDialog(mContext, cmd, R.string.dropbox,
                "Upload?",
                new String[]{mContext.getResources().getString(R.string.menuRestore),
                        mContext.getResources().getString(R.string.Cancel)});

    }

    @Override
    protected Boolean doInBackground(Void... params) {
        long mOldLocalRevision;
        if (Tools.isPreferenceAvialable(mContext, R.string.dropboxBackupLocalRevisonKey))
            mOldLocalRevision = Tools.getPreferenceLong(mContext, R.string.dropboxBackupLocalRevisonKey);
        else {
            mOldLocalRevision = mFile.lastModified() - 1;
            Tools.setPreference(mContext, R.string.dropboxBackupLocalRevisonKey, mOldLocalRevision);
        }
        return (mFile.lastModified() > mOldLocalRevision);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (result) {
            dialog.show();
        }
    }
}
