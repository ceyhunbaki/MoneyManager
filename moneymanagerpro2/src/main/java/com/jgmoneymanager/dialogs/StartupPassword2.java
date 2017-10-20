package com.jgmoneymanager.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.jgmoneymanager.entity.MyApplicationLocal;
import com.jgmoneymanager.paid.R;
import com.jgmoneymanager.tools.Tools;

public class StartupPassword2 extends AppCompatActivity {

	private MyApplicationLocal myApp;

	String password = "";

	EditText edPassword;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(this.getClass().getName(), "onCreate");
		Tools.loadLanguage(this, null);
		if (savedInstanceState != null) {
			password = Tools.getStringFromBundle(savedInstanceState, "password");
		}
		myApp = (MyApplicationLocal) getApplication();
		if (!myApp.getAskPassword()) {
			this.setResult(RESULT_OK);
			finish();
		} else {
			//isStartupAction = getIntent().getAction().equals(Constants.ActionStartupPassword);
			this.showDialog(100);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Tools.putToBundle(outState, "password", password);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		LayoutInflater li = LayoutInflater.from(this);
		View dialogFileInputView = li.inflate(R.layout.startuppassword2, null);

		AlertDialog.Builder dialogFileInputBuilder = new AlertDialog.Builder(this);
		dialogFileInputBuilder.setTitle(null);
		dialogFileInputBuilder.setView(dialogFileInputView);
		AlertDialog dialogFileInput = dialogFileInputBuilder.create();
		dialogFileInput.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				//evvel beleydi
				//StartupPassword.this.setResult(RESULT_CANCELED);
				//StartupPassword.this.finish();

				cancelDialog();
			}
		});
		return dialogFileInput;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		final AlertDialog dialogFileInput = (AlertDialog) dialog;

		Button btOK = (Button) dialogFileInput.findViewById(R.id.btSPOk);
		btOK.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (SetPassword.checkPassword(StartupPassword2.this, password)) {
					StartupPassword2.this.setResult(RESULT_OK);
					myApp.setFinishApplication(false);
					myApp.setAskPassword(false);
					StartupPassword2.this.finish();
				} else {
					DialogTools.toastDialog(StartupPassword2.this, R.string.msgIncorrectPassword, Toast.LENGTH_SHORT);
					edPassword.setText("");
					password = "";
				}
			}
		});
		
		/*Button btCancel = (Button)dialogFileInput.findViewById(R.id.btSPCancel);
		btCancel.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				cancelDialog();				
			}
		});*/

		edPassword = (EditText) dialogFileInput.findViewById(R.id.edSPPassword);
		if (password.length() != 0)
			edPassword.setText(password.replaceAll(".", "*"));

		Button btForget = (Button) dialogFileInput.findViewById(R.id.btSPForget);
		btForget.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				SetPassword.forgetPassword(StartupPassword2.this);
			}
		});

		super.onPrepareDialog(id, dialog);
	}

	void cancelDialog() {
		StartupPassword2.this.setResult(RESULT_CANCELED);
		myApp.setFinishApplication(true);
		myApp.setAskPassword(false);
		StartupPassword2.this.finish();
		//moveTaskToBack(true);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();

		cancelDialog();
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

	public void myClickHandler(View target) {
		switch (target.getId()) {
			case R.id.pas0:
				appendNumber("0");
				break;
			case R.id.pas1:
				appendNumber("1");
				break;
			case R.id.pas2:
				appendNumber("2");
				break;
			case R.id.pas3:
				appendNumber("3");
				break;
			case R.id.pas4:
				appendNumber("4");
				break;
			case R.id.pas5:
				appendNumber("5");
				break;
			case R.id.pas6:
				appendNumber("6");
				break;
			case R.id.pas7:
				appendNumber("7");
				break;
			case R.id.pas8:
				appendNumber("8");
				break;
			case R.id.pas9:
				appendNumber("9");
				break;
			case R.id.pasDelete:
				deleteLastSymbol();
				break;
		}
	}

	void appendNumber(String number) {
		if (password.length() < 4) {
			password += number;
			edPassword.setText(password.replaceAll(".", "*"));
		}
	}

	void deleteLastSymbol() {
		if (password.length() >= 1) {
			password = password.substring(0, password.length() - 1);
			edPassword.setText(password.replaceAll(".", "*"));
		}
	}
}