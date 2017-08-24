package com.jgmoneymanager.tools;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.jgmoneymanager.database.MoneyManagerProviderMetaData;
import com.jgmoneymanager.mmlibrary.R;

import java.io.File;
import java.io.IOException;

public class BackupDatabaseFileTask extends AsyncTask<String, Void, Boolean> {
    private final Context ctx;
    private final String fileName;

	public BackupDatabaseFileTask(Context context, String fileName) {
		ctx = context;
		this.fileName = fileName;
	}

	// can use UI thread here
	protected void onPreExecute() {
		// this.dialog.setMessage("Exporting database...");
		// this.dialog.show();
	}

	// automatically done on worker thread (separate from UI thread)
	protected Boolean doInBackground(final String... args) {

		final File dbFile = new File(Environment.getDataDirectory()
				/*+ "/data/com.jgmoneymanager.pro/databases/"*/
                + "/data/" + ctx.getPackageName() + "/databases/"
				+ MoneyManagerProviderMetaData.DATABASE_NAME);

        String revision = Tools.LongDateToString(dbFile.lastModified(), "dd.MM.yyyy HH:mm:ss");

       File exportDir = new File(Constants.backupDirectory);
       if (!exportDir.exists()) {
          exportDir.mkdirs();
       }
       File file = new File(exportDir, fileName);

       try {
          file.createNewFile();
          Tools.copyFile(dbFile, file);
          return true;
       } catch (IOException e) {
          Log.e("MoneyManager.backup", e.getMessage(), e);
          return false;
       }
    }

    // can use UI thread here
    protected void onPostExecute(final Boolean success) {
       //if (this.dialog.isShowing()) {
       //   this.dialog.dismiss();
       //}
       if (success) {
          Toast.makeText(ctx, R.string.msgBackupSuccessful, Toast.LENGTH_SHORT).show();
       } else {
          Toast.makeText(ctx, R.string.msgBackupFailed, Toast.LENGTH_SHORT).show();
       }
    }

}