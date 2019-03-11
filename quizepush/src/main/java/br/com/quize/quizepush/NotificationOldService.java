package br.com.quize.quizepush;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import br.com.quize.quizepush.PushQuize.PushQuize;

public class NotificationOldService extends Service {
    private AlarmManager mAlarmManager;
    private PushQuize p;

    public NotificationOldService() {

    }

    public void onCreate() {
        super.onCreate();
        Log.d("[PUSH_QUIZE]","Creating pushquize service");
        this.mAlarmManager = (AlarmManager)this.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        this.p = new PushQuize();
        this.start();
    }

    public void onTaskRemoved(Intent rootIntent) {
        Log.d("[PUSH_QUIZE]","Task removed, recreating soon");
        restartServiceForced();
    }
    public void restartServiceForced(){
        Intent restartService = new Intent(this.getApplicationContext(), this.getClass());
        restartService.setPackage(this.getPackageName());
        PendingIntent restartServiceIntent = PendingIntent.getService(this.getApplicationContext(), 1, restartService, PendingIntent.FLAG_UPDATE_CURRENT);
        this.mAlarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 3000L, restartServiceIntent);
    }

    public void onDestroy() {

        Log.d("[PUSH_QUIZE]","Service destroyed,recreating");
        //teste
        restartServiceForced();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("[PUSH_QUIZE]","Service started ... ");
        this.start();

        return Service.START_STICKY;
    }

    private void start() {

        final NotificationOldService t = this;
        p.DownloadNotifications(new PushQuize.OnNotificationAvailable() {
            @Override
            public void endedNotification() {
                Log.e("PUSH","ENDED NOTIFICATION RECEIVE");
                NotificationService.LoadAndScheduleAlarmNotifications(getApplicationContext());
                //restartServiceForced();
                t.stopSelf();
            }

            @Override
            public void newNotification(String id, String title, String text, String date, String serverDate) {

                Log.e("PUSH","SAVING NEW NOTIFICATION");
                DatabaseHelper db = new DatabaseHelper(getApplicationContext());
                String adjusted_date = db.AdjustNotificationDate(serverDate,date);
                db.CreateNotification(id,title,text,adjusted_date);
            }

            @Override
            public void cancelNotification(String id) {
                Log.e("PUSH","CANCELING NOTIFICATION");
                DatabaseHelper db = new DatabaseHelper(getApplicationContext());
                db.UpdateShownNotification(id);
            }
        });


    }

    private void stop() {
    }



    public IBinder onBind(Intent intent) {
        return null;
    }

}
