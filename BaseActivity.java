package pedropablomoral.com.diferente;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.ProgressCallback;
import com.koushikdutta.ion.Response;

import java.io.File;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import pedropablomoral.com.diferente.clases.Articulo;
import pedropablomoral.com.diferente.clases.ArticulosSqlite;
import pedropablomoral.com.diferente.clases.DiferenteSqlite;
import pedropablomoral.com.diferente.clases.Respuesta;
import pedropablomoral.com.diferente.clases.Usuario;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static pedropablomoral.com.diferente.Constantes.LOG_TAG;

public class BaseActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private SharedPreferences mSharedPreferences;
    private BaseActivity thisActivity;
    public String token;
    public String email;
    public String refreshtoken;
    public String codcli;
    public String usuario,tienda;
    DiferenteSqlite bdpedidos = DiferenteSqlite.getInstance(thisActivity);
    ArticulosSqlite bdarticulos = ArticulosSqlite.getInstance();
    ProgressBar progressBar;
    Articulo prueba;

    protected void onCreateDrawer() {

        setContentView(R.layout.activity_base);
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        initSharedPreferences();
        if (Permisos()){
            CrearDirectorios();
            ComprobarBaseDatos();
            prueba = bdarticulos.GetArticulo("28859");
            Log.d(LOG_TAG, "onCreateDrawer: "+prueba.getDescart());
            /** Comprueba si hay token y si no ha expirado */
            if (!token.equals("") && ComprobarToken()) {

                thisActivity = this;
                Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
                setSupportActionBar(toolbar);
                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

                ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                        this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
                drawer.addDrawerListener(toggle);
                toggle.syncState();

                NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
                navigationView.setNavigationItemSelectedListener(this);
                View header = navigationView.getHeaderView(0);


                initSharedPreferences();
                TextView textView= (TextView) header.findViewById(R.id.nombreusuario);
                TextView tienda = (TextView) header.findViewById(R.id.tienda);
                tienda.setText(Devuelvetienda());
                textView.setText(usuario);
                Log.d(LOG_TAG, "onCreateDrawer: "+ Usuario.getUsuario().getNomcli());


            } else {
                Logout();

            }

        }
        //Permisos();



    }
    @Override

    protected void onResume (){
        super.onResume();
        initSharedPreferences();
        if (!token.equals("") && ComprobarToken()) {

        } else {
            Logout();
        }

    }
    private void Logout() {
 /**       Vacia las variables token y email, limpia el historial de la aplicacion
                y devuelve al usuario a la pantalla de login */
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(Constantes.TOKEN, "");
        editor.putString(Constantes.EMAIL, "");
        editor.putString(Constantes.REFRESH_TOKEN, "");
        editor.putString(Constantes.CODCLI, "");
        editor.apply();
        finish();
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_gallery) {
            Intent intent = new Intent(this, PerfilActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_manage) {
            Logout();
        } else if (id == R.id.nav_pedidos_internos) {
            Intent intent = new Intent(this, PedidosInternosActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void initSharedPreferences() {
        /** Asignamos los valores correspondientes a las avriables token y email
         * para que estén disponibles en todas las Activities que extiendan
         * de nuestra baseActivity y poder incluir el token en todas las peticiones
         * al servidor*/
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        token = mSharedPreferences.getString(Constantes.TOKEN, "");
        usuario = mSharedPreferences.getString(Constantes.NOMCLI, "");
        refreshtoken = mSharedPreferences.getString(Constantes.REFRESH_TOKEN, "");
        codcli = mSharedPreferences.getString(Constantes.CODCLI, "");
        Usuario.getUsuario().setNOMCLI(usuario);
        Usuario.getUsuario().setCODCLI(codcli);
    }

    private boolean ComprobarToken() {
      /** Comprueba si el token no ha expirado */

        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(Constantes.SECRET.getBytes())
                    .parseClaimsJws(token).getBody();
            System.out.println("body: " + claims.toString());
            System.out.println("Issuer: " + claims.getIssuer());
            System.out.println("Expiration: " + claims.getExpiration());
            return true;
        } catch (ExpiredJwtException ex) {
            System.out.println("exception : " + ex.getMessage());
            //RefrescarToken();

                return false;

        }


    }
    private void CrearDirectorios () {
        File directorioDiferente = new File(Environment.getExternalStorageDirectory()+"/diferente/");
        File directorioPedidos = new File(Environment.getExternalStorageDirectory()+"/diferente/pedidos/");
        File directorioPedidosPendientes = new File(Environment.getExternalStorageDirectory()+"/diferente/pedidos/pendientes/");
        File directorioPedidosGuardados = new File(Environment.getExternalStorageDirectory()+"/diferente/pedidos/grabados/");
        File directorioInventarios = new File(Environment.getExternalStorageDirectory()+"/diferente/inventarios/");
        if(!directorioDiferente.exists())
        {
            directorioDiferente.mkdir();

        }

        if(!directorioPedidos.exists())
        {
            directorioPedidos.mkdir();

        }


        if(!directorioPedidosPendientes.exists())
        {
            directorioPedidosPendientes.mkdir();

        }
        if(!directorioPedidosGuardados.exists())
        {
            directorioPedidosGuardados.mkdir();

        }
        if(!directorioInventarios.exists())
        {
            directorioInventarios.mkdir();

        }

    }
    @TargetApi(Build.VERSION_CODES.M)
    private boolean Permisos() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.v(LOG_TAG,"Permission is granted");
            return true;

        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.v(LOG_TAG,"Permission is granted");
                return true;
            }
        }


            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

        return false;
    }
    private void ComprobarBaseDatos(){
        String urlInput ="http://192.168.1.172:2017/app/diferente.sqlite";
        // TODO Auto-generated method stub
        File basedatos = new File(Environment.getExternalStorageDirectory()+"/diferente/diferente.sqlite");

        if(!basedatos.exists())
        {
            progressBar.setVisibility(View.VISIBLE);
            Ion.with(this.getApplicationContext())
                    .load(urlInput)
                    .progressBar(progressBar)
                    .progress(new ProgressCallback() {@Override
                    public void onProgress(long downloaded, long total) {
                        System.out.println("Downloaded " + downloaded + " / " + total);
                    }
                    })
                    .write(new File(Environment.getExternalStorageDirectory()+"/diferente/diferente.sqlite"))
                    .setCallback(new FutureCallback<File>() {
                        @Override
                        public void onCompleted(Exception e, File file) {
                            // download done...
                            // do stuff with the File or error
                            if (e != null) {

                                e.printStackTrace();

                                Toast.makeText(getApplicationContext(), "Error descargando base de datos", Toast.LENGTH_LONG).show();

                            }
                            System.out.println("Downloaded 100 %");
                            progressBar.setVisibility(View.GONE);
                        }
                    });


        }


    }
    public String Devuelvetienda (){
        getApplicationContext();
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        WifiInfo wifiInfo = wm.getConnectionInfo();
        Log.d("wifiInfo", wifiInfo.toString());
        Log.d("SSID",wifiInfo.getSSID());




        String ssid=wifiInfo.getSSID();

        if (ssid.startsWith("\"") && ssid.endsWith("\"")){
            ssid = ssid.substring(1, ssid.length()-1);
        }
        if (ssid.equals("difebaja")||ssid.equals("DIFE-PRIVADA")||ssid.equals("ONOBB16")||ssid.equals("difebaja2")||ssid.equals("difebaja3")||ssid.equals("difebaja4")){
            tienda="Vargas";

            Log.d("ip ", "Vargas");
        }
        if (ssid.equals("dife_oficinas")||ssid.equals("DIFE-SARDI-N")){
            Log.d("ip ", "Sardinero");

            tienda="Sardinero";


        }
        if (ssid.equals("diferente")){
            Log.d("ip ", "General");

            tienda="General";


        }


        return tienda;
    }
    private void RefrescarToken(){

        Ion.with(getApplicationContext())
                .load("POST",Constantes.URL+"token")
                .setHeader("RefreshToken", refreshtoken)
                .setHeader("codcli", codcli)
                .setLogging("ION_VERBOSE_LOGGING", Log.VERBOSE)
                .asString()
                .withResponse()
                .setCallback(new FutureCallback<Response<String>>() {
                    @Override
                    public void onCompleted(Exception e, Response<String> result) {
                        // do stuff with the result or error
                        // print the response code, ie, 200
                        int status;
                        status=result.getHeaders().code();
                        System.out.println(result.getHeaders().code());
                        // print the String that was downloaded
                        System.out.println(result.getResult());

                        if (e != null) {

                            e.printStackTrace();

                            Toast.makeText(getApplicationContext(), "Error loading user data", Toast.LENGTH_LONG).show();
                            return ;
                        }

                        Log.d(LOG_TAG, result.toString() );
                        final Gson gson = new Gson();
                        Respuesta respuesta = gson.fromJson(result.getResult(),Respuesta.class);
                        if(status==200){

                            Toast.makeText(getApplicationContext(), respuesta.getMessage(), Toast.LENGTH_LONG).show();
                            SharedPreferences.Editor editor = mSharedPreferences.edit();
                            editor.putString(Constantes.TOKEN,respuesta.getToken());
                            editor.putString(Constantes.EMAIL,respuesta.getMessage());
                            editor.putString(Constantes.REFRESH_TOKEN,respuesta.getRefresh_token());
                            editor.putString(Constantes.CODCLI,respuesta.getCodcli());
                            editor.apply();


                        }
                        if (status==404 || status == 401){
                            Toast.makeText(getApplicationContext(), respuesta.getMessage(), Toast.LENGTH_LONG).show();

                        }


                    }
                });
    }


}
