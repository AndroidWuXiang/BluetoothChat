package cn.com.wuxiang.bluetoothchat.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.PriorityQueue;
import java.util.UUID;

/**
 * Created by wuxiang on 16-10-28.
 */

public class BluetoothChatService {
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    //unique UUID
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //name
    private static final String NAME_INSECURE = "BluetoothChatInsecure";
    private Context mContext;
    private Handler mHandler;
    private BluetoothAdapter mAdapter;
    private int mState;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    public BluetoothChatService(Context context,Handler hanlder){
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mContext = context;
        mHandler = hanlder;
        mState = STATE_NONE;
    }
    public synchronized  void setState(int state){
        mState = state;
        mHandler.obtainMessage(Contants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }
    public synchronized int  getState(){
        return mState;
    }
    //connect the bluetoothdevice
    class ConnectThread extends Thread{
        private BluetoothSocket bluetoothSocket;
        private BluetoothDevice bluetoothDevice;

        public ConnectThread(BluetoothDevice device){
            bluetoothDevice = device;
            BluetoothSocket tmp = null;
            try{
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID_INSECURE);
            }catch (IOException e){

            }
            //Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            if(tmp!=null)
            bluetoothSocket = tmp;
        }
        @Override
        public void run() {
            mAdapter.cancelDiscovery();
                try {
                    bluetoothSocket.connect();
                    Log.e("run error","bluetoothSocket:"+bluetoothSocket.isConnected());
                }catch(IOException e1){
                    try{
                        Log.e("error","ConnectThread:"+e1);
                        bluetoothSocket.close();
                    }catch(IOException e2){

                    }
                    return;
                }
                synchronized (BluetoothChatService.this) {
                    mConnectThread = null;
                    /*switch (mState) {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                            // Situation normal. Start the connected thread.
                            connected(bluetoothSocket, bluetoothSocket.getRemoteDevice());
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                            // Either not ready or already connected. Terminate new socket.
                            try {
                                bluetoothSocket.close();
                            } catch (IOException e) {
                                Log.e("error","ConnectThread2:"+e);
                            }
                            break;
                    }*/
                }

              connected(bluetoothSocket, bluetoothDevice);
        }
        public void cancel(){
            try {
                bluetoothSocket.close();
            } catch (IOException e) {

            }
        }
    }
    class AcceptThread extends Thread{
        private BluetoothServerSocket mSocket;
        public AcceptThread(){
            BluetoothServerSocket tmp = null;
            try{
                tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME_INSECURE,MY_UUID_INSECURE);
                if(tmp!= null){
                    mSocket = tmp;
                    Log.e("error","mSocket is not null");
                }
            }catch(IOException e){

            }

        }

        @Override
        public void run() {
            BluetoothSocket socket = null;
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mSocket.accept();
                } catch (IOException e) {
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothChatService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {

                                }
                                break;
                        }
                    }
                }
            }
        }
        public void cancel() {

            try {
                mSocket.close();
            } catch (IOException e) {

            }
        }

    }
    public void start(){

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_LISTEN);

        if(mAcceptThread == null){
                mAcceptThread = new AcceptThread();
                mAcceptThread.start();
        }


    }
    public void connect(BluetoothDevice device){
        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }
    private class ConnectedThread extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            bluetoothSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
                Log.e("tmpIn",""+tmpIn);
            } catch (IOException e) {
                    Log.e("error","this is connected IOException");
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        @Override
        public void run() {
            super.run();
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED) {
                Log.e("read","STATE_CONNECTED");
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(Contants.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    //connectionLost();
                    // Start the service over to restart listening mode
                    BluetoothChatService.this.start();
                    break;
                }
            }
        }
        public void write(byte[] buffer) {
            Log.e("write","buffer:"+buffer);
            Log.e("out","outstream:"+mmOutStream);
            try {
                //bluetoothSocket.connect();
                mmOutStream.write(buffer);
                Log.e("write","buffer:"+buffer);
                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(Contants.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e("error", "this is exception"+e);
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {

            }
        }
    }


    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        Log.e("write","go here:"+out);
        r.write(out);
    }
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device){

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if(mAcceptThread !=null){
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(Contants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Contants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setState(STATE_CONNECTED);
    }
    public synchronized void stop() {

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        setState(STATE_NONE);
    }
}
