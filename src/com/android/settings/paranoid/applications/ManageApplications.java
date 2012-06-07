/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.settings.paranoid.applications;

import static com.android.settings.Utils.prepareCustomPreferencesList;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StatFs;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;

import com.android.internal.content.PackageHelper;
import com.android.settings.R;
import com.android.settings.Settings.StorageUseActivity;
import com.android.settings.paranoid.applications.ApplicationsState.AppEntry;

import java.util.ArrayList;
import java.util.Comparator;

import com.android.settings.paranoid.RomUtils;

final class CanBeOnSdCardChecker {
    final IPackageManager mPm;
    int mInstallLocation;
    
    CanBeOnSdCardChecker() {
        mPm = IPackageManager.Stub.asInterface(
                ServiceManager.getService("package"));
    }
    
    void init() {
        try {
            mInstallLocation = mPm.getInstallLocation();
        } catch (RemoteException e) {
            Log.e("CanBeOnSdCardChecker", "Is Package Manager running?");
            return;
        }
    }
    
    boolean check(ApplicationInfo info) {
        boolean canBe = false;
        if ((info.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0) {
            canBe = true;
        } else {
            if ((info.flags & ApplicationInfo.FLAG_FORWARD_LOCK) == 0 &&
                    (info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                if (info.installLocation == PackageInfo.INSTALL_LOCATION_PREFER_EXTERNAL ||
                        info.installLocation == PackageInfo.INSTALL_LOCATION_AUTO) {
                    canBe = true;
                } else if (info.installLocation
                        == PackageInfo.INSTALL_LOCATION_UNSPECIFIED) {
                    if (mInstallLocation == PackageHelper.APP_INSTALL_EXTERNAL) {
                        // For apps with no preference and the default value set
                        // to install on sdcard.
                        canBe = true;
                    }
                }
            }
        }
        return canBe;
    }
}

/**
 * Activity to pick an application that will be used to display installation information and
 * options to uninstall/delete user data for system applications. This activity
 * can be launched through Settings or via the ACTION_MANAGE_PACKAGE_STORAGE
 * intent.
 */
public class ManageApplications extends Fragment implements
        OnItemClickListener,
        TabHost.TabContentFactory, TabHost.OnTabChangeListener {
    static final String TAG = "ManageApplications";
    static final boolean DEBUG = false;
    
    // attributes used as keys when passing values to InstalledAppDetails activity
    public static final String APP_CHG = "chg";
    
    // constant value that can be used to check return code from sub activity.
    private static final int INSTALLED_APP_DETAILS = 1;

    public static final int SIZE_TOTAL = 0;
    public static final int SIZE_INTERNAL = 1;
    public static final int SIZE_EXTERNAL = 2;

    // sort order that can be changed through the menu can be sorted alphabetically
    // or size(descending)
    private static final int MENU_OPTIONS_BASE = 0;
    // Filter options used for displayed list of applications
    public static final int FILTER_APPS_ALL = MENU_OPTIONS_BASE + 0;
    public static final int FILTER_APPS_THIRD_PARTY = MENU_OPTIONS_BASE + 1;
    public static final int FILTER_APPS_SDCARD = MENU_OPTIONS_BASE + 2;

    public static final int SORT_ORDER_ALPHA = MENU_OPTIONS_BASE + 4;
    public static final int SORT_ORDER_SIZE = MENU_OPTIONS_BASE + 5;
    public static final int SHOW_RUNNING_SERVICES = MENU_OPTIONS_BASE + 6;
    public static final int SHOW_BACKGROUND_PROCESSES = MENU_OPTIONS_BASE + 7;
    // sort order
    private int mSortOrder = SORT_ORDER_ALPHA;
    // Filter value
    private int mFilterApps = FILTER_APPS_THIRD_PARTY;
    
    private ApplicationsState mApplicationsState;
    private ApplicationsAdapter mApplicationsAdapter;
    
    // layout inflater object used to inflate views
    private LayoutInflater mInflater;
    
    private String mCurrentPkgName;
    
    private View mLoadingContainer;

    private View mListContainer;

    // ListView used to display list
    private ListView mListView;

    TextView mStorageChartLabel;
    TextView mUsedStorageText;
    TextView mFreeStorageText;

    private Menu mOptionsMenu;
    
    // These are for keeping track of activity and tab switch state.
    private int mCurView;
    private boolean mResumedRunning;
    private boolean mActivityResumed;
    
    static final String TAB_DOWNLOADED = "Downloaded";
    static final String TAB_RUNNING = "Running";
    static final String TAB_ALL = "All";
    static final String TAB_SDCARD = "OnSdCard";
    private View mRootView;

    private boolean mShowBackground = false;
    
    // -------------- Copied from TabActivity --------------

    private TabHost mTabHost;
    private String mDefaultTab = null;

    // -------------- Copied from TabActivity --------------

    final Runnable mRunningProcessesAvail = new Runnable() {
        public void run() {
            handleRunningProcessesAvail();
        }
    };

    // View Holder used when displaying views
    static class AppViewHolder {
        ApplicationsState.AppEntry entry;
        TextView appName;
        ImageView appIcon;
        TextView appDetails;
        TextView disabled;
        CheckBox checkBox;
        
        void updateDpiText(Context context) {           
		// ParanoidAndroid
		String mDpi = RomUtils.getProperty(entry.info.packageName + ".dpi", "0");
		int mIntegerLayout = Integer.parseInt(RomUtils.getProperty(entry.info.packageName + ".mode", "0"));
		String mLayout = context.getString(R.string.style_default);
		switch(mIntegerLayout){
			case 1:
				mLayout = context.getString(R.string.style_phone);
			break;
			case 2:
				mLayout = context.getString(R.string.style_tablet);				
			break;
		}
		String mAppDetails = "DPI: "+(!mDpi.equals("0") ? mDpi : "---")+" "+
			context.getString(R.string.app_detail_splitter)+" "+mLayout;
		appDetails.setText(mAppDetails);
        }
    }
    
    /*
     * Custom adapter implementation for the ListView
     * This adapter maintains a map for each displayed application and its properties
     * An index value on each AppInfo object indicates the correct position or index
     * in the list. If the list gets updated dynamically when the user is viewing the list of
     * applications, we need to return the correct index of position. This is done by mapping
     * the getId methods via the package name into the internal maps and indices.
     * The order of applications in the list is mirrored in mAppLocalList
     */
    class ApplicationsAdapter extends BaseAdapter implements Filterable,
            ApplicationsState.Callbacks, AbsListView.RecyclerListener {
        private final ApplicationsState mState;
        private final ArrayList<View> mActive = new ArrayList<View>();
        private ArrayList<ApplicationsState.AppEntry> mBaseEntries;
        private ArrayList<ApplicationsState.AppEntry> mEntries;
        private boolean mResumed;
        private int mLastFilterMode=-1, mLastSortMode=-1;
        private int mWhichSize = SIZE_TOTAL;
	private Context mContext;
        CharSequence mCurFilterPrefix;

        private Filter mFilter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                ArrayList<ApplicationsState.AppEntry> entries
                        = applyPrefixFilter(constraint, mBaseEntries);
                FilterResults fr = new FilterResults();
                fr.values = entries;
                fr.count = entries.size();
                return fr;
            }

            @SuppressWarnings("unchecked")
			@Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mCurFilterPrefix = constraint;
                mEntries = (ArrayList<ApplicationsState.AppEntry>)results.values;
                notifyDataSetChanged();
            }
        };

