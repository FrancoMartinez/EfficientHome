package cl.martinez.franco.efficienthome;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import cl.martinez.franco.efficienthome.MainActivity;
import cl.martinez.franco.efficienthome.R;

/**
 * Created by Franco on 14-09-16.
 */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        showNotification(context);
    }

    public void showNotification(Context context) {
            Intent i = new Intent(context, MainActivity.class);
            PendingIntent pi = PendingIntent.getActivity(context, 0, i, 0);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_warning_black_24dp) //SETEA ICONO
                    .setContentTitle("Recordatorio")    //SETEA TITULO
                    .setContentText("Recuerde ventilar las habitaciones.");  //SETEA TEXTO
            mBuilder.setContentIntent(pi);
            mBuilder.setDefaults(Notification.DEFAULT_SOUND);   //SETEA QUE SEA CON SONIDO
            mBuilder.setAutoCancel(true)
                    .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000});   //SETEA QUE VIBRE
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(0, mBuilder.build());
    }
}