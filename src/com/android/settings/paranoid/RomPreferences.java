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
    private static final String KEY_LOCKSCREEN_VIBRATION = "pref_lockscreen_vibration";
    private static final String KEY_MP4_RECORDING = "pref_recording_format";
    private static final String KEY_PHYSICAL = "pref_physical_keys";

    private PreferenceCategory mPrefCategoryUi;
    private Preference mOtaUpdates;
    private ListPreference mStatusbarTransparency;
    private CheckBoxPreference mSoftKeys;
    private CheckBoxPreference mTabletMode;
    private CheckBoxPreference mLockscreenVibration;
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

		mOtaUpdates = prefSet.findPreference(KEY_OTA_UPDATES);
	        mOtaUpdates.setSummary(mOtaUpdates.getSummary() + " v"+ RomUtils.getProp("ro.paranoid.shortversion"));

		mStatusbarTransparency = (ListPreference) prefSet.findPreference(KEY_STATUSBAR_TRANSPARENCY);
		mStatusbarTransparency.setOnPreferenceChangeListener(this);
		mStatusbarTransparency.setValue(Integer.toString(Settings.System.getInt(getActivity().getContentResolver(), Settings.System.STATUS_BAR_TRANSPARENCY, 100)));

		mSoftKeys = (CheckBoxPreference) prefSet.findPreference(KEY_SOFT_KEYS);
		mSoftKeys.setOnPreferenceChangeListener(this);
		mSoftKeys.setChecked(Settings.System.getInt(getActivity().getContentResolver(), Settings.System.SOFT_KEYS, 0) == 1);
 
		mTabletMode = (CheckBoxPreference) prefSet.findPreference(KEY_TABLET_MODE);
		mTabletMode.setOnPreferenceChangeListener(this);
		mTabletMode.setChecked(Utils.isScreenLarge());

		mLockscreenVibration = (CheckBoxPreference) prefSet.findPreference(KEY_LOCKSCREEN_VIBRATION);
		mLockscreenVibration.setOnPreferenceChangeListener(this);
		mLockscreenVibration.setChecked(Settings.System.getInt(getActivity().getContentResolver(), Settings.System.LOCKSCREEN_VIBRATION, 1) == 1);
	        
                mPrefCategoryUi = (PreferenceCategory) findPreference(CATEGORY_UI);
               
                if(Utils.isScreenLarge()){
                    mPrefCategoryUi.removePreference(mStatusbarTransparency);
                }

	        RomUtils.setContext(mContext);
	}
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
	if (preference == mSoftKeys){
            mValue = mSoftKeys.isChecked();
            Settings.System.putInt(getActivity().getContentResolver(), Settings.System.SOFT_KEYS, mValue ? 1 : 0);
	} else if (preference == mTabletMode){
	    mValue = mTabletMode.isChecked();
            RomUtils.setPropierty("ro.sf.lcd_density", mValue ? Integer.parseInt(RomUtils.getProperty("rom_tablet_base", "")) : Integer.parseInt(RomUtils.getProperty("rom_phone_base", "")));
	    if(mValue)
	    	Settings.System.putInt(getActivity().getContentResolver(), Settings.System.SENSE_RECENT, 0);
	    getRequiredDialog(R.string.requires_reboot, 0);
        } else  if (preference == mLockscreenVibration){
            mValue = mLockscreenVibration.isChecked();
            Settings.System.putInt(getActivity().getContentResolver(), Settings.System.LOCKSCREEN_VIBRATION, mValue ? 1 : 0);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
	String key = preference.getKey();
	if (KEY_STATUSBAR_TRANSPARENCY.equals(key)) {
            int value = Integer.parseInt((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(), Settings.System.STATUS_BAR_TRANSPARENCY, value);
	}
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
