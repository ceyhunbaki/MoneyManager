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

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxFileSizeException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
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

    private DropboxAPI<?> mApi;
    private String mPath;
    private String mCurrRevision;
    private File mFile;

    private Context mContext;
    private Context mGivenContext;

    private String mErrorMsg;
    String newRevision;
    
    private boolean backupDeleted = false;
    
    MainScreen mMainScreen;

    public DropboxCheckRevisionForDownload(Context context, DropboxAPI<?> api, String dropboxPath, File file, String currRevision, MainScreen mainScreen) {
        // We set the context this way so we don't accidentally leak activities
        mContext = context.getApplicationContext();
        mGivenContext = context;

        mCurrRevision = currRevision;
        mApi = api;
        mPath = dropboxPath;
        mFile = file;
        
        mMainScreen = mainScreen;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {            
            String path = mPath + mFile.getName();
            Entry existingEntry = mApi.metadata(path, 1, null, false, null);
            if ((existingEntry.bytes == 0) || (existingEntry.isDeleted)) {
            	backupDeleted = true;
            	return false;
            }
            else {
            	//Date tt = Tools.StringToDate(existingEntry.modified, Constants.DateFormatDropboxRevision);
            	newRevision = existingEntry.modified;
            	return (!mCurrRevision.equals(newRevision));
            }

        } catch (DropboxUnlinkedException e) {
            // This session wasn't authenticated properly or user unlinked
            mErrorMsg = mContext.getString(R.string.autentificateError);
        } catch (DropboxFileSizeException e) {
            // File size too big to upload via the API
            mErrorMsg = mContext.getString(R.string.fileBigError);
        } catch (DropboxPartialFileException e) {
            // We canceled the operation
            mErrorMsg = mContext.getString(R.string.canceled);
        } catch (DropboxServerException e) {
            // Server-side exception.  These are examples of what could happen,
            // but we don't do anything special with them here.
            if (e.error == DropboxServerException._401_UNAUTHORIZED) {
                // Unauthorized, so we should unlink them.  You may want to
                // automatically log the user out in this case.
            } else if (e.error == DropboxServerException._403_FORBIDDEN) {
                // Not allowed to access this
            } else if (e.error == DropboxServerException._404_NOT_FOUND) {
                // path not found (or if it was the thumbnail, can't be
                // thumbnailed)
            } else if (e.error == DropboxServerException._507_INSUFFICIENT_STORAGE) {
                // user is over quota
            } else {
                // Something else
            }
            // This gets the Dropbox error, translated into the user's language
            mErrorMsg = e.body.userError;
            if (mErrorMsg == null) {
                mErrorMsg = e.body.error;
            }
        } catch (DropboxIOException e) {
            // Happens all the time, probably want to retry automatically.
            mErrorMsg = mContext.getString(R.string.canceled);
        } catch (DropboxParseException e) {
            // Probably due to Dropbox server restarting, should retry
            mErrorMsg = mContext.getString(R.string.dropboxError);
        } catch (DropboxException e) {
            // Unknown error
            mErrorMsg = mContext.getString(R.string.unknownError);
        } 
        return false;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (result) {
            DropboxDownload dDownload = new DropboxDownload(mGivenContext, mApi, "", mFile, mMainScreen);
            dDownload.execute();
        } 
        else if (backupDeleted)
        	DialogTools.toastDialog(mContext, R.string.msgDropboxBackupDeleted, Toast.LENGTH_LONG);
    }
}