        public ApplicationsAdapter(ApplicationsState state, Context context) {
            mState = state;
	    mContext = context;
        }

        public void resume(int filter, int sort) {
            if (DEBUG) Log.i(TAG, "Resume!  mResumed=" + mResumed);
            if (!mResumed) {
                mResumed = true;
                mState.resume(this);
                mLastFilterMode = filter;
                mLastSortMode = sort;
                rebuild(true);
            } else {
                rebuild(filter, sort);
            }
        }

        public void pause() {
            if (mResumed) {
                mResumed = false;
                mState.pause();
            }
        }

        public void rebuild(int filter, int sort) {
            if (filter == mLastFilterMode && sort == mLastSortMode) {
                return;
            }
            mLastFilterMode = filter;
            mLastSortMode = sort;
            rebuild(true);
        }
        
        public void rebuild(boolean eraseold) {
            if (DEBUG) Log.i(TAG, "Rebuilding app list...");
            ApplicationsState.AppFilter filterObj;
            Comparator<AppEntry> comparatorObj;
            boolean emulated = Environment.isExternalStorageEmulated();
            if (emulated) {
                mWhichSize = SIZE_TOTAL;
            } else {
                mWhichSize = SIZE_INTERNAL;
            }
            switch (mLastFilterMode) {
                case FILTER_APPS_THIRD_PARTY:
                    filterObj = ApplicationsState.THIRD_PARTY_FILTER;
                    break;
                case FILTER_APPS_SDCARD:
                    filterObj = ApplicationsState.ON_SD_CARD_FILTER;
                    if (!emulated) {
                        mWhichSize = SIZE_EXTERNAL;
                    }
                    break;
                default:
                    filterObj = null;
                    break;
            }
            switch (mLastSortMode) {
                case SORT_ORDER_SIZE:
                    switch (mWhichSize) {
                        case SIZE_INTERNAL:
                            comparatorObj = ApplicationsState.INTERNAL_SIZE_COMPARATOR;
                            break;
                        case SIZE_EXTERNAL:
                            comparatorObj = ApplicationsState.EXTERNAL_SIZE_COMPARATOR;
                            break;
                        default:
                            comparatorObj = ApplicationsState.SIZE_COMPARATOR;
                            break;
                    }
                    break;
                default:
                    comparatorObj = ApplicationsState.ALPHA_COMPARATOR;
                    break;
            }
            ArrayList<ApplicationsState.AppEntry> entries
                    = mState.rebuild(filterObj, comparatorObj);
            if (entries == null && !eraseold) {
                // Don't have new list yet, but can continue using the old one.
                return;
            }
            mBaseEntries = entries;
            if (mBaseEntries != null) {
                mEntries = applyPrefixFilter(mCurFilterPrefix, mBaseEntries);
            } else {
                mEntries = null;
            }
            notifyDataSetChanged();

            if (entries == null) {
                mListContainer.setVisibility(View.INVISIBLE);
                mLoadingContainer.setVisibility(View.VISIBLE);
            } else {
                mListContainer.setVisibility(View.VISIBLE);
                mLoadingContainer.setVisibility(View.GONE);
            }
        }

