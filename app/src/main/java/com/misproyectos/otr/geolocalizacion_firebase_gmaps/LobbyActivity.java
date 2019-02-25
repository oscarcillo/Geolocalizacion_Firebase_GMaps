package com.misproyectos.otr.geolocalizacion_firebase_gmaps;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LobbyActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private List<String> listaUsuarios = new ArrayList<>();

    private ListView listViewUsuarios;

    private FusedLocationProviderClient fusedLocationClient;
    private int MY_PERMISSION_REQUEST_LOCATION;

    boolean parada = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        final Intent i = new Intent(this, MapsActivity.class);
        i.putExtra("nombrePropio", getIntent().getStringExtra("nombrePropio"));
        listViewUsuarios = findViewById(R.id.listViewUsuarios);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        //
        mDatabase = FirebaseDatabase.getInstance().getReference().child("usuarios");

        uploadLocationFirebase();

        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                listaUsuarios.clear();
                for(DataSnapshot snapshot: dataSnapshot.getChildren()){
                    MapsPOJO mp = snapshot.getValue(MapsPOJO.class);
                    if(!mp.getNombre().equals(getIntent().getStringExtra("nombrePropio")))
                        listaUsuarios.add(mp.getNombre());
                }
                //llenar la listview de usuarios
                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                        getApplicationContext(),
                        android.R.layout.simple_list_item_1,
                        listaUsuarios );

                listViewUsuarios.setAdapter(arrayAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        listViewUsuarios.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                i.putExtra("nombrePareja", listaUsuarios.get(position));
                startActivity(i);
            }
        });
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
                            latlang.put("nombre", getIntent().getStringExtra("nombrePropio"));
                            mDatabase.child(getIntent().getStringExtra("nombrePropio")).setValue(latlang);
                        }
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        uploadLocationFirebase();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mDatabase.child(getIntent().getStringExtra("nombrePropio")).removeValue();
        parada = true;
        finish();
    }
}
