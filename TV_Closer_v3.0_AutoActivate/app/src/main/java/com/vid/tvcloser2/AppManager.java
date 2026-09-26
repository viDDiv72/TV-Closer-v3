package com.vid.tvcloser2;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppManager {
    private final Context context;
    private final DevicePolicyManager dpm;
    private final ComponentName adminComponent;
    private final PackageManager pm;

    public AppManager(Context context, DevicePolicyManager dpm, ComponentName adminComponent) {
        this.context = context;
        this.dpm = dpm;
        this.adminComponent = adminComponent;
        this.pm = context.getPackageManager();
    }

    public int suspendApps() throws Exception {
        List<String> appsToClose = getClosableApps();
        
        if (appsToClose.isEmpty()) {
            return 0;
        }

        String[] packageArray = appsToClose.toArray(new String[0]);
        
        // Suspender
        dpm.setPackagesSuspended(adminComponent, packageArray, true);
        
        // Esperar 120 ms
        Thread.sleep(120);
        
        // Reactivar
        dpm.setPackagesSuspended(adminComponent, packageArray, false);
        
        return packageArray.length;
    }

    private List<String> getClosableApps() {
        Set<String> closable = new HashSet<>();
        String ownPackage = context.getPackageName();
        String homePackage = getHomePackage();

        // Recopilar apps de LEANBACK_LAUNCHER
        collectApps(closable, "android.intent.category.LEANBACK_LAUNCHER", ownPackage, homePackage);
        
        // Recopilar apps de LAUNCHER
        collectApps(closable, "android.intent.category.LAUNCHER", ownPackage, homePackage);

        return new ArrayList<>(closable);
    }

    private void collectApps(Set<String> result, String category, String ownPackage, String homePackage) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(category);

        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);
        
        for (ResolveInfo info : resolveInfos) {
            String packageName = info.activityInfo.packageName;
            
            if (!isProtected(packageName, ownPackage, homePackage)) {
                result.add(packageName);
            }
        }
    }

    private boolean isProtected(String packageName, String ownPackage, String homePackage) {
        // Proteger TV Closer
        if (packageName.equals(ownPackage)) return true;
        
        // Proteger launcher
        if (packageName.equals(homePackage)) return true;
        
        // Proteger componentes críticos de Android/Xiaomi
        if (packageName.equals("android") || 
            packageName.startsWith("android.") ||
            packageName.equals("com.android.systemui") ||
            packageName.equals("com.android.settings") ||
            packageName.equals("com.android.shell") ||
            packageName.startsWith("com.xiaomi.") ||
            packageName.startsWith("com.google.")) {
            return true;
        }
        
        return false;
    }

    private String getHomePackage() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        
        ResolveInfo resolveInfo = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
        
        if (resolveInfo != null && resolveInfo.activityInfo != null) {
            return resolveInfo.activityInfo.packageName;
        }
        
        return "com.android.launcher";
    }
}
