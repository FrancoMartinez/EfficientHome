package cl.martinez.franco.efficienthome;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private ArrayList<String> lstSensores = new ArrayList<String>();
    private ArrayList<String> menues = new ArrayList<String>();
    private List<String> ubicaciones = new ArrayList<String>();
    private List<String> consejos = new ArrayList<String>();
    private ArrayAdapter adapter, adaptersensores;
    private ListView lv, lvSensor;
    private int numconsejo;
    private TextView consejo;
    private SharedPreferences prefs;
    private String ip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        lv = (ListView) findViewById(R.id.lstmenu);
        lvSensor = (ListView)findViewById(R.id.lstSensores);

        consejo = (TextView) findViewById(R.id.txtConsejo);

//        menues.add("Historial Alertas");
        menues.add("Historial Ventilación");
        menues.add("Consumo Eléctrico");
//        menues.add("Test Arduino");
        menues.add("Notificaciones");
        menues.add("Configuración");

        adapter = new ArrayAdapter(this, R.layout.menu_items, menues);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(this);

        lstSensores.clear();
        try {
            adaptersensores.notifyDataSetChanged();
        } catch (Exception e){
            e.printStackTrace();
        }

        prefs = getSharedPreferences("Configuraciones", Context.MODE_PRIVATE);
        ip = prefs.getString("IPRaspberry", "");

        CargaConsejos();
        //CargaSensores();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {      //CONTROLA LOS CLICKS EN EL MENÚ
//        if (menues.get(position).equals("Test Arduino")) {
//            Intent intent = new Intent(this, ArduinoTestActivity.class);
//            startActivity(intent);
//        } else
        if (menues.get(position).equals("Historial Ventilación")) {
            Intent intent = new Intent(this, ScrollingVentHistoryActivity.class);
            startActivity(intent);
        } else if (menues.get(position).equals("Consumo Eléctrico")) {
            Intent intent = new Intent(this, ScrollingConsumptionActivity.class);
            startActivity(intent);
        } else if (menues.get(position).equals("Notificaciones")) {
            Intent intent = new Intent(this, NotificationActivity.class);
            startActivity(intent);
        } else if (menues.get(position).equals("Configuración")) {
            Intent intent = new Intent(this, ScrollingConfigurationActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(this, menues.get(position), Toast.LENGTH_SHORT).show();
        }
    }

    public void CargaSensores(){        //MUESTRA LA TEMPERATURA DEL SENSOR EN PANTALLA PRINCIPAL
        if (ip.equals("")){
            Toast.makeText(this, "No existe una dirección de Raspberry PI registrada.", Toast.LENGTH_LONG).show();
        } else {
            ubicaciones.clear();
            CargaUbicaciones(); //Carga las ubicaciones
            if (ubicaciones.size() > 0){    //Si hay busca datos de arduino
                CondensationData sensordata = new CondensationData(this, ip, "main");
                sensordata.obtenerDatosSensores();
            }
        }
    }

    public void recibirDatosSensores(String resultado){
        lvSensor.setAdapter(null);
        //Instanciar adaptador
        final SensorAdapter adaptersensores = new SensorAdapter(lstSensores, MainActivity.this);
        runOnUiThread(new Runnable() {
            public void run() {adaptersensores.notifyDataSetChanged();
            }
        });
        lvSensor.setAdapter(adaptersensores);

        lstSensores.removeAll(lstSensores);
        adaptersensores.notifyDataSetChanged();

        String[] datosresultados = resultado.split(";");
        for (String temporal : datosresultados) {
            String[] dividirDatos = temporal.split(",");


            for (int x = 0; x < ubicaciones.size(); x++){
                String separaubicacion[] = ubicaciones.get(x).split("-");
                if (dividirDatos[0].equals(separaubicacion[0].trim())){

                    String strTemperatura;
                    String strHumedad;
                    Boolean existesensor = false;
                    if (dividirDatos[1].equals("-99")){
                        strTemperatura = "-99";
                    } else {
                        strTemperatura = dividirDatos[1] +"ºC";
                    }

                    if (dividirDatos[2].equals("-99")){
                        strHumedad = "-99";
                    } else {
                        strHumedad = dividirDatos[2] +"%";
                    }

                    if (separaubicacion[2].trim().equals("NO")){
                        strTemperatura = "NO";
                        strHumedad = "NO";
                    }

                    for (int y = 0; y < lstSensores.size(); y++){
                        String sensores[] = lstSensores.get(y).split("-");

                        if (sensores[0].trim().equals(dividirDatos[0].trim())){
                            existesensor = true;
                            lstSensores.remove(y);
                            lstSensores.add(y, separaubicacion[0]+ " - " + separaubicacion[1] + " - T: " + strTemperatura + " - H: " + strHumedad);

                        }
                    }
                    if (!existesensor) {
                        lstSensores.add(separaubicacion[0]+ " - " + separaubicacion[1] + " - T: " + strTemperatura + " - H: " + strHumedad);
                    }
                }
            }
        }
        adaptersensores.notifyDataSetChanged();
    }

    public void CargaUbicaciones(){
        AdminSQLite admin = new AdminSQLite(this, "efficienthome", null, 1);
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

    public void CargaConsejos(){
        consejos.add(getResources().getString(R.string.tip1));
        consejos.add(getResources().getString(R.string.tip2));
        consejos.add(getResources().getString(R.string.tip3));
        consejos.add(getResources().getString(R.string.tip4));
        consejos.add(getResources().getString(R.string.tip5));
        consejos.add(getResources().getString(R.string.tip6));
        consejos.add(getResources().getString(R.string.tip7));
        consejos.add(getResources().getString(R.string.tip8));
        consejos.add(getResources().getString(R.string.tip9));
        consejos.add(getResources().getString(R.string.tip10));
        consejos.add(getResources().getString(R.string.tip11));
        consejos.add(getResources().getString(R.string.tip12));
        consejos.add(getResources().getString(R.string.tip13));
        consejos.add(getResources().getString(R.string.tip14));
        consejos.add(getResources().getString(R.string.tip15));
        consejos.add(getResources().getString(R.string.tip16));
        consejos.add(getResources().getString(R.string.tip17));
        consejos.add(getResources().getString(R.string.tip18));
        consejos.add(getResources().getString(R.string.tip19));
        consejos.add(getResources().getString(R.string.tip20));
        consejos.add(getResources().getString(R.string.tip21));
        consejos.add(getResources().getString(R.string.tip22));

        NuevoConsejo();
    }

    public void NuevoConsejo(){
        int min = 0;
        int max = 21;

        Random r = new Random();
        numconsejo = r.nextInt(max - min + 1) + min;

        consejo.setText(consejos.get(numconsejo));
    }

    @Override
    protected void onResume() {
        super.onResume();
        NuevoConsejo();
        CargaSensores();
    }
}
