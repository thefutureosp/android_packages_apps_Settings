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

public class RecentPreferences extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    
   private static final String TAG = "RecentPreferences";

    private static final String CATEGORY_RECENTS_GENERAL = "category_recent_general";
    private static final String KEY_SENSE_RECENT = "pref_sense_recent";
    private static final String KEY_ICON = "pref_recent_apps_icon";
    private static final String KEY_TEXT_SIZE = "pref_recent_text_size";

    private PreferenceCategory mPrefCategoryRecent;
    private ListPreference mRecentTextSize;
    private CheckBoxPreference mRecentIcon;
    private CheckBoxPreference mSenseRecent;

    private static boolean mValue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getPreferenceManager() != null) {
            addPreferencesFromResource(R.xml.recent_apps_settings);
            PreferenceScreen prefSet = getPreferenceScreen();

            mSenseRecent = (CheckBoxPreference) prefSet.findPreference(KEY_SENSE_RECENT);
            mSenseRecent.setOnPreferenceChangeListener(this);
            mSenseRecent.setChecked(Settings.System.getInt(getActivity().getContentResolver(), Settings.System.SENSE_RECENT, 0) == 1);

            mRecentTextSize = (ListPreference) prefSet.findPreference(KEY_TEXT_SIZE);
            mRecentTextSize.setOnPreferenceChangeListener(this);
            mRecentTextSize.setValue(Integer.toString(Settings.System.getInt(getActivity().getContentResolver(), Settings.System.RECENTS_TEXT_SIZE, 14)));

            mRecentIcon = (CheckBoxPreference) prefSet.findPreference(KEY_ICON);
            mRecentIcon.setOnPreferenceChangeListener(this);
            mRecentIcon.setChecked(Settings.System.getInt(getActivity().getContentResolver(), Settings.System.RECENTS_ICON, 1) == 1);

            RomUtils.setContext(getActivity());

                mPrefCategoryRecent = (PreferenceCategory) findPreference(CATEGORY_RECENTS_GENERAL);
            
            if(Utils.isScreenLarge()){
                mPrefCategoryRecent.removePreference(mSenseRecent);
            }

        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mSenseRecent){
            mValue = mSenseRecent.isChecked();
            Settings.System.putInt(getActivity().getContentResolver(), Settings.System.SENSE_RECENT, mValue ? 1 : 0);
        } else if (preference == mRecentIcon){
            mValue = mRecentIcon.isChecked();
            Settings.System.putInt(getActivity().getContentResolver(), Settings.System.RECENTS_ICON, mValue ? 1 : 0);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
    
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (KEY_TEXT_SIZE.equals(key)) {
            int value = Integer.parseInt((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(), Settings.System.RECENTS_TEXT_SIZE, value);
        }
        return true;
    }

}
