package br.com.quize.quizepush;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "quizepush";
    public static final String TABLE_NAME = "notification";
    public static final String NOTIFICATION_ID = "id";
    public static final String NOTIFICATION_TITLE = "title";
    public static final String NOTIFICATION_TEXT = "text";
    public static final String NOTIFICATION_DATE = "date";
    public static final String NOTIFICATION_STATUS = "status";

    private static final String  DATEFORMAT = "yyyy-MM-dd HH:mm";
    private static final String STATUS_NEW = "new";
    private static final String STATUS_SCHEDULED = "scheduled";
    private static final String STATUS_SHOWN = "shown";



    public class Notification{

        public String ID;
        public String TITLE;
        public String TEXT;
        public Date DATE;
        public String STATUS;

        public Notification(String Id,String Title,String Text,String Date,String Status) {
            this.ID = Id;
            this.TITLE = Title;
            this.TEXT = Text;
            this.STATUS = Status;

            SimpleDateFormat dateFormat = new SimpleDateFormat(DATEFORMAT);
            try {
                Date date = dateFormat.parse(Date);
                this.DATE = date;
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }


    SQLiteDatabase db;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
        db = this.getWritableDatabase();
    }



    public String AdjustNotificationDate(String sDate,String date){

        SimpleDateFormat dateFormat = new SimpleDateFormat(DATEFORMAT);
        try {

            Date serverDate = dateFormat.parse(sDate);
            Date currentDate = new Date();
            long diffInMillies = Math.abs(currentDate.getTime() - serverDate.getTime());

            Date notiDate = dateFormat.parse(date);
            notiDate.setTime( notiDate.getTime() + diffInMillies);

            return dateFormat.format(notiDate);

        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }



    public void CreateNotification(String id,String title,String text,String date){

        if(CheckIfExits(id)){
            return;
        }
        ContentValues insertValues = new ContentValues();
        insertValues.put(NOTIFICATION_ID, id);
        insertValues.put(NOTIFICATION_TITLE, title);
        insertValues.put(NOTIFICATION_TEXT, text);
        insertValues.put(NOTIFICATION_DATE, date);
        insertValues.put(NOTIFICATION_STATUS, STATUS_NEW);

        db.insert(TABLE_NAME,null,insertValues);
    }
    public void DeleteNotification( String id ){
        db.execSQL("Delete from  "+ TABLE_NAME + " WHERE id = '" + id+"'");
    }
    public void ClearNotification(  ){
        db.execSQL("Delete from  "+ TABLE_NAME );
    }
    public  boolean CheckIfExits(String id) {

        String Query = "Select * from " + TABLE_NAME + " where id = '" + id + "'";
        Cursor cursor = db.rawQuery(Query, null);
        if(cursor.getCount() <= 0){
            cursor.close();
            return false;
        }
        cursor.close();
        return true;
    }
    public  boolean CheckIfShown(String id) {

        String Query = "Select status from " + TABLE_NAME + " where id = '" + id + "'";
        Cursor cursor = db.rawQuery(Query, null);
        if(cursor.getCount() <= 0){
            cursor.close();
            return false;
        }
        if (cursor.moveToFirst()){

            if(cursor.getString(0).equals(STATUS_SHOWN)){
                cursor.close();
                return true;
            }
        }
        cursor.close();
        return false;
    }

    public void UpdateHasScheduleNotification( String id ){
        db.execSQL("UPDATE "+ TABLE_NAME + " SET status = '" + STATUS_SCHEDULED + "' WHERE id = '" + id + "'");
    }
    public void UpdateShownNotification( String id ){
        db.execSQL("UPDATE "+ TABLE_NAME + " SET status = '" + STATUS_SHOWN + "' WHERE id = '" + id + "'");
    }
    public List<Notification> ListPendingScheduleNotification(){
        List<Notification> notis = new LinkedList<Notification>();

        String query = "SELECT  * FROM "+ TABLE_NAME + " where status != '"+STATUS_SHOWN+"' order by " + NOTIFICATION_DATE + " asc limit 1";

        Cursor cursor = db.rawQuery(query, null);

        // 3. go over each row, build book and add it to list
        Notification noti = null;
        if (cursor.moveToFirst()) {
            do {
                noti = new Notification(cursor.getString(0),cursor.getString(1),cursor.getString(2),cursor.getString(3),cursor.getString(4));

                // Add book to books
                notis.add(noti);
            } while (cursor.moveToNext());
        }
        cursor.close();
        Log.d("PUSH", "RESULT: " + notis.toString());

        // return books
        return notis;

    }
    public List<Notification> ListAllNotShownNotifications(){
        List<Notification> notis = new LinkedList<Notification>();

        String query = "SELECT  * FROM "+ TABLE_NAME + " where status != '"+STATUS_SHOWN+"' order by " + NOTIFICATION_DATE + " asc limit 1";

        Cursor cursor = db.rawQuery(query, null);

        // 3. go over each row, build book and add it to list
        Notification noti = null;
        if (cursor.moveToFirst()) {
            do {
                noti = new Notification(cursor.getString(0),cursor.getString(1),cursor.getString(2),cursor.getString(3),cursor.getString(4));

                // Add book to books
                notis.add(noti);
            } while (cursor.moveToNext());
        }
        cursor.close();
        Log.d("PUSH", "RESULT: " + notis.toString());

        // return books
        return notis;

    }




    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + TABLE_NAME + "(id TEXT,title TEXT,text TEXT,date TEXT,status TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}
