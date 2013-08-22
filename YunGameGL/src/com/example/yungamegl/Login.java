package com.example.yungamegl;

import android.app.Activity;  
import android.content.Intent;  
import android.os.Bundle;  
import android.util.Log;
import android.view.View;  
import android.widget.Button;  
import android.widget.EditText;  
  
public class Login extends Activity {  
    /** Called when the activity is first created. */  
      
    private static Button loginButton;  
    private static Button cancelButton;  
    private static Button registerButton;  
    private static Button exitButton;  
    private ButtonListener bl = new ButtonListener();  
      
    private EditText et1;  
    private EditText et2;  
    private Intent intent;  
    @Override  
    public void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);  
        setContentView(R.layout.login);  
          
        //��ӵ�½��ť����  
        loginButton = (Button)findViewById(R.id.login_ok);  
        loginButton.setOnClickListener(bl);  
  
        //���ȡ����ť����  
        cancelButton = (Button)findViewById(R.id.login_reset);  
        cancelButton.setOnClickListener(bl);  
        
        //���ע�ᰴť����  
        registerButton = (Button)findViewById(R.id.register);  
        registerButton.setOnClickListener(bl);  
         
        //����˳���ť����  
        exitButton = (Button)findViewById(R.id.exit);   
        exitButton.setOnClickListener(bl);  
    }  
     private class ButtonListener implements View.OnClickListener {  
            public void onClick(View view) {  
                // TODO Auto-generated method stub  
                if(view == loginButton) {  
                    et1 = (EditText)findViewById(R.id.username_info);  
                    et2 = (EditText)findViewById(R.id.password_info);  
                    
                    Log.d("IN:", et1.getText().toString());
                    Log.d("IN:", et2.getText().toString());
                    
                    if((et1.getText().toString()).equals("") && (et2.getText().toString()).equals("")) {  
                        intent = new Intent();  
                          
                        //��Bundle�����ݵ�ǰActivity������  
                        Bundle bundle = new Bundle();  
                        bundle.putString("USERNAME", et1.getText().toString());  
                        intent.putExtras(bundle);  
                          
                        intent.setClass(Login.this, GameList.class);  
                          
                        //����Activity  
                        startActivity(intent);  
                    }else {  
                        intent = new Intent();  
                          
                        intent.setClass(Login.this, ErrorPage.class);  
                        //����Activity  
                        startActivity(intent);  
                    }  
                }else if(view == cancelButton) {  
                    intent = new Intent();  
                    //ͨ��Login�����������Login  
                    intent.setClass(Login.this, Login.class);  
                    //����Activity  
                    startActivity(intent);  
                }else if(view == registerButton) {  
                      
                }else if(view == exitButton) {  
                    finish();  
                }  
            }  
     }  
}  