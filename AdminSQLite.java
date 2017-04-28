package cl.martinez.franco.efficienthome;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Franco on 16/11/2016.
 */
public class AdminSQLite extends SQLiteOpenHelper {

    Context sqlcontext;
    String sqlCreateUbicacion = "CREATE TABLE ubicacion (codigoubicacion INTEGER PRIMARY KEY, nombreubicacion STRING, existeth STRING, existev STRING)";
    String sqlCreateHistorialVent = "CREATE TABLE historialvent (codigoventilacion INTEGER PRIMARY KEY AUTOINCREMENT, minutosabierta INTEGER, minutoscerrada INTEGER, fecha DATETIME)";
    String sqlCreateHistorialNotif = "CREATE TABLE historialnotif (codigonotificacion INTEGER PRIMARY KEY AUTOINCREMENT, fecha DATETIME, descripcion STRING)";

    public AdminSQLite(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        sqlcontext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(sqlCreateUbicacion);
        db.execSQL(sqlCreateHistorialVent);
        db.execSQL(sqlCreateHistorialNotif);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS ubicacion");
        db.execSQL(sqlCreateUbicacion);

        db.execSQL("DROP TABLE IF EXISTS historialvent");
        db.execSQL(sqlCreateHistorialVent);

        db.execSQL("DROP TABLE IF EXISTS historialnotif");
        db.execSQL(sqlCreateHistorialNotif);
    }
}
