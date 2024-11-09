package com.example.safetymap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

//import com.example.safetmapapp.R;
import com.google.android.gms.maps.model.LatLng;

public class IntentFragment extends DialogFragment {

    private LatLng destination;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle b = getArguments();
        if (b != null) {
            destination = new LatLng(b.getDouble("lat"), b.getDouble("lng"));
        }

        LayoutInflater layoutInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.intent_fragment, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Oops!")
                .setMessage("Looks like we are missing some relevant data in that path. Your safest bet may be Uber or Google Maps")
                .setView(view)
                .setPositiveButton("Google Maps", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (destination != null) {
                            String uriString = "google.navigation:q=" + destination.latitude + "," + destination.longitude;
                            Uri gmmIntentUri = Uri.parse(uriString);
                            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                            mapIntent.setPackage("com.google.android.apps.maps");
                            if (mapIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                                startActivity(mapIntent);
                            }
                        }
                    }
                })
                .setNegativeButton("Uber", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            PackageManager pm = getActivity().getPackageManager();
                            pm.getPackageInfo("com.ubercab", PackageManager.GET_ACTIVITIES);
                            String uri = "uber://?action=setPickup&pickup=my_location&client_id=YOUR_CLIENT_ID";
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                            if (intent.resolveActivity(pm) != null) {
                                startActivity(intent);
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            // No Uber app! Open mobile website.
                            String url = "https://m.uber.com/sign-up?client_id=YOUR_CLIENT_ID";
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                                startActivity(intent);
                            }
                        }
                    }
                });

        return builder.create();
    }
}
