package com.example.ueda.test1;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

/**
 * Created by ueda on 2015/05/12.
 */
public class PanoramaViewActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Toast.makeText(this,"test",Toast.LENGTH_SHORT).show();
        Button btn = new Button(this);
        btn.setText("testactivity");
        btn.setBackgroundColor(Color.rgb(0,100,200));
        setContentView(btn);
    }
}
