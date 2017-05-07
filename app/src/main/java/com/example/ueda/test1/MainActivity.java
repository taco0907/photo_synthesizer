package com.example.ueda.test1;

import android.app.ActivityGroup;
import android.app.LocalActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;


import com.google.android.gms.panorama.Panorama;
import com.google.android.gms.panorama.PanoramaApi;

import com.theta360.lib.PtpipInitiator;
import com.theta360.lib.ThetaException;
import com.theta360.lib.ptpip.entity.ObjectHandles;
import com.theta360.lib.ptpip.entity.ObjectInfo;
import com.theta360.lib.ptpip.entity.PtpObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends ActivityGroup implements
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener
{

    public static final int AUDIO_NUM_1 = 1;
    public static final int AUDIO_NUM_2 = 2;
    public static final int AUDIO_NUM_3 = 3;

    public static final int WAVE_KIND_SIN = 1;
    public static final int WAVE_KIND_SQUARE = 2;
    public static final int WAVE_KIND_SAW = 3;


    private Button m_btn;
    private Button m_btn_sound;
    private Button m_btn_panorama;
    private ImageView m_img;
    private SeekBar m_seek;
    private SeekBar m_seek2;
    private SeekBar m_seek3;
    private TextView m_text_accel;
    private Spinner m_spinner;

    private PtpipInitiator camera;
    private LogView log_viewer;

    private PtpipInitiator m_camera;
    private SensorManager m_sensormanager;
    private MySensorListener m_sensor_listener;

    private AudioTrack audioTrack;
    private AudioTrack audioTrack2;//２つ走らせるなら、別スレッドで？
    private AudioTrack audioTrack3;
    private byte[] sinWave;
    private byte[] sinWave2;
    private byte[] sinWave3;
    private int BUFFSIZE = 4400;
    private int BUFF_WRITE_SIZE = 4400;
    private float m_freq = 440;
    private float m_freq2 = 440;
    private float m_freq3 = 440;
    private int m_wave_kind = 0;
    private int m_wave_kind2 = 0;
    private int m_wave_kind3 = 0;

    private boolean is_running = false;

    private GoogleApiClient mGoogleApiClient;
    private WindowManager wm;
    private Button m_btn_tmp;
    private LocalActivityManager am;
    private LinearLayout layout_group_test;

    private CustomView m_custom_view;


    private WindowManager.LayoutParams params;
    private WindowManager.LayoutParams params2;
    int view_flag = 1;
    private Handler mHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        m_btn = (Button)findViewById(R.id.btn);
        m_btn.setOnClickListener(new BtnOnClickListener());
        m_btn_panorama = (Button)findViewById(R.id.btn_panorama);
        m_btn_panorama.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent,0);
            }
        });
        m_seek = (SeekBar)findViewById(R.id.seekbar);
        m_seek2 = (SeekBar)findViewById(R.id.seekbar2);
        m_seek3 = (SeekBar)findViewById(R.id.seekbar3);
        m_seek.setOnSeekBarChangeListener(new MySeekOnSeekBarChangeListner(AUDIO_NUM_1));
        m_seek2.setOnSeekBarChangeListener(new MySeekOnSeekBarChangeListner(AUDIO_NUM_2));
        m_seek3.setOnSeekBarChangeListener(new MySeekOnSeekBarChangeListner(AUDIO_NUM_3));
        m_text_accel = (TextView)findViewById(R.id.text_accel);
        m_spinner = (Spinner)findViewById(R.id.spinner_1);
        ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_spinner_item);
        adapter.add("sin");
        adapter.add("square");
        adapter.add("saw");
        m_spinner.setAdapter(adapter);
        m_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Spinner spinner;
                spinner = (Spinner)parent;
                String tmp_str = (String)spinner.getSelectedItem();
                switch (tmp_str) {
                    case "sin" :
                        m_wave_kind = WAVE_KIND_SIN;
                        break;
                    case "square" :
                        m_wave_kind = WAVE_KIND_SQUARE;
                        break;
                    case "saw" :
                        m_wave_kind = WAVE_KIND_SAW;
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        layout_group_test = (LinearLayout)findViewById(R.id.main_layout);
        am = getLocalActivityManager();


        m_btn_sound = (Button)findViewById(R.id.btn_sound);
        m_btn_sound.setOnClickListener(new SoundBtnOnClickListner());
        m_img = (ImageView)findViewById(R.id.img);
        log_viewer = (LogView)findViewById(R.id.log_view);

        m_sensormanager = (SensorManager)getSystemService(SENSOR_SERVICE);
        m_sensor_listener = new MySensorListener();

        mHandler = new Handler();


        //材料生成.
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                44100,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                BUFFSIZE,
                AudioTrack.MODE_STREAM
        );


        audioTrack2 = new AudioTrack(AudioManager.STREAM_MUSIC,
                44100,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                BUFFSIZE,
                AudioTrack.MODE_STREAM
        );

        audioTrack3 = new AudioTrack(AudioManager.STREAM_MUSIC,
                44100,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                BUFFSIZE,
                AudioTrack.MODE_STREAM
        );

        sinWave = new byte[BUFFSIZE];
        sinWave2 = new byte[BUFFSIZE];
        sinWave3 = new byte[BUFFSIZE];



        //お蔵入り.

        //BUFFSIZE分フレーム再生が終わると呼ばれる
