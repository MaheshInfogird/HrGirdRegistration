package com.hrgirdadmin;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class MainActivity extends AppCompatActivity {

    public static final String MyPREFERENCES = "MyPrefs" ;
    public static final String MyPREFERENCES_url = "MyPrefs_url" ;
    SharedPreferences pref, shared_pref;
    
    Button btn_attendance, btn_registration;
    CheckInternetConnection internetConnection;
    UserSessionManager session;
    ConnectionDetector cd;
    
    String response_version, myJson1, Url;
    String Packagename;
    String url_http, logo;
    
    int version_code;
    
    ImageView main_logo;
    LinearLayout progress_layout;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_attendance = (Button)findViewById(R.id.btn_attendance);
        btn_registration = (Button)findViewById(R.id.btn_registration);

        session = new UserSessionManager(getApplicationContext());
        internetConnection = new CheckInternetConnection(getApplicationContext());
        cd = new ConnectionDetector(getApplicationContext());
        url_http = cd.geturl();
        //Log.i("url_http", url_http);
        
        
        pref = getSharedPreferences(MyPREFERENCES, MODE_PRIVATE);
        shared_pref = getSharedPreferences(MyPREFERENCES_url, MODE_PRIVATE);
        Url = (shared_pref.getString("url", ""));
        logo = (shared_pref.getString("logo", ""));
        //Log.i("Url", Url);
        //Log.i("logo", logo);
        
        main_logo = (ImageView)findViewById(R.id.main_logo);
        progress_layout = (LinearLayout)findViewById(R.id.progress_layout_main);

        Picasso.with(getApplicationContext()).load(logo).into(main_logo);
        
        try
        {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            version_code = info.versionCode;
            Packagename = info.packageName;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }
        
        getCheckVersion();
        
        main_logo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                session.logout_url();

                Intent intent = new Intent(MainActivity.this, UrlActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btn_attendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_attendance.setBackgroundResource(R.drawable.attendance_button);
                btn_registration.setBackgroundResource(R.drawable.admin_button);
                btn_attendance.setTextColor(getResources().getColor(R.color.RedTextColor));
                btn_registration.setTextColor(getResources().getColor(R.color.WhiteTextColor));

                if (internetConnection.hasConnection(getApplicationContext())) 
                {
                    progress_layout.setVisibility(View.VISIBLE);
                    Intent intent = new Intent(MainActivity.this, AttendanceActivity.class);
                    startActivity(intent);
                    finish();
                } 
                else {
                    Toast.makeText(MainActivity.this, "Please check your internet connection", Toast.LENGTH_LONG).show();
                }
            }
        });

        btn_registration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_attendance.setBackgroundResource(R.drawable.admin_button);
                btn_registration.setBackgroundResource(R.drawable.attendance_button);
                btn_attendance.setTextColor(getResources().getColor(R.color.WhiteTextColor));
                btn_registration.setTextColor(getResources().getColor(R.color.RedTextColor));

                if (internetConnection.hasConnection(getApplicationContext())) 
                {
                    if (session.isUserLoggedIn())
                    {
                        progress_layout.setVisibility(View.VISIBLE);
                        Intent intent = new Intent(MainActivity.this, ThumbRegistration.class);
                        startActivity(intent);
                        finish();
                    } 
                    else {
                        progress_layout.setVisibility(View.VISIBLE);
                        Intent intent = new Intent(MainActivity.this, LogInActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
                else {
                    Toast.makeText(MainActivity.this, "Please check your internet connection", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    
    
    public void getCheckVersion()
    {
        class GetCheckVersion extends AsyncTask<String, Void, String>
        {
            @Override
            protected void onPreExecute() {
            }

            @Override
            protected String doInBackground(String... params)
            {
                try
                {
                    String leave_url = ""+url_http+""+Url+"/owner/hrmapi/getversion/?";

                    String query3 = String.format("apptype=%s", URLEncoder.encode("1", "UTF-8"));
                    URL url = new URL(leave_url + query3);
                    Log.i("url", ""+ url);

                    HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestMethod("GET");
                    connection.setUseCaches(false);
                    connection.setAllowUserInteraction(false);
                    connection.setDoInput(true);
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connection.setDoOutput(true);
                    int responseCode = connection.getResponseCode();

                    if (responseCode == HttpURLConnection.HTTP_OK)
                    {
                        String line;
                        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        while ((line = br.readLine()) != null)
                        {
                            response_version = "";
                            response_version += line;
                        }
                    }
                    else
                    {
                        response_version = "";
                    }
                }
                catch (Exception e){
                    Log.e("Exception", e.toString());
                }

                return response_version;
            }

            @Override
            protected void onPostExecute(String result)
            {
                if (result != null)
                {
                    myJson1 = result;
                    Log.i("myJson", myJson1);

                    if (myJson1.equals("[]"))
                    {
                        Toast.makeText(MainActivity.this, "Sorry... Bad internet connection", Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        try
                        {
                            JSONArray jsonArray = new JSONArray(myJson1);
                            //Log.i("jsonArray", "" + jsonArray);

                            JSONObject object = jsonArray.getJSONObject(0);
                            
                            int get_version = object.getInt("Version");
                            
                            if (version_code != get_version)
                            {
                                AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                                alertDialog.setTitle("New Update");
                                alertDialog.setMessage("Please update your app");
                                alertDialog.setCancelable(false);
                                alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(Intent.ACTION_VIEW);
                                        intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.hrgirdadmin&hl=en"));
                                        startActivity(intent);
                                        finish();
                                    }
                                });

                                alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent startMain = new Intent(Intent.ACTION_MAIN);
                                        startMain.addCategory(Intent.CATEGORY_HOME);
                                        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(startMain);
                                        finish();
                                    }
                                });

                                alertDialog.show();
                            }
                        }
                        catch (JSONException e) {
                            Log.e("JsonException", e.toString());
                        }
                    }
                }
                else {
                    Toast.makeText(MainActivity.this, "Sorry...Bad internet connection", Toast.LENGTH_LONG).show();
                }
            }
        }

        GetCheckVersion getCheckVersion = new GetCheckVersion();
        getCheckVersion.execute();
    }

    
    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    public void onBackPressed()
    {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }
}
