/*
 * Copyright (C) 2026 The LineageOS Project
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

package org.lineageos.settings.refreshrate;

import android.app.ActivityTaskManager;
import android.app.IActivityTaskManager;
import android.app.Service;
import android.app.TaskStackListener;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class RefreshService extends Service {

    private static final String TAG = "RefreshService";

    private boolean mScreenOn = true;
    private String mCurrentApp = "";
    private RefreshUtils mRefreshUtils;
    private IActivityTaskManager mActivityTaskManager;

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                return;
            }
            switch (intent.getAction()) {
                case Intent.ACTION_SCREEN_OFF -> {
                    mScreenOn = false;
                    mRefreshUtils.restoreSystemRefreshRate();
                }
                case Intent.ACTION_SCREEN_ON -> {
                    mScreenOn = true;
                    updateRefreshRate();
                }
                default -> {
                    // Ignore other broadcasts.
                }
            }
        }
    };

    private final TaskStackListener mTaskListener = new TaskStackListener() {
        @Override
        public void onTaskStackChanged() {
            try {
                final ActivityTaskManager.RootTaskInfo focusedTask =
                        ActivityTaskManager.getService().getFocusedRootTaskInfo();
                if (focusedTask != null && focusedTask.topActivity != null) {
                    ComponentName taskComponentName = focusedTask.topActivity;
                    String foregroundApp = taskComponentName.getPackageName();
                    if (foregroundApp != null && !foregroundApp.equals(mCurrentApp)) {
                        mCurrentApp = foregroundApp;
                        updateRefreshRate();
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to query focused task", e);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mRefreshUtils = new RefreshUtils(this);
        try {
            mActivityTaskManager = ActivityTaskManager.getService();
            mActivityTaskManager.registerTaskStackListener(mTaskListener);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to register TaskStackListener", e);
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mIntentReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (mActivityTaskManager != null) {
                mActivityTaskManager.unregisterTaskStackListener(mTaskListener);
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to unregister TaskStackListener", e);
        }
        unregisterReceiver(mIntentReceiver);
        if (mRefreshUtils != null) {
            mRefreshUtils.restoreSystemRefreshRate();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void updateRefreshRate() {
        if (mScreenOn) {
            mRefreshUtils.applyForPackage(mCurrentApp);
        } else {
            mRefreshUtils.restoreSystemRefreshRate();
        }
    }
}
