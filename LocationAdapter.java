package cl.martinez.franco.efficienthome;

import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by Franco on 17-11-16.
 */
public class LocationAdapter extends BaseAdapter implements ListAdapter {
    private ArrayList<String> list = new ArrayList<String>();
    private Context context;

    public LocationAdapter(ArrayList<String> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int pos) {
        return list.get(pos);
    }

    @Override
    public long getItemId(int pos) { return pos; }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.location_items, null);
        }

        TextView listItemText = (TextView)view.findViewById(R.id.txtUbicacion);
        listItemText.setText(list.get(position));

        ImageButton btnDelete = (ImageButton)view.findViewById(R.id.btnDelete);


        btnDelete.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                AlertDialog.Builder mBuilder = new AlertDialog.Builder(v.getRootView().getContext())
                        .setTitle("Eliminar")
                        .setMessage("¿Está seguro que desea eliminar la ubicación?");

                mBuilder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int id) {
                        eliminaUbicacion(list.get(position));
                        list.remove(position);
                        notifyDataSetChanged();
                    }
                });

                mBuilder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int id) {
                    }
                });

                mBuilder.show();
            }
        });

        return view;
    }

    public void eliminaUbicacion(String ubicacion){
        AdminSQLite admin = new AdminSQLite(context,"efficienthome",null,1);
        SQLiteDatabase bd = admin.getWritableDatabase();

        String separador[] = ubicacion.trim().split("-");
        int contador = bd.delete("ubicacion", "codigoubicacion=" + separador[0], null);

        bd.close();

        if (contador == 1){
            Toast.makeText(context,"Ubicación eliminada",Toast.LENGTH_SHORT).show();
        }
    }
}
