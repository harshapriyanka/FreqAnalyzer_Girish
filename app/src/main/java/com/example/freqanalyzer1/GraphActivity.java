package com.example.freqanalyzer1;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;


public class GraphActivity extends AppCompatActivity {
    GraphView graphView;
    String mFileName;
    LineGraphSeries<DataPoint> series;
    LineGraphSeries<DataPoint> phaseSeries;
    LinkedHashMap<Float,Double> AngleMap;
    LinkedHashMap<Float,Double> originalAngleMap;
    LinkedHashMap<Float,Double> phaseDifMap;
    static boolean isOriginalFile = false;
    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    FileOutputStream os = null;
    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            +"/"+ AUDIO_RECORDER_FOLDER);
    Recording recording = new Recording();
    String filePath = "";
    String fileName = "";
    boolean stopWhile=true;
    ArrayList<ArrayList<Float>> frequencyArrayArray = new ArrayList<ArrayList<Float>>();
    ArrayList<ArrayList<Double>> magnitudeArrayArray = new ArrayList<ArrayList<Double>>();
    ArrayList<Map<Float,Double>> originalAngleMapArray = new ArrayList<>();
    ArrayList<Map<Float,Double>> AngleMapArray = new ArrayList<>();
    Switch phaseSwitch;
    HSSFWorkbook workbook = null;
    HSSFSheet firstSheet = null;
    int exelRow=0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        long start = System.currentTimeMillis();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        Intent intent = getIntent();
        // initiate a Switch
        phaseSwitch = (Switch) findViewById(R.id.phaseSwitch);

        filePath = intent.getStringExtra("filePath");
        fileName = intent.getStringExtra("fileName");

        //graphView = (GraphView)findViewById(R.id.graphy);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        graphView = (GraphView)findViewById(R.id.graphy);
        /*graphView.getViewport().setMaxXAxisSize(20000);
        graphView.getViewport().setMaxYAxisSize(800000);*/
        boolean isRecording = true;
        stopThread=true;
        frequencyArrayArray = new ArrayList<ArrayList<Float>>();
        magnitudeArrayArray = new ArrayList<ArrayList<Double>>();
        workbook =  new HSSFWorkbook();
        isOriginalFile = false;
        prepare();
        /*fileName = "kilo5trim0dot01s.wav";
        isOriginalFile = true;
        prepare();*/
        createExel(workbook,filePath,fileName);
        //extractLogToFileAndWeb();
        long time = System.currentTimeMillis() - start;
        Log.i("Time Delay : ",""+time);
    }

    public void createExel(HSSFWorkbook workbook,String filePath,String fileName){
        FileOutputStream fos = null;
        try {
            File file = new File(filePath+"/"+fileName.replaceAll(".wav", "")+".xls");
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

    public void prepare(){
        exelRow=0;
        stopWhile=true;
        Log.i("sii","aasd");
            FileInputStream in = null;
            try {
                File file = new File(filePath);
                in = new FileInputStream(file.getAbsolutePath() + "/"+fileName);

                /*File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Music");
                in = new FileInputStream(file.getAbsolutePath() + "/kilo5.wav");*/
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
                        cell.setCellValue(minBufferSize);
                        exelRow++;
                        HSSFRow row1 = firstSheet.createRow(exelRow);
                        HSSFCell cellA = row1.createCell(0);
                        cellA.setCellValue(duration);
                        exelRow++;
                        HSSFRow row2 = firstSheet.createRow(exelRow);
                        HSSFCell cellB = row2.createCell(0);
                        cellB.setCellValue(timeFrames);
                        exelRow++;
                        HSSFRow row3 = firstSheet.createRow(exelRow);
                        HSSFCell cellC = row3.createCell(0);
                        cellC.setCellValue(timeFramesDuration);
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
                        for(int l=0;l<shorts.length;l++){
                            HSSFRow row1 = sheet.createRow(descreteTime+1);
                            HSSFCell cellA = row1.createCell(0);
                            cellA.setCellValue(descreteTime*1000/sampleRate);
                            HSSFCell cellB = row1.createCell(1);
                            cellB.setCellValue(shorts[l]);
                            descreteTime++;
                        }
                        recording.calculate();
                        /*if(null != os){

                            try {
                                os.write(data);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }


                        }*/
                    }else{
                        /*if(os!=null){
                            os.close();
                            recording.stopRecording();
                        }*/

                        if(isOriginalFile){
                            if(false){
                                File tempFile = new File(file.getAbsolutePath()+"/phase.txt");
                                if(tempFile.exists()){
                                    tempFile.delete();
                                }
                                FileWriter fw1=new FileWriter(file.getAbsolutePath()+"/phase.txt");
                                fw1.write(AngleMapArray.toString());
                                fw1.close();

                                tempFile = new File(file.getAbsolutePath()+"/phaseOriginal.txt");
                                if(tempFile.exists()){
                                    tempFile.delete();
                                }
                                fw1=new FileWriter(file.getAbsolutePath()+"/phaseOriginal.txt");
                                fw1.write(originalAngleMapArray.toString());
                                fw1.close();

                                final ArrayList<Map<Float,Double>> phaseDifMapArray = new ArrayList<>();
                                for(int a=0;a< AngleMapArray.size();a++){
                                    phaseDifMap = new LinkedHashMap<Float, Double>();
                                    for(Map.Entry<Float,Double> entry : AngleMap.entrySet()){
                                        phaseDifMap.put(entry.getKey(),entry.getValue()-originalAngleMapArray.get(a).get(entry.getKey()));
                                    }
                                    phaseDifMapArray.add(phaseDifMap);
                                }
                                final ArrayList<LineGraphSeries<DataPoint>> phaseSeriesArray = new ArrayList<>();
                                for(int c=0;c<phaseDifMapArray.size();c++){
                                    phaseSeries = new LineGraphSeries<DataPoint>();
                                    for(Map.Entry<Float,Double> entry : phaseDifMapArray.get(c).entrySet()){
                                        phaseSeries.appendData(new DataPoint(entry.getKey()+(c*0.0001),entry.getValue()+(c*0.0001)),true,minBufferSize);
                                    }
                                    phaseSeriesArray.add(phaseSeries);
                                }

                                tempFile = new File(file.getAbsolutePath()+"/phaseDiff.txt");

                                if(tempFile.exists()){
                                    tempFile.delete();
                                }
                                FileWriter fw=new FileWriter(file.getAbsolutePath()+"/phaseDiff.txt");

                                fw.write(phaseDifMapArray.toString());
                                fw.close();



                                Log.i("Task","Completed");
                            }
                        }else{
                            double[] sumMagnitude=new double[magnitudeArrayArray.get(0).size()];
                            double[] avgMagnitude=new double[magnitudeArrayArray.get(0).size()];
                            float sumFrequency=0;
                            int size = magnitudeArrayArray.size();

                            for(int a=0;a<magnitudeArrayArray.size();a++){
                                for(int b=0;b<((ArrayList<Double>)(magnitudeArrayArray.get(a))).size();b++){
                                    sumMagnitude[b] = sumMagnitude[b]+((ArrayList<Double>)(magnitudeArrayArray.get(a))).get(b);
                                }
                            }
                            graphView.removeSeries(series);
                            series = new LineGraphSeries<DataPoint>();
                            for(int c=0;c<sumMagnitude.length;c++){
                                avgMagnitude[c] = avgMagnitude[c]+sumMagnitude[c]/size;
                                series.appendData(new DataPoint(frequencyArrayArray.get(0).get(c),avgMagnitude[c]),true,minBufferSize);
                            }
                            graphView.addSeries(series);
                            double max_magnitude = 0;
                            for (int m = 0; m < avgMagnitude.length; m++) {
                                if (avgMagnitude[m] > max_magnitude) {
                                    max_magnitude =  avgMagnitude[m];
                                }
                            }
                            Log.i("avgMagnitude : ", Arrays.toString(avgMagnitude)+"");
                            CharSequence text = "max_magnitude : "+max_magnitude;
                            int duration = Toast.LENGTH_LONG;

                            Toast toast = Toast.makeText(getApplicationContext(), text, duration);
                            toast.show();

                        }
                        stopWhile=false;
                        createExel(workbook1,filePath,"timeDomain_"+fileName);

                        // for finding timeFrame in which maximum magnitude occurs
                        /*ArrayList<Integer> freqIndexArray = new ArrayList<Integer>();
                        for(int freqIndex = 0;freqIndex<frequencyArrayArray.get(0).size();freqIndex++){
                            if(frequencyArrayArray.get(0).get(freqIndex)>4500 && frequencyArrayArray.get(0).get(freqIndex)<5500){
                                freqIndexArray.add(freqIndex);
                            }
                        }
                        double maxMagnitude = 0;
                        for(int timeFrame=0;timeFrame<magnitudeArrayArray.size();timeFrame++){
                            for(int a=0;a<freqIndexArray.size();a++){

                            }
                        }*/



                    }
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
    int minBufferSize = 512;
    private int audioSource = MediaRecorder.AudioSource.MIC;
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private int sampleRate = 44100;
    short[] buffer;
    boolean isFilter = true;
    //int count  =1;
    boolean stopThread = true;
    class Recording extends Thread {
        @Override
        public void run() {

            while (stopThread) {
                //count++;
                /*minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioEncoding);
                minBufferSize = 2048;*/
                AudioRecord recorder = new AudioRecord(audioSource, sampleRate, channelConfig, audioEncoding, minBufferSize);
                buffer = new short[minBufferSize];

                if (recorder.getState() == AudioRecord.STATE_UNINITIALIZED) {
                    Thread.currentThread().interrupt();
                    recorder.release();
                    return;
                } else {
                    Log.i(MainActivity.class.getSimpleName(), "Started.");
                    //callback.onStart();
                }
                recorder.startRecording();
                int bufferReadResult = recorder.read(buffer, 0, minBufferSize); // record data from mic into buffer

                if (bufferReadResult > 0) {
                    calculate();
                    //writeAudioDataToFile(convertShortArray());
                }
                recorder.stop();
                recorder.release();
                /*if(count > 400){
                    stopThread = false;
                    if(os!=null){
                        try {
                            os.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        stopRecording();
                    }

                    break;
                }*/
            }
        }







        /*public void calculate2(){
            graphView.removeSeries(series);
            series = new LineGraphSeries<DataPoint>();

            for (int i = 0; i < minBufferSize - 1; ++i) {
                int y = buffer[i];
                series.appendData(new DataPoint(i,y),true,minBufferSize);
//                fftBuffer[i] = (double)((buffer[2*i] & 0xFF) | (buffer[2*i+1] << 8)) / 32768.0F;
//                complexSignal[i] = new Complex(temp,0.0);
            }
            graphView.addSeries(series);
        }*/

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




            for (int i = 0; i < minBufferSize - 1; ++i) {
                float frequency = (float)i * 44100F / (float)minBufferSize;
                /*if(isFilter){


                    if((frequency  < freqMin1 || (frequency > freqMin2 && frequency < freqMax1) || frequency > freqMax2)){

                        Log.e("Freq made zero ", "" + frequency    );

                        //zero out this frequency
                        fftArray[i] = new Complex(0,0);
                    }
                }*/
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

            return (file.getAbsolutePath() + "/" + System.currentTimeMillis() + AUDIO_RECORDER_FILE_EXT_WAV);
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

        /*private void startRecording(){
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    RECORDER_SAMPLERATE, RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING, bufferSize);

            int i = recorder.getState();
            if(i==1)
                recorder.startRecording();

            isRecording = true;

            recordingThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    writeAudioDataToFile();
                }
            },"AudioRecorder Thread");

            recordingThread.start();
        }*/

        private void writeAudioDataToFile(byte[] data){
            //getTempFilename();
            try {
                if(os==null){
                    os = new FileOutputStream(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                            +"/"+ AUDIO_RECORDER_FOLDER + "/" + AUDIO_RECORDER_TEMP_FILE);
                }
            } catch (FileNotFoundException e) {
// TODO Auto-generated catch block
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

        private void stopRecording(){
            Log.i("inside ","stopRecording");
           /* if(null != recorder){
                isRecording = false;

                int i = recorder.getState();
                if(i==1)
                    recorder.stop();
                recorder.release();

                recorder = null;
                recordingThread = null;
            }*/

            copyWaveFile(file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE,getFilename());
            //deleteTempFile();
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
            int channels = 2;
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

}
