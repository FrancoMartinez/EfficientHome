package cl.martinez.franco.efficienthome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.PointsGraphSeries;

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
import java.util.Collections;
import java.util.Iterator;

public class ChartActivity extends AppCompatActivity {

    public GraphView chart;
    PointsGraphSeries<DataPoint> series;
    String stringUrl, id;
    Integer iTemperatura, iHumedad, Contador;
    Double dTemperatura, dHumedad;
    ArrayList<SensorData> datos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chart);

        SharedPreferences prefs = getSharedPreferences("Configuraciones", Context.MODE_PRIVATE);
        stringUrl = prefs.getString("IPArduinoYun", "10.40.4.3");
        stringUrl = "http://" + stringUrl + "/arduino/216";

        datos = new ArrayList<SensorData>();

        id = getIntent().getExtras().getString("ID");

        ConfigurarGrafico();

    }

    public void Cargar(View view){
        series = new PointsGraphSeries<DataPoint>(new DataPoint[] {});
        Contador = 0;
        GetTempHum();
    }

    public void ConfigurarGrafico(){
        chart = (GraphView) findViewById(R.id.chart);

        //Títulos
        chart.setTitle("Gráfico Temperatura/Humedad");
        chart.getGridLabelRenderer().setVerticalAxisTitle("Humedad");
        chart.getGridLabelRenderer().setHorizontalAxisTitle("Temperatura");

        //Min y max de X
        chart.getViewport().setXAxisBoundsManual(true);
        chart.getViewport().setMinX(0);
        chart.getViewport().setMaxX(60);

        //Min y max de y
        chart.getViewport().setYAxisBoundsManual(true);
        chart.getViewport().setMinY(0);
        chart.getViewport().setMaxY(100);

    }

    public void GetTempHum (){
        new ReadJSON().execute(stringUrl);
    }

    public void ObtenerDatosReady(){
        if (Contador <= 5){
            new ReadJSON().execute(stringUrl);
        } else {
            try {
                Collections.sort(datos);
                Iterator itDatos=datos.iterator();

                while (itDatos.hasNext()) {
                    SensorData elementoLista = (SensorData) itDatos.next();
                    elementoLista.getTemperatura();
                    elementoLista.getHumedad();
                    series.appendData(new DataPoint(elementoLista.getTemperatura(), elementoLista.getHumedad()), false, 100);
                    chart.addSeries(series);
                }
            }catch (Exception exc){
                exc.printStackTrace();
            }

            Toast.makeText(this, "Gráfico cargado", Toast.LENGTH_SHORT).show();
        }
    }

    private class ReadJSON extends AsyncTask<String, Void, String> {    //Tarea asíncrona para leer JSON

        protected String doInBackground(String... urls) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return readJSONFeed(urls[0]);
        }

        protected void onPostExecute(String result) {
            try {
                JSONArray array = new JSONArray(result);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject row = array.optJSONObject(i);

                    if (row.getString("ID").equals(id)){
                        if (row.getString("Temperatura").trim().toUpperCase().equals("-99")){
                            iTemperatura  = -99;
                        } else {
                            dTemperatura = Double.parseDouble(row.getString("Temperatura"));
                            iTemperatura = dTemperatura.intValue();
                        }
                        if (row.getString("Humedad").trim().toUpperCase().equals("-99")){
                            iHumedad  = -99;
                        } else {
                            dHumedad = Double.parseDouble(row.getString("Humedad"));
                            iHumedad = dHumedad.intValue();
                        }

                        datos.add(new SensorData(iTemperatura, iHumedad));
                        Contador = Contador + 1;
                        ObtenerDatosReady();
                    }
                }
            } catch (Exception e) {
                Log.d("ReadJSON", e.getLocalizedMessage());
            }
        }
    }

    public String readJSONFeed(String URL) {       //Descarga los datos JSON
        StringBuilder stringBuilder = new StringBuilder();
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(URL);
        try {
            HttpResponse response = httpClient.execute(httpGet);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == 200) {
                HttpEntity entity = response.getEntity();
                InputStream inputStream = entity.getContent();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.toString().replace("NaN","-99");
                    stringBuilder.append(line);
                }
                inputStream.close();
            } else {
                Log.d("JSON", "Error al descargar el archivo");
            }
        } catch (Exception e) {
            Log.d("readJSONFeed", e.getLocalizedMessage());
        }
        return stringBuilder.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
