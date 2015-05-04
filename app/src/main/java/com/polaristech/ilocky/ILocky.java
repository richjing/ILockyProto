package com.polaristech.ilocky;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by richard on 15/5/2.
 */
public class ILocky {
    private final Context mContext;
    private static final Map<Context, ILocky> sInstanceMap = new HashMap<Context, ILocky>();
    private BluetoothAdapter mBluetoothAdapter;

    ILocky(Context context) {
        mContext = context;
    }

    public static ILocky getInstance(Context context) {
        if (null == context) {
            return null;
        }
        synchronized (sInstanceMap) {
            final Context appContext = context.getApplicationContext();

            ILocky instance = sInstanceMap.get(appContext);
            if (null == instance) {
                instance = new ILocky(appContext);
                sInstanceMap.put(appContext, instance);
            }
            return instance;
        }
    }
    String mName="iLocky";
    public void turnONProto(BluetoothAdapter bluetoothAdapter,String iLockyName) {
        if(iLockyName!=null&&iLockyName.length()==0)
            mName=iLockyName;
        if (bluetoothAdapter == null)
            return;
        mBluetoothAdapter = bluetoothAdapter;
        mHandler=new Handler();
        scanLeDevice(true);

    }
    private boolean mScanning;
    private Handler mHandler;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    private BluetoothGatt mBluetoothGatt;
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    if(device.getName().equals(mName)) {
                        scanLeDevice(false);
                        mBluetoothGatt=device.connectGatt(mContext,false,mGattCallback);
                    };

                }
            };
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private BluetoothGattCallback mGattCallback= new BluetoothGattCallback() {
        private final String TAG = ILocky.class.getSimpleName();
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectionState = STATE_CONNECTED;
                Log.i(TAG, "Connected to GATT server.");
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = STATE_DISCONNECTED;

            }
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattCharacteristic characteristic=null;
                BluetoothGattService service=gatt.getService(UUID.fromString("00003000-0000-1000-8000-00805f9b34fb"));
                if(service!=null)
                characteristic= service.getCharacteristic(UUID.fromString("00003001-0000-1000-8000-00805f9b34fb"));
                if(characteristic!=null) {
                    characteristic.setValue("1");
                    gatt.writeCharacteristic(characteristic);
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            close();
        }

    };
    public void close(){
        if(mBluetoothAdapter!=null) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

 }
