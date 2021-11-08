package com.example.itag.ui.follow;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.itag.MainActivity;
import com.example.itag.MyApp;
import com.example.itag.R;
import com.example.itag.databinding.FragmentFollowBinding;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import cn.wch.ch34xuartdriver.CH34xUARTDriver;

public class FollowFragment extends Fragment {

    private FollowViewModel mViewModel;
    private FragmentFollowBinding fragmentFollowBinding;


    private static final String ACTION_USB_PERMISSION = "cn.wch.wchusbdriver.USB_PERMISSION";
    private EditText readText;
    private boolean isOpen;
    private Handler handler;
    private MainActivity activity;
    private Fragment fragment;
    private Button openButton, clearButton;
    public byte[] readBuffer;
    public int baudRate;
    public byte stopBit;
    public byte dataBit;
    public byte parity;
    public byte flowControl;

    private TextView textView_follow_info,textCompass;


    private SensorManager sm=null;
    private Sensor aSensor=null;
    private Sensor mSensor=null;
    float[] accelerometerValues=new float[3];
    float[] magneticFieldValues=new float[3];
    float[] values=new float[3];
    float[] rotate=new float[9];

    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://ip/tag?useSSL=false&useUnicode=true&characterEncoding=UTF-8";
    static final String USER = "";
    static final String PASS = "";


    public static FollowFragment newInstance() {
        return new FollowFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.fragment_follow, container, false);


        textView_follow_info=view.findViewById(R.id.textView_follow_info);

        textCompass=view.findViewById(R.id.textView_compass);

        sm=(SensorManager)getActivity().getSystemService(Context.SENSOR_SERVICE);
        aSensor=sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensor=sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sm.registerListener(myListener, aSensor, SensorManager.SENSOR_DELAY_GAME);
        sm.registerListener(myListener, mSensor, SensorManager.SENSOR_DELAY_GAME);



