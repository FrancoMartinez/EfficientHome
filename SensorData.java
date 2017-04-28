package cl.martinez.franco.efficienthome;

/**
 * Created by Franco on 28-09-16.
 */
public class SensorData implements Comparable<SensorData> {
    private int Temperatura;
    private int Humedad;

    public SensorData(int temperatura, int humedad) {
        Temperatura = temperatura;
        Humedad = humedad;
    }

    public int getTemperatura() {
        return Temperatura;
    }

    public int getHumedad() {
        return Humedad;
    }

    @Override
    public int compareTo(SensorData ds) {
        if (Temperatura < ds.Temperatura) {
            return -1;
        }
        if (Temperatura > ds.Temperatura) {
            return 1;
        }
        return 0;
    }
}
