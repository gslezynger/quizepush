package br.com.quize.quizepush;


import android.app.Notification;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import br.com.quize.quizepush.PushQuize.PushQuize;


public class NotificationService extends JobService {
    private static final int JOB_ID = 69;
    //5 minute interval
    private static final int INTERVAL =  1000 * 30 * 1;

    public static void schedule(Context context) {
//        if(isJobServiceOn(context)){
//            Toast.makeText(context, "  JOB ALREADY STARTED ", Toast.LENGTH_LONG).show();
//            return;
//        }
        Log.e("PUSH","Scheduling Push Loader");
        ComponentName component = new ComponentName(context, NotificationService.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, component);


        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setMinimumLatency(INTERVAL);
        }else{
            builder.setPeriodic(INTERVAL).setPersisted(true);
        }

        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(builder.build());
    }

    @Override
    public boolean onStartJob(JobParameters params) {

//        Toast.makeText(this, "  Doing WORK", Toast.LENGTH_LONG).show();
        Log.e("PUSH","DOING MY WORK");
        doMyWork(params,getApplicationContext());


        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {

        return false;
    }

    private void doMyWork(final JobParameters params,final Context context) {
        PushQuize p = new PushQuize();



        p.DownloadNotifications(new PushQuize.OnNotificationAvailable() {
            @Override
            public void endedNotification() {

                Log.e("PUSH","ENDED NOTIFICATION RECEIVE");
                LoadAndScheduleAlarmNotifications(context);
                //Reschedule the Service before calling job finished
                if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
//                    Toast.makeText(context, " STARTING JOB ", Toast.LENGTH_LONG).show();
                    jobFinished(params,false);
                    schedule(context);
                }else{
                    jobFinished(params,false);
                }

            }

            @Override
            public void newNotification(String id, String title, String text, String date, String serverDate) {

                Log.e("PUSH","NEW NOTIFICATION REVEIVED");
                DatabaseHelper db = new DatabaseHelper(context);
                String adjusted_date = db.AdjustNotificationDate(serverDate,date);
                db.CreateNotification(id,title,text,adjusted_date);
            }
        });

    }

    public static  void LoadAndScheduleAlarmNotifications(Context context){
        DatabaseHelper db = new DatabaseHelper(context);
        NotificationHelper notificationHelper = new NotificationHelper(context);

        List<DatabaseHelper.Notification> notificationList = db.ListPendingScheduleNotification();

        for (DatabaseHelper.Notification n :notificationList ) {
            if(!n.DATE.before(new Date())){
                Notification noti = notificationHelper.createNotification(n.TITLE,n.TEXT);

                notificationHelper.scheduleNotification(noti,n.ID,n.DATE.getTime());
                db.UpdateHasScheduleNotification(n.ID);

            }else{
                db.DeleteNotification(n.ID);
            }


        }
    }
    public static  void LoadAndRescheduleAlarmNotifications(Context context){
        Log.e("PUSH","Rescheduling notifications");
        DatabaseHelper db = new DatabaseHelper(context);
        NotificationHelper notificationHelper = new NotificationHelper(context);

        List<DatabaseHelper.Notification> notificationList = db.ListAllNotShownNotifications();

        for (DatabaseHelper.Notification n :notificationList ) {
            if(!n.DATE.before(new Date())){
                Notification noti = notificationHelper.createNotification(n.TITLE,n.TEXT);
                notificationHelper.scheduleNotification(noti,n.ID,n.DATE.getTime());
                db.UpdateHasScheduleNotification(n.ID);
            }else{
                db.DeleteNotification(n.ID);
            }
        }
    }

    public static void ShowedMessage(Context context,String id){
        Log.e("PUSH","Message displayed: " + id);
        DatabaseHelper db = new DatabaseHelper(context);
        db.UpdateShownNotification(id);
    }
    public static boolean isJobServiceOn( Context context ) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService( Context.JOB_SCHEDULER_SERVICE ) ;

        boolean hasBeenScheduled = false ;

        for ( JobInfo jobInfo : scheduler.getAllPendingJobs() ) {
            if ( jobInfo.getId() == JOB_ID ) {
                hasBeenScheduled = true ;
                break ;
            }
        }

        return hasBeenScheduled ;
    }
}
