package com.example.dronegcs;

import android.graphics.Color;
import android.support.annotation.NonNull;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.overlay.PolygonOverlay;
import com.naver.maps.map.overlay.PolylineOverlay;
import com.o3dr.services.android.lib.coordinate.LatLong;

import java.util.ArrayList;

public class ManageOverlay {
    private NaverMap mymap;
    private ArrayList<LatLng> ppointlist = new ArrayList<LatLng>();
    private ArrayList<Marker> markerlist = new ArrayList<>();
    private ArrayList<Marker> spraymakers = new ArrayList<>();
    private ArrayList<LatLng> spraypointlist = new ArrayList<>();




    private PathOverlay pathline = new PathOverlay();
    private ArrayList<InfoWindow> infowindowlist = new ArrayList<>();
    private PolygonOverlay polygon = new PolygonOverlay();
    private ArrayList<Marker> boundlist = new ArrayList<>();
    protected MainActivity mainactivity;
    public ManageOverlay(NaverMap mymap,MainActivity mainactivity)
    {
        this.mainactivity = mainactivity;
        this.mymap = mymap;
    }

    public void setPPosition(LatLong latLong){
        Marker marker = new Marker();
        LatLng coord = new LatLng(latLong.getLatitude(),latLong.getLongitude());
        ppointlist.add(coord);
        markerlist.add(marker);
        marker.setWidth(40);
        marker.setHeight(70);
        marker.setPosition(coord);
        marker.setMap(mymap);
    }

    public void drawCustomPath(){
        if(ppointlist.size()>1){

            pathline.setCoords(ppointlist);
            pathline.setColor(Color.GREEN);
            pathline.setMap(mymap);
            for(int i=0;i<ppointlist.size();i++){
                markerlist.get(i).setTag(String.format(""+(i+1)));

                InfoWindow infoWindow = new InfoWindow();
                infoWindow.setAdapter((new InfoWindow.DefaultTextAdapter(mainactivity) {
                    @NonNull
                    @Override
                    public CharSequence getText(@NonNull InfoWindow infoWindow) {
                        return (CharSequence)infoWindow.getMarker().getTag();
                    }
                }));
                infoWindow.open(markerlist.get(i));
                infowindowlist.add(infoWindow);
            }
        }


    }
    public void drawPolygon(){

        polygon.setCoords(ppointlist);
        polygon.setColor(0);

        polygon.setOutlineWidth(2);
        polygon.setMap(mymap);
    }

    public void drawSprayPoint(ArrayList<LatLong> sprayplist){


        for(Marker marker : markerlist){
            marker.setMap(null);
        }
        for(Marker marker : spraymakers){
            marker.setMap(null);
        }
        spraypointlist.clear();
        spraymakers.clear();


        for(LatLong latlong:sprayplist){
            LatLng latlng = new LatLng(latlong.getLatitude(),latlong.getLongitude());
            spraypointlist.add(latlng);
            Marker marker = new Marker();
            marker.setWidth(40);
            marker.setHeight(70);
            marker.setPosition(latlng);
            marker.setMap(mymap);

            spraymakers.add(marker);
        }
        for(int i=0;i<spraypointlist.size();i++){
            spraymakers.get(i).setTag(String.format(""+(i+1)));
            InfoWindow infoWindow = new InfoWindow();
            infoWindow.setAdapter((new InfoWindow.DefaultTextAdapter(mainactivity) {
                @NonNull
                @Override
                public CharSequence getText(@NonNull InfoWindow infoWindow) {
                    return (CharSequence)infoWindow.getMarker().getTag();
                }
            }));
            infoWindow.open(spraymakers.get(i));
            infowindowlist.add(infoWindow);
        }

        pathline.setCoords(spraypointlist);
        pathline.setColor(Color.GREEN);
        pathline.setMap(mymap);

    }

    public void reset(){
        for(Marker marker : markerlist){
            marker.setMap(null);
        }
        for(Marker marker : spraymakers){
            marker.setMap(null);
        }
        pathline.setMap(null);
        polygon.setMap(null);
        ppointlist.clear();
        spraypointlist.clear();
        markerlist.clear();

    }
    public ArrayList<LatLng> getspraylist(){
        return spraypointlist;
    }
    public ArrayList<LatLng> getpointlist(){
        return ppointlist;
    }

}
