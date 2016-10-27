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
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.com.wuxiang.bluetoothchat.adapter.BluetoothDeviceAdapter;

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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        initPermission();
        setIntentFilter();
        setListener();

    }

    //initialize controls
    private void init(){
        tb_bluetooth = (ToggleButton)MainActivity.this.findViewById(R.id.tb);
        bt_search = (Button)MainActivity.this.findViewById(R.id.bt_search);
        lv_bluetooth = (ListView)MainActivity.this.findViewById(R.id.lv_bt);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter.isEnabled()){
            tb_bluetooth.setChecked(true);
        }
        else{
            tb_bluetooth.setChecked(false);
        }
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
                    if(device!=null) {
                        for (Map map_d : list_device_info) {
                            if (device.getAddress().equals(map_d.get("address"))) {
                                return;
                            }
                        }
                    }
                    Map<String,Object> map = new HashMap<String,Object>();
                    if(TextUtils.isEmpty(device.getName())){
                        map.put("name",device.getAddress());
                    }
                    else{
                        map.put("name",device.getName());
                    }
                    map.put("address",device.getAddress());
                    if(BluetoothDevice.BOND_BONDED == device.getBondState()){
                        map.put("state","已配对");
                    }
                    else{
                        map.put("state","未配对");
                    }
                    list_device_info.add(map);
                    lv_bluetooth.setAdapter(adapter);
                    adapter.notifyDataSetChanged();

                }else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){

                }else if(action.equals(BluetoothAdapter.STATE_CONNECTED)){

                }else if(BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)){

                }else if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)){
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.e("1234",device.getName());
                    if(BluetoothDevice.BOND_BONDED == device.getBondState())
                    for(Map map : list_device_info){
                        if(device.getAddress().equals(map.get("address"))){
                            map.put("state","已配对");
                        }else{
                            map.put("state","未配对");
                        }
                    }
                    adapter.notifyDataSetChanged();
                    lv_bluetooth.setAdapter(adapter);
                }
            }
        };
        lv_bluetooth.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(MainActivity.this,""+list_device_info.get(position).get("address"),Toast.LENGTH_SHORT).show();
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(list_device_info.get(position).get("address")+"");
                if(BluetoothDevice.BOND_BONDED != device.getBondState()){
                    try{
                        Method createBondMethod = BluetoothDevice.class.getMethod("createBond");
                        createBondMethod.invoke(device);

                    }catch (Exception e){

                    }
                }
                else{

                }
            }
        });
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
                 lv_bluetooth.removeAllViews();
             }

            }
        });
        bt_search.setOnClickListener(new View.OnClickListener() {
                 @Override
                 public void onClick(View v) {
                     if(list_device_info.size()>0){
                         list_device_info.removeAll(list_device_info);
                     }
                     if(mBluetoothAdapter.isDiscovering()){
                           mBluetoothAdapter.cancelDiscovery();
                     }
                     mBluetoothAdapter.startDiscovery();
                 }
        });

    }
}
