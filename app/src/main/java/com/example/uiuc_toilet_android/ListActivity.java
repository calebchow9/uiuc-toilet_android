package com.example.uiuc_toilet_android;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.icu.util.Calendar;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

import static android.location.Location.distanceBetween;

public class ListActivity extends AppCompatActivity{
    private String BASE_URL = "https://uiuc-toilet.herokuapp.com";
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

        final Intent intent = getIntent();
        //initialize bottom navigation bar
        final BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_list);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.nav_map:
                        startActivity(new Intent(getApplicationContext(), MapsActivity.class));
                        overridePendingTransition(0, 0);
                        return true;
                    case R.id.nav_list:
                        return true;
                    case R.id.nav_settings:
                        startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
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
                        drawable.setColorFilter(getResources().getColor(R.color.secondary), PorterDuff.Mode.SRC_ATOP);
                    }
                    favorite = true;
                    getFavorites();
                    setTitle("Favorites");
                    return true;
                }
                drawable.clearColorFilter();
                favorite = false;
                setTitle("Nearby");
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
                            //sort by distance away
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
                            //take first 20
                            if(brList.size() >20){
                                brList.subList(20, brList.size()).clear();
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
        public void onChildDraw (Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive){

            new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    .addSwipeRightBackgroundColor(Color.RED)
                    .addSwipeRightLabel("Favorite")
                    .setSwipeRightLabelColor(Color.WHITE)
                    .addSwipeRightActionIcon(R.drawable.ic_favorite_white_24dp)
                    .setSwipeRightLabelTextSize(TypedValue.COMPLEX_UNIT_SP, 14)
                    .addSwipeLeftBackgroundColor(Color.BLACK)
                    .addSwipeLeftLabel("Hide")
                    .setSwipeLeftLabelTextSize(TypedValue.COMPLEX_UNIT_SP, 14)
                    .setSwipeLeftLabelColor(Color.WHITE)
                    .addSwipeLeftActionIcon(R.drawable.ic_visibility)
                    .create()
                    .decorate();

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }

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
        Collections.sort(brList, new Comparator<Bathroom>() {
            @Override
            public int compare(Bathroom o1, Bathroom o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });

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
