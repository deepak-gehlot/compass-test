package com.sriharilabs.compass;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.IntentSender;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.util.Log;
import android.widget.RelativeLayout;
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
import com.sriharilabs.PreferenceConnector;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CompassActivity extends AppCompatActivity {

    private static final String TAG = "CompassActivity";

    private Compass compass;
    private ImageView mLockUnloackImg;
    private Button mThemesBtn;
    private TextView latTxt, lngTxt, cityTxt, textViewAds;
    private boolean isLock = false;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private InterstitialAd mInterstitialAd;
    private AdView mAdView;
    private int selectedTheme = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);

        selectedTheme = PreferenceConnector.readInteger(this, PreferenceConnector.THEME, R.mipmap.t5_final);

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-5957117184239951/4531208310");
        mInterstitialAd.loadAd(new AdRequest.Builder().build());

        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        compass = new Compass(this);
        compass.arrowView = (ImageView) findViewById(R.id.main_image_hands);
        compass.dialView = (ImageView) findViewById(R.id.main_image_dial);
        compass.textView = (TextView) findViewById(R.id.textView);
        mLockUnloackImg = (ImageView) findViewById(R.id.imageViewLock);
        latTxt = (TextView) findViewById(R.id.textViewLat);
        lngTxt = (TextView) findViewById(R.id.textViewLng);
        cityTxt = (TextView) findViewById(R.id.textViewCity);
        textViewAds = (TextView) findViewById(R.id.textViewAds);
        mThemesBtn = (Button) findViewById(R.id.btnThemes);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(width-130, width-130);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        compass.dialView.setLayoutParams(layoutParams);
        compass.arrowView.setLayoutParams(layoutParams);
        setThemes(selectedTheme);

        mThemesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showThemeDialog();
            }
        });

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

    private void showThemeDialog() {
        final Dialog dialogItemDetails = new Dialog(CompassActivity.this);
        dialogItemDetails.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogItemDetails.setContentView(R.layout.dialog_theme);
        dialogItemDetails.getWindow().setBackgroundDrawable(
                new ColorDrawable(Color.TRANSPARENT));

        TextView dismiss = (TextView) dialogItemDetails.findViewById(R.id.dismissBtn);
        ViewPager viewPager = (ViewPager) dialogItemDetails.findViewById(R.id.viewPagerItemImages);
        viewPager.setAdapter(new CustomPagerAdapter(dialogItemDetails, CompassActivity.this));

        dismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogItemDetails.dismiss();
            }
        });
        dialogItemDetails.show();
    }

    private void setThemes(int position) {
        switch (position) {
            case R.mipmap.t1_final:
                compass.arrowView.setImageDrawable(ContextCompat.getDrawable(CompassActivity.this, R.mipmap.t1_hand));
                compass.dialView.setImageDrawable(ContextCompat.getDrawable(CompassActivity.this, R.mipmap.t1_dial));
                break;

            case R.mipmap.t2_final:
                compass.arrowView.setImageDrawable(ContextCompat.getDrawable(CompassActivity.this, R.mipmap.t2_hand));
                compass.dialView.setImageDrawable(ContextCompat.getDrawable(CompassActivity.this, R.mipmap.t2_dial));
                break;

            case R.mipmap.t3_final:
                compass.arrowView.setImageDrawable(ContextCompat.getDrawable(CompassActivity.this, R.mipmap.t3_hand));
                compass.dialView.setImageDrawable(ContextCompat.getDrawable(CompassActivity.this, R.mipmap.t3_dial));
                break;

            case R.mipmap.t4_final:
                compass.arrowView.setImageDrawable(ContextCompat.getDrawable(CompassActivity.this, R.mipmap.t4_hand));
                compass.dialView.setImageDrawable(ContextCompat.getDrawable(CompassActivity.this, R.mipmap.t4_dial));
                break;

            case R.mipmap.t5_final:
                compass.arrowView.setImageDrawable(ContextCompat.getDrawable(CompassActivity.this, R.mipmap.rotating));
                compass.dialView.setImageDrawable(ContextCompat.getDrawable(CompassActivity.this, R.mipmap.dial));
                break;
        }
    }

    public class CustomPagerAdapter extends PagerAdapter {

        private Context mContext;
        List<Integer> imageList = new ArrayList<>();

        Dialog dialog;

        public CustomPagerAdapter(Dialog dialog, Context context) {
            mContext = context;
            this.dialog = dialog;
            imageList.add(R.mipmap.t5_final);
            imageList.add(R.mipmap.t1_final);
            imageList.add(R.mipmap.t2_final);
            imageList.add(R.mipmap.t3_final);
            imageList.add(R.mipmap.t4_final);
            for (int i = 0; i < imageList.size(); i++) {
                if (selectedTheme == imageList.get(i)) {
                    imageList.remove(i);
                }
            }
        }

        @Override
        public Object instantiateItem(ViewGroup collection, final int position) {
            ImageView imageView = new ImageView(mContext);
            imageView.setImageResource(imageList.get(position));

            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PreferenceConnector.writeInteger(CompassActivity.this, PreferenceConnector.THEME, imageList.get
                            (position));
                    selectedTheme = imageList.get(position);
                    setThemes(imageList.get(position));
                    dialog.dismiss();
                }
            });
            collection.addView(imageView);
            return imageView;
        }

        @Override
        public void destroyItem(ViewGroup collection, int position, Object view) {
            collection.removeView((View) view);
        }

        @Override
        public int getCount() {
            return imageList.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }
}