package com.jgmoneymanager.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData;
import com.jgmoneymanager.main.MainScreen;
import com.jgmoneymanager.main.R;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.DropboxUploadTaskLocal;
import com.jgmoneymanager.tools.Tools;

import java.io.File;

/**
 * Created by Ceyhun on 21/07/2016.
 */
public class DropboxUploadService extends Service {

    private NotificationManager notificationMgr;
    private ThreadGroup myThreads = new ThreadGroup("ServiceWorker");

    @Override
    public void onCreate() {
        super.onCreate();
        notificationMgr =(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        new Thread(myThreads, new ServiceWorker(this), getResources().getString(R.string.app_name)).start();

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        notificationMgr.cancelAll();
        myThreads.interrupt();
        super.onDestroy();
    }

    class ServiceWorker implements Runnable
    {
        Context mContext;
        ServiceWorker(Context context) {
            mContext = context;
        }

        public void run() {
            // do background processing here... we'll just sleep...
            try {
                long mOldLocalRevision = Tools.getPreferenceLong(mContext, com.jgmoneymanager.mmlibrary.R.string.dropboxBackupLocalRevisonKey);
                File file = new File(Environment.getDataDirectory() + "/data/" + mContext.getPackageName() + "/databases/"
                        + MoneyManagerProviderMetaData.DATABASE_NAME);
                if (mOldLocalRevision == file.lastModified()) {
                    stopSelf();
                }
                else {
                    Thread.sleep(10000);

                    if (!Tools.getPreference(mContext, com.jgmoneymanager.main.R.string.dropboxTokenKey).equals("null")
                            && Tools.isInternetAvailable(mContext)) {
                        displayNotificationMessage(getResources().getString(R.string.msgDropboxSnycing), mContext);

                        DropboxAPI<AndroidAuthSession> mApi;
                        AppKeyPair appKeys = new AppKeyPair(Constants.dropboxKey, Constants.dropboxSecret);
                        AndroidAuthSession session = new AndroidAuthSession(appKeys, Constants.dropboxAccessType);
                        mApi = new DropboxAPI<AndroidAuthSession>(session);

                        mApi.getSession().setOAuth2AccessToken(Tools.getPreference(mContext, com.jgmoneymanager.main.R.string.dropboxTokenKey));
                        DropboxUploadTaskLocal dUpload = new DropboxUploadTaskLocal(mContext, mApi, "", file, false);
                        dUpload.execute();
                    }
                }
            } catch (InterruptedException e) {
                Log.v("ServiceWorker", "... sleep interrupted");
            }
        }
    }


    private void displayNotificationMessage(String message, Context context)
    {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context);
        notification.setSmallIcon(R.drawable.icon)
                .setTicker(message)
                .setContentText(message)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, MainScreen.class), 0))
                .setAutoCancel(false);
        NotificationManager mgr= (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        mgr.notify(1, notification.build());
        /*Notification notification = new Notification(R.drawable.icon, message, System.currentTimeMillis());

        notification.flags = Notification.FLAG_NO_CLEAR;

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainScreen.class), 0);

        notification.setLatestEventInfo(this, getResources().getString(R.string.app_name), message, contentIntent);

        notificationMgr.notify(0, notification);*/
    }

}
