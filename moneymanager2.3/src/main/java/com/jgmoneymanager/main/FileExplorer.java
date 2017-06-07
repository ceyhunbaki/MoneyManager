package com.jgmoneymanager.main;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.jgmoneymanager.SlidingMenu.MyHorizontalScrollView;
import com.jgmoneymanager.SlidingMenu.OnSwipeTouchListener;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyAbstractDemoChartctivity;
import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.tools.Command;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.LocalTools;
import com.jgmoneymanager.tools.Tools;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class FileExplorer extends MyActivity{

	public static final int DialogOpenFileID = 1000;
	public static final int DialogOpenFolderID = 1100;
	private static int dialogType = DialogOpenFileID;

	private MyHorizontalScrollView scrollView;
	private static View menu;
	private View app;
	private ImageView btnSlide;
	public static boolean menuOut = false;

	private final int menuDelete = Menu.FIRST;
	private final int menuEdit = menuDelete + 1;
	private final int menuSelectCtx = menuDelete + 2;

	String btAddFolder = "btAddFolder";
	String btSelectFolder = "btSelectFolder";

	static final String strRoot = "/";
	//static String curDirSt = strRoot;			
	File baseDir = new File(Constants.baseDirectory);
	static StringBuffer currentDir;
	static ArrayList<String> arrayItem = new ArrayList<String>();
	static ArrayList<String> arrayPath = new ArrayList<String>();
	public static String filePath = strRoot;

	ListView listviewT;
	TextView textviewPathCurrent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initializeViews();
		((TextView)findViewById(R.id.tvATTitle)).setText(R.string.explorer);
		//requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		//setContentView(R.layout.fileexplorer);
		//getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
		//((TextView)findViewById(R.id.cusTitleText)).setText(R.string.explorer);

		Bundle bundle = getIntent().getExtras();
		if (bundle.containsKey(Constants.title))
			this.setTitle(bundle.getString(Constants.title));
		if (bundle.containsKey(Constants.dialogType))
			dialogType = bundle.getInt(Constants.dialogType);

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
		LayoutInflater inflater = LayoutInflater.from(this);
		scrollView = (MyHorizontalScrollView) inflater.inflate(R.layout.horz_scroll_with_list_menu, null);

		setContentView(scrollView);

		//myApp = (MyApplicationLocal)getApplication();

		menu = inflater.inflate(R.layout.horz_scroll_menu, null);
		//app = inflater.inflate(R.layout.activitytest, null);
		app = inflater.inflate(R.layout.fileexplorer, null);
		ViewGroup tabBar = (ViewGroup) app.findViewById(R.id.relATTop);

		btnSlide = (ImageView) tabBar.findViewById(R.id.btATMenu);
		btnSlide.setOnClickListener(new MyAbstractDemoChartctivity.ClickListenerForScrolling(scrollView, menu));

		final View[] children = new View[] { menu, app };

		// Scroll to app (view[1]) when layout finished.
		int scrollToViewIdx = 1;
		scrollView.initViews(children, scrollToViewIdx, new MyAbstractDemoChartctivity.SizeCallbackForMenu(btnSlide));

		LocalTools.addButtonToMenuList(this, R.string.menuSelectFolder, btSelectFolder);
		LocalTools.addButtonToMenuList(this, R.string.menuAddFolder, btAddFolder);

		menu.setOnTouchListener(mySwipeListener);
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
							(file.isDirectory() && (hasDirectory(file)) && (dialogType == DialogOpenFolderID))) {
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
		final File file = new File(filePath);
		switch (item.getItemId()) {
		case menuDelete:
			File deletedFile = new File(arrayPath.get((int) info.id));
			if (deletedFile.canRead()) {
				Command cmdDelete = new Command() {
					@Override
					public void execute() {
						Tools.deleteFolder(file);
						refreshDir(file.getParent(), listviewT, arrayPath, arrayItem, getBaseContext());
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
				editFolder(FileExplorer.this, filePath, editedFile.getName());
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
				Tools.addFolder(path, inputText.getText().toString());
				refreshDir(path, listviewT, arrayPath, arrayItem, context);
			}
		};
		AlertDialog fileNameDialog = DialogTools.InputDialog(FileExplorer.this, cmd, R.string.addFolder, inputText, R.drawable.ic_menu_add);
		fileNameDialog.show();
		fileNameDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(inputText.getText().toString().trim().length() != 0);
	}

	private void editFolder(final Context context, final String path, final String name) {
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

		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			if (file.isDirectory()) {
				arrayItemT.add(file.getName() + "/");
				arrayPathT.add(file.getPath());
			}
			else if (dialogType == DialogOpenFileID) {
				arrayItemT.add(file.getName());
				arrayPathT.add(file.getPath());
			}
		}

		// fill the list view
		listviewT.setAdapter(new ArrayAdapter<String>(contextT, R.layout.dialog_row_white, arrayItemT));

	}

	private void selectFolder(final String fileName) {
		Command cmd = new Command() {
			@Override
			public void execute() {
				filePath = fileName;
				FileExplorer.this.setResult(RESULT_OK);
				FileExplorer.this.finish();
			}
		};
		AlertDialog confirmDialog = DialogTools.confirmDialog(FileExplorer.this, cmd, R.string.msgConfirm, getResources().getString(R.string.msgWantThisDirectory) + fileName + "] ?");
		confirmDialog.show();
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

	public void myClickHandler(View target) {
		hideMenu();
		if (target.getTag() == btAddFolder) {
			addFolder(FileExplorer.this, currentDir.toString());
		}
		else if (target.getTag() == btSelectFolder) {
			selectFolder(textviewPathCurrent.getText().toString());
		}
	}

	//menu el ile geriye cekmek ucun
	private OnSwipeTouchListener mySwipeListener = new OnSwipeTouchListener() {
		public void onSwipeLeft() {
			if (menu.getVisibility() == View.VISIBLE) {
				menu.setVisibility(View.INVISIBLE);
				scrollView.smoothScrollTo(menu.getMeasuredWidth(), 0);
				menuOut = false;
			}
		}
		public void onSwipeRight() {
			if (menu.getVisibility() == View.INVISIBLE) {
				menu.setVisibility(View.VISIBLE);
				scrollView.smoothScrollTo(0, 0);
				menuOut = true;
			}
		}
	};

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		if (menuOut) {
			hideMenu();
		}
		else {
			FileExplorer.menu.setVisibility(View.VISIBLE);
			scrollView.smoothScrollTo(0, 0);
			menuOut = true;
		}
		return super.onMenuOpened(featureId, menu);
	}

	void hideMenu() {
		FileExplorer.menu.setVisibility(View.INVISIBLE);
		scrollView.smoothScrollTo(FileExplorer.menu.getMeasuredWidth(), 0);
		menuOut = false;
	}

	/**
	 * Helper for examples with a HSV that should be scrolled by a menu View's width.
	 */
	static class ClickListenerForScrolling implements View.OnClickListener {
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
	static class SizeCallbackForMenu implements MyHorizontalScrollView.SizeCallback {
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
