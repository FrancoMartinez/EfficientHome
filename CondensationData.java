package cl.martinez.franco.efficienthome;

import android.os.AsyncTask;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Franco on 23-05-17.
 */
public class CondensationData {
    private String ip, menu;
    private MainActivity main;

    public CondensationData(MainActivity main, String ip, String menu) {
        this.main = main;
        this.ip = ip;
        this.menu = menu;
    }

    public void obtenerDatosSensores(){
        URL url = null;
        try {
            url = new URL("http://" + ip + "/informe.php?caso=8");
        } catch (MalformedURLException e) {
            message("No es posible rescatar información de sensores.");
        }
        new DownloadFilesTaskDatos().execute(url);
    }

    private void message(String msg) {
        Toast.makeText(main, msg, Toast.LENGTH_LONG).show();
    }

    private class DownloadFilesTaskDatos extends AsyncTask<URL, Integer, String> {
        protected String doInBackground(URL... urls) {
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

        protected void onPostExecute(String resultado) {
            if (menu.equals("main")){
                main.recibirDatosSensores(resultado);
            } else {

            }

        }
    }
}