        ArrayList<ApplicationsState.AppEntry> applyPrefixFilter(CharSequence prefix,
                ArrayList<ApplicationsState.AppEntry> origEntries) {
            if (prefix == null || prefix.length() == 0) {
                return origEntries;
            } else {
                String prefixStr = ApplicationsState.normalize(prefix.toString());
                final String spacePrefixStr = " " + prefixStr;
                ArrayList<ApplicationsState.AppEntry> newEntries
                        = new ArrayList<ApplicationsState.AppEntry>();
                for (int i=0; i<origEntries.size(); i++) {
                    ApplicationsState.AppEntry entry = origEntries.get(i);
                    String nlabel = entry.getNormalizedLabel();
                    if (nlabel.startsWith(prefixStr) || nlabel.indexOf(spacePrefixStr) != -1) {
                        newEntries.add(entry);
                    }
                }
                return newEntries;
            }
        }

        @Override
        public void onRunningStateChanged(boolean running) {
            getActivity().setProgressBarIndeterminateVisibility(running);
        }

        @Override
        public void onRebuildComplete(ArrayList<AppEntry> apps) {
            if (mLoadingContainer.getVisibility() == View.VISIBLE) {
                mLoadingContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
            }
            mListContainer.setVisibility(View.VISIBLE);
            mLoadingContainer.setVisibility(View.GONE);
            mBaseEntries = apps;
            mEntries = applyPrefixFilter(mCurFilterPrefix, mBaseEntries);
            notifyDataSetChanged();
        }

