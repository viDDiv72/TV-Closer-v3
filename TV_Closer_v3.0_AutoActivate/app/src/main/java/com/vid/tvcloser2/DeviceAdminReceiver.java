package com.vid.tvcloser2;

import android.content.Context;
import android.content.Intent;

public class DeviceAdminReceiver extends android.app.admin.DeviceAdminReceiver {
    
    @Override
    public void onEnabled(Context context, Intent intent) {
        // Device Admin activado
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        // Device Admin desactivado
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return "No se puede desactivar TV Closer";
    }
}
