package com.kapstranspvtltd.kaps_partner.goods_driver_activities.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;

import com.kapstranspvtltd.kaps_partner.goods_driver_activities.NewLiveRideActivity;
import com.kapstranspvtltd.kaps_partner.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingsFragment extends Fragment {


    public SettingsFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        // Find the LinearLayout
        LinearLayout liveRideLayout = view.findViewById(R.id.live_ride);

        // Set click listener
        liveRideLayout.setOnClickListener(v -> navigateToLiveRide());

        return view;
    }

    private void navigateToLiveRide() {
        Intent intent = new Intent(requireContext(), NewLiveRideActivity.class);
        intent.putExtra("FromFCM", false);
        startActivity(intent);
    }
}