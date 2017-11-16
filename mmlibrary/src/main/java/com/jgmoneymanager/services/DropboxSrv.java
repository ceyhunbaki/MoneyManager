package com.jgmoneymanager.services;

import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;

//import com.cloudrail.si.interfaces.CloudStorage;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxUploader;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;
import com.jgmoneymanager.mmlibrary.R;
import com.jgmoneymanager.tools.ProgressInputStream;
import com.jgmoneymanager.tools.Tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DropboxSrv {
    public static String Upload(Context context, final File mFile,
                                StringBuilder revision, DbxClientV2 service, final ProgressDialog mDialog) {
        String mErrorMsg = "";
        try {
//            service.login();

            InputStream inputStream = new FileInputStream(mFile);
            //ProgressInputStream progressInputStream = new ProgressInputStream(inputStream, progressListener);
            Log.i("Servis", "Before upload");
//            service.upload(
//                    "/"+mFile.getName(),
//                    progressInputStream,
//                    1024L,
//                    true
//            );
//            Tools.setPreference(context, R.string.dropboxTokenKey, service.saveAsString().toString(), false);
            try {
                //DbxUploader dbxUploader = service.files().uploadBuilder("/"+mFile.getName()).withMode(WriteMode.OVERWRITE);
                FileMetadata metadata = service.files().uploadBuilder("/"+mFile.getName()).withMode(WriteMode.OVERWRITE).uploadAndFinish(new ProgressInputStream(inputStream, new ProgressInputStream.ProgressInputListener() {
                    @Override
                    public void onProgressChanged(long bytes) {
                        if (mDialog != null)
                            mDialog.setProgress(Math.round((100*bytes)/mFile.length()));
                    }
                }));
//                FileMetadata metaData = service.files().uploadBuilder("/"+mFile.getName()).withMode(WriteMode.OVERWRITE)
//                        .uploadAndFinish(progressInputStream);
                revision.append(metadata.getRev());
            } catch (DbxException | IOException e) {
                mErrorMsg = context.getString(R.string.canceled) + ": " + e.getMessage();
            }
        } catch (IOException e) {
            mErrorMsg = context.getString(R.string.canceled);
        }
        return mErrorMsg;
    }
}
