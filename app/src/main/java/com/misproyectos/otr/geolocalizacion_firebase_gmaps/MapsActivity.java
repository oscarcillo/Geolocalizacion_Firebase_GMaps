package com.misproyectos.otr.geolocalizacion_firebase_gmaps;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private DatabaseReference mDatabase;

    private String nombrePropio;
    private String nombrePareja;
    //
    private FusedLocationProviderClient fusedLocationClient;
    private int MY_PERMISSION_REQUEST_LOCATION;

    Handler handler = new Handler();

    boolean parada = false;
    boolean animacionCompletada = false;

    PolylineOptions options = new PolylineOptions();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        Intent i = getIntent();
        nombrePropio = i.getStringExtra("nombrePropio");
        nombrePareja = i.getStringExtra("nombrePareja");
        //
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        uploadLocationFirebase();

        //
        //dibujar ruta entre los dos marcadores
        options.color( Color.parseColor( "#CC0000FF" ) );
        options.width( 5 );
        options.visible( true );
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mDatabase.child("usuarios").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mMap.clear();
                for(DataSnapshot snapshot: dataSnapshot.getChildren()){
                    MapsPOJO mp = snapshot.getValue(MapsPOJO.class);
                    if(mp.getNombre().equals(nombrePropio) || mp.getNombre().equals(nombrePareja)){
                        double latitud = mp.getLatitud();
                        double longitud = mp.getLongitud();
                        MarkerOptions markerOptions = new MarkerOptions();
                        markerOptions.position(new LatLng(latitud, longitud));
                        mMap.addMarker(markerOptions);
                        if(mp.getNombre().equals(nombrePareja) && !animacionCompletada){
                            //mover la camara al punto en el que está el marcador
                            Location l = new Location("");
                            l.setLatitude(latitud);
                            l.setLongitude(longitud);
                            moveCamera(l);
                        }
                        options.add( new LatLng( latitud, longitud));
                    }
                }

                mMap.addPolyline(options);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        handler.postDelayed(new Runnable(){
            public void run(){
                if(parada)return;
                uploadLocationFirebase();
                handler.postDelayed(this, 5000);
            }
        }, 5000);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mDatabase.child("usuarios").child(nombrePropio).removeValue();
        parada = true;
        finish();
    }

    public void uploadLocationFirebase() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}
                    , MY_PERMISSION_REQUEST_LOCATION);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            Log.e("hola", "localizacion subida");
                            Map<String, Object> latlang = new HashMap<>();
                            latlang.put("latitud", location.getLatitude());
                            latlang.put("longitud", location.getLongitude());
                            latlang.put("nombre", nombrePropio);
                            mDatabase.child("usuarios").child(nombrePropio).setValue(latlang);
                        }
                    }
                });
    }

    public void moveCamera(Location location){
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 13));

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(location.getLatitude(), location.getLongitude()))      // Sets the center of the map to location user
                .zoom(7)                   // Sets the zoom
                .bearing(0)                // Sets the orientation of the camera to east
                .tilt(40)                   // Sets the tilt of the camera to 30 degrees
                .build();                   // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        animacionCompletada = true;
    }

}
