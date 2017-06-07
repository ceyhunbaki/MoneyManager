package com.jgmoneymanager.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.jgmoneymanager.database.MoneyManagerProviderMetaData;
import com.jgmoneymanager.main.R;
import com.jgmoneymanager.main.TransactionEdit;
import com.jgmoneymanager.tools.Constants;

/**
 * Created by Ceyhun on 24/06/2016.
 */
public class ConfAddTransWidgetActivity extends AppWidgetProvider {
    private static final String ACTION_CLICK = "ACTION_CLICK";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {

        // Get all ids
        ComponentName thisWidget = new ComponentName(context, ConfAddTransWidgetActivity.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int widgetId : allWidgetIds) {
            // create some random data
            //int number = (new Random().nextInt(100));

            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

            Intent configIntent = new Intent(context, TransactionEdit.class);

            configIntent.setAction(Intent.ACTION_INSERT);
            Bundle bundle = new Bundle();
            bundle.putString(Constants.UpdateMode, Constants.Insert);
            Constants.TransactionType = Constants.TransactionTypeExpence;
            bundle.putInt(MoneyManagerProviderMetaData.TransactionsTableMetaData.TRANSTYPE, Constants.TransactionType);
            configIntent.putExtras(bundle);

            PendingIntent configPendingIntent = PendingIntent.getActivity(context, 0, configIntent, 0);

            remoteViews.setOnClickPendingIntent(R.id.update, configPendingIntent);

            appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);

        }
    }
}