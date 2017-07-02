package tk.urbantaxi.utxi;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Info;
import com.akexorcist.googledirection.model.Leg;
import com.akexorcist.googledirection.model.Route;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.GeoApiContext;
import com.google.maps.RoadsApi;
import com.google.maps.model.SnappedPoint;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import tk.urbantaxi.utxi.classes.Requestor;
import tk.urbantaxi.utxi.models.DriverLocations;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static tk.urbantaxi.utxi.classes.Constants.ROAD_API_KEY;
import static tk.urbantaxi.utxi.classes.Constants.USER_PROFILES;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, View.OnClickListener {
    private static final int PAGE_SIZE_LIMIT = 100;
    private static final int PAGINATION_OVERLAP = 5;

    GoogleMap mGoogleMap;
    GoogleApiClient mGoogleMapApiClient;
    Marker marker;
    Marker marker2;

    private double longitude;
    private double latitude;
    private  String type = "regular";
    private String companytype = "all";
    private Requestor requestor;
    private Timer timer = null;
    public List<DriverLocations> locationsModelList = new ArrayList();
    public Boolean checker = false;
    public Boolean cameraUpdater = true;
    public Boolean snapping = false;
    public Boolean dragged = false;
    public JSONArray gdrivers;

    public Button btnTaxi;
    List<com.google.maps.model.LatLng> mCapturedLocations = new ArrayList();
    private GeoApiContext mContext;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = new GeoApiContext().setApiKey(ROAD_API_KEY);

        Map<String, Object> param = new LinkedHashMap<>();
        requestor = new Requestor("getnearbytaxis", param, this){
            @Override
            public void postExecute(Boolean cancelled, String result){
                if(!cancelled){
                    try {
                        Log.e("Result", result);
                        JSONObject object = new JSONObject(result);
                        JSONArray drivers = object.getJSONArray("drivers");
                        gdrivers = drivers;
                        if(drivers.length() == 0){
                            removeMarkers();
                            locationsModelList.clear();
                        }
                        removeLocations(drivers);
                        if(!snapping){
                            snapping = true;
                            for(int i = 0; i < drivers.length(); i++){
                                JSONObject driver = drivers.getJSONObject(i);
                                Double lat = Double.parseDouble(driver.getString("latitude"));
                                Double longi = Double.parseDouble(driver.getString("longitude"));
                                mCapturedLocations.add(new com.google.maps.model.LatLng(lat, longi));
                            }
                            new mTaskSnapToRoads().execute();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        if (googleServicesAvailable()) {
            setContentView(R.layout.activity_main);

            btnTaxi = (Button)findViewById(R.id.btnTaxi);
            btnTaxi.setOnClickListener(this);

            initMap();
        } else {
            // no google maps
        }
    }

    private void initMap() {
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);

    }

    public boolean googleServicesAvailable() {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int isAvailable = api.isGooglePlayServicesAvailable(this);
        if (isAvailable == ConnectionResult.SUCCESS) {
            return true;
        } else if (api.isUserResolvableError(isAvailable)) {
            Dialog dialog = api.getErrorDialog(this, isAvailable, 0);
            dialog.show();
        } else {
            Toast.makeText(this, "Can't connect to play services", Toast.LENGTH_LONG).show();
        }
        return false;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        if (mGoogleMap != null) {
            mGoogleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                @Override
                public View getInfoWindow(Marker marker) {
                    return null;
                }

                @Override
                public View getInfoContents(Marker marker) {
                    if(!marker.getTitle().equals("I am Here")){
                        View v = getLayoutInflater().inflate(R.layout.info_window, null);

                        TextView text_name = (TextView) v.findViewById(R.id.text_name);
                        TextView text_rating = (TextView) v.findViewById(R.id.text_rating);
                        ImageView prof = (ImageView)v.findViewById(R.id.prof_img);

                        LatLng ll = marker.getPosition();
                        text_name.setText(marker.getTitle());
                        text_rating.setText(marker.getSnippet());

                        int tag = (int) marker.getTag();
                        Picasso.with(MainActivity.this).load(USER_PROFILES + locationsModelList.get(tag).getImage()).into(prof, new InfoWindowRefresher(marker));
                        return v;
                    }
                    return null;
                }

            });

            mGoogleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                    @Override
                    public void onInfoWindowClick(final Marker marker2) {
                        if(!marker2.getTitle().equals("I am Here")){
                            Log.e("Marker Test:", "Clicked!");
//                            String serverKey = "AIzaSyBgU0mK1vWHJsxp8HvTF3gCUStmGGvsL6g";
//                            LatLng origin = marker2.getPosition();
//                            LatLng destination = marker.getPosition();
//                            GoogleDirection.withServerKey(serverKey)
//                                    .from(origin)
//                                    .to(destination)
//                                    .execute(new DirectionCallback() {
//                                        @Override
//                                        public void onDirectionSuccess(Direction direction, String rawBody) {
//                                            if(direction.isOK()) {
//                                                Route route = direction.getRouteList().get(0);
//                                                Leg leg = route.getLegList().get(0);
////                                                ArrayList<LatLng> directionPositionList = leg.getDirectionPoint();
////                                                PolylineOptions polylineOptions = DirectionConverter.createPolyline(getApplicationContext(), directionPositionList, 5, Color.RED);
////                                                mGoogleMap.addPolyline(polylineOptions);
//
//                                                Info distanceInfo = leg.getDistance();
//                                                Info durationInfo = leg.getDuration();
//                                                String distance = distanceInfo.getText();
//                                                String duration = durationInfo.getText();
//                                                Log.e("Distance And Duration", distance + " " + duration);
//
//
//                                            }
//                                        }
//
//                                        @Override
//                                        public void onDirectionFailure(Throwable t) {
//                                            // Do something here
//                                        }
//                                    });

                            Geocoder geocoder;
                            List<Address> addresses;
                            geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                            String finalAddress = null;
                            try {
                                addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
                                String address = addresses.get(0).getAddressLine(0);
                                String city = addresses.get(0).getLocality();
                                String state = addresses.get(0).getAdminArea();
                                finalAddress = address + ", " + city + ", " + state;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            int tag = (int) marker2.getTag();
                            Intent intent = new Intent(getApplicationContext(), Driver.class);
                            intent.putExtra("name", locationsModelList.get(tag).getName());
                            intent.putExtra("company", locationsModelList.get(tag).getCompany());
                            intent.putExtra("image", locationsModelList.get(tag).getImage());
                            intent.putExtra("model", locationsModelList.get(tag).getModel());
                            intent.putExtra("plate", locationsModelList.get(tag).getPlate());
                            intent.putExtra("vehicleImage", locationsModelList.get(tag).getVehicleImage());
                            intent.putExtra("sessionId", locationsModelList.get(tag).getSessionId());
                            intent.putExtra("address", finalAddress);
                            intent.putExtra("pickupLat", latitude);
                            intent.putExtra("pickupLng", longitude);
                            startActivity(intent);
                        }
                    }
                });
            mGoogleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                @Override
                public void onMarkerDragStart(Marker marker) {

                }

                @Override
                public void onMarkerDrag(Marker marker) {

                }

                @Override
                public void onMarkerDragEnd(Marker marker2) {
                    dragged = true;
                    marker.setPosition(marker2.getPosition());
                    latitude = marker2.getPosition().latitude;
                    longitude = marker2.getPosition().longitude;
                }
            });
        }
        //goToLocation(30.4549047,-91.1730681,16);
        mGoogleMapApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleMapApiClient.connect();

        callAsynchronousTask();
    }
    LocationRequest mLocationRequest;

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(30000);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleMapApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    @Override
    public void onLocationChanged(Location location) {
        if(location==null){
            Toast.makeText(this, "Can't get location", Toast.LENGTH_LONG).show();
        } else{
            if(!dragged){
                if(checker == false){
                    LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
                    CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll,17);
                    mGoogleMap.animateCamera(update);
                    checker = true;
                }


                latitude = location.getLatitude();
                longitude = location.getLongitude();
                Log.e("Google", "Locations has changed" + latitude);

                if(marker != null){
                    marker.remove();
                }

                int height = 100;
                int width = 100;
                BitmapDrawable bitmapdraw=(BitmapDrawable)getResources().getDrawable(R.drawable.ic_user);
                Bitmap b=bitmapdraw.getBitmap();
                Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
                MarkerOptions options = new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromBitmap(smallMarker))
                        .position(new LatLng(location.getLatitude(), location.getLongitude()))
                        .flat(true)
                        .title("I am Here")
                        .draggable(true);
                marker = mGoogleMap.addMarker(options);
            }

            callAsynchronousTask();
        }
    }

    public void callAsynchronousTask() {
        if(timer == null) {
            final Handler handler = new Handler();
            timer = new Timer();
            TimerTask doAsynchronousTask = new TimerTask() {
                @Override
                public void run() {
                    handler.post(new Runnable() {
                        public void run() {
                            if(latitude != 0.0) {
                                Log.e("Test", "Runnable" + latitude + " " + longitude);
                                Map<String, Object> newParam = new LinkedHashMap<>();
                                newParam.put("latitude", latitude);
                                newParam.put("longitude", longitude);
                                newParam.put("vehicletype", type);
                                newParam.put("companytype", companytype);
                                requestor.setParam(newParam);
                                requestor.execute();
                            }
                        }
                    });
                }
            };
            timer.schedule(doAsynchronousTask, 1, 5000); //execute in every 50000 ms
        }
    }

    public Integer getExistingPosition(Integer id){
        for(int i = 0; i < locationsModelList.size(); i ++){
            if(id == locationsModelList.get(i).getUserId()){
                return i;
            }
        }
        return null;
    }

    public void removeLocations(JSONArray drivers){
        Iterator<DriverLocations> i = locationsModelList.iterator();
        while(i.hasNext()){
            DriverLocations location = i.next();
            Log.e("Evaluating", location.getName());
            int counter = 0;
            for(int x = 0; x < drivers.length(); x++){
                try {
                    JSONObject driver = drivers.getJSONObject(x);
                    Log.e("Checker", driver.getString("name"));
                    if(location.getUserId() == driver.getInt("user_id")){
                        counter++;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if(counter == 0){
                Log.e("Remove driver", location.getName());
                location.getMarker().remove();
                i.remove();
            }
        }
    }

    public void removeMarkers(){
        for(int i = 0; i < locationsModelList.size(); i++){
            locationsModelList.get(i).getMarker().remove();
        }
    }

    public void updateCamera(){
        if(cameraUpdater){
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (DriverLocations driver : locationsModelList) {
                builder.include(driver.getMarker().getPosition());
            }
            builder.include(marker.getPosition());
            LatLngBounds bounds = builder.build();
            int padding = 0; // offset from edges of the map in pixels
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
            mGoogleMap.animateCamera(cu);
            cameraUpdater = false;
        }
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.btnTaxi){
            companytype = "taxi";
            cameraUpdater = true;
        }
    }

    private class InfoWindowRefresher implements Callback {
        private Marker marker;

        private InfoWindowRefresher(Marker markerToRefresh) {
            this.marker = markerToRefresh;
        }

        @Override
        public void onSuccess() {
            if (marker != null && marker.isInfoWindowShown()) {
                marker.hideInfoWindow();
                marker.showInfoWindow();
            }
        }

        @Override
        public void onError() {}
    }

    public class mTaskSnapToRoads extends AsyncTask<Void, Void, List<SnappedPoint>> {

                @Override
                protected List<SnappedPoint> doInBackground(Void... params) {
                    try {
                        return snapToRoads(mContext);
                    } catch (final Exception ex) {
                        ex.printStackTrace();
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(List<SnappedPoint> snappedPoints) {
                    for (SnappedPoint point : snappedPoints) {
                        Log.e("Snapped Locations", point.location.lat + " " + point.location.lng);
                    }

                    try{
                        for(int i = 0; i < gdrivers.length(); i++){
                            JSONObject driver = gdrivers.getJSONObject(i);
                            Integer existingPosition = getExistingPosition(driver.getInt("user_id"));
                            if(existingPosition == null){
                                Log.e("Creating marker", driver.getString("name"));

                                Double lat = snappedPoints.get(i).location.lat;
                                Double longi = snappedPoints.get(i).location.lng;

                                DriverLocations locationsModel = new DriverLocations();
                                locationsModel.setId(driver.getInt("id"));
                                locationsModel.setUserId(driver.getInt("user_id"));
                                locationsModel.setName(driver.getString("name"));
                                locationsModel.setCompany(driver.getString("company"));
                                locationsModel.setLatitude(lat);
                                locationsModel.setLongitude(longi);
                                locationsModel.setDistance(driver.getDouble("distance"));
                                locationsModel.setImage(driver.getString("image"));
                                locationsModel.setSessionId(driver.getInt("session_id"));
                                locationsModel.setModel(driver.getString("model"));
                                locationsModel.setPlate(driver.getString("plate"));
                                locationsModel.setVehicleImage(driver.getString("vehicle_image"));

                                MarkerOptions option = new MarkerOptions()
                                        .title(driver.getString("name"))
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ictxi))
                                        .position(new LatLng(lat, longi))
                                        .snippet(driver.getString("company"));
                                Marker marker = mGoogleMap.addMarker(option);
                                marker.setTag(locationsModelList.size());
                                locationsModel.setMarker(marker);

                                locationsModelList.add(locationsModel);
                            }else{

                                Double lat = snappedPoints.get(i).location.lat;
                                Double longi = snappedPoints.get(i).location.lng;

                                locationsModelList.get(existingPosition).setId(driver.getInt("id"));
                                locationsModelList.get(existingPosition).setUserId(driver.getInt("user_id"));
                                locationsModelList.get(existingPosition).setName(driver.getString("name"));
                                locationsModelList.get(existingPosition).setCompany(driver.getString("company"));
                                locationsModelList.get(existingPosition).setLatitude(lat);
                                locationsModelList.get(existingPosition).setLongitude(longi);
                                locationsModelList.get(existingPosition).setDistance(driver.getDouble("distance"));
                                locationsModelList.get(existingPosition).setImage(driver.getString("image"));
                                locationsModelList.get(existingPosition).setSessionId(driver.getInt("session_id"));
                                locationsModelList.get(existingPosition).setModel(driver.getString("model"));
                                locationsModelList.get(existingPosition).setPlate(driver.getString("plate"));
                                locationsModelList.get(existingPosition).setVehicleImage(driver.getString("vehicle_image"));

                                Double lat2 = locationsModelList.get(existingPosition).getLatitude();
                                Double longi2 = locationsModelList.get(existingPosition).getLongitude();
                                locationsModelList.get(existingPosition).getMarker().setPosition(new LatLng(lat2, longi2));
                                locationsModelList.get(existingPosition).getMarker().setTitle(locationsModelList.get(existingPosition).getName());
                                locationsModelList.get(existingPosition).getMarker().setSnippet(locationsModelList.get(existingPosition).getCompany());
                                locationsModelList.get(existingPosition).getMarker().setTag(existingPosition);
                            }
                        }
                        mCapturedLocations.clear();
                        updateCamera();
                        snapping = false;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            };

    private List<SnappedPoint> snapToRoads(GeoApiContext context) throws Exception {
        List<SnappedPoint> snappedPoints = new ArrayList<>();

        int offset = 0;
        while (offset < mCapturedLocations.size()) {
            // Calculate which points to include in this request. We can't exceed the APIs
            // maximum and we want to ensure some overlap so the API can infer a good location for
            // the first few points in each request.
            if (offset > 0) {
                offset -= PAGINATION_OVERLAP;   // Rewind to include some previous points
            }
            int lowerBound = offset;
            int upperBound = Math.min(offset + PAGE_SIZE_LIMIT, mCapturedLocations.size());

            // Grab the data we need for this page.
            com.google.maps.model.LatLng[] page = mCapturedLocations
                    .subList(lowerBound, upperBound)
                    .toArray(new com.google.maps.model.LatLng[upperBound - lowerBound]);

            // Perform the request. Because we have interpolate=true, we will get extra data points
            // between our originally requested path. To ensure we can concatenate these points, we
            // only start adding once we've hit the first new point (i.e. skip the overlap).
            SnappedPoint[] points = RoadsApi.snapToRoads(context, true, page).await();
            boolean passedOverlap = false;
            for (SnappedPoint point : points) {
                if (offset == 0 || point.originalIndex >= PAGINATION_OVERLAP - 1) {
                    passedOverlap = true;
                }
                if (passedOverlap) {
                    snappedPoints.add(point);
                }
            }

            offset = upperBound;
        }

        return snappedPoints;
    }
}
