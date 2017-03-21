package com.hrgirdadmin;

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HttpsURLConnection;

import SecuGen.FDxSDKPro.JSGFPLib;
import SecuGen.FDxSDKPro.SGAutoOnEventNotifier;
import SecuGen.FDxSDKPro.SGFDxDeviceName;
import SecuGen.FDxSDKPro.SGFDxErrorCode;
import SecuGen.FDxSDKPro.SGFDxSecurityLevel;
import SecuGen.FDxSDKPro.SGFDxTemplateFormat;
import SecuGen.FDxSDKPro.SGFingerInfo;
import SecuGen.FDxSDKPro.SGFingerPresentEvent;

/**
 * Created by admin on 25-11-2016.
 */
public class AttendanceActivity extends AppCompatActivity implements SGFingerPresentEvent {

    public static final String MyPREFERENCES_url = "MyPrefs_url" ;
    SharedPreferences  shared_pref;
    SharedPreferences.Editor editor1;

    private byte[] mRegisterImage;
    private byte[] mRegisterTemplate;
    private byte[] mVerifyImage;
    private static int mImageWidth;
    private static int mImageHeight;
    private PendingIntent mPermissionIntent;

    public static final String MyPREFERENCES = "MyPrefs" ;
    int PRIVATE_MODE = 0;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    String password;

    private IntentFilter filter;
    long dwTimeStart = 0, dwTimeEnd = 0, dwTimeElapsed = 0;
    private JSGFPLib sgfplib;

    private Bitmap grayBitmap;
    private int[] grayBuffer;
    private int[] mMaxTemplateSize;
    private byte[] mVerifyTemplate;
    int[] score = new int[1];

    private SGAutoOnEventNotifier auto_on;
    String myJSON = null;
    String RegisteredBase64, MobileNo;
    String EmpId;
    String Sign_InOut_id = "1";
    String logout_id = "0";

    LinearLayout atndnc_logout;
    EditText ed_atndncLogout;
    Button btn_atndncLogout;
    Toolbar toolbar;
    EditText ed_MobNo;
    TextView txt_matchMsg, txt_Time;
    ImageView img_Match;
    Button btn_signIn, btn_signOut;
    RadioButton rd_signIn, rd_signOut;
    RelativeLayout content_frame;
    CoordinatorLayout snackbarCoordinatorLayout;

    Calendar c;
    TextToSpeech textToSpeech;
    String ResponseCode, Message;
    boolean[] matched = new boolean[1];

    ProgressDialog progressDialog;

    static boolean logout_status = true;
    Timer timer;
    MyTimerTask myTimerTask;
    Handler someHandler;
    ConnectionDetector cd;
    String Url;
    String url_http;
    UserSessionManager session;
    CheckInternetConnection internetConnection;
    public static NetworkChange receiver;
    Snackbar snackbar;
    
    String android_id;
    boolean device_info = false;
    
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

