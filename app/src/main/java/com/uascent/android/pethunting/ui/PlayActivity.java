package com.uascent.android.pethunting.ui;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.uascent.android.pethunting.MyApplication;
import com.uascent.android.pethunting.R;
import com.uascent.android.pethunting.devices.BluetoothLeClass;
import com.uascent.android.pethunting.model.BtDevice;
import com.uascent.android.pethunting.myviews.VerticalSeekBar;
import com.uascent.android.pethunting.service.BleComService;
import com.uascent.android.pethunting.tools.Lg;

import java.util.Timer;

public class PlayActivity extends BaseActivity implements View.OnClickListener,
        SeekBar.OnSeekBarChangeListener, VerticalSeekBar.OnSeekBarStopListener
        , VerticalSeekBar.OnSeekBarStopTouchListener, View.OnTouchListener/*, View.OnLongClickListener */ {
    private static final String TAG = "PlayActivity";

    //    private static final int TIMEPERCMD = 100;
    private VerticalSeekBar ver_sb;
    private TextView tv_empty;
    private ImageView iv_play_guide, iv_play_home;
    private ImageView iv_top_dir, iv_below_dir, iv_left_dir, iv_right_dir;
    private ImageView iv_battery;
    private IService mService;
    private Handler mHandler;
    private BtDevice device;
    private static int speedValue = 0;
    private static int dirValue = 0;
    private static int startSpeed = 0;
    //    private Long preTime = 0l;
    private int countBatteryLow = 0;

    private BluetoothAdapter mBluetoothAdapter;
    //    private boolean isCmd = false;
    private boolean isDownUp = true;
    private int curProgress = 0;

    //求电量的平均值
    private byte[] batteryArray = new byte[5];
    private byte batteryIndex = 0;
    private boolean isFrist = true;

    private Timer timer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Lg.i(TAG, "onCreate");
        if (android.os.Build.VERSION.SDK_INT >= 20) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setContentView(R.layout.activity_play);
        super.onCreate(savedInstanceState);
        initViews();
        mHandler = new Handler();
        device = (BtDevice) getIntent().getSerializableExtra("device");
        Intent i = new Intent(this, BleComService.class);
        getApplicationContext().bindService(i, mConnection, Context.BIND_AUTO_CREATE);
        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager != null) {
            mBluetoothAdapter = mBluetoothManager.getAdapter();
        } else {
            showShortToast(getString(R.string.moible_not_support_bluetooth4));
        }
        timer = new Timer();
    }

    private void initViews() {
        ver_sb = (VerticalSeekBar) findViewById(R.id.ver_sb);
        VerticalSeekBar.setSeekBarStopListener(this);
        VerticalSeekBar.setSeekBarStopTouchListener(this);
        ver_sb.setOnSeekBarChangeListener(this);
        tv_empty = (TextView) findViewById(R.id.tv_empty);
        tv_empty.setOnClickListener(this);
        iv_play_guide = (ImageView) findViewById(R.id.iv_play_guide);
        iv_play_guide.setOnClickListener(this);
        iv_play_home = (ImageView) findViewById(R.id.iv_play_home);
        iv_play_home.setOnClickListener(this);

        iv_top_dir = (ImageView) findViewById(R.id.iv_top_dir);
        iv_top_dir.setOnTouchListener(this);
        iv_top_dir.setOnClickListener(this);
//        iv_top_dir.setOnLongClickListener(this);

        iv_below_dir = (ImageView) findViewById(R.id.iv_below_dir);
        iv_below_dir.setOnClickListener(this);
        iv_below_dir.setOnTouchListener(this);
//        iv_below_dir.setOnLongClickListener(this);

        iv_left_dir = (ImageView) findViewById(R.id.iv_left_dir);
        iv_left_dir.setOnClickListener(this);
        iv_left_dir.setOnTouchListener(this);
//        iv_left_dir.setOnLongClickListener(this);

        iv_right_dir = (ImageView) findViewById(R.id.iv_right_dir);
        iv_right_dir.setOnClickListener(this);
        iv_right_dir.setOnTouchListener(this);
//        iv_right_dir.setOnLongClickListener(this);

        iv_battery = (ImageView) findViewById(R.id.iv_battery);
    }

    @Override
    public void onClick(View v) {
        Intent intent = null;
        switch (v.getId()) {
            case R.id.iv_play_guide:
                MyApplication.getInstance().isAutoBreak = true;
//                autoBreakConnect();
                intent = new Intent(this, UserGuideActivity.class);
                startActivity(intent);
                finish();
                break;
            case R.id.iv_play_home:
                MyApplication.getInstance().isAutoBreak = true;
//                autoBreakConnect();
                intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
                break;
            default:
                break;
        }
    }

    void sendMouseCmd(String addr, int cmd) {
        Lg.i(TAG, "sendMouseCmd:" + cmd);
        controlMouseDir(addr, cmd);
//        getMouseRsp(addr);
    }

    /**
     * 发送控制老鼠速度命令
     *
     * @param addr
     * @param cmd
     */
    void sendMouseSpeedCmd(String addr, int value, int cmd) {
        Lg.i(TAG, "sendMouseCmd:" +addr+""+ value + "  " + cmd);
        controlMouseSpeed(addr, value, cmd);
//        getMouseRsp(addr);
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//        ver_sb_per.setText(progress + "%");
        speedValue = progress / 10;
        curProgress = progress;
//        Lg.i(TAG, "onProgressChanged_speedValue->>" + speedValue);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Lg.i(TAG, "play_onStartTrackingTouch");
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Lg.i(TAG, "play_onStartTrackingTouch");
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
                //读电量
                mService.setBatteryNoc(device.getAddress());
            } catch (RemoteException e) {
                Lg.i(TAG, " " + e);
            }
        }
    };


    private ICallback.Stub mCallback = new ICallback.Stub() {
        @Override
        public void onConnect(String address) throws RemoteException {
            Lg.i(TAG, "onConnect called");
        }

        @Override
        public void onDisconnect(final String address) throws RemoteException {
            Lg.i(TAG, "PlayActivity onDisconnect called");
            //蓝牙意外断开，考虑重连接
            if (!MyApplication.getInstance().isAutoBreak) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        showShortToast(getString(R.string.bluetooth_has_breaked));
                        relinkBlueTooth();
                    }
                });
            }
        }

        @Override
        public boolean onRead(String address, byte[] val) throws RemoteException {
            Lg.i(TAG, "onRead called:");
            return false;
        }


        @Override
        public boolean onWrite(final String address, final byte[] val) throws RemoteException {
            Lg.i(TAG, "电量onWrite->>" + val[0]);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    //求平均值
                    if (batteryAver(val[0]) <= 30) {
                        iv_battery.setBackgroundResource(R.drawable.empty_battery);
                        if (countBatteryLow % 3 == 0) {
                            showLongToast(getString(R.string.low_battery_remind));
                        }
                        countBatteryLow++;
                    } else if (batteryAver(val[0]) >= 70) {
                        iv_battery.setBackgroundResource(R.drawable.full_battery);
                    } else {
                        iv_battery.setBackgroundResource(R.drawable.half_battery);
                    }
                }
            });
            return true;
        }

