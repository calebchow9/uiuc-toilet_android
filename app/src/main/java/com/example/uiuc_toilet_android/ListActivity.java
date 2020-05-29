package com.example.uiuc_toilet_android;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static android.location.Location.distanceBetween;

public class ListActivity extends AppCompatActivity{
    private String BASE_URL = "http://192.168.3.10:3000";
    RecyclerView recyclerView;
    ListAdapter adapter;
    RecyclerView.LayoutManager layoutManager;
    List<Bathroom> brList = new ArrayList<>();
    Bathroom deletedBR;
    boolean favorite = false;

    private double latitude;
    private double longitude;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        buildRecyclerView();
        if(getSupportActionBar() != null){
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.main)));
        }

//        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
//        String curre = sharedPref.getString("favorites", "");
//        SharedPreferences.Editor editor = sharedPref.edit();
//        editor.putString("favorites",  "");
//        editor.commit();

        final Intent intent = getIntent();
        //initialize bottom navigation bar
        final BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_list);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.favorite:
                Drawable drawable = item.getIcon();
                if(!favorite){
                    if(drawable != null){
                        drawable.mutate();
                        drawable.setColorFilter(getResources().getColor(R.color.female), PorterDuff.Mode.SRC_ATOP);
                    }
                    favorite = true;
                    getFavorites();
                    return true;
                }
                drawable.clearColorFilter();
                favorite = false;
                getBathrooms(latitude, longitude);
                return true;
            case R.id.search:
                item.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                    @Override
                    public boolean onMenuItemActionExpand(MenuItem menuItem) {
                        SearchView searchView = (SearchView) item.getActionView();
                        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                            @Override
                            public boolean onQueryTextSubmit(String query) {
                                brList.clear();
                                searchBathroom(query);
                                return false;
                            }
                            @Override
                            public boolean onQueryTextChange(String newText) {
                                return false;
                            }
                        });
                        return true;
                    }

                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                        getBathrooms(latitude, longitude);
                        return true;
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void buildRecyclerView() {
        recyclerView = findViewById(R.id.recyclerview);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        adapter = new ListAdapter(brList);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipe);
        itemTouchHelper.attachToRecyclerView(recyclerView);
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
                                String openTime = br.getString("openTime");
                                String closeTime = br.getString("closeTime");
                                float[] distance = new float[1];
                                distanceBetween(latitude, longitude, brLatitude, brLongitude, distance);
                                double locationDistance = distance[0];
                                Bathroom bathroom = new Bathroom (id, name, gender, openTime, closeTime, brLatitude, brLongitude, locationDistance);
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

    ItemTouchHelper.SimpleCallback swipe = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder, int direction) {

            final int position = viewHolder.getAdapterPosition();

            switch (direction) {
                case ItemTouchHelper.LEFT:
                    deletedBR = brList.get(position);
                    if(favorite){
                        unFavorite(deletedBR.getName());
                    }
                    brList.remove(position);
                    adapter.notifyItemRemoved(position);
                    Snackbar.make(recyclerView,"Hid " + deletedBR.getName(), Snackbar.LENGTH_LONG)
                            .setAction("Undo", new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    brList.add(position, deletedBR);
                                    adapter.notifyItemInserted(position);
                                }
                            }).show();
                    break;
                case ItemTouchHelper.RIGHT:
                    final String bathroomName = brList.get(position).getName();
                    SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                    String curr = sharedPref.getString("favorites", "");
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("favorites", curr + bathroomName + ",");
                    editor.commit();
                    Log.d("favorites", sharedPref.getString("Favorites", ""));

                    Snackbar.make(recyclerView, "Favorited " + brList.get(position).getName(), Snackbar.LENGTH_LONG)
                            .setAction("Undo", new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    unFavorite(bathroomName);
                                }
                            }).show();
                    adapter.notifyDataSetChanged();
                    break;
            }
        }
    };

    private void unFavorite(String bathroomName){
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        String curr = sharedPref.getString("favorites", "");
        Log.d("fav", curr);
        String[] favorites = curr.split(",");
        for(int i = 0; i < favorites.length; i++){
            if(favorites[i].equals(bathroomName)){
                favorites[i] = "";
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String s : favorites) {
            if(!s.equals("")) {
                sb.append(s).append(",");
            }
        }
        Log.d("removed", sb.toString());
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("favorites", sb.toString());
        editor.commit();
    }

    private void getFavorites(){
        brList.clear();
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        String curr = sharedPref.getString("favorites", "");
        String[] favorites = curr.split(",");
        for(String s : favorites){
            searchBathroom(s);
        }

    }

    private void searchBathroom(String bathroomName){
        JSONObject name = new JSONObject();
        try{
            name.put("name", bathroomName);
        } catch (Exception e){
            e.printStackTrace();
        }

        RequestQueue queue;
        queue = Volley.newRequestQueue(this);

        JsonObjectRequest stringRequest = new JsonObjectRequest(com.android.volley.Request.Method.POST, BASE_URL+"/bathroom/name", name,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("info", response.toString());
                        try{
                            String id = response.getString("_id");
                            String name = response.getString("name");
                            String gender = response.getString("gender");
                            double brLatitude = Double.parseDouble(response.getString("latitude"));
                            double brLongitude = Double.parseDouble(response.getString("longitude"));
                            String openTime = response.getString("openTime");
                            String closeTime = response.getString("closeTime");
                            float[] distance = new float[1];
                            distanceBetween(latitude, longitude, brLatitude, brLongitude, distance);
                            double locationDistance = distance[0];
                            Bathroom bathroom = new Bathroom (id, name, gender, openTime, closeTime, brLatitude, brLongitude, locationDistance);
                            brList.add(bathroom);
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
