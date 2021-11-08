package com.example.itag.ui.distance;

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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.itag.MainActivity;
import com.example.itag.MyApp;
import com.example.itag.R;
import com.example.itag.databinding.FragmentDistanceBinding;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import cn.wch.ch34xuartdriver.CH34xUARTDriver;

public class DistanceFragment extends Fragment {

    private DistanceViewModel mViewModel;
    private FragmentDistanceBinding fragmentDistanceBinding;

    private static final String ACTION_USB_PERMISSION = "cn.wch.wchusbdriver.USB_PERMISSION";

    private EditText editText_distance;
    private Button button_distance_open, button_distance_clear;
    private TextView textView_distance_info;

    private boolean isOpen;
    private Handler handler;
    private Fragment fragment;
    public byte[] readBuffer;

    public int baudRate;
    public byte stopBit;
    public byte dataBit;
    public byte parity;
    public byte flowControl;

    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://ip/tag?useSSL=false&useUnicode=true&characterEncoding=UTF-8";
    static final String USER = "";
    static final String PASS = "";


    public static DistanceFragment newInstance() {
        return new DistanceFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_distance, container, false);

        com.example.itag.MyApp.driver = new CH34xUARTDriver((UsbManager) getActivity().getSystemService(Context.USB_SERVICE), getContext(), ACTION_USB_PERMISSION);
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);// 保持常亮的屏幕的状态

        ifUSB();

        editText_distance = (EditText) view.findViewById(R.id.editText_distance);
        button_distance_open = (Button) view.findViewById(R.id.button_distance_open);
        button_distance_clear = (Button) view.findViewById(R.id.button_distance_clear);
        textView_distance_info=view.findViewById(R.id.textView_distance_info);

        baudRate = 115200;
        stopBit = 1;
        dataBit = 8;
        parity = 0;
        flowControl = 0;

        readBuffer = new byte[512];
        isOpen = false;
        fragment = this;

        button_distance_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                editText_distance.setText("");
            }
        });

        button_distance_open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (openUSB()) {
                    new readThread().start();//开启线程
                }
            }
        });

        handler = new Handler(Looper.getMainLooper()) {

            public void handleMessage(Message msg) {
                editText_distance.append((String) msg.obj);
            }
        };
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(DistanceViewModel.class);
        // TODO: Use the ViewModel
    }

    private void ifUSB() {
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

    private boolean openUSB() {
        if (!isOpen) {
            int retVal = MyApp.driver.ResumeUsbList();  // ResumeUsbList方法用于枚举CH34X设备以及打开相关设备
            if (retVal == -1) {
                com.example.itag.MyApp.driver.CloseDevice();
                return false;
            } else if (retVal == 0) {
                if (!com.example.itag.MyApp.driver.UartInit()) {   //对串口设备进行初始化操作
                    com.example.itag.MyApp.driver.CloseDevice();
                    return false;
                } else {
                    isOpen = true;
                    button_distance_open.setText("Close");
                    MyApp.driver.SetConfig(baudRate, dataBit, stopBit, parity, flowControl);
                    return true;
                }
            } else {
                com.example.itag.MyApp.driver.CloseDevice();
                return false;
            }
        } else {
            com.example.itag.MyApp.driver.CloseDevice();
            button_distance_open.setText("Open");
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
                    if (receive.length() > 23) {
                        String subString = receive.substring(21, 23) + receive.substring(18, 20);
                        int dis = Integer.parseInt(hexTodec(subString));
                        try {
                            Class.forName(JDBC_DRIVER);
                            conn = DriverManager.getConnection(DB_URL, USER, PASS);
                        } catch (SQLException | ClassNotFoundException e) {
                            textView_distance_info.setText(e.getMessage());
                        }
                        try {
                            insert(dis, conn);
                        } catch (Exception e) {
                            textView_distance_info.setText(e.toString());
                        }
                        msg.obj = dis + "cm" +"\n";
                        handler.sendMessage(msg);
                    }
                }
            }
        }
    }

    private static void insert(int distance, Connection conn) throws SQLException {
        PreparedStatement pstmt;
        String sql = "insert into distance(distance) values(?)";
        pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, distance);
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
    public String hexTodec(String content) {
        int number = 0;
        String a = content.substring(3);
        String b = content.substring(2, 3);
        String c = content.substring(1, 2);
        number = charTonum(a) + charTonum(b) * 16 + charTonum(c) * 16 * 16;
        return Integer.toString(number);
    }

    //字符转数字
    public int charTonum(String chars) {
        int num = 0;
        if (chars.equals("a"))
            num = 10;
        else if (chars.equals("b"))
            num = 11;
        else if (chars.equals("c"))
            num = 12;
        else if (chars.equals("d"))
            num = 13;
        else if (chars.equals("e"))
            num = 14;
        else if (chars.equals("f"))
            num = 15;
        else
            num = Integer.parseInt(chars);
        return num;
    }
}