//        audioTrack.setPositionNotificationPeriod(BUFFSIZE);
//        audioTrack.setPlaybackPositionUpdateListener(
//
//                new AudioTrack.OnPlaybackPositionUpdateListener() {
//
//                    @Override
//                    public void onMarkerReached(AudioTrack track) {
//
//                    }
//
//                    @Override
//                    public void onPeriodicNotification(final AudioTrack audioTrack) {
//
//                        //audioTrack.write(sinWave,0,BUFFSIZE);
//
//                        audioTrack.play();
//                        //write処理.
//                        SoundTask m_st = new SoundTask(sinWave, BUFFSIZE);
//                        m_st.start();
//
//
//                    }
//                }
//        );

        //generateSinWave(440);
        //サイン波ここまで


        //とりあえずシータとは接続しない.
        //new LoadPhotoTask("192.168.1.1").execute();

    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Panorama.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        mGoogleApiClient.connect();




    }

    @Override
    protected void onStop() {
        if(mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        m_sensormanager.registerListener(
                m_sensor_listener,
                m_sensormanager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //wm.removeViewImmediate(m_btn_tmp);
        wm.removeViewImmediate(m_custom_view);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        m_sensormanager.unregisterListener(m_sensor_listener);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case 0:
                showPanorama(data);
                break;
            default:
                appendLogView("result code error");
                break;
            //エラー処理入れてない…
        }
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

    private void showPanorama(Intent data) {
        Uri uri = data.getData();
        PendingResult<PanoramaApi.PanoramaResult> result = Panorama.PanoramaApi.loadPanoramaInfo(mGoogleApiClient, uri);
        result.setResultCallback(new ResultCallback<PanoramaApi.PanoramaResult>() {
            @Override
            public void onResult(PanoramaApi.PanoramaResult panoramaResult) {
                Intent intent = panoramaResult.getViewerIntent();
                if(intent != null) {
                    appendLogView("panorama get!");

                    startActivity(intent);

//                    キャプチャとるのもむりやった…
//                    File file = new File(Environment.getExternalStorageDirectory(),"aaa.jpg");
//                    saveCapture(findViewById(android.R.id.content), file);



                    //複数Activity
                    //むりやった…

//                    Window window = am.startActivity("panorama",intent);
//                    View view = window.getDecorView();
//                    layout_group_test.addView(view);



//                      View重ねる実験.

                    params = new WindowManager.LayoutParams(
                            WindowManager.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.WRAP_CONTENT,
                            0,600,
                            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                            PixelFormat.TRANSLUCENT);
                    params2 = new WindowManager.LayoutParams(
                            WindowManager.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.WRAP_CONTENT,
                            0,600,
                            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                            PixelFormat.TRANSLUCENT);
                    wm = (WindowManager)getApplication().getSystemService(Context.WINDOW_SERVICE);

//                    m_btn_tmp = new Button(getApplication());
//                    m_btn_tmp.setText("layer_test");
//
//                    m_btn_tmp.setAlpha(0xff);
//                    m_btn_tmp.setVerticalFadingEdgeEnabled(true);
//                    m_btn_tmp.setHorizontalFadingEdgeEnabled(true);
//                    m_btn.setOnTouchListener(new WindowOnTouchListner());
//                    m_btn.setOnClickListener(new WindosOnClickListener());
//                    wm.addView(m_btn_tmp, params);

                    m_custom_view = new CustomView(MainActivity.this);
                    //m_custom_view.setFilterTouchesWhenObscured(true);
                    wm.addView(m_custom_view, params);



//                    view_flag = -1;
//
//                    //タイマーで切り替えてみるか…
//                    Timer viewTimer = new Timer(true);
//                    viewTimer.scheduleAtFixedRate(new TimerTask() {
//                        @Override
//                        public void run() {
//
//                            wm.removeView(m_custom_view);
//
//                            if(view_flag > 0) {
//
//                                mHandler.post(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        wm.addView(m_custom_view, params);
//                                    }
//                                });
//
//                            }else{
//
//                                mHandler.post(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        wm.addView(m_custom_view, params2);
//                                    }
//                                });
//                            }
//                            view_flag *= -1;
//
//                        }
//                    },0,100);
//
//                    PackageManager pm = getPackageManager();
//                    ResolveInfo resolveInfo = pm.resolveActivity(intent,0);
//                    ActivityInfo activityInfo = resolveInfo.activityInfo;
//
//                    appendLogView("パッケージ名 ："+activityInfo.packageName);
//                    appendLogView("Activityクラス名 ："+activityInfo.name);
//

                }else {
                    appendLogView("not panorama..");
                }
            }
        });


    }

    public void saveCapture(View view, File file) {
        Bitmap capture = getViewCapture(view);
        FileOutputStream fos = null;

        try{
            fos = new FileOutputStream(file);
            capture.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();

            appendLogView("flush");

        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(fos != null) {
                try {
                    fos.close();
                }catch (IOException ie) {
                    fos = null;
                }
            }
        }

    }

    public Bitmap getViewCapture(View view) {
        view.setDrawingCacheEnabled(true);

        Bitmap cache = view.getDrawingCache();
        Bitmap screenShot = Bitmap.createBitmap(cache);
        view.setDrawingCacheEnabled(false);
        return  screenShot;
    }



    @Override
    public void onConnected(Bundle bundle) {
        appendLogView("connnected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        appendLogView("connnection suspend");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        appendLogView("connnection failed");
    }

    class BtnOnClickListener implements OnClickListener {

        boolean flg = false;

        @Override
        public void onClick(View v) {

            log_viewer.append("btn_click");

            if(flg == false) {
                m_img.setImageResource(R.drawable.ic_launcher);
                flg = true;
            }else {
                m_img.setImageResource(0);
                flg = false;
            }

        }

    }

    class SoundBtnOnClickListner implements OnClickListener{

        private boolean flg = true;
        SoundTask m_st = null;
        SoundTask2 m_st2 = null;
        SoundTask3 m_st3 = null;

        //おためし
        Bitmap cache;
        Bitmap bitmap;

        @Override
        public void onClick(View v) {


            if(flg == true) {
                is_running = true;
                appendLogView("sound_on");
                audioTrack.setVolume(1);
                audioTrack.play();

                audioTrack2.setVolume(1);
                audioTrack2.play();

                audioTrack3.setVolume(1);
                audioTrack3.play();

                m_st = new SoundTask();
                m_st.start();

                m_st2 = new SoundTask2();
                m_st2.start();

                m_st3 = new SoundTask3();
                m_st3.start();

                flg = false;
            }else{
                is_running = false;
                audioTrack.setVolume(0);
                audioTrack.stop();
                audioTrack2.setVolume(0);
                audioTrack2.stop();
                audioTrack3.setVolume(0);
                audioTrack3.stop();

                m_st = null;
                m_st2 = null;
                m_st3 = null;
                appendLogView("sound_stop");
                flg = true;

            }


            //おためしで…スクショとってみる
            layout_group_test.setDrawingCacheEnabled(true);
            cache = layout_group_test.getDrawingCache();
            if(cache == null) {
            }
            bitmap = Bitmap.createBitmap(cache);
            layout_group_test.setDrawingCacheEnabled(false);
            appendLogView("bit count : " + bitmap.getPixel(500,1200));

            //おためしで…スクショとってみる2
            File file = new File(Environment.getExternalStorageDirectory(),"aaa.jpg");
            //file.getParentFile().mkdir();
            saveCapture(findViewById(android.R.id.content), file);


        }
    }

    class MySeekOnSeekBarChangeListner implements SeekBar.OnSeekBarChangeListener {

        int audio_num = 0;

        MySeekOnSeekBarChangeListner(int rv_num){
            audio_num = rv_num;
        }


        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            float at_freq = 0;

            at_freq = (progress/50.0f) * 440;
            appendLogView("seek : " + progress + " at_freq_old : " + at_freq);


            //とりあえず20の整数倍にしとけばいいのか？
            //BUFFSIZE可変で解決しそうなので削除.

            int tmp = Math.round(at_freq);
            tmp /= 10;
            tmp = tmp - (tmp % 2);
            tmp *= 10;
            at_freq = (float)tmp;

            switch(audio_num) {
                case AUDIO_NUM_1:
                    m_freq = at_freq;
                    break;
                case AUDIO_NUM_2:
                    m_freq2 = at_freq;
                    break;
                case AUDIO_NUM_3:
                    m_freq3 = at_freq;
                    break;
                default:
                    break;
            }
            //BUFFSIZE調整.
//            int buff_tmp = (int)(88000*22 / m_freq);
//            if(buff_tmp <= BUFFSIZE ){
//                BUFF_WRITE_SIZE = buff_tmp;
//            }
//            appendLogView("buff_tmp : " + buff_tmp + " at_freq_new : " + m_freq);

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }

    class SoundTask extends Thread {

         //音生成、再生用タスク.

         @Override
         public void run() {
             setPriority(Thread.MAX_PRIORITY);

             while(is_running) {

                 //sinWaveにデータ生成.

                 double dt = 1.0f / 44000;
                 double t = 0.0f;

                 switch (m_wave_kind) {
                     case WAVE_KIND_SIN:
                         for (int i = 0; i < BUFF_WRITE_SIZE; i++, t += dt) {
                             sinWave[i] = (byte) (Byte.MAX_VALUE * Math.sin(m_freq * Math.PI * t));
                         }
                         break;

                     case WAVE_KIND_SQUARE:
                         for (int i = 0; i < BUFF_WRITE_SIZE; i++, t += dt) {
                             if(Math.sin(m_freq * Math.PI * t) > 0 ) {
                                 sinWave[i] = (byte)(Byte.MAX_VALUE * i);
                             }else {
                                 sinWave[i] = -1 * Byte.MAX_VALUE;
                             }
                         }
                         break;

                     case WAVE_KIND_SAW:
                         for (int i = 0; i < BUFF_WRITE_SIZE; i++, t += dt) {
                             //のこぎり波.
                             sinWave[i] = 0;
                             for(int j=0; j<7; j++) {
                                 sinWave[i] += (byte) ((Byte.MAX_VALUE) * Math.sin(m_freq * Math.PI * t));
                             }
                         }
                         break;

                     default:
                         break;

                 }


                 //書き込み.
                 audioTrack.write(sinWave,0,BUFF_WRITE_SIZE);

             }

         }

    }


    class SoundTask2 extends Thread {

        //音生成、再生用タスク.

        @Override
        public void run() {
            setPriority(Thread.MAX_PRIORITY);

            while(is_running) {

                //sinWaveにデータ生成.

                //とりあえず20の整数倍にしとけばいいのか？
//                int tmp = Math.round(m_freq);
//
//                tmp /= 10;
//                tmp = tmp - (tmp % 2);
//                tmp *= 10;
//                m_freq = (float)tmp;
//                //appendLogView("rv_freq : " + m_freq);

                double dt = 1.0f / 44000;
                double t = 0.0f;

                for (int i = 0; i < BUFF_WRITE_SIZE; i++, t += dt) {
                    sinWave2[i] = (byte) (Byte.MAX_VALUE * Math.sin(m_freq2 * Math.PI * t));
                    //sinWave2[i] += 0.02*i;
                }

                //書き込み.
                audioTrack2.write(sinWave2,0,BUFF_WRITE_SIZE);

            }
        }
    }

    class SoundTask3 extends Thread {

        //音生成、再生用タスク.

        @Override
        public void run() {
            setPriority(Thread.MAX_PRIORITY);

            while(is_running) {

                double dt = 1.0f / 44000;
                double t = 0.0f;

                for (int i = 0; i < BUFF_WRITE_SIZE; i++, t += dt) {
                    sinWave3[i] = (byte) (Byte.MAX_VALUE * Math.sin(m_freq3 * Math.PI * t));
                    //sinWave3[i] += i;
                }

                //書き込み.
                audioTrack3.write(sinWave3,0,BUFF_WRITE_SIZE);

            }
        }
    }


    class MySensorListener implements SensorEventListener {

        float[] gravity = new float[3];


        @Override
        public void onSensorChanged(SensorEvent event) {

            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    gravity = event.values.clone();
                    m_text_accel.setText(gravity[0] + " " + gravity[1] + " " + gravity[2]);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

    }

    public class CustomView extends View {

        public CustomView(Context context) {
            super(context);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {

            appendLogView("toucch");
            Log.e("ueda","44");

            return false;
        }
    }

    private void appendLogView(final String rv_text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                log_viewer.append(rv_text);
            }
        });
    }

    private class LoadPhotoTask extends AsyncTask<Void, Object, PtpObject> {

        private String cameraIpAdress;
        private int objectHandle;


        public LoadPhotoTask (String cameraIpAdress) {
            this.cameraIpAdress = cameraIpAdress;
            //this.objectHandle = objectHandle;

        }

        @Override
        protected PtpObject doInBackground(Void... params) {
            appendLogView("doin");

            try {
                PtpipInitiator camera = new PtpipInitiator(cameraIpAdress);
                m_camera = camera;
                ObjectHandles objectHandles = camera.getObjectHandles(
                        PtpipInitiator.PARAMETER_VALUE_DEFAULT,
                        PtpipInitiator.PARAMETER_VALUE_DEFAULT,
                        PtpipInitiator.PARAMETER_VALUE_DEFAULT
                );

                int objectHandleCount = objectHandles.size();
                appendLogView("handle count : " + String.valueOf(objectHandleCount));

                for(int i =0;i<objectHandleCount; i++) {
                    final int objecthandle = objectHandles.getObjectHandle(i);
                    ObjectInfo object = camera.getObjectInfo(objecthandle);
                    appendLogView("object " + i + object.getCaptureDate());

                    //サムネイル取得.
                    if(object.getObjectFormat() == ObjectInfo.OBJECT_FORMAT_CODE_EXIF_JPEG) {
                        PtpObject thum = camera.getThumb(objecthandle);
                        final byte[] thumnailImage = thum.getDataObject();
                        ByteArrayInputStream inputStreamThum = new ByteArrayInputStream(thumnailImage);
                        final Drawable thumnail = BitmapDrawable.createFromStream(inputStreamThum, null);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                m_img.setImageDrawable(thumnail);
                            }
                        });

                    }
                }

                //おためし

                /*
                camera.setTimeLapseInterval(1000);
                int tmp = m_camera.getTimeLapseInterval();
                appendLogView("inter:" + tmp);
                */

                /*
                try {

                    camera.setTimeLapseInterval(10000);
                    int tmp = m_camera.getTimeLapseInterval();
                    appendLogView("inter:" + tmp);

                }catch (NullPointerException e) {
                    appendLogView(Log.getStackTraceString(e));
                }catch (ThetaException e) {
                    appendLogView("io exxx");
                }
                */

                //ObjectInfo objectInfo = camera.getObjectInfo(objectHandle);

                //PtpObject resizedImageObject = camera.getResizedImageObject(objectHandle, 2048, 1024);

                return null;

            }catch (IOException e) {
              appendLogView("io exception");
                return null;

            } catch(ThetaException e) {
                appendLogView("theta exception");
                String errorLog = Log.getStackTraceString(e);
                appendLogView(errorLog);
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(PtpObject ptpObject) {
            super.onPostExecute(ptpObject);
            appendLogView("doin_end");

            //タイマータスクで写真とろうとしたけど無理っぽいので…
            //とりあえずけしとく


            MyTimerTask timerTask = new MyTimerTask();
            Timer mTimer = new Timer(true);
            mTimer.schedule(timerTask,500,500);



        }

    }

    class MyTimerTask extends TimerTask{

        private int time_count = 0;

        @Override
        public void run() {
            time_count++;
            appendLogView("time is : " + time_count);


            try {
                //String gpsInfo = m_camera.getGpsInfo();
                //appendLogView("prop : " + gpsInfo);
                appendLogView("prop : "+ m_camera.getDevicePropValue(0x5020));
                //m_camera.initiateCapture();
                return;
            }catch (ThetaException e){
                appendLogView("shoot fail");
                return;
            }

        }
    }




}
