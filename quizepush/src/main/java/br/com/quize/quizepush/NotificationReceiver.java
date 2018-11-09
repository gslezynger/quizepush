package br.com.quize.quizepush;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class NotificationReceiver extends BroadcastReceiver {

    public static String NOTIFICATION_ID = "notification-id";
    public static String NOTIFICATION_MESSAGEID = "notification-message-id";
    public static String NOTIFICATION = "notification";
    @Override
    public void onReceive(Context context, Intent intent) {
//        Log.i(NotificationReceiver.class.getSimpleName(), "cacete");
//        Toast.makeText(context, "cacete", Toast.LENGTH_LONG).show();


        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notification = intent.getParcelableExtra(NOTIFICATION);

            int id = intent.getIntExtra(NOTIFICATION_ID, 0);
            String idMessage = intent.getStringExtra(NOTIFICATION_MESSAGEID);


            if(!NotificationService.CheckMessageShown(context,idMessage)){
                notificationManager.notify(id, notification);
                NotificationService.ShowedMessage(context,idMessage);
            }else{
                Log.e("PUSH","MESSAGE CANCELED");
            }
        }
    }
}
