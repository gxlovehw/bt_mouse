package com.uascent.android.pethunting.ui;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;

import com.uascent.android.pethunting.R;
import com.uascent.android.pethunting.adapter.DeviceListAdapter;
import com.uascent.android.pethunting.model.BtDevice;
import com.uascent.android.pethunting.service.BleComService;
import com.uascent.android.pethunting.tools.Lg;

import java.util.ArrayList;

/**
 * Created by lenovo001 on 2016/5/28.
 */
public class ConnectCatActivity extends BaseActivity implements AdapterView.OnItemClickListener, View.OnClickListener {
    private static final String TAG = "ConnectCatActivity";

    private ArrayList<BtDevice> mListData = new ArrayList<BtDevice>();
    private IService mService;
    private Handler mHandler;
    boolean mScanningStopped;
    private ImageView iv_load_null;
    private Button bt_match;
    private ListView lv_device;
    private DeviceListAdapter adpater;

    private int index_checked = 0;
    private int count_device = 0;
    private BtDevice device;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_cat);
        initViews();
        mHandler = new Handler();
        showLoadingDialog();
        Intent i = new Intent(this, BleComService.class);
        getApplicationContext().bindService(i, mConnection, Context.BIND_AUTO_CREATE);
        Lg.i(TAG, "onCreate()");
    }

    private void initViews() {
        bt_match = (Button) findViewById(R.id.bt_match);
        bt_match.setOnClickListener(this);
        iv_load_null = (ImageView) findViewById(R.id.iv_load_null);
        lv_device = (ListView) findViewById(R.id.lv_device);
        lv_device.setOnItemClickListener(this);
        adpater = new DeviceListAdapter(this, mListData);
        lv_device.setAdapter(adpater);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Lg.i(TAG, "onServiceDisconnected");
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Lg.i(TAG, "onServiceConnected");
            mService = IService.Stub.asInterface(service);
            try {
                mService.registerCallback(mCallback);
            } catch (RemoteException e) {
                Lg.i(TAG, " " + e);
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    scanLeDevice(true);
                    Lg.i(TAG, "scanLeDevice(true)");
                }
            });
        }
    };

    private void scanLeDevice(final boolean enable) {
        BluetoothManager mBluetoothManager;
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager == null) {
            Lg.i(TAG, "Unable to initialize BluetoothManager.");
            return;
        }
        final BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (enable) {
            //搜索10s,10s后停止搜索
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    closeLoadingDialog();
                    Lg.i(TAG, "stop scanning after 10s");
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mScanningStopped = true;
                    if (mListData == null || mListData.size() == 0) {
                        showShortToast(getResources().getString(R.string.search_device_empty));
                        lv_device.setVisibility(View.GONE);
                        iv_load_null.setVisibility(View.VISIBLE);
                        Lg.i(TAG, "mListData的大小为0");
                    } else {
                        showShortToast(getResources().getString(R.string.search_device_over));
                    }

                }
            }, 10 * 1000);

            mBluetoothAdapter.startLeScan(mLeScanCallback);
            mScanningStopped = false;
        } else {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanningStopped = true;
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    Lg.i(TAG, "before_addDevice");
                    closeLoadingDialog();
                    addDevice(device.getAddress(), device.getName(), rssi);
                }
            };

    private ICallback.Stub mCallback = new ICallback.Stub() {
        @Override
        public void onConnect(String address) throws RemoteException {
        }

        @Override
        public void onDisconnect(String address) throws RemoteException {
            synchronized (mListData) {
                Lg.i(TAG, "onDisconnect called");
                //断开连接， do .....
//                for (int i = 0; i < mListData.size(); i++) {
//                    BtDevice d = mListData.get(i);
//                    if (d.getAddress().equals(address)) {
//                        d.setStatus(BluetoothAntiLostDevice.BLE_STATE_INIT);
//                        d.setPosition(BtDevice.LOST);
//                    }
//                }
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
//                    mDeviceListAdapter.notifyDataSetChanged();
//                    checkAntiLost();
                }
            });
        }

        @Override
        public boolean onRead(String address, byte[] val) throws RemoteException {
            Lg.i(TAG, "onRead called");
            return false;
        }


        @Override
        public boolean onWrite(final String address, byte[] val) throws RemoteException {
            Lg.i(TAG, "onWrite called");
            return true;
        }


        @Override
        public void onSignalChanged(String address, int rssi) throws RemoteException {
            synchronized (mListData) {
                Lg.i(TAG, "onSignalChanged called address = " + address + " rssi = " + rssi);
//                for (int i = 0; i < mListData.size(); i++) {
//                    BtDevice d = mListData.get(i);
//                    if (d.getAddress().equals(address)) {
//                        d.setRssi(rssi);
//                    }
//                }
            }
        }

        public void onPositionChanged(String address, int position) throws RemoteException {
            synchronized (mListData) {
                Lg.i(TAG, "onPositionChanged called address = " + address + " newpos = " + position);
//                for (int i = 0; i < mListData.size(); i++) {
//                    BtDevice d = mListData.get(i);
//                    if (d.getAddress().equals(address)) {
//                        d.setPosition(position);
//                    }
//                }
            }
        }

        @Override
        public void onAlertServiceDiscovery(final String btaddr, boolean support) throws RemoteException {

        }
    };

    public void addDevice(final String address, final String name, final int rssi) {
        Lg.i(TAG, "addDevice called:" + address + "   " + name + "  " + rssi);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                //避免重复添加
                for (BtDevice ele : mListData) {
                    if (ele.getAddress().equalsIgnoreCase(address)) {
                        return;
                    }
                }
                BtDevice device = new BtDevice();
                device.setName(name);
                device.setAddress(address);
                device.setRssi(rssi);
                if (count_device == 0) {
                    device.setChecked(true);
                }
                mListData.add(device);
                count_device = count_device + 1;
                Lg.i(TAG, "add_device_ok");
                adpater.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mListData.get(index_checked).setChecked(false);
        mListData.get(position).setChecked(true);
        index_checked = position;
        adpater.notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        Intent intent = null;
        switch (v.getId()) {
            case R.id.bt_match:
                if (mListData == null || mListData.size() == 0) {
                    showShortToast(getResources().getString(R.string.device_is_empty_not_match));
                    return;
                }
                if (connectBLE(index_checked)) {
                    showShortToast(getResources().getString(R.string.match_device_ok));
                    intent = new Intent(this, PlayActivity.class);
                    intent.putExtra("device", device);
                    startActivity(intent);
                } else {
                    showShortToast(getResources().getString(R.string.match_device_fail));
                }
                break;
            default:
                break;
        }
    }

