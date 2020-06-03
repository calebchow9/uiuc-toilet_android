package com.example.uiuc_toilet_android;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.icu.util.Calendar;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.location.Location.distanceBetween;
import static com.example.uiuc_toilet_android.ListAdapter.parseDistance;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {
    private String BASE_URL = "https://uiuc-toilet.herokuapp.com";
    private List<Bathroom> brList = new ArrayList<>();
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private final LatLng mDefaultLocation = new LatLng(40.107861, -88.227225);
    private static final int DEFAULT_ZOOM = 18;
    private static final String TAG = "tag";
    boolean init_zoom = true;

    private double latitude;
    private double longitude;

    double prev_latitude;
    double prev_longitude;

    private CameraPosition mCameraPosition;
    private Location mLastKnownLocation;
    private boolean mLocationPermissionGranted;
    private boolean requestingLocationUpdates;
    private LocationRequest locationRequest;

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";
    private static final String REQUESTING_LOCATION_UPDATES_KEY = "key";

    FloatingActionButton floatingActionButton;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        setupMapIfNeeded();

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        requestingLocationUpdates = true;

        floatingActionButton = findViewById(R.id.floatingActionButton);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent send = new Intent(getApplicationContext(), AddActivity.class);
                send.putExtra("latitude", latitude);
                send.putExtra("longitude", longitude);
                startActivity(send);
            }
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_map);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.nav_map:
                        return true;
                    case R.id.nav_list:
                        startActivity(new Intent(getApplicationContext(), ListActivity.class));
                        overridePendingTransition(0, 0);
                        return true;
                    case R.id.nav_settings:
                        startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                        return true;
                }
                return false;
            }
        });
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        getLocationPermission();
        updateLocationUI();
        getInitialDeviceLocation();

        MapStateManager mgr = new MapStateManager(this);
        CameraPosition position = mgr.getSavedCameraPosition();
        if (position != null) {
            CameraUpdate update = CameraUpdateFactory.newCameraPosition(position);
            mMap.moveCamera(update);

            mMap.setMapType(mgr.getSavedMapType());
        }
    }

    private void getInitialDeviceLocation() {
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            if (mLastKnownLocation != null) {
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()), 18));
                            }
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getDeviceLocation(final Float zoom) {
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // If user moved, move the camera to center on the new location
                            mLastKnownLocation = task.getResult();
                            if (mLastKnownLocation != null) {
                                if(Math.abs(mLastKnownLocation.getLatitude() - prev_latitude) > 0.00004491576 || Math.abs(mLastKnownLocation.getLongitude() - prev_longitude) > 0.00004491576){ // >5m
                                    Log.d("tag", "moved");
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()), zoom));
                                    getMarkers(latitude, longitude);
                                }
                            }
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true;
            }
        }
    }

    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                mMap.getUiSettings().setMapToolbarEnabled(false);
//                mMap.setPadding(0, 120, 0, 0);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getMarkers(final double latitude, final double longitude){
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
                                String openTime = br.getString("openTime");
                                String closeTime = br.getString("closeTime");
                                float[] distance = new float[1];
                                distanceBetween(latitude, longitude, brLatitude, brLongitude, distance);
                                double locationDistance = distance[0];
                                boolean status = false;
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                    Calendar rightNow = Calendar.getInstance();
                                    int hour = rightNow.get(Calendar.HOUR_OF_DAY);
                                    int minute = rightNow.get(Calendar.MINUTE);
                                    double now = hour + (minute*0.01);
                                    openTime = openTime.replace(":", ".");
                                    closeTime = closeTime.replace(":", ".");
                                    double open = Double.parseDouble(openTime);
                                    double close = Double.parseDouble(closeTime);
                                    if(now >= open && now <= close){
                                        status = true;
                                    }
                                }
                                Bathroom bathroom = new Bathroom (id, name, gender, openTime, closeTime, brLatitude, brLongitude, locationDistance, status);
                                brList.add(bathroom);
                            }
                            Collections.sort(brList, new Comparator<Bathroom>() {
                                @Override
                                public int compare(Bathroom z1, Bathroom z2) {
                                    if (z1.getDistanceFromUser() > z2.getDistanceFromUser())
                                        return 1;
                                    if (z1.getDistanceFromUser() < z2.getDistanceFromUser())
                                        return -1;
                                    return 0;
                                }
                            });
                            //first 20
                            if(brList.size() >20){
                                brList.subList(20, brList.size()).clear();
                            }
                            for(int i = 0; i < brList.size(); i++){
                                switch(brList.get(i).getGender()){
                                    case "Male":
                                        mMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(brList.get(i).getLatitude(), brList.get(i).getLongitude()))
                                                .title(parseBathroomTitle(brList.get(i)))
                                                .snippet(parseBathroomInfo(brList.get(i)))
                                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                                        break;
                                    case "Female":
                                        mMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(brList.get(i).getLatitude(), brList.get(i).getLongitude()))
                                                .title(parseBathroomTitle(brList.get(i)))
                                                .snippet(parseBathroomInfo(brList.get(i)))
                                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));
                                        break;
                                    case "Both":
                                        mMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(brList.get(i).getLatitude(), brList.get(i).getLongitude()))
                                                .title(parseBathroomTitle(brList.get(i)))
                                                .snippet(parseBathroomInfo(brList.get(i)))
                                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                                        break;
                                    default:
                                        mMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(brList.get(i).getLatitude(), brList.get(i).getLongitude()))
                                                .title(parseBathroomTitle(brList.get(i)))
                                                .snippet(parseBathroomInfo(brList.get(i)))
                                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                                }
                            }

                        }
                        catch (Exception e){
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

    @Override
    protected void onResume() {
        super.onResume();
        if (requestingLocationUpdates) {
            startLocationUpdates();
        }
    }
    private void startLocationUpdates() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        mFusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        onLocationChanged(locationResult.getLastLocation());
                    }
                },
                Looper.myLooper());
    }

    public void onLocationChanged(Location location) {
        String msg = "Updated Location: " + location.getLatitude() + "," + location.getLongitude();
        Log.d("location", msg);
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        prev_latitude = latitude;
        prev_longitude = longitude;
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        if(init_zoom){
            init_zoom = false;
            getDeviceLocation((float)18.5);
            return;
        }
        getDeviceLocation(mMap.getCameraPosition().zoom);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MapStateManager mgr = new MapStateManager(this);
        mgr.saveMapState(mMap);
    }

    private String parseBathroomTitle(Bathroom br){
        if(br.getStatus()){
            return br.getName() + " [OPEN]";
        }
        return br.getName() + " [CLOSED]";
    }

    private String parseBathroomInfo(Bathroom br){
        DecimalFormat df = new DecimalFormat("#.#");
        return  "Distance: " + parseDistance(Double.parseDouble(df.format(br.getDistanceFromUser())));
    }

    private void setupMapIfNeeded() {
        if (mMap == null) {
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }
    }


}
