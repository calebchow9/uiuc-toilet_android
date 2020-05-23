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

import java.util.List;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.BRViewHolder> {
    private List<Bathroom> brList;


    public static class BRViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView distance;
        ImageView male;
        ImageView female;

        public BRViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.bathroom_name);
            distance = itemView.findViewById(R.id.distance);
            male = itemView.findViewById(R.id.ic_male);
            female = itemView.findViewById(R.id.ic_female);
        }

    }

    public ListAdapter(List<Bathroom> brList) {
        this.brList = brList;
    }

    @Override
    public BRViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bathroom, parent, false);
        return new BRViewHolder(v);
    }

    @Override
    public void onBindViewHolder(BRViewHolder holder, final int position) {
        Resources res = holder.itemView.getContext().getResources();
        Bathroom currentItem = brList.get(position);

        holder.name.setText(currentItem.getName());
        holder.distance.setText(parseDistance(currentItem.getDistanceFromUser()));

        switch(currentItem.getGender()){
            case "male":
                holder.male.setColorFilter(res.getColor(R.color.male));
                break;
            case "female":
                holder.female.setColorFilter(res.getColor(R.color.female));
            case "both":
                holder.male.setColorFilter(res.getColor(R.color.male));
                holder.female.setColorFilter(res.getColor(R.color.female));
            default:
        }

        //on click, open specific SpaceActivity page
//        holder.itemView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent openSpace = new Intent(v.getContext(), SpaceActivity.class);
//                Bathroom br = brList.get(position);
//                String spaceString = new Gson().toJson(space);
//                Log.d("export", spaceString);
//                openSpace.putExtra("space", spaceString);
//                v.getContext().startActivity(openSpace);
//            }
//        });
    }

    private static String parseDistance(double distance) {
        double distanceKM = distance/1000; //convert to km
        if (distanceKM >= 1000) {
            return ">999KM";
        } else if (distanceKM >= 1) {
            return  distanceKM + "km away";
        } else {
            return Math.round(distance) + "m away";
        }
    }

    @Override
    public int getItemCount() {
        return brList.size();
    }
}
