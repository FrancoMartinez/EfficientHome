package cl.martinez.franco.efficienthome;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.media.Image;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import static cl.martinez.franco.efficienthome.R.mipmap.btnrojo;

/**
 * Created by Franco on 20-12-16.
 */
public class SensorAdapter extends BaseAdapter implements ListAdapter {
    private ArrayList<String> list = new ArrayList<String>();
    private Context context;
    private static boolean valido;
    private String id;

    public SensorAdapter(ArrayList<String> list, Context context) {
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
            view = inflater.inflate(R.layout.sensor_items, null);
        }

        TextView listItemText = (TextView)view.findViewById(R.id.txtSensores);
        listItemText.setText(list.get(position));

        ImageButton imageItem = (ImageButton)view.findViewById(R.id.btnSensor);

        String sensores[] = listItemText.getText().toString().split("-");

        id = sensores[0].trim();

        valido = true;
        if (listItemText.getText().toString().indexOf("-99") > 0){
            valido = false;
        }

        if (valido){
            imageItem.setBackgroundResource(R.mipmap.btnverde);
        } else {
            imageItem.setBackgroundResource(R.mipmap.btnrojo);
        }


        notifyDataSetChanged();

        imageItem.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (valido){
                    Intent intent = new Intent(context, ChartActivity.class);
                    intent.putExtra("ID", id);
                    context.startActivity(intent);
                }
            }
        });

        return view;
    }
}
