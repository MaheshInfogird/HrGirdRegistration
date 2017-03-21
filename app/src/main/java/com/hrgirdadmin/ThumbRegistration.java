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
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import SecuGen.FDxSDKPro.JSGFPLib;
import SecuGen.FDxSDKPro.SGAutoOnEventNotifier;
import SecuGen.FDxSDKPro.SGFDxDeviceName;
import SecuGen.FDxSDKPro.SGFDxErrorCode;
import SecuGen.FDxSDKPro.SGFDxTemplateFormat;
import SecuGen.FDxSDKPro.SGFingerInfo;
import SecuGen.FDxSDKPro.SGFingerPresentEvent;

/**
 * Created by adminsitrator on 27/02/2017.
 */
public class ThumbRegistration extends AppCompatActivity implements java.lang.Runnable, SGFingerPresentEvent {

    public static final String MyPREFERENCES_url = "MyPrefs_url" ;
    SharedPreferences shared_pref;

    public static final String MyPREFERENCES = "MyPrefs" ;
    int PRIVATE_MODE = 0;
    SharedPreferences pref;
    SharedPreferences.Editor editor;

    private byte[] mRegisterImage;
    private byte[] mRegisterTemplate;
    private static int mImageWidth;
    private static int mImageHeight;
    private PendingIntent mPermissionIntent;
    private IntentFilter filter;
    long dwTimeStart = 0, dwTimeEnd = 0, dwTimeElapsed = 0;
    private JSGFPLib sgfplib;
    private Bitmap grayBitmap;
    private int[] grayBuffer;
    private int[] mMaxTemplateSize;
    private SGAutoOnEventNotifier auto_on;
    int[] score = new int[1];

    public static NetworkChange receiver;
    CheckInternetConnection internetConnection;
    ConnectionDetector cd;
    UserSessionManager session;
    TextToSpeech textToSpeech;

    Toolbar toolbar;
    ProgressDialog progressDialog;
    ProgressDialog progressDialog1;

    String myJSON = null;
    String RegisteredBase64_1, RegisteredBase64_2, RegisteredBase64_3, RegisteredBase64_4;
    String MobileNo;
    String emp_id;
    String str_RegisteredThumbs;
    String password;
    String logout_id = "0";
    String Url;
    String url_http;
    
    boolean thumb = false;

    EditText ed_MobNo;
    TextView txt_empName, txt_empId;
    ImageView img_register1, img_register2, img_register3, img_register4;
    Button btn_ViewDetails;
    LinearLayout reg_logout, res_logout, progress_layout;
    EditText ed_regLogout, ed_resLogout;
    Button btn_regLogout, btn_resetThumb, btn_resLogout;
    Snackbar snackbar;
    CoordinatorLayout snackbarCoordinatorLayout;

