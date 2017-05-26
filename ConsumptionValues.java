package cl.martinez.franco.efficienthome;

import android.os.AsyncTask;
import android.widget.Toast;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

public class ConsumptionValues {
    private double valorkwh;
    private double valortransporte;
    private int valoradministracion;
    private String ip, menu;
    private ScrollingConsumptionActivity consumo;
    private ScrollingConfigurationActivity configuracion;

    public void setValorkwh(double valorkwh) {
        this.valorkwh = valorkwh;
    }

    public void setValortransporte(double valortransporte) {
        this.valortransporte = valortransporte;
    }

    public void setValoradministracion(int valoradministracion) {
        this.valoradministracion = valoradministracion;
    }

    public ConsumptionValues(ScrollingConsumptionActivity consumo, String ip, String menu) {
        this.consumo = consumo;
        this.ip = ip;
        this.menu = menu;
    }

    public ConsumptionValues(ScrollingConfigurationActivity configuracion, String ip, String menu) {
        this.configuracion = configuracion;
        this.ip = ip;
        this.menu = menu;
    }

    public ConsumptionValues(double valorkwh, double valortransporte, int valoradministracion) {
        this.valorkwh = valorkwh;
        this.valortransporte = valortransporte;
        this.valoradministracion = valoradministracion;
    }

    public double getValorkwh() {
        return valorkwh;
    }

    public double getValortransporte() {
        return valortransporte;
    }

    public int getValoradministracion() {
        return valoradministracion;
    }

    private void message(String msg) {
        Toast.makeText(consumo, msg, Toast.LENGTH_LONG).show();
    }

    private class DownloadFilesTaskValores extends AsyncTask<URL, Integer, String> {
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
                return "Error de conexión";
            } finally {
                urlConnection.disconnect();
            }
        }

        protected void onPostExecute(String resultado) {
            if (menu.equals("consumo")){
                consumo.recibirValores(resultado);
            } else {
                configuracion.recibirValores(resultado);
            }
        }
    }

    public void obtenerValores(){
        System.out.println("esta es la ip que llega: " + ip.trim());
        URL url = null;
        try {
            url = new URL("http://"+ip.trim()+"/informe.php?caso=5");
        } catch (Exception e) {
            message("Error 4");
        }
        new DownloadFilesTaskValores().execute(url);
    }

    public void updateValores(){
        URL url = null;
        try {
            url = new URL("http://"+ip.trim()+"/informe.php?caso=7&valorkwh=" + String.valueOf(valorkwh) + "&valortrans=" + String.valueOf(valortransporte) + "&valoradmin=" + String.valueOf(valoradministracion));
            //url = new URL("hhtp://"+ip.trim()+"/informe.php?");
        } catch (Exception e) {
            message("Error update");
        }
        new UpdateTaskValores().execute(url);
    }

    private class UpdateTaskValores extends AsyncTask<URL, String, String> {
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
            configuracion.recibirUpdate(resultado);
        }
    }
}