    private static final String ACTION_USB_DETACHED  = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    private final BroadcastReceiver mUsbReceiver1 = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent) 
        {
            String action = intent.getAction();
            if (ACTION_USB_DETACHED.equals(action)) 
            {
                Log.i("DEVICE_DETACHED","ACTION_USB_DEVICE_DETACHED");
                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) 
                {
                    Toast.makeText(AttendanceActivity.this, "Device disconnected", Toast.LENGTH_LONG).show();
                    long error1 = sgfplib.Close();
                    finish();               
                }
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_attendance);

        toolbar = (Toolbar)findViewById(R.id.toolbar_inner);
        TextView Header = (TextView)findViewById(R.id.header_text);
        ImageView img_logout = (ImageView)findViewById(R.id.img_logout);
        setSupportActionBar(toolbar);

        img_logout.setVisibility(View.GONE);
        
        session = new UserSessionManager(getApplicationContext());
        internetConnection = new CheckInternetConnection(getApplicationContext());

        cd = new ConnectionDetector(getApplicationContext());
        url_http = cd.geturl();
        //Log.i("url_http", url_http);

        shared_pref = getSharedPreferences(MyPREFERENCES_url, MODE_PRIVATE);
        Url = (shared_pref.getString("url", ""));
        //Log.i("Url", Url);

        pref = getApplicationContext().getSharedPreferences(MyPREFERENCES, PRIVATE_MODE);
        password = pref.getString("password", "password");

        atndnc_logout = (LinearLayout)findViewById(R.id.atndnc_logout);
        ed_atndncLogout = (EditText)findViewById(R.id.ed_atndnc_logout);
        btn_atndncLogout = (Button)findViewById(R.id.btn_atndnc_logout);
        content_frame = (RelativeLayout)findViewById(R.id.content_frame);
        txt_Time = (TextView)findViewById(R.id.txtTime);
        snackbarCoordinatorLayout = (CoordinatorLayout)findViewById(R.id.snackbarCoordinatorLayout);

        if (getSupportActionBar() != null)
        {
            getSupportActionBar().setTitle("");
            Header.setText("Attendance");
            img_logout.setVisibility(View.GONE);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(getResources().getDrawable(R.drawable.abc_ic_ab_back_mtrl_am_alpha));
        }
        
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        
        receiver = new NetworkChange()
        {
            @Override
            protected void onNetworkChange()
            {
                if (receiver.isConnected)
                {
                    if (snackbar != null)
                    {
                        snackbar.dismiss();
                    }
                }
                else
                {
                    internetConnection.showNetDisabledAlertToUser(AttendanceActivity.this);

                    snackbar = Snackbar.make(snackbarCoordinatorLayout, "Please check your internet connection", Snackbar.LENGTH_INDEFINITE);
                    View sbView = snackbar.getView();
                    sbView.setBackgroundColor(getResources().getColor(R.color.RedTextColor));
                    snackbar.show();
                }
            }
        };

        img_logout.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(AttendanceActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                alertDialog.setTitle("Logout user / Switch to Admin");
                alertDialog.setCancelable(true);
                alertDialog.setPositiveButton("Switch To Admin", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        atndnc_logout.setVisibility(View.VISIBLE);
                        ed_atndncLogout.requestFocus();
                        logout_id = "0";
                        btn_atndncLogout.setText("Switch");
                    }
                });
                
                alertDialog.setNegativeButton("Logout", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        logout_id = "1";
                        auto_on.stop();
                        //logout_status = false;
                        editor = pref.edit();
                        editor.clear();
                        editor.commit();
                        session.logoutUser();
                        
                        Intent intent = new Intent(AttendanceActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });

                alertDialog.show();
            }
        });

        mMaxTemplateSize = new int[1];

        mPermissionIntent = PendingIntent.getBroadcast(AttendanceActivity.this, 0, new Intent(ACTION_USB_PERMISSION), 0);

        sgfplib = new JSGFPLib((UsbManager)getSystemService(Context.USB_SERVICE));

        auto_on = new SGAutoOnEventNotifier(sgfplib, AttendanceActivity.this);
        auto_on.start();

        ed_MobNo = (EditText)findViewById(R.id.ed_match_mobNo);
        txt_matchMsg = (TextView)findViewById(R.id.txtMatch);
        img_Match = (ImageView)findViewById(R.id.match_fingerprint);
        btn_signIn = (Button)findViewById(R.id.btn_atndnc_signIn);
        btn_signOut = (Button)findViewById(R.id.btn_atndnc_signOut);
        rd_signIn = (RadioButton)findViewById(R.id.radio_signIn);
        rd_signOut = (RadioButton)findViewById(R.id.radio_signOut);

        grayBuffer = new int[JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES*JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES];
        for (int i=0; i<grayBuffer.length; ++i)
            grayBuffer[i] = android.graphics.Color.GRAY;
        grayBitmap = Bitmap.createBitmap(JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES, JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES, Bitmap.Config.ARGB_8888);
        grayBitmap.setPixels(grayBuffer, 0, JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES, 0, 0, JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES, JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES);

        int[] sintbuffer = new int[(JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES/2)*(JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES/2)];
        for (int i=0; i<sintbuffer.length; ++i)
            sintbuffer[i] = android.graphics.Color.GRAY;
        Bitmap sb = Bitmap.createBitmap(JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES/2, JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES/2, Bitmap.Config.ARGB_8888);
        sb.setPixels(sintbuffer, 0, JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES / 2, 0, 0, JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES / 2, JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES / 2);
        img_Match.setImageBitmap(grayBitmap);

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener()
        {
            @Override
            public void onInit(int status)
            {
                if(status != TextToSpeech.ERROR)
                {
                    textToSpeech.setLanguage(Locale.getDefault());
                }
            }
        });

        rd_signIn.setChecked(true);

        btn_atndncLogout.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String PassWord = ed_atndncLogout.getText().toString();
                if (PassWord.equals(password))
                {
                    if (logout_id.equals("1"))
                    {
                        auto_on.stop();
                        //logout_status = false;
                        editor = pref.edit();
                        editor.clear();
                        editor.commit();
                        session.logoutUser();

                        Intent intent = new Intent(AttendanceActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    else
                    {
                        auto_on.stop();
                        //logout_status = false;
                        Intent intent = new Intent(AttendanceActivity.this, ThumbRegistration.class);
                        startActivity(intent);
                        finish();
                    }
                }
                else
                {
                    ed_atndncLogout.setError("Please enter correct password");
                }
            }
        });

        rd_signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sign_InOut_id = "1";
                rd_signIn.setButtonDrawable(getResources().getDrawable(R.drawable.checkedradiobtn));
                rd_signOut.setButtonDrawable(getResources().getDrawable(R.drawable.uncheckedradiobtn));
            }
        });

        rd_signOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sign_InOut_id = "2";
                rd_signIn.setButtonDrawable(getResources().getDrawable(R.drawable.uncheckedradiobtn));
                rd_signOut.setButtonDrawable(getResources().getDrawable(R.drawable.checkedradiobtn));
            }
        });
        
        deviceData();
        
        someHandler = new Handler(getMainLooper());
        someHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy   hh:mm a");
                sdf.setLenient(false);
                Date today = new Date();
                String time = sdf.format(today);
                txt_Time.setText(time);
                someHandler.postDelayed(this, 10000);
            }
        }, 10);
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

    public void getThumbExpression()
    {
        class GetDataJSON extends AsyncTask<String, Void, String>
        {
            private URL url;
            private String response = "";

            @Override
            protected String doInBackground(String... params)
            {
                try
                {
                    String Transurl = ""+url_http+""+Url+"/owner/hrmapi/getthumexpression/?";

                    String query = String.format("mobile=%s&android_devide_id=%s",
                            URLEncoder.encode(MobileNo, "UTF-8"),
                            URLEncoder.encode(android_id, "UTF-8"));
                    
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
                    else
                    {
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
                Log.i("result", "" + result);
                if (result.equals("[]"))
                {
                    Toast.makeText(AttendanceActivity.this, "Check Internet connection", Toast.LENGTH_LONG).show();
                }
                else
                {
                    try
                    {
                        JSONArray json = new JSONArray(result);
                        //Log.i("json", "" + json);

                        JSONObject jsonObj = json.getJSONObject(0);

                        String responsecode = jsonObj.getString("responsecode");

                        if (responsecode.equals("1"))
                        {
                            mVerifyImage = new byte[mImageWidth * mImageHeight];
                            ByteBuffer byteBuf = ByteBuffer.allocate(mImageWidth * mImageHeight);
                            //Log.i("result", Arrays.toString(mVerifyImage));
                            long result1 = sgfplib.GetImage(mVerifyImage);
                            Log.i("result", "" + result1);
                            img_Match.setImageBitmap(AttendanceActivity.toGrayscale(mVerifyImage));

                            //result0 = sgfplib.SetTemplateFormat(SecuGen.FDxSDKPro.SGFDxTemplateFormat.TEMPLATE_FORMAT_SG400);
                            result1 = sgfplib.SetTemplateFormat(SecuGen.FDxSDKPro.SGFDxTemplateFormat.TEMPLATE_FORMAT_ISO19794);
                            Log.i("result1", "" + result1);

                            SGFingerInfo fpInfo = new SGFingerInfo();
                            for (int i = 0; i < mVerifyTemplate.length; ++i)
                                mVerifyTemplate[i] = 0;

                            dwTimeStart = System.currentTimeMillis();
                            result1 = sgfplib.CreateTemplate(fpInfo, mVerifyImage, mVerifyTemplate);
                            Log.i("result2", "" + result1);
                            matched = new boolean[1];

                            if (result1 == 0)
                            {
                                try
                                {
                                    JSONArray array = jsonObj.getJSONArray("thumexpression");
                                    Log.i("array", ""+array);

                                    for (int i = 0; i < array.length(); i++)
                                    {
                                        JSONObject object = array.getJSONObject(i);
                                        //Log.i("object", ""+object);

                                        if (!matched[0])
                                        {
                                            RegisteredBase64 = object.getString("thumexpression");
                                            Log.i("RegisteredBase64", RegisteredBase64);

                                            EmpId = object.getString("empId");
                                            Log.i("EmpId", EmpId);

                                            mRegisterTemplate = Base64.decode(RegisteredBase64, Base64.DEFAULT);
                                            Log.i("mRegisterTemplate", "" + mRegisterTemplate);

                                            String str = RegisteredBase64.substring(0,35);
                                            Log.i("str", str);

                                            String base64 = " Rk1SACAyMAAAIAAKADUAAAEsAZAAxQDFAQ";
                                            Log.i("base64", base64);

                                            if (str.equalsIgnoreCase(base64))
                                            {
                                                Log.i("inside_if", "inside_if");
                                                txt_matchMsg.setText("Thumb not registered properly!!\n");
                                                textToSpeech.speak("Thumb Not Registered Properly!!", TextToSpeech.QUEUE_FLUSH, null);
                                                txt_matchMsg.setTextColor(Color.RED);
                                            }
                                            else
                                            {
                                                Log.i("inside_else", "inside_else");
                                                //result = sgfplib.MatchTemplate(mRegisterTemplate, mVerifyTemplate, SGFDxSecurityLevel.SL_NORMAL, matched);
                                                sgfplib.MatchIsoTemplate(mRegisterTemplate, 0, mVerifyTemplate, 0, SGFDxSecurityLevel.SL_NORMAL, matched);
                                                //result = sgfplib.MatchTemplate(mRegisterTemplate, mVerifyTemplate, SGFDxSecurityLevel.SL_NORMAL, matched);
                                                
                                                if (matched[0])
                                                {
                                                    makeAttendance();
                                                }
                                                else
                                                {
                                                    Log.i("NOT MATCHED!!", "NOT MATCHED!!");
                                                }
                                            }
                                        }
                                    }
                                }
                                catch (JSONException e)
                                {
                                    e.printStackTrace();
                                }

                                if (!matched[0])
                                {
                                    txt_matchMsg.setText("Sorry thumb not matched!!\n");
                                    textToSpeech.speak("Sorry Thumb Not Matched", TextToSpeech.QUEUE_FLUSH, null);
                                    txt_matchMsg.setTextColor(Color.RED);
                                    Log.i("NOT MATCHED!!", "NOT MATCHED!!");
                                }
                            }
                            else
                            {
                                txt_matchMsg.setText("Please press thumb properly!!\n");
                                textToSpeech.speak("Please press thumb properly", TextToSpeech.QUEUE_FLUSH, null);
                                txt_matchMsg.setTextColor(Color.RED);
                            }

                            dwTimeEnd = System.currentTimeMillis();
                            dwTimeElapsed = dwTimeEnd - dwTimeStart;
                            
                        }
                        else
                        {
                            String msg = jsonObj.getString("msg");
                            String message = msg.substring(2, msg.length()-2);
                            ed_MobNo.setText("");
                            txt_matchMsg.setText(message);
                            txt_matchMsg.setTextColor(Color.RED);
                        }
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        GetDataJSON getDataJSON = new GetDataJSON();
        getDataJSON.execute();
    }


    public void makeAttendance()
    {
        class GetDataJSON extends AsyncTask<String, Void, String>
        {
            private URL url;
            private String response = "";

            @Override
            protected void onPreExecute()
            {
                progressDialog = ProgressDialog.show(AttendanceActivity.this, "Please wait", "Matching thumb...", true);
                progressDialog.show();
            }

            @Override
            protected String doInBackground(String... params)
            {
                try
                {
                    String Transurl = ""+url_http+""+Url+"/owner/hrmapi/makeattendance/?";
                    
                    String query = String.format("empId=%s&signId=%s", URLEncoder.encode(EmpId, "UTF-8"), URLEncoder.encode(Sign_InOut_id, "UTF-8"));
                    url = new URL(Transurl + query);
                    Log.i("url", "" + url);

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setReadTimeout(10000);
                    conn.setConnectTimeout(5000);
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
                    progressDialog.dismiss();
                    Toast.makeText(AttendanceActivity.this, "Check Internet connection", Toast.LENGTH_LONG).show();
                }
                else
                {
                    try
                    {
                        JSONArray json = new JSONArray(result);
                        //Log.i("json", "" + json);

                        JSONObject jsonObj = json.getJSONObject(0);

                        ResponseCode = jsonObj.getString("responsecode");
                        Message = jsonObj.getString("msg");

                        SimpleDateFormat sdf1 = new SimpleDateFormat("HH.mm");
                        String strDate = sdf1.format(c.getTime());
                        double time = Double.parseDouble(strDate);

                        if (matched[0])
                        {
                            c = Calendar.getInstance();
                            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");
                            sdf.setLenient(false);
                            Date today = new Date();
                            String time1 = sdf.format(today);

                            if (Sign_InOut_id.equals("1"))
                            {
                                if (ResponseCode.equals("1"))
                                {
                                    String firstName = jsonObj.getString("firstName");
                                    String lastName = jsonObj.getString("firstName");
                                    String empName = firstName + lastName;

                                    progressDialog.dismiss();

                                    txt_matchMsg.setText(Message);
                                    txt_matchMsg.setTextColor(getResources().getColor(R.color.GreenColor));

                                    textToSpeech.speak("Welcome "+firstName, TextToSpeech.QUEUE_FLUSH, null);
                                    Log.i("MATCHED", "MATCHED");
                                    ed_MobNo.setText("");
                                }
                                else
                                {
                                    progressDialog.dismiss();

                                    txt_matchMsg.setText(Message);
                                    textToSpeech.speak(Message, TextToSpeech.QUEUE_FLUSH, null);
                                    txt_matchMsg.setTextColor(Color.RED);
                                }
                            }
                            else if (Sign_InOut_id.equals("2"))
                            {
                                if (ResponseCode.equals("1"))
                                {
                                    String firstName = jsonObj.getString("firstName");
                                    String lastName = jsonObj.getString("firstName");
                                    String empName = firstName + lastName;

                                    progressDialog.dismiss();

                                    txt_matchMsg.setText(Message);
                                    txt_matchMsg.setTextColor(getResources().getColor(R.color.GreenColor));

                                    textToSpeech.speak("Bye Bye "+firstName, TextToSpeech.QUEUE_FLUSH, null);
                                    Log.i("MATCHED", "MATCHED");
                                    ed_MobNo.setText("");
                                }
                                else
                                {
                                    progressDialog.dismiss();

                                    txt_matchMsg.setText(Message);
                                    textToSpeech.speak(Message, TextToSpeech.QUEUE_FLUSH, null);
                                    txt_matchMsg.setTextColor(Color.RED);
                                }
                            }
                        }
                        else
                        {
                            progressDialog.dismiss();
                            Log.i("NOT MATCHED!!", "NOT MATCHED!!");
                        }

                        Log.i("Successful", "Successful");
                    }
                    catch (JSONException e) {
                        progressDialog.dismiss();
                        Toast.makeText(AttendanceActivity.this, "Sorry...Json exception", Toast.LENGTH_LONG).show();
                        Log.i("Exception", e.toString());
                    }
                }
            }
        }
        GetDataJSON getDataJSON = new GetDataJSON();
        getDataJSON.execute();
    }

    public static Bitmap toGrayscale(byte[] mImageBuffer)
    {
        byte[] Bits = new byte[mImageBuffer.length * 4];
        for (int i = 0; i < mImageBuffer.length; i++)
        {
            Bits[i * 4] = Bits[i * 4 + 1] = Bits[i * 4 + 2] = mImageBuffer[i]; // Invert the source bits
            Bits[i * 4 + 3] = -1;// 0xff, that's the alpha.
        }

        Bitmap bmpGrayscale = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ARGB_8888);
        //Bitmap bm contains the fingerprint img
        bmpGrayscale.copyPixelsFromBuffer(ByteBuffer.wrap(Bits));
        return bmpGrayscale;
    }

    public void CaptureFingerPrint()
    {
        //auto_on.stop();
        if (!ed_MobNo.getText().toString().equals("")) 
        {
            MobileNo = ed_MobNo.getText().toString();

            if (MobileNo.length() >= 10) 
            {
                c = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss a");
                sdf.setLenient(false);
                Date today = new Date();
                String s = sdf.format(today);
                
                getThumbExpression();
                
            }
            else {
                ed_MobNo.setError("Mobile no. must be greater than 9 digits");
            }
        }
        else {
            ed_MobNo.setError("Please enter mobile no");
        }
        
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() 
        {
            @Override
            public void run() 
            {
                img_Match.setImageBitmap(grayBitmap);
                txt_matchMsg.setText("");
                auto_on.start();
            }
        }, 5000);
    }

    public Handler fingerDetectedHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            //Handle the message
                CaptureFingerPrint();
        }
    };

    @Override
    public void SGFingerPresentCallback()
    {
        auto_on.stop();
        fingerDetectedHandler.sendMessage(new Message());
    }


    @Override
    public void onResume()
    {
        super.onResume();

        /*if (timer != null)
        {
            timer.cancel();
            timer = null;
        }*/

        filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);

        IntentFilter filter = new IntentFilter(ACTION_USB_DETACHED);
        registerReceiver(mUsbReceiver1, filter);

        IntentFilter filter1 = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(receiver, filter1);

        long error = sgfplib.Init(SGFDxDeviceName.SG_DEV_AUTO);

        if (error != SGFDxErrorCode.SGFDX_ERROR_NONE)
        {
            AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
            dlgAlert.setCancelable(false);
            if (error == SGFDxErrorCode.SGFDX_ERROR_DEVICE_NOT_FOUND)
                dlgAlert.setMessage("The attached fingerprint device is not supported on Android");
            else
                dlgAlert.setMessage("Fingerprint device initialization failed!");
            dlgAlert.setTitle("No Device");
            dlgAlert.setPositiveButton("OK", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog,int whichButton){
                    //logout_status = false;
                    finish();
                    return;
                }
            });
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
                                //logout_status = false;
                                finish();
                                return;
                            }
                        });
                dlgAlert.setCancelable(false);
                dlgAlert.create().show();
            }
            else
            {
                device_info = true;
                mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                filter = new IntentFilter(ACTION_USB_PERMISSION);
                registerReceiver(mUsbReceiver, filter);
                sgfplib.GetUsbManager().requestPermission(usbDevice, mPermissionIntent);

                error = sgfplib.OpenDevice(0);
                SecuGen.FDxSDKPro.SGDeviceInfoParam deviceInfo = new SecuGen.FDxSDKPro.SGDeviceInfoParam();
                error = sgfplib.GetDeviceInfo(deviceInfo);
                mImageWidth = deviceInfo.imageWidth;
                mImageHeight= deviceInfo.imageHeight;
                sgfplib.SetTemplateFormat(SGFDxTemplateFormat.TEMPLATE_FORMAT_ISO19794);
                sgfplib.GetMaxTemplateSize(mMaxTemplateSize);
                //mRegisterTemplate = new byte[mMaxTemplateSize[0]];
                mVerifyTemplate = new byte[mMaxTemplateSize[0]];
                sgfplib.WriteData((byte)5, (byte)1);
            }
        }
    }


    @Override
    public void onPause() {
        Log.d("onPause", "onPause()");

       /* if(logout_status)
        {
            if (timer == null)
            {
                myTimerTask = new MyTimerTask();
                timer = new Timer();
                timer.schedule(myTimerTask, 100, 100);
            }

            ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
            activityManager.moveTaskToFront(getTaskId(), 0);

            if (this.isFinishing())
            {
                Intent it = new Intent(this,AttendanceActivity.class);
                startActivity(it);
                finish();
                //Insert your finishing code here
            }
        }*/

        unregisterReceiver(mUsbReceiver);
        unregisterReceiver(mUsbReceiver1);
        unregisterReceiver(receiver);
        
        sgfplib.CloseDevice();
        mVerifyImage = null;
        mRegisterTemplate = null;
        mVerifyTemplate = null;
        img_Match.setImageBitmap(grayBitmap);
        super.onPause();
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(AttendanceActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void bringApplicationToFront()
    {
        KeyguardManager myKeyManager = (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
        if( myKeyManager.inKeyguardRestrictedInputMode())
            return;

        Log.i("TAG", "====Bringging Application to Front====");

        Intent notificationIntent = new Intent(this, AttendanceActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        try
        {
            pendingIntent.send();
        }
        catch (PendingIntent.CanceledException e)
        {
            e.printStackTrace();
        }
    }

    class MyTimerTask extends TimerTask
    {
        @Override
        public void run()
        {
            bringApplicationToFront();
        }
    }
}
