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

package com.android.settings.cyanogenmod;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class NavBar extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "NavBarSettings";

    private static final String KEY_SOFT_KEYS = "pref_soft_keys";
    private static final String KEY_NAV_BAR_EDITOR = "navigation_bar";

    private CheckBoxPreference mSoftKeys;
    private PreferenceScreen mNavBarEditor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getPreferenceManager() != null) {
            addPreferencesFromResource(R.xml.navbar_settings);

            PreferenceScreen prefSet = getPreferenceScreen();

            mSoftKeys = (CheckBoxPreference) prefSet.findPreference(KEY_SOFT_KEYS);
            if (mSoftKeys != null) {
		mSoftKeys.setOnPreferenceChangeListener(this);
     	        mSoftKeys.setChecked(Settings.System.getInt(getActivity().getContentResolver(), 
                    Settings.System.SOFT_KEYS, 0) == 1);
            }

            mNavBarEditor = (PreferenceScreen) prefSet.findPreference(KEY_NAV_BAR_EDITOR);
            mNavBarEditor.setEnabled(mSoftKeys.isChecked());
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mSoftKeys){
            boolean mValue = mSoftKeys.isChecked();
            Settings.System.putInt(getActivity().getContentResolver(), Settings.System.SOFT_KEYS, mValue ? 1 : 0);
            mNavBarEditor.setEnabled(mSoftKeys.isChecked());
	}
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }

}