        @Override
        public void onPackageListChanged() {
            rebuild(false);
        }

        @Override
        public void onPackageIconChanged() {
            // don't care about icons loaded in the background.
        }

	@Override
        public void onPackageSizeChanged(String packageName) {
            
        }

        @Override
        public void onAllSizesComputed() {
            if (mLastSortMode == SORT_ORDER_SIZE) {
                rebuild(false);
            }
        }
        
        public int getCount() {
            return mEntries != null ? mEntries.size() : 0;
        }
        
        public Object getItem(int position) {
            return mEntries.get(position);
        }
        
        public ApplicationsState.AppEntry getAppEntry(int position) {
            return mEntries.get(position);
        }

        public long getItemId(int position) {
            return mEntries.get(position).id;
        }
        
        public View getView(int position, View convertView, ViewGroup parent) {
            // A ViewHolder keeps references to children views to avoid unnecessary calls
            // to findViewById() on each row.
            AppViewHolder holder;

            // When convertView is not null, we can reuse it directly, there is no need
            // to reinflate it. We only inflate a new View when the convertView supplied
            // by ListView is null.
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.paranoid_manage_applications_item, null);

                // Creates a ViewHolder and store references to the two children views
                // we want to bind data to.
                holder = new AppViewHolder();
                holder.appName = (TextView) convertView.findViewById(R.id.app_name);
                holder.appIcon = (ImageView) convertView.findViewById(R.id.app_icon);
                holder.appDetails = (TextView) convertView.findViewById(R.id.app_details);
                holder.disabled = (TextView) convertView.findViewById(R.id.app_disabled);
                holder.checkBox = (CheckBox) convertView.findViewById(R.id.app_on_sdcard);
                convertView.setTag(holder);
            } else {
                // Get the ViewHolder back to get fast access to the TextView
                // and the ImageView.
                holder = (AppViewHolder) convertView.getTag();
            }

            // Bind the data efficiently with the holder
            ApplicationsState.AppEntry entry = mEntries.get(position);
            synchronized (entry) {
                holder.entry = entry;
                if (entry.label != null) {
                    holder.appName.setText(entry.label);
                    holder.appName.setTextColor(getActivity().getResources().getColorStateList(
                            entry.info.enabled ? android.R.color.primary_text_dark
                                    : android.R.color.secondary_text_dark));
                }
                mState.ensureIcon(entry);
                if (entry.icon != null) {
                    holder.appIcon.setImageDrawable(entry.icon);
                }

                holder.updateDpiText(mContext);

                if (InstalledAppDetails.SUPPORT_DISABLE_APPS) {
                    holder.disabled.setVisibility(entry.info.enabled ? View.GONE : View.VISIBLE);
                } else {
                    holder.disabled.setVisibility(View.GONE);
                }
                if (mLastFilterMode == FILTER_APPS_SDCARD) {
                    holder.checkBox.setVisibility(View.VISIBLE);
                    holder.checkBox.setChecked((entry.info.flags
                            & ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0);
                } else {
                    holder.checkBox.setVisibility(View.GONE);
                }
            }
            mActive.remove(convertView);
            mActive.add(convertView);
            return convertView;
        }

        @Override
        public Filter getFilter() {
            return mFilter;
        }

        @Override
        public void onMovedToScrapHeap(View view) {
            mActive.remove(view);
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        mApplicationsState = ApplicationsState.getInstance(getActivity().getApplication());
        mApplicationsAdapter = new ApplicationsAdapter(mApplicationsState, getActivity());
        Intent intent = getActivity().getIntent();
        String action = intent.getAction();
        String defaultTabTag = TAB_DOWNLOADED;
        String className = getArguments() != null
                ? getArguments().getString("classname") : null;
        if (className == null) {
            className = intent.getComponent().getClassName();
        }
        if (className.equals(StorageUseActivity.class.getName())
                || Intent.ACTION_MANAGE_PACKAGE_STORAGE.equals(action)
                || className.endsWith(".StorageUse")) {
            mSortOrder = SORT_ORDER_SIZE;
            mFilterApps = FILTER_APPS_ALL;
            defaultTabTag = TAB_ALL;
        } else if (Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS.equals(action)) {
            // Select the all-apps tab, with the default sorting
            defaultTabTag = TAB_ALL;
        }
        
        if (savedInstanceState != null) {
            mSortOrder = savedInstanceState.getInt("sortOrder", mSortOrder);
            mFilterApps = savedInstanceState.getInt("filterApps", mFilterApps);
            String tmp = savedInstanceState.getString("defaultTabTag");
            if (tmp != null) defaultTabTag = tmp;
            mShowBackground = savedInstanceState.getBoolean("showBackground", false);
        }
        
        mDefaultTab = defaultTabTag;
        
        new StatFs("/data");
        new StatFs(Environment.getExternalStorageDirectory().toString());

        getActivity().getText(R.string.invalid_size_value);
        getActivity().getText(R.string.computing_size);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // initialize the inflater
        mInflater = inflater;
        mRootView = inflater.inflate(R.layout.paranoid_manage_applications, null);
        mLoadingContainer = mRootView.findViewById(R.id.loading_container);
        mListContainer = mRootView.findViewById(R.id.list_container);
        // Create adapter and list view here
        ListView lv = (ListView) mListContainer.findViewById(android.R.id.list);
        View emptyView = mListContainer.findViewById(com.android.internal.R.id.empty);
        if (emptyView != null) {
            lv.setEmptyView(emptyView);
        }
        lv.setOnItemClickListener(this);
        lv.setSaveEnabled(true);
        lv.setItemsCanFocus(true);
        lv.setOnItemClickListener(this);
        lv.setTextFilterEnabled(true);
        mListView = lv;
        lv.setRecyclerListener(mApplicationsAdapter);
        mListView.setAdapter(mApplicationsAdapter);

        mStorageChartLabel = (TextView)mListContainer.findViewById(R.id.storageChartLabel);
        mUsedStorageText = (TextView)mListContainer.findViewById(R.id.usedStorageText);
        mFreeStorageText = (TextView)mListContainer.findViewById(R.id.freeStorageText);

        mResumedRunning = false;
        mCurView = VIEW_NOTHING;

        mTabHost = (TabHost) mInflater.inflate(R.layout.manage_apps_tab_content, container, false);
        mTabHost.setup();
        final TabHost tabHost = mTabHost;
        tabHost.addTab(tabHost.newTabSpec(TAB_DOWNLOADED)
                .setIndicator(getActivity().getString(R.string.filter_apps_third_party),
                        getActivity().getResources().getDrawable(R.drawable.ic_tab_download))
                .setContent(this));
        if (!Environment.isExternalStorageEmulated()) {
            tabHost.addTab(tabHost.newTabSpec(TAB_SDCARD)
                    .setIndicator(getActivity().getString(R.string.filter_apps_onsdcard),
                            getActivity().getResources().getDrawable(R.drawable.ic_tab_sdcard))
                    .setContent(this));
        }
        tabHost.addTab(tabHost.newTabSpec(TAB_ALL)
                .setIndicator(getActivity().getString(R.string.filter_apps_all),
                        getActivity().getResources().getDrawable(R.drawable.ic_tab_all))
                .setContent(this));
        tabHost.setCurrentTabByTag(mDefaultTab);
        tabHost.setOnTabChangedListener(this);

        // adjust padding around tabwidget as needed
        prepareCustomPreferencesList(container, mTabHost, mListView, false);

        return mTabHost;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mActivityResumed = true;
        showCurrentTab();
        updateOptionsMenu();
        mTabHost.getTabWidget().setEnabled(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("sortOrder", mSortOrder);
        outState.putInt("filterApps", mFilterApps);
        if (mDefaultTab != null) {
            outState.putString("defautTabTag", mDefaultTab);
        }
        outState.putBoolean("showBackground", mShowBackground);
    }

    @Override
    public void onPause() {
        super.onPause();
        mActivityResumed = false;
        mApplicationsAdapter.pause();
        if (mResumedRunning) {
            mResumedRunning = false;
        }
        mTabHost.getTabWidget().setEnabled(false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == INSTALLED_APP_DETAILS && mCurrentPkgName != null) {
            mApplicationsState.requestSize(mCurrentPkgName);
        }
    }
    
    // utility method used to start sub activity
    private void startApplicationDetailsActivity() {
        // start new fragment to display extended information
        Bundle args = new Bundle();
        args.putString(InstalledAppDetails.ARG_PACKAGE_NAME, mCurrentPkgName);

        PreferenceActivity pa = (PreferenceActivity)getActivity();
        pa.startPreferencePanel(InstalledAppDetails.class.getName(), args,
                R.string.application_info_label, null, this, INSTALLED_APP_DETAILS);
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.i(TAG, "onCreateOptionsMenu in " + this + ": " + menu);
        mOptionsMenu = menu;
        // note: icons removed for now because the cause the new action
        // bar UI to be very confusing.
        menu.add(0, SORT_ORDER_ALPHA, 1, R.string.sort_order_alpha)
                //.setIcon(android.R.drawable.ic_menu_sort_alphabetically)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, SORT_ORDER_SIZE, 2, R.string.sort_order_size)
                //.setIcon(android.R.drawable.ic_menu_sort_by_size)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        updateOptionsMenu();
    }
    
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        updateOptionsMenu();
    }
    
    @Override
    public void onDestroyOptionsMenu() {
        mOptionsMenu = null;
    }
    
    void updateOptionsMenu() {
        if (mOptionsMenu == null) {
            return;
        }
        
        /*
         * The running processes screen doesn't use the mApplicationsAdapter
         * so bringing up this menu in that case doesn't make any sense.
         */
        mOptionsMenu.findItem(SORT_ORDER_ALPHA).setVisible(mSortOrder != SORT_ORDER_ALPHA);
        mOptionsMenu.findItem(SORT_ORDER_SIZE).setVisible(mSortOrder != SORT_ORDER_SIZE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int menuId = item.getItemId();
        if ((menuId == SORT_ORDER_ALPHA) || (menuId == SORT_ORDER_SIZE)) {
            mSortOrder = menuId;
            mApplicationsAdapter.rebuild(mFilterApps, mSortOrder);  
        }
        updateOptionsMenu();
        return true;
    }
    
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        ApplicationsState.AppEntry entry = mApplicationsAdapter.getAppEntry(position);
        mCurrentPkgName = entry.info.packageName;
        startApplicationDetailsActivity();
    }
    
    public View createTabContent(String tag) {
        return mRootView;
    }

    static final int VIEW_NOTHING = 0;
    static final int VIEW_LIST = 1;

    private void selectView(int which) {
        if (which == VIEW_LIST) {
            if (mResumedRunning) {
                mResumedRunning = false;
            }
            if (mCurView != which) {
                mListContainer.setVisibility(View.VISIBLE);
                mLoadingContainer.setVisibility(View.GONE);
            }
            if (mActivityResumed) {
                mApplicationsAdapter.resume(mFilterApps, mSortOrder);
            }
	}
        mCurView = which;
        final Activity host = getActivity();
        if (host != null) {
            host.invalidateOptionsMenu();
        }
    }

    void handleRunningProcessesAvail() {

    }

    public void showCurrentTab() {
        String tabId = mDefaultTab = mTabHost.getCurrentTabTag();
        int newOption;
        if (TAB_DOWNLOADED.equalsIgnoreCase(tabId)) {
            newOption = FILTER_APPS_THIRD_PARTY;
        } else if (TAB_ALL.equalsIgnoreCase(tabId)) {
            newOption = FILTER_APPS_ALL;
        } else if (TAB_SDCARD.equalsIgnoreCase(tabId)) {
            newOption = FILTER_APPS_SDCARD;
        } else {
            // Invalid option. Do nothing
            return;
        }
        
        mFilterApps = newOption;
        selectView(VIEW_LIST);
        updateOptionsMenu();
    }

    public void onTabChanged(String tabId) {
        showCurrentTab();
    }
}
