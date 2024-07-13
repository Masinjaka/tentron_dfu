package com.tentrontechnologies.tentron_dfu;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import no.nordicsemi.android.dfu.DfuBaseService;

public class DfuService extends DfuBaseService {
    @Nullable
    @Override
    protected Class<? extends Activity> getNotificationTarget() {
        return NotificationActivity.class;
    }

    @Override
    protected boolean isDebug() {
        return true;
    }

    @Override
    protected void updateForegroundNotification(@NonNull NotificationCompat.Builder builder) {
        super.updateForegroundNotification(builder);
        // Customization of the foreground notification look i guess
    }
}
