package edu.gvsu.cis.getterj.flyordrive;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;


public class ResultsActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    Double startLat;
    Double startLon;
    Double endLat;
    Double endLon;

    public String getEncodedPolyline() {
        return encodedPolyline;
    }

    public void setEncodedPolyline(String encodedPolyline) {
        this.encodedPolyline = encodedPolyline;
    }

    String encodedPolyline;





    private Marker myMarker;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private static final String TAG = "GglPlayServicesActivity";

    private static final String KEY_IN_RESOLUTION = "is_in_resolution";

    /**
     * Request code for auto Google Play Services error resolution.
     */
    protected static final int REQUEST_CODE_RESOLUTION = 1;

    /**
     * Google API client.
     */
    private GoogleApiClient mGoogleApiClient;

    /**
     * Determines if the client is in a resolution state, and
     * waiting for resolution intent to return.
     */
    private boolean mIsInResolution;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_results);

        TextView flyCost = (TextView) findViewById(R.id.fCost);
        TextView flyTime = (TextView) findViewById(R.id.fTime);
        TextView flyMiles = (TextView) findViewById(R.id.fMiles);
        TextView driveCost = (TextView) findViewById(R.id.dCost);
        TextView driveTime = (TextView) findViewById(R.id.dTime);
        TextView driveMiles = (TextView) findViewById(R.id.dMiles);
        TextView resultsLabel = (TextView) findViewById(R.id.resultsLabel);
        TextView flyLabel = (TextView) findViewById(R.id.flyLabel);
        flyLabel.setPaintFlags(flyLabel.getPaintFlags()| Paint.UNDERLINE_TEXT_FLAG);
        TextView driveLabel = (TextView) findViewById(R.id.driveLabel);
        driveLabel.setPaintFlags(driveLabel.getPaintFlags()| Paint.UNDERLINE_TEXT_FLAG);

        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance();

        Intent fromMain = getIntent();

        setEncodedPolyline(fromMain.getStringExtra("pointsEncoded"));

String polyline = fromMain.getStringExtra("pointsEncoded");
        int flightTimeInMins = Integer.parseInt(fromMain.getStringExtra("flyingTime"));
        int flightHours = flightTimeInMins/60;
        int flightMins = flightTimeInMins%60;
        double flightPrice = Double.parseDouble(fromMain.getStringExtra("flightPrice"));
        double drivePrice = fromMain.getDoubleExtra("driveCost",0);
        String milesToDriveString = fromMain.getStringExtra("driveMiles") + " miles";
        startLat = Double.parseDouble(fromMain.getStringExtra("startLat"));
        startLon = Double.parseDouble(fromMain.getStringExtra("startLon"));
        endLat = Double.parseDouble(fromMain.getStringExtra("endLat"));
        endLon = Double.parseDouble(fromMain.getStringExtra("endLon"));
        driveCost.setText(currencyFormatter.format(drivePrice));
        driveMiles.setText(milesToDriveString);
        driveTime.setText(fromMain.getStringExtra("driveDuration"));
        flyCost.setText(currencyFormatter.format(flightPrice));
        flyMiles.setText(fromMain.getIntExtra("flightMileage", 0) + " miles");
        flyTime.setText(flightHours + " hours " + flightMins + " mins");
        if(drivePrice < flightPrice)
        {
            resultsLabel.setText("Drive!");

        }
        else
        {
            resultsLabel.setText("Fly!");

        }
        setUpMapIfNeeded();
        if (savedInstanceState != null) {
            mIsInResolution = savedInstanceState.getBoolean(KEY_IN_RESOLUTION, false);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_results, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when the Activity is made visible.
     * A connection to Play Services need to be initiated as
     * soon as the activity is visible. Registers {@code ConnectionCallbacks}
     * and {@code OnConnectionFailedListener} on the
     * activities itself.
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    // Optionally, add additional APIs and scopes if required.
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        mGoogleApiClient.connect();
    }

    /**
     * Called when activity gets invisible. Connection to Play Services needs to
     * be disconnected as soon as an activity is invisible.
     */
    @Override
    protected void onStop() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }
    /**
     * Saves the resolution state.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IN_RESOLUTION, mIsInResolution);
    }

    /**
     * Handles Google Play Services resolution callbacks.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_RESOLUTION:
                retryConnecting();
                break;
        }
    }

    private void retryConnecting() {
        mIsInResolution = false;
        if (!mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }
    }

    /**
     * Called when {@code mGoogleApiClient} is connected.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "GoogleApiClient connected");
    }

    /**
     * Called when {@code mGoogleApiClient} connection is suspended.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
        retryConnecting();
    }

    /**
     * Called when {@code mGoogleApiClient} is trying to connect but failed.
     * Handle {@code result.getResolution()} if there is a resolution
     * available.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // Show a localized error dialog.
            GooglePlayServicesUtil.getErrorDialog(
                    result.getErrorCode(), this, 0, new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            retryConnecting();
                        }
                    }).show();
            return;
        }
        // If there is an existing resolution error being displayed or a resolution
        // activity has started before, do nothing and wait for resolution
        // progress to be completed.
        if (mIsInResolution) {
            return;
        }
        mIsInResolution = true;
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
            retryConnecting();
        }
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link com.google.android.gms.maps.SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    public void setUpMap() {
        mMap.addMarker(new MarkerOptions().position(new LatLng(startLat, startLon)).title("Marker"));
        mMap.addMarker(new MarkerOptions().position(new LatLng(endLat, endLon)).title("Marker"));

        mMap.addPolyline(new PolylineOptions()
                .add(new LatLng(startLat, startLon), new LatLng(endLat, endLon))
                .width(10)
                .color(Color.rgb(208, 90, 90))
        );

        List<LatLng> points = new ArrayList<LatLng>();
        points = decodePoly(this.encodedPolyline);
        mMap.addPolyline(new PolylineOptions()
        .addAll(points)
                .width(10)
                .color(Color.rgb(90, 117, 208))
        );



        LatLng geoPos = new LatLng(startLat, startLon);

        CameraPosition campos = CameraPosition.builder()
                .target(geoPos)
                .zoom(5)
                .build();

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(campos));
        if (myMarker == null) /* if we don't have a marker yet, create and add */
            myMarker = mMap.addMarker(new MarkerOptions().position(geoPos));
        else
            myMarker.setPosition (geoPos);
    }
    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> points = PolyUtil.decode(encoded);
        return points;
    }
}
