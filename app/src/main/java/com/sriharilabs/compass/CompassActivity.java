package com.sriharilabs.compass;

import android.Manifest;
import android.content.IntentSender;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.util.Log;
import android.widget.TextView;

import com.github.jksiezni.permissive.PermissionsGrantedListener;
import com.github.jksiezni.permissive.PermissionsRefusedListener;
import com.github.jksiezni.permissive.Permissive;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;


public class CompassActivity extends AppCompatActivity {

    private static final String TAG = "CompassActivity";

    private Compass compass;
    private ImageView mLockUnloackImg;
    private TextView latTxt, lngTxt, cityTxt, textViewAds;
    private boolean isLock = false;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private InterstitialAd mInterstitialAd;
    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-5957117184239951/4531208310");
        mInterstitialAd.loadAd(new AdRequest.Builder().build());

        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        compass = new Compass(this);
        compass.arrowView = (ImageView) findViewById(R.id.main_image_hands);
        compass.textView = (TextView) findViewById(R.id.textView);
        mLockUnloackImg = (ImageView) findViewById(R.id.imageViewLock);
        latTxt = (TextView) findViewById(R.id.textViewLat);
        lngTxt = (TextView) findViewById(R.id.textViewLng);
        cityTxt = (TextView) findViewById(R.id.textViewCity);
        textViewAds = (TextView) findViewById(R.id.textViewAds);

        mLockUnloackImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isLock) {
                    isLock = false;
                    compass.start();
                    mLockUnloackImg.setImageResource(R.mipmap.unlock);
                } else {
                    isLock = true;
                    compass.stop();
                    mLockUnloackImg.setImageResource(R.mipmap.lock);
                }
            }
        });


        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();

// ...

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize
                // location requests here.
                // ...
                startLocationUpdates();
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(CompassActivity.this,
                                1011);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    // ...
                    latTxt.setText("" + getFilterDouble(location.getLatitude()));
                    lngTxt.setText("" + getFilterDouble(location.getLongitude()));

                    try {
                        Geocoder geocoder = new Geocoder(CompassActivity.this, Locale.getDefault());
                        List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location
                                .getLongitude(), 1);
                        String cityName = addresses.get(0).getLocality();
                        cityTxt.setText(cityName);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            ;
        };

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                // Load the next interstitial.
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }

        });
        textViewAds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                } else {
                    Log.d("TAG", "The interstitial wasn't loaded yet.");
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "start compass");
        compass.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        compass.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        compass.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "stop compass");
        compass.stop();
    }

    private void startLocationUpdates() {
        new Permissive.Request(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                .whenPermissionsGranted(new PermissionsGrantedListener() {
                    @Override
                    public void onPermissionsGranted(String[] permissions) throws SecurityException {
                        // given permissions are granted
                        mFusedLocationClient.requestLocationUpdates(createLocationRequest(),
                                mLocationCallback,
                                null /* Looper */);
                    }
                })
                .whenPermissionsRefused(new PermissionsRefusedListener() {
                    @Override
                    public void onPermissionsRefused(String[] permissions) {
                        // given permissions are refused
                    }
                })
                .execute(CompassActivity.this);
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_compass, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }


    protected LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    private Double getFilterDouble(double value) {
        DecimalFormat df = new DecimalFormat("#.####");
        return Double.valueOf(df.format(value));
    }
}
