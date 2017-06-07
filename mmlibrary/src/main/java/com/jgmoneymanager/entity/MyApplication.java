package com.jgmoneymanager.entity;

import android.app.Application;
import android.util.Log;

public class MyApplication extends Application {
	protected Boolean askPassword;

	protected Boolean previousActionIsPause;
    private Boolean finishApplication;

    //private boolean isWindowFocused = false;
    private boolean isBackPressed = false;

    private static boolean isMainDetailsChanged = false;
	
	//boolean somethingRunning = false;

	public boolean getAskPassword() {
		if (askPassword == null)
			askPassword = true;
		return askPassword;
	}

	public void setAskPassword(boolean askPassword) {
		if (! askPassword)
			setPreviousActionIsPause(false);
		this.askPassword = askPassword;
		Log.i("setAskPassword", String.valueOf(askPassword));
	}

	public Boolean getPreviousActionIsPause() {
		return previousActionIsPause;
	}

	public void setPreviousActionIsPause(Boolean previousActionIsPause) {
		Log.i("PreviousActionIsPause", String.valueOf(previousActionIsPause));
		this.previousActionIsPause = previousActionIsPause;
	}

	public boolean isBackPressed() {
		return isBackPressed;
	}

	public void setBackPressed(boolean isBackPressed) {
		this.isBackPressed = isBackPressed;
	}

	public Boolean getFinishApplication() {
		if (finishApplication == null)
			finishApplication = false;
		return finishApplication;
	}

	public void setFinishApplication(Boolean finishApplication) {
		this.finishApplication = finishApplication;
		Log.i("setFinishApplication", String.valueOf(finishApplication));
	}

	public boolean isMainDetailsChanged() {
		return isMainDetailsChanged;
	}
	public void setMainDetailsChanged(boolean value) {
		isMainDetailsChanged = value;
	}	
	public void refreshMainDetails() {
    	isMainDetailsChanged = true;
    }

	/*public boolean isSomethingRunning() {
		return somethingRunning;
	}
	public void setSomethingRunning(boolean somethingRunning) {
		this.somethingRunning = somethingRunning;
	}*/
}
