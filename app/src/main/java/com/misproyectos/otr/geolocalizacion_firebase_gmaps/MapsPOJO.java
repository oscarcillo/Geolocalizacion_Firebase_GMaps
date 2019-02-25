package com.misproyectos.otr.geolocalizacion_firebase_gmaps;

public class MapsPOJO {

    private double latitud;
    private double longitud;
    private String nombre;

    public double getLongitud() {
        return longitud;
    }

    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }

    public double getLatitud() {
        return latitud;
    }

    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombrePropio) {
        this.nombre = nombrePropio;
    }
}
