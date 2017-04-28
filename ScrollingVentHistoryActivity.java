package cl.martinez.franco.efficienthome;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class ScrollingVentHistoryActivity extends AppCompatActivity {

    private ListView lv;
    private ArrayList<String> historial = new ArrayList<String>();
    private ArrayAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scrolling_vent_history);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        lv = (ListView) findViewById(R.id.lstVentHistory);
        adapter = new ArrayAdapter(this, R.layout.menu_items, historial);
        lv.setAdapter(adapter);

        llenaUbicacion();

    }

    public void llenaUbicacion(){
        AdminSQLite admin = new AdminSQLite(this, "efficienthome", null, 1);
        SQLiteDatabase bd = admin.getWritableDatabase();
        String sql = "select fecha, minutosabierta, minutoscerrada from historialvent order by codigoventilacion";

        Cursor fila = bd.rawQuery(sql, null);
        if (fila.moveToFirst()){
            String fecha = fila.getString(0).trim();
            String minutosabierta = fila.getString(1).trim();
            String minutoscerrada = fila.getString(2).trim();

            historial.add(fecha + " - Min. abiertas: " + minutosabierta + " - Min. cerradas: " + minutoscerrada);

            while (fila.moveToNext()){
                fecha = fila.getString(0).trim();
                minutosabierta = fila.getString(1).trim();
                minutoscerrada = fila.getString(2).trim();

                historial.add(fecha + " - Min. abiertas: " + minutosabierta + " - Min. cerradas: " + minutoscerrada);
            }
            bd.close();

        }else{
            bd.close();
        }

    }
}
