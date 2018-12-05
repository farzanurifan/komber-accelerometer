package com.example.farzanurifan.komberaccelerometer;

import android.Manifest;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private float sensor_x, sensor_y, sensor_z;
    private String allSensor[] = new String[200];
    private int panjangData = 0;
    GraphView graph;
    TextView data, timer, coba, info;
    Button starter;
    ToneGenerator toneGen;

    private final int WRITE_EXTERNAL_STORAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestStoragePermission();

        info = (TextView) findViewById(R.id.info);
        data = (TextView) findViewById(R.id.data);
        timer = (TextView) findViewById(R.id.timer);
        coba = (TextView) findViewById(R.id.coba);
        starter = (Button) findViewById(R.id.starter);
        graph = (GraphView) findViewById(R.id.graph);
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_FASTEST);

        // set manual X bounds
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(-senAccelerometer.getMaximumRange());
        graph.getViewport().setMaxY(senAccelerometer.getMaximumRange());

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(150);

        // enable scaling and scrolling
        graph.getViewport().setScalable(true);
        graph.getViewport().setScalableY(true);

        String infoColor = "<font color='red'>X,</font> <font color='green'>Y,</font> <font color='blue'>Z:</font>";
        info.setText(Html.fromHtml(infoColor), TextView.BufferType.SPANNABLE);
        data.setTextColor(Color.BLACK);
        timer.setTextColor(Color.BLACK);
        coba.setTextColor(Color.BLACK);
        timer.setText("Timer: 5 detik");
        coba.setText("Tekan tombol start untuk mulai merekam");

        toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

        starter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new CountDownTimer(5100, 1000){
                    int counter = 5;
                    public void onTick(long millisUntilFinished){
                        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP,100);
                        String time = "Timer: " + String.valueOf(counter) + " detik";
                        timer.setText(time);
                        counter--;
                    }
                    public  void onFinish(){
                        timer.setText("Data sedang direkam");
                        coba.setText("Data direkam selama 5 detik");
                        recordData();
                    }
                }.start();
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            sensor_x = sensorEvent.values[0];
            sensor_y = sensorEvent.values[1];
            sensor_z = sensorEvent.values[2];
            String hasil = sensor_x + ", " + sensor_y + ", " + sensor_z;
            data.setText(hasil);
        }
    }

    public void recordData() {
        File sdCard = Environment.getExternalStorageDirectory();
        Date currentTime = Calendar.getInstance().getTime();
        File folder = new File(sdCard.getAbsolutePath() + "/komber");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        final String csv = sdCard.getAbsolutePath() + "/komber/" + currentTime + ".csv";

        panjangData = 0;
        allSensor = new String [200];
        try {
            new CountDownTimer(5000, 25){
                CSVWriter writer = new CSVWriter(new FileWriter(csv));
                int counter = 0;
                public void onTick(long millisUntilFinished){
                    toneGen.startTone(ToneGenerator.TONE_CDMA_PIP,25);
                    allSensor[counter] = sensor_x + "," + sensor_y + "," + sensor_z;
                    String [] hasilCsv = allSensor[counter].split(",");

                    if(counter == 0) {
                        String [] header = {"sensor_x", "sensor_y", "sensor_z"};
                        writer.writeNext(header);
                    }
                    writer.writeNext(hasilCsv);

                    counter++;
                }
                public  void onFinish(){
                    panjangData = counter;
                    graph.removeAllSeries();

                    LineGraphSeries<DataPoint> series1 = new LineGraphSeries<DataPoint>(generateData(0));
                    LineGraphSeries<DataPoint> series2 = new LineGraphSeries<DataPoint>(generateData(1));
                    LineGraphSeries<DataPoint> series3 = new LineGraphSeries<DataPoint>(generateData(2));

                    series1.setColor(Color.RED);
                    series2.setColor(Color.GREEN);
                    series3.setColor(Color.BLUE);

                    graph.addSeries(series1);
                    graph.addSeries(series2);
                    graph.addSeries(series3);

                    try {
                        timer.setText("Selesai");
                        coba.setText("Data tersimpan, tekan tombol start untuk memulai kembali");
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private DataPoint[] generateData(int index) {
        int count = panjangData - 1;
        DataPoint[] values = new DataPoint[count];
        for (int i=0; i < count; i++) {
            String [] koko = allSensor[i].split(",");
            DataPoint v = new DataPoint(i, Double.valueOf(koko[index]));
            values[i] = v;
        }
        return values;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(WRITE_EXTERNAL_STORAGE)
    public void requestStoragePermission() {
        String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if(!EasyPermissions.hasPermissions(this, perms)) {
            EasyPermissions.requestPermissions(this, "Please grant the storage permission", WRITE_EXTERNAL_STORAGE, perms);
        }
    }
}
