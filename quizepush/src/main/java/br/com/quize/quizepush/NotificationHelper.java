package br.com.quize.quizepush;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.Date;
import java.util.Random;

public class NotificationHelper {

    private Context mContext;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;
    public static final String NOTIFICATION_CHANNEL_ID = "10001";

    public NotificationHelper(Context context) {
        mContext = context;
    }

    public Notification createNotification(String title, String message)
    {
        /**Creates an explicit intent for an Activity in your app**/
//        Intent resultIntent = new Intent(mContext , Main.class);
//        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//
//        PendingIntent resultPendingIntent = PendingIntent.getActivity(mContext,
//                0 /* Request code */, resultIntent,
//                PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder = new NotificationCompat.Builder(mContext);
        mBuilder.setSmallIcon(R.drawable.ic_quize_noti);
        mBuilder.setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setNumber(1)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI);

        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
        {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "NOTIFICATION_CHANNEL_NAME", importance);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            notificationChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            assert mNotificationManager != null;
            mBuilder.setChannelId(NOTIFICATION_CHANNEL_ID);
            mNotificationManager.createNotificationChannel(notificationChannel);
        }
        assert mNotificationManager != null;
//        mNotificationManager.notify(0 /* Request Code */, mBuilder.build());

        return mBuilder.build();

    }
    public void scheduleNotification(Notification notification,String id, long futureInMillis) {

        final int idcontrol = (int) System.currentTimeMillis();
        Intent notificationIntent = new Intent(mContext,NotificationReceiver.class);
        notificationIntent.putExtra(NotificationReceiver.NOTIFICATION_ID, idcontrol);
        notificationIntent.putExtra(NotificationReceiver.NOTIFICATION_MESSAGEID, id);
        notificationIntent.putExtra(NotificationReceiver.NOTIFICATION, notification);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext,idcontrol, notificationIntent, PendingIntent.FLAG_ONE_SHOT);


        long futureInMillisAux = futureInMillis - System.currentTimeMillis();

//        Log.e("PUSH","Notification scheduled: " + data);
//        Log.e("PUSH","Notification scheduled in millis update: " + futureInMillisAux);

        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.e("PUSH","Notification > M scheduled in millis update: " + futureInMillisAux);
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, futureInMillis, pendingIntent);
        }else{
            Log.e("PUSH","Notification < M scheduled in millis update: " + futureInMillisAux);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, futureInMillis, pendingIntent);
        }
    }
}