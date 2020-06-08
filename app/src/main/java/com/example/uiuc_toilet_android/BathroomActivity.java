package com.example.uiuc_toilet_android;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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

public class BathroomActivity extends AppCompatActivity implements OnMapReadyCallback {
    String BASE_URL = "https://uiuc-toilet.herokuapp.com";
    double latitude;
    double longitude;
    String id;
    private GoogleMap mMap;

    TextView bathroomName;
    TextView hours;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bathroom);

        Intent intent = getIntent();
        id = intent.getStringExtra("id");
        latitude = intent.getDoubleExtra("lat", 0.0);
        longitude = intent.getDoubleExtra("long", 0.0);

        bathroomName = findViewById(R.id.bathroomName);
        hours = findViewById(R.id.time);

        getBathroom(id);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if(mapFragment != null){
            mapFragment.getMapAsync(this);
        }



    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude),17));
        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(latitude, longitude)));
    }

    private void getBathroom(String id){
        JSONObject id_obj = new JSONObject();
        try{
            id_obj.put("_id", id);
        } catch (Exception e){
            e.printStackTrace();
        }

        RequestQueue queue;
        queue = Volley.newRequestQueue(this);

        JsonObjectRequest stringRequest = new JsonObjectRequest(com.android.volley.Request.Method.POST, BASE_URL+"/bathroom/"+id, id_obj,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("TAG", response.toString());
                        try{
                            String name = response.getString("name");
                            String gender = response.getString("gender");
                            bathroomName.setText(String.format("%s [%s]", name, gender));
                            String openTime = response.getString("openTime");
                            String closeTime = response.getString("closeTime");
                            hours.setText(String.format("%s-%s", openTime, closeTime));

                        } catch (Exception e){
                            e.printStackTrace();
                        }
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
