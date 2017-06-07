package com.jgmoneymanager.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import android.content.res.Resources;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxFileSizeException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.jgmoneymanager.mmlibrary.R;

public class DropboxSrv {
	public static String Upload(String mPath, File mFile,
		    StringBuilder revision, DropboxAPI<?> mApi, ProgressListener progressListener) {
		String mErrorMsg = "";
		try {
            // By creating a request, we get a handle to the putFile operation,
            // so we can cancel it later if we want to
            FileInputStream fis = new FileInputStream(mFile);
            String path = mPath + mFile.getName();
            Entry response = mApi.putFileOverwrite(path, fis, mFile.length(), progressListener);
            revision.append(response.modified);
            return mErrorMsg;

        } catch (DropboxUnlinkedException e) {
            // This session wasn't authenticated properly or user unlinked
            mErrorMsg = Resources.getSystem().getString(R.string.autentificateError);
        } catch (DropboxFileSizeException e) {
            // File size too big to upload via the API
            mErrorMsg = Resources.getSystem().getString(R.string.fileBigError);
        } catch (DropboxPartialFileException e) {
            // We canceled the operation
            mErrorMsg = Resources.getSystem().getString(R.string.canceled);
        } catch (DropboxServerException e) {
            // Server-side exception.  These are examples of what could happen,
            // but we don't do anything special with them here.
            /*if (e.error == DropboxServerException._401_UNAUTHORIZED) {
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
            }*/
            // This gets the Dropbox error, translated into the user's language
            mErrorMsg = e.body.userError;
            if (mErrorMsg == null) {
                mErrorMsg = e.body.error;
            }
        } catch (DropboxIOException e) {
            // Happens all the time, probably want to retry automatically.
            mErrorMsg = Resources.getSystem().getString(R.string.canceled);
        } catch (DropboxParseException e) {
            // Probably due to Dropbox server restarting, should retry
            mErrorMsg = Resources.getSystem().getString(R.string.dropboxError);
        } catch (DropboxException e) {
            // Unknown error
            mErrorMsg = Resources.getSystem().getString(R.string.unknownError);
        } catch (FileNotFoundException e) {
            mErrorMsg = e.getMessage();
        }
		return mErrorMsg;
	}
}
