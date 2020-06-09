package com.example.uiuc_toilet_android;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.List;

public class BathroomActivity extends AppCompatActivity implements OnMapReadyCallback {
    String BASE_URL = "https://uiuc-toilet.herokuapp.com";
    double latitude;
    double longitude;
    String gender;
    String id;
    private GoogleMap mMap;

    TextView bathroomName;
    TextView hours;
    TextView numRatings;
    TextView userRatingText;
    TextView commentsText;
    EditText addComment;
    ImageView back_button;
    RatingBar brRating;
    RatingBar userRating;
    double uRating;
    Button submit;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bathroom);

        Intent intent = getIntent();
        id = intent.getStringExtra("id");
        latitude = intent.getDoubleExtra("lat", 0.0);
        longitude = intent.getDoubleExtra("long", 0.0);
        gender = intent.getStringExtra("gender");


        bathroomName = findViewById(R.id.bathroomName);
        hours = findViewById(R.id.time);
        numRatings = findViewById(R.id.numRatings);
        brRating = findViewById(R.id.ratingBar);
        userRating = findViewById(R.id.set_ratingBar);
        userRatingText = findViewById(R.id.rating_text);
        commentsText = findViewById(R.id.comments);
        submit = findViewById(R.id.button_submit);
        addComment = findViewById(R.id.add_comment);

        getBathroom(id);

        back_button = findViewById(R.id.button_back);
        back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if(mapFragment != null){
            mapFragment.getMapAsync(this);
        }

        userRating.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                userRatingText.setText(String.valueOf(rating));
                uRating = userRating.getRating();
            }
        });

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                update(id, userRating.getRating(), addComment.getText().toString());
                Toast.makeText(getApplicationContext(), "Bathroom updated!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });



    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude),17));
        switch(gender){
            case "Male":
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(latitude, longitude))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                break;
            case "Female":
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(latitude, longitude))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));
                break;
            case "Both":
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(latitude, longitude))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                break;
        }
    }

    private void getBathroom(String id){
        JSONObject id_obj = new JSONObject();
        try{
            id_obj.put("id", id);
        } catch (Exception e){
            e.printStackTrace();
        }

        RequestQueue queue;
        queue = Volley.newRequestQueue(this);

        JsonObjectRequest stringRequest = new JsonObjectRequest(com.android.volley.Request.Method.GET, BASE_URL+"/bathroom/"+id, id_obj,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("TAG", response.toString());
                        try{
                            JSONObject bathroom = response.getJSONObject("bathroom");
                            String name = bathroom.getString("name");
                            String gender = bathroom.getString("gender");
                            bathroomName.setText(String.format("%s [%s]", name, gender));
                            String openTime = bathroom.getString("openTime");
                            String closeTime = bathroom.getString("closeTime");
                            hours.setText(String.format("%s-%s", openTime, closeTime));
                            float rating = BigDecimal.valueOf (bathroom.getDouble("rating")).floatValue();
                            brRating.setRating(rating);
                            int numRatings_int = bathroom.getInt("numRatings");
                            numRatings.setText(String.format("(%s)", String.valueOf(numRatings_int)));
                            String comments = bathroom.getString("comments");
                            if(comments.equals("")){
                                commentsText.setText(R.string.comments_null);
                            }else {
                                commentsText.setText(comments);
                            }
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

    private void update(String id, double rating, String comment){
        //update rating logic
        double num = Double.parseDouble(numRatings.getText().toString().replaceAll("[^0-9]", ""));
        double curr_rating = ((brRating.getRating()*num) + rating)/(1+ num);

        JSONObject id_obj = new JSONObject();
        try{
            id_obj.put("id", id);
            if(rating != 0){
                id_obj.put("rating", curr_rating);
                id_obj.put("numRatings", num+1);
            } else {
                id_obj.put("rating", brRating.getRating());
                id_obj.put("numRatings", num);
            }
            if(commentsText.getText().toString().equals(getString(R.string.comments_null))){
                id_obj.put("comments", comment);
            } else{
                id_obj.put("comments", commentsText.getText().toString() + "\n" + comment);
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        RequestQueue queue;
        queue = Volley.newRequestQueue(this);

        JsonObjectRequest stringRequest = new JsonObjectRequest(com.android.volley.Request.Method.PATCH, BASE_URL+"/bathroom/"+id, id_obj,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("TAG", response.toString());
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


