package com.example.uiuc_toilet_android;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.location.Location.distanceBetween;

public class ListActivity extends AppCompatActivity{
    private String BASE_URL = "http://192.168.3.10:3000";
    RecyclerView recyclerView;
    ListAdapter adapter;
    RecyclerView.LayoutManager layoutManager;
    List<Bathroom> brList = new ArrayList<>();

    private double latitude;
    private double longitude;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        buildRecyclerView();

        //initialize bottom navigation bar
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_map);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.nav_map:
                        finish();
                        overridePendingTransition(0, 0);
                        return true;
                    case R.id.nav_list:
                        return true;
                }
                return false;
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ListActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }else{
            LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
            if(lm != null){
                Location curr = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                if(curr != null){
                    latitude = curr.getLatitude();
                    longitude = curr.getLongitude();
                }
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000 , 5, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        getBathrooms(location.getLatitude(), location.getLongitude());
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {

                    }

                    @Override
                    public void onProviderEnabled(String provider) {

                    }

                    @Override
                    public void onProviderDisabled(String provider) {

                    }
                });
            }
        }

        getBathrooms(latitude, longitude);

    }

    private void buildRecyclerView() {
        recyclerView = findViewById(R.id.recyclerview);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        adapter = new ListAdapter(brList);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
    }

    private void getBathrooms(final double latitude, final double longitude){
        RequestQueue queue;
        queue = Volley.newRequestQueue(this);

        JsonArrayRequest stringRequest = new JsonArrayRequest(com.android.volley.Request.Method.GET, BASE_URL+"/bathroom", null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.d("info", response.toString());
                        brList.clear();
                        try{
                            for(int i = 0; i < response.length(); i++){
                                JSONObject br = response.getJSONObject(i);
                                String id = br.getString("_id");
                                String name = br.getString("name");
                                String gender = br.getString("gender");
                                double brLatitude = Double.parseDouble(br.getString("latitude"));
                                double brLongitude = Double.parseDouble(br.getString("longitude"));
                                double openTime = br.getDouble("openTime");
                                double closeTime = br.getDouble("closeTime");
                                float[] distance = new float[1];
                                distanceBetween(latitude, longitude, brLatitude, brLongitude, distance);
                                double locationDistance = distance[0];
                                Bathroom bathroom = new Bathroom (id, name, gender, brLatitude, brLongitude, openTime, closeTime, locationDistance);
                                brList.add(bathroom);
                            }
                            adapter.notifyDataSetChanged();
                        }
                        catch (Exception e){
                            e.printStackTrace();
                            adapter.notifyDataSetChanged();
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
