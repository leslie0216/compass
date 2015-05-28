package ca.usask.chl848.rotationsensor;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends Activity {

    private String m_userName;
    private String m_userId;

    /** BT begin
     */
    private enum ServerOrCilent{
        NONE,
        SERVER,
        CILENT
    };

    private ServerThread m_serverThread = null;
    private ClientThread m_clientThread = null;
    private ConnectedThread m_connectedThread = null;

    private BluetoothAdapter m_bluetoothAdapter = null;
    private BluetoothDevice m_device = null;

    private BluetoothServerSocket m_serverSocket = null;
    private BluetoothSocket m_socket = null;

    private UUID m_UUID = UUID.fromString("8bb345b0-712a-400a-8f47-6a4bda472638");

    private InputStream m_inStream;
    private OutputStream m_outStream;

    private ServerOrCilent m_mode = ServerOrCilent.CILENT;

    private static int REQUEST_ENABLE_BLUETOOTH = 1;

    private ArrayList m_messageList = new ArrayList();

    private boolean isConnected;

    /** BT end
     */

    private SensorManager sm;

    private Sensor aSensor;

    private Sensor mSensor;

    private boolean m_isSensorRegistered;

    float[] accelerometerValues = new float[3];
    float[] magneticFieldValues = new float[3];

    private MainView m_mainView;

    long startTime = 0;
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (m_messageList.size() == 0) {
                m_mainView.sendRotation();
            }
            m_mainView.invalidate();
            sendMessage();
            timerHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = this.getIntent();
        Bundle bundle = intent.getExtras();
        m_userName = bundle.getString("user");
        m_userId = bundle.getString("id");

        setTitle(m_userId+" : "+m_userName);

        /**
         * BT begin
         */
        setupBluetooth();
        /**
         * BT end
         */

        m_mainView = new MainView(this);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        this.addContentView(m_mainView, new LinearLayout.LayoutParams(displayMetrics.widthPixels, displayMetrics.heightPixels));

        Button btn = new Button(this);
        btn.setText("Add Ball");
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (m_mainView != null && m_mainView.getBallCount() == 0) {
                    m_mainView.addBall();
                }
            }
        });

        RelativeLayout relativeLayout = new RelativeLayout(this);

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        relativeLayout.addView(btn, layoutParams);

        this.addContentView(relativeLayout, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        m_isSensorRegistered = false;
        registerSensors();

        calculateOrientation();

        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);
    }

    private void registerSensors() {
        if (!m_isSensorRegistered) {
            m_isSensorRegistered = true;
            sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

            aSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

            sm.registerListener(myListener, aSensor, SensorManager.SENSOR_DELAY_NORMAL);
            sm.registerListener(myListener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onPause() {
        sm.unregisterListener(myListener);
        m_isSensorRegistered = false;
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    final SensorEventListener myListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                accelerometerValues = event.values;
            }

            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                magneticFieldValues = event.values;
            }

            calculateOrientation();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private void calculateOrientation() {
        float[] values = new float[3];
        float[] R = new float[9];

        SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues);

        SensorManager.getOrientation(R, values);

        values[0] = (float)Math.toDegrees(values[0]);
        values[1] = (float)Math.toDegrees(values[1]);
        values[2] = (float)Math.toDegrees(values[2]);

        updateView(values);
    }

    public  void updateView(float[] values) {
        if(m_mainView != null) {
            m_mainView.setRotation(values);
        }
    }

    /**
     * BT begin
     */
    public void showToast(String message)
    {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        setupThread();
        registerSensors();
        super.onResume();
    }

    @Override
    protected void onRestart() {
        setupThread();
        registerSensors();
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        stopThreads();
        super.onDestroy();
    }

    private void stopThreads() {
        if(m_bluetoothAdapter!=null&&m_bluetoothAdapter.isDiscovering()){
            m_bluetoothAdapter.cancelDiscovery();
        }

        if (m_serverThread != null) {
            m_serverThread.cancel();
            m_serverThread = null;
        }
        if (m_clientThread != null) {
            m_clientThread.cancel();
            m_clientThread = null;
        }
        if (m_connectedThread != null) {
            m_connectedThread.cancel();
            m_connectedThread = null;
        }
    }

    public void showMessageOnMainView(String msg) {
        if(m_mainView != null)
            m_mainView.setMessage(msg);
    }

    private void setupBluetooth(){
        showMessageOnMainView("Not Connected");
        isConnected = false;

        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(m_bluetoothAdapter != null){  //Device support Bluetooth
            if(!m_bluetoothAdapter.isEnabled()){
                Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_ENABLE_BLUETOOTH);
            }
            else {
                setupThread();
            }
        }
        else{   //Device does not support Bluetooth

            Toast.makeText(this,"Bluetooth not supported on device", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                setupThread();
            }
            else {
                showToast("Bluetooth is not enable on your device");
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void setupThread(){
        if (m_mode == ServerOrCilent.SERVER) {
            if (m_serverThread == null) {
                m_serverThread = new ServerThread();
                m_serverThread.start();
            }
        } else if(m_mode == ServerOrCilent.CILENT) {
            findDevices();
            if (m_clientThread == null) {
                m_clientThread = new ClientThread();
                m_clientThread.start();
            }
        }
    }

    public void findDevices() {
        Set<BluetoothDevice> pairedDevices = m_bluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                if (device.getName().contains("312"))
                {
                    m_device = device;
                    break;
                }
                //mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    }

    public String getBTName() {
        return m_userName;
    }

    private class ConnectedThread extends Thread {
        public ConnectedThread() {
            try {
                m_inStream = m_socket.getInputStream();
                m_outStream = m_socket.getOutputStream();
                showMessageOnMainView("Connected");
                isConnected = true;
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        @Override
        public void run() {

            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    // Read from the InputStream
                    if( m_inStream != null && (bytes = m_inStream.read(buffer)) > 0 )
                    {
                        byte[] buf_data = new byte[bytes];
                        for(int i=0; i<bytes; i++)
                        {
                            buf_data[i] = buffer[i];
                        }
                        String msg = new String(buf_data);
                        receiveBTMessage(msg);
                    }
                } catch (IOException e) {
                    cancel();
                    showMessageOnMainView("Not Connected");
                    isConnected = false;
                    m_mainView.clearRemotePhoneInfo();
                    break;
                }
            }
        }

        public void write(String msg) {
            try {
                if (m_outStream != null) {
                    m_outStream.write(msg.getBytes());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                if (m_inStream != null) {
                    m_inStream.close();
                    m_inStream = null;
                }
                if (m_outStream != null) {
                    m_outStream.close();
                    m_outStream = null;
                }
                if (m_socket != null) {
                    m_socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ServerThread extends Thread {
        public ServerThread () {
            try {
                m_serverSocket = m_bluetoothAdapter.listenUsingRfcommWithServiceRecord("Blue_chat", m_UUID);
            } catch (IOException e) {
                m_serverSocket = null;
            }
        }
        @Override
        public void run() {
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                if (m_serverSocket != null) {
                    try {
                        showMessageOnMainView("Connecting...");
                        m_socket = m_serverSocket.accept();
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                else {
                    try {
                        m_serverSocket = m_bluetoothAdapter.listenUsingRfcommWithServiceRecord("Blue_chat", m_UUID);
                    } catch (IOException e) {
                        m_serverSocket = null;
                    }
                    continue;
                }

                if (m_socket != null) {

                    //Do work to manage the connection (in a separate thread)
                    m_connectedThread = new ConnectedThread();
                    m_connectedThread.start();

                    try {
                        m_serverSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                    break;
                }
            }
        }

        public void cancel() {
            try {
                if (m_inStream != null) {
                    m_inStream.close();
                    m_inStream = null;
                }
                if (m_outStream != null) {
                    m_outStream.close();
                    m_outStream = null;
                }
                if (m_serverSocket != null) {
                    m_serverSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ClientThread extends Thread {
        public ClientThread() {
            initSocket();
        }

        private void initSocket() {
            try {
                if (m_device != null) {
                    m_socket = m_device.createInsecureRfcommSocketToServiceRecord(m_UUID);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (true) {
                if (m_socket != null) {
                    m_bluetoothAdapter.cancelDiscovery();

                    try {
                        showMessageOnMainView("Connecting...");
                        m_socket.connect();
                    } catch (IOException e) {
                        try {
                            m_socket.close();
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                        initSocket();
                        continue;
                    }

                    //Do work to manage the connection (in a separate thread)
                    m_connectedThread = new ConnectedThread();
                    m_connectedThread.start();
                    break;
                } else {
                    initSocket();
                }
            }
        }

        public void cancel() {
            try {
                if (m_inStream != null) {
                    m_inStream.close();
                    m_inStream = null;
                }
                if (m_outStream != null) {
                    m_outStream.close();
                    m_outStream = null;
                }

                if (m_socket != null) {
                    m_socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendBTMessage(String msg) {
        if (m_connectedThread != null) {
            m_connectedThread.write(msg);
        }
    }

    public void addMessage(String msg) {
        m_messageList.add(msg);
    }

    public void sendMessage(){
        if (m_messageList.size() != 0) {
            String msg = (String)m_messageList.get(0);
            m_messageList.remove(0);
            sendBTMessage(msg);
        }
    }

    private void receiveBTMessage(String msg){
        try {
            JSONArray jsonArray = new JSONArray(msg);

            int len = jsonArray.length();

            ArrayList<String> ids = new ArrayList<>();

            for (int i=0; i<len; ++i) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                final String senderId = jsonObject.getString("id");
                int senderColor = jsonObject.getInt("color");
                float senderX = (float) jsonObject.getDouble("x");
                float senderY = (float) jsonObject.getDouble("y");
                float senderZ = (float) jsonObject.getDouble("z");

                //MainView mv = (MainView) findViewById(m_mainViewId);
                if (m_mainView != null) {
                    m_mainView.updateRemotePhone(senderId, senderColor, senderX, senderY, senderZ);
                }

                boolean isSendingBall = jsonObject.getBoolean("isSendingBall");
                if (isSendingBall && m_mainView != null) {
                    String receiverId = jsonObject.getString("receiverId");
                    if (receiverId.equalsIgnoreCase(m_mainView.getPhoneId())) {
                        String ballId = jsonObject.getString("ballId");
                        int ballColor = jsonObject.getInt("ballColor");
                        m_mainView.receivedBall(ballId, ballColor);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showToast("received ball from : " + senderId);
                            }
                        });
                    }
                }

                ids.add(senderId);
            }

            ArrayList<MainView.RemotePhoneInfo> remotePhoneInfos = m_mainView.getRemotePhones();
            ArrayList<MainView.RemotePhoneInfo> lostPhoneInfos = new ArrayList<>();
            for (MainView.RemotePhoneInfo phoneInfo : remotePhoneInfos) {
                if (!ids.contains(phoneInfo.m_id)) {
                    lostPhoneInfos.add(phoneInfo);
                }
            }

            if (!lostPhoneInfos.isEmpty()) {
                m_mainView.removePhones(lostPhoneInfos);
            }
        }catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected(){
        return isConnected;
    }

    /**
     * BT end
     */

}
