/*
 *  YAMMP - Yet Another Multi Media Player for android
 *  Copyright (C) 2011-2012  Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 *  This file is part of YAMMP.
 *
 *  YAMMP is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  YAMMP is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with YAMMP.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.yammp.app;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.yammp.Constants;
import org.yammp.R;
import org.yammp.util.PluginInfo;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;

public class PluginFragment extends SherlockListFragment implements Constants,
		LoaderCallbacks<List<PluginInfo>> {

	private PluginAdapter mAdapter;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setHasOptionsMenu(true);
		mAdapter = new PluginAdapter(getActivity(), R.layout.playlist_list_item);
		setListAdapter(mAdapter);
		setListShown(false);
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Loader<List<PluginInfo>> onCreateLoader(int id, Bundle args) {
		return new PluginsListLoader(getActivity());
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		PluginInfo item = mAdapter.getItem(position);
		Intent intent = new Intent();
		intent.setPackage(item.pname);
		try {
			intent.setAction(INTENT_CONFIGURE_PLUGIN);
			startActivity(intent);
		} catch (ActivityNotFoundException e1) {
			try {
				intent.setAction(INTENT_OPEN_PLUGIN);
				startActivity(intent);
			} catch (ActivityNotFoundException e2) {
				Toast.makeText(getActivity(), R.string.plugin_not_supported, Toast.LENGTH_SHORT)
						.show();
			}
		}

	}

	@Override
	public void onLoaderReset(Loader<List<PluginInfo>> loader) {
		mAdapter.setData(null);
	}

	@Override
	public void onLoadFinished(Loader<List<PluginInfo>> loader, List<PluginInfo> data) {
		mAdapter.setData(data);
		if (isResumed()) {
			setListShown(true);
		} else {
			setListShownNoAnimation(true);
		}

	}

	public static class PluginsListLoader extends AsyncTaskLoader<List<PluginInfo>> {

		private PackageManager mPackageManager;
		private PackageIntentReceiver mPackageObserver;
		private InterestingConfigChanges mLastConfig = new InterestingConfigChanges();
		List<PluginInfo> mApps;

		/**
		 * Perform alphabetical comparison of application entry objects.
		 */
		public static final Comparator<PluginInfo> ALPHA_COMPARATOR = new Comparator<PluginInfo>() {

			private final Collator sCollator = Collator.getInstance();

			@Override
			public int compare(PluginInfo object1, PluginInfo object2) {
				return sCollator.compare(object1.name, object2.name);
			}
		};

		public PluginsListLoader(Context context) {
			super(context);
			mPackageManager = context.getPackageManager();
		}

		@Override
		public void deliverResult(List<PluginInfo> apps) {
			if (isReset()) {
				// An async query came in while the loader is stopped. We
				// don't need the result.
				if (apps != null) {
					onReleaseResources(apps);
				}
			}
			List<PluginInfo> oldApps = apps;
			mApps = apps;

			if (isStarted()) {
				// If the Loader is currently started, we can immediately
				// deliver its results.
				super.deliverResult(apps);
			}

			// At this point we can release the resources associated with
			// 'oldApps' if needed; now that the new result is delivered we
			// know that it is no longer in use.
			if (oldApps != null) {
				onReleaseResources(oldApps);
			}
		}

		@Override
		public List<PluginInfo> loadInBackground() {
			List<PluginInfo> plugins = new ArrayList<PluginInfo>();
			List<Intent> intents = new ArrayList<Intent>();
			intents.add(new Intent(INTENT_CONFIGURE_PLUGIN));
			intents.add(new Intent(INTENT_OPEN_PLUGIN));
			for (Intent intent : intents) {
				for (ResolveInfo info : mPackageManager.queryIntentActivities(intent, 0)) {
					plugins.add(new PluginInfo(info, mPackageManager));
				}
			}
			Collections.sort(plugins, ALPHA_COMPARATOR);
			return plugins;
		}

		/**
		 * Handles a request to cancel a load.
		 */
		@Override
		public void onCanceled(List<PluginInfo> apps) {
			super.onCanceled(apps);

			// At this point we can release the resources associated with 'apps'
			// if needed.
			onReleaseResources(apps);
		}

		/**
		 * Helper function to take care of releasing resources associated with
		 * an actively loaded data set.
		 */
		protected void onReleaseResources(List<PluginInfo> apps) {
			// For a simple List<> there is nothing to do. For something
			// like a Cursor, we would close it here.
		}

		/**
		 * Handles a request to completely reset the Loader.
		 */
		@Override
		protected void onReset() {
			super.onReset();

			// Ensure the loader is stopped
			onStopLoading();

			// At this point we can release the resources associated with 'apps'
			// if needed.
			if (mApps != null) {
				onReleaseResources(mApps);
				mApps = null;
			}

			// Stop monitoring for changes.
			if (mPackageObserver != null) {
				getContext().unregisterReceiver(mPackageObserver);
				mPackageObserver = null;
			}
		}

		/**
		 * Handles a request to start the Loader.
		 */
		@Override
		protected void onStartLoading() {
			if (mApps != null) {
				// If we currently have a result available, deliver it
				// immediately.
				deliverResult(mApps);
			}

			// Start watching for changes in the app data.
			if (mPackageObserver == null) {
				mPackageObserver = new PackageIntentReceiver(this);
			}

			// Has something interesting in the configuration changed since we
			// last built the app list?
			boolean configChange = mLastConfig.applyNewConfig(getContext().getResources());

			if (takeContentChanged() || mApps == null || configChange) {
				// If the data has changed since the last time it was loaded
				// or is not currently available, start a load.
				forceLoad();
			}
		}

		/**
		 * Handles a request to stop the Loader.
		 */
		@Override
		protected void onStopLoading() {
			// Attempt to cancel the current load task if possible.
			cancelLoad();
		}

		/**
		 * Helper for determining if the configuration has changed in an
		 * interesting way so we need to rebuild the app list.
		 */
		public static class InterestingConfigChanges {

			final Configuration mLastConfiguration = new Configuration();
			int mLastDensity;

			boolean applyNewConfig(Resources res) {
				int configChanges = mLastConfiguration.updateFrom(res.getConfiguration());
				boolean densityChanged = mLastDensity != res.getDisplayMetrics().densityDpi;
				if (densityChanged
						|| (configChanges & (ActivityInfo.CONFIG_LOCALE
								| ActivityInfo.CONFIG_UI_MODE | ActivityInfo.CONFIG_SCREEN_LAYOUT)) != 0) {
					mLastDensity = res.getDisplayMetrics().densityDpi;
					return true;
				}
				return false;
			}
		}

		/**
		 * Helper class to look for interesting changes to the installed apps so
		 * that the loader can be updated.
		 */
		public static class PackageIntentReceiver extends BroadcastReceiver {

			final PluginsListLoader mLoader;

			public PackageIntentReceiver(PluginsListLoader loader) {
				mLoader = loader;
				IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
				filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
				filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
				filter.addDataScheme("package");
				mLoader.getContext().registerReceiver(this, filter);
				// Register for events related to sdcard installation.
				IntentFilter sdFilter = new IntentFilter();
				sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
				sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
				mLoader.getContext().registerReceiver(this, sdFilter);
			}

			@Override
			public void onReceive(Context context, Intent intent) {
				// Tell the loader about the change.
				mLoader.onContentChanged();
			}
		}

	}

	private class PluginAdapter extends ArrayAdapter<PluginInfo> {

		public PluginAdapter(Context context, int resource) {
			super(context, resource);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;

			if (convertView == null) {
				view = getLayoutInflater(getArguments()).inflate(R.layout.plugin_list_item, parent,
						false);
			} else {
				view = convertView;
			}

			ViewHolder viewholder = view.getTag() == null ? new ViewHolder(view)
					: (ViewHolder) view.getTag();

			PluginInfo info = getItem(position);
			viewholder.plugin_icon.setImageDrawable(info.icon);
			viewholder.plugin_name.setText(info.name);
			viewholder.plugin_description.setText(info.description);

			return view;
		}

		public void setData(List<PluginInfo> data) {
			clear();
			if (data != null) {
				for (PluginInfo item : data) {
					add(item);
				}
			}
		}

		private class ViewHolder {

			ImageView plugin_icon;
			TextView plugin_name;
			TextView plugin_description;

			public ViewHolder(View view) {

				plugin_icon = (ImageView) view.findViewById(R.id.plugin_icon);
				plugin_name = (TextView) view.findViewById(R.id.plugin_name);
				plugin_description = (TextView) view.findViewById(R.id.plugin_description);
			}
		}

	}
}
