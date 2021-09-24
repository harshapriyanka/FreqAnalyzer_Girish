package com.example.freqanalyzer1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.jjoe64.graphview.GraphView;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.UniversalAudioInputStream;
import be.tarsos.dsp.mfcc.MFCC;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class MainActivity extends AppCompatActivity {

    //LineGraphSeries<DataPoint> series;
    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    FileOutputStream os = null;
    AudioRecord recorder;
    MediaPlayer mPlayer;
    String filePath = "";
    String fileName = "";
    File file;
    int minBufferSize = 512;
    private int audioSource = MediaRecorder.AudioSource.MIC;
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private int sampleRate = 44100;
    short[] buffer;
    boolean isFilter = false;




    LineGraphSeries<DataPoint> series;
    LineGraphSeries<DataPoint> phaseSeries;
    LinkedHashMap<Float,Double> AngleMap;
    LinkedHashMap<Float,Double> originalAngleMap;
    LinkedHashMap<Float,Double> phaseDifMap;
    static boolean isOriginalFile = false;



    boolean stopWhile=true;
    ArrayList<ArrayList<Float>> frequencyArrayArray = new ArrayList<ArrayList<Float>>();
    ArrayList<ArrayList<Double>> magnitudeArrayArray = new ArrayList<ArrayList<Double>>();
    ArrayList<Map<Float,Double>> originalAngleMapArray = new ArrayList<>();
    ArrayList<Map<Float,Double>> AngleMapArray = new ArrayList<>();
    Switch phaseSwitch;
    HSSFWorkbook workbook = null;
    HSSFSheet firstSheet = null;
    int exelRow=0;
    int exelCol=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    123);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    123);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    123);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_LOGS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_LOGS},
                    123);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SYNC_SETTINGS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SYNC_SETTINGS},
                    123);
        }

        ProgressBar  simpleProgressBar= (ProgressBar)findViewById(R.id.simpleProgressBar);
        Button showGraph = (Button)findViewById(R.id.showGraph);

        showGraph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(intent, 1);

                String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                        +"/"+ AUDIO_RECORDER_FOLDER;
                Log.d("Files", "Path: " + path);
                File directory = new File(path);
                File[] files = directory.listFiles();
                Log.d("Files", "Size: "+ files.length);
                for (int i = 0; i < files.length; i++)
                {
                    if(files[i].getName().endsWith(".wav")){
                        Log.d("Files", "FileName:" + files[i].getName());
                        Automation automation = new Automation();
                        automation.automate(files[i].getName());
                    }

                }


            }
        });


        final Button playSound = (Button)findViewById(R.id.playSound);
        final Button playSound57 = (Button)findViewById(R.id.playSound57);
        final Button playSound31013 = (Button)findViewById(R.id.playSound31013);
        final Button recordAudio = (Button)findViewById(R.id.recordAudio);
        final Button stopRecord = (Button)findViewById(R.id.stopRecord);

        stopRecord.setVisibility(View.INVISIBLE);

        playSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //trimFile();
                getTempFilename();
                startThread = true;
                long start = System.currentTimeMillis();
                recordAudio();
                startRecording();
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.kilo5trim0dot01s);
                mediaPlayer.start();


                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                stopRecord();
//                long end = System.currentTimeMillis();
//                long elapsedTime = end - start;

