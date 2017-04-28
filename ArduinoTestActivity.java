package cl.martinez.franco.efficienthome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import java.lang.reflect.Array;

public class ArduinoTestActivity extends AppCompatActivity {
    private String url;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.arduino_test);

        SharedPreferences prefs =
                getSharedPreferences("Configuraciones",Context.MODE_PRIVATE);

        url = prefs.getString("IPArduinoYun", "10.40.4.3");
        url = "http://" + url + "/arduino/216";
    }

    public void btnGetData(View view) {
        try {
            new ReadJSON().execute(url);        //Envía la URL
        } catch (Exception e) {
            Toast.makeText(getBaseContext(), "Error al leer datos", Toast.LENGTH_SHORT).show();
        }
    }

    private class ReadJSON extends AsyncTask<String, Void, String> {    //Tarea asíncrona para leer JSON

        protected String doInBackground(String... urls) {   //Obtiene la info. del clima y lo retorna a onPostExecute
            return readJSONFeed(urls[0]);
        }

        protected void onPostExecute(String result) {
            try {
                JSONArray array = new JSONArray(result);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject row = array.optJSONObject(i);

                    String estado = "";
                    if (row.getString("Ventana").equals("0")) {
                        estado = "Cerrada";
                    }
                    if (row.getString("Ventana").equals("1")) {
                        estado = "Abierta";
                    }
                    Toast.makeText(getBaseContext(),
                            "ID: "
                                    + row.getString("ID") + " - Temperatura: "
                                    + row.getInt("Temperatura") + "ºC - Humedad: "
                                    + row.getInt("Humedad") + "% - Ventana: "
                                    + estado,
                            Toast.LENGTH_SHORT).show();
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
    protected void onDestroy(){
        super.onDestroy();
    }
}