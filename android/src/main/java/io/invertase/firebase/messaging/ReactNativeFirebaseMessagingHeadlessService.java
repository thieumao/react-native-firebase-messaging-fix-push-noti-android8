package io.invertase.firebase.messaging;

import android.content.Intent;
import android.os.Bundle;
import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.jstasks.HeadlessJsTaskConfig;
import com.google.firebase.messaging.RemoteMessage;
import io.invertase.firebase.common.ReactNativeFirebaseJSON;

import javax.annotation.Nullable;

public class ReactNativeFirebaseMessagingHeadlessService extends HeadlessJsTaskService {
  private static final long TIMEOUT_DEFAULT = 60000;
  private static final String TIMEOUT_JSON_KEY = "messaging_android_headless_task_timeout";
  private static final String TASK_KEY = "ReactNativeFirebaseMessagingHeadlessTask";

  // START: Fix bug push notification on Android 8 when Quit App
  public static String getMetadata(Context context, String name) {
    try {
      ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
        context.getPackageName(), PackageManager.GET_META_DATA);
      if (appInfo.metaData != null) {
        return appInfo.metaData.getString(name);
      }
    } catch (PackageManager.NameNotFoundException e) {
      // if we can’t find it in the manifest, just return empty
    }

    return "";
  }

  @Override
  public void onCreate() {
    super.onCreate();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      String CHANNEL_ID = getMetadata(this, "com.google.firebase.messaging.default_notification_channel_id");
      String CHANNEL_NAME = CHANNEL_ID;
      NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_DEFAULT);

      ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

      Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("")
        .setContentText("").build();
      this.startForeground(1, notification);
    }
  }
  // END: Fix bug push notification on Android 8 when Quit App

  @Override
  protected @Nullable
  HeadlessJsTaskConfig getTaskConfig(Intent intent) {
    Bundle extras = intent.getExtras();
    if (extras == null) return null;
    RemoteMessage remoteMessage = intent.getParcelableExtra("message");

    return new HeadlessJsTaskConfig(
      TASK_KEY,
      ReactNativeFirebaseMessagingSerializer.remoteMessageToWritableMap(remoteMessage),
      ReactNativeFirebaseJSON.getSharedInstance().getLongValue(TIMEOUT_JSON_KEY, TIMEOUT_DEFAULT),
      // Prevents race condition where the user opens the app at the same time as a notification
      // is delivered, causing a crash.
      true
    );
  }
}