//                Toast.makeText(MainActivity.this, Math.toIntExact((int) elapsedTime),
//                        Toast.LENGTH_LONG).show();
                //extractLogToFileAndWeb();


            }
        });


        playSound57.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //trimFile();
                getTempFilename();
                startThread = true;
                recordAudio();
                startRecording();
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.kilo5trim0dot01s);
                MediaPlayer mediaPlayer7 = MediaPlayer.create(getApplicationContext(), R.raw.s7khz1s01);
                mediaPlayer.start();
                mediaPlayer7.start();


                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                stopRecord();
                //extractLogToFileAndWeb();


            }
        });


        playSound31013.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //trimFile();
                getTempFilename();
                startThread = true;
                recordAudio();
                startRecording();
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.s3khzs01);
                MediaPlayer mediaPlayer7 = MediaPlayer.create(getApplicationContext(), R.raw.s10khzs01);
                MediaPlayer mediaPlayer9 = MediaPlayer.create(getApplicationContext(), R.raw.s13khzs01);
                mediaPlayer.start();
                mediaPlayer7.start();
                mediaPlayer9.start();


                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                stopRecord();
                //extractLogToFileAndWeb();


            }
        });

        recordAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getTempFilename();
                startThread = true;
                recordAudio.setEnabled(false);
                recordAudio.setVisibility(View.INVISIBLE);
                stopRecord.setVisibility(View.VISIBLE);
                recordAudio();
                startRecording();
                recordAudio.setEnabled(true);
                recordAudio.setVisibility(View.VISIBLE);

            }
        });



        stopRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecord.setEnabled(false);
                stopRecord();
                stopRecord.setVisibility(View.INVISIBLE);
                stopRecord.setEnabled(true);

            }
        });

        final Button filterAudio = (Button)findViewById(R.id.filterAudio);
        final Button MFCC = (Button)findViewById(R.id.MFCC);
        final Button Amplitude_Energy_Data = (Button)findViewById(R.id.Amplitude_Energy_Data);

        filterAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getTempFilename();
                //filterAudio.setEnabled(false);
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");

                //Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                //intent.addCategory(Intent.CATEGORY_OPENABLE);
                //intent.setType("*/*");
                startActivityForResult(intent, 2);
                ProgressBar  simpleProgressBar= (ProgressBar)findViewById(R.id.simpleProgressBar);
                simpleProgressBar.setVisibility(View.INVISIBLE);

            }
        });
        MFCC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getTempFilename();
                /*For opening filemanager and selecting .wav file*/
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.putExtra("ta","1");
                startActivityForResult(intent, 2);




                MFCCThread mfccThread = new MFCCThread();
                mfccThread.start();

            }
        });



        Amplitude_Energy_Data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getTempFilename();
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.putExtra("ta","2");
                startActivityForResult(intent, 2);
            }
        });

       /* if(isCreateFile){
            createFile();
        }
        graphView = (GraphView)findViewById(R.id.graphy);
        graphView.getViewport().setMaxXAxisSize(20000);
        graphView.getViewport().setMaxYAxisSize(800000);
        boolean isRecording = false;
        Log.i("sii","aasd");*/
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();
                    String uriString = uri.toString();
                    File myFile = new File(uriString);
                    String path = myFile.getAbsolutePath();
                    String displayName = null;

                    if (uriString.startsWith("content://")) {
                        Cursor cursor = null;
                        try {
                            cursor = getContentResolver().query(uri, null, null, null, null);
                            if (cursor != null && cursor.moveToFirst()) {
                                displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                            }
                        } finally {
                            cursor.close();
                        }
                    } else if (uriString.startsWith("file://")) {
                        displayName = myFile.getName();
                    }
                    uriString = uriString.replaceFirst("file://","");
                    uriString = uriString.replaceAll("/"+displayName,"");
                    filePath = uriString;
                    fileName = displayName;
                    Log.i("Uri ",""+uriString);
                    Log.i("displayName ",""+displayName);
                    Intent intent = new Intent(MainActivity.this, GraphActivity.class);
                    intent.putExtra("filePath",filePath);
                    intent.putExtra("fileName",fileName);
                    startActivity(intent);
                }
                break;
            case 2:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();
                    String uriString = uri.toString();
                    File myFile = new File(uriString);
                    String path = myFile.getAbsolutePath();
                    String displayName = null;

                    if (uriString.startsWith("content://")) {
                        Cursor cursor = null;
                        try {
                            cursor = getContentResolver().query(uri, null, null, null, null);
                            if (cursor != null && cursor.moveToFirst()) {
                                displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                            }
                        } finally {
                            cursor.close();
                        }
                    } else if (uriString.startsWith("file://")) {
                        displayName = myFile.getName();
                    }
                    int index = uriString.indexOf("Music/AudioRecorder");
                    String uriStringsub = uriString.substring(0,index-1);
                    uriString = uriString.replaceFirst(uriStringsub,"");

                    Log.i("uri string2",uriString);
                    filePath = uriString;
                    fileName = displayName;
                    Log.i("Uri ",""+uriString);
                    Log.i("displayName ",""+displayName);
                    ProgressBar  simpleProgressBar= (ProgressBar)findViewById(R.id.simpleProgressBar);
                    simpleProgressBar.setVisibility(View.INVISIBLE);
                    workbook =  new HSSFWorkbook();
                    filterAudio();
                }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void amplitude_energy_data(){

    }

    public void filterAudio(){

        FileInputStream in = null;
        try {
            File file = new File(filePath);
            if(!filePath.contains("/storage/emulated/0")){
                Log.i("getExternalStorage",Environment.getExternalStorageDirectory().toString());
                in = new FileInputStream(Environment.getExternalStorageDirectory().toString()+"/"+file.getAbsolutePath());
            }else{
                in = new FileInputStream(file.getAbsolutePath());
            }
            Log.i("path",""+file.getAbsolutePath()+"/"+fileName);
                /*File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Music");
                in = new FileInputStream(file.getAbsolutePath() + "/kilo5.wav");*/
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        int i = Integer.MAX_VALUE;
        int k = 0;
        while (true) {
            try {
                Log.d("in.available ",""+in.available());
                Log.d(" Number  ",""+k++);
                int j = in.available();
                if(j>minBufferSize*2){
                    byte[] buff = new byte[minBufferSize*2];
                    i = in.read(buff, 0, buff.length);
                    short[] shorts = new short[buff.length/2];
                    // to turn bytes to shorts as either big endian or little endian.
                    ByteBuffer.wrap(buff).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
                    Log.d(" Shorts :   ",""+shorts.length);
                    buffer = shorts;
                    isFilter = true;
                    filtering();
                }else{
                    if(os!=null){
                        try {
                            os.close();
                            isFilter=false;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        os=null;
                        stopRecording();
                    }

                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void recordAudio(){
        minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioEncoding);
        minBufferSize = 2048;
        recorder = new AudioRecord(audioSource, sampleRate, channelConfig, audioEncoding, minBufferSize);
        buffer = new short[minBufferSize];

        if (recorder.getState() == AudioRecord.STATE_UNINITIALIZED) {
            Thread.currentThread().interrupt();
            recorder.release();
            return;
        } else {
            Log.i(MainActivity.class.getSimpleName(), "Started.");
            //callback.onStart();
        }
    }

    public void startRecording(){
        recorder.startRecording();
        RecordingThread recordingThread = new RecordingThread();
        recordingThread.start();
    }





    boolean startThread = true;
    class MFCCThread extends Thread{
        public void run(){
            System.out.println("thread is running...");
            //List of all files and get all MFCC at a time
            File directoryPath = new File("/storage/emulated/0/Music/AudioRecorder");
            //List of all files and directories
            File filesList[] = directoryPath.listFiles();

            String[] samples1={"corner","wall"};
            String[] samples2={"1","2","3","4","5","6"};
            exelRow=1;

            workbook =  new HSSFWorkbook();
            firstSheet = workbook.createSheet("MFCC");

            int a=3;
            int b=3;
            while(a<=11 && b <=11){
                String filterN="";
                File[] matches=null;
                exelRow=1;
                if(a == 11 && b == 11){
                    final String filterName=a+""+b;

                    filterN=filterName;
                    matches = directoryPath.listFiles(new FilenameFilter()
                    {
                        public boolean accept(File dir, String name)
                        {
                            return name.contains(filterName);
                        }
                    });
                }else if(b==11){
                    final String filterName="0"+a+""+b;
                    filterN=filterName;
                    matches = directoryPath.listFiles(new FilenameFilter()
                    {
                        public boolean accept(File dir, String name)
                        {
                            return name.contains(filterName);
                        }
                    });
                } else if (a == 11) {
                    final String filterName=a+"0"+b;
                    filterN=filterName;
                    matches = directoryPath.listFiles(new FilenameFilter()
                    {
                        public boolean accept(File dir, String name)
                        {
                            return name.contains(filterName);
                        }
                    });
                } else {
                    final String filterName = "0" + a + "0" + b;
                    filterN = filterName;
                    matches = directoryPath.listFiles(new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            return name.contains(filterName);
                        }
                    });
                }

                Log.i(filterN+" files : ",Arrays.toString(matches));
                if(matches.length==0){
                    b=b+2;
                    if(b>11){
                        a=a+2;
                        b=3;
                    }
                    continue;
                }

                for(File file : matches) {
                    if(file.getAbsolutePath().contains(".wav")){
                        System.out.println("File path: "+file.getAbsolutePath());
                        filePath=file.getAbsolutePath();
                        mfcc();
                        FileInputStream in = null;
                        try {
                            in = new FileInputStream(file.getAbsolutePath());
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }

                    }
                }
                b=b+2;
                if(b>11){
                    a=a+2;
                    b=3;
                }
                createExel(workbook,filePath,filterN);
            }






        }
    }
    class RecordingThread extends Thread{
        int count=1;
        public void run(){

       //For writing into file from audio
            while(startThread){
                Log.i("count : ",""+count);
                count++;
                long startTime = System.nanoTime();

                int bufferReadResult = recorder.read(buffer, 0, minBufferSize); // record data from mic into buffer

                if (bufferReadResult > 0) {
                    writeAudioDataToFile(convertShortArray());
                }
                long endTime = System.nanoTime();
                long timeElapsed = endTime - startTime;
                //Log.i("Execution time : ",""+timeElapsed / 1000000);

                Log.i("timeElapsed : ",""+timeElapsed/1000000);

            }
            if(!startThread){
                if(os!=null){
                    try {
                        os.close();
                        os=null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    recorder.stop();
                    recorder.release();
                    stopRecording();
                    //data();
                }
            }
            //for scripting exel file directly from record without saving into storage
            /*int bufferReadResult = recorder.read(buffer, 0, minBufferSize); // record data from mic into buffer

            if (bufferReadResult > 0) {
                filtering();
            }*/

        }

    }

    public void stopRecord(){
        startThread = false;
    }


    public void addFeaturesToExel(){
        exelRow=0;
        stopWhile=true;
        Log.i("sii","aasd");
        FileInputStream in = null;
        try {
            File file = new File(filePath);

            in = new FileInputStream(file.getAbsolutePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        int i = Integer.MAX_VALUE;
        HSSFWorkbook workbook1 =  new HSSFWorkbook();
        HSSFSheet sheet = workbook1.createSheet("Time_Domain");
        double totalEnergy=0;
        while (stopWhile) {
            try {
                Log.d("in.available ",""+in.available());
                int j = in.available();
                if(j>minBufferSize*2 || (fileName.equals("kilo5trim0dot01s.wav") && j>0)){
                    byte[] buff = new byte[minBufferSize*2];
                    //minBufferSize = buff.length/2;
                    i = in.read(buff, 0, buff.length);
                    short[] shorts = new short[buff.length/2];
                    // to turn bytes to shorts as either big endian or little endian.
                    ByteBuffer.wrap(buff).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
                    Log.d(" Shorts :   ",""+shorts.length);
                    buffer = shorts;


                    calculate();


                    totalEnergy = totalEnergy+frame_Energy;
                }else{
                    HSSFRow row6 = firstSheet.createRow(exelRow);
                    row6.createCell(0).setCellValue("Total_Energy");
                    row6.createCell(1).setCellValue(totalEnergy);
                    exelRow++;
                    createExel(workbook,filePath,"timeDomain_"+fileName);
                    stopWhile=false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void createExel(HSSFWorkbook workbook,String filePath,String fileName){
        FileOutputStream fos = null;
        try {
            File file=null;
            Log.i("create exe file ",""+filePath);
            if(!filePath.contains("/storage/emulated/0")){
                file = new File(Environment.getExternalStorageDirectory().toString()+"/"+filePath.replaceAll(".wav", "")+".xls");
            }else{
                file = new File("/storage/emulated/0/Music/AudioRecorder/"+fileName+".xls");
            }

            if(file.exists()){
                file.delete();
            }
            fos = new FileOutputStream(file);
            workbook.write(fos);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.flush();
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public void amplitude_time_excel(){
        workbook =  new HSSFWorkbook();
        firstSheet = workbook.createSheet("Amplitude_time_excel");
        exelRow=0;
        stopWhile=true;
        Log.i("amplitude_time_excel","aasd");
        FileInputStream in = null;
        try {
            File file = new File(filePath);
            if(!filePath.contains("/storage/emulated/0")){
                Log.i("getExternalStorage",Environment.getExternalStorageDirectory().toString());
                in = new FileInputStream(Environment.getExternalStorageDirectory().toString()+"/"+file.getAbsolutePath());
            }else{
                in = new FileInputStream(file.getAbsolutePath());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        int i = Integer.MAX_VALUE;
        double totalEnergy=0;
        while (stopWhile) {
            try {
                Log.d("in.available ",""+in.available());
                int j = in.available();
                if(j>minBufferSize*2){
                    byte[] buff = new byte[minBufferSize*2];
                    //minBufferSize = buff.length/2;
                    i = in.read(buff, 0, buff.length);
                    short[] shorts = new short[buff.length/2];
                    // to turn bytes to shorts as either big endian or little endian.
                    ByteBuffer.wrap(buff).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
                    Log.d(" Shorts :   ",""+shorts.length);
                    buffer = shorts;
                    for(int k=0;k<buffer.length;k++){
                        HSSFRow row = firstSheet.createRow(exelRow);
                        row.createCell(0).setCellValue(exelRow);
                        row.createCell(1).setCellValue(buffer[k]);
                        exelRow++;
                    }
                }else{

                    createExel(workbook,filePath,"timeDomain_"+fileName);
                    stopWhile=false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void frameEnergy_frame_excel(){
        workbook =  new HSSFWorkbook();
        firstSheet = workbook.createSheet("Amplitude_time_excel");
        exelRow=0;
        stopWhile=true;
        Log.i("amplitude_time_excel","aasd");
        FileInputStream in = null;
        try {
            File file = new File(filePath);
            if(!filePath.contains("/storage/emulated/0")){
                Log.i("getExternalStorage",Environment.getExternalStorageDirectory().toString());
                in = new FileInputStream(Environment.getExternalStorageDirectory().toString()+"/"+file.getAbsolutePath());
            }else{
                in = new FileInputStream(file.getAbsolutePath());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        int i = Integer.MAX_VALUE;
        double totalEnergy=0;
        while (stopWhile) {
            try {
                Log.d("in.available ",""+in.available());
                int j = in.available();
                if(j>minBufferSize*2){
                    byte[] buff = new byte[minBufferSize*2];
                    //minBufferSize = buff.length/2;
                    i = in.read(buff, 0, buff.length);
                    short[] shorts = new short[buff.length/2];
                    // to turn bytes to shorts as either big endian or little endian.
                    ByteBuffer.wrap(buff).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
                    Log.d(" Shorts :   ",""+shorts.length);
                    buffer = shorts;
                    frameEnergyCalculate();
                }else{

                    createExel(workbook,filePath,"timeDomain_"+fileName);
                    stopWhile=false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void prepare(){
        exelRow=0;
        stopWhile=true;
        Log.i("sii","aasd");
        FileInputStream in = null;
        try {
            File file = new File(filePath);
            Log.i("file.getAbsolutePath()",Environment.getExternalStorageDirectory().toString()+"/"+file.getAbsolutePath());
            in = new FileInputStream(Environment.getExternalStorageDirectory().toString()+"/"+file.getAbsolutePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        int i = Integer.MAX_VALUE;
        int k = 0;
        if(fileName.equals("kilo5trim0dot01s.wav")){
            firstSheet = workbook.createSheet("Data_Original_Sheet");
        }else{
            firstSheet = workbook.createSheet("Data_Sheet");
        }
        int descreteTime=0;
        HSSFWorkbook workbook1 =  new HSSFWorkbook();
        HSSFSheet sheet = workbook1.createSheet("Time_Domain");
        double totalEnergy=0;
        while (stopWhile) {
            try {
                Log.d("in.available ",""+in.available());
                if(k==0){
                    float duration = (float)in.available()/(2*sampleRate);
                    int timeFrames = in.available()/(2*minBufferSize);
                    if(timeFrames==0){
                        timeFrames=1;
                    }
                    float timeFramesDuration = (float)(duration/timeFrames);

                    HSSFRow row = firstSheet.createRow(exelRow);
                    HSSFCell cell = row.createCell(0);
                    cell.setCellValue("minBufferSize");
                    row.createCell(1).setCellValue(minBufferSize);
                    exelRow++;
                    HSSFRow row1 = firstSheet.createRow(exelRow);
                    HSSFCell cellA = row1.createCell(0);
                    cellA.setCellValue("duration in sec");
                    row1.createCell(1).setCellValue(duration);;
                    exelRow++;
                    HSSFRow row2 = firstSheet.createRow(exelRow);
                    HSSFCell cellB = row2.createCell(0);
                    cellB.setCellValue("timeFrames");
                    row2.createCell(1).setCellValue(timeFrames);
                    exelRow++;
                    HSSFRow row3 = firstSheet.createRow(exelRow);
                    HSSFCell cellC = row3.createCell(0);
                    cellC.setCellValue("timeFramesDuration in sec");
                    row3.createCell(1).setCellValue(timeFramesDuration);
                    exelRow++;

                    Log.i("duration",""+duration);
                    Log.i("timeFrames",""+timeFrames);
                    Log.i("timeFramesDuration",""+timeFramesDuration);

                }
                Log.d(" Number  ",""+k++);

                int j = in.available();
                if(j>minBufferSize*2 || (fileName.equals("kilo5trim0dot01s.wav") && j>0)){
                    byte[] buff = new byte[minBufferSize*2];
                    //minBufferSize = buff.length/2;
                    i = in.read(buff, 0, buff.length);
                    short[] shorts = new short[buff.length/2];
                    // to turn bytes to shorts as either big endian or little endian.
                    ByteBuffer.wrap(buff).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
                    Log.d(" Shorts :   ",""+shorts.length);
                    buffer = shorts;
                    HSSFRow row3 = firstSheet.createRow(exelRow);
                    HSSFCell cellC = row3.createCell(0);
                    cellC.setCellValue("Time_Frame_"+k);
                    exelRow++;
                    HSSFRow row4 = firstSheet.createRow(exelRow);
                    row4.createCell(0).setCellValue("Frequency");
                    row4.createCell(1).setCellValue("Amplitude");
                    exelRow++;

                    calculate();

                    HSSFRow row5 = firstSheet.createRow(exelRow);
                    row5.createCell(0).setCellValue("frame_Energy");
                    row5.createCell(1).setCellValue(frame_Energy);
                    exelRow++;
                    totalEnergy = totalEnergy+frame_Energy;
                }else{
                    HSSFRow row6 = firstSheet.createRow(exelRow);
                    row6.createCell(0).setCellValue("Total_Energy");
                    row6.createCell(1).setCellValue(totalEnergy);
                    exelRow++;
                    createExel(workbook,filePath,"timeDomain_"+fileName);
                    stopWhile=false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void mfcc(){
        int sampleRate = 44100;
        int bufferSize = 512;
        int bufferOverlap = 128;
        //new AndroidFFMPEGLocator(this);
        final List<float[]> mfccList = new ArrayList<>(200);
        InputStream inStream = null;
        try {
            if(!filePath.contains("/storage/emulated/0")){
                File file = new File(filePath);
                Log.i("getExternalStorage",Environment.getExternalStorageDirectory().toString());
                inStream = new FileInputStream(Environment.getExternalStorageDirectory().toString()+"/"+file.getAbsolutePath());
            }
            inStream = new FileInputStream(filePath);

            //inStream = new FileInputStream(filePath+"/"+fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        AudioDispatcher dispatcher = new AudioDispatcher(new UniversalAudioInputStream(inStream, new TarsosDSPAudioFormat(sampleRate, bufferSize, 1, true, true)), bufferSize, bufferOverlap);
        final MFCC mfcc = new MFCC(bufferSize, sampleRate, 20, 50, 4500, 5500);
        dispatcher.addAudioProcessor(mfcc);
        dispatcher.addAudioProcessor(new AudioProcessor() {

            @Override
            public void processingFinished() {
            }

            @Override
            public boolean process(AudioEvent audioEvent) {
                mfccList.add( mfcc.getMFCC());
                return true;
            }
        });
        dispatcher.run();
        Log.i("mfccList",mfccList.toString());

        HSSFRow row = firstSheet.createRow(exelRow);
        String filename = filePath.replaceAll("/storage/emulated/0/Music/AudioRecorder/","");
        row.createCell(0).setCellValue(filename);
        exelCol=1;
        for(int i=0;i<mfccList.size();i++){
            if(i==2){
                Log.i("mfcc "+i,Arrays.toString(mfccList.get(i)));
                for(int a=0;a<mfccList.get(i).length;a++){
                    row.createCell(exelCol).setCellValue((mfccList.get(i))[a]);
                    exelCol++;
                }
            }
        }
        addFeaturesToExel();
        if(filePath.contains("corner")){
            row.createCell(exelCol).setCellValue("1");
            exelCol++;
        }else{
            row.createCell(exelCol).setCellValue("0");
            exelCol++;
        }
        exelRow++;

    }
    double frame_Energy = 0;
    public byte[] calculate() {
        ArrayList<Double> magnitudeArray = new ArrayList<Double>();
        ArrayList<Float> frequencyArray = new ArrayList<Float>();
        double[] magnitude = new double[minBufferSize];
        //Create Complex array for use in FFT
        Complex[] fftTempArray = new Complex[minBufferSize];
        for (int i = 0; i < minBufferSize; i++) {
            float frequency = (float)i * 44100F / (float)minBufferSize;
            fftTempArray[i] = new Complex(buffer[i], 0);

        }
        Log.e("fft length : ",""+fftTempArray.length);
        //Obtain array of FFT data
        final Complex[] fftArray = FFT.fft(fftTempArray);
        // calculate power spectrum (magnitude) values from fft[]
        float freqMin1 = 4500;
        float freqMin2 = 5500;

        float freqMax1 = sampleRate-freqMin2;
        float freqMax2 = sampleRate-freqMin1;

        originalAngleMap = new LinkedHashMap<Float, Double>();
        AngleMap = new LinkedHashMap<Float, Double>();



        frame_Energy = 0;
        for (int i = 0; i < minBufferSize - 1; ++i) {
            float frequency = (float)i * 44100F / (float)minBufferSize;
            try {
                if (frequency > 4900 && frequency < 5100) {

                    double angle = Math.atan(fftArray[i].im() / fftArray[i].re()) * (180 / Math.PI);
                    if (isOriginalFile) {
                        originalAngleMap.put(frequency, angle);

                    } else {
                        AngleMap.put(frequency, angle);

                    }
                }
            }
            catch(Exception e){Log.i("Eroor ",e+"");}


            double real = fftArray[i].re();
            double imaginary = fftArray[i].im();
            magnitude[i] = Math.sqrt(real * real + imaginary * imaginary);
            frequencyArray.add(frequency);
            magnitudeArray.add(magnitude[i]);
            HSSFRow rowA = firstSheet.createRow(exelRow);
            if(frequency>4800 && frequency<5200){
                Log.e("Magnitude", "" + magnitude[i]);

                Log.e("Freq", "" + frequency    );
                HSSFCell cellA = rowA.createCell(0);
                cellA.setCellValue(frequency);
                HSSFCell cellB = rowA.createCell(1);
                cellB.setCellValue(magnitude[i]);
                exelRow++;
                frame_Energy = frame_Energy+magnitude[i]*magnitude[i];
            }


        }

        if(!isOriginalFile){
            magnitudeArrayArray.add(magnitudeArray);
            frequencyArrayArray.add(frequencyArray);
            AngleMapArray.add(AngleMap);
        }else{
            originalAngleMapArray.add(originalAngleMap);
        }

            /*graphView.getViewport().setXAxisBoundsManual(true);
            graphView.getViewport().setMinX(0);
            graphView.getViewport().setMaxX(44100);*/

        // find largest peak in power spectrum
        double max_magnitude = magnitude[0];
        int max_index = 0;
            /*for (int i = 0; i < magnitude.length; ++i) {
                if (magnitude[i] > max_magnitude) {
                    max_magnitude = (int) magnitude[i];
                    max_index = i;
                }
            }
            double freq = 44100 * max_index / minBufferSize;//here will get frequency in hz like(17000,18000..etc)
            Log.e("Freq", "" + freq);
            Log.e("Magnitude", "" + magnitude[max_index]);*/

            /*Complex[] invfftTempArray = FFT.ifft(fftArray);
            Log.e("fftArray size",""+fftArray.length);
            Log.e("invfftTempArray size",""+invfftTempArray.length);*/
            /*short[] invbuffer = new short[minBufferSize];

            for (int i = 0; i < minBufferSize; i++) {
                invbuffer[i] = (short) invfftTempArray[i].re();
            }
            Log.e("invbuffer ",""+invbuffer.length);

            ByteBuffer bb = ByteBuffer.allocate(minBufferSize * 2);

            for(int index = 0; index != minBufferSize; ++index)
            {
                bb.putShort(invbuffer[index]);
            }
            byte[] byteArray = bb.array();

            byte [] buffer = new byte[invbuffer.length * 2];

            int short_index=0,byte_index = 0;

            for(NOP; short_index != minBufferSize; NOP)
            {
                buffer[byte_index]     = (byte) (invbuffer[short_index] & 0x00FF);
                buffer[byte_index + 1] = (byte) ((invbuffer[short_index] & 0xFF00) >> 8);

                ++short_index; byte_index += 2;
            }

            Log.e("byteArray ",""+buffer.length);

            writeAudioDataToFile(buffer);*/
        return null;
    }

    public byte[] frameEnergyCalculate() {
        ArrayList<Double> magnitudeArray = new ArrayList<Double>();
        ArrayList<Float> frequencyArray = new ArrayList<Float>();
        double[] magnitude = new double[minBufferSize];
        //Create Complex array for use in FFT
        Complex[] fftTempArray = new Complex[minBufferSize];
        for (int i = 0; i < minBufferSize; i++) {
            float frequency = (float)i * 44100F / (float)minBufferSize;
            fftTempArray[i] = new Complex(buffer[i], 0);

        }
        Log.e("fft length : ",""+fftTempArray.length);
        //Obtain array of FFT data
        final Complex[] fftArray = FFT.fft(fftTempArray);
        // calculate power spectrum (magnitude) values from fft[]
        float freqMin1 = 4500;
        float freqMin2 = 5500;

        float freqMax1 = sampleRate-freqMin2;
        float freqMax2 = sampleRate-freqMin1;

        originalAngleMap = new LinkedHashMap<Float, Double>();
        AngleMap = new LinkedHashMap<Float, Double>();



        frame_Energy = 0;
        HSSFRow rowA = firstSheet.createRow(exelRow);

        for (int i = 0; i < minBufferSize - 1; ++i) {
            float frequency = (float)i * 44100F / (float)minBufferSize;
            try {
                if (frequency > 4900 && frequency < 5100) {

                    double angle = Math.atan(fftArray[i].im() / fftArray[i].re()) * (180 / Math.PI);
                    if (isOriginalFile) {
                        originalAngleMap.put(frequency, angle);

                    } else {
                        AngleMap.put(frequency, angle);

                    }
                }
            }
            catch(Exception e){Log.i("Eroor ",e+"");}


            double real = fftArray[i].re();
            double imaginary = fftArray[i].im();
            magnitude[i] = Math.sqrt(real * real + imaginary * imaginary);
            frequencyArray.add(frequency);
            magnitudeArray.add(magnitude[i]);
            if(frequency>4800 && frequency<5200){
                Log.e("Magnitude", "" + magnitude[i]);

                Log.e("Freq", "" + frequency    );

                frame_Energy = frame_Energy+magnitude[i]*magnitude[i];
            }
        }
        HSSFCell cellA = rowA.createCell(0);
        cellA.setCellValue(exelRow);
        HSSFCell cellB = rowA.createCell(1);
        cellB.setCellValue(frame_Energy);
        exelRow++;

        if(!isOriginalFile){
            magnitudeArrayArray.add(magnitudeArray);
            frequencyArrayArray.add(frequencyArray);
            AngleMapArray.add(AngleMap);
        }else{
            originalAngleMapArray.add(originalAngleMap);
        }
        return null;
    }

    public void trimFile(){
        String inputName = "kilo5trim0dot01s.wav";
        stopWhile=true;
        Log.i("sii","aasd");
        FileInputStream in = null;
        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                    +"/"+ AUDIO_RECORDER_FOLDER);
            in = new FileInputStream(file.getAbsolutePath() + "/"+inputName);
                /*File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Music");
                in = new FileInputStream(file.getAbsolutePath() + "/kilo5.wav");*/
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        int i = Integer.MAX_VALUE;
        int k = 0;
        while (stopWhile) {
            try {
                Log.d("in.available ",""+in.available());
                Log.d(" Number  ",""+k++);
                int j = in.available();
                if(j>4096 && k<=1){
                    byte[] buff = new byte[4096];
                    minBufferSize = buff.length/2;
                    i = in.read(buff, 0, buff.length);
                    writeAudioDataToFile(buff);

                }else{
                        if(os!=null){
                            os.close();
                            stopRecording();
                        }

                    stopWhile=false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    public byte[] filtering() {
        double[] magnitude = new double[minBufferSize];
        //Create Complex array for use in FFT
        Complex[] fftTempArray = new Complex[minBufferSize];
        for (int i = 0; i < minBufferSize; i++) {
            float frequency = (float)i * 44100F / (float)minBufferSize;
            fftTempArray[i] = new Complex(buffer[i], 0);

        }
        //Obtain array of FFT data
        final Complex[] fftArray = FFT.fft(fftTempArray);
        // calculate power spectrum (magnitude) values from fft[]
        float freqMin1 = 4800;
        float freqMin2 = 5200;

        float freqMax1 = sampleRate-freqMin2;
        float freqMax2 = sampleRate-freqMin1;


        for (int i = 0; i < minBufferSize - 1; ++i) {
            float frequency = (float)i * 44100F / (float)minBufferSize;

            if(isFilter){
                if((frequency  < freqMin1 || (frequency > freqMin2 && frequency < freqMax1) || frequency > freqMax2)){
                    fftArray[i] = new Complex(0,0);
                }
            }


            double real = fftArray[i].re();
            double imaginary = fftArray[i].im();
            magnitude[i] = Math.sqrt(real * real + imaginary * imaginary);

        }


        Complex[] invfftTempArray = FFT.ifft(fftArray);
        short[] invbuffer = new short[minBufferSize];

        for (int i = 0; i < minBufferSize; i++) {
            invbuffer[i] = (short) invfftTempArray[i].re();
        }

        ByteBuffer bb = ByteBuffer.allocate(minBufferSize * 2);

        for(int index = 0; index != minBufferSize; ++index)
        {
            bb.putShort(invbuffer[index]);
        }
        byte[] byteArray = bb.array();

        byte [] buffer = new byte[invbuffer.length * 2];

        int short_index=0,byte_index = 0;

        while(short_index != minBufferSize)
        {
            buffer[byte_index]     = (byte) (invbuffer[short_index] & 0x00FF);
            buffer[byte_index + 1] = (byte) ((invbuffer[short_index] & 0xFF00) >> 8);

            ++short_index; byte_index += 2;
        }
        writeAudioDataToFile(buffer);
        return buffer;
    }

    public byte[] convertShortArray(){
        byte [] bufferNew = new byte[buffer.length*2];

        int short_index=0,byte_index = 0;

        for(/*NOP*/; short_index != minBufferSize; /*NOP*/)
        {
            bufferNew[byte_index]     = (byte) (buffer[short_index] & 0x00FF);
            bufferNew[byte_index + 1] = (byte) ((buffer[short_index] & 0xFF00) >> 8);

            ++short_index; byte_index += 2;
        }
        return bufferNew;
    }

    private String getFilename(){
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                +"/"+ AUDIO_RECORDER_FOLDER);
        //File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        return (file.getAbsolutePath() + "/" + System.currentTimeMillis()+"demo" + AUDIO_RECORDER_FILE_EXT_WAV);
    }



    private String getTempFilename(){
        //String filepath = Environment.getExternalStorageDirectory().getPath();
        file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                +"/"+ AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        File tempFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                +"/"+ AUDIO_RECORDER_FOLDER+"/"+AUDIO_RECORDER_TEMP_FILE);

        if(tempFile.exists()){
            tempFile.delete();
        }
        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }


    private void writeAudioDataToFile(byte[] data){
        try {
            if(os==null){
                os = new FileOutputStream(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                        +"/"+ AUDIO_RECORDER_FOLDER + "/" + AUDIO_RECORDER_TEMP_FILE);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        if(null != os){

            try {
                os.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }


        }



    }

    public File extractLogToFileAndWeb(){
        //set a file
        String fullName = System.currentTimeMillis()+"appLog.log";
        File file = new File(filePath+"/"+fullName);

        //clears a file
        if(file.exists()){
            file.delete();
        }


        //write log to file
        int pid = android.os.Process.myPid();
        try {
            String command = String.format("logcat -d -v threadtime *:*");
            Process process = Runtime.getRuntime().exec(command);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder result = new StringBuilder();
            String currentLine = null;

            while ((currentLine = reader.readLine()) != null) {
                if (currentLine != null && currentLine.contains(String.valueOf(pid))) {
                    result.append(currentLine);
                    result.append("\n");
                }
            }

            FileWriter out = new FileWriter(file);
            out.write(result.toString());
            out.close();

            //Runtime.getRuntime().exec("logcat -d -v time -f "+file.getAbsolutePath());
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
        }


        //clear the log
        try {
            Runtime.getRuntime().exec("logcat -c");
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
        }

        return file;
    }

    private void stopRecording(){
        Log.i("inside ","stopRecording");
        copyWaveFile(file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE,getFilename());
        deleteTempFile();
    }

    private void deleteTempFile() {
        File files = new File(file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);

        files.delete();
    }

    private void copyWaveFile(String inFilename,String outFilename){
        Log.i("inside ","copyWaveFile");

        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 1;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;

        byte[] data = new byte[minBufferSize*2];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            //AppLog.logString("File size: " + totalDataLen);

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,longSampleRate, channels, byteRate);

            while(in.read(data) != -1){
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i("Task :","Completed");
    }

    private void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = RECORDER_BPP; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }













}



