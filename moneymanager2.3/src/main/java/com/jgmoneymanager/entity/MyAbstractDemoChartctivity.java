package com.jgmoneymanager.entity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.HorizontalScrollView;

import com.google.analytics.tracking.android.EasyTracker;
import com.jgmoneymanager.SlidingMenu.MyHorizontalScrollView;
import com.jgmoneymanager.dialogs.SetPassword;
import com.jgmoneymanager.dialogs.StartupPassword2;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.LocalTools;
import com.jgmoneymanager.tools.Tools;

import chart.AbstractDemoChart;

public class MyAbstractDemoChartctivity extends AbstractDemoChart {

	public MyApplicationLocal myApp;
    public static boolean menuOut = false;

	//DropboxAutoUpload dropboxAutoUpload;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Tools.loadLanguage(this, null);

		myApp = (MyApplicationLocal) getApplication();

		myApp.setPreviousActionIsPause(false);
		
		myApp.setAskPassword(false);
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
		myApp.setPreviousActionIsPause(false);
		LocalTools.onResumeEvents(this);
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		
		if (myApp.getAskPassword() && SetPassword.passwordRequired(this)) {
        	Intent intent = new Intent(this, StartupPassword2.class);
        	intent.setAction(Constants.ActionStartupPassword);
        	startActivityForResult(intent, Constants.RequestPasswordInStartup);
        }       
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

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

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getDesc() {
        return null;
    }

    @Override
    public Intent execute(Context context) {
        return null;
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

    /**
     * Helper for examples with a HSV that should be scrolled by a menu View's width.
     */
    public static class ClickListenerForScrolling implements View.OnClickListener {
        HorizontalScrollView scrollView;
        View menu;

        public ClickListenerForScrolling(HorizontalScrollView scrollView, View menu) {
            super();
            this.scrollView = scrollView;
            this.menu = menu;
        }

        @Override
        public void onClick(View v) {
            // Ensure menu is visible
            if (!menuOut) {
                scrollView.smoothScrollTo(0, 0);
                menu.setVisibility(View.VISIBLE);
            } else {
                scrollView.smoothScrollTo(menu.getMeasuredWidth(), 0);
                menu.setVisibility(View.INVISIBLE);
            }
            menuOut = !menuOut;
        }
    }

    /**
     * Helper that remembers the width of the 'slide' button, so that the 'slide' button remains in view, even when the menu is
     * showing.
     */
    public static class SizeCallbackForMenu implements MyHorizontalScrollView.SizeCallback {
        int btnWidth;
        View btnSlide;

        public SizeCallbackForMenu(View btnSlide) {
            super();
            this.btnSlide = btnSlide;
        }

        @Override
        public void onGlobalLayout() {
            btnWidth = btnSlide.getMeasuredWidth();
            System.out.println("btnWidth=" + btnWidth);
        }

        @Override
        public void getViewSize(int idx, int w, int h, int[] dims) {
            dims[0] = w;
            dims[1] = h;
            final int menuIdx = 0;
            if (idx == menuIdx) {
                dims[0] = w - btnWidth;
            }
        }
    }

}
