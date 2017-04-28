package cl.martinez.franco.efficienthome;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
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

public class LocationActivity extends AppCompatActivity {
    private EditText Nombre, ID;
    private CheckBox ChkTH, ChkV;
    private String url;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.location);

        ID = (EditText) findViewById(R.id.ID);
        Nombre = (EditText) findViewById(R.id.txtNombre);
        ChkTH = (CheckBox) findViewById(R.id.ChkTH);
        ChkV = (CheckBox) findViewById(R.id.ChkV);

        SharedPreferences prefs = getSharedPreferences("Configuraciones", Context.MODE_PRIVATE);

        url = prefs.getString("IPArduinoYun", "10.40.4.3");
        url = "http://" + url + "/arduino/216";

    }

    public void btnGuardaUbicacion(View view){
        if(ID.getText().length()==0){ //Revision de los datos si estan vacíos
            ID.setError("Debe ingresar identificador");
        }
        if(Nombre.getText().length()==0){
            Nombre.setError("Debe ingresar nombre");
        }
        if(ChkTH.isChecked() == false && ChkV.isChecked() == false){
            ChkTH.setError("Debe existir al menos un tipo de sensor");
            ChkV.setError("Debe existir al menos un tipo de sensor");
        }
        else{
            try {   //Valida que exista ID y luego guarda
                new ReadJSON().execute(url);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class ReadJSON extends AsyncTask<String, Void, String> {    //Tarea asíncrona para leer JSON

        protected String doInBackground(String... urls) {   //Obtiene la info. del clima y lo retorna a onPostExecute
            return readJSONFeed(urls[0]);
        }

        protected void onPostExecute(String result) {
            try {
                Boolean existe = false;
                JSONArray array = new JSONArray(result);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject row = array.optJSONObject(i);

                    if (row.getString("ID").trim().equals(ID.getText().toString().trim())){
                        existe = true;
                    }
                }
                if (existe){
                    GuardarUbicacion();
                } else {
                    Toast.makeText(getBaseContext(),"No se encuentra el sensor ID "+ ID.getText().toString() + " conectado",Toast.LENGTH_LONG).show();
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

    public void GuardarUbicacion(){
        AdminSQLite admin = new AdminSQLite(this, "efficienthome", null, 1);
        SQLiteDatabase bd = admin.getWritableDatabase();

        Integer id = Integer.parseInt(ID.getText().toString());
        String nombre = Nombre.getText().toString().toUpperCase();
        String existeth;
        String existev;

        if (ChkTH.isChecked()) {
            existeth = "SI";
        } else {
            existeth = "NO";
        }

        if (ChkV.isChecked()) {
            existev = "SI";
        } else {
            existev = "NO";
        }

        Cursor fila=bd.rawQuery("select * from ubicacion where nombreubicacion='" + nombre + "' or codigoubicacion =" + id, null); //Validación de datos

        if(!fila.moveToFirst()){
            ContentValues datos=new ContentValues();
            datos.put("codigoubicacion", id);
            datos.put("nombreubicacion", nombre);
            datos.put("existeth", existeth);
            datos.put("existev", existev);
            bd.insert("ubicacion",null,datos);
            bd.close();

            Toast.makeText(this, "Ubicación agregada", Toast.LENGTH_LONG).show();
            finish();
        } else {
            Toast.makeText(this, "El Identificador o el Nombre ya existe", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }
}
