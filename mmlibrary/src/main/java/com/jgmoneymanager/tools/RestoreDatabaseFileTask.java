package com.jgmoneymanager.tools;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.jgmoneymanager.database.MoneyManagerProviderMetaData;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData.AccountTableMetaData;
import com.jgmoneymanager.mmlibrary.R;
import com.jgmoneymanager.services.CurrencySrv;
import com.jgmoneymanager.services.RPTransactionSrv;
import com.jgmoneymanager.services.TransferSrv;

import java.io.File;
import java.io.IOException;

public class RestoreDatabaseFileTask extends AsyncTask<String, Void, Boolean> {
    private final Context ctx;
    private final String importFilePath;
    private boolean invalidDBFile = false;

	public RestoreDatabaseFileTask(Context context, String path) {
		ctx = context;
		importFilePath = path;
	}

	// private final ProgressDialog dialog = new ProgressDialog(ctx);

	// can use UI thread here
	protected void onPreExecute() {
		// this.dialog.setMessage("Exporting database...");
		// this.dialog.show();
	}

	// automatically done on worker thread (separate from UI thread)
	protected Boolean doInBackground(final String... args) {

		SQLiteDatabase checkDB = null;
		try {
			checkDB = SQLiteDatabase.openDatabase(importFilePath, null, SQLiteDatabase.OPEN_READONLY);
			checkDB.rawQuery("select * from " + AccountTableMetaData.TABLE_NAME, null);
			checkDB.close();
		} catch (SQLiteException e) {
			invalidDBFile = true;
			return false;
		}

		File dbFile = new File(importFilePath);

		/*File importDir = new File(Environment.getDataDirectory() + "/data/com.jgmoneymanager.pro/databases/");*/
		File importDir = new File(Environment.getDataDirectory() + "/data/" + ctx.getPackageName() + "/databases/");

		File file = new File(importDir, MoneyManagerProviderMetaData.DATABASE_NAME);

		try {
			file.createNewFile();
			Tools.copyFile(dbFile, file);
			return true;
		} catch (IOException e) {
			Log.e("MoneyManager.mypck", e.getMessage(), e);
			return false;
		}
	}

	// can use UI thread here
	protected void onPostExecute(final Boolean success) {
		// if (this.dialog.isShowing()) {
		// this.dialog.dismiss();
		// }
		if (success) {
			Toast.makeText(ctx, R.string.msgRestoreSuccessful, Toast.LENGTH_SHORT).show();
			CurrencySrv.refreshDefaultCurrency(ctx);
			TransferSrv.controlTransfers(ctx);
			RPTransactionSrv.controlRPTransactions(ctx);
		} else {
			if (invalidDBFile)
				Toast.makeText(ctx, R.string.msgInvalidRestoredFile, Toast.LENGTH_SHORT).show();
			else 
				Toast.makeText(ctx, R.string.msgRestoreFailed, Toast.LENGTH_SHORT).show();
		}
	}

}