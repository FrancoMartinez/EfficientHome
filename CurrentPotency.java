package cl.martinez.franco.efficienthome;

import android.os.AsyncTask;
import android.widget.Toast;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class CurrentPotency {
    private ScrollingConsumptionActivity consumo;
    private String ip;

    public CurrentPotency(ScrollingConsumptionActivity consumo, String ip) {
        this.consumo = consumo;
        this.ip = ip;
    }

    private void message(String msg) {
        Toast.makeText(consumo, msg, Toast.LENGTH_LONG).show();
    }

    private class DownloadFilesTaskKwh extends AsyncTask<URL, Integer, String> {
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
                return "Error de conexión";
            } finally {
                urlConnection.disconnect();
            }
        }

        protected void onPostExecute(String resultado) {
            consumo.recibirListaPotencia(resultado);
        }
    }

    public void obtenerKWH(){
        URL url = null;
        try {
            url = new URL("http://"+ip.trim()+"/informe.php?caso=2");
            System.out.println(url);
        } catch (Exception e) {
            message("Error 4");
        }
        new DownloadFilesTaskKwh().execute(url);
    }
}
