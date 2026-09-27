package com.vid.tvcloser2;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;

public class AppCloserAccessibilityService extends AccessibilityService {

    private static final int MAX_TRACKED_PACKAGES = 8;
    private static final long STEP_TIMEOUT_MS = 4000;
    private static final long NEXT_PACKAGE_DELAY_MS = 700;

    private static final String[] FORCE_STOP_LABELS = {
            "forzar detención", "forzar la detención", "force stop", "detener forzosamente"
    };
    private static final String[] CONFIRM_LABELS = {
            "aceptar", "ok", "detener aplicación", "force stop"
    };

    private enum Step { NONE, WAITING_APP_INFO, WAITING_CONFIRM_DIALOG }

    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;
    private String ownPackage;
    private String homePackage;

    private final Set<String> recentForegroundPackages =
            Collections.synchronizedSet(new LinkedHashSet<>());
    private final Queue<String> pendingPackages = new ArrayDeque<>();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private Step currentStep = Step.NONE;

    private final Runnable timeoutRunnable = () -> {
        currentStep = Step.NONE;
        processNextPackage();
    };

    @Override
    public void onCreate() {
        super.onCreate();
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, DeviceAdminReceiver.class);
        ownPackage = getPackageName();
        homePackage = getHomePackage();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int type = event.getEventType();

        if (currentStep == Step.WAITING_APP_INFO) {
            if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    || type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                tryClickForceStop();
            }
            return;
        }

        if (currentStep == Step.WAITING_CONFIRM_DIALOG) {
            if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    || type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                tryClickConfirm();
            }
            return;
        }

        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            trackForegroundPackage(event);
        }
    }

    private void trackForegroundPackage(AccessibilityEvent event) {
        CharSequence pkg = event.getPackageName();
        if (pkg == null) {
            return;
        }
        String packageName = pkg.toString();
        if (!shouldClose(packageName)) {
            return;
        }
        synchronized (recentForegroundPackages) {
            recentForegroundPackages.remove(packageName);
            recentForegroundPackages.add(packageName);
            while (recentForegroundPackages.size() > MAX_TRACKED_PACKAGES) {
                Iterator<String> it = recentForegroundPackages.iterator();
                if (it.hasNext()) {
                    it.next();
                    it.remove();
                }
            }
        }
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
        if (currentStep != Step.NONE) {
            // Ya hay un cierre en curso, no interferir
            return;
        }

        String[] targets;
        synchronized (recentForegroundPackages) {
            targets = recentForegroundPackages.toArray(new String[0]);
            recentForegroundPackages.clear();
        }

        if (targets.length == 0) {
            return;
        }

        if (closeViaDeviceOwner(targets)) {
            return;
        }

        pendingPackages.clear();
        pendingPackages.addAll(Arrays.asList(targets));
        processNextPackage();
    }

    private void processNextPackage() {
        handler.removeCallbacks(timeoutRunnable);

        String next = pendingPackages.poll();
        if (next == null) {
            currentStep = Step.NONE;
            onAllPackagesClosed();
            return;
        }

        currentStep = Step.WAITING_APP_INFO;
        openAppInfo(next);
        handler.postDelayed(timeoutRunnable, STEP_TIMEOUT_MS);
    }

    private void onAllPackagesClosed() {
        Toast.makeText(this, "TV Closer: todas las aplicaciones han sido cerradas", Toast.LENGTH_SHORT).show();

        try {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } catch (Exception e) {
            goHome();
        }
    }

    private void openAppInfo(String packageName) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } catch (Exception e) {
            processNextPackage();
        }
    }

    private void tryClickForceStop() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return;
        }

        AccessibilityNodeInfo target = findNodeByTextContains(root, FORCE_STOP_LABELS);
        if (target != null) {
            boolean clicked = clickNodeOrParent(target);
            if (clicked) {
                currentStep = Step.WAITING_CONFIRM_DIALOG;
                handler.removeCallbacks(timeoutRunnable);
                handler.postDelayed(timeoutRunnable, STEP_TIMEOUT_MS);
            }
        }
    }

    private void tryClickConfirm() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return;
        }

        AccessibilityNodeInfo target = findNodeByTextContains(root, CONFIRM_LABELS);
        if (target != null) {
            clickNodeOrParent(target);
            handler.removeCallbacks(timeoutRunnable);
            handler.postDelayed(this::processNextPackage, NEXT_PACKAGE_DELAY_MS);
        }
    }

    private AccessibilityNodeInfo findNodeByTextContains(AccessibilityNodeInfo node, String[] labels) {
        if (node == null) {
            return null;
        }

        if (matchesAny(node.getText(), labels) || matchesAny(node.getContentDescription(), labels)) {
            return node;
        }

        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) {
                continue;
            }
            AccessibilityNodeInfo found = findNodeByTextContains(child, labels);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private boolean matchesAny(CharSequence text, String[] labels) {
        if (text == null) {
            return false;
        }
        String lower = text.toString().toLowerCase(Locale.ROOT);
        for (String label : labels) {
            if (lower.contains(label)) {
                return true;
            }
        }
        return false;
    }

    private boolean clickNodeOrParent(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = node;
        while (current != null) {
            if (current.isClickable()) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            current = current.getParent();
        }
        return false;
    }

    private void goHome() {
        performGlobalAction(GLOBAL_ACTION_HOME);
    }

    private boolean closeViaDeviceOwner(String[] packageNames) {
        if (devicePolicyManager == null || adminComponent == null) {
            return false;
        }
        if (!devicePolicyManager.isDeviceOwnerApp(ownPackage)) {
            return false;
        }
        try {
            devicePolicyManager.setPackagesSuspended(adminComponent, packageNames, true);
            devicePolicyManager.setPackagesSuspended(adminComponent, packageNames, false);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean shouldClose(String packageName) {
        if (packageName.equals(ownPackage)) {
            return false;
        }
        if (homePackage != null && packageName.equals(homePackage)) {
            return false;
        }
        if (packageName.equals("android")
                || packageName.startsWith("android.")
                || packageName.equals("com.android.systemui")
                || packageName.equals("com.android.settings")
                || packageName.equals("com.android.tv.settings")
                || packageName.equals("com.android.shell")) {
            return false;
        }
        return true;
    }

    private String getHomePackage() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        PackageManager pm = getPackageManager();
        ResolveInfo resolveInfo = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo != null && resolveInfo.activityInfo != null) {
            return resolveInfo.activityInfo.packageName;
        }
        return "com.android.launcher";
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;

        setServiceInfo(info);
    }
}
