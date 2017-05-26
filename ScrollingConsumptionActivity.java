package cl.martinez.franco.efficienthome;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class ScrollingConsumptionActivity extends AppCompatActivity {

    private TextView MontokWh, MontoAdminServ, MontoTranskwh, MontoPago, MontoPromedio;
    private EditText MetaConsumo;
    private SharedPreferences prefs;
    private Double Gasto, Valorkwh, ValorTranskwh;
    private Integer kwhActual, ultimoDiaActual;
    private String ip, fechamax, fechamin;
    private BarChart GraficoAnual;
    private LineChart GraficoDiario;
    private ArrayList<BarEntry> DatosMes;
    private int año, mes, ValorAdminkwh;
    private Calendar cal, cal2;
    private Boolean existe;
    private LineDataSet dataset;
    private LineData data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scrolling_electricity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        prefs = getSharedPreferences("Configuraciones", Context.MODE_PRIVATE);
        ip = prefs.getString("IPRaspberry", "");

        //Carga controles

        MontokWh = (TextView) findViewById(R.id.lblMontokWh);
        MontoAdminServ = (TextView) findViewById(R.id.lblMontoAdminServ);
        MontoTranskwh = (TextView) findViewById(R.id.lblMontoTranskwh);
        MontoPago = (TextView) findViewById(R.id.lblMontoPago);
        MontoPromedio = (TextView) findViewById(R.id.txtConsumoPromedio);
        MetaConsumo = (EditText) findViewById(R.id.txtConsumoEsperado);

        MetaConsumo.setText(prefs.getString("MetaConsumo", "0"));

        //Año y mes actual
        año=0;
        mes=0;
        cal = Calendar.getInstance();
        año = cal.get(Calendar.YEAR);
        mes = cal.get(Calendar.MONTH);
        mes = mes + 1;  //Los meses están indexados desde 0

        System.out.println(String.valueOf(año) + " " + String.valueOf(mes));

        ultimoDiaActual = obtenerUltimoDiaMes(año, mes);


        if (ip.equals("")){
            Toast.makeText(this, "No existe una dirección de Raspberry PI registrada.", Toast.LENGTH_LONG).show();
        } else {
            //Para el consumo
            cargaConsumo();
        }
    }

    private void cargaConsumo(){
        CurrentPotency potenciaActual = new CurrentPotency(this, ip);//llamo a la clase que maneja la tabla de datos del dia actual
        potenciaActual.obtenerKWH(); //solicito los datos almacenados en la tabla
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                esperarKwh();//una vez pasa el segundo continuo
            }
        }, 1000); //se espera un segundo debido a que son muchos datos

    }

    private void message(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    public void esperarKwh(){ //solicito la tension de esta vivienda y sus respectivos precios
        ConsumptionValues valor = new ConsumptionValues(this, ip, "consumo");
        valor.obtenerValores();
    }

    public void recibirListaPotencia (String resultado){ // recibo los watts
        if(resultado.equals("Error 9") || resultado.equals("Error de conexión")){
            message(resultado);
            //finish();
        }
        else{
            calcularKwh(resultado); //envia a calcular los kwh
        }
    }

    public void recibirValores(String resultado){
        if (resultado.equals("Error de conexión")){
            message(resultado);
            //finish();
        } else {
            String[] precios = resultado.split(","); //lo recibido por consulta
            System.out.println(precios[0] + " " + precios[1] + " " + precios[2]);
            ConsumptionValues valores = new ConsumptionValues(Double.parseDouble(precios[0]),
                    Double.parseDouble(precios[1]), Integer.parseInt(precios[2]));
            calcularPrecio(valores);
        }
    }

    private void calcularPrecio(ConsumptionValues valores){
        Valorkwh = valores.getValorkwh();
        ValorAdminkwh = valores.getValoradministracion();
        ValorTranskwh = valores.getValortransporte();
        Gasto = 0.0;
        Gasto = valores.getValorkwh() * kwhActual;
        MontokWh.setText(Formatear(Redondea(Gasto)) + " (" + Formatear(Redondea(kwhActual)) + " kWh)");
        Gasto = 0.0;
        Gasto = valores.getValortransporte() * kwhActual;
        MontoTranskwh.setText(Formatear(Redondea(Gasto)));
        Gasto = 0.0;
        Gasto = Double.parseDouble(String.valueOf(valores.getValoradministracion()));
        MontoAdminServ.setText(Formatear(Redondea(Gasto)));
        Gasto = valores.getValorkwh() * kwhActual + valores.getValortransporte() * kwhActual + valores.getValoradministracion();
        MontoPago.setText(Formatear(Redondea(Gasto)));

        CalcularPromedio();

        //Gráficos
        CargarGraficoAnual();
        CargarGraficoDiario();
    }

    private void calcularKwh(String resultado){
        new CurrentPotency(this, ip);
        if (resultado.equals("") ){
            kwhActual = 0;
        } else if (resultado.equals("Error de conexión")) {
            message(resultado);
        } else {
            kwhActual = Integer.parseInt(resultado);
        }
    }

    public void CalcularPromedio(){
        //3 meses anteriores
        cal2 = GregorianCalendar.getInstance();
        cal2.add(Calendar.MONTH, -1);

        int mesmax = cal2.get(Calendar.MONTH) + 1; // por el índice que es 0 se suma 1
        int añomax  = cal2.get(Calendar.YEAR);
        int ultimomax = obtenerUltimoDiaMes(añomax, mesmax);

        if (mesmax < 10) {
            fechamax = String.valueOf(añomax) + "0" + String.valueOf(mesmax) + String.valueOf(ultimomax);
        } else {
            fechamax = String.valueOf(añomax) + String.valueOf(mesmax) + String.valueOf(ultimomax);
        }

        cal2.add(Calendar.MONTH, -2);
        int mesmin = cal2.get(Calendar.MONTH) + 1;
        int añomin  = cal2.get(Calendar.YEAR);

        if (mesmin < 10) {
            fechamin = String.valueOf(añomin) + "0" + String.valueOf(mesmin) + "01";
        } else {
            fechamin = String.valueOf(añomin) + String.valueOf(mesmin) + "01";
        }

        AverageCost promediocosto = new AverageCost(this,ip, fechamin, fechamax);
        promediocosto.obtenerPromedioCosto();
    }
    public void CargarGraficoAnual() {

        String año="";
        Calendar cal = GregorianCalendar.getInstance();
        año = String.valueOf(cal.get(Calendar.YEAR));

        MonthlyPotency potenciaMensual = new MonthlyPotency(this,ip, año);
        potenciaMensual.obtenerPotenciaMensual();

    }

    public void CargarGraficoDiario() {
        DailyPotency potenciaDiaria = new DailyPotency(this,ip);
        potenciaDiaria.obtenerPotenciaDiaria();
    }

    public void recibirConsumoPorDia(String resultado){
        if(resultado.equals("Error 8")){
            message("No existe información de consumo por día, intente mañana.");
        }
        else {
            GraficoDiario = (LineChart) findViewById(R.id.LineChart);

            ArrayList<String> Dias = new ArrayList<String>();
            ArrayList<Entry> Gasto = new ArrayList<>();

            for (int i = 1; i <= ultimoDiaActual; i++) {   //Lleno el label de todos los días del mes
                Dias.add(String.valueOf(i));
            }

            for (int x = 0; x < ultimoDiaActual; x++) {
                existe = false;
                String[] diasconsumo = resultado.split(";");
                for (String temporal : diasconsumo) {
                    String[] dividirDias = temporal.split(",");
                    int diarecuperado = Integer.parseInt(dividirDias[0]);
                    Double temp = Double.parseDouble(dividirDias[1]);
                    //System.out.println("datos " + String.valueOf(temp) + " " + String.valueOf(Valorkwh) + " " + String.valueOf(ValorTranskwh));
                    temp = temp * (Valorkwh + ValorTranskwh);
                    int costorecuperado = (int) Redondea(temp +  (ValorAdminkwh/ultimoDiaActual));
                    if (x == diarecuperado -1) {
                        existe = true;
                        Gasto.add(new Entry(costorecuperado, x));
                    }
                }

                if (existe == false){
                    Gasto.add(new Entry(0,x));
                }
            }

            String MesDelAño = "";

            if (mes == 1) {
                MesDelAño = "Enero";
            } else if (mes == 2) {
                MesDelAño = "Febrero";
            } else if (mes == 3) {
                MesDelAño = "Marzo";
            } else if (mes == 4) {
                MesDelAño = "Abril";
            } else if (mes == 5) {
                MesDelAño = "Mayo";
            } else if (mes == 6) {
                MesDelAño = "Junio";
            } else if (mes == 7) {
                MesDelAño = "Julio";
            } else if (mes == 8) {
                MesDelAño = "Agosto";
            } else if (mes == 9) {
                MesDelAño = "Septiembre";
            } else if (mes == 10) {
                MesDelAño = "Octubre";
            } else if (mes == 11) {
                MesDelAño = "Noviembre";
            } else if (mes == 12) {
                MesDelAño = "Diciembre";
            }

            if (Integer.valueOf(MetaConsumo.getText().toString()) > 0) {
                ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
                ArrayList<Entry> Meta = new ArrayList<>();
                int meta = Integer.valueOf(MetaConsumo.getText().toString());
                Double metadiaria = Redondea(meta / ultimoDiaActual);
                for (int i = 0; i < ultimoDiaActual; i++) {
                    Meta.add(new Entry(Integer.parseInt(String.valueOf(Formatear(metadiaria))), i));
                }

                LineDataSet line1 = new LineDataSet(Gasto, "Consumo diario mes de " + MesDelAño + " en pesos");
                line1.setColor(Color.BLUE);
                line1.setFillColor(Color.BLUE);
                line1.setDrawCircles(false);
                line1.setDrawValues(false);
                dataSets.add(line1);

                LineDataSet line2 = new LineDataSet(Meta, "Consumo esperado al día");
                line2.setFillColor(Color.RED);
                line2.setDrawCircles(false);
                line2.setColor(Color.RED);
                line2.setDrawValues(false);
                dataSets.add(line2);
                data = new LineData(Dias, dataSets);
                GraficoDiario.setData(data);
            } else {
                dataset = new LineDataSet(Gasto, "Consumo diario mes de " + MesDelAño + " en pesos");
                data = new LineData(Dias, dataset);
                dataset.setValueTextColor(Color.BLUE);
                dataset.setDrawValues(false);
                dataset.setDrawCubic(true);
                dataset.setDrawFilled(true);
                GraficoDiario.setData(data);
            }

                 new LineDataSet(Gasto, "Consumo esperado");


            YAxis left = GraficoDiario.getAxisLeft();
            left.setDrawAxisLine(false); // no axis line
            left.setDrawGridLines(false); // no grid lines
            left.setDrawZeroLine(true); // draw a zero line
            left.setAxisMinValue(0f);   // start at zero

            YAxis right = GraficoDiario.getAxisRight();
            right.setDrawLabels(false); // no axis labels
            right.setDrawGridLines(false); // no grid lines


            GraficoDiario.setDescription("");
            GraficoDiario.setFocusable(false);

            XAxis xAxis = GraficoDiario.getXAxis();
            xAxis.setLabelRotationAngle(-30);

            GraficoDiario.setData(data);
            GraficoDiario.animateY(2500);
        }
    }

    public void recibirConsumoPorMes(String resultado) {
        if(resultado.equals("Error 7")){
            message(resultado);
            finish();
        }
        else{
            GraficoAnual = (BarChart) findViewById(R.id.BarGraph);
            DatosMes = new ArrayList<>();

            ArrayList<String> Meses = new ArrayList<>();
            Meses.add("Ene");
            Meses.add("Feb");
            Meses.add("Mar");
            Meses.add("Abr");
            Meses.add("May");
            Meses.add("Jun");
            Meses.add("Jul");
            Meses.add("Ago");
            Meses.add("Sep");
            Meses.add("Oct");
            Meses.add("Nov");
            Meses.add("Dic");

            DatosMes.add(new BarEntry(0f,0));
            DatosMes.add(new BarEntry(0f,1));
            DatosMes.add(new BarEntry(0f,2));
            DatosMes.add(new BarEntry(0f,3));
            DatosMes.add(new BarEntry(0f,4));
            DatosMes.add(new BarEntry(0f,5));
            DatosMes.add(new BarEntry(0f,6));
            DatosMes.add(new BarEntry(0f,7));
            DatosMes.add(new BarEntry(0f,8));
            DatosMes.add(new BarEntry(0f,9));
            DatosMes.add(new BarEntry(0f,10));
            DatosMes.add(new BarEntry(0f,11));

            String[] mesesconsumo = resultado.split(";");

            for (String temporal : mesesconsumo) {
                String[] dividirMes = temporal.split(",");

                if (dividirMes[0].equals("1")) {
                    DatosMes.remove(0);
                    DatosMes.add(new BarEntry(Float.parseFloat(dividirMes[2]+ "f"), 0));
                }

                if (dividirMes[0].equals("2")) {
                    DatosMes.remove(1);
                    DatosMes.add(new BarEntry(Float.parseFloat(dividirMes[2]+ "f"), 1));
                }

                if (dividirMes[0].equals("3")) {
                    DatosMes.remove(2);
                    DatosMes.add(new BarEntry(Float.parseFloat(dividirMes[2]+ "f"), 2));
                }

                if (dividirMes[0].equals("4")) {
                    DatosMes.remove(3);
                    DatosMes.add(new BarEntry(Float.parseFloat(dividirMes[2]+ "f"), 3));
                }

                if (dividirMes[0].equals("5")) {
                    DatosMes.remove(4);
                    DatosMes.add(new BarEntry(Float.parseFloat(dividirMes[2]+ "f"), 4));
                }

                if (dividirMes[0].equals("6")) {
                    DatosMes.remove(5);
                    DatosMes.add(new BarEntry(Float.parseFloat(dividirMes[2]+ "f"), 5));
                }

                if (dividirMes[0].equals("7")) {
                    DatosMes.remove(6);
                    DatosMes.add(new BarEntry(Float.parseFloat(dividirMes[2]+ "f"), 6));
                }

                if (dividirMes[0].equals("8")) {
                    DatosMes.remove(7);
                    DatosMes.add(new BarEntry(Float.parseFloat(dividirMes[2]+ "f"), 7));
                }

                if (dividirMes[0].equals("9")) {
                    DatosMes.remove(8);
                    DatosMes.add(new BarEntry(Float.parseFloat(dividirMes[2]+ "f"), 8));
                }

                if (dividirMes[0].equals("10")) {
                    DatosMes.remove(9);
                    DatosMes.add(new BarEntry(Float.parseFloat(dividirMes[2]+ "f"), 9));
                }

                if (dividirMes[0].equals("11")) {
                    DatosMes.remove(10);
                    DatosMes.add(new BarEntry(Float.parseFloat(dividirMes[2]+ "f"), 10));
                }

                if (dividirMes[0].equals("12")) {
                    DatosMes.remove(11);
                    DatosMes.add(new BarEntry(Float.parseFloat(dividirMes[2]+ "f"), 11));
                }
            }

            BarDataSet BarDatos = new BarDataSet(DatosMes, "Consumo mensual del año " + String.valueOf(año) + " en pesos");

            BarData DataAnual = new BarData(Meses, BarDatos);
            DataAnual.setValueTextColor(Color.BLUE);

            YAxis left = GraficoAnual.getAxisLeft();
            left.setDrawAxisLine(false); // no axis line
            left.setDrawGridLines(false); // no grid lines
            left.setDrawZeroLine(true); // draw a zero line
            left.setAxisMinValue(0f);   // start at zero

            YAxis right = GraficoAnual.getAxisRight();
            right.setDrawLabels(false); // no axis labels
            right.setDrawGridLines(false); // no grid lines

            GraficoAnual.setData(DataAnual);
            GraficoAnual.setDescription("");
            GraficoAnual.setFocusable(false);
            GraficoAnual.animateY(2500);

            XAxis xAxis = GraficoAnual.getXAxis();
            xAxis.setLabelRotationAngle(-30);
        }
    }

    public void GuardarMetaConsumo(View v){
        if (MetaConsumo.getText().toString().equals("")) {MetaConsumo.setText("0");}
        if (Integer.parseInt(MetaConsumo.getText().toString()) >= 0) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("MetaConsumo");
            editor.commit();
            editor.putString("MetaConsumo", MetaConsumo.getText().toString());
            editor.commit();

            CargarGraficoDiario();

            Toast.makeText(this, "Meta de consumo establecida", Toast.LENGTH_LONG).show();

            Intent servintent = new Intent(this, ServicePushConsumo.class);
            PendingIntent pintent = PendingIntent.getService(this, 0, servintent, 0);
            AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
            alarm.cancel(pintent);
            alarm.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),5000, pintent);
            //startService(new Intent(this, ServicePushConsumo.class));
        } else {
            Toast.makeText(this, "Debe ingresar meta", Toast.LENGTH_LONG).show();
        }
    }

    public void recibirPromedio(String resultado){
        if(resultado.equals("Error 10")){
            message(resultado);
            finish();
        } else {
            MontoPromedio.setText(resultado);
        }
    }

    double Redondea(double d)
    {
        DecimalFormat decimal = new DecimalFormat("#");
        return Double.valueOf(decimal.format(d));
    }

    public static String Formatear(double d)
    {
        if(d == (int) d)
            return String.format("%d",(int)d);
        else
            return String.format("%s",d);
    }

    public int obtenerUltimoDiaMes (int anio, int mes) {
        Calendar calendario=Calendar.getInstance();
        calendario.set(anio, mes-1, 1);
        return calendario.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

}
