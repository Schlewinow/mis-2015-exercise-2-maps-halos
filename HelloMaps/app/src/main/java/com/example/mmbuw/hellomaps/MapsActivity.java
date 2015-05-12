package com.example.mmbuw.hellomaps;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends FragmentActivity
{
    /**
     * Shared preference key. In case this code is ever separated to different classes, this will still allow access.
     */
    private final String PREFERENCE_FILE_KEY = "BUW_Maps_Shared_Preferences";

    /**
     * Identifier in the shared preferences used for the total amount of initialized markers.
     */
    private final String MARKER_COUNT = "Marker_Counter";

    /**
     * Identifier in the shared preferences used for the text of a marker.
     */
    private final String MARKER_LABEL = "Marker_Label";

    /**
     * Identifier in the shared preferences used for the longitude of a marker.
     */
    private final String MARKER_LONGITUDE = "Marker_Longitude";

    /**
     * Identifier in the shared preferences used for the latitude of a marker.
     */
    private final String MARKER_LATITUDE = "Marker_Latitude";

    /**
     * Identifier in the shared preferences text to separate the ID of the marker from the label
     */
    private final String ID_SEPARATOR = "#-#";

    /**
     * Might be null if Google Play services APK is not available.
     */
    private GoogleMap mMap;

    /**
     * Marker label text input field.
     */
    private EditText mMarkerInput = null;

    /**
     * Faster runtime marker access.
     */
    private ArrayList<Marker> mMarkers = null;

    /**
     * Access to currently drawn halo circles.
     */
    private Map<Integer, Circle> mCircles = null;

    /**
     * Used to store marker data.
     */
    private SharedPreferences mPrefs = null;

    /**
     * Used for indexing markers.
     */
    private int mCurrentMaxMarker = 0;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Instantiate containers.
        this.mMarkers = new ArrayList<Marker>();
        this.mCircles = new HashMap<Integer, Circle>();

        // Setup prefs to store and read marker data.
        this.mPrefs = this.getSharedPreferences(PREFERENCE_FILE_KEY, MODE_PRIVATE);
        if(this.mPrefs.contains(MARKER_COUNT))
        {
            this.mCurrentMaxMarker = this.mPrefs.getInt(MARKER_COUNT, 0);
        }

        setUpMapIfNeeded();

        // Get input field for marker label.
        this.mMarkerInput = (EditText)this.findViewById(R.id.inputField);

        // Listener for long clicks on map sets marker label.
        this.mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            public void onMapLongClick(LatLng point)
            {
                createMarker(point, mMarkerInput.getText().toString(),null, true);
            }
        });

        // Delete markers when dragging (I just wanted an option to delete markers).
        this.mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            public void onMarkerDragStart(Marker marker)
            {
            }

            public void onMarkerDrag(Marker marker)
            {
            }

            public void onMarkerDragEnd(Marker marker)
            {
                deleteMarker(marker);
            }
        });

        // Self-implemented click behaviour.
        this.mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            public boolean onMarkerClick(Marker marker)
            {
                return showMarkerText(marker);
            }
        });

        // The whole halo-implementation lies behind this call.
        this.mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            public void onCameraChange(CameraPosition cameraPosition)
            {
                updateHalos();
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded()
    {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null)
        {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();

            // Check if we were successful in obtaining the map.
            if (mMap != null)
            {
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
    private void setUpMap()
    {
        if( this.mCurrentMaxMarker == 0)
        {
            // If the application starts for the first time, create a first marker.
            //mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
            this.createMarker(new LatLng(0, 0), "Marker", null, true);
        }
        else
        {
            // Load markers from shared prefs.
            for(int index = 0; index < this.mCurrentMaxMarker; ++index)
            {
                if(this.mPrefs.contains(MARKER_LABEL + index))
                {
                    // Text might be empty, so there is just an ID. Check that.
                    String[] splitText = this.mPrefs.getString(MARKER_LABEL + index, "").split(ID_SEPARATOR);
                    String label = "";
                    if(splitText.length > 1)
                    {
                        label = splitText[1];
                    }

                    Float longitude = this.mPrefs.getFloat(MARKER_LONGITUDE + index, 0.0f);
                    Float latitude = this.mPrefs.getFloat(MARKER_LATITUDE + index, 0.0f);
                    this.createMarker(new LatLng(latitude, longitude), label, index, false);
                }
            }
        }
    }

    /**
     * Create a custom marker.
     * @param point Where to put the marker.
     * @param label Text used with the marker.
     * @param id Unique identifier. When null is passed, the marker counter is used to generate an auto-ID.
     * @param saveToPrefs Do you want to save the created marker in the prefs?
     */
    private void createMarker(LatLng point, String label, Integer id, boolean saveToPrefs)
    {
        // If no ID is given, use marker counter.
        if(id == null)
        {
            id = this.mCurrentMaxMarker;
        }

        // Create with custom properties.
        MarkerOptions options = new MarkerOptions().position(point)
                .title("" + id + ID_SEPARATOR + label)
                .alpha(0.75f)
                .icon(BitmapDescriptorFactory.defaultMarker(140.0f));
        Marker newMarker = mMap.addMarker(options);

        // Used as deletion command;
        newMarker.setDraggable(true);

        // Save for access.
        this.mMarkers.add(newMarker);

        if (saveToPrefs)
        {
            SharedPreferences.Editor editor = this.mPrefs.edit();
            editor.putString(MARKER_LABEL + this.mCurrentMaxMarker, newMarker.getTitle());
            editor.putFloat(MARKER_LONGITUDE + this.mCurrentMaxMarker, (float) point.longitude);
            editor.putFloat(MARKER_LATITUDE + this.mCurrentMaxMarker, (float) point.latitude);

            this.mCurrentMaxMarker++;
            editor.putInt(MARKER_COUNT, this.mCurrentMaxMarker);
            editor.commit();
        }
    }

    /**
     * Deletes a marker.
     * @param marker The marker to be deleted. For this operation to work, the marker must have an ID.
     */
    private void deleteMarker (Marker marker)
    {
        // Remove marker from prefs. This is why the ID is necessary, to identify the marker.
        int markerID = Integer.parseInt(marker.getTitle().split(ID_SEPARATOR)[0]);
        SharedPreferences.Editor editor = this.mPrefs.edit();
        editor.remove(MARKER_LABEL + markerID);
        editor.remove(MARKER_LONGITUDE + markerID);
        editor.remove(MARKER_LATITUDE + markerID);
        editor.commit();

        // Remove marker from running app.
        mMarkers.remove(marker);
        marker.remove();
    }

    /**
     * Shows the text of a marker to the user.
     * @param marker The marker to show it's text.
     * @return Necessary for use with OnMarkerClick.
     */
    private boolean showMarkerText (Marker marker)
    {
        // Text might be empty, so there is just an ID. Check that.
        String[] splitText = marker.getTitle().split(ID_SEPARATOR);
        String outputText = "";
        if (splitText.length > 1)
        {
            outputText = splitText[1];
        }

        String originalText = marker.getTitle();
        marker.setTitle(outputText);                // Do not show ID to user!
        marker.showInfoWindow();
        marker.setTitle(originalText);              // Yet delete ID neither!

        // Basic idea: http://stackoverflow.com/questions/11548864/how-to-make-an-android-program-wait
        Handler h = new Handler();
        h.postDelayed(new CloseInfoWindowRunnable(marker), 1000);

        // Suppress default Marker click behaviour.
        return true;
    }

    /**
     * Helper to close info window with some delay after clicking it.
     */
    private class CloseInfoWindowRunnable implements Runnable
    {
        private Marker mLocalMarker;

        public CloseInfoWindowRunnable(Marker marker)
        {
            this.mLocalMarker = marker;
        }

        public void run()
        {
            mLocalMarker.hideInfoWindow();
        }
    }

    /**
     * Draw the halos. Manages whether circles are created, updated and/or removed.
     * (Master Chief, you know what to do! Blow these things up!)
     */
    private void updateHalos()
    {
        LatLngBounds screenBounds = this.mMap.getProjection().getVisibleRegion().latLngBounds;

        for(int index = 0; index < this.mMarkers.size(); ++index)
        {
            Marker marker = this.mMarkers.get(index);
            int markerIndex = Integer.parseInt(marker.getTitle().split(ID_SEPARATOR)[0]);

            // Circle management. Create new if it does not yet exist, get existing otherwise.
            Circle circle;
            if(!this.mCircles.containsKey(markerIndex) || this.mCircles.get(markerIndex) == null)
            {
                // Add circle if it does not yet exist.
                CircleOptions co = new CircleOptions();
                co.center(marker.getPosition());
                co.radius(0.0);
                co.strokeColor(Color.argb(128, 0, 255, 128));

                Circle newCircle = this.mMap.addCircle(co);
                this.mCircles.put(markerIndex, newCircle);
                circle = newCircle;
            }
            else
            {
                circle = this.mCircles.get(markerIndex);
            }

            // Update circle.
            if (!screenBounds.contains(marker.getPosition()))
            {
                this.updateRadius(marker, circle, screenBounds);
            }
            else
            {
                // Remove circles that are not needed anymore.
                if(this.mCircles.containsKey(markerIndex))
                {
                    this.mCircles.get(markerIndex).remove();
                    this.mCircles.put(markerIndex, null);
                    this.mCircles.remove(markerIndex);
                }
            }
        }
    }

    /**
     * Update size of a given circle.
     * @param marker The marker the circle belongs to.
     * @param circle The circle around the marker.
     * @param screenBounds The bounds of the current map screen.
     */
    private void updateRadius(Marker marker, Circle circle, LatLngBounds screenBounds)
    {
        LatLng cameraPos = screenBounds.getCenter();
        LatLng markerPos = marker.getPosition();

        double radiusCircle;
        double radiusOffset = 1000000.0 / Math.pow(mMap.getCameraPosition().zoom, 2);   // magic number, but it does the job somehow

        if(markerPos.longitude >= screenBounds.southwest.longitude &&
                markerPos.longitude <= screenBounds.northeast.longitude)
        {
            // Marker is beyond upper or lower border of screen.
            markerPos = new LatLng(markerPos.latitude, cameraPos.longitude);
            double yRadius = this.getDistanceBetweenPoints(cameraPos, markerPos);

            LatLng upperEdgePoint = new LatLng(screenBounds.northeast.latitude, cameraPos.longitude);
            double yCenterToEdge = this.getDistanceBetweenPoints(cameraPos, upperEdgePoint);

            radiusCircle = yRadius - yCenterToEdge + radiusOffset;
        }
        else if(markerPos.latitude >= screenBounds.southwest.latitude &&
                markerPos.latitude <= screenBounds.northeast.latitude)
        {
            // Marker is beyond left or right border of screen.
            markerPos = new LatLng(cameraPos.latitude, markerPos.longitude);
            double xRadius = this.getDistanceBetweenPoints(cameraPos, markerPos);

            LatLng rightEdgePoint = new LatLng(cameraPos.latitude, screenBounds.northeast.longitude);
            double xCenterToEdge = this.getDistanceBetweenPoints(cameraPos, rightEdgePoint);

            radiusCircle = xRadius - xCenterToEdge + radiusOffset;
        }
        else
        {
            // Marker is in a corner area of the screen.
            LatLng closestCorner;

            // We need to get the closest corner of the screen to the marker.
            if(markerPos.longitude < screenBounds.southwest.longitude &&
                    markerPos.latitude < screenBounds.southwest.latitude)
            {
                // bottom left
                closestCorner = screenBounds.southwest;
            }
            else if(markerPos.longitude < screenBounds.southwest.longitude &&
                    markerPos.latitude > screenBounds.northeast.latitude)
            {
                // top left
                closestCorner = new LatLng(screenBounds.northeast.latitude, screenBounds.southwest.longitude);
            }
            else if (markerPos.longitude > screenBounds.northeast.longitude &&
                    markerPos.latitude > screenBounds.northeast.latitude)
            {
                // top right
                closestCorner = screenBounds.northeast;
            }
            else
            {
                // bottom right
                closestCorner = new LatLng(screenBounds.southwest.latitude, screenBounds.northeast.longitude);
            }

            double radiusToCorner = this.getDistanceBetweenPoints(markerPos, closestCorner);
            radiusCircle = radiusToCorner + radiusOffset;
        }

        circle.setRadius(radiusCircle);
    }

    /**
     * Get distance in meters between two points
     * @param start First of the two points.
     * @param dest Second of the two points.
     * @return Distance as double in meter.
     */
    private double getDistanceBetweenPoints(LatLng start, LatLng dest)
    {
        float[] results = new float[3];
        Location.distanceBetween(start.latitude, start.longitude, dest.latitude, dest.longitude, results);
        return results[0];

        // Source: http://stackoverflow.com/questions/639695/how-to-convert-latitude-or-longitude-to-meters (Converted to Java)
        // note: unreliable, seems to have some strong rounding error
        /*double earthRadius = 6378.137; // Radius of earth in Km

        double lat1 = Math.toRadians(start.latitude);//start.latitude * Math.PI / 180.0;
        double lat2 = Math.toRadians(dest.longitude);//dest.latitude * Math.PI / 180.0;
        double deltaLatitude = Math.toRadians(dest.latitude - start.latitude);// * Math.PI / 180.0;
        double deltaLongitude = Math.toRadians(dest.longitude - start.longitude);// * Math.PI / 180.0;

        double a = Math.sin(deltaLatitude * 0.5) * Math.sin(deltaLatitude * 0.5);
        a = a + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLongitude * 0.5) * Math.sin(deltaLongitude * 0.5);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0-a));
        double d = earthRadius * c;
        return d * 1000.0; // meters*/
    }
}
