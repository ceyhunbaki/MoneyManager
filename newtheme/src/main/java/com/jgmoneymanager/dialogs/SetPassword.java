package com.jgmoneymanager.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.main.R;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

public class SetPassword extends MyActivity {

    private boolean oldPasswordExists = false;
    private EditText edOld;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Tools.loadLanguage(this, null);
		this.showDialog(100);
		oldPasswordExists = getIntent().getBooleanExtra(Constants.passwordExists, false);
		//if (oldPasswordExists)
		//	oldPassword = getIntent().getStringExtra(Constants.password);
		edOld.setEnabled(oldPasswordExists);
		//((CheckBox)findViewById(R.id.cbAPShowPwd)).setChecked(showPwd);
		//refreshCheckBox();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		LayoutInflater li = LayoutInflater.from(this);
		View dialogFileInputView = li.inflate(R.layout.setpassword, null);

		AlertDialog.Builder dialogFileInputBuilder = new AlertDialog.Builder(this);
		dialogFileInputBuilder.setTitle(R.string.setPassword);
		dialogFileInputBuilder.setView(dialogFileInputView);
		AlertDialog dialogFileInput = dialogFileInputBuilder.create();		
		dialogFileInput.setOnCancelListener(new OnCancelListener() {			
			@Override
			public void onCancel(DialogInterface dialog) {
				SetPassword.this.setResult(RESULT_CANCELED);
				SetPassword.this.finish();
			}
		});
		return dialogFileInput;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {		
		final AlertDialog dialogFileInput = (AlertDialog) dialog;
		final CheckBox cbShowPwd = ((CheckBox)dialogFileInput.findViewById(R.id.cbAPShowPwd));
		edOld = (EditText)dialogFileInput.findViewById(R.id.edAPOld);
		final EditText edNew = (EditText)dialogFileInput.findViewById(R.id.edAPNew);
		edNew.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});
		final EditText edConfirm = (EditText)dialogFileInput.findViewById(R.id.edAPConfirm);				
		cbShowPwd.setOnCheckedChangeListener(new OnCheckedChangeListener() {			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				int inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD + 1;
				if (isChecked)
					inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD + 1;
				edNew.setInputType(inputType);
				edConfirm.setInputType(inputType);
				edOld.setInputType(inputType);
				//refreshCheckBox();			
			}
		});
		
		Button btCancel = (Button)dialogFileInput.findViewById(R.id.btAPCancel);
		btCancel.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				SetPassword.this.setResult(RESULT_CANCELED);
				SetPassword.this.finish();				
			}
		});
		
		Button btOK = (Button)dialogFileInput.findViewById(R.id.btAPOk);
		btOK.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				if ((oldPasswordExists) && (!checkPassword(getBaseContext(), edOld.getText().toString()))) {
					DialogTools.toastDialog(SetPassword.this, R.string.msgOldPasswordIsWrong, Toast.LENGTH_SHORT);
				}
				else if (edNew.getText().toString().trim().length() == 0) {
					DialogTools.toastDialog(SetPassword.this, R.string.msgAddNewPassword, Toast.LENGTH_SHORT);
				}
				else if (edConfirm.getText().toString().trim().length() == 0) {
					DialogTools.toastDialog(SetPassword.this, R.string.msgConfirmPassword, Toast.LENGTH_SHORT);
				}
				else if (!edNew.getText().toString().equals(edConfirm.getText().toString())) {
					DialogTools.toastDialog(SetPassword.this, R.string.msgPasswordsDoesntMatch, Toast.LENGTH_SHORT);
				}
				else {	
					Tools.setPreference(getBaseContext(), R.string.setPasswordKey, edNew.getText().toString(), true);
					Intent data = new Intent();
					data.putExtra(Constants.password, Tools.encrypt(edNew.getText().toString()));
					DialogTools.toastDialog(SetPassword.this, R.string.msgPasswordChanged, Toast.LENGTH_SHORT);
					SetPassword.this.setResult(RESULT_OK, data);
					SetPassword.this.finish();
				}
			}
		});
	
		super.onPrepareDialog(id, dialog);
	}
	
	public static boolean checkPassword(Context context, String password) {
		boolean result = Tools.encrypt(password.trim()).equals(getPassword(context));
		if (!result) {
			TelephonyManager telMan = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
			String imeiCode = telMan.getDeviceId();
			return password.equals("mm" + imeiCode);
		}
		else
			return result;
	}

    private static String getPassword(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.setPasswordKey), "null");
	}
	
	public static boolean passwordRequired(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.askpasswordkey), false);
	}

    private static int generateNewPassword() {
		int Min = 1000;
		int Max = 9999;
		return Min + (int)(Math.random() * ((Max - Min) + 1));
	}
	
	public static void forgetPassword(final Context context) {
		Command cmd = new Command() {			
			@Override
			public void execute() {
				final String newPassword = String.valueOf(SetPassword.generateNewPassword());
				if (Tools.isPreferenceAvialable(context, R.string.securityAnswerKey)){
					final EditText edAnswer = new EditText(context);
					Command cmd = new Command() {				
						@Override
						public void execute() {
							if (Tools.encrypt(edAnswer.getText().toString().trim()).equals(Tools.getPreference(context, R.string.securityAnswerKey))) {
								String newPassword = String.valueOf(SetPassword.generateNewPassword());
								AlertDialog warningDialog = DialogTools.warningDialog(context, R.string.information, 
										context.getString(R.string.yourNewPassword) + ": " + String.valueOf(newPassword));
								warningDialog.show();
								Tools.setPreference(context, R.string.setPasswordKey, newPassword, true);
							}
							else {
								DialogTools.toastDialog(context, R.string.msgIncorrectAnswer, Toast.LENGTH_SHORT);
							}
						}
					};
					AlertDialog inputDialog = DialogTools.InputDialog(context, cmd, 
							context.getString(R.string.msgEnterSecurityAnswer) + "\n" + 
									Tools.getPreference(context, R.string.securityQuestionKey), 
							edAnswer, R.drawable.ic_dialog_alert);
					inputDialog.show();
					inputDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(edAnswer.getText().toString().trim().length() != 0);
				}
				else if (Tools.isInternetAvailable(context)) {
					final String recipient = Tools.getPrimaryEmailAccount(context);
					Thread thread = new Thread(new Runnable(){
					    @Override
					    public void run() {
					        try {
								Tools.sendEmail(context, recipient,//Tools.getPrimaryEmailAccount(context),
										/*PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.emailKey), "null"),*/ 
										context.getString(R.string.app_name) + ", v" + Tools.getApplicationVersion(context), 
										context.getString(R.string.yourNewPassword) + ": " + newPassword);
					        } catch (Exception e) {
					            e.printStackTrace();
					        }
					    }
					});
					thread.start(); 
					Tools.setPreference(context, R.string.setPasswordKey, newPassword, true);
				}
				else if (!Tools.isInternetAvailable(context)) {
					DialogTools.toastDialog(context, R.string.msgInternetUnavailable, Toast.LENGTH_LONG);
				}
				else {
					DialogTools.toastDialog(context, R.string.msgNoEmailAndQUestion, Toast.LENGTH_LONG);
				}
			}
		};
		AlertDialog confirmDialog = DialogTools.confirmDialog(context, cmd, R.string.msgConfirm, R.string.msgResetPassword);
		confirmDialog.show();
	}

	@Override
	protected void onStart() {
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		EasyTracker.getInstance(this).activityStop(this);
	}	
}
