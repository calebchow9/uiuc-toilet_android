package com.example.uiuc_toilet_android;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONObject;

import static android.location.Location.distanceBetween;

public class AddActivity extends AppCompatActivity implements OnMapReadyCallback {
    private String BASE_URL = "http://192.168.3.10:3000";
    private GoogleMap mMap;

    EditText name;
    EditText username;
    ImageView back_button;
    TextView location;
    Button male;
    boolean male_clicked = false;
    Button female;
    boolean female_clicked = false;
    EditText open;
    EditText close;
    Button create;

    double latitude;
    double longitude;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        Intent intent = getIntent();
        latitude = intent.getDoubleExtra("latitude", 0.0);
        longitude = intent.getDoubleExtra("longitude", 0.0);

        location = findViewById(R.id.location_text);
        String text = "(" + latitude + ", " + longitude + ")";
        location.setText(text);

        name = findViewById(R.id.name_text);
        username = findViewById(R.id.username_text);
        male = findViewById(R.id.male_button);
        female = findViewById(R.id.female_button);

        open = findViewById(R.id.open_text);
        close = findViewById(R.id.close_text);
        open.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(MotionEvent.ACTION_UP == event.getAction()){
                    TimePickerDialog timePickerDialog = new TimePickerDialog(AddActivity.this, new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker timePicker, int hourOfDay, int minutes) {
                            @SuppressLint("DefaultLocale") String text = String.format("%02d:%02d", hourOfDay, minutes);
                            open.setText(text);
                        }
                    }, 0, 0, false);
                    timePickerDialog.show();
                }
                return false;
            }
        });
        close.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(MotionEvent.ACTION_UP == event.getAction()){
                    TimePickerDialog timePickerDialog = new TimePickerDialog(AddActivity.this, new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker timePicker, int hourOfDay, int minutes) {
                            @SuppressLint("DefaultLocale") String text = String.format("%02d:%02d", hourOfDay, minutes);
                            close.setText(text);
                        }
                    }, 0, 0, false);
                    timePickerDialog.show();
                }
                return false;
            }
        });

        male.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if(!male_clicked){
                        male.setBackgroundTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.male));
                        male_clicked = true;
                        return;
                    }
                    male.setBackgroundTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.grey_text));
                    male_clicked = false;
                }
            }
        });
        female.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if(!female_clicked){
                        female.setBackgroundTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.female));
                        female_clicked = true;
                        return;
                    }
                    female.setBackgroundTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.grey_text));
                    female_clicked = false;
                }
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if(mapFragment != null){
            mapFragment.getMapAsync(this);
        }


        back_button = findViewById(R.id.button_back);
        back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        create = findViewById(R.id.button_add);
        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(male_clicked && female_clicked){
                    addBathroom(name.getText().toString(), latitude, longitude, "Both", open.getText().toString(), close.getText().toString(), username.getText().toString());
                } else if (male_clicked){
                    addBathroom(name.getText().toString(), latitude, longitude, "Male", open.getText().toString(), close.getText().toString(), username.getText().toString());
                } else if (female_clicked){
                    addBathroom(name.getText().toString(), latitude, longitude, "Female", open.getText().toString(), close.getText().toString(), username.getText().toString());
                }
                finish();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude),17));
        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(latitude, longitude)));
    }

    private void addBathroom(String name, double latitude, double longitude, String gender, String openTime, String closeTime, String username){
        JSONObject add = new JSONObject();
        try{
            add.put("name", name);
            add.put("latitude", latitude);
            add.put("longitude", longitude);
            add.put("gender", gender);
            add.put("openTime", openTime);
            add.put("closeTime", closeTime);
            add.put("username", username);
        } catch (Exception e){
            e.printStackTrace();
        }

        RequestQueue queue;
        queue = Volley.newRequestQueue(this);

        JsonObjectRequest stringRequest = new JsonObjectRequest(com.android.volley.Request.Method.POST, BASE_URL+"/bathroom", add,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("add", response.toString());
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        queue.add(stringRequest);
    }

}
