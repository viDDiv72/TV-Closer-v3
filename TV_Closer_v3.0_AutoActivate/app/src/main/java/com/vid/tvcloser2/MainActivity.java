package com.vid.tvcloser2;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DevicePolicyManager dpm;
    private AccessibilityManager accessibilityManager;
    private ComponentName adminComponent;
    private TextView statusText;
    private Button actionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.status_text);
        actionButton = findViewById(R.id.action_button);

        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        accessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        adminComponent = new ComponentName(this, DeviceAdminReceiver.class);

        updateStatus();

        actionButton.setOnClickListener(v -> {
            if (isDeviceOwner()) {
                runCleanup();
            } else if (isAccessibilityEnabled()) {
                runCleanup();
            } else {
                requestActivation();
            }
        });
    }

    private void updateStatus() {
        if (isDeviceOwner()) {
            statusText.setText("✓ Device Owner activado (modo óptimo)");
            actionButton.setText("Cerrar aplicaciones");
        } else if (isAccessibilityEnabled()) {
            statusText.setText("✓ Servicio de accesibilidad activo (modo limitado)");
            actionButton.setText("Cerrar aplicaciones");
        } else {
            statusText.setText("✗ No activado. Toca el botón para activar.");
            actionButton.setText("Activar ahora");
        }
    }

    private boolean isDeviceOwner() {
        return dpm != null && dpm.isDeviceOwnerApp(getPackageName());
    }

    private boolean isAccessibilityEnabled() {
        if (accessibilityManager == null || !accessibilityManager.isEnabled()) {
            return false;
        }

        List<AccessibilityServiceInfo> services = accessibilityManager
                .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        
        for (AccessibilityServiceInfo service : services) {
            if (service.getId().contains(getPackageName())) {
                return true;
            }
        }
        return false;
    }

    private void requestActivation() {
        Toast.makeText(this, "Abre Ajustes > Accesibilidad > Servicios y activa TV Closer", 
                Toast.LENGTH_LONG).show();
        
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
    }

    private void runCleanup() {
        if (isDeviceOwner()) {
            runCleanupDeviceOwner();
        } else if (isAccessibilityEnabled()) {
            runCleanupAccessibility();
        } else {
            Toast.makeText(this, "No hay permisos activos", Toast.LENGTH_SHORT).show();
        }
    }

    private void runCleanupDeviceOwner() {
        try {
            AppManager manager = new AppManager(this, dpm, adminComponent);
            int closedCount = manager.suspendApps();
            
            Toast.makeText(this, "Aplicaciones cerradas: " + closedCount, Toast.LENGTH_SHORT).show();
            
            // Regresa a home después de 1 segundo
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                goHome();
            }, 1000);
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void runCleanupAccessibility() {
        try {
            // Enviar intent al servicio de accesibilidad
            Intent serviceIntent = new Intent(this, AppCloserAccessibilityService.class);
            serviceIntent.setAction("com.vid.tvcloser2.CLOSE_APPS");
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            
            Toast.makeText(this, "Aplicaciones en proceso de cierre...", Toast.LENGTH_SHORT).show();
            
            // Regresa a home después de 2 segundos
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                goHome();
            }, 2000);
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void goHome() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(homeIntent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }
}
