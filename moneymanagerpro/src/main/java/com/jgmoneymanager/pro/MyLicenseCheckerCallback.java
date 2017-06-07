package com.jgmoneymanager.pro;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Handler;
import android.widget.Toast;

import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.tools.Command;

public class MyLicenseCheckerCallback implements LicenseCheckerCallback {
	
	Activity activity;
	Handler mHandler;
	
    public static final String BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqlSyNTRCtyl6JXAGIiq9Jk2alsptajp7Rtyn1Lchqqx1N18ZkQdh2qgmgd1" +
            "XAk2Llf0i//3YI4XImbOFQqc3CNtIAlm24auEz2ak23q/BTs2Wvd5DK6BQ51Ks8w/QUtUgVpLSOKSWKBWLUXjuGq6st2ockzQ94okfj6zk+BaVMNADJL2qgVpgIN5huHcgGExLMwOQCz/c" +
            "jqiL7uENNPSP/TWnEOa3SmqXNwxsAiEii2201l16+28IMBnAXxvUtwfrQxrBf7FZ6V2cTzPwO05ASt6Wpf4qs9VA/uX8C7Miv6KdSmwwpIB9WQ0vWBb8JoEqDNMrHQfieJG7TcOsvYGtwI" +
            "DAQAB";
    // Generate your own 20 random bytes, and put them here.
    public static final byte[] SALT = new byte[] {
	     -70, 98, 36, -128, -113, -25, 62, -15, 24, 102, -94,
	     -49, 107, -97, -26, -100, -45, 21, -63, 53
	     };

    public MyLicenseCheckerCallback(Activity activity, Handler handler) {
		this.activity = activity;
		this.mHandler = handler;
	}

	@Override
	public void applicationError(int errorCode) {
        String result = String.format(activity.getString(R.string.application_error), errorCode);
        displayResult(result);
	}
	
	private void displayResult(final String result) {
        mHandler.post(new Runnable() {
            public void run() {
                //mStatusText.setText(result);
            	DialogTools.toastDialog(activity, result, Toast.LENGTH_LONG);
            	activity.setProgressBarIndeterminateVisibility(false);
                //mCheckLicenseButton.setEnabled(true);
            }
        });
    }

	@Override
	public void allow(int reason) {
        if (activity.isFinishing()) {
            // Don't update UI if Activity is finishing.
            return;
        }
        // Should allow user access.
        displayResult(activity.getString(R.string.allow));
	}

	@Override
	public void dontAllow(int reason) {
        if (activity.isFinishing()) {
            // Don't update UI if Activity is finishing.
            return;
        }
        //displayResult(activity.getString(R.string.dont_allow));
        Command cmd = new Command() {
			@Override
			public void execute() {
				activity.finish();
				android.os.Process.killProcess(android.os.Process.myPid()); 
			}
		};
        AlertDialog dialog = DialogTools.warningDialog(activity, R.string.msgWarning, cmd, "Application is not lisensed");
        dialog.show();
	}
}
