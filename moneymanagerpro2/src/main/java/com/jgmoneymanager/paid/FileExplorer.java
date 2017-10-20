package com.jgmoneymanager.paid;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class FileExplorer extends MyActivity /*implements NavigationView.OnNavigationItemSelectedListener*/{

	public static final int DialogOpenFileID = 1000;
	public static final int DialogOpenFolderID = 1100;
	private static int dialogType = DialogOpenFileID;
	public static final String paramSelBackupFolder = "paramSelectBackupFolder";

	private final int menuDelete = Menu.FIRST;
	private final int menuEdit = menuDelete + 1;
	private final int menuSelectCtx = menuDelete + 2;

	private final int btAddFolder = menuDelete + 3;
	private final int btSelectFolder = menuDelete + 4;

	static final String strRoot = "/";
	//static String curDirSt = strRoot;			
	File baseDir = new File(Constants.baseDirectory);
	static StringBuffer currentDir;
	static ArrayList<String> arrayItem = new ArrayList<String>();
	static ArrayList<String> arrayPath = new ArrayList<String>();
	public static String filePath = strRoot;

	boolean selectBackupFolder = false;

	ListView listviewT;
	TextView textviewPathCurrent;

	final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = PackageManager.PERMISSION_GRANTED;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initializeViews();
		//setContentView(R.layout.fileexplorer);

		Bundle bundle = getIntent().getExtras();
		if (bundle.containsKey(Constants.title))
			this.setTitle(bundle.getString(Constants.title));
		if (bundle.containsKey(Constants.dialogType))
			dialogType = bundle.getInt(Constants.dialogType);
		if (bundle.containsKey(paramSelBackupFolder))
			selectBackupFolder = bundle.getString(paramSelBackupFolder).equals("1");

		if (savedInstanceState != null) {
			currentDir = new StringBuffer(Tools.getStringFromBundle(savedInstanceState, "currentDir"));
			arrayItem = Tools.getStringArrayListFromBundle(savedInstanceState, "arrayItem");
			arrayPath = Tools.getStringArrayListFromBundle(savedInstanceState, "arrayPath");
			filePath = Tools.getStringFromBundle(savedInstanceState, "filePath");
		}
		else {
			if (bundle.containsKey(Constants.folderKey))
				filePath = bundle.getString(Constants.folderKey);
			else if (baseDir.exists())
		    	filePath = baseDir.getPath();
			currentDir = new StringBuffer(filePath);
		}

		reloadScreen();
	}

	private void initializeViews() {
		setContentView(R.layout.main_activity);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

		LinearLayout mainLayout = (LinearLayout) findViewById(R.id.content_main_layout);
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		View child = inflater.inflate(R.layout.fileexplorer, null);
		mainLayout.addView(child, params);

//		Menu menu = navigationView.getMenu();
//		menu.add(0, btSelectFolder, btSelectFolder, R.string.menuSelectFolder);
//		menu.add(0, btAddFolder, btAddFolder, R.string.menuAddFolder);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Tools.putToBundle(outState, "currentDir", currentDir.toString());
		Tools.putToBundle(outState, "arrayItem", arrayItem);
		Tools.putToBundle(outState, "arrayPath", arrayPath);
		Tools.putToBundle(outState, "filePath", currentDir.toString());
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		reloadScreen();
	}

	private void reloadScreen() {

		File path = new File(filePath);
		if (!path.exists()) {
			filePath = Environment.getRootDirectory().getPath();
			currentDir = new StringBuffer(filePath);
		}

		textviewPathCurrent = (TextView) findViewById(R.id.tvFEPath);
		textviewPathCurrent.setText(filePath);
		listviewT = (ListView) findViewById(R.id.lvFe);
		registerForContextMenu(listviewT);
		refreshDir(filePath, listviewT, arrayPath, arrayItem, this);
		listviewT.setTextFilterEnabled(true);

		listviewT.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, final View arg1, int position, long arg3) {

				final File file = new File(arrayPath.get(position));

				if (file.canRead()) {
					FileExplorer.filePath = file.getPath();
					if ((file.isDirectory() && (dialogType == DialogOpenFileID)) ||
							(file.isDirectory() && /*(hasDirectory(file)) &&*/ (dialogType == DialogOpenFolderID))) {
						textviewPathCurrent.setText(file.getPath());
						currentDir.insert(0, arrayPath.get(position));
						currentDir.setLength(arrayPath.get(position).length());
						refreshDir(arrayPath.get(position), listviewT, arrayPath, arrayItem, arg1.getContext());
					} else {
						selectFolder(file.getPath());
					}
				} else {
					// can't access the directory
					DialogTools.toastDialog(arg1.getContext(), getString(R.string.accessNot), Toast.LENGTH_LONG);
				}
			}
		});

		Button button = (Button) findViewById(R.id.btFEUp);
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (!currentDir.toString().equals(strRoot)) {
					File file = new File(currentDir.toString());

					if (!file.getName().equals(strRoot)) {

						refreshDir(file.getParent(), listviewT, arrayPath, arrayItem, v.getContext());

						//if (file.getParent().equals(strRoot) == false) {
						currentDir.insert(0, file.getParent());
						currentDir.setLength(file.getParent().length());
						//}

						textviewPathCurrent.setText(file.getParent());
					}
				}
			}
		});
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.setHeaderIcon(R.drawable.ic_menu_manage);
		menu.add(0, menuDelete, menuDelete, R.string.menuDelete);
		menu.add(0, menuEdit, menuEdit, R.string.menuEdit);
		menu.add(0, menuSelectCtx, menuSelectCtx, R.string.menuSelectFolder);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		super.onContextItemSelected(item);
		final AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case menuDelete:
			final File deletedFile = new File(arrayPath.get((int) info.id));
			if (deletedFile.canRead()) {
				Command cmdDelete = new Command() {
					@Override
					public void execute() {
						Tools.deleteFolder(deletedFile);
						refreshDir(deletedFile.getParent(), listviewT, arrayPath, arrayItem, getBaseContext());
					}
				};
				AlertDialog dialog = DialogTools.confirmDialog(FileExplorer.this, cmdDelete, R.string.msgConfirm,
						getString(R.string.deleteFolder) + " - " + arrayPath.get((int) info.id) +
						"?");
				dialog.show();
			}
			else
				DialogTools.toastDialog(FileExplorer.this, getString(R.string.accessNot), Toast.LENGTH_LONG);
			break;
		case menuEdit:
			File editedFile = new File(arrayPath.get((int) info.id));
			if (editedFile.canRead())
				editFolder(editedFile.getParent(), editedFile.getName());
			else
				DialogTools.toastDialog(FileExplorer.this, getString(R.string.accessNot), Toast.LENGTH_LONG);
			break;
		case menuSelectCtx:
			File selectedFile = new File(arrayPath.get((int) info.id));
			filePath = selectedFile.getPath();
			selectFolder(selectedFile.getName());
			break;
		default:
			break;
		}
		return true;
	}

	private void addFolder(final Context context, final String path) {
		final EditText inputText = new EditText(FileExplorer.this);
		Command cmd = new Command() {
			@Override
			public void execute() {
				int permissionCheck = ContextCompat.checkSelfPermission(context,
						android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
				if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
					Tools.addFolder(path, inputText.getText().toString());
					refreshDir(path, listviewT, arrayPath, arrayItem, context);
				}
				else {
					ActivityCompat.requestPermissions(FileExplorer.this,
							new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
							MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
				}
			}
		};
		AlertDialog fileNameDialog = DialogTools.InputDialog(FileExplorer.this, cmd, R.string.addFolder, inputText, R.drawable.ic_menu_add);
		fileNameDialog.show();
		fileNameDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(inputText.getText().toString().trim().length() != 0);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		switch (requestCode) {
			case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {

					// permission was granted, yay! Do the
					// contacts-related task you need to do.
					addFolder(FileExplorer.this, currentDir.toString());

				} else {

					// permission denied, boo! Disable the
					// functionality that depends on this permission.
					DialogTools.toastDialog(FileExplorer.this, R.string.error, Toast.LENGTH_SHORT);
				}
				return;
			}

			// other 'case' lines to check for other
			// permissions this app might request
		}
	}

	private void editFolder(final String path, final String name) {
		final EditText inputText = new EditText(FileExplorer.this);
		inputText.setText(name);
		Command cmd = new Command() {
			@Override
			public void execute() {
				Tools.ediFile(path, name, inputText.getText().toString());
				refreshDir(path, listviewT, arrayPath, arrayItem, FileExplorer.this);
			}
		};
		AlertDialog fileNameDialog = DialogTools.InputDialog(FileExplorer.this, cmd, R.string.addFolder, inputText, R.drawable.ic_menu_add);
		fileNameDialog.show();
		fileNameDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(inputText.getText().toString().trim().length() != 0);
	}

	private void refreshDir(String strT, ListView listviewT,
			ArrayList<String> arrayPathT, ArrayList<String> arrayItemT, Context contextT) {
		arrayItemT.clear();
		arrayPathT.clear();

		// fill array adapter
		File f = new File(strT);
		File[] files = f.listFiles();

		try {
			Arrays.sort(files, new Comparator<File>() {
				public int compare(File f1, File f2) {
					String f1Name = f1.isDirectory() ? "/" + f1.getName() : f1.getName();
					String f2Name = f2.isDirectory() ? "/" + f2.getName() : f2.getName();
					return String.valueOf(f1Name).compareTo(f2Name);
				}
			});
		}
		catch (Exception e) { }

		try {
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				if (file.isDirectory()) {
					arrayItemT.add(file.getName() + "/");
					arrayPathT.add(file.getPath());
				} else if (dialogType == DialogOpenFileID) {
					arrayItemT.add(file.getName());
					arrayPathT.add(file.getPath());
				}
			}

			// fill the list view
			listviewT.setAdapter(new ArrayAdapter<String>(contextT, R.layout.dialog_row_white, arrayItemT));
		}
		catch (Exception e) {

		}

	}

	private void selectFolder(final String fileName) {
		Command cmd = new Command() {
			@Override
			public void execute() {
				filePath = fileName;

				if (selectBackupFolder) {
					Tools.setPreference(FileExplorer.this, R.string.backupFolderKey, filePath, false);
				}

				FileExplorer.this.setResult(RESULT_OK);
				FileExplorer.this.finish();
			}
		};
		AlertDialog confirmDialog = DialogTools.confirmDialog(FileExplorer.this, cmd, R.string.msgConfirm, getResources().getString(R.string.msgWantThisDirectory) + fileName + "] ?");
		confirmDialog.show();
	}

	public void myClickHandler(View target) {
		switch (target.getId()) {
			case R.id.btFeSelectThis:
				selectFolder(textviewPathCurrent.getText().toString());
				break;
			case R.id.btFeAddFolder:
				addFolder(FileExplorer.this, currentDir.toString());
				break;
			default:
				break;
		}
	}

//	@Override
//	public boolean onNavigationItemSelected(MenuItem item) {
//		int id = item.getItemId();
//		if (id == btSelectFolder) {
//			selectFolder(textviewPathCurrent.getText().toString());
//		}
//		else if (id == btAddFolder) {
//			addFolder(FileExplorer.this, currentDir.toString());
//		}
//
//		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
//		drawer.closeDrawer(GravityCompat.START);
//
//		return true;
//	}
}
