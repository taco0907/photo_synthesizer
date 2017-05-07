package com.example.ueda.test1;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Created by ueda on 2015/04/12.
 */
public class LogView extends ScrollView {

    private TextView m_text;

    public LogView(Context context, AttributeSet attrs) {
        super(context, attrs);

        m_text = new TextView(context);
        m_text.setBackgroundResource(android.R.color.darker_gray);
        m_text.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        this.addView(m_text);
    }

    public void append(CharSequence rv_text) {
        m_text.append(rv_text);
        m_text.append(System.getProperty("line.separator"));
        fullScroll(FOCUS_DOWN);
    }


}
