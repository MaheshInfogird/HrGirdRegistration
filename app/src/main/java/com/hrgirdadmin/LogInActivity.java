package com.hrgirdadmin;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
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

import javax.net.ssl.HttpsURLConnection;

import SecuGen.FDxSDKPro.JSGFPLib;
import SecuGen.FDxSDKPro.SGFDxDeviceName;
import SecuGen.FDxSDKPro.SGFDxErrorCode;

/**
 * Created by admin on 25-11-2016.
 */
public class LogInActivity extends AppCompatActivity
{
    public static final String MyPREFERENCES_url = "MyPrefs_url" ;
    SharedPreferences shared_pref;
    
    public static final String MyPREFERENCES = "MyPrefs" ;
    int PRIVATE_MODE = 0;
    SharedPreferences pref;
    SharedPreferences.Editor editor;

    ConnectionDetector cd;
    UserSessionManager session;
    CheckInternetConnection internetConnection;

    String Url, logo;
    String url_http;
    String myJSON = null;
    String UserName, Password;
    
    ProgressDialog progressDialog;
    EditText ed_userName, ed_password;
    TextView txt_forgotPass;
    Button btn_signIn;
    LinearLayout signIn_layout, progress_layout;
    ImageView logo_login;
    LinearLayout poweredby_layout;
    
    String android_id;

