package com.jgmoneymanager.services;

import android.content.Context;
import android.util.Log;

import com.cloudrail.si.interfaces.CloudStorage;
import com.jgmoneymanager.mmlibrary.R;
import com.jgmoneymanager.tools.ProgressInputStream;
import com.jgmoneymanager.tools.Tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DropboxSrvLocal {
    public static String Upload(Context context, String mPath, File mFile,
                                StringBuilder revision, CloudStorage service, ProgressInputStream.ProgressListener progressListener) {
        String mErrorMsg = "";
        try {
            service.login();

            InputStream inputStream = new FileInputStream(mFile);
            ProgressInputStream progressInputStream = new ProgressInputStream(inputStream, progressListener);
            Log.i("Servis", "Before upload");
            service.upload(
                    "/"+mFile.getName(),
                    progressInputStream,
                    1024L,
                    true
            );
            Tools.setPreference(context, R.string.dropboxTokenKey, service.saveAsString().toString(), false);
            Log.i("Servis", "After upload");
            Log.i("Servis - PATH", mPath);
            //revision.append(service.getMetadata(mFile.getName()));
            Log.i("Servis", "After revision");
        } catch (IOException e) {
            // We canceled the operation
            mErrorMsg = context.getString(R.string.canceled);
        }
        return mErrorMsg;
    }
}
