package cl.martinez.franco.efficienthome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;

public class ChartActivity extends AppCompatActivity {
    private String ApiURL, API, ip;
    private Integer iTemperatura, iHumedad;
    private Boolean ExisteT, ExisteH, ExisteDato;
    private ScatterChart Grafico;
    private ArrayList<Entry> Dato;
    private ArrayList<String> Datox;
    private ScatterDataSet dataset;
    private ScatterData scatter;
    private java.net.URL URL;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chart);

        prefs = getSharedPreferences("Configuraciones", Context.MODE_PRIVATE);
        API = prefs.getString("APIKEY", "");

        //URL RASPBERRY PI
        ip = prefs.getString("IPRaspberry", "");

        CargarUsuario();
    }

    private void message(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    public void CargarUsuario(){
        URL urlusr = null;
        try {
            urlusr = new URL("http://"+ip.trim()+"/informe.php?caso=9");
        } catch (Exception e) {
            System.out.println("No se pudo rescatar usuario");
        }
        new DownloadFilesTaskUsuario().execute(urlusr);
    }

    public void Cargar(){
        new ReadJSON().execute(URL);
    }

    public void ConfigurarGrafico(){
        Grafico = (ScatterChart) findViewById(R.id.ScatterChart);

        Dato = new ArrayList<>();
        Datox = new ArrayList<String>();

        YAxis left = Grafico.getAxisLeft();
        left.setDrawAxisLine(false); // no axis line
        left.setDrawGridLines(false); // no grid lines
        left.setDrawZeroLine(true); // draw a zero line
        left.setAxisMinValue(0f);   // start at zero


        YAxis right = Grafico.getAxisRight();
        right.setDrawLabels(false); // no axis labels
        right.setDrawGridLines(false); // no grid lines


        Grafico.setDescription("");
        Grafico.setFocusable(false);

        XAxis xAxis = Grafico.getXAxis();
        xAxis.setLabelRotationAngle(-30);

        Grafico.animateY(2500);
    }

    private class ReadJSON extends AsyncTask<URL, Void, String> {    //Tarea asíncrona para leer JSON

        protected String doInBackground(URL... urls) {
            URL url = urls[0];
            return readJSONFeed(url);
        }

        protected void onPostExecute(String result) {
            try {
                ExisteDato = Boolean.FALSE;
                JSONArray array = new JSONArray(result);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject row = array.optJSONObject(i);
                    if (!row.getString("_id").equals("0")){
                        if (row.getString("temperatura").trim().toUpperCase().equals("-99")) {
                            ExisteT = Boolean.FALSE;
                        } else {
                            iTemperatura = Integer.parseInt(row.getString("temperatura"));
                            ExisteT = Boolean.TRUE;
                        }
                        if (row.getString("humedad").trim().toUpperCase().equals("-99")) {
                            ExisteH = Boolean.FALSE;
                        }  else {
                            iHumedad = Integer.parseInt(row.getString("humedad"));
                            ExisteH = Boolean.TRUE;
                        }

                        if (ExisteT && ExisteH ){
                            Dato.add(new Entry(iTemperatura, iHumedad));
                            ExisteDato = Boolean.TRUE;
                        }

                    }
                }
                if (ExisteDato){
                    for (int i = 1; i <= 100; i++) {   //Lleno el label de humedad
                        Datox.add(String.valueOf(i));
                    }
                    dataset = new ScatterDataSet(Dato, "Temperatura vs Humedad");
                    scatter = new ScatterData(Datox, dataset);
                    dataset.setValueTextColor(Color.rgb(0,60,160));
                    dataset.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
                    dataset.setScatterShapeSize(10);
                    dataset.setDrawValues(false);
                    Grafico.setData(scatter);
                } else {
                    message("No existe información del día");
                }
            } catch (Exception e) {
                Log.d("ReadJSON", e.getLocalizedMessage());
            }
        }
    }

    public String readJSONFeed(URL url) {       //Descarga los datos JSON
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            return "Error de Conexión";
        }
        try {
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
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

    private class DownloadFilesTaskUsuario extends AsyncTask<URL, Integer, String> {    //Realiza la conexión y retorna el usuario
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
            String[] datosusuario = resultado.split(",");
            String usuario = datosusuario[0];

            if (!API.equals("")){
                Date cDate = new Date();
                String fDate = new SimpleDateFormat("dd/MM/yy").format(cDate);

                System.out.println(fDate);

                ApiURL = "https://api.mlab.com/api/1/databases/efficientmdb/collections/HistoricoCondensacion?q={'fecha':'"+fDate+"','id_usuario':"+usuario+"}&apiKey="+ API.trim();
                System.out.println(ApiURL);
                URL = null;
                try {
                    URL = new URL(ApiURL);
                } catch(MalformedURLException e) {
                    e.printStackTrace();
                }
                ConfigurarGrafico();
                Cargar();
            } else {
                message("No se ha encontrado API KEY, debe ingresarla en configuraciones");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
