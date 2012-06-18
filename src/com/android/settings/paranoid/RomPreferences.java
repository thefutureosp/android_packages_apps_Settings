/*
 * Copyright (C) 2012 ParanoidAndroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.paranoid;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.provider.Settings;
import android.text.method.DigitsKeyListener;
import android.widget.EditText;
import android.widget.Toast;

import com.android.settings.Utils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.android.settings.Utils;

public class RomPreferences extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    
   private static final String TAG = "ParanoidAndroid/RomPreferences";

    private static final String CATEGORY_HYBRID_GENERAL = "category_hybrid_general";
    private static final String KEY_APP_LIST_SCREEN = "pref_manage_applications";

    private static final String KEY_TABLET_MODE = "pref_tablet_mode";
    private static final String KEY_SYSTEM_DENSITY = "pref_system_density";
    private static final String KEY_USER_DENSITY = "pref_user_density";
    private static final String KEY_USER_MODE = "pref_user_mode";
    private static final String KEY_FRAME_DENSITY = "pref_framework_density";
    private static final String KEY_SYSUI_DENSITY = "pref_systemui_density";
    private static final String KEY_ENABLE_HYBRID = "pref_enable_hybrid";
    private static final String CUSTOM_LCD_DENSITY = "-1";

    private PreferenceCategory mPrefCategoryHybrid;
    private PreferenceScreen mAppList;
    private CheckBoxPreference mTabletMode;
    private ListPreference mGlobalLcdDensity;
    private ListPreference mLcdDensity;
    private ListPreference mUserMode;
    private ListPreference mFrameDensity;
    private ListPreference mSysUiDensity;
    private CheckBoxPreference mEnableHybrid;

    private static boolean mValue;
    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();

        if (getPreferenceManager() != null) {
		addPreferencesFromResource(R.xml.paranoid_preferences);
		PreferenceScreen prefSet = getPreferenceScreen();

                mTabletMode = (CheckBoxPreference) prefSet.findPreference(KEY_TABLET_MODE);
                mTabletMode.setOnPreferenceChangeListener(this);
                mTabletMode.setChecked(Utils.isScreenLarge());

		mEnableHybrid = (CheckBoxPreference) prefSet.findPreference(KEY_ENABLE_HYBRID);
		mEnableHybrid.setOnPreferenceChangeListener(this);
                mEnableHybrid.setChecked(Integer.parseInt(RomUtils.getProperty("hybrid_mode")) == 1);

		mGlobalLcdDensity = (ListPreference) prefSet.findPreference(KEY_SYSTEM_DENSITY);
		mGlobalLcdDensity.setOnPreferenceChangeListener(this);
		mGlobalLcdDensity.setValue(RomUtils.getProperty("system_default_dpi"));
                mGlobalLcdDensity.setEnabled(mEnableHybrid.isChecked());	

		mLcdDensity = (ListPreference) prefSet.findPreference(KEY_USER_DENSITY);
		mLcdDensity.setOnPreferenceChangeListener(this);
		mLcdDensity.setValue(RomUtils.getProperty("user_default_dpi"));
                mLcdDensity.setEnabled(mEnableHybrid.isChecked());

		mUserMode = (ListPreference) prefSet.findPreference(KEY_USER_MODE);
		mUserMode.setOnPreferenceChangeListener(this);
		mUserMode.setValue(RomUtils.getProperty("user_default_mode"));
                mUserMode.setEnabled(mEnableHybrid.isChecked());

		mFrameDensity = (ListPreference) prefSet.findPreference(KEY_FRAME_DENSITY);
		mFrameDensity.setOnPreferenceChangeListener(this);
		mFrameDensity.setValue(RomUtils.getProperty("android.dpi"));
                mFrameDensity.setEnabled(mEnableHybrid.isChecked());

		mSysUiDensity = (ListPreference) prefSet.findPreference(KEY_SYSUI_DENSITY);
		mSysUiDensity.setOnPreferenceChangeListener(this);
		mSysUiDensity.setValue(RomUtils.getProperty("com.android.systemui.dpi"));
		mSysUiDensity.setEnabled(mEnableHybrid.isChecked());
                
                mAppList = (PreferenceScreen) prefSet.findPreference(KEY_APP_LIST_SCREEN);
                mAppList.setEnabled(mEnableHybrid.isChecked());
		
		RomUtils.setContext(mContext);

                mPrefCategoryHybrid = (PreferenceCategory) findPreference(CATEGORY_HYBRID_GENERAL);
	}
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mTabletMode){
	    mValue = mTabletMode.isChecked();
            RomUtils.setPropierty("ro.sf.lcd_density", mValue ? Integer.parseInt(RomUtils.getProperty("rom_tablet_base", "")) : Integer.parseInt(RomUtils.getProperty("rom_phone_base", "")));
	    if(mValue)
	    	Settings.System.putInt(getActivity().getContentResolver(), Settings.System.SENSE_RECENT, 0);
        } else if (preference == mEnableHybrid){
            mValue = mEnableHybrid.isChecked();
            RomUtils.setHybridProperty("hybrid_mode", mValue ? "1" : "0");
            mGlobalLcdDensity.setEnabled(mEnableHybrid.isChecked());
            mLcdDensity.setEnabled(mEnableHybrid.isChecked());
            mUserMode.setEnabled(mEnableHybrid.isChecked());
            mFrameDensity.setEnabled(mEnableHybrid.isChecked());
            mSysUiDensity.setEnabled(mEnableHybrid.isChecked());
            mAppList.setEnabled(mEnableHybrid.isChecked());
	}
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
	String key = preference.getKey();
        if(KEY_USER_DENSITY.equals(key)){
	    String value = (String) newValue;
            if(value.equals(CUSTOM_LCD_DENSITY))
	       getDensityDialog("user_default_dpi", -1);
            else
	       RomUtils.setHybridProperty("user_default_dpi", String.valueOf(value));
        } else if(KEY_USER_MODE.equals(key)) {
            String value = (String) newValue;
            RomUtils.setHybridProperty("user_default_mode", value);
	} else if(KEY_SYSTEM_DENSITY.equals(key)) {
            String value = (String) newValue;
            if(value.equals(CUSTOM_LCD_DENSITY))
                getDensityDialog("system_default_dpi", -1);
            else
                RomUtils.setHybridProperty("system_default_dpi", value);
	} else if(KEY_FRAME_DENSITY.equals(key)) {
            String value = (String) newValue;
            if(value.equals(CUSTOM_LCD_DENSITY))
                getDensityDialog("android.dpi", 0);
            else
                RomUtils.setHybridProperty("android.dpi", value);
	} else if(KEY_SYSUI_DENSITY.equals(key)) {
            String value = (String) newValue;
            if(value.equals(CUSTOM_LCD_DENSITY))
                getDensityDialog("com.android.systemui.dpi", 1);
            else {
                RomUtils.setHybridProperty("com.android.systemui.dpi", value);
                RomUtils.triggerAction(1);
            }
	} 
	return true;
    }

    public void getDensityDialog(final String propierty, final int requiredDialog){
	AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
        final EditText input = new EditText(mContext);
        DigitsKeyListener onlyDecimalAllowed = new DigitsKeyListener(true, true);
               input.setKeyListener(onlyDecimalAllowed);
               alert.setView(input)
                   .setTitle(R.string.dpi_custom_value)
                   .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
                   public void onClick(DialogInterface dialog, int whichButton) {
                       String value = input.getText().toString();
                       int mDensity;
                       try{
                           mDensity = Integer.parseInt(value);
                           RomUtils.setHybridProperty(propierty, String.valueOf(mDensity));
		           if(requiredDialog == 0)
			        getRequiredDialog(R.string.requires_reboot, 0);
		           if(requiredDialog == 1)
			        RomUtils.triggerAction(1);
                       } catch (Exception e){
                           Toast.makeText(mContext, getString(R.string.lcd_density_no_value), Toast.LENGTH_LONG).show();
                       }
		
        }});
        alert.show();
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

