package com.example.uiuc_toilet_android;

import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.util.List;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.BRViewHolder> {
    private List<Bathroom> brList;


    public static class BRViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView distance;
        View male;
        View female;
        View status_bar;
        TextView hours;

        public BRViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.bathroom_name);
            distance = itemView.findViewById(R.id.distance);
            male = itemView.findViewById(R.id.male_bar);
            female = itemView.findViewById(R.id.female_bar);
            status_bar = itemView.findViewById(R.id.status_bar);
            hours = itemView.findViewById(R.id.hours_text);
        }

    }

    public ListAdapter(List<Bathroom> brList) {
        this.brList = brList;
    }

    @Override
    public BRViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bathroom_2, parent, false);
        return new BRViewHolder(v);
    }

    @Override
    public void onBindViewHolder(BRViewHolder holder, final int position) {
        Resources res = holder.itemView.getContext().getResources();
        Bathroom currentItem = brList.get(position);

        holder.name.setText(currentItem.getName());
        holder.distance.setText(parseDistance(currentItem.getDistanceFromUser()));

        String hours = currentItem.getOpenTime() + " - " + currentItem.getCloseTime();
        holder.hours.setText(hours);

        switch(currentItem.getGender()){
            case "Male":
                holder.male.setBackgroundColor(res.getColor(R.color.male));
                holder.male.setVisibility(View.VISIBLE);
                holder.female.setVisibility(View.INVISIBLE);
                break;
            case "Female":
                holder.male.setBackgroundColor(res.getColor(R.color.female));
                holder.male.setVisibility(View.VISIBLE);
                holder.female.setVisibility(View.INVISIBLE);
                break;
            case "Both":
                holder.male.setBackgroundColor(res.getColor(R.color.male));
                holder.female.setBackgroundColor(res.getColor(R.color.female));
                holder.male.setVisibility(View.VISIBLE);
                holder.female.setVisibility(View.VISIBLE);
                break;
            default:
        }

        if(currentItem.getStatus()){
            holder.status_bar.setBackgroundColor(res.getColor(R.color.open));
        } else{
            holder.status_bar.setBackgroundColor(res.getColor(R.color.closed));
        }
        holder.status_bar.setVisibility(View.VISIBLE);

//        holder.itemView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//            }
//        });
    }

    public static String parseDistance(double distance) {
        double distanceKM = distance/1000; //convert to km
        if (distanceKM >= 10) {
            return ">10km";
        } else if (distanceKM >= 1) {
            DecimalFormat df = new DecimalFormat("#.#");
            return df.format(distanceKM) + "km";
        } else {
            return Math.round(distance) + "m";
        }
    }

    @Override
    public int getItemCount() {
        return brList.size();
    }
}
