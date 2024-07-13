package com.tentrontechnologies.tentron_dfu;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.Nullable;

public class NotificationActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(isTaskRoot()){

            // Get the activity that is running on the app or the main activity
            /*
            * There is a task stack that contains all of the tasks made with the app and we
            * have to return to this task stack when we click on the notification
            * */
            PackageManager pm = getApplication().getPackageManager();
            Intent intent = pm.getLaunchIntentForPackage(getApplication().getPackageName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Get the extras if there is any
            Bundle extras = getIntent().getExtras();
            if(extras != null){
                intent.putExtras(extras);
            }
            // Start the said activity
            startActivity(intent);
        }
        // Finish the notification activity
        finish();
    }
}