        com.example.itag.MyApp.driver = new CH34xUARTDriver((UsbManager) getActivity().getSystemService(Context.USB_SERVICE), getContext(), ACTION_USB_PERMISSION);
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);// 保持常亮的屏幕的状态
        readText = (EditText) view.findViewById(R.id.editText_follow);
        openButton = (Button) view.findViewById(R.id.button_follow_open);
        clearButton = (Button) view.findViewById(R.id.button_follow_close);
        init();
        ifUSB();


        //打开流程主要步骤为ResumeUsbList，UartInit
        openButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if(openUSB()){

                    new readThread().start();//开启线程

                }
            }
        });

        handler = new Handler(Looper.getMainLooper()) {

            public void handleMessage(Message msg) {

                readText.append((String) msg.obj);

            }
        };
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(FollowViewModel.class);
        // TODO: Use the ViewModel
    }

    final SensorEventListener myListener=new SensorEventListener(){

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO Auto-generated method stub
            if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
                accelerometerValues=event.values;
            }
            if(event.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD){
                magneticFieldValues=event.values;
            }

            SensorManager.getRotationMatrix(rotate, null, accelerometerValues, magneticFieldValues);
            SensorManager.getOrientation(rotate, values);
            //经过SensorManager.getOrientation(rotate, values);得到的values值为弧度

            values[0]=(float) Math.toDegrees(values[0]);  //转换为角度
            //int azi=(int ) Math.toDegrees(values[0]);
            //textCompass.setText(String.valueOf((int)values[0]));
        }};



    //处理界面
    private void init() {


        //默认波特率115200
        baudRate = 115200;
        /* default is stop bit 1 */
        stopBit = 1;
        /* default data bit is 8 bit */
        dataBit = 8;
        /* parity default is none */
        parity = 0;
        /* default flow control is is none */
        flowControl = 0;

        readBuffer = new byte[512];
        isOpen = false;
        fragment = this;

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                readText.setText("");
            }
        });
        return;
    }

    private void ifUSB(){
        if (!com.example.itag.MyApp.driver.UsbFeatureSupported())// 判断系统是否支持USB HOST
        {
            Dialog dialog = new AlertDialog.Builder(getContext()).setTitle("提示").setMessage("您的手机不支持USB HOST，请更换其他手机再试！").setPositiveButton
                    ("确认", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            System.exit(0);
                        }
                    }).create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }
    }

    private boolean openUSB(){
        if (!isOpen) {
            int retVal = MyApp.driver.ResumeUsbList();  // ResumeUsbList方法用于枚举CH34X设备以及打开相关设备
            if (retVal == -1)
            {
                com.example.itag.MyApp.driver.CloseDevice();
                return false;
            }
            else if (retVal == 0){
                if (!com.example.itag.MyApp.driver.UartInit()) {   //对串口设备进行初始化操作
                    com.example.itag.MyApp.driver.CloseDevice();
                    return false;
                }
                else {
                    isOpen = true;
                    openButton.setText("Close");
                    MyApp.driver.SetConfig(baudRate, dataBit, stopBit, parity, flowControl);
                    return true;
                }
            }
            else {
                com.example.itag.MyApp.driver.CloseDevice();
                return false;
            }
        }
        else {
            com.example.itag.MyApp.driver.CloseDevice();
            openButton.setText("Open");
            isOpen = false;
            return false;
        }
    }

    private class readThread extends Thread {
        private Connection conn = null;
        public void run() {
            byte[] buffer = new byte[64];
            while (true) {
                Message msg = Message.obtain();
                if (!isOpen) {
                    break;
                }
                int length = com.example.itag.MyApp.driver.ReadData(buffer, 64);
                if (length > 0) {
                    String receive = toHexString(buffer, length);
                    if(receive.length()>23) {

                        String subString = receive.substring(21, 23) + receive.substring(18, 20);
                        int dis = Integer.valueOf(hexTodec(subString));
                        int azh=(int)values[0];

                        if(azh<0){
                            azh=azh+360;
                        }

                        try {
                            Class.forName(JDBC_DRIVER);
                            conn = DriverManager.getConnection(DB_URL,USER,PASS);
                        } catch (SQLException e) {
                            // TODO Auto-generated catch block
                            System.out.println(e.getMessage());
                            textView_follow_info.setText(e.getMessage());
                        } catch (ClassNotFoundException e) {
                            // TODO Auto-generated catch block
                            System.out.println(e.getMessage());
                            textView_follow_info.setText(e.getMessage());
                        }
                        try {
                            insert(dis,azh, conn);
                        } catch (Exception e) {
                            System.out.println(e);
                            textView_follow_info.setText(String.valueOf(e));
                        }

                        msg.obj = dis +"cm"+ azh+"°"+"\n";
                        handler.sendMessage(msg);
                    }
                }
            }
        }
    }

    private static void insert(int distance, int compass, Connection conn) throws SQLException {
        PreparedStatement pstmt;
        String sql = "insert into tag1(distance,compass) values(?,?)";
        pstmt=conn.prepareStatement(sql);
        //distance=Integer.parseInt(editText2.getText().toString());
        //compass=Integer.parseInt(editText3.getText().toString());
        pstmt.setInt(1,distance);
        pstmt.setInt(2,compass);

        pstmt.executeUpdate();
        System.out.println("插入成功！");
        pstmt.close();
        conn.close();
    }

    private String toHexString(byte[] arg, int length) {
        String result = new String();
        if (arg != null) {
            for (int i = 0; i < length; i++) {
                result = result
                        + (Integer.toHexString(
                        arg[i] < 0 ? arg[i] + 256 : arg[i]).length() == 1 ? "0"
                        + Integer.toHexString(arg[i] < 0 ? arg[i] + 256
                        : arg[i])
                        : Integer.toHexString(arg[i] < 0 ? arg[i] + 256
                        : arg[i])) + " ";
            }
            return result;
        }
        return "";
    }
    //16进制转10进制
    public String hexTodec(String content){
        int number=0;
        String a=content.substring(3);
        String b=content.substring(2,3);
        String c=content.substring(1,2);
        number=charTonum(a)+charTonum(b)*16+charTonum(c)*16*16;
        return Integer.toString(number);
    }
    //字符转数字
    public int charTonum(String chars){
        int num=0;
        if(chars.equals("a"))
            num=10;
        else if(chars.equals("b"))
            num=11;
        else if(chars.equals("c"))
            num=12;
        else if(chars.equals("d"))
            num=13;
        else if(chars.equals("e"))
            num=14;
        else if(chars.equals("f"))
            num=15;
        else
            num=Integer.parseInt(chars);
        return num;
    }

}