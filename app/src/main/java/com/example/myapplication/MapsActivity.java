package com.example.myapplication;

import androidx.fragment.app.FragmentActivity;
import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.os.Handler;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.ui.IconGenerator;
import com.google.transit.realtime.GtfsRealtime;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    public List<GtfsRealtime.FeedEntity> feedEntities = new ArrayList<>();
    public static final String MyPREFERENCES = "MyPrefs";
    private GoogleMap mMap;
    LocationManager locationManager;
    SharedPreferences sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            mMap.setMyLocationEnabled(true);
            return;
        }
        //if the internet is on the location will be fetched from the provider
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {

                    //get the latitude
                    double latitude = location.getLatitude();
                    //get the longitude
                    double longitude = location.getLongitude();
                    //instatiate class LatLng
                    LatLng latLng = new LatLng(latitude, longitude);
                    //instatiate the class Geocoder
                    Geocoder geocoder = new Geocoder(getApplicationContext());
                    try {
                        List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);
                        String str = addressList.get(0).getLocality() + ",";
                        str = "You are here!" + str + addressList.get(0).getCountryName();
                        //current location marker shows locality and country as well
                        mMap.addMarker(new MarkerOptions().position(latLng).title(str));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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
            //the current location is provided  by the GPS
        } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {

                    //get the latitude
                    double latitude = location.getLatitude();
                    //get the longitude
                    double longitude = location.getLongitude();
                    //instatiate class LatLng
                    LatLng latLng = new LatLng(latitude, longitude);
                    //instatiate the class Geocoder
                    Geocoder geocoder = new Geocoder(getApplicationContext());
                    try {
                        List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);
                        String str = addressList.get(0).getLocality() + ",";
                        str = str + addressList.get(0).getCountryName();
                        //current location marker shows locality and country as well
                        mMap.addMarker(new MarkerOptions().position(latLng).title(str));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


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
//runnable function to call the transittask function after every 15 seconds to fetch updated bus information
        Runnable r2 = new Runnable() {
            public void run() {
                new TransitTask().execute();
                feedEntities.clear();
            }
        };

        new Handler().postDelayed(r2, 2000);

    }

    @Override
    //onDestroy() will save the app state when the user exits from the app
    protected void onDestroy() {
        //this will work when activity is destroyed.
        super.onDestroy();
        LatLng center = mMap.getCameraPosition().target;
        //to save the state
        SharedPreferences sharedPref = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        //get latitude and longitude
        double lat = center.latitude;
        double lng = center.longitude;
        float zoom = mMap.getCameraPosition().zoom;
        editor.putLong("lat", Double.doubleToRawLongBits(lat));
        editor.putLong("long", Double.doubleToRawLongBits(lng));
        editor.putFloat("zoom", zoom);
        editor.commit();
    }

    @Override
    //onDestroy() will save the app state when the user exits from the app
    protected void onStop() {
        //this will work when activity is terminated.
        super.onStop();
        LatLng center = mMap.getCameraPosition().target;
        //to save the state
        SharedPreferences sharedPref = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        //get latitude and longitude
        double lat = center.latitude;
        double lng = center.longitude;
        float zoom = mMap.getCameraPosition().zoom;
        editor.putLong("lat", Double.doubleToRawLongBits(lat));
        editor.putLong("long", Double.doubleToRawLongBits(lng));
        editor.putFloat("zoom", zoom);
        editor.commit();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);
        SharedPreferences sharedPreferences = getSharedPreferences(MyPREFERENCES,MODE_PRIVATE);
        double lat = Double.longBitsToDouble(sharedPreferences.getLong("lat", Double.doubleToLongBits(0)));
        double lng = Double.longBitsToDouble(sharedPreferences.getLong("long", Double.doubleToLongBits(0)));
        float zoom = sharedPreferences.getFloat("zoom",0);

        if(lat != 0 ){
            LatLng latLng = new LatLng(lat,lng);
            //moving the camera
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,zoom));
        }else{
            LatLng latLng = new LatLng(44.63107,-63.572687);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,14));
        }

    }


//the class which calls the transit data as the background task
    public class TransitTask extends AsyncTask<Void,Void, List<GtfsRealtime.FeedEntity>> {

        @Override
        protected List<GtfsRealtime.FeedEntity> doInBackground(Void... voids) {
            URL url = null;
            try {
                //url from the requirement
                url = new URL("http://gtfs.halifax.ca/realtime/Vehicle/VehiclePositions.pb");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            GtfsRealtime.FeedMessage feed = null;
            try {
                //fetch the streaming data for the transit
                feed = GtfsRealtime.FeedMessage.parseFrom(url.openStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            String data = "";
            feedEntities.clear();
            for (GtfsRealtime.FeedEntity entity : feed.getEntityList()) {
                feedEntities.add(entity);
            }
            return feedEntities;
        }



        @Override
        protected void onPostExecute(List<GtfsRealtime.FeedEntity> feedEntities) {
            for(GtfsRealtime.FeedEntity entity: feedEntities){
                //fetch the bus details
                IconGenerator iconGen = new IconGenerator(MapsActivity.this);
                String routeId = entity.getVehicle().getTrip().getRouteId();
                double latitude = entity.getVehicle().getPosition().getLatitude();
                double longitude = entity.getVehicle().getPosition().getLongitude();
                LatLng latLong = new LatLng(latitude, longitude);
                String status = String.valueOf(entity.getVehicle().getCurrentStatus());
                String stop_seq = String.valueOf(entity.getVehicle().getCurrentStopSequence());
                String stop_id = entity.getVehicle().getStopId();
                String congestion = String.valueOf(entity.getVehicle().getCongestionLevel());
                String position = String.valueOf(entity.getVehicle().getPosition());
                String time = String.valueOf(entity.getTripUpdate());
                //create markers for the buses
                mMap.addMarker(new MarkerOptions().position(latLong).title(routeId).snippet(status+"\n"+stop_seq+"\n"+stop_id+"\n"+congestion+"\n"+position+"\n"+time).icon(BitmapDescriptorFactory.fromBitmap(iconGen.makeIcon(routeId))));


                //code for creating a custom icon to show bus number
                mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

                    @Override
                    public View getInfoWindow(Marker arg0) {
                        return null;
                    }

                    @Override
                    public View getInfoContents(Marker marker) {

                        Context mContext = getApplicationContext();
                        LinearLayout info = new LinearLayout(mContext);
                        info.setOrientation(LinearLayout.VERTICAL);

                        TextView title = new TextView(mContext);
                        title.setTextColor(Color.BLACK);
                        title.setGravity(Gravity.CENTER);
                        title.setTypeface(null, Typeface.BOLD);
                        title.setText(marker.getTitle());

                        TextView snippet = new TextView(mContext);
                        snippet.setTextColor(Color.GRAY);
                        snippet.setText(marker.getSnippet());

                        info.addView(title);
                        info.addView(snippet);

                        return info;
                    }
                });
            }
        //run the runnable to fetch updated data every 15 seconds
            Runnable r2 = new Runnable() {
                public void run() {
                    if (mMap != null)
                    {
                        mMap.clear();
                    }
                    new TransitTask().execute();

                }
            };

            new Handler().postDelayed(r2,15000);
        }
    }

}


