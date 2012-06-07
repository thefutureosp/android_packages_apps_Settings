/*
 * ParanoidAndroid P.A.D Preference Fragment. (c) 2012 ParanoidAndroid Team
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
import android.text.method.DigitsKeyListener;
import android.widget.EditText;
import android.widget.Toast;
import com.android.settings.Utils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import android.os.SystemProperties;

public class HybridPreferences extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    
   private static final String TAG = "ParanoidAndroid/HybridPreferences";

    private static final String CATEGORY_HYBRID_GENERAL = "category_hybrid_general";
    private static final String KEY_APP_LIST_SCREEN = "pref_manage_applications";
    private static final String KEY_GLOBAL_DENSITY = "pref_global_density";
    private static final String KEY_LCD_DENSITY = "pref_lcd_density";
    private static final String KEY_FRAME_DENSITY = "pref_framework_density";
    private static final String KEY_SYSUI_DENSITY = "pref_systemui_density";
    private static final String KEY_ENABLE_HYBRID = "pref_enable_hybrid";
    private static final int SYSTEM_DEFAULT_DPI = 192;
    private static final int USER_DEFAULT_DPI = 260;
    private static final int FRAMEWORK_DEFAULT_DPI = 192;
    private static final int SYSTEMUI_DEFAULT_DPI = 192;
    private static final int CUSTOM_LCD_DENSITY = -1;

    private PreferenceCategory mPrefCategoryHybrid;
    private PreferenceScreen mAppList;
    private ListPreference mGlobalLcdDensity;
    private ListPreference mLcdDensity;
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
		addPreferencesFromResource(R.xml.hybrid_settings);
		PreferenceScreen prefSet = getPreferenceScreen();

		mEnableHybrid = (CheckBoxPreference) prefSet.findPreference(KEY_ENABLE_HYBRID);
		mEnableHybrid.setOnPreferenceChangeListener(this);

        	mEnableHybrid.setChecked(Integer.parseInt(RomUtils.getProperty("hybrid_mode","0")) == 1);

		mGlobalLcdDensity = (ListPreference) prefSet.findPreference(KEY_GLOBAL_DENSITY);
		mGlobalLcdDensity.setOnPreferenceChangeListener(this);
		mGlobalLcdDensity.setValue(RomUtils.getProperty("system_default_dpi", String.valueOf(SYSTEM_DEFAULT_DPI)));	

		mLcdDensity = (ListPreference) prefSet.findPreference(KEY_LCD_DENSITY);
		mLcdDensity.setOnPreferenceChangeListener(this);
		mLcdDensity.setValue(RomUtils.getProperty("user_default_dpi", String.valueOf(USER_DEFAULT_DPI)));
	        mLcdDensity.setEnabled(mEnableHybrid.isChecked());

		mFrameDensity = (ListPreference) prefSet.findPreference(KEY_FRAME_DENSITY);
		mFrameDensity.setOnPreferenceChangeListener(this);
		mFrameDensity.setValue(RomUtils.getProperty("framework-res.dpi", String.valueOf(FRAMEWORK_DEFAULT_DPI)));

		mSysUiDensity = (ListPreference) prefSet.findPreference(KEY_SYSUI_DENSITY);
		mSysUiDensity.setOnPreferenceChangeListener(this);
		mSysUiDensity.setValue(RomUtils.getProperty("com.android.systemui.dpi", String.valueOf(SYSTEMUI_DEFAULT_DPI)));
		mSysUiDensity.setEnabled(mEnableHybrid.isChecked());
                
                mAppList = (PreferenceScreen) prefSet.findPreference(KEY_APP_LIST_SCREEN);
                mAppList.setEnabled(mEnableHybrid.isChecked());
		
		RomUtils.setContext(mContext);

                mPrefCategoryHybrid = (PreferenceCategory) findPreference(CATEGORY_HYBRID_GENERAL);
		mPrefCategoryHybrid.removePreference(mFrameDensity);
	}
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
	if (preference == mEnableHybrid){
            mValue = mEnableHybrid.isChecked();
            RomUtils.setHybridProperty("hybrid_mode", mValue ? "1" : "0");
			mGlobalLcdDensity.setEnabled(mEnableHybrid.isChecked());
            mLcdDensity.setEnabled(mEnableHybrid.isChecked());
		    mSysUiDensity.setEnabled(mEnableHybrid.isChecked());
            mAppList.setEnabled(mEnableHybrid.isChecked());
	}
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
	 String key = preference.getKey();
	 if(KEY_LCD_DENSITY.equals(key)){
	    int value = Integer.parseInt((String) newValue);
            if(value == CUSTOM_LCD_DENSITY)
	       getDensityDialog(USER_DEFAULT_DPI, "user_default_dpi", 0);
            else
	       RomUtils.setHybridProperty("user_default_dpi", String.valueOf(value));
	} else if(KEY_GLOBAL_DENSITY.equals(key)){
		int value = Integer.parseInt((String) newValue);
            if(value == CUSTOM_LCD_DENSITY)
	       getDensityDialog(SYSTEM_DEFAULT_DPI, "system_default_dpi", 0);
            else
	       RomUtils.setHybridProperty("system_default_dpi", String.valueOf(value));
	} else if(KEY_FRAME_DENSITY.equals(key)){
	    int value = Integer.parseInt((String) newValue);
            if(value == CUSTOM_LCD_DENSITY)
	        getDensityDialog(FRAMEWORK_DEFAULT_DPI, "framework-res.dpi", -1);
            else
	       RomUtils.setHybridProperty("framework-res.dpi", String.valueOf(value));
	} else if(KEY_SYSUI_DENSITY.equals(key)){
	    int value = Integer.parseInt((String) newValue);
            if(value == CUSTOM_LCD_DENSITY)
		getDensityDialog(SYSTEMUI_DEFAULT_DPI, "com.android.systemui.dpi", 1);
            else{
	       RomUtils.setHybridProperty("com.android.systemui.dpi", String.valueOf(value));
	       RomUtils.triggerAction(1);
	    }
	} 
	return true;
    }

    public void getDensityDialog(final int density, final String propierty, final int requiredDialog){
	AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
        final EditText input = new EditText(mContext);
        DigitsKeyListener onlyDecimalAllowed = new DigitsKeyListener(true, true);
               input.setKeyListener(onlyDecimalAllowed);
               alert.setView(input)
                   .setTitle(R.string.lcd_density_custom)
                   .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
                   public void onClick(DialogInterface dialog, int whichButton) {
                       String value = input.getText().toString();
                       int mDensity;
                       try{
                           mDensity = Integer.parseInt(value);
                       } catch (Exception e){
                           Toast.makeText(mContext, getString(R.string.lcd_density_no_value), Toast.LENGTH_LONG).show();
                           mDensity = density;
                       }
		RomUtils.setHybridProperty(propierty, String.valueOf(mDensity));
		if(requiredDialog == 0)
			getRequiredDialog(R.string.requires_reboot, 0);
		if(requiredDialog == 1)
			RomUtils.triggerAction(1);
		
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

