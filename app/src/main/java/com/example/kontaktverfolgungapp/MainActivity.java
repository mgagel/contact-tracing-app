package com.example.kontaktverfolgungapp;

import androidx.annotation.NonNull;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.example.kontaktverfolgungapp.dbclient.ClientApp;
import com.google.zxing.Result;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity {

//-------Gesture--------------------------------------------------------------------------------

    private float x1,x2;
    static final int MIN_DISTANCE = 50;


//-------Menu--------------------------------------------------------------------------------

    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;
    private EditText newcontactpopup_firstname, newcontactpopup_lastname;
    private Button button_cancel, button_save;

//-------Login--------------------------------------------------------------------------------

    private EditText newVorname, newNachname;
    private TextView textView;
    private Button  button_Save;

//-------QR-Code--------------------------------------------------------------------------------

    private CodeScanner mCodeScanner;
    boolean CameraPermission = false;
    final int CAMERA_PERM = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ClientApp.initServerConnection();

//-------Login--------------------------------------------------------------------------------

            dialogBuilder = new AlertDialog.Builder(this);
            final View loginPopupView = getLayoutInflater().inflate(R.layout.login_popup, null);
            newVorname = (EditText) loginPopupView.findViewById(R.id.vorname_text);
            newNachname = (EditText) loginPopupView.findViewById(R.id.nachname_text);

            textView = (TextView) loginPopupView.findViewById(R.id.loginTextView);

            button_Save = (Button) loginPopupView.findViewById(R.id.save_Button);

            //open Shared Pref File
            SharedPreferences mySPR = getSharedPreferences("Pref", 0);
            int UID = mySPR.getInt("UID", 0);

            //init dialogBuilder
            dialogBuilder.setCancelable(false);
            if (UID <= 0) {
                dialogBuilder.setView(loginPopupView);
                dialog = dialogBuilder.create();
                dialog.show();
            }

            button_Save.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String n = newVorname.getText().toString();
                    String ph = newNachname.getText().toString();


                    //validate names
                    if(newVorname.length()==0){
                        newVorname.setError("Vorname eingeben");
                    }
                    else if(!n.matches("[a-zA-z]+")){

                        newVorname.setError("Es sind nur Buchstaben erlaubt!");
                    }
                    else if(newNachname.length()==0){
                        newNachname.setError("Nachname eingebn");
                    }
                    else if(!ph.matches("[a-zA-z]+")){

                        newNachname.setError("Es sind nur Buchstaben erlaubt!");
                    }

                    else{
                        //open Shared Pref File
                        SharedPreferences mySPR = getSharedPreferences("Pref", 0);
                        //Editor Klasse initiaisieren
                        SharedPreferences.Editor editor = mySPR.edit();

                        editor.putString("vornameKey", n);
                        editor.putString("nachnameKey", ph);
                        //speichern
                        editor.commit();

                        //database integrated for newUser
                        int UID = ClientApp.newUser(n+" "+ph+" ");
                        if (UID == 0) {
                            Toast.makeText(MainActivity.this, "Nutzer konnte nicht abgespeichert werden.", Toast.LENGTH_LONG).show();
                        } else {
                            editor.putInt("UID", UID);

                            editor.commit();
                        }
                        //schließen des Fensters
                        dialog.dismiss();
                    }


                }
            });


            //test

//Values from file into text fields
            newVorname.setText(mySPR.getString("vornameKey", ""));
            newNachname.setText(mySPR.getString("nachnameKey", ""));

//-------QR-Code--------------------------------------------------------------------------------

        CodeScannerView scannerView =  findViewById(R.id.scanner_view);
        mCodeScanner = new CodeScanner(this,scannerView);


        // Only if the camera access is allowed, the QR code scanner will open.
        askPermission();
        if (CameraPermission) {

            //When a QR code is found, the frame is mapped there
            scannerView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    mCodeScanner.startPreview();
                }
            });

            mCodeScanner.setDecodeCallback(new DecodeCallback() {
                @Override
                public void onDecoded(@NonNull Result result) {


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            int PID= Integer.parseInt(String.valueOf(result));
                            SharedPreferences mySPR = getSharedPreferences("Pref", 0);
                            int UID = mySPR.getInt("UID", 0);
                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            Date date = new Date(result.getTimestamp());
                            String dateTime = simpleDateFormat.format(date);
                            ClientApp.scanQR(UID, PID, dateTime);
                            Toast.makeText(MainActivity.this, "QR-Code wurde erfolgreich gescannt.", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        }

    }




    private void askPermission(){

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERM);

            } else {
                mCodeScanner.startPreview();
                CameraPermission = true;
            }

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == CAMERA_PERM) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                mCodeScanner.startPreview();
                CameraPermission = true;
            }else {

                //If the camera access was denied, a dialog appears to ask for the camera access again.
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.CAMERA)){

                    new AlertDialog.Builder(this)

                            .setTitle("Erlaubnis")
                            .setMessage("Klicken Sie auf Fortfahren, um den Zugriff auf ihrer Kamera zu erlauben.")
                            .setPositiveButton("Fortfahren", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA},CAMERA_PERM);
                                }
                            }).setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            dialog.dismiss();

                        }
                    }).create().show();

                }else {
                    //If the user rejects the accesses again, he can quit the app
                    new AlertDialog.Builder(this)

                            .setTitle("Berechtigung")
                            .setMessage("Sie haben die Berechtigungen verweigert. Bitte erlauben Sie alle Berechtigungen unter den Einstellungen.")
                            .setPositiveButton("Einstellungen", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    //Guide the user through the application settings so that the user can grant permissions.
                                    dialog.dismiss();
                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.fromParts("package", getPackageName(), null));
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finish();

                                }
                            }).setNegativeButton("Nein, App beenden", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            dialog.dismiss();
                            finish();
                        }
                    }).create().show();

                }
            }

        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // when the app is running in the background, the resources are released
    @Override
    protected void onPause() {
        if (CameraPermission){
            mCodeScanner.releaseResources();
        }

        super.onPause();
    }


