package com.example.a_eye.Support;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.example.a_eye.R;

public class Set_Dialog {
    private Context myContext;
    private AlertDialog dialog;
    public Set_Dialog(Context main) {
        myContext = main;
    }

    public void setup() {
        LayoutInflater inflater = (LayoutInflater)myContext.getApplicationContext().getSystemService(myContext.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.custom_alertdialog, null);
        ImageView customIcon = (ImageView)view.findViewById(R.id.cumtomdialogicon);
        AlertDialog.Builder builder = new AlertDialog.Builder(myContext);
        builder.setView(view);

        dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    public void show() {
        dialog.show();
    }

    public void dismiss() {
        dialog.dismiss();
    }
}
