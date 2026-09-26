package com.vid.tvcloser2;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import java.util.List;

public class AppCloserAccessibilityService extends AccessibilityService {

    private ActivityManager activityManager;

    @Override
    public void onCreate() {
        super.onCreate();
        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // No necesitamos procesar eventos en este caso
    }

    @Override
    public void onInterrupt() {
        // Manejo de interrupciones
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "com.vid.tvcloser2.CLOSE_APPS".equals(intent.getAction())) {
            closeApps();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void closeApps() {
        try {
            List<ActivityManager.RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();
            String ownPackage = getPackageName();
            String homePackage = getHomePackage();

            for (ActivityManager.RunningAppProcessInfo process : processes) {
                if (shouldClose(process.processName, ownPackage, homePackage)) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            activityManager.killBackgroundProcesses(process.processName);
                        } else {
                            Runtime.getRuntime().exec("kill -9 " + process.pid);
                        }
                    } catch (Exception e) {
                        // Continuar si falla uno
                    }
                }
            }
        } catch (Exception e) {
            // Silencioso si hay error
        }
    }

    private boolean shouldClose(String processName, String ownPackage, String homePackage) {
        // No cerrar nuestra propia app
        if (processName.equals(ownPackage) || processName.contains(ownPackage)) {
            return false;
        }

        // No cerrar el launcher/home
        if (processName.equals(homePackage) || processName.contains(homePackage)) {
            return false;
        }

        // No cerrar componentes críticos
        if (processName.equals("android") ||
            processName.startsWith("android.") ||
            processName.equals("com.android.systemui") ||
            processName.equals("com.android.settings") ||
            processName.equals("com.android.shell") ||
            processName.startsWith("com.xiaomi.") ||
            processName.startsWith("com.google.")) {
            return false;
        }

        return true;
    }

    private String getHomePackage() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);

        List<ActivityManager.RunningTaskInfo> tasks;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tasks = activityManager.getTasks(1);
            if (!tasks.isEmpty()) {
                return tasks.get(0).topActivity.getPackageName();
            }
        }

        // Fallback a launcher por defecto
        return "com.android.launcher";
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_INTERACTION_TIMES
                | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        
        setServiceInfo(info);
    }
}
