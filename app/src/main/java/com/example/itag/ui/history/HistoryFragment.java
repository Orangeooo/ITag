package com.example.itag.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.itag.R;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class HistoryFragment extends Fragment {

    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://ip/tag?useSSL=false&useUnicode=true&characterEncoding=UTF-8";
    static final String USER = "";
    static final String PASS = "";

    static TextView textView;
    static Button button,button2;
    static TextView editText2,editText3;

    private HistoryViewModel mViewModel;

    public static HistoryFragment newInstance() {
        return new HistoryFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view= inflater.inflate(R.layout.fragment_history, container, false);

        textView=view.findViewById(R.id.editText);
        editText2=view.findViewById(R.id.editText2);
        editText3=view.findViewById(R.id.editText3);

        button=view.findViewById(R.id.button);
        button2=view.findViewById(R.id.button2);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(runnable).start();
            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(runnable2).start();

            }
        });
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(HistoryViewModel.class);
        // TODO: Use the ViewModel
    }

    Runnable runnable = new Runnable() {

        private Connection conn = null;
        @Override
        public void run() {
            // TODO Auto-generated method stub
            try {
                Class.forName(JDBC_DRIVER);
                conn = DriverManager.getConnection(DB_URL,USER,PASS);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                System.out.println(e.getMessage());
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                System.out.println(e.getMessage());
            }
            try {
                //insert(888,60, conn);
                select(conn);
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    };

    Runnable runnable2 = new Runnable() {

        private Connection conn = null;
        @Override
        public void run() {
            // TODO Auto-generated method stub
            try {
                Class.forName(JDBC_DRIVER);
                conn = DriverManager.getConnection(DB_URL,USER,PASS);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                System.out.println(e.getMessage());
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                System.out.println(e.getMessage());
            }
            try {
                insert(666,60, conn);
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    };

    private static void insert(int distance, int compass, Connection conn) throws SQLException {
        PreparedStatement pstmt;
        String sql = "insert into tag1(distance,compass) values(?,?)";
        pstmt=conn.prepareStatement(sql);

        distance=Integer.parseInt(editText2.getText().toString());
        compass=Integer.parseInt(editText3.getText().toString());
        pstmt.setInt(1,distance);
        pstmt.setInt(2,compass);

        pstmt.executeUpdate();
        System.out.println("插入成功！");
        pstmt.close();
        conn.close();
    }

    private static void select(Connection conn) throws SQLException{

        Statement stmt = conn.createStatement();
        String sql="select * from tag1";
        ResultSet res = stmt.executeQuery(sql);
        StringBuffer stringBuffer=new StringBuffer(100);

        while(res.next()){
            System.out.println("id:"+res.getInt("id") + "\\"
                    + res.getDate("time") +"\\"
                    + res.getTime("time") + "\\"
                    + res.getInt("distance") + "cm\\"
                    + res.getInt("compass") + "°\\");

            String i="id:"+res.getInt("id") + "\t";
            String dat=res.getDate("time") +"\t";
            String tim=res.getTime("time")+"\t";
            String dis=res.getInt("distance") + "cm"+"\t";
            String com=res.getInt("compass") + "°";
            stringBuffer.append(i+dat+tim+dis+com+"\n");
            textView.setText(stringBuffer);
        }
        res.close();
        stmt.close();
        conn.close();
    }

}