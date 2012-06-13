/**
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.android.settings.paranoid.applications;

import com.android.settings.R;
import com.android.settings.paranoid.applications.ApplicationsState.AppEntry;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.usb.IUsbManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox; 
import android.widget.CompoundButton; 
import android.widget.CompoundButton.OnCheckedChangeListener; 
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import android.content.ComponentName;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.text.method.DigitsKeyListener;
import android.widget.EditText;

import com.android.settings.paranoid.RomUtils;

/*
 * Modified by ParanoidAndroid Team 
 */
public class InstalledAppDetails extends Fragment
        implements CompoundButton.OnCheckedChangeListener,
        ApplicationsState.Callbacks {
    private static final String TAG="InstalledAppDetails";
    static final boolean SUPPORT_DISABLE_APPS = true;
    private static final boolean localLOGV = false;    
    public static final String REMOVE_ENTRY = "-1";
    public static final String CUSTOM_VALUE = "custom";
    private static final int MENU_APPLY = Menu.FIRST;
    private static final int MENU_DISCARD = Menu.FIRST + 1;
    
    public static final String ARG_PACKAGE_NAME = "package";

    private PackageManager mPm;
    private IUsbManager mUsbManager;
    private DevicePolicyManager mDpm;
    private ApplicationsState mState;
    private ApplicationsState.AppEntry mAppEntry;
    private PackageInfo mPackageInfo;
    private View mRootView;
    private boolean mMoveInProgress = false;
    private TextView mAppVersion;
    private Menu mOptionsMenu;

    //PARANOIDANDROID: PAD & PAL
    private TextView mDpiText;
    private TextView mLayoutText;
    private TextView mForceText;
    private Spinner mDensity;
    private Spinner mLayoutMode;
    private CheckBox mForceScaling;

    private String[] mArrayHeadersPad;
    private String[] mArrayValuesPad;
    private String[] mArrayHeadersPal;
    private String[] mArrayValuesPal;   

    private String mSelectedDensity;
    private String mSelectedLayout;
    private String mSelectedForce;
    
    // Dialog identifiers used in showDialog
    private static final int DLG_BASE = 0;
    private static final int DLG_FORCE_STOP = DLG_BASE + 5;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);        
        mState = ApplicationsState.getInstance(getActivity().getApplication());
        mPm = getActivity().getPackageManager();
        IBinder b = ServiceManager.getService(Context.USB_SERVICE);
        mUsbManager = IUsbManager.Stub.asInterface(b);
        mDpm = (DevicePolicyManager)getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
        new CanBeOnSdCardChecker();
		setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = mRootView = inflater.inflate(R.layout.paranoid_installed_app_details, null);
        
        getActivity().getText(R.string.computing_size);
        
	// PARANOIDANDROID: PAD & PAL
	mDpiText = (TextView) view.findViewById(R.id.dpi_text);
	mLayoutText = (TextView) view.findViewById(R.id.layout_text);
	mForceText = (TextView) view.findViewById(R.id.force_text);

	mArrayHeadersPad = getActivity().getResources().getStringArray(R.array.entries_pad_lcd_densities);
	mArrayValuesPad  = getActivity().getResources().getStringArray(R.array.values_pad_lcd_densities);
	mArrayHeadersPal  = getActivity().getResources().getStringArray(R.array.entries_layout_modes);
	mArrayValuesPal  = getActivity().getResources().getStringArray(R.array.values_layout_modes);

	// DENSITY TAP
   	mDensity = (Spinner) view.findViewById(R.id.pad_spinner);
   	ArrayAdapter mPadAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.entries_pad_lcd_densities, android.R.layout.simple_spinner_item);
   	mPadAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
   	mDensity.setAdapter(mPadAdapter);

	// LAYOUT TAP
	mLayoutMode = (Spinner) view.findViewById(R.id.pal_spinner);
   	ArrayAdapter mPalAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.entries_layout_modes, android.R.layout.simple_spinner_item);
   	mPalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
   	mLayoutMode.setAdapter(mPalAdapter);

	// FORCE TAP
	mForceScaling = (CheckBox) view.findViewById(R.id.force_scaling); 	

    return view;
    }

    private int getSelectionPAD(String value){
		if(value.equals("0") || value.equals(REMOVE_ENTRY))
			return 0;
		for(int i=0; i<mArrayValuesPad.length; i++)
			if(mArrayValuesPad[i].equals(value))
				return i;
		return mArrayValuesPad.length - 1;
    }

    private int getSelectionPAL(String value){
		if(value.equals("0") || value.equals(REMOVE_ENTRY))
			return 0;
		for(int i=0; i<mArrayValuesPal.length; i++)
			if(mArrayValuesPal[i].equals(value))
				return i;
		return 0;
    }

    private void refreshParanoidParameters() {
	// GET FRESH BATCH OF PROPS
	String TempDpi = RomUtils.getFixedProperty(mAppEntry.info.packageName + ".dpi", REMOVE_ENTRY);
	String TempLay = RomUtils.getProperty(mAppEntry.info.packageName + ".mode", REMOVE_ENTRY);
	String TempFor = RomUtils.getProperty(mAppEntry.info.packageName + ".force", REMOVE_ENTRY);
	int TempDpiState = getSelectionPAD(TempDpi);
	int TempLayState = getSelectionPAL(TempLay);

	// SET UI ELEMENTS ACCORDINGLY
	mDpiText.setText(mArrayHeadersPad[TempDpiState].equals(CUSTOM_VALUE) ? TempDpi : mArrayHeadersPad[TempDpiState]);
	mLayoutText.setText(mArrayHeadersPal[TempLayState]);
	mForceText.setText(TempFor.equals("1") ? getActivity().getString(R.string.force_yes) : getActivity().getString(R.string.force_no));

	// SET SETTINGS CONTROLS
	mDensity.setSelection(TempDpiState);
	mLayoutMode.setSelection(TempLayState);		
   	mForceScaling.setChecked(TempFor.equals("1"));
    }

    public void resetSelectedOptions(){
	// RESET PROP VALUES
	RomUtils.setHybridProperty(mAppEntry.info.packageName + ".dpi", REMOVE_ENTRY);
	RomUtils.setHybridProperty(mAppEntry.info.packageName + ".mode", REMOVE_ENTRY);
	RomUtils.setHybridProperty(mAppEntry.info.packageName + ".force", REMOVE_ENTRY);

	// REFRESH
	refreshParanoidParameters();
        showDialogInner(DLG_FORCE_STOP, 0);
    }

    public void applySelectedOptions(){
	// GET STATES		
	mSelectedDensity = mArrayValuesPad[mDensity.getSelectedItemPosition()];
	mSelectedLayout = mArrayValuesPal[mLayoutMode.getSelectedItemPosition()];
	mSelectedForce = mForceScaling.isChecked() ? "1" : REMOVE_ENTRY;

	// CUSTOM VALUE?
	if (mSelectedDensity.equals(CUSTOM_VALUE)) {
             AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
             final EditText input = new EditText(getActivity());
             DigitsKeyListener onlyDecimalAllowed = new DigitsKeyListener(true, true);
             input.setKeyListener(onlyDecimalAllowed);
             alert.setView(input)
             .setTitle(R.string.dpi_custom_value)
             .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
             public void onClick(DialogInterface dialog, int whichButton) {
                  String mNewDensity = input.getText().toString();				
                  RomUtils.setHybridProperty(mAppEntry.info.packageName + ".dpi", mNewDensity);
                  RomUtils.setHybridProperty(mAppEntry.info.packageName + ".mode", mSelectedLayout);
                  RomUtils.setHybridProperty(mAppEntry.info.packageName + ".force", mSelectedForce);
                  refreshParanoidParameters();
                  showDialogInner(DLG_FORCE_STOP, 0);
             }});
             alert.show();
	} 

	// NORMAL VALUE
	else {
             RomUtils.setHybridProperty(mAppEntry.info.packageName + ".dpi", mSelectedDensity);
             RomUtils.setHybridProperty(mAppEntry.info.packageName + ".mode", mSelectedLayout);
             RomUtils.setHybridProperty(mAppEntry.info.packageName + ".force", mSelectedForce);
             refreshParanoidParameters();
             showDialogInner(DLG_FORCE_STOP, 0);
   	}
    } 

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_APPLY:
            	applySelectedOptions();
                return true;
            case MENU_DISCARD:
            	resetSelectedOptions();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	 mOptionsMenu = menu;
         menu.add(Menu.NONE, MENU_APPLY, 0, R.string.action_apply)
                 .setIcon(android.R.drawable.ic_menu_save)
                 .setEnabled(true)
                 .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
         menu.add(Menu.NONE, MENU_DISCARD, 0, R.string.action_reset)
		 .setIcon(android.R.drawable.ic_menu_close_clear_cancel)
                 .setEnabled(true)
                 .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
 	 super.onCreateOptionsMenu(menu, inflater);
    }

    // Utility method to set application label and icon.
    private void setAppLabelAndIcon(PackageInfo pkgInfo) {
        View appSnippet = mRootView.findViewById(R.id.app_snippet);
        ImageView icon = (ImageView) appSnippet.findViewById(R.id.app_icon);
        mState.ensureIcon(mAppEntry);
        icon.setImageDrawable(mAppEntry.icon);
        // Set application name.
        TextView label = (TextView) appSnippet.findViewById(R.id.app_name);
        label.setText(mAppEntry.label);
        // Version number of application
        mAppVersion = (TextView) appSnippet.findViewById(R.id.app_size);

        if (pkgInfo != null && pkgInfo.versionName != null) {
            mAppVersion.setVisibility(View.VISIBLE);
            mAppVersion.setText(getActivity().getString(R.string.version_text,
                    String.valueOf(pkgInfo.versionName)));
        } else {
            mAppVersion.setVisibility(View.INVISIBLE);
        }
    }

	private boolean refreshUi() {
        if (mMoveInProgress) {
            return true;
        }
        final Bundle args = getArguments();
        String packageName = (args != null) ? args.getString(ARG_PACKAGE_NAME) : null;
        if (packageName == null) {
            Intent intent = (args == null) ?
                    getActivity().getIntent() : (Intent) args.getParcelable("intent");
            if (intent != null) {
                packageName = intent.getData().getSchemeSpecificPart();
            }
        }
        mAppEntry = mState.getEntry(packageName);
        
        if (mAppEntry == null) {
            return false; // onCreate must have failed, make sure to exit
        }
        
        // Get application info again to refresh changed properties of application
        try {
            mPackageInfo = mPm.getPackageInfo(mAppEntry.info.packageName,
                    PackageManager.GET_DISABLED_COMPONENTS |
                    PackageManager.GET_UNINSTALLED_PACKAGES |
                    PackageManager.GET_SIGNATURES);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Exception when retrieving package:" + mAppEntry.info.packageName, e);
            return false; // onCreate must have failed, make sure to exit
        }
        
        // Get list of preferred activities
        List<ComponentName> prefActList = new ArrayList<ComponentName>();
        
        // Intent list cannot be null. so pass empty list
        List<IntentFilter> intentList = new ArrayList<IntentFilter>();
        mPm.getPreferredActivities(intentList, prefActList, packageName);
        if(localLOGV) Log.i(TAG, "Have "+prefActList.size()+" number of activities in prefered list");
        try {
            mUsbManager.hasDefaults(packageName);
        } catch (RemoteException e) {
            Log.e(TAG, "mUsbManager.hasDefaults", e);
        }
        
	// SET APP OUTFIT
        setAppLabelAndIcon(mPackageInfo);

	// REFRESH UI ELEMENTS
        refreshParanoidParameters();
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        
        mState.resume(this);
        if (!refreshUi()) {
            setIntentAndFinish(true, true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mState.pause();
    }

    @Override
    public void onAllSizesComputed() {
    }

    @Override
    public void onPackageIconChanged() {
    }

    @Override
    public void onPackageListChanged() {
        refreshUi();
    }

    @Override
    public void onRebuildComplete(ArrayList<AppEntry> apps) {
    }

    @Override
    public void onPackageSizeChanged(String packageName) {
    }

    @Override
    public void onRunningStateChanged(boolean running) {
    }
   
    private void setIntentAndFinish(boolean finish, boolean appChanged) {
        if(localLOGV) Log.i(TAG, "appChanged="+appChanged);
        Intent intent = new Intent();
        intent.putExtra(ManageApplications.APP_CHG, appChanged);
        PreferenceActivity pa = (PreferenceActivity)getActivity();
        pa.finishPreferencePanel(this, Activity.RESULT_OK, intent);
    }
    
    private void showDialogInner(int id, int moveErrorCode) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id, moveErrorCode);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }
    
    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id, int moveErrorCode) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            args.putInt("moveError", moveErrorCode);
            frag.setArguments(args);
            return frag;
        }

        InstalledAppDetails getOwner() {
            return (InstalledAppDetails)getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            getArguments().getInt("moveError");
            switch (id) {
                case DLG_FORCE_STOP:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(getActivity().getText(R.string.hybrid_force_stop_dlg_title))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(getActivity().getText(R.string.hybrid_force_stop_dlg_text))
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Force stop
                            getOwner().forceStopPackage(getOwner().mAppEntry.info.packageName);
                            Toast.makeText(getActivity(), R.string.actions_applied, Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton(R.string.dlg_cancel, null)
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }
    }

    private void forceStopPackage(String pkgName) {
        ActivityManager am = (ActivityManager)getActivity().getSystemService(
                Context.ACTIVITY_SERVICE);
        am.forceStopPackage(pkgName);
        mState.invalidatePackage(pkgName);
        ApplicationsState.AppEntry newEnt = mState.getEntry(pkgName);
        if (newEnt != null) {
            mAppEntry = newEnt;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {}
}

