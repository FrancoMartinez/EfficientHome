package cl.martinez.franco.efficienthome;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;


public class NotificationActivity extends AppCompatActivity {

    EditText Horas, Minutos, Repeticion;
    AlarmManager aMan;
    PendingIntent pIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notification);

        Horas = (EditText)findViewById(R.id.Hora);
        Minutos = (EditText)findViewById(R.id.Minuto);
        Repeticion = (EditText)findViewById(R.id.Repeticion);

    }

    public void alarma(View view){      //MÉTODO ENCARGADO DE SETEAR LA ALARMA
        if(Horas.getText().length()==0){    //VALIDACIÓN DE DATOS
            Horas.setError("Debe ingresar hora");
        } else {
            if (Integer.parseInt(Horas.getText().toString()) > 24){
                Horas.setError("No puede ser superior a 24");
            } else {
                if (Integer.parseInt(Horas.getText().toString()) == 24){
                    Horas.setText("00");
                }
            }
        }
        if(Minutos.getText().length()==0){
            Minutos.setError("Debe ingresar minutos");
        } else {
            if (Integer.parseInt(Minutos.getText().toString()) > 59) {
                Minutos.setError("No puede ser superior a 59");
            }
        }
        if(Repeticion.getText().length()==0){
            Repeticion.setError("Debe ingresar repetición");
        } else {

            Intent intent = new Intent(getBaseContext(), AlarmReceiver.class);  //CREA UN INTENT DE LA CLASE QUE GENERA LA NOTIFICACIÓN
            pIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            intent.setData((Uri.parse("custom://" + System.currentTimeMillis())));  //SETEA HORA ACTUAL

            Calendar cal = GregorianCalendar.getInstance();  //CREA CALENDARIO Y SETEA LOS DATOS QUE SON INGRESADOS EN LA APP.
            cal.set (Calendar.HOUR_OF_DAY, Integer.parseInt(Horas.getText().toString()));
            cal.set (Calendar.MINUTE, Integer.parseInt(Minutos.getText().toString()));
            cal.set (Calendar.SECOND, 00);
            cal.set (Calendar.MILLISECOND, 00);

            aMan = (AlarmManager)getSystemService(ALARM_SERVICE);   //CREA LA ALARMA
            aMan.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), Integer.parseInt(Repeticion.getText().toString()) * 1000, pIntent); //SETEA LA ALARMA DESDE LA HORA DE INICIO Y ESPECIFICA QUE SE REINICIE CADA CIERTO TIEMPO

            Toast.makeText(this, "Alarma guardada",Toast.LENGTH_LONG).show();

        }

    }

    public void alarmaDos(View view){
        if(Horas.getText().length()==0){
            Horas.setError("Debe ingresar hora");
        } else {
            if (Integer.parseInt(Horas.getText().toString()) > 24){
                Horas.setError("No puede ser superior a 24");
            } else {
                if (Integer.parseInt(Horas.getText().toString()) == 24){
                    Horas.setText("00");
                }
            }
        }
        if(Minutos.getText().length()==0){
            Horas.setError("Debe ingresar minutos");
        }
        if(Repeticion.getText().length()==0){
            Repeticion.setError("Debe ingresar repetición");
        } else {

            Calendar c = Calendar.getInstance();
            c.setTime(new Date());

            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            Intent alarmIntent = new Intent(this, AlarmReceiver.class);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            alarmIntent.setData((Uri.parse("custom://" + System.currentTimeMillis())));
            alarmManager.cancel(pendingIntent);

            Calendar alarmStartTime = Calendar.getInstance();
            alarmStartTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(Horas.getText().toString()));
            alarmStartTime.set(Calendar.MINUTE, Integer.parseInt(Minutos.getText().toString()));
            alarmStartTime.set(Calendar.SECOND, 00);
            alarmStartTime.set(Calendar.MILLISECOND, 00);

            int SegundosRep = Integer.parseInt(Repeticion.getText().toString());

            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, alarmStartTime.getTimeInMillis(), SegundosRep * 1000, pendingIntent);

            Toast.makeText(this, "Alarma guardada",Toast.LENGTH_LONG).show();
        }
    }


    public void cancelaralarma(View view){
        try {
            aMan.cancel(pIntent);
            //notif.CancelaAlarma();
            Toast.makeText(this, "Alarma cancelada", Toast.LENGTH_SHORT).show();
        } catch (Exception io){

        }
    }
}
