package com.github.premek.kyosc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.github.premek.kyosc.fragments.AboutMeDialogFragment;
import com.github.premek.kyosc.fragments.AddNewOSCControlListDialogFragment;
import com.github.premek.kyosc.fragments.ConfirmDialogFragment;
import com.github.premek.kyosc.fragments.NetworkSettingsDialogFragment;
import com.github.premek.kyosc.fragments.OpenFileDialogFragment;
import com.github.premek.kyosc.fragments.SaveFileDialogFragment;
import com.github.premek.kyosc.fragments.OSCViewFragment;
import com.github.premek.kyosc.osc.OSCWrapper;
import com.github.premek.kyosc.utils.NavigationDrawerView;
import com.github.premek.kyosc.utils.Utilities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements
		com.github.premek.kyosc.fragments.OSCViewFragment.OnMenuToggledListener,
		com.github.premek.kyosc.fragments.AddNewOSCControlListDialogFragment.OnNewOSCControlSelected,
		com.github.premek.kyosc.fragments.SaveFileDialogFragment.OnSaveFileNameSelectedListener,
		com.github.premek.kyosc.fragments.OpenFileDialogFragment.OnOpenFileNameSelectedListener,
        com.github.premek.kyosc.fragments.NetworkSettingsDialogFragment.OnNetworkSettingsChangedListener,
        com.github.premek.kyosc.utils.NavigationDrawerView.OnOSCMenuItemClickedListener
{

	private final static String TAG_DIALOG_ADD_NEW_ITEM = "dlgAddNewItem";
	private final static String TAG_DIALOG_SAVE_FILE_NAME = "dlgSaveFileName";
	private final static String TAG_DIALOG_OPEN_FILE_NAME = "dlgOpenFileName";
    private final static String TAG_DIALOG_NETWORK_SETTINGS = "dlgNetworkSettings";
    private final static String TAG_DIALOG_ABOUT_ME = "dlgAboutMe";
    private final static String TAG_DIALOG_CONFIRM_EXIT = "dlgConfirm";

    private final static String NETWORK_SETTINGS_FILE = "kyosc_network.cfg";

	private OSCViewFragment mOSCViewFragment;
	private String mBaseFolder;
	
	private String mCurrentFileName;

    private String mIPAddress = "10.0.0.2";
    private int mPort = 4559;
    private boolean mConnectOnStartUp = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_layout);

		verifyStoragePermissions(this);
		handleExternalStorage();
        restoreNetworkSettingsFromFile();

        initializeNavigationDrawer();
        makeImmersive();

		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		
		this.mOSCViewFragment = (OSCViewFragment) getSupportFragmentManager().findFragmentById(R.id.frgOSCView);

		ft.commit();



        if(this.mConnectOnStartUp) {
            connectOSC();
        }
	}

    @Override
    protected void onStart() {
        super.onStart();

        new Handler().postDelayed(new Runnable() {
            public void run() {
                mDrawerLayout.openDrawer(Gravity.START);
            }
        }, 1000);
    }

    private DrawerLayout mDrawerLayout;
    private com.github.premek.kyosc.utils.NavigationDrawerView mDrawer;

    private void initializeNavigationDrawer() {
       mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
       mDrawer = (com.github.premek.kyosc.utils.NavigationDrawerView) findViewById(R.id.left_drawer);
       mDrawer.setOnOSCMenuActionClicked(this);
    }


    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    /**
     * Checks if the app has permission to write to device storage
     * <p>
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

	private void handleExternalStorage() {
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			Toast.makeText(this, "External Storage Read Only!", Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(this, "External Storage Not Available!", Toast.LENGTH_LONG).show();
			return;
		}

		File baseFolderFile = new File(new File(Environment.getExternalStorageDirectory(), "kyosc"), "templates");
		if (!baseFolderFile.exists()) {
			if (!baseFolderFile.mkdirs()) {
				Toast.makeText(this, "Unable To Build Application Folder!", Toast.LENGTH_LONG).show();
				return;
			}
            saveSampleTemplateFiles(baseFolderFile);
		}

		this.mBaseFolder = baseFolderFile.getAbsolutePath();
	}

	@Override
	public void openNewOSCItemDialog() {
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		Fragment prev = getSupportFragmentManager().findFragmentByTag(MainActivity.TAG_DIALOG_ADD_NEW_ITEM);
		if (prev != null) {
			ft.remove(prev);
		}

		ft.addToBackStack(null);

		AddNewOSCControlListDialogFragment newDialog = AddNewOSCControlListDialogFragment.newInstance();
		newDialog.show(ft, MainActivity.TAG_DIALOG_ADD_NEW_ITEM);
	}

    @Override
    public void openSaveTemplateDialog() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag(MainActivity.TAG_DIALOG_SAVE_FILE_NAME);
        if (prev != null) {
            ft.remove(prev);
        }

        ft.addToBackStack(null);

        SaveFileDialogFragment saveDlgFrag = SaveFileDialogFragment.getInstance(this.mBaseFolder, this.mCurrentFileName);
        saveDlgFrag.show(ft, MainActivity.TAG_DIALOG_SAVE_FILE_NAME);
    }

    @Override
    public void notifySettingsClosed() {
        if (Build.VERSION.SDK_INT >= 19) {
            regainImmersive();
        }
    }

	@Override
	public void onNewOSCControlSelected(String selectedItem) {
		this.mOSCViewFragment.addNewOSCControl(selectedItem);
	}

	@Override
	public void oscMenuItemActionSelected(NavigationDrawerView.OSCMenuActionEvent event) {
		if(event.getAction() == NavigationDrawerView.OSCMenuActionEvent.ACTION_NEW) {
			this.mOSCViewFragment.clearForNewTemplate();
            this.mDrawer.setCurrentTemplate("untitled");
            this.mCurrentFileName = "";
		}
		else if (event.getAction() == NavigationDrawerView.OSCMenuActionEvent.ACTION_OPEN) {
			// Show Save Dialog, pass the return to the OSCViewFragment
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

			Fragment prev = getSupportFragmentManager().findFragmentByTag(MainActivity.TAG_DIALOG_OPEN_FILE_NAME);
			if (prev != null) {
				ft.remove(prev);
			}

			ft.addToBackStack(null);

			OpenFileDialogFragment openDlgFrag = OpenFileDialogFragment.newInstance(this.mBaseFolder);
			openDlgFrag.show(ft, MainActivity.TAG_DIALOG_OPEN_FILE_NAME);

		}
        else if(event.getAction() == NavigationDrawerView.OSCMenuActionEvent.ACTION_NETWORK) {
            // Show Network settings gragment
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            Fragment prev = getSupportFragmentManager().findFragmentByTag(MainActivity.TAG_DIALOG_NETWORK_SETTINGS);
            if(prev != null) {
                ft.remove(prev);
            }
            ft.addToBackStack(null);

            NetworkSettingsDialogFragment frgNetworkSettingsDialog = NetworkSettingsDialogFragment.getInstance(this.mIPAddress, this.mPort, this.mConnectOnStartUp);
            frgNetworkSettingsDialog.show(ft, MainActivity.TAG_DIALOG_NETWORK_SETTINGS);
        }
        else if(event.getAction() == NavigationDrawerView.OSCMenuActionEvent.ACTION_ABOUT) {
            // Show Network settings gragment
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            Fragment prev = getSupportFragmentManager().findFragmentByTag(MainActivity.TAG_DIALOG_ABOUT_ME);
            if(prev != null) {
                ft.remove(prev);
            }
            ft.addToBackStack(null);

            final AboutMeDialogFragment frgAboutMeDialog = AboutMeDialogFragment.newInstance();
            frgAboutMeDialog.setRequestListener(new AboutMeDialogFragment.RequestListener() {
                public void onDonationsRequested() {
                    frgAboutMeDialog.dismiss();
                }
            });
            frgAboutMeDialog.show(ft, MainActivity.TAG_DIALOG_ABOUT_ME);
        }

        this.mDrawerLayout.closeDrawer(Gravity.START);
	}

    private void showExitConfirmDialog() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag(MainActivity.TAG_DIALOG_CONFIRM_EXIT);
        if(prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        final ConfirmDialogFragment frg = ConfirmDialogFragment.newInstance("Do you really want to exit?", "Exit", "Cancel");
        frg.setConfirmDialogResultListener(new ConfirmDialogFragment.ConfirmDialogResultListener() {
            @Override
            public void onPositiveSelected() {
                frg.dismiss();
                MainActivity.super.onBackPressed();
            }

            @Override
            public void onNegativeSelected() {
                frg.dismiss();
            }
        });
        frg.show(ft, MainActivity.TAG_DIALOG_CONFIRM_EXIT);
    }

    public void oscMenuItemTemplateSelected(String templateName) {

    }

    private void connectOSC() {
        try {
            OSCWrapper.getInstance(this, this.mIPAddress, this.mPort);
        }
        catch(Exception exp) {
            Toast.makeText(this, "I couldn't initialize OSC", Toast.LENGTH_SHORT).show();
        }
    }

	@Override
	public void onSaveFileSelected(File file) {

		String jsonTemplate = this.mOSCViewFragment.buildJSONString();
		if (!Utilities.write(jsonTemplate, file)) {
			Toast.makeText(this, "Error Saving Template", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(this, "Template Saved", Toast.LENGTH_SHORT).show();
		}

        String fn = file.getName();
        this.mCurrentFileName = fn.substring(0, fn.length() - 5);
        this.mDrawer.setCurrentTemplate(this.mCurrentFileName);
	}

	@Override
	public void onOpenFileSelected(String fileName) {
		this.mOSCViewFragment.inflateTemplate(this.mBaseFolder + File.separator + fileName);
		this.mOSCViewFragment.disableTemplateEditing();
		this.mCurrentFileName = fileName.substring(0, fileName.length() - 5);
        this.mDrawer.setCurrentTemplate(this.mCurrentFileName);
    }

    @Override
    public void onNetworkSettingsChanged(String ipAddress, int port, boolean connectOnStartUp) {

        this.mIPAddress = ipAddress;
        this.mPort = port;
        this.mConnectOnStartUp = connectOnStartUp;

        connectOSC();

        saveOSCNetworkSettings();
    }

    private void saveOSCNetworkSettings() {
        try {
            FileOutputStream fos = openFileOutput(NETWORK_SETTINGS_FILE, Context.MODE_PRIVATE);

            String data = this.mIPAddress + "#" + this.mPort + "#" + this.mConnectOnStartUp;
            fos.write(data.getBytes());
            fos.close();
        }
        catch(Exception exp) {
            Toast.makeText(this, "Could Not Update OSC Network Settings File", Toast.LENGTH_SHORT).show();
            exp.printStackTrace();
        }
    }

    private void restoreNetworkSettingsFromFile() {
        try {
            FileInputStream fis = openFileInput(NETWORK_SETTINGS_FILE);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[512];
            int bytes_read;
            while((bytes_read = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytes_read);
            }

            String data = new String(baos.toByteArray());
            String[] pieces = data.split("#");

            if(pieces.length != 3) {
                throw new Exception("Network Settings File Seems To Be Corrupt");
            }

            this.mIPAddress = pieces[0];
            this.mPort = Integer.parseInt(pieces[1]);
            this.mConnectOnStartUp = Boolean.parseBoolean(pieces[2]);

        }
        catch(FileNotFoundException fnfe) {}
        catch(Exception exp) {
            Toast.makeText(this, "Could Not Read OSC Network Settings File", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveSampleTemplateFiles(File directory) {
        String[] defaultTemplates = new String[] {
                "template1.json",
                "template2.json",
                "template3.json"
        };

        for(int i = 0; i < defaultTemplates.length; i += 1){
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(directory.getAbsolutePath() + "/" + defaultTemplates[i]);
                InputStream is = getAssets().open(defaultTemplates[i]);

                byte[] buffer = new byte[is.available()];
                is.read(buffer, 0, buffer.length);
                is.close();

                fos.write(buffer, 0, buffer.length);
            }
            catch(IOException ioe) {
                Log.d("MainActivity", "Unable to default template");
            }
            finally {
                try {
                    if(fos != null) {
                        fos.close();
                    }
                }
                catch(Exception e){}
            }

        }

    }

    /***
     * full screen immersive mode
     * requires API 19
     */
    private void makeImmersive() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
            | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    /***
     * regains fullscreen immersive mode after navigation bar and status bar
     * appears.
     * requires API 19
     */
    private void regainImmersive() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (Build.VERSION.SDK_INT >= 19 && hasFocus) {
            regainImmersive();
        }
    }

    /***
     * Overriding this method to show confirmation dialog as the user might have accidentally
     * pressed back button.
     * maybe I should add a feature to track changes and show save template option if the template
     * has unsaved changes.
     */
    @Override
    public void onBackPressed() {
        if(this.mOSCViewFragment.isSettingsActive()) {
            this.mOSCViewFragment.closeSettingsView();
            return;
        }

        showExitConfirmDialog();
    }
}
