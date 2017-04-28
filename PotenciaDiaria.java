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

public class PotenciaDiaria {
    private String ip;
    private ScrollingConsumptionActivity consumo;

    public PotenciaDiaria(ScrollingConsumptionActivity consumo, String ip) {
        this.consumo = consumo;
        this.ip = ip;
    }

    public void obtenerPotenciaDiaria(){
        URL url = null;
        try {
            url = new URL("http://" + ip + "/informe.php?caso=3");
        } catch (MalformedURLException e) {
            message("Error 8");
        }
        new DownloadFilesTaskPotencias().execute(url);
    }

    private void message(String msg) {
        Toast.makeText(consumo, msg, Toast.LENGTH_LONG).show();
    }

    private class DownloadFilesTaskPotencias extends AsyncTask<URL, Integer, String> {
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
            consumo.recibirConsumoPorDia(resultado);
        }
    }
}