//        @Override
//        public void onSignalChanged(String address, int rssi) throws RemoteException {
//            Lg.i(TAG, "onSignalChanged called address = " + address + " rssi = " + rssi);
//        }
//
//        public void onPositionChanged(String address, int position) throws RemoteException {
//            Lg.i(TAG, "onPositionChanged called address = " + address + " newpos = " + position);
//        }
//
//        @Override
//        public void onAlertServiceDiscovery(final String btaddr, boolean support) throws RemoteException {
//            Lg.i(TAG, "onAlertServiceDiscovery");
//        }

        @Override
        public void onMouseServiceDiscovery(String address, boolean support) throws RemoteException {
            Lg.i(TAG, "onMouseServiceDiscovery_support" + support);
            if (support) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        showShortToast(getString(R.string.relinked_bluetooth_ok));
                    }
                });
            }
        }
    };

    /**
     * 重新连接蓝牙
     */
    private void relinkBlueTooth() {
        Lg.i(TAG, "relinkBlueTooth");
        if (mBluetoothAdapter != null) {
            Lg.i(TAG, "mBluetoothAdapter != null");
            if (mBluetoothAdapter.isEnabled()) {
                Lg.i(TAG, "mBluetoothAdapter.isEnabled()");
                showShortToast(getString(R.string.check_ble_switch));
                if (device != null) {
                    Lg.i(TAG, "relink--->device != null");
                    //mService.disconnect();
                    relinkBleDevice();
                }
            } else {
                Lg.i(TAG, "!----mBluetoothAdapter.isEnabled()");
                openBlueTooth();
            }
        } else {  //打开蓝牙开关之后，重新连接设备
            Lg.i(TAG, "mBluetoothAdapter = null");
            BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = mBluetoothManager.getAdapter();
            openBlueTooth();
        }
    }

    private void openBlueTooth() {
        Lg.i(TAG, "openBlueTooth()");
        Long temTime = System.currentTimeMillis();
        mBluetoothAdapter.enable();
        while (!mBluetoothAdapter.isEnabled()) {
            if (System.currentTimeMillis() - temTime > (30 * 1000)) {
                closeLoadingDialog();
                return;
            }
            showLoadingDialog(getString(R.string.bluetooth_is_opening));
        }
        if (mBluetoothAdapter.isEnabled()) {
            closeLoadingDialog();
            relinkBleDevice();
        } else {
            showLoadingDialog(getString(R.string.bluetooth_switch_not_opened));
        }
    }

    /**
     * 重新连接ble 设备
     */
    private void relinkBleDevice() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
