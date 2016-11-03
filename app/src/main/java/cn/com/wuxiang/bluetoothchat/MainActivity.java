package cn.com.wuxiang.bluetoothchat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.com.wuxiang.bluetoothchat.adapter.BluetoothDeviceAdapter;
import cn.com.wuxiang.bluetoothchat.service.BluetoothChatService;
import cn.com.wuxiang.bluetoothchat.service.Contants;

public class MainActivity extends AppCompatActivity {

    //controls
    private ToggleButton tb_bluetooth;
    private Button bt_search;
    private ListView lv_bluetooth;
    //adapter
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDeviceAdapter adapter;
    //list:save device's information
    private List<Map<String,Object>> list_device_info;
    //BroadcastReceiver
    public BroadcastReceiver mReceiver;
    public BluetoothChatService mService;
    private Button send;
    private EditText sengContent;
    private StringBuffer mOutStringBuffer;
    private cn.com.wuxiang.bluetoothchat.adapter.BluetoothChatService mmService;


    private Handler mHanlder = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case Contants.MESSAGE_WRITE:
                    Toast.makeText(MainActivity.this,"write",Toast.LENGTH_SHORT).show();
                    break;
                case Contants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf);
                    Toast.makeText(MainActivity.this,"receive the message:"+readMessage,Toast.LENGTH_SHORT).show();
                    Log.e("message",readMessage);
                    break;
                case Contants.MESSAGE_DEVICE_NAME:
                    String name = msg.getData().getString(Contants.DEVICE_NAME);
                        Toast.makeText(MainActivity.this, "Connected to "
                                + name, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }

        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        initPermission();
        setIntentFilter();
        setListener();
        lv_bluetooth.setAdapter(adapter);
    }

    //initialize controls
    private void init(){
        tb_bluetooth = (ToggleButton)MainActivity.this.findViewById(R.id.tb);
        bt_search = (Button)MainActivity.this.findViewById(R.id.bt_search);
        lv_bluetooth = (ListView)MainActivity.this.findViewById(R.id.lv_bt);
        send = (Button)MainActivity.this.findViewById(R.id.send);
        sengContent = (EditText)MainActivity.this.findViewById(R.id.send_content);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mService = new BluetoothChatService(MainActivity.this,mHanlder);
        mmService = new cn.com.wuxiang.bluetoothchat.adapter.BluetoothChatService(MainActivity.this,mHanlder);
        send = (Button)MainActivity.this.findViewById(R.id.send);
        mOutStringBuffer = new StringBuffer("");
        list_device_info = new ArrayList<Map<String,Object>>();
        adapter = new BluetoothDeviceAdapter(this,list_device_info);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.e("action",action);
                //Accept a variety of Bluetooth Broadcasting
                if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){

                }else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
                        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                        switch (state){
                            case BluetoothAdapter.STATE_ON:
                                tb_bluetooth.setChecked(true);
                                initPairedBluetoothDevice();
                                break;
                            case BluetoothAdapter.STATE_OFF:
                                if (!mBluetoothAdapter.isEnabled()) {
                                    tb_bluetooth.setChecked(false);
                                    list_device_info.removeAll(list_device_info);
                                    adapter.notifyDataSetChanged();
                                    lv_bluetooth.setAdapter(adapter);
                                }
                                break;
                        }

                }else if(BluetoothDevice.ACTION_FOUND.equals(action)){
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Map<String,Object> map = new HashMap<String,Object>();
                    for(int i=0;i<list_device_info.size();i++){
                        if(device.getAddress().equals(list_device_info.get(i).get("address"))){
                            list_device_info.remove(i);
                            adapter.notifyDataSetChanged();
                            break;
                        }
                    }
                    map.put("address", device.getAddress());
                        //Map<String,Object> map = new HashMap<String,Object>();
                        if (TextUtils.isEmpty(device.getName())) {
                            map.put("name", device.getAddress());
                        } else {
                            map.put("name", device.getName());
                        } 
                        if (BluetoothDevice.BOND_BONDED == device.getBondState()) {
                            map.put("state", "已配对");
                        } else {
                            map.put("state", "未配对");
                        }
                        list_device_info.add(map);
                        adapter.notifyDataSetChanged();
                }else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if(BluetoothDevice.BOND_BONDED==device.getBondState()){
                        for(Map map : list_device_info){
                            if(device.getAddress().equals(map.get("address"))){
                                map.put("state","已配对");
                            }
                        }
                    }
                    else if(BluetoothDevice.BOND_NONE == device.getBondState()){
                        for(Map map : list_device_info){
                            if(device.getAddress().equals(map.get("address"))){
                                map.put("state","未配对");
                            } 
                        }
                    }

                }else if(action.equals(BluetoothAdapter.STATE_CONNECTED)){

                }else if(BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)){
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if(BluetoothDevice.BOND_BONDED == device.getBondState())
                        for(Map map : list_device_info){
                            if(device.getAddress().equals(map.get("address"))){
                                map.put("state","已配对");
                            }else{
                                map.put("state","未配对");
                            }
                        }
                    adapter.notifyDataSetChanged();
                   /* lv_bluetooth.setAdapter(adapter); */

                }else if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)){
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if(BluetoothDevice.BOND_BONDED == device.getBondState())
                    for(Map map : list_device_info){
                        if(device.getAddress().equals(map.get("address"))){
                            map.put("state","已配对");
                        }else{
                            map.put("state","未配对");
                        }
                    }
                    adapter.notifyDataSetChanged();
                }
            }
        };
        lv_bluetooth.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mBluetoothAdapter.cancelDiscovery();
                Toast.makeText(MainActivity.this,""+list_device_info.get(position).get("address"),Toast.LENGTH_SHORT).show();
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(list_device_info.get(position).get("address")+"");
                if(BluetoothDevice.BOND_BONDED != device.getBondState()){
                    try{
                        Method createBondMethod = BluetoothDevice.class.getMethod("createBond");
                        createBondMethod.invoke(device);

                    }catch (Exception e){

                    }
                }
                else if(device.getBondState() == BluetoothDevice.BOND_BONDED){
                    mService.connect(device);
                }
            }
        });
        if(mBluetoothAdapter.isEnabled()){
            initPairedBluetoothDevice();
            tb_bluetooth.setChecked(true);

        }
        else{
            tb_bluetooth.setChecked(false);
        }
    }
    public void initPairedBluetoothDevice(){
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        Log.e("size",devices.size()+"");
        if(devices.size()>0){
            for(Iterator<BluetoothDevice> it = devices.iterator(); it.hasNext();){
                BluetoothDevice device = (BluetoothDevice)it.next();
                HashMap<String,Object> map = new HashMap<String, Object>();
                Log.e("address",device.getAddress()+"");
                map.put("name",device.getName());
                map.put("address",device.getAddress());
                map.put("state", "已配对");
                list_device_info.add(map);
                adapter.notifyDataSetChanged();
                /*lv_bluetooth.setAdapter(adapter);*/
            }
        }else{
            list_device_info.removeAll(list_device_info);
            adapter.notifyDataSetChanged();
            Toast.makeText(MainActivity.this,"还没有已配对的远程蓝牙设备！",Toast.LENGTH_SHORT).show();
        }
    }

    //initialize the IntentFilter
    private void setIntentFilter(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.EXTRA_BOND_STATE);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mReceiver,intentFilter);

    }
    //
    //Android 6.0 Start permissions required to execute the code in order to ensure their safety.
    private void initPermission(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH,Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_ADMIN},0);
        }
    }
    //set the onclick
    public void setListener(){
        tb_bluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
             if(tb_bluetooth.isChecked()){
                 mBluetoothAdapter.enable();

             }else{
                 mBluetoothAdapter.disable();
                 list_device_info.removeAll(list_device_info);
                 adapter.notifyDataSetChanged();
             }

            }
        });
        bt_search.setOnClickListener(new View.OnClickListener() {
                 @Override
                 public void onClick(View v) {
                         for(int i=0;i<list_device_info.size();i++){
                             if(!"已配对".equals(list_device_info.get(i).get("state")))
                             list_device_info.remove(i);
                         }
                         if (mBluetoothAdapter.isDiscovering()) {
                             mBluetoothAdapter.cancelDiscovery();
                         }
                         mBluetoothAdapter.startDiscovery();
                 }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                 Log.e("test","this is a test message");
                 String message = "this is a successful message";
                 sendMessage(message);

            }
        });
    }

    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(MainActivity.this, "You are not connected to a device", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            Log.e("message","message:"+message);
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            //mOutEditText.setText(mOutStringBuffer);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mService.start();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }
}
