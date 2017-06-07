package com.jgmoneymanager.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.jgmoneymanager.entity.CheckBoxItem;
import com.jgmoneymanager.mmlibrary.R;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Command.CommandWrapper;
import com.jgmoneymanager.tools.Constants;

import java.util.ArrayList;

public class DialogTools {

	//static Object resultValue;

    private static final CommandWrapper DISMISS = new CommandWrapper(Command.NO_OP);

	// Dialog methods
	public static AlertDialog confirmDialog(final Context context,
			final Command command, int title, int message) {
		return confirmDialog(context, command, title, context.getResources()
				.getString(message), new String[] {
				context.getResources().getString(R.string.Yes),
				context.getResources().getString(R.string.No) });
	}

	public static AlertDialog confirmDialog(final Context context,
			final Command command, int title, String message) {
		return confirmDialog(context, command, title, message, new String[] {
				context.getResources().getString(R.string.Yes),
				context.getResources().getString(R.string.No) });
	}

	public static AlertDialog confirmDialog(final Context context,
			final Command command, int title, int message, String[] buttonNames) {
		return confirmDialog(context, command, title, context.getResources()
				.getString(message), buttonNames);
	}

	public static AlertDialog confirmDialog(final Context context,
			final Command command, int title, String message,
			String[] buttonNames) {
		return confirmDialog(context, command, Command.NO_OP, title, message, buttonNames);
	}

	public static AlertDialog confirmDialog(final Context context,
			final Command yesCommand, final Command noCommand, int title, String message,
			String[] buttonNames) {

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setCancelable(true);
		builder.setIcon(R.drawable.ic_dialog_alert);
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setInverseBackgroundForced(true);
		builder.setPositiveButton(buttonNames[0], new CommandWrapper(yesCommand));
		builder.setNegativeButton(buttonNames[1], new CommandWrapper(noCommand));
		//dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		return builder.create();
	}

	public static AlertDialog confirmWithCancelDialog(final Context context,
													  final Command yesCommand, final Command noCommand,
													  final Command cancelCommand, int title, int message) {
		String[] buttonNames = new String[] {context.getString(R.string.Yes), context.getString(R.string.No), context.getString(R.string.Cancel)};
		return confirmWithCancelDialog(context, yesCommand, noCommand, cancelCommand, title, context.getString(message), buttonNames);
	}

	public static AlertDialog confirmWithCancelDialog(final Context context,
													  final Command yesCommand, final Command noCommand,
													  final Command cancelCommand, int title, String message,
													  String[] buttonNames) {

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setCancelable(true);
		builder.setIcon(R.drawable.ic_dialog_alert);
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setInverseBackgroundForced(true);
		builder.setPositiveButton(buttonNames[0], new CommandWrapper(yesCommand));
		builder.setNegativeButton(buttonNames[1], new CommandWrapper(noCommand));
		builder.setNeutralButton(buttonNames[2], new CommandWrapper(cancelCommand));
		return builder.create();
	}

	public static AlertDialog warningDialog(final Context context, int title, String message) {
		return warningDialog(context, title, Command.NO_OP, message);
	}

	public static AlertDialog warningDialog(final Context context, int title, Command command, String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setCancelable(false);
		builder.setIcon(R.drawable.ic_dialog_alert);
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setInverseBackgroundForced(true);
		builder.setNeutralButton(R.string.ok, new CommandWrapper(command));
		return builder.create();
	}
	


	/*public static AlertDialog informationDialog(final Context context, int title, int message) {
		return informationDialog(context, title, context.getResources().getString(message));
	}*/

	public static AlertDialog informationDialog(final Context context, int title, String message) {
		/*AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setCancelable(true);
		builder.setIcon(R.drawable.ic_dialog_info);
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setInverseBackgroundForced(true);
		builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				return;
			}
		});
		return builder.create();*/
		return informationDialog(context, title, message, Command.NO_OP);
	}
	
