package com.jgmoneymanager.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DropboxFileInfo;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.AccountTableMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.main.MainScreen;
import com.jgmoneymanager.main.SettingsScreen;
import com.jgmoneymanager.mmlibrary.R;

/**
 * Here we show getting metadata for a directory and downloading a file in a
 * background thread, trying to show typical exception handling and flow of
 * control for an app that downloads a file from Dropbox.
 */

public class DropboxDownload extends AsyncTask<Void, Long, Boolean> {


    private Context mContext;
    private final ProgressDialog mDialog;
    private DropboxAPI<?> mApi;
    private String mPath;
    private File mFile;
    private long mFileLen;

    //private boolean mCanceled;
    private String mErrorMsg;
    
    MainScreen mMainsScreen = null;
    
    FileOutputStream fos;
    
    String revision;

    // Note that, since we use a single file name here for simplicity, you
    // won't be able to use this code for two simultaneous downloads.
    //private final static String fileName = MoneyManagerProviderMetaData.DATABASE_NAME;

    public DropboxDownload(Context context, DropboxAPI<?> api, String dropboxPath, File file, MainScreen mainScreen) {
        // We set the context this way so we don't accidentally leak activities
        mContext = context.getApplicationContext();
        mFileLen = file.length();
        
        mApi = api;
        mPath = dropboxPath;
        mFile = file;
        
        mMainsScreen = mainScreen;

        mDialog = new ProgressDialog(context);
        mDialog.setMax(100);
        mDialog.setMessage(context.getString(R.string.msgDropboxSnycing));
        mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mDialog.setProgress(0);
        /*mDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, context.getString(R.string.Cancel), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // This will cancel the putFile operation
                mCanceled = true;
            }
        });*/
        mDialog.show();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try { //TODO bura alinmadi
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
        	}
        	catch (FileNotFoundException e1) {
        		fos = new FileOutputStream(mFile);
        	}
            String path = mPath + mFile.getName();
            DropboxFileInfo info = mApi.getFile(path, null, fos, 
            	new ProgressListener() {
                @Override
                public long progressInterval() {
                    // Update the progress bar every half-second or so
                    return 500;
                }

                @Override
                public void onProgress(long bytes, long total) {
                    publishProgress(bytes);                    
                }
            });
            
            if (tempFound) {
            	SQLiteDatabase checkDB = null;
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

            if (info == null) {
                mErrorMsg = mContext.getString(R.string.noBackup);
                return false;
            }
        	
            revision = info.getMetadata().modified;
            // We must have a legitimate picture
            return true;

        } catch (DropboxUnlinkedException e) {
            // The AuthSession wasn't properly authenticated or user unlinked.
        } catch (DropboxPartialFileException e) {
            // We canceled the operation
            mErrorMsg = mContext.getString(R.string.canceled);
        } catch (DropboxServerException e) {
            // Server-side exception.  These are examples of what could happen,
            // but we don't do anything special with them here.
            /*if (e.error == DropboxServerException._304_NOT_MODIFIED) {
                // won't happen since we don't pass in revision with metadata
            } else if (e.error == DropboxServerException._401_UNAUTHORIZED) {
                // Unauthorized, so we should unlink them.  You may want to
                // automatically log the user out in this case.
            } else if (e.error == DropboxServerException._403_FORBIDDEN) {
                // Not allowed to access this
            } else if (e.error == DropboxServerException._404_NOT_FOUND) {
                // path not found (or if it was the thumbnail, can't be
                // thumbnailed)
            } else if (e.error == DropboxServerException._406_NOT_ACCEPTABLE) {
                // too many entries to return
            } else if (e.error == DropboxServerException._415_UNSUPPORTED_MEDIA) {
                // can't be thumbnailed
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
            mErrorMsg = mContext.getString(R.string.networkError);
        } catch (DropboxParseException e) {
            // Probably due to Dropbox server restarting, should retry
            mErrorMsg = mContext.getString(R.string.dropboxError);
        } catch (DropboxException e) {
            // Unknown error
            mErrorMsg = mContext.getString(R.string.unknownError);
        } catch (FileNotFoundException e) {
            mErrorMsg = mContext.getString(R.string.noBackup);
		}
        return false;
    }

    @Override
    protected void onProgressUpdate(Long... progress) {
        int percent = (int)(100.0*(double)progress[0]/mFileLen + 0.5);
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
            		mMainsScreen.refreshAccountDetails(mMainsScreen.selectedAccountID);
            	}
            	else
            		SettingsScreen.languageChanged = true;
            }
            catch (Exception e) {
            	
            }
            // Set the image now that we have it
            //mView.setImageDrawable(mDrawable);
        } else {
            // Couldn't download it, so show an error
        	DialogTools.toastDialog(mContext, mErrorMsg, Toast.LENGTH_LONG);
        }
    }
}
