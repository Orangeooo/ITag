package com.example.itag.ui.location;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.itag.MyApp;
import com.example.itag.R;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import cn.wch.ch34xuartdriver.CH34xUARTDriver;

public class LocationFragment extends Fragment {

    private LocationViewModel mViewModel;
    private static final String ACTION_USB_PERMISSION = "cn.wch.wchusbdriver.USB_PERMISSION";

    private EditText editText1,editText2,editText3,editText4,editText5,editText6,editText7;
    private Button button_location;

    public int baudRate;
    public byte stopBit;
    public byte dataBit;
    public byte parity;
    public byte flowControl;

    private boolean isOpen;
    private Handler handler;
    public byte[] readBuffer;

    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://ip/tag?useSSL=false&useUnicode=true&characterEncoding=UTF-8";
    static final String USER = "";
    static final String PASS = "";

    public static LocationFragment newInstance() {
        return new LocationFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view= inflater.inflate(R.layout.fragment_location, container, false);

        com.example.itag.MyApp.driver = new CH34xUARTDriver((UsbManager) getActivity().getSystemService(Context.USB_SERVICE), getContext(), ACTION_USB_PERMISSION);
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);// 保持常亮的屏幕的状态

        editText1=view.findViewById(R.id.editText1);
        editText2=view.findViewById(R.id.editText2);
        editText3=view.findViewById(R.id.editText3);
        editText4=view.findViewById(R.id.editText4);
        editText5=view.findViewById(R.id.editText5);
        editText6=view.findViewById(R.id.editText6);
        editText7=view.findViewById(R.id.editText7);
        button_location=view.findViewById(R.id.button_location);

        init();
        ifUSB();

        button_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(openUSB()){

                    new readThread().start();//开启线程

                }
            }
        });

        handler = new Handler(Looper.getMainLooper()) {

            public void handleMessage(Message msg) {
                String d1=msg.obj.toString().split("/")[0];
                String d2=msg.obj.toString().split("/")[1];
                String d3=msg.obj.toString().split("/")[2];

                editText4.setText(d1);
                editText5.setText(d2);
                editText6.setText(d3);

            }
        };

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(LocationViewModel.class);
        // TODO: Use the ViewModel
    }

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
        Fragment fragment = this;

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
                    button_location.setText("Close");
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
            button_location.setText("Open");
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
                    if(receive.length()>34) {
                        String string1 = receive.substring(21, 23) + receive.substring(18, 20);
                        String string2 = receive.substring(27, 29) + receive.substring(24, 26);
                        String string3 = receive.substring(33, 35) + receive.substring(30, 32);
                        int d1 = Integer.parseInt(hexTodec(string1));
                        int d2 = Integer.parseInt(hexTodec(string2));
                        int d3 = Integer.parseInt(hexTodec(string3));

                        String str1=editText1.getText().toString();
                        String str2=editText2.getText().toString();
                        String str3=editText3.getText().toString();

                        double x1=Double.parseDouble(str1.split("/")[0]);
                        double y1=Double.parseDouble(str1.split("/")[1]);
                        double x2=Double.parseDouble(str2.split("/")[0]);
                        double y2=Double.parseDouble(str2.split("/")[1]);
                        double x3=Double.parseDouble(str3.split("/")[0]);
                        double y3=Double.parseDouble(str3.split("/")[1]);

                        int []x={0,0};
                        double a11 = 2*(x1-x3);
                        double a12 = 2*(y1-y3);
                        double b1 = Math.pow(x1,2)-Math.pow(x3,2) +Math.pow(y1,2)-Math.pow(y3,2) +Math.pow(d3,2)-Math.pow(d1,2);
                        double a21 = 2*(x2-x3);
                        double a22 = 2*(y2-y3);
                        double b2 = Math.pow(x2,2)-Math.pow(x3,2) +Math.pow(y2,2)-Math.pow(y3,2) +Math.pow(d3,2)-Math.pow(d2,2);
                        x[0]= (int)((b1*a22-a12*b2)/(a11*a22-a12*a21));
                        x[1]= (int)((a11*b2-b1*a21)/(a11*a22-a12*a21));

                        String location=x[0]+"/"+x[1];
                        editText7.setText(location);

                        try {
                            Class.forName(JDBC_DRIVER);
                            conn = DriverManager.getConnection(DB_URL,USER,PASS);
                        } catch (SQLException e) {
                            // TODO Auto-generated catch block
                            System.out.println(e.getMessage());
                            editText7.setText(e.getMessage());
                        } catch (ClassNotFoundException e) {
                            // TODO Auto-generated catch block
                            System.out.println(e.getMessage());
                            editText7.setText(e.getMessage());
                        }
                        try {
                            insert(location, conn);
                        } catch (Exception e) {
                            System.out.println(e);
                            editText7.setText(String.valueOf(e));
                        }

                        msg.obj = d1 +"/"+d2+"/"+d3+"\n";
                        handler.sendMessage(msg);
                    }
                }
            }
        }
    }

    private static void insert(String location, Connection conn) throws SQLException {
        PreparedStatement pstmt;
        String sql = "insert into locate(location) values(?)";
        pstmt=conn.prepareStatement(sql);
        pstmt.setString(1,location);
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