    private JSGFPLib sgfplib;
    private IntentFilter filter;
    private PendingIntent mPermissionIntent;

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action))
            {
                synchronized (this)
                {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
                    {
                        if(device != null) {
                        }
                        else
                            Log.e("null", "mUsbReceiver.onReceive() Device is null");
                    }
                    else
                        Log.e("denied", "mUsbReceiver.onReceive() permission denied for device " + device);
                }
            }
        }
    };
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        internetConnection = new CheckInternetConnection(getApplicationContext());

        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        sgfplib = new JSGFPLib((UsbManager)getSystemService(Context.USB_SERVICE));
        
        ed_userName = (EditText)findViewById(R.id.ed_userName);
        ed_password = (EditText)findViewById(R.id.ed_password);
        btn_signIn = (Button) findViewById(R.id.btn_signIn);
        signIn_layout = (LinearLayout)findViewById(R.id.signIn_layout);
        progress_layout = (LinearLayout)findViewById(R.id.progress_layout);
        txt_forgotPass = (TextView)findViewById(R.id.forgot_pass);
        poweredby_layout = (LinearLayout)findViewById(R.id.layout_poweredby_login);
        
        cd = new ConnectionDetector(getApplicationContext());
        url_http = cd.geturl();
        //Log.i("url_http", url_http);
        session = new UserSessionManager(getApplicationContext());

        shared_pref = getSharedPreferences(MyPREFERENCES_url, MODE_PRIVATE);
        Url = (shared_pref.getString("url", ""));
        logo = (shared_pref.getString("logo", ""));
        //Log.i("Url", Url);
        //Log.i("logo", logo);
        

        logo_login = (ImageView)findViewById(R.id.logo_login);
        Picasso.with(getApplicationContext()).load(logo).into(logo_login);

        txt_forgotPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LogInActivity.this, ForgotPassword.class);
                startActivity(intent);
                finish();
            }
        });

        ed_userName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                poweredby_layout.setVisibility(View.GONE);
            }
        });
        
        btn_signIn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                UserName = ed_userName.getText().toString();
                Password = ed_password.getText().toString();
                
                if (internetConnection.hasConnection(getApplicationContext())) 
                {
                    if (UserName.equals("") && Password.equals("")) 
                    {
                        ed_userName.setError("Please enter email/mobile");
                        ed_password.setError("Please enter password");
                        txtChange();
                    } 
                    else if (UserName.equals("")) {
                        ed_userName.setError("Please enter email/mobile");
                        txtChange();
                    }
                    else if (Password.equals("")) {
                        ed_password.setError("Please enter password");
                        txtChange();
                    } 
                    else {
                        signIn();
                    }
                }
                else {
                    Toast.makeText(LogInActivity.this, "Please check your internet connection", Toast.LENGTH_LONG).show();
                }
            }
        });
        
        deviceData();
    }

    public void deviceData()
    {
        android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        int version = Build.VERSION.SDK_INT;
        String versionRelease = Build.VERSION.RELEASE;

        String Androidversion = manufacturer + model + version + versionRelease;
    }

    public void txtChange()
    {
        ed_userName.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ed_userName.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        ed_password.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ed_password.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    public void signIn()
    {
        class GetDataJSON extends AsyncTask<String, Void, String>
        {
            private URL url;
            private String response = "";

            @Override
            protected void onPreExecute()
            {
                progressDialog = new ProgressDialog(LogInActivity.this, ProgressDialog.THEME_HOLO_LIGHT);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setTitle("Please wait");
                progressDialog.setMessage("Signing In...");
                progressDialog.show();
            }

            @Override
            protected String doInBackground(String... params)
            {
                try
                {
                    String Transurl = ""+url_http+""+Url+"/owner/hrmapi/signIn/?";
                    
                    String query = String.format("email=%s&password=%s&android_devide_id=%s&signinby=%s", 
                            URLEncoder.encode(UserName, "UTF-8"),
                            URLEncoder.encode(Password, "UTF-8"),
                            URLEncoder.encode(android_id, "UTF-8"),
                            URLEncoder.encode("1", "UTF-8"));
                    
                    url = new URL(Transurl + query);
                    Log.i("url", "" + url);

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setReadTimeout(10000);
                    conn.setConnectTimeout(10000);
                    conn.setRequestMethod("GET");
                    conn.setDoInput(true);
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    conn.setDoOutput(true);
                    int responseCode = conn.getResponseCode();

                    if (responseCode == HttpsURLConnection.HTTP_OK)
                    {
                        String line;
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        while ((line = br.readLine()) != null)
                        {
                            response += line;
                        }
                    }
                    else {
                        response = "";
                    }

                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                return response;
            }

            @Override
            protected void onPostExecute(String result)
            {
                myJSON = result;
                Log.i("response", result);
                if (response.equals("[]"))
                {
                    Toast.makeText(LogInActivity.this, "Invalid Login", Toast.LENGTH_LONG).show();
                }
                else
                {
                    try
                    {
                        JSONArray json = new JSONArray(result);
                        //Log.i("json", "" + json);

                        JSONObject object = json.getJSONObject(0);

                        String responsecode = object.getString("responseCode");

                        if (responsecode.equals("1"))
                        {
                            progressDialog.dismiss();
                            
                            session.createUserLoginSession(UserName, Password);

                            String uId = object.getString("uId");
                            String firstName = object.getString("firstName");
                            String lastName = object.getString("lastName");
                            String Name = firstName + lastName;
                            String email = object.getString("email");
                            String mobile = object.getString("mobile");
                            String subuserid = object.getString("subuserid");
                            
                            pref = getApplicationContext().getSharedPreferences(MyPREFERENCES, PRIVATE_MODE);
                            editor = pref.edit();
                            editor.putString("password", Password);
                            editor.putString("uId", uId);
                            editor.commit();
                            
                            progress_layout.setVisibility(View.VISIBLE);
                                
                            Intent intent = new Intent(LogInActivity.this, ThumbRegistration.class);
                            startActivity(intent);
                            finish();
                        }
                        else
                        {
                            progressDialog.dismiss();

                            String msg = object.getString("responseMessage");
                            String message = msg.substring(2, msg.length()-2);

                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(LogInActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                            alertDialog.setTitle(message);
                            alertDialog.setCancelable(true);
                            alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    dialog.dismiss();
                                }
                            });

                            alertDialog.show();
                        }
                    }
                    catch (JSONException e){
                        progressDialog.dismiss();
                        Log.i("Exception", e.toString());
                    }
                }
            }
        }
        GetDataJSON getDataJSON = new GetDataJSON();
        getDataJSON.execute();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);
        
        long error = sgfplib.Init( SGFDxDeviceName.SG_DEV_AUTO);

        if (error != SGFDxErrorCode.SGFDX_ERROR_NONE)
        {
            AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
            if (error == SGFDxErrorCode.SGFDX_ERROR_DEVICE_NOT_FOUND)
                dlgAlert.setMessage("The attached fingerprint device is not supported on Android");
            else
                dlgAlert.setMessage("Fingerprint device initialization failed!");
            dlgAlert.setTitle("SecuGen Fingerprint SDK");
            dlgAlert.setPositiveButton("OK", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog,int whichButton){
                    finish();
                    return;
                }
            });
            dlgAlert.setCancelable(false);
            dlgAlert.create().show();
        }
        else
        {
            UsbDevice usbDevice = sgfplib.GetUsbDevice();
            if (usbDevice == null)
            {
                AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
                dlgAlert.setMessage("Fingerprint device not found!");
                dlgAlert.setTitle("No Device");
                dlgAlert.setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int whichButton){
                                finish();
                                return;
                            }
                        });
                dlgAlert.setCancelable(false);
                dlgAlert.create().show();
            }
            else
            {
                sgfplib.GetUsbManager().requestPermission(usbDevice, mPermissionIntent);
                error = sgfplib.OpenDevice(0);
                SecuGen.FDxSDKPro.SGDeviceInfoParam deviceInfo = new SecuGen.FDxSDKPro.SGDeviceInfoParam();
                error = sgfplib.GetDeviceInfo(deviceInfo);
            }
        }
    }


    @Override
    public void onPause()
    {
        unregisterReceiver(mUsbReceiver);
        
        sgfplib.CloseDevice();
        super.onPause();
    }

    
    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(LogInActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