//                showShortToast(getString(R.string.bluetooth_isconnecting));
                try {
                    if (!mService.connect(device.getAddress())) {
                        showShortToast(getString(R.string.relinked_bluetooth_fail));
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 控制方向
     *
     * @param addr
     * @param dir
     */
    public void controlMouseDir(String addr, int dir) {
        try {
            mService.controlMouse(addr, dir);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 控制加速度
     *
     * @param addr
     * @param dir
     */
    public void controlMouseSpeed(String addr, int value, int dir) {
        try {
            mService.controlMouseSpeed(addr, value, dir);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    public void getMouseRsp(String addr) {
        try {
            mService.readMouseRsp(addr);
            Lg.i(TAG, "readMouseRsp->>" + addr);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        Lg.i(TAG, "onResume");
        MyApplication.getInstance().isAutoBreak = false;
        if (curProgress != 0) {
            ver_sb.setProgress(0);
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        Lg.i(TAG, "onPause");
        if (!MyApplication.getInstance().isAutoBreak) {
            ver_sb.setProgress(0);
            Lg.i(TAG, "onPause_setProgress(0)");
            dirValue = BluetoothLeClass.MOUSE_STOP;
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        Lg.i(TAG, "onStop");
        if (!MyApplication.getInstance().isAutoBreak) {
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    Lg.i(TAG, "onStop_run()");
                    if (mConnection != null && device != null) {
                        controlMouseDir(device.getAddress(), BluetoothLeClass.MOUSE_STOP);
                    }
                }
            }.start();
        }
        super.onStop();
    }

    @Override
    public void finish() {
        Lg.i(TAG, "finish");
        if (!MyApplication.getInstance().isAutoBreak) {
            Lg.i(TAG, "finish_move");
            moveTaskToBack(true);
        } else {
            Lg.i(TAG, "super_finish");
            super.finish();
        }
    }

    @Override
    protected void onDestroy() {
        Lg.i(TAG, "onDestroy--->" + MyApplication.getInstance().isAutoBreak);
        if (MyApplication.getInstance().isAutoBreak) {
            breakConnect();
            Lg.i(TAG, "onDestroy->>unbindService");
        }
        super.onDestroy();
    }

    /**
     * 断开连接
     */
    private void breakConnect() {
        Lg.i(TAG, "onDestroy->>breakConnect");
        if (mConnection != null) {
            try {
                if (dirValue != BluetoothLeClass.MOUSE_STOP) {
                    if (device != null) {
                        controlMouseDir(device.getAddress(), BluetoothLeClass.MOUSE_STOP);
                    }
                }
                Lg.i(TAG, "onDestroy->>unregisterCallback");
                if (mService != null) {
                    if (device != null) {
                        Lg.i(TAG, "disconnect_device_address = " + device.getAddress());
                        mService.disconnect(device.getAddress());
                        device = null;
                    }
                    mService.unregisterCallback(mCallback);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        getApplicationContext().unbindService(mConnection);
    }


    @Override
    public void onSeekBarStop() {
        Lg.i(TAG, "onSeekBarStop_滑动到底部-->>" + "   speedValue:" + 0 + "  dirValue: " + dirValue);
        ver_sb.setProgress(0);
        if (device != null) {
            if (dirValue != 0) {
                sendMouseSpeedCmd(device.getAddress(), 0, dirValue);
            } else {
                //方向键先松开  dirvalue=0
                sendMouseCmd(device.getAddress(), dirValue);
            }
        }
        startSpeed = 0;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (v.getId()) {
            case R.id.iv_top_dir:
                dirValue = BluetoothLeClass.MOUSE_UP;
                break;
            case R.id.iv_below_dir:
                dirValue = BluetoothLeClass.MOUSE_DOWN;
                break;
            case R.id.iv_left_dir:
                dirValue = BluetoothLeClass.MOUSE_LEFT;
                break;
            case R.id.iv_right_dir:
                dirValue = BluetoothLeClass.MOUSE_RIGHT;
                break;
            default:
                break;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:  //判断按下和抬起的时间间隔
                Lg.i(TAG, "event.getAction()---ACTION_DOWN--" + dirValue);
                if (isDownUp) {
//                    if (timer != null) {
//                        timer.cancel();
//                    }
//                    timer = new Timer();
//                    preTime = System.currentTimeMillis();
//                    timer.schedule(new TimerTask() {
//                        @Override
//                        public void run() {
                    if (device != null) {
                        if (speedValue != BluetoothLeClass.MOUSE_STOP) {  //先滑动滑动条
                            sendMouseSpeedCmd(device.getAddress(), speedValue, dirValue);
                        } else {
                            //先按住方向键
                            sendMouseCmd(device.getAddress(), dirValue);
                        }
                    }
//                        }
//                    }, TIMEPERCMD);
                } else {
                    showShortToast(getString(R.string.only_click_button));
                }
                isDownUp = false;

                break;

            case MotionEvent.ACTION_MOVE:
                break;

            case MotionEvent.ACTION_UP:
                dirValue = BluetoothLeClass.MOUSE_STOP;
                Lg.i(TAG, "event.getAction()---ACTION_UP--" + BluetoothLeClass.MOUSE_STOP);
//                if (System.currentTimeMillis() - preTime < TIMEPERCMD) {
//                    timer.cancel();
//                } else {
                if (device != null) {
                    sendMouseCmd(device.getAddress(), BluetoothLeClass.MOUSE_STOP);
                }
//                }
                isDownUp = true;

                break;
            default:
                break;

        }

        return false;
    }

    @Override
    public void onSeekBarStopTouch() {
        Lg.i(TAG, "onSeekBarStopTouch_speedValue->>>" + speedValue + " startSpeed->>>" + startSpeed);
        if (device != null && speedValue != startSpeed) {
            if (dirValue != 0) {
                sendMouseSpeedCmd(device.getAddress(), speedValue, dirValue);
            } else {  //方向键先松开
                sendMouseCmd(device.getAddress(), dirValue);
            }
            startSpeed = speedValue;
        }
    }

    @Override
    public void onBackPressed() {
        Lg.i(TAG, "onBackPressed");
        MyApplication.getInstance().isAutoBreak = true;
        super.onBackPressed();
    }

//    @Override
//    public boolean onLongClick(View v) {
//        if (!isCmd) {
//            Lg.i(TAG, "onLongClick");
//            isCmd = true;
//            if (device != null) {
//                if (speedValue != 0) {  //先滑动滑动条
//                    sendMouseSpeedCmd(device.getAddress(), speedValue, dirValue);
//                } else {   //先按住方向键
//                    sendMouseCmd(device.getAddress(), dirValue);
//                }
//            }
//        }
//        return false;
//    }

    /**
     * 电量的平均值
     *
     * @return
     */
    private byte batteryAver(byte value) {
        batteryArray[batteryIndex] = value;
        byte tem = 0;
        int countBattery = 0;
        if (isFrist) {
            for (int i = 0; i <= batteryIndex; i++) {
                countBattery = countBattery + batteryArray[batteryIndex];
            }
            tem = (byte) (countBattery / (batteryIndex + 1));
        } else {
            for (int i = 0; i < batteryArray.length; i++) {
                countBattery = countBattery + batteryArray[batteryIndex];
            }
            tem = (byte) (countBattery / batteryArray.length);
        }
        batteryIndex++;
        if (batteryIndex == batteryArray.length) {
            isFrist = false;
            batteryIndex = 0;
        }
        return tem;
    }

}
