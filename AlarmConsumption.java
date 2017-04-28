package cl.martinez.franco.efficienthome;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.widget.EditText;
import android.widget.TextView;


/**
 * Created by Franco on 24-04-17.
 */
public class AlarmConsumption extends BroadcastReceiver{
    private static final int NOTIF_ID = 1;
    private String ip, MetaConsumo;
    private Double Gasto;
    private int kwhActual;
    private Context contexto;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.contexto = context;
        SharedPreferences prefs = context.getSharedPreferences("Configuraciones", Context.MODE_PRIVATE);
        ip = prefs.getString("IPRaspberry", "");

        MetaConsumo = prefs.getString("MetaConsumo", "0");

        //Para el consumo
        PotenciaActual potenciaActual = new PotenciaActual((ScrollingConsumptionActivity) contexto, ip) ;//llamo a la clase que maneja la tabla de datos del dia actual
        potenciaActual.obtenerKWH(); //solicito los datos almacenados en la tabla
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                esperarKwh();//una vez pasa el segundo continuo
            }
        }, 1000); //se espera un segundo debido a que son muchos datos


        showNotification(context);
    }

    public void showNotification(Context context) {


        if (Gasto > Integer.parseInt(MetaConsumo)) {
            Intent i = new Intent(context, ScrollingConsumptionActivity.class);
            PendingIntent pi = PendingIntent.getActivity(context, 0, i, 0);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_warning_black_24dp) //SETEA ICONO
                    .setContentTitle("Meta de consumo")    //SETEA TITULO
                    .setContentText("Ha superado la meta de consumo");  //SETEA TEXTO
            mBuilder.setContentIntent(pi);
            mBuilder.setDefaults(Notification.DEFAULT_SOUND);   //SETEA QUE SEA CON SONIDO
            mBuilder.setAutoCancel(true)
                    .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000});   //SETEA QUE VIBRE
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(0, mBuilder.build());
        }
    }

    public void esperarKwh(){ //solicito la tension de esta vivienda y sus respectivos precios
        Valores valor = new Valores((ScrollingConfigurationActivity) contexto, ip,"push");
        valor.obtenerValores();
    }

    public void recibirListaPotencia (String resultado){ // recibo los watts
            calcularKwh(resultado); //envia a calcular los kwh
    }

    private void calcularKwh(String resultado){
        new PotenciaActual((ScrollingConsumptionActivity) contexto, ip);
        kwhActual = Integer.parseInt(resultado);
    }

    public void recibirValores(String resultado){
        String[] precios = resultado.split(","); //lo recibido por consulta
        System.out.println(precios[0] + " " + precios[1] + " " + precios[2]);
        Valores valores = new Valores(Double.parseDouble(precios[0]),
                Double.parseDouble(precios[1]),Integer.parseInt(precios[2]));
        calcularPrecio(valores);
    }

    private void calcularPrecio(Valores valores){
        Gasto = 0.0;
        Gasto = valores.getValorkwh() * kwhActual + valores.getValortransporte() * kwhActual + valores.getValoradministracion();
    }
}
