package com.jgmoneymanager.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.jgmoneymanager.paid.R;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import java.util.Date;

/**
 * Created by Ceyhun on 22.12.2015.
 */
public class LocalDialog {


    public static AlertDialog DualDateDialog(final Context context, View dialogView, Date startDate, Date endDate, final Command cmd) {
        final Button dpStartDate = (Button) dialogView.findViewById(R.id.dmDateFrom);
        dpStartDate.setText(Tools.DateToString(startDate, Constants.DateFormatUser));
        final Button dpEndDate = (Button) dialogView.findViewById(R.id.dmDateTo);
        dpEndDate.setText(Tools.DateToString(endDate, Constants.DateFormatUser));

        final AlertDialog viewDialogCustom = DialogTools.CustomDialog(context, dialogView);

        final Command cmdPeriod = new Command() {
            @Override
            public void execute() {
                final Date localStartDate = Tools.StringToDate(dpStartDate.getText().toString(), Constants.DateFormatUser);
                final Date localEndDate = Tools.StringToDate(dpEndDate.getText().toString(), Constants.DateFormatUser);
                if (localStartDate.compareTo(localEndDate) < 0) {
                    cmd.execute();
                    viewDialogCustom.dismiss();
                } else
                    DialogTools.toastDialog(context, R.string.msgFromDateIsLessThanTo, Toast.LENGTH_LONG);
            }
        };

        Command cmdCancel = new Command() {
            @Override
            public void execute() {
                viewDialogCustom.dismiss();
            }
        };

        DialogTools.setButtonActions(dialogView, R.id.dmBtOK, R.id.dmBtCancel, cmdPeriod, cmdCancel);
        return viewDialogCustom;
    }
}
