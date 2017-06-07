package com.jgmoneymanager.entity;

import android.content.Intent;
import android.util.Log;

import com.jgmoneymanager.main.R;
import com.jgmoneymanager.services.DropboxUploadService;
import com.jgmoneymanager.tools.Tools;

public class MyApplicationLocal extends MyApplication {

	@Override
	public void setAskPassword(boolean askPassword) {
		super.askPassword = askPassword;
		Log.i("setAskPassword", String.valueOf(askPassword));

		if (Tools.getPreferenceBool(this, R.string.dropboxAutoSyncKey, false)) {
			try {
				Intent intent = new Intent(this, DropboxUploadService.class);
				if (askPassword && super.getPreviousActionIsPause()) {
					try {
						Log.i("DropboxUploadService", "STARTED");
						startService(intent);
					} catch (Exception e) {
					}
				} else {
					Log.i("DropboxUploadService", "STOPPED");
					stopService(intent);
				}
				;
			} catch (Exception e) {

			}
		}
	}
}
