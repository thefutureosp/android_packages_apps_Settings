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
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

import java.lang.Thread;
import java.lang.Runnable;


public class RomPreferences extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    
   private static final String TAG = "ParanoidAndroid/RomPreferences";

    private static final String CATEGORY_HYBRID_GENERAL = "category_hybrid_general";
    private static final String KEY_APP_LIST_SCREEN = "pref_manage_applications";
    private static final String KEY_STATUS_BAR_MODE = "pref_statusbar_mode";
    private static final String KEY_USER_MODE = "pref_user_mode";
    private static final String KEY_SYSTEM_DPI = "pref_system_dpi";
    private static final String KEY_USER_DPI = "pref_user_dpi";
    private static final String KEY_FRAME_DPI = "pref_framework_dpi";
    private static final String KEY_SYSUI_DPI = "pref_systemui_dpi";
    private static final String CUSTOM_LCD_DENSITY = "custom";

    private PreferenceCategory mPrefCategoryHybrid;
    private PreferenceScreen mAppList;
    private ListPreference mStatusBarMode;
    private ListPreference mUserMode;
    private ListPreference mSystemDpi;
    private ListPreference mUserDpi;
    private ListPreference mFrameDpi;
    private ListPreference mSysUiDpi;

    private static boolean mValue;
    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();

        if (getPreferenceManager() != null) {
            addPreferencesFromResource(R.xml.paranoid_preferences);
            PreferenceScreen prefSet = getPreferenceScreen();

            mStatusBarMode = (ListPreference) prefSet.findPreference(KEY_STATUS_BAR_MODE);
            mStatusBarMode.setOnPreferenceChangeListener(this);

            mUserMode = (ListPreference) prefSet.findPreference(KEY_USER_MODE);
            mUserMode.setOnPreferenceChangeListener(this);

            mSystemDpi = (ListPreference) prefSet.findPreference(KEY_SYSTEM_DPI);
            mSystemDpi.setOnPreferenceChangeListener(this);
            
            mUserDpi = (ListPreference) prefSet.findPreference(KEY_USER_DPI);
            mUserDpi.setOnPreferenceChangeListener(this);

            mFrameDpi = (ListPreference) prefSet.findPreference(KEY_FRAME_DPI);
            mFrameDpi.setOnPreferenceChangeListener(this);

            mSysUiDpi = (ListPreference) prefSet.findPreference(KEY_SYSUI_DPI);
            mSysUiDpi.setOnPreferenceChangeListener(this);
                    
            mAppList = (PreferenceScreen) prefSet.findPreference(KEY_APP_LIST_SCREEN);
            mPrefCategoryHybrid = (PreferenceCategory) findPreference(CATEGORY_HYBRID_GENERAL);
                    
            RomUtils.setContext(mContext);
         }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if(KEY_SYSTEM_DPI.equals(key)) {
            String value = (String) newValue;
            if(value.equals(CUSTOM_LCD_DENSITY))
                getDensityDialog("%system_default_dpi");
            else
                RomUtils.setHybridProperty("%system_default_dpi", value);
        } else if(KEY_USER_DPI.equals(key)) {
            String value = (String) newValue;
            if(value.equals(CUSTOM_LCD_DENSITY))
                getDensityDialog("%user_default_dpi");
            else
                RomUtils.setHybridProperty("%user_default_dpi", value);
        } else if(KEY_STATUS_BAR_MODE.equals(key)) {
            int value = Integer.parseInt((String) newValue);
            if(value == 1){
                Settings.System.putInt(getActivity().getContentResolver(), Settings.System.SENSE_RECENT, 0);
                RomUtils.setHybridProperty("com.android.systemui.dpi", "%rom_systemui_dpi");
                RomUtils.setHybridProperty("com.android.systemui.mode", "2");
            } else if(value == 2){
                RomUtils.setHybridProperty("com.android.systemui.dpi", "0");
                RomUtils.setHybridProperty("com.android.systemui.mode", "1");
            }
        } else if(KEY_USER_MODE.equals(key)) {
            String value = (String) newValue;
            RomUtils.setHybridProperty("%user_default_mode", value);
        } else if(KEY_FRAME_DPI.equals(key)) {
            String value = (String) newValue;
            if(value.equals(CUSTOM_LCD_DENSITY))
                getDensityDialog("android.dpi");
            else
                RomUtils.setHybridProperty("android.dpi", value);
        } else if(KEY_SYSUI_DPI.equals(key)) {
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
    
    public void getDensityDialog(String property) {
        getDensityDialog(property, -1);
    }

    public void getDensityDialog(final String property, final int trigger){
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
                        RomUtils.setHybridProperty(property, String.valueOf(mDensity));
                        if(trigger != -1)
                         RomUtils.triggerAction(trigger);
                    } catch (NumberFormatException e){
                        Toast.makeText(mContext, getString(R.string.dpi_no_value), Toast.LENGTH_LONG).show();
                    }
                   }
              });
        alert.show();
    }

}
