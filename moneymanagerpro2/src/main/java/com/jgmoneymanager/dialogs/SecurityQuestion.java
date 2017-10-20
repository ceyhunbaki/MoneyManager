package com.jgmoneymanager.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.paid.R;
import com.jgmoneymanager.tools.Tools;

public class SecurityQuestion extends MyActivity {

    private static String question;
    private static String answer;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Tools.loadLanguage(this, null);
		question = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.securityQuestionKey), "");
		//answer = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.securityAnswerKey), "");
		this.showDialog(100);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		LayoutInflater li = LayoutInflater.from(this);
		View dialogFileInputView = li.inflate(R.layout.securityquestion, null);

		AlertDialog.Builder dialogFileInputBuilder = new AlertDialog.Builder(this);
		dialogFileInputBuilder.setTitle(R.string.securityQuestionTitle);
		dialogFileInputBuilder.setView(dialogFileInputView);
		AlertDialog dialogFileInput = dialogFileInputBuilder.create();		
		dialogFileInput.setOnCancelListener(new OnCancelListener() {			
			@Override
			public void onCancel(DialogInterface dialog) {
				SecurityQuestion.this.setResult(RESULT_CANCELED);
				SecurityQuestion.this.finish();
			}
		});
		return dialogFileInput;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		final AlertDialog dialogFileInput = (AlertDialog) dialog;
		
		final EditText edQuestion = (EditText) dialogFileInput.findViewById(R.id.edSQQuestion);
		if (question.length() != 0)
			edQuestion.setText(question);
		
		final EditText edAnswer = (EditText) dialogFileInput.findViewById(R.id.edSQAnswer);
		/*if (answer.length() != 0)
			edAnswer.setText(answer);*/
		
		Button btOk = (Button) dialogFileInput.findViewById(R.id.btSQOk);
		btOk.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View arg0) {				
				if (edQuestion.getText().toString().trim().length() == 0)
					DialogTools.toastDialog(SecurityQuestion.this, R.string.msgEnterQuestion, Toast.LENGTH_LONG);
				else if (edAnswer.getText().toString().trim().length() == 0)
					DialogTools.toastDialog(SecurityQuestion.this, R.string.msgEnterAnswer, Toast.LENGTH_LONG);
				else {
					answer = edAnswer.getText().toString().trim();
					Tools.setPreference(getBaseContext(), R.string.securityAnswerKey, answer, true);
					Tools.setPreference(getBaseContext(), R.string.securityQuestionKey,
							addQuestionSignAtTheEnd(edQuestion.getText().toString().trim()), false);
					SecurityQuestion.this.setResult(RESULT_OK);
					SecurityQuestion.this.finish();			
				}
			}
		});
		
		Button btCancel = (Button)dialogFileInput.findViewById(R.id.btSQCancel);
		btCancel.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				SecurityQuestion.this.setResult(RESULT_CANCELED);
				SecurityQuestion.this.finish();				
			}
		});
		
		super.onPrepareDialog(id, dialog);
	}
	
	String addQuestionSignAtTheEnd(String inputText) {
		if (inputText.substring(inputText.length() - 1).equals("?"))
			return inputText;
		else return inputText + "?";
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