    List<String> RegisteredThumbs;

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
                    Toast.makeText(ThumbRegistration.this, "Device disconnected", Toast.LENGTH_LONG).show();
                    long error1 = sgfplib.Close();
                    finish();
                }
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        toolbar = (Toolbar)findViewById(R.id.toolbar_inner);
        TextView Header = (TextView)findViewById(R.id.header_text);
        ImageView img_logout = (ImageView)findViewById(R.id.img_logout);
        setSupportActionBar(toolbar);
       
        cd = new ConnectionDetector(getApplicationContext());
        url_http = cd.geturl();
        //Log.i("url_http", url_http);

        session = new UserSessionManager(getApplicationContext());
        internetConnection = new CheckInternetConnection(getApplicationContext());

        shared_pref = getSharedPreferences(MyPREFERENCES_url, MODE_PRIVATE);
        Url = (shared_pref.getString("url", ""));
        //Log.i("Url", Url);

        pref = getApplicationContext().getSharedPreferences(MyPREFERENCES, PRIVATE_MODE);
        password = pref.getString("password", "password");

        reg_logout = (LinearLayout)findViewById(R.id.reg_logout);
        ed_regLogout = (EditText)findViewById(R.id.ed_reg_logout);
        btn_regLogout = (Button)findViewById(R.id.btn_reg_logout);
        ed_resLogout = (EditText)findViewById(R.id.ed_res_logout);
        btn_resLogout = (Button)findViewById(R.id.btn_res_logout);
        snackbarCoordinatorLayout = (CoordinatorLayout)findViewById(R.id.CoordinatorLayout_reg);
        progress_layout = (LinearLayout)findViewById(R.id.progress_layout_tmbreg);


        if (getSupportActionBar() != null)
        {
            getSupportActionBar().setTitle("");
            Header.setText("Registration");
            img_logout.setVisibility(View.VISIBLE);
        }

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
                    internetConnection.showNetDisabledAlertToUser(ThumbRegistration.this);

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
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(ThumbRegistration.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                alertDialog.setTitle("Logout user / Switch activity");
                alertDialog.setCancelable(true);
                alertDialog.setPositiveButton("Switch To Attendance", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        editor = pref.edit();
                        editor.clear();
                        editor.commit();
                        session.logoutUser();
                        progress_layout.setVisibility(View.VISIBLE);
                        Intent intent = new Intent(ThumbRegistration.this, AttendanceActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
                alertDialog.setNegativeButton("Logout", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        logout_id = "1";
                        
                        editor = pref.edit();
                        editor.clear();
                        editor.commit();
                        session.logoutUser();

                        Intent intent = new Intent(ThumbRegistration.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });

                alertDialog.show();
            }
        });

        mMaxTemplateSize = new int[1];

        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);

        sgfplib = new JSGFPLib((UsbManager)getSystemService(Context.USB_SERVICE));

        auto_on = new SGAutoOnEventNotifier(sgfplib, ThumbRegistration.this);
        auto_on.start();
        
        ed_MobNo = (EditText)findViewById(R.id.ed_register_mobNo);
        txt_empName = (TextView)findViewById(R.id.txt_employeeName);
        txt_empId = (TextView)findViewById(R.id.txt_employeeId);
        img_register1 = (ImageView)findViewById(R.id.img_finger1);
        img_register2 = (ImageView)findViewById(R.id.img_finger2);
        img_register3 = (ImageView)findViewById(R.id.img_finger3);
        img_register4 = (ImageView)findViewById(R.id.img_finger4);
        btn_ViewDetails = (Button)findViewById(R.id.btn_ViewDetails);
        btn_resetThumb = (Button)findViewById(R.id.btn_reset_thumb);
        res_logout = (LinearLayout)findViewById(R.id.res_logout);


        grayBuffer = new int[JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES*JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES];
        for (int i=0; i<grayBuffer.length; ++i)
            grayBuffer[i] = android.graphics.Color.GRAY;
        grayBitmap = Bitmap.createBitmap(JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES, JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES, Bitmap.Config.ARGB_8888);
        grayBitmap.setPixels(grayBuffer, 0, JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES, 0, 0, JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES, JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES);

        int[] sintbuffer = new int[(JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES / 2) * (JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES / 2)];
        for (int i=0; i<sintbuffer.length; ++i)
            sintbuffer[i] = android.graphics.Color.GRAY;
        Bitmap sb = Bitmap.createBitmap(JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES / 2, JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES / 2, Bitmap.Config.ARGB_8888);
        sb.setPixels(sintbuffer, 0, JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES / 2, 0, 0, JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES / 2, JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES / 2);
        img_register1.setImageBitmap(grayBitmap);
        img_register2.setImageBitmap(grayBitmap);
        //img_register3.setImageBitmap(grayBitmap);
        //img_register4.setImageBitmap(grayBitmap);

        btn_ViewDetails.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                MobileNo = ed_MobNo.getText().toString();
                if (internetConnection.hasConnection(getApplicationContext()))
                {
                    if (!ed_MobNo.getText().toString().equals(""))
                    {
                        if (MobileNo.length() >= 10)
                        {
                            txt_empId.setText("");
                            txt_empName.setText("");
                            InputMethodManager in = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                            in.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
                            getEmpDetails();
                        }
                        else {
                            ed_MobNo.setError("Mobile no. must be greater than 9 digits");
                        }
                    }
                    else {
                        ed_MobNo.setError("Please enter mobile no.");
                    }
                }
                else {
                    Toast.makeText(ThumbRegistration.this, "Please check your internet connection", Toast.LENGTH_LONG).show();
                }
            }
        });

        btn_resetThumb.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (internetConnection.hasConnection(getApplicationContext()))
                {
                   /* res_logout.setVisibility(View.VISIBLE);
                    reg_logout.setVisibility(View.GONE);
                    ed_resLogout.requestFocus();*/
                    progress_layout.setVisibility(View.VISIBLE);
                    //unregisterReceiver(mUsbReceiver);
                    Intent intent = new Intent(ThumbRegistration.this, ResetThumbActivity.class);
                    startActivity(intent);
                    finish();
                }
                else {
                    Toast.makeText(ThumbRegistration.this, "Please check your internet connection", Toast.LENGTH_LONG).show();
                }
            }
        });

        btn_resLogout.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String PassWord = ed_resLogout.getText().toString();

                if (PassWord.equals(password))
                {
                    Intent intent = new Intent(ThumbRegistration.this, ResetThumbActivity.class);
                    startActivity(intent);
                    finish();
                }
                else
                {
                    ed_resLogout.setError("Please enter correct password");
                }
            }
        });

        btn_regLogout.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String PassWord = ed_regLogout.getText().toString();

                if (PassWord.equals(password))
                {
                    if (logout_id.equals("1"))
                    {
                        editor = pref.edit();
                        editor.clear();
                        editor.commit();
                        session.logoutUser();
                        //unregisterReceiver(mUsbReceiver);
                        Intent intent = new Intent(ThumbRegistration.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    else
                    {
                        progress_layout.setVisibility(View.VISIBLE);
                        Intent intent = new Intent(ThumbRegistration.this, AttendanceActivity.class);
                        AttendanceActivity.logout_status = true;
                        startActivity(intent);
                        finish();
                    }
                }
                else
                {
                    ed_regLogout.setError("Please enter correct password");
                }
            }
        });

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
    }

    public void CaptureFingerPrint()
    {
        auto_on.stop();
        MobileNo = ed_MobNo.getText().toString();
        if (!ed_MobNo.getText().toString().equals(""))
        {
            if (MobileNo.length() >= 10)
            {
                if (!txt_empName.getText().toString().equals(""))
                {
                    mRegisterImage = new byte[mImageWidth * mImageHeight];
                    ByteBuffer byteBuf = ByteBuffer.allocate(mImageWidth * mImageHeight);
                    dwTimeStart = System.currentTimeMillis();

                    long result = sgfplib.GetImage(mRegisterImage);
                    dwTimeEnd = System.currentTimeMillis();
                    dwTimeElapsed = dwTimeEnd - dwTimeStart;
                    Log.i("result", "" + result + " [" + dwTimeElapsed + "]");
                    dwTimeStart = System.currentTimeMillis();
                    //result = sgfplib.SetTemplateFormat(SecuGen.FDxSDKPro.SGFDxTemplateFormat.TEMPLATE_FORMAT_SG400);
                    result = sgfplib.SetTemplateFormat(SecuGen.FDxSDKPro.SGFDxTemplateFormat.TEMPLATE_FORMAT_ISO19794);
                    dwTimeEnd = System.currentTimeMillis();
                    dwTimeElapsed = dwTimeEnd - dwTimeStart;
                    Log.i("SetTemplateFormat", "" + result + " [" + dwTimeElapsed + "]");
                    SGFingerInfo fpInfo = new SGFingerInfo();

                    for (int i = 0; i < mRegisterTemplate.length; ++i)
                        mRegisterTemplate[i] = 0;
                    dwTimeStart = System.currentTimeMillis();
                    result = sgfplib.CreateTemplate(fpInfo, mRegisterImage, mRegisterTemplate);
                    dwTimeEnd = System.currentTimeMillis();
                    dwTimeElapsed = dwTimeEnd - dwTimeStart;
                    Log.i("CreateTemplate", "" + result + " [" + dwTimeElapsed + "]");
                    boolean[] matched = new boolean[1];
                    Log.i("mRegisterTemplate", "" + mRegisterTemplate);

                    if (RegisteredBase64_1 == null)
                    {
                        img_register1.setImageBitmap(ThumbRegistration.toGrayscale(mRegisterImage));
                        RegisteredBase64_1 = android.util.Base64.encodeToString(mRegisterTemplate, android.util.Base64.NO_WRAP);
                        Log.i("RegisteredBase64_1", RegisteredBase64_1);

                        String str = RegisteredBase64_1.substring(0,35);
                        Log.i("str", "" + str);

                        String base64 = "Rk1SACAyMAAAIAAKADUAAAEsAZAAxQDFAQA";
                        if (str.equalsIgnoreCase(base64))
                        {
                            RegisteredBase64_1 = null;
                            img_register1.setImageBitmap(grayBitmap);
                            Toast.makeText(ThumbRegistration.this, "Thumb not registered properly. Please try again", Toast.LENGTH_LONG).show();
                            textToSpeech.speak("Thumb not registered properly. Please try again", TextToSpeech.QUEUE_FLUSH, null);

                        }
                    }
                    else if (RegisteredBase64_2 == null)
                    {
                        img_register2.setImageBitmap(ThumbRegistration.toGrayscale(mRegisterImage));
                        RegisteredBase64_2 = android.util.Base64.encodeToString(mRegisterTemplate, android.util.Base64.NO_WRAP);
                        Log.i("RegisteredBase64_2", RegisteredBase64_2);

                        String str = RegisteredBase64_2.substring(0,35);
                        Log.i("str", "" + str);

                        String base64 = "Rk1SACAyMAAAIAAKADUAAAEsAZAAxQDFAQA";

                        if (str.equalsIgnoreCase(base64))
                        {
                            RegisteredBase64_2 = null;
                            img_register2.setImageBitmap(grayBitmap);
                            Toast.makeText(ThumbRegistration.this, "Thumb not registered properly. Please try again", Toast.LENGTH_LONG).show();
                            textToSpeech.speak("Thumb not registered properly. Please try again", TextToSpeech.QUEUE_FLUSH, null);

                        }
                    }
                    /*else if (RegisteredBase64_3 == null)
                    {
                        img_register3.setImageBitmap(RegistrationActivity.toGrayscale(mRegisterImage));
                        RegisteredBase64_3 = android.util.Base64.encodeToString(mRegisterTemplate, android.util.Base64.NO_WRAP);
                        Log.i("RegisteredBase64_3", RegisteredBase64_3);

                        String str = RegisteredBase64_3.substring(0,35);
                        Log.i("str", "" +str);

                        String base64 = "Rk1SACAyMAAAIAAKADUAAAEsAZAAxQDFAQA";

                        if (str.equalsIgnoreCase(base64))
                        {
                            RegisteredBase64_3 = null;
                            img_register3.setImageBitmap(grayBitmap);
                            Toast.makeText(RegistrationActivity.this, "Thumb not registered properly. Please try again", Toast.LENGTH_LONG).show();
                            textToSpeech.speak("Thumb not registered properly. Please try again", TextToSpeech.QUEUE_FLUSH, null);

                        }
                    }
                    else if (RegisteredBase64_4 == null)
                    {
                        img_register4.setImageBitmap(RegistrationActivity.toGrayscale(mRegisterImage));
                        RegisteredBase64_4 = android.util.Base64.encodeToString(mRegisterTemplate, android.util.Base64.NO_WRAP);
                        Log.i("RegisteredBase64_4", RegisteredBase64_4);
                        String str = RegisteredBase64_4.substring(0,35);
                        Log.i("str", "" +str);

                        String base64 = "Rk1SACAyMAAAIAAKADUAAAEsAZAAxQDFAQA";

                        if (str.equalsIgnoreCase(base64))
                        {
                            RegisteredBase64_4 = null;
                            img_register4.setImageBitmap(grayBitmap);
                            Toast.makeText(RegistrationActivity.this, "Thumb not registered properly. Please try again", Toast.LENGTH_LONG).show();
                            textToSpeech.speak("Thumb not registered properly. Please try again", TextToSpeech.QUEUE_FLUSH, null);

                        }
                    }*/

                    //if (RegisteredBase64_1 != null && RegisteredBase64_2 != null && RegisteredBase64_3 != null && RegisteredBase64_4 != null)
                    if (RegisteredBase64_1 != null && RegisteredBase64_2 != null)
                    {
                        MobileNo = ed_MobNo.getText().toString();
                        //str_RegisteredThumbs = RegisteredBase64_1 + ", " + RegisteredBase64_2 + ", " + RegisteredBase64_3 + ", " + RegisteredBase64_4;
                        str_RegisteredThumbs = RegisteredBase64_1 + ", " + RegisteredBase64_2;

                        RegisteredThumbs = new ArrayList<String>();
                        RegisteredThumbs.add(RegisteredBase64_1);
                        RegisteredThumbs.add(RegisteredBase64_2);
                        //RegisteredThumbs.add(RegisteredBase64_3);
                        //RegisteredThumbs.add(RegisteredBase64_4);
                        Log.i("str_RegisteredThumbs", "" + str_RegisteredThumbs);

                        thumbRegistration();
                    }
                }
                else {
                    ed_MobNo.setError("Please view Employee details");
                }
            }
            else {
                ed_MobNo.setError("Mobile no. must be greater than 9 digits");
            }
        }
        else {
            ed_MobNo.setError("Please enter mobile no.");
        }

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                Log.i("start", "start");
                thumb = true;
                auto_on.start();
            }
        }, 2000);
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
        bmpGrayscale.copyPixelsFromBuffer(ByteBuffer.wrap(Bits));
        return bmpGrayscale;
    }

    @Override
    public void run() {
    }

    @Override
    public void SGFingerPresentCallback() {
        fingerDetectedHandler.sendMessage(new Message());
    }

    public Handler fingerDetectedHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            CaptureFingerPrint();
            fingerDetectedHandler.removeMessages(0);
        }
    };

    public void getEmpDetails()
    {
        class GetDataJSON extends AsyncTask<String, Void, String>
        {
            private URL url;
            private String response = "";

            @Override
            protected void onPreExecute()
            {
                progressDialog = ProgressDialog.show(ThumbRegistration.this, "Please wait", "Getting Employee Details...", true);
                progressDialog.show();
            }

            @Override
            protected String doInBackground(String... params)
            {
                try
                {
                    String Transurl = ""+url_http+""+Url+"/owner/hrmapi/empdetail/?";
                    String query = String.format("mobile=%s", URLEncoder.encode(MobileNo, "UTF-8"));
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
                if (progressDialog!=null && progressDialog.isShowing())
                {
                    progressDialog.dismiss();
                }

                myJSON = result;
                Log.i("response", result);

                if (response.equals("[]"))
                {
                    ed_MobNo.setText("");
                    ed_MobNo.setError("Wrong mobile no.");
                }
                else
                {
                    try
                    {
                        JSONArray json = new JSONArray(result);
                        //Log.i("json", "" + json);

                        JSONObject object = json.getJSONObject(0);

                        String responsecode = object.getString("responsecode");

                        if (responsecode.equals("1"))
                        {
                            String emp_firstname = object.getString("firstName");
                            String emp_lastname = object.getString("lastName");
                            String emp_name = emp_firstname + " " + emp_lastname;
                            emp_id = object.getString("uId");

                            txt_empName.setText(emp_name);
                            txt_empId.setText(emp_id);
                        }
                        else if (responsecode.equals("2"))
                        {
                            String msg = object.getString("msg");
                            String message = msg.substring(2, msg.length()-2);
                            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null);

                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(ThumbRegistration.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                            alertDialog.setTitle(message);
                            alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    img_register1.setImageBitmap(grayBitmap);
                                    img_register2.setImageBitmap(grayBitmap);
                                    //img_register3.setImageBitmap(grayBitmap);
                                    //img_register4.setImageBitmap(grayBitmap);
                                    txt_empName.setText("");
                                    txt_empId.setText("");
                                    ed_MobNo.setText("");
                                }
                            });
                            alertDialog.show();
                        }
                        else if (responsecode.equals("0"))
                        {
                            String msg = object.getString("msg");
                            String message = msg.substring(2, msg.length()-2);

                            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null);

                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(ThumbRegistration.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                            alertDialog.setTitle(message);
                            alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    ed_MobNo.setText("");
                                }
                            });
                            alertDialog.show();
                        }
                    }
                    catch (JSONException e){
                        Toast.makeText(ThumbRegistration.this, "Sorry...Bad internet connection", Toast.LENGTH_LONG).show();
                        Log.e("Exception", e.toString());
                    }
                }
            }
        }
        GetDataJSON getDataJSON = new GetDataJSON();
        getDataJSON.execute();
    }


    public void thumbRegistration()
    {
        class GetDataJSON extends AsyncTask<String, Void, String>
        {
            private URL url;
            private String response = "";

            @Override
            protected void onPreExecute() {
                progressDialog1 = ProgressDialog.show(ThumbRegistration.this, "Please wait", "Registering thumb...", true);
                progressDialog1.show();
            }

            @Override
            protected String doInBackground(String... params)
            {
                try
                {
                    String Transurl = ""+url_http+""+Url+"/owner/hrmapi/thumregistration/?";

                    String query = String.format("empId=%s&thumexp=%s", URLEncoder.encode(emp_id, "UTF-8"), URLEncoder.encode(str_RegisteredThumbs, "UTF-8"));
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
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                return response;
            }

            @Override
            protected void onPostExecute(String result)
            {
                myJSON = result;
                Log.i("response", result);

                progressDialog1.dismiss();

                if (response.equals("[]"))
                {
                    Toast.makeText(ThumbRegistration.this, "Sorry...Bad internet connection", Toast.LENGTH_LONG).show();
                }
                else
                {
                    Toast.makeText(ThumbRegistration.this, "Thumbs Registered Successfully", Toast.LENGTH_LONG).show();
                    textToSpeech.speak("Thumbs Registered Successfully!", TextToSpeech.QUEUE_FLUSH, null);

                    img_register1.setImageBitmap(grayBitmap);
                    img_register2.setImageBitmap(grayBitmap);
                    //img_register3.setImageBitmap(grayBitmap);
                    //img_register4.setImageBitmap(grayBitmap);
                    txt_empName.setText("");
                    txt_empId.setText("");
                    ed_MobNo.setText("");

                    RegisteredBase64_1 = null;
                    RegisteredBase64_2 = null;
                    //RegisteredBase64_3 = null;
                    //RegisteredBase64_4 = null;
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
        
        IntentFilter filter = new IntentFilter(ACTION_USB_DETACHED);
        registerReceiver(mUsbReceiver1, filter);

        IntentFilter filter1 = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(receiver, filter1);

        long error = sgfplib.Init( SGFDxDeviceName.SG_DEV_AUTO);

        if (error != SGFDxErrorCode.SGFDX_ERROR_NONE)
        {
            AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
            if (error == SGFDxErrorCode.SGFDX_ERROR_DEVICE_NOT_FOUND)
                dlgAlert.setMessage("The attached fingerprint device is not supported on Android");
            else
                dlgAlert.setMessage("Fingerprint device initialization failed!");
            dlgAlert.setTitle("Fingerprint SDK");
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
                mImageWidth = deviceInfo.imageWidth;
                mImageHeight= deviceInfo.imageHeight;
                sgfplib.SetTemplateFormat(SGFDxTemplateFormat.TEMPLATE_FORMAT_ISO19794);
                sgfplib.GetMaxTemplateSize(mMaxTemplateSize);
                mRegisterTemplate = new byte[mMaxTemplateSize[0]];
                sgfplib.WriteData((byte)5, (byte)1);
            }
        }
    }


    @Override
    public void onPause()
    {
        unregisterReceiver(mUsbReceiver);
        unregisterReceiver(mUsbReceiver1);
        unregisterReceiver(receiver);

        sgfplib.CloseDevice();
        mRegisterImage = null;
        mRegisterTemplate = null;
        img_register1.setImageBitmap(grayBitmap);
        super.onPause();
    }


    @Override
    public void onBackPressed()
    {
        return;
    }
}