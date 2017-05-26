package cl.martinez.franco.efficienthome;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.CancelledKeyException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ServicePushVentilation extends Service{      //Servicio encargado de calcular tiempos y enviar notificaciones push
    private String estado, ip, horainicio, horatermino;
    private String[] splithora;
    private Date dHoraInicio, dHoraTermino, dHoraActual;
    private Integer Hora, Minutos, MinutosAbiertos, MinutosCerrados, TiempoRefresco, Porcentaje, MetaConsumo;
    private Boolean Llamar, Primer, Registrar;
    private Calendar c;
    private SharedPreferences prefs;
    private URL url;
    public static boolean StopNotifications = false;
    private List<String> ubicaciones = new ArrayList<String>();

    public ServicePushVentilation() {
    }

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        prefs = getSharedPreferences("Configuraciones", Context.MODE_PRIVATE);

        MetaConsumo = 0;
        TiempoRefresco = 0;

        //URL ARDUINO
        ip = prefs.getString("IPRaspberry", "");

        url = null;
        try {
            url = new URL("http://" + ip + "/informe.php?caso=8");
        } catch (MalformedURLException e) {
            Log.d("ServicioVentilacion", e.getLocalizedMessage());
        }



        //SETEA TIEMPO DE REFRESCO
        TiempoRefresco = 10000;

        //SETEA PORCENTAJE DE ACEPTACIÓN
        Porcentaje = Integer.parseInt(prefs.getString("PAceptacion","100"));

        //SETEA HORAS
        horainicio = prefs.getString("HoraInicio", "");
        horatermino = prefs.getString("HoraTermino","");

        c = Calendar.getInstance();

        try {
            //Configura hora de inicio con la fecha de hoy
            dHoraInicio = c.getTime();
            splithora = horainicio.split(":");
            Hora = Integer.parseInt(splithora[0]);
            Minutos = Integer.parseInt(splithora[1]);
            dHoraInicio.setHours(Hora);
            dHoraInicio.setMinutes(Minutos);
            dHoraInicio.setSeconds(0);
            System.out.println("INICIO:" + dHoraInicio);

            //Configura hora de término con la fecha de hoy
            dHoraTermino = c.getTime();
            splithora = horatermino.split(":");
            Hora = Integer.parseInt(splithora[0]);
            Minutos = Integer.parseInt(splithora[1]);
            dHoraTermino.setHours(Hora);
            dHoraTermino.setMinutes(Minutos);
            dHoraTermino.setSeconds(0);
            System.out.println("FINAL:" + dHoraTermino);
        } catch (Exception e) {
            e.printStackTrace();

        }

        Llamar = DentroDeHora();    //Valida si la hora actual está dentro del rango
        System.out.println(Llamar + " HA: " + dHoraActual + " HI: " + dHoraInicio + " HT: " + dHoraTermino);

        Primer = true;
        Registrar = false;
        MinutosAbiertos = 0;
        MinutosCerrados = 0;

        CargaUbicaciones();

        if (Llamar){
            ObtenerDatosArduino();
        }

        return START_STICKY;
    }

    public void ObtenerDatosArduino(){      //Inicia el asynctask por primera vez
        new DownloadFilesTaskDatos().execute(url);
    }

    public void ObtenerDatosReady(){        //Se ejecuta luego del primer postonexecute y sigue hasta que se termine el tiempo de preguntar
        Llamar = DentroDeHora();
        if(StopNotifications)
            return;
        if (Llamar){
            if (estado != "Abierta"){
                MinutosCerrados = MinutosCerrados + TiempoRefresco;
            } else {
                MinutosAbiertos = MinutosAbiertos + TiempoRefresco;
            }
            System.out.println("Vuelvo a llamar");
            new DownloadFilesTaskDatos().execute(url);
        } else {
            if (Registrar) {
                AlmacenaDatos(MinutosAbiertos, MinutosCerrados);
                Registrar = false;
            }
        }
    }

    private class DownloadFilesTaskDatos extends AsyncTask<URL, Integer, String> {
        protected String doInBackground(URL... urls) {
            if (!DownloadFilesTaskDatos.this.isCancelled()) {
                if (Primer) {
                    URL url = urls[0];
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
                } else {
                    try {
                        Thread.sleep(TiempoRefresco * 60 * 1000); //Minutos a milisegundos   COMENTAR PARA PROBAR
                        //Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    URL url = urls[0];
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
            } else {
                return null;
            }
        }

        protected void onPostExecute(String resultado) {
            try {
                String[] datosresultados = resultado.split(";");
                int TotalSensores=0;
                int SensoresAbiertos=0;
                for (String temporal : datosresultados) {
                    String[] dividirDatos = temporal.split(",");
                    for (int x = 0; x < ubicaciones.size(); x++) {
                        String separaubicacion[] = ubicaciones.get(x).split("-");
                        System.out.println(dividirDatos[0] + " - " + dividirDatos[1] + " - " + dividirDatos[2] + " - " + dividirDatos[3]);
                        if (dividirDatos[0].equals(separaubicacion[0].trim())) {
                            if (separaubicacion[3].trim().equals("SI")){
                                TotalSensores = TotalSensores + 1;
                                System.out.println("ventana:" + separaubicacion[3]);
                                if (dividirDatos[3].trim().equals("1")) { //Abierta
                                    SensoresAbiertos = SensoresAbiertos + 1;
                                }
                            }
                        }
                    }
                }

                System.out.println("TS: " + TotalSensores);
                System.out.println("SA: " + SensoresAbiertos);
                estado = "";

                //Calculo si cumple el porcentaje de aceptación

                int PActual;
                if (SensoresAbiertos == 0 || TotalSensores == 0 ){
                    PActual = 0;
                }else {
                    PActual = Math.round((SensoresAbiertos * 100) / TotalSensores);
                }

                System.out.println(PActual);
                System.out.println(Porcentaje);
                if (PActual >= Porcentaje){ //Cumple
                    estado = "Abierta";
                } else {
                    estado = "Cerrada";
                }

                System.out.println(estado);

                if (estado == "Cerrada") {
                    if (Llamar) {
                        EnviaNotificacion();
                    }
                }
                ObtenerDatosReady();
            } catch (Exception e) {
                Log.d("DownloadDatos", e.getLocalizedMessage());
            }
            Primer = false;
        }
    }

    public boolean DentroDeHora(){
        c = Calendar.getInstance();
        dHoraActual = c.getTime();

        if (dHoraActual.compareTo(dHoraInicio) > 0 && dHoraActual.compareTo(dHoraTermino) < 0){
            Registrar = true;
            return true;
        } else {
            return false;
        }
    }

    public void CargaUbicaciones(){
        AdminSQLite admin = new AdminSQLite(getBaseContext(), "efficienthome", null, 1);
        SQLiteDatabase bd = admin.getWritableDatabase();
        String sql = "select codigoubicacion, nombreubicacion, existeth, existev from ubicacion order by codigoubicacion";
        Cursor fila = bd.rawQuery(sql, null);
        if (fila.moveToFirst()){
            String codigoubicacion = fila.getString(0).trim();
            String nombreubicacion = fila.getString(1).trim();
            String existeth = fila.getString(2).trim();
            String existev = fila.getString(3).trim();

            ubicaciones.add(codigoubicacion + " - " + nombreubicacion + " - " + existeth + " - " + existev);
            while (fila.moveToNext()){
                codigoubicacion = fila.getString(0).trim();
                nombreubicacion = fila.getString(1).trim();
                existeth = fila.getString(2).trim();
                existev = fila.getString(3).trim();

                ubicaciones.add(codigoubicacion + " - " + nombreubicacion + " - " + existeth + " - " + existev);
            }
            bd.close();

        }else{
            bd.close();
        }

    }

    public void AlmacenaDatos(int minabiertos, int mincerrados){
        AdminSQLite admin = new AdminSQLite(this, "efficienthome", null, 1);
        SQLiteDatabase bd = admin.getWritableDatabase();

        c = Calendar.getInstance();

        Cursor fila=bd.rawQuery("select * from historialvent where minutosabierta ="+ minabiertos + " and minutoscerrada=" + mincerrados + " and fecha =" + getDateTime(), null); //Validación de datos

        if(!fila.moveToFirst()){
            ContentValues datos=new ContentValues();
            datos.put("minutosabierta", minabiertos);
            datos.put("minutoscerrada", mincerrados);
            datos.put("fecha", getDateTime());
            bd.insert("historialvent",null,datos);
            bd.close();
            System.out.println("Ventilación almacenada");

        } else {
            System.out.println("Error al almacenar ventilación");
        }
    }

    private String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    public void EnviaNotificacion(){
        Intent i = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_warning_black_24dp) //SETEA ICONO
                .setContentTitle("Recordatorio")    //SETEA TITULO
                .setContentText("Debe abrir las ventanas para poder ventilar");  //SETEA TEXTO
        mBuilder.setContentIntent(pi);
        mBuilder.setDefaults(Notification.DEFAULT_SOUND);   //SETEA QUE SEA CON SONIDO
        mBuilder.setAutoCancel(true)
                .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000});   //SETEA QUE VIBRE
        NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, mBuilder.build());
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }

}
