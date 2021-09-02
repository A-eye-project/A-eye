package com.example.a_eye.Support;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.a_eye.R;

public class Set_Dialog {
    private Context myContext;
    private AlertDialog loadDialog;
    private AlertDialog resultDialog;
    private TextView resultText;
    private Handler handler = new Handler();

    public Set_Dialog(Context main) {
        myContext = main;
    }

    public void loding_setup() {
        LayoutInflater inflater = (LayoutInflater) myContext.getApplicationContext().getSystemService(myContext.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.loading_alertdialog, null);
        ImageView customIcon = (ImageView) view.findViewById(R.id.cumtomdialogicon);
        AlertDialog.Builder builder = new AlertDialog.Builder(myContext);
        builder.setView(view);

        loadDialog = builder.create();
        loadDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    public void result_setup() {
        LayoutInflater inflater = (LayoutInflater) myContext.getApplicationContext().getSystemService(myContext.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.result_alertdialog, null);
        TextView resultTitle = (TextView) view.findViewById(R.id.result_title);
        resultTitle.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);
        resultText = (TextView) view.findViewById(R.id.result_text);
        Button noBtn = (Button) view.findViewById(R.id.result_button);
        AlertDialog.Builder builder = new AlertDialog.Builder(myContext);
        builder.setView(view);

        resultDialog = builder.create();
        resultDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        noBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resultDialog.dismiss();
            }
        });
    }

    public void loading_show() {
        loadDialog.show();
    }

    public void loading_dismiss() {
        loadDialog.dismiss();
    }

    public void result_str(String str) {
        resultText.setText(str);
    }

    public void result_show() {
        resultDialog.show();
    }

    public void result_dismiss() {
        resultDialog.dismiss();
    }
}
