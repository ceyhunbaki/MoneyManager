package com.jgmoneymanager.entity;

import android.content.Context;
import android.content.Intent;
import android.support.multidex.MultiDex;

import com.jgmoneymanager.main.R;
import com.jgmoneymanager.services.DropboxUploadService;
import com.jgmoneymanager.tools.Tools;

public class MyApplicationLocal extends MyApplication {

	@Override
	public void setAskPassword(boolean askPassword) {
		super.askPassword = askPassword;

		if (Tools.getPreferenceBool(this, R.string.dropboxAutoSyncKey, false)) {
			try {
				Intent intent = new Intent(this, DropboxUploadService.class);
				if (askPassword && super.getPreviousActionIsPause()) {
					if (Tools.isInternetAvailable(this, Tools.getPreferenceBool(this, com.jgmoneymanager.mmlibrary.R.string.dropboxAutoSyncWiFiKey, false)))
						try {
							startService(intent);
						} catch (Exception e) {
						}
				} else {
					stopService(intent);
				}
			} catch (Exception e) {

			}
		}
	}

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		MultiDex.install(this);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		/*new Instabug.Builder(this, "3f09072a48022205fe01c93130618bd7")
				.setInvocationEvent(InstabugInvocationEvent.SHAKE)
				.build();*/
	}
}
