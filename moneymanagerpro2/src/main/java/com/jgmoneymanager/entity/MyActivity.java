package com.jgmoneymanager.entity;

import android.app.ActionBar;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.analytics.tracking.android.EasyTracker;
import com.jgmoneymanager.dialogs.SetPassword;
import com.jgmoneymanager.dialogs.StartupPassword2;
import com.jgmoneymanager.paid.MainScreen;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.LocalTools;
import com.jgmoneymanager.tools.Tools;

public class MyActivity extends AppCompatActivity {

	public MyApplicationLocal myApp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Tools.loadLanguage(this, null);

		myApp = (MyApplicationLocal) getApplication();

		myApp.setPreviousActionIsPause(false);
		
		if ((this instanceof MainScreen) && SetPassword.passwordRequired(this)) {
			Intent intent = new Intent(this, StartupPassword2.class);
			intent.setAction(Constants.ActionStartupPassword);
			startActivityForResult(intent, Constants.RequestPasswordInStartup);
		}
		else if (!(this instanceof MainScreen))
			myApp.setAskPassword(false);

		try{

			ActivityInfo activityInfo = getPackageManager().getActivityInfo(getComponentName(),
					PackageManager.GET_META_DATA);

			ActionBar actionBar = getActionBar();

			if(actionBar != null)
				actionBar.setTitle(activityInfo.labelRes);

		}catch (PackageManager.NameNotFoundException ex){

			Log.e(this.getClass().getSimpleName(),
					"Error while getting activity info. " + ex.getMessage(), ex);

		}
 	}

	@Override
	protected void onStart() {
		//applicationWillEnterForeground();

		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
		
		//myApp.setSomethingRunning(true);

		/*Intent intent = new Intent(this, DropboxAutoUpload.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);*/
	}

	@Override
	protected void onStop() {
		super.onStop();
		EasyTracker.getInstance(this).activityStop(this);
		if (myApp.getPreviousActionIsPause())
			myApp.setAskPassword(true);

		//myApp.setSomethingRunning(false);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.i(this.getClass().getName(), "OnPause");
		myApp.setPreviousActionIsPause(true);

		/*try {
			unbindService(mConnection);
		} catch (Exception e) {
			Log.e("onStop - unbind", e.getMessage());
		}*/
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(this.getClass().getName(), "OnResume");
		myApp.setPreviousActionIsPause(false);
		LocalTools.onResumeEvents(this);
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Log.i(this.getClass().getName(), "onRestart");
		
		if (this instanceof MainScreen) {					
			if (myApp.getFinishApplication()) {
				myApp.setAskPassword(true);
				finish();
			}
		}
		
		if (myApp.getAskPassword() && SetPassword.passwordRequired(this)) {
        	Intent intent = new Intent(this, StartupPassword2.class);
        	intent.setAction(Constants.ActionStartupPassword);
        	startActivityForResult(intent, Constants.RequestPasswordInStartup);
        }       
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i(this.getClass().getName(), "onDestroy");

		// Log.i(this.getClass().getName(), "onDestroy - false");
		if (this instanceof MainScreen) 
			myApp.setAskPassword(true);
		else 
			myApp.setAskPassword(false);			
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == Constants.RequestPasswordInStartup) {
			if (resultCode != RESULT_OK)
				finish();
			else 
				myApp.setAskPassword(false);
		}
		
		myApp.setAskPassword(false);

		if (myApp.getFinishApplication()) {
			if (this instanceof MainScreen) {
				myApp.setFinishApplication(false);
				myApp.setAskPassword(true);
				moveTaskToBack(true);
			}
			finish();				
		}
	}

	@Override
	public void openOptionsMenu() {
		Configuration config = getResources().getConfiguration();

		if ((config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) > Configuration.SCREENLAYOUT_SIZE_LARGE) {
			int originalScreenLayout = config.screenLayout;
			config.screenLayout = Configuration.SCREENLAYOUT_SIZE_LARGE;
			super.openOptionsMenu();
			config.screenLayout = originalScreenLayout;

		} else {
			super.openOptionsMenu();
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		//myApp.setWindowFocused(hasFocus);

		if (myApp.isBackPressed() && !hasFocus) {
			myApp.setBackPressed(false);
			//myApp.setWindowFocused(true);
		}

		super.onWindowFocusChanged(hasFocus);
	}

	/*private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder binder) {
			DropboxAutoUpload.MyBinder b = (DropboxAutoUpload.MyBinder) binder;
			dropboxAutoUpload = b.getService();
			// DialogTools.toastDialog(MainScreen.this, "Connected",
			// Toast.LENGTH_SHORT);
		}

		public void onServiceDisconnected(ComponentName className) {
			dropboxAutoUpload = null;
		}
	};*/

}