	public static AlertDialog informationDialog(final Context context, int title, int message, Command command) {
		return informationDialog(context, title, context.getResources().getString(message), command);
	}

	public static AlertDialog informationDialog(final Context context, int title, String message, Command command) {

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setCancelable(true);
		builder.setIcon(R.drawable.ic_dialog_info);
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setInverseBackgroundForced(true);
		builder.setNeutralButton(R.string.ok, new CommandWrapper(command));
		return builder.create();
	}

	public static AlertDialog InputDialog(final Context context,
			final Command command, String title, View input, int iconID) {
        return InputDialog(context, command, Command.NO_OP, title, input, iconID, R.string.ok);
	}

	public static AlertDialog InputDialog(final Context context,
										  final Command command, int title, View input, int iconID, int yesButtonTitle) {
		return InputDialog(context, command, Command.NO_OP, context.getResources().getString(title), input, iconID, yesButtonTitle);
	}

	public static AlertDialog InputDialog(final Context context,
										  final Command yesCommand, final Command noCommand, String title, View input, int iconID) {
		return 	InputDialog(context, yesCommand, noCommand, title, input, iconID, R.string.ok);
	}

	public static AlertDialog InputDialog(final Context context,
			final Command yesCommand, final Command noCommand, String title, View input, int iconID, int yesButtonTitle) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setCancelable(true);
		builder.setIcon(iconID);
		builder.setTitle(title);
		builder.setInverseBackgroundForced(true);

		builder.setView(input);
		
		builder.setPositiveButton(yesButtonTitle, new CommandWrapper(yesCommand));
		builder.setNegativeButton(R.string.Cancel, new CommandWrapper(noCommand));
		final AlertDialog dialog = builder.create();
		