//    public void goConnectDevice(int index) {
//        BtDevice device = mListData.get(index);
//        int status = device.getStatus();
//        Lg.i(TAG, "device_status = " + status);
//        switch (status) {
//            case BluetoothLeClass.BLE_STATE_CONNECTED: {
////                turnOnImmediateAlert(device.getAddress());
////                device.setStatus(BluetoothLeClass.BLE_STATE_ALERTING);
//                break;
//            }
//
//            case BluetoothLeClass.BLE_STATE_ALERTING: {
////                turnOffImmediateAlert(device.getAddress());
////                device.setStatus(BluetoothLeClass.BLE_STATE_CONNECTED);
//                break;
//            }
//            case BluetoothLeClass.BLE_STATE_CONNECTING: {
//                break;
//            }
//            default:
//            case BluetoothLeClass.BLE_STATE_INIT: {
//                synchronized (mListData) {
//                    if (connectBLE(device.getAddress())) {
//                        device.setStatus(BluetoothLeClass.BLE_STATE_CONNECTING);
//                    }
//                }
//                break;
//            }
//        }
//    }

    public boolean connectBLE(int index) {
        boolean ret = false;
        device = mListData.get(index);
        int status = device.getStatus();
        Lg.i(TAG, "device_status = " + status);
        try {
            ret = mService.connect(device.getAddress());
        } catch (RemoteException e) {
            e.printStackTrace();
            return ret;
        }
        return ret;
    }

}