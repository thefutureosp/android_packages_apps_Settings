/*
 * ParanoidAndroid Main Preferences Fragment. (c) 2012 D4rKn3sSyS
 */

package com.android.settings.paranoid;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.provider.Settings;
import android.app.AlertDialog;
import android.os.SystemProperties;

import com.android.settings.Utils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.io.File;

public class RomPreferences extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    
   private static final String TAG = "ParanoidAndroid/RomPreferences";

    private static final String CATEGORY_UI = "category_ui";
    private static final String KEY_OTA_UPDATES = "pref_ota_updates";
    private static final String KEY_STATUSBAR_TRANSPARENCY = "pref_statusbar_transparency";
    private static final String KEY_SOFT_KEYS = "pref_soft_keys";
    private static final String KEY_TABLET_MODE = "pref_tablet_mode";
    private static final String KEY_MP4_RECORDING = "pref_recording_format";
    private static final String KEY_PHYSICAL = "pref_physical_keys";

    private PreferenceCategory mPrefCategoryUi;
    private Preference mOtaUpdates;
    private ListPreference mStatusbarTransparency;
    private CheckBoxPreference mSoftKeys;
    private CheckBoxPreference mTabletMode;
    private CheckBoxPreference mMpeg4Recording;
    private CheckBoxPreference mPhysicalKeys;
	
    private static boolean mValue;

    private static Context mContext;	

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();

        if (getPreferenceManager() != null) {
		addPreferencesFromResource(R.xml.paranoid_preferences);
		PreferenceScreen prefSet = getPreferenceScreen();

 
		mTabletMode = (CheckBoxPreference) prefSet.findPreference(KEY_TABLET_MODE);
		mTabletMode.setOnPreferenceChangeListener(this);

		boolean IsTab = SystemProperties.getInt("ro.sf.lcd_density", 160) <= Integer.parseInt(RomUtils.getProperty("rom_tablet_base", ""));
		mTabletMode.setChecked(IsTab);
	       
        RomUtils.setContext(mContext);
	}
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		if (preference == mTabletMode){
	    	mValue = mTabletMode.isChecked();
            RomUtils.setPropierty("ro.sf.lcd_density", mValue ? Integer.parseInt(RomUtils.getProperty("rom_tablet_base", "")) : Integer.parseInt(RomUtils.getProperty("rom_phone_base", "")));
			RomUtils.setPropierty("qemu.hw.mainkeys", mValue ? 1 : 0);
		    getRequiredDialog(R.string.requires_reboot, 0);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
	String key = preference.getKey();
        return true;
    }

    
    public void getRequiredDialog(final int message, final int action) {
	final Context mContext = getActivity();
	AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
            .setMessage(message)
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
		    RomUtils.triggerAction(action);
                }
            })
            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
	            dialog.dismiss();
                }
            });
	AlertDialog mAlert = builder.create();
	mAlert.show();
    }      
        
}