		try{
			final EditText edInput = (EditText) input;
			edInput.addTextChangedListener(new TextWatcher() {				
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}
				
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}
				
				@Override
				public void afterTextChanged(Editable s) {
					dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(edInput.getText().toString().length() != 0);
				}
			});
		}
		catch (Exception e) {
			
		}

		return dialog;
	}
	
	public static AlertDialog InputDialog(final Context context,
			final Command command, int title, View input, int iconID) {
        return InputDialog(context, command, context.getResources().getString(title), input, iconID);
	}

	/*
	 * public static AlertDialog InputDialogWithView(final Context context,
	 * final Command command, String title, View view, int iconID) {
	 * AlertDialog.Builder builder = new AlertDialog.Builder(context);
	 * builder.setCancelable(true); builder.setIcon(iconID);
	 * builder.setTitle(title); builder.setInverseBackgroundForced(true);
	 * builder.setView(view);
	 * 
	 * builder.setPositiveButton(R.string.ok, new CommandWrapper(command));
	 * builder.setNegativeButton("Cancel", DISMISS); return builder.create(); }
	 * 
	 * public static AlertDialog InputDialogWithView(final Context context,
	 * final Command command, int title, View view, int iconID) { AlertDialog
	 * builder = InputDialogWithView(context, command,
	 * context.getResources().getString(title), view, iconID); return builder; }
	 */

	public static void toastDialog(Context context, int message, int duration) {
		toastDialog(context, context.getResources().getString(message),
				duration);
	}

	public static void toastDialog(Context context, String message, int duration) {
		try{
			Toast toast = Toast.makeText(context, message, duration);
			toast.show();
		}
		catch (Exception e) {
			
		}
	}

	public static AlertDialog RadioListDialog(final Context context,
			final Command command, int title, final Cursor cursor, final String labelColumn, boolean cancelable, boolean closeCursorOnDialogClose) {

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setCancelable(cancelable);
		builder.setIcon(R.drawable.ic_menu_edit);
		builder.setTitle(title);
		builder.setSingleChoiceItems(cursor, -1, labelColumn,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						Constants.cursorPosition = item;
						CommandWrapper cmd = new CommandWrapper(command);
						cmd.execute();
						dialog.dismiss();
					}
				});

		//builder.setPositiveButton(R.string.ok, new CommandWrapper(command));
		if (cancelable)
			builder.setNegativeButton(R.string.Cancel, DISMISS);

		AlertDialog dialog = builder.create();
		if (closeCursorOnDialogClose) {
			dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialogInterface) {
					cursor.close();
				}
			});
			dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialogInterface) {
					cursor.close();
				}
			});
		}

		return dialog;
	}

	public static AlertDialog RadioListDialog(final Context context,
			final Command command, int title, View view) {

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setCancelable(true);
		builder.setIcon(R.drawable.ic_menu_edit);
		builder.setTitle(title);
		
		builder.setView(view);

		builder.setPositiveButton(R.string.ok, new CommandWrapper(command));
		builder.setNegativeButton(R.string.Cancel, DISMISS);
		return builder.create();
	}

	public static AlertDialog CustomDialog(final Context context,
			final Command command, int title, View view, int iconID) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setCancelable(true);
		builder.setIcon(iconID);
		builder.setTitle(title);
		builder.setInverseBackgroundForced(true);

		builder.setView(view);

		builder.setPositiveButton(R.string.ok, new CommandWrapper(command));
		builder.setNegativeButton(R.string.Cancel, DISMISS);
		return builder.create();
	}

	/**
	 * Actions for buttons will be set with the method @setButtonActions
	 * @param context
	 * @param view
	 * @return
	 */
	public static AlertDialog CustomDialog(final Context context, View view) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setCancelable(true);
        /*builder.setIcon(iconID);
        builder.setTitle(title);*/
		builder.setInverseBackgroundForced(true);
		builder.setView(view);
		return builder.create();
	}

	/**
	 * Set actions for buttons
	 * @param view parentView
	 * @param okButtonID
	 * @param cancelButtonID
	 * @param okCommand Action for @okButtonID
	 * @param cancelCommand Action for @cancelButtonID
	 */
	public static void setButtonActions(View view, int okButtonID, int cancelButtonID, final Command okCommand, final Command cancelCommand) {
		view.findViewById(okButtonID).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				okCommand.execute();
			}
		});

		view.findViewById(cancelButtonID).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				cancelCommand.execute();
			}
		});
	}

	/**
	 * Set actions for buttons
	 * @param view
	 * @param buttonID
	 * @param command
	 */
	public static void setButtonActions(View view, int buttonID, final Command command) {
		view.findViewById(buttonID).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				command.execute();
			}
		});
	}

	public static AlertDialog CheckListDialog(final Context context,
			final Command command, int title,
			final ArrayList<CheckBoxItem> itemsList,
			final boolean[] checkedItems) {

		ArrayList<String> itemsStringList = new ArrayList<>(
				itemsList.size());
		for (int i = 0; i < itemsList.size(); i++)
			itemsStringList.add(itemsList.get(i).getName());

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setCancelable(true);
		builder.setIcon(R.drawable.ic_menu_edit);
		builder.setTitle(title);
		builder.setMultiChoiceItems(
				itemsStringList.toArray(new String[itemsStringList.size()]),
				checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
					public void onClick(DialogInterface dialog, int which,
							boolean isChecked) {

					}
				});

		builder.setPositiveButton(R.string.ok, new CommandWrapper(command));
		builder.setNegativeButton(R.string.Cancel, DISMISS);
		return builder.create();
	}
	
	public static void systemNotification(Context context, Intent notificationIntent, int notificationID, String tickerText, String title, String text, int icon) {	
		NotificationManager mNotificationManager =(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		long when = System.currentTimeMillis();         
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
		Notification notification = new Notification(icon, tickerText, when);
		notification.setLatestEventInfo(context, title, text, contentIntent);
		notification.flags = Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL;
		mNotificationManager.notify(notificationID, notification);	
	}

}
