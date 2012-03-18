package org.yammp.util;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

public class PluginInfo {

	public Drawable icon;
	public CharSequence name;
	public CharSequence description;
	public String pname;

	public PluginInfo(ResolveInfo resolveInfo, PackageManager pm) {
		ApplicationInfo info = resolveInfo.activityInfo.applicationInfo;
		icon = info.loadIcon(pm);
		name = info.loadLabel(pm);
		pname = info.packageName;
		description = info.loadDescription(pm);
		if (description == null) {
			description = "";
		}
	}
}