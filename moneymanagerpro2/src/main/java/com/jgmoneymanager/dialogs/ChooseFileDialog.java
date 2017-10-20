package com.jgmoneymanager.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.paid.R;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class ChooseFileDialog extends MyActivity {
	public static final int DialogOpenFileID = 1000;
	public static final int DialogOpenFolderID = 1100;
	private static int dialogType = DialogOpenFileID;

	public static String filePath;

	final String strRoot = "/";

	String curDirSt = strRoot;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle bundle = getIntent().getExtras();
		if (bundle.containsKey(Constants.title))
			this.setTitle(bundle.getString(Constants.title));
		if (bundle.containsKey(Constants.dialogType))
			dialogType = bundle.getInt(Constants.dialogType);
		if (savedInstanceState != null)
			curDirSt = Tools.getStringFromBundle(savedInstanceState, "curDirSt");
		showDialog1();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Tools.putToBundle(outState, "curDirSt", curDirSt);
		super.onSaveInstanceState(outState);
	}

	public void showDialog1() {
		this.showDialog(dialogType);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		/*
		 * switch (id) { case DialogOpenFileID:
		 */
		LayoutInflater li = LayoutInflater.from(this);
		View dialogFileInputView = li.inflate(R.layout.dialog_open, null);

		AlertDialog.Builder dialogFileInputBuilder = new AlertDialog.Builder(
				this);
		//dialogFileInputBuilder.setTitle(R.string.msgSelectFile);
		dialogFileInputBuilder.setView(dialogFileInputView);
		AlertDialog dialogFileInput = dialogFileInputBuilder.create();

		dialogFileInput.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				ChooseFileDialog.this.setResult(RESULT_CANCELED);
				ChooseFileDialog.this.finish();
			}
		});

		return dialogFileInput;
		/*
		 * default: break; } return null;
		 */
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		File baseDir;
		if (getIntent().getAction().equals(Constants.ActionChooseFileForRestore))
			baseDir = new File(Constants.backupDirectory);
		else
			baseDir = new File(Constants.baseDirectory);
		if (baseDir.exists() && curDirSt.equals(strRoot))
			curDirSt = baseDir.getPath();

		final StringBuffer currentDir = new StringBuffer(curDirSt);
		final ArrayList<String> arrayItem = new ArrayList<String>();
		final ArrayList<String> arrayPath = new ArrayList<String>();
		final AlertDialog dialogFileInput = (AlertDialog) dialog;
		final TextView textviewPathCurrent = (TextView) dialogFileInput.findViewById(R.id.tvDofPath);
		textviewPathCurrent.setText(curDirSt);
		final ListView listviewT = (ListView) dialogFileInput.findViewById(R.id.dofList);

		try {
			refreshDir(curDirSt, listviewT, arrayPath, arrayItem, this);
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		listviewT.setTextFilterEnabled(true);

		listviewT.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, final View arg1, int position, long arg3) {
				try {
					final File file = new File(arrayPath.get(position));

					if (file.canRead()) {
						if ((file.isDirectory() && (dialogType == DialogOpenFileID))
								|| (file.isDirectory() && (hasDirectory(file)) && (dialogType == DialogOpenFolderID))) {
							textviewPathCurrent.setText(file.getPath());
							currentDir.insert(0, arrayPath.get(position));
							currentDir.setLength(arrayPath.get(position).length());
							refreshDir(arrayPath.get(position), listviewT, arrayPath, arrayItem, arg1.getContext());
						} else {
							AlertDialog.Builder ConfirmDialog = new AlertDialog.Builder(arg1.getContext());
							ConfirmDialog.setMessage(getString(R.string.msgWantThisDirectory) + file.getName() + "] ?")
									.setCancelable(false).setPositiveButton(R.string.Yes,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										// return the selected file's path
										/*Toast.makeText(arg1.getContext(), getResources().getString(R.string.msgYouChoosedThis)
														+ " " + file.getPath(),
												Toast.LENGTH_LONG).show();*/
										filePath = file.getPath();
										dialog.dismiss();
										dialogFileInput.dismiss();
										ChooseFileDialog.this.setResult(RESULT_OK);
										ChooseFileDialog.this.finish();
									}
								})
								.setNegativeButton(R.string.No, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {dialog.cancel();}
								});
							AlertDialog alert = ConfirmDialog.create();
							alert.show();
						}
					} else {
						// can't access the directory
						Toast.makeText(arg1.getContext(), R.string.msgCannotGetFolder, Toast.LENGTH_LONG).show();
					}
				}
				catch (Exception e) {
					System.out.print(e.getMessage());
				}
			}
		});

		Button button = (Button) dialogFileInput.findViewById(R.id.btDofUp);
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				try {
					File file = new File(currentDir.toString());

					if (!file.getName().equals(strRoot)) {
						refreshDir(file.getParent(), listviewT, arrayPath, arrayItem, v.getContext());

						if (!file.getParent().equals(strRoot)) {
							currentDir.insert(0, file.getParent());
							currentDir.setLength(file.getParent().length());
						}

						textviewPathCurrent.setText(file.getParent());
					}
				} catch (Exception e) {
					DialogTools.toastDialog(ChooseFileDialog.this, R.string.msgAccessDenied, Toast.LENGTH_SHORT);
				}
			}
		});

	}

	private void refreshDir(String strT, ListView listviewT,
			ArrayList<String> arrayPathT, ArrayList<String> arrayItemT,
			Context contextT) throws Exception {
			ArrayList<String> arrayItemNew = new ArrayList<String>();
			ArrayList<String> arrayPathNew = new ArrayList<String>();
		
			//arrayItemT.clear();
			//arrayPathT.clear();
	
			// fill array adapter
			File f = new File(strT);
			File[] files = f.listFiles();
	
			Arrays.sort(files, new Comparator<File>() {
				public int compare(File f1, File f2) {
					String f1Name = f1.isDirectory() ? "/" + f1.getName() : f1.getName();
					String f2Name = f2.isDirectory() ? "/" + f2.getName() : f2.getName();
					return String.valueOf(f1Name).compareTo(f2Name);
				}
			});
	
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				if (file.isDirectory()) {
					arrayItemNew.add(file.getName() + "/");
					arrayPathNew.add(file.getPath());
				} else if (dialogType == DialogOpenFileID) {
					arrayItemNew.add(file.getName());
					arrayPathNew.add(file.getPath());
				}
			}
	
			// fill the list view
			listviewT.setAdapter(new ArrayAdapter<String>(contextT,
					R.layout.dialog_row, arrayItemNew));
			
			assignList(arrayItemNew, arrayItemT);
			assignList(arrayPathNew, arrayPathT);
			
			if (!curDirSt.equals(strT))
				curDirSt = strT;
	}

	private boolean hasDirectory(File file) {
		File[] files = file.listFiles();

		if (files != null)
			for (int i = 0; i < files.length; i++) {
				File f = files[i];
				if (f.isDirectory())
					return true;
			}

		return false;
	}
	
	void assignList(ArrayList<String> listFrom, ArrayList<String> listTo) {
		listTo.clear();
		for (int i = 0; i < listFrom.size(); i++) {
			listTo.add(listFrom.get(i));
		}
	}
}