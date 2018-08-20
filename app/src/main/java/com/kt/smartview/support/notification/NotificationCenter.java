package com.kt.smartview.support.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.kt.smartview.R;
import com.kt.smartview.support.log.YWMLog;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */
public class NotificationCenter {
    private final YWMLog logger = new YWMLog(NotificationCenter.class);
    private Context context;
    private NotificationManager notificationManager;
    private final int gereralNotificationId = 1000;
    private Handler handler = new Handler();
    public NotificationCenter(Context context) {
        // TODO Auto-generated constructor stub
        this.context = context;
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
    public void showNotification(Context context, String ticker, String title, String message, Intent intent){

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder mCompatBuilder = new NotificationCompat.Builder(context);
        mCompatBuilder.setSmallIcon(R.mipmap.ic_launcher);
        mCompatBuilder.setTicker(ticker);
        mCompatBuilder.setWhen(System.currentTimeMillis());
        mCompatBuilder.setContentTitle(title);
        mCompatBuilder.setContentText(message);
        mCompatBuilder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
        mCompatBuilder.setContentIntent(pendingIntent);
        mCompatBuilder.setAutoCancel(true);
        notificationManager.notify(gereralNotificationId, mCompatBuilder.build());
    }
    public void removeAll(){
        notificationManager.cancel(gereralNotificationId);
    }

    private Toast toast;
    public void showCustomToast(final String title, final String message,
                                 final int duration) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                View toast_new_message;
                TextView textview_title;
                TextView textview_message;

                if(toast == null){
                    toast = new Toast(context);
                    LayoutInflater inflater = (LayoutInflater) context
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    toast_new_message = inflater.inflate(
                            R.layout.toast_new_message, null, false);
                    toast.setView(toast_new_message);
                    toast.setGravity(48, 0, 150);
                }else{
                    toast_new_message = toast.getView();
                }
                textview_title = (TextView) toast_new_message
                        .findViewById(R.id.title);
                textview_message = (TextView) toast_new_message
                        .findViewById(R.id.message);

                textview_title.setText(title);
                textview_message.setText(message);
                toast.setDuration(duration);
                toast.show();
            }
        });
    }
}
