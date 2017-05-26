package cl.martinez.franco.efficienthome;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Franco on 23-04-17.
 */
public class ServicePushConsumo extends Service {
    public static int id = 999;
    private String ip;
    private Integer Meta, kwhActual;
    private SharedPreferences prefs;
    private Double Gasto;

    private static String TAG = "ServicePushConsumo";

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        prefs = getSharedPreferences("Configuraciones", Context.MODE_PRIVATE);

        //URL RASPBERRY PI
        ip = prefs.getString("IPRaspberry", "");

        //Meta consumo
        Meta = Integer.parseInt(prefs.getString("MetaConsumo", ""));

        if (Meta > 0){  //Si se ingresó mayor a 0 busco
            solicitoPotencia();
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    obtenerValores();//una vez pasa el segundo continuo
                }
            }, 1000); //se espera un segundo debido a que son muchos datos
        } else {
            stopSelf();    //Se destruye
        }
        Log.d(TAG, "ServicePushConsumo started");
        return super.onStartCommand(intent, flags, startId); // START_STICKY;
    }

    public void solicitoPotencia(){ //Solicita los kWh mensual
        URL url = null;
        try {
            url = new URL("http://"+ip.trim()+"/informe.php?caso=2");
            System.out.println(url);
        } catch (Exception e) {
            System.out.println("Error 4");
        }
        new DownloadFilesTaskKwh().execute(url);
    }

    public void obtenerValores(){   //Solicita datos de cálculo
        URL url = null;
        try {
            url = new URL("http://"+ip.trim()+"/informe.php?caso=5");
        } catch (Exception e) {
            System.out.println("Error 4");
        }
        new DownloadFilesTaskValores().execute(url);
    }

    private class DownloadFilesTaskKwh extends AsyncTask<URL, Integer, String> {    //Realiza la conexión y retorna los kWh del mes
        protected String doInBackground(URL... urls) {
            URL url = urls[0];
            HttpURLConnection urlConnection = null;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                System.out.println("Error: " + e);
                return "Error de Conexión";
            }
            try {
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                //readStream(in);
                BufferedReader r = new BufferedReader(new InputStreamReader(in));
                String x = "";
                x = r.readLine();
                String total = "";

                while (x != null) {
                    total += x;
                    x = r.readLine();
                }
                return total;

            } catch (IOException e) {
                return "Error de manejo de stream";
            } finally {
                urlConnection.disconnect();
            }
        }

        protected void onPostExecute(String resultado) {
            recibirListaPotencia(resultado);
        }
    }

    private class DownloadFilesTaskValores extends AsyncTask<URL, Integer, String> {    //Realiza la conexión y retorna los valores para cálculo
        protected String doInBackground(URL... urls) {
            URL url = urls[0];
            System.out.println(url);
            HttpURLConnection urlConnection = null;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                return "Error de Conexión";
            }
            try {
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                //readStream(in);
                BufferedReader r = new BufferedReader(new InputStreamReader(in));
                String x = "";
                x = r.readLine();
                String total = "";

                while (x != null) {
                    total += x;
                    x = r.readLine();
                }
                return total;

            } catch (IOException e) {
                return "Error de manejo de stream";
            } finally {
                urlConnection.disconnect();
            }
        }

        protected void onPostExecute(String resultado) {
            recibirValores(resultado);
        }
    }

    public void recibirListaPotencia (String resultado){  //Recibe los kWh del mes
        if(resultado.equals("Error 9")){
            System.out.println(resultado);
        }
        else if(resultado.equals("Error de manejo de stream")) {
            Toast.makeText(this, "Error de conexión", Toast.LENGTH_LONG).show();
        } else {
            kwhActual = Integer.parseInt(resultado);
        }
    }

    public void recibirValores(String resultado){   //Recibe las variables para calcular gastos
        String[] precios = resultado.split(",");
        Gasto = Double.parseDouble(precios[0]) * kwhActual + Double.parseDouble(precios[1]) * kwhActual + Integer.parseInt(precios[2]);
        System.out.println("Mi gasto en el servicepush es de: " + String.valueOf(Gasto));
        System.out.println("Mi meta es de: " + String.valueOf(Meta));
        if (Gasto > Meta){
            EnviaNotificacion();
        } else {
            stopSelf();
        }
    }


    public void EnviaNotificacion(){
        Intent i = new Intent(this, ScrollingConsumptionActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_warning_black_24dp) //SETEA ICONO
                .setContentTitle("Meta incompleta")    //SETEA TITULO
                .setContentText("Ha superado el consumo eléctrico");  //SETEA TEXTO
        mBuilder.setContentIntent(pi);
        mBuilder.setDefaults(Notification.DEFAULT_SOUND);   //SETEA QUE SEA CON SONIDO
        mBuilder.setAutoCancel(true)
                .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000});   //SETEA QUE VIBRE
        NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, mBuilder.build());
        stopSelf();
    }

        @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        Log.d(TAG, "ServicePushConsumo started");
    }
}