//-------Gesture--------------------------------------------------------------------------------

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        //  Returns the integer constants MotionEvent.ACTION_DOWN and MotionEvent.ACTION_UP
        switch(event.getAction())
        {
            // starting to swipe time gesture
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();  //Returns the x coordinate of the touch event
                break;

            // ending time swipe gesture
            case MotionEvent.ACTION_UP:
                x2 = event.getX();  //Returns the x coordinate of the touch event
                float deltaX = x2 - x1; //getting value for horizontal swipe
                if (Math.abs(deltaX) > MIN_DISTANCE)
                {
                    // Right to Left swipe action
                    if (x1 > x2)
                    {
                        Intent i = new Intent(MainActivity.this, Activity_Swipe_Left.class);
                        startActivity(i);
                    }
                }

                break;
        }
        return super.onTouchEvent(event);
    }

//-------Menu--------------------------------------------------------------------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflate = getMenuInflater();
        inflate.inflate(R.menu.example_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.item1){
            createNewContactDialog();
        }

        return super.onOptionsItemSelected(item);
    }

    public void createNewContactDialog (){

        dialogBuilder = new AlertDialog.Builder(this);
        final View contactPopupView = getLayoutInflater().inflate(R.layout.popup, null);
        newcontactpopup_firstname = (EditText) contactPopupView.findViewById(R.id.newcontactpopup_firstname);
        newcontactpopup_lastname = (EditText)  contactPopupView.findViewById(R.id.newcontactpopup_lastname);

        button_save = (Button) contactPopupView.findViewById(R.id.saveButton);
        button_cancel = (Button) contactPopupView.findViewById(R.id.cancelButton);

        dialogBuilder.setView(contactPopupView);
        dialog = dialogBuilder.create();
        dialog.show();
        //open Shared Pref File
        SharedPreferences mySPR = getSharedPreferences("Pref", 0);
        //Load saved names from file
        newcontactpopup_firstname.setText(mySPR.getString("vornameKey", ""));
        newcontactpopup_lastname.setText(mySPR.getString("nachnameKey", ""));



        button_save.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                String n = newcontactpopup_firstname.getText().toString();
                String ph = newcontactpopup_lastname.getText().toString();
                //check if first name input is empty
                if(newcontactpopup_firstname.length()==0){
                    newcontactpopup_firstname.setError("Vorname eingeben");
                }
                //check if special characters are included
                else if(!n.matches("[a-zA-z]+")){

                    newcontactpopup_firstname.setError("Es sind nur Buchstaben erlaubt!");
                }
                //check if last name input is empty
                else if(newcontactpopup_lastname.length()==0){
                    newcontactpopup_lastname.setError("Nachname eingebn");
                }
                //check if special characters are included
                else if(!ph.matches("[a-zA-z]+")){

                    newcontactpopup_lastname.setError("Es sind nur Buchstaben erlaubt!");
                }
                else{
                //open Shared Pref File
                SharedPreferences mySPR = getSharedPreferences("Pref", 0);
                //Editor Klasse initiaisieren
                SharedPreferences.Editor editor = mySPR.edit();

                editor.putString("vornameKey", n);
                editor.putString("nachnameKey", ph);
                    //speichern
                editor.commit();

                // database integrated for setName
                int UID = mySPR.getInt("UID", 0);
                if (UID != 0) {
                    ClientApp.setName(UID, n + " " + ph + ";");
                } else {
                    Toast.makeText(MainActivity.this, "Ihre UID konnte nicht abgerufen werden.", Toast.LENGTH_LONG).show();
                }

                //close
                dialog.dismiss();}


            }

        });


        
        button_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog.dismiss();
            }
        });
    }



}