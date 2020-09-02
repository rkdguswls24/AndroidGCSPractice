package com.example.dronegcs;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;

import android.graphics.PointF;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationSource;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapView;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.LocationOverlay;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.overlay.PolygonOverlay;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.MissionApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.apis.solo.SoloCameraApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.companion.solo.SoloAttributes;
import com.o3dr.services.android.lib.drone.companion.solo.SoloState;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.connection.ConnectionType;
import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.drone.mission.item.command.YawCondition;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Waypoint;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.GuidedState;
import com.o3dr.services.android.lib.drone.property.Home;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.NaverMapSdk;
import com.naver.maps.map.util.FusedLocationSource;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.SimpleCommandListener;
import com.o3dr.services.android.lib.util.MathUtils;

import org.droidplanner.services.android.impl.core.helpers.geoTools.LineLatLong;
import org.droidplanner.services.android.impl.core.polygon.Polygon;
import org.droidplanner.services.android.impl.core.survey.grid.CircumscribedGrid;
import org.droidplanner.services.android.impl.core.survey.grid.Trimmer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, DroneListener, TowerListener, LinkListener{
    private boolean dronestate = false;
    protected Drone drone;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private int droneType = Type.TYPE_UNKNOWN;
    private ControlTower controlTower;
    private boolean connectDrone = false;
    private boolean armstatus = false;
    private boolean altitudeset = false;
    private boolean maplock = false;
    private boolean mapoption = false;
    private boolean mapcads =false;
    private boolean mapfollow = true;
    private boolean togglebtn = false;
    private boolean missionlist = false;
    private LinearLayout btnset;
    private LinearLayout armingbtn;
    private LinearLayout setlist;
    private double dronealtitude=5.5;
    private static final String TAG = MainActivity.class.getSimpleName();
    private Button takeoffsetbtn;
    private final Handler handler = new Handler();
    private FusedLocationSource locationSource;
    private NaverMap mymap;
    private GuideMode guide;
    private ArrayList<LatLng> pathcoords = new ArrayList<>();
    private PathOverlay dronepath;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private Spinner modeSelector;
    private LocationOverlay locationOverlay;
    private ArrayList<String> alertlist = new ArrayList<>();
    private RecyclerView recyclerView;
    private SimpleTextAdapter adapter;

    //polygon
    private boolean[] mission = {false , false,false};
    public ArrayList<LatLong> polygonPointList = new ArrayList<LatLong>();
    public ArrayList<LatLong> sprayPointList = new ArrayList<>();
    private MainActivity mainActivity;
    private ManageOverlay manageOverlays;
    private LatLong pointA = null;
    private LatLong pointB = null;
    private double sprayDistance = 5.5f;
    private int maxSprayDistance = 50;
    private int capacity = 0;
    private double sprayAngle;


    @Nullable
    private LocationManager locationManager;

    @Nullable
    private LocationSource.OnLocationChangedListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Context context = getApplicationContext();
        this.controlTower = new ControlTower(context);
        this.drone = new Drone(context);

        this.modeSelector = (Spinner) findViewById(R.id.flymode);
        this.modeSelector.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onFlightModeSelected(view);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }

        });
        locationSource =
                new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }


        if (!connectDrone) {
            armingbtn = (LinearLayout) findViewById(R.id.connectmenu);
            armingbtn.setVisibility(View.INVISIBLE);
        }
        if(armstatus==false){

            setlist= (LinearLayout)findViewById(R.id.takeoffsetlist);
            setlist.setVisibility(View.INVISIBLE);
        }
        takeoffsetbtn = (Button)findViewById(R.id.takeoffset);
        takeoffsetbtn.setText("고도"+dronealtitude);
        LinearLayout list1 = (LinearLayout)findViewById(R.id.maplocklayer);
        LinearLayout list2 = (LinearLayout)findViewById(R.id.mapoptionlayer);
        LinearLayout list3 = (LinearLayout)findViewById(R.id.mapcadstrallayer);
        LinearLayout list4 = (LinearLayout)findViewById(R.id.missiondrawer);
        LinearLayout list5 = (LinearLayout)findViewById(R.id.abdistancesetlist);
        LinearLayout list6 = (LinearLayout)findViewById(R.id.spraysetlist);
        btnset =(LinearLayout)findViewById(R.id.linearLayout3);
        btnset.setVisibility(View.INVISIBLE);
        list1.setVisibility(View.INVISIBLE);
        list2.setVisibility(View.INVISIBLE);
        list3.setVisibility(View.INVISIBLE);
        list4.setVisibility(View.INVISIBLE);
        list5.setVisibility(View.INVISIBLE);
        list6.setVisibility(View.INVISIBLE);

        guide = new GuideMode();
        dronepath = new PathOverlay();

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setReverseLayout(true);
        mLayoutManager.setStackFromEnd(true);


        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(mLayoutManager);

        adapter = new SimpleTextAdapter(alertlist);
        recyclerView.setAdapter(adapter);

        mapFragment.getMapAsync(this);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,  @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated()) { // 권한 거부됨
                mymap.setLocationTrackingMode(LocationTrackingMode.None);
            }
            return;
        }
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
    }
    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.mymap = naverMap;
        manageOverlays = new ManageOverlay(this.mymap,this);
        mymap.setOnMapLongClickListener((pointF, latLng) -> {
            droneguide(latLng);
        });
        
        mymap.setOnMapClickListener((pointF, latLng) -> {
            LatLong latlong = new LatLong(latLng.latitude,latLng.longitude);
            polygonMission(latlong);
            customMission(latlong);
            abMission(latlong);
            setWaypoint();
        });
        // 네이버 로고 위치 변경
        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setLogoMargin(2080, 0, 0, 925);

        // 나침반 제거
        uiSettings.setCompassEnabled(false);

        // 축척 바 제거
        uiSettings.setScaleBarEnabled(false);

        // 줌 버튼 제거
        uiSettings.setZoomControlEnabled(false);

        Button absetlist = (Button)findViewById(R.id.abdistance);

        Button spraylist = (Button)findViewById(R.id.spraygap);


        absetlist.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view){
                LinearLayout list = (LinearLayout)findViewById(R.id.abdistancesetlist);
                onlistbtnTap(list);
            }
        });
        spraylist.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view){
                LinearLayout list = (LinearLayout)findViewById(R.id.spraysetlist);
                onlistbtnTap(list);
            }
        });


    }


    // mission confirm
    private void setWaypoint(){
        final Mission mMission = new Mission();
        ArrayList<LatLng> list = manageOverlays.getpointlist();
        Log.d("Mission",""+list.size());
        for(int i=0;i<list.size();i++){
            Waypoint waypoint = new Waypoint();
            waypoint.setDelay(1);

            LatLongAlt latLongAlt = new LatLongAlt(list.get(i).latitude,list.get(i).longitude,dronealtitude);
            waypoint.setCoordinate(latLongAlt);

            mMission.addMissionItem(waypoint);
        }
        final Button Btnsendmission = (Button)findViewById(R.id.sendmission);

        Btnsendmission.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v){
                if(Btnsendmission.getText().equals("임무전송")){
                    if(manageOverlays.getspraylist().size()>0)
                        setMission(mMission);
                    else
                        alertUser("need Marker");
                }
                else if(Btnsendmission.getText().equals("임무시작")){
                    ChangeToAutoMode();
                    Btnsendmission.setText("임무중지");

                }
                else if(Btnsendmission.getText().equals("임무중지")){
                    ChangeToLoiterMode();
                    Btnsendmission.setText("임무전송");
                }
           }
        });
    }
    private void ChangeToLoiterMode(){
        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LOITER, new SimpleCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Loiter 모드로 변경 중...");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Loiter 모드 변경 실패 : " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Loiter 모드 변경 실패.");
            }
        });
    }

    private void ChangeToAutoMode(){
        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_AUTO, new SimpleCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Auto 모드로 변경 중...");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Auto 모드 변경 실패 : " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Auto 모드 변경 실패.");
            }
        });
    }
    public void setMission(Mission mMission){
        MissionApi.getApi(this.drone).setMission(mMission,true);

    }

    //mission pause
    private void pauseMission(){
        MissionApi.getApi(this.drone).pauseMission(null);
    }
    // mission abort
    private void Mission_sent(){
        alertUser("미션 업로드");
        Button Btnsendmission = (Button)findViewById(R.id.sendmission);
        Btnsendmission.setText("임무시작");
    }



//drawpolygon
//manageoverlay
    public void customMission(LatLong latLong){
        if(mission[2]==true){
            mission[0] = false;
            mission[1] =false;
            polygonPointList.add(latLong);
            manageOverlays.setPPosition(latLong);
            manageOverlays.drawCustomPath();
        }

    }
    public void polygonMission(LatLong latLong){
        if(mission[1]==true){
            mission[0] = false;
            mission[2] = false;
            addPolygonPoint(latLong);
        }
    }
    public void abMission(LatLong latLong){
        double angle1 = 1, angle2 = 1;
        int direction = 1;
        if(mission[0]==true)
        {
            polygonPointList.add(latLong);

            manageOverlays.setPPosition(latLong);
            mission[2] = false;
            mission[1] = false;
            if (polygonPointList.size() == 2) {
                angle1 = MathUtils.getHeadingFromCoordinates(polygonPointList.get(0), polygonPointList.get(1));
                LatLong newPoint = MathUtils.newCoordFromBearingAndDistance(polygonPointList.get(1), angle1 - (90 * direction), 100);
                //addPolygonPoint(newPoint);
                addABPoint(newPoint);


                //angle2 = MathUtils.getHeadingFromCoordinates(polygonPointList.get(1), polygonPointList.get(0));
                newPoint = MathUtils.newCoordFromBearingAndDistance(polygonPointList.get(0), angle1 - 90, 100);
                //addPolygonPoint(newPoint);
                addABPoint(newPoint);
            }
        }

    }
    public void missionClear(){
        mission[0] = false;
        mission[1] = false;
        mission[2] = false;
    }
    public void addABPoint(LatLong latLong){
        polygonPointList.add(latLong);

        manageOverlays.setPPosition(latLong);
        sprayAngle = MathUtils.getHeadingFromCoordinates(polygonPointList.get(0), polygonPointList.get(1));;
        try{
            makeGrid();
        }catch(Exception e){
            Log.d("myCheck","예외처리 : " + e.getMessage());
        }
    }

public void addPolygonPoint(LatLong latLong) {



    polygonPointList.add(latLong);

    manageOverlays.setPPosition(latLong);


    if (polygonPointList.size() == 1) {

    }



    if (polygonPointList.size() > 2) {
        manageOverlays.drawPolygon();
        sprayAngle = makeSprayAngle();

        try {
            makeGrid();
           // makeBound();
        } catch(Exception e) {
            Log.d("myCheck","예외처리 : " + e.getMessage());
        }
    }
}
    //폴리곤 라인 가장 긴 길이 찾아서 각도 반환
    protected double makeSprayAngle() {
        Polygon poly = makePoly();
        double angle = 0;
        double maxDistance = 0;
        List<LineLatLong> lineLatLongList = poly.getLines();
        for (LineLatLong lineLatLong : lineLatLongList) {
            double lineDistance = MathUtils.getDistance2D(lineLatLong.getStart(), lineLatLong.getEnd());
            if(maxDistance < lineDistance) {
                maxDistance = lineDistance;
                angle = lineLatLong.getHeading();
            }
        }
        Log.d("mycheck",""+angle);
        return angle;
    }

    private Polygon makePoly() {
        Polygon poly = new Polygon();
        List<LatLong> latLongList = new ArrayList<>();
        for(LatLong latLong : polygonPointList) {
            latLongList.add(latLong);
        }
        poly.addPoints(latLongList);
        return poly;
    }

    public void makeGrid() throws Exception {
        if(this == null) throw new Exception("PolygonSpray retreiving MapActivity returns null");

        List<LatLong> polygonPoints = new ArrayList<>();
        for(LatLong latLong : polygonPointList) {
            polygonPoints.add(latLong);
        }

        List<LineLatLong> circumscribedGrid = new CircumscribedGrid(polygonPoints, this.sprayAngle, sprayDistance).getGrid();
        List<LineLatLong> trimedGrid = new Trimmer(circumscribedGrid, makePoly().getLines()).getTrimmedGrid();

        for (int i = 0; i < trimedGrid.size(); i++) {
            LineLatLong line = trimedGrid.get(i);
            if(line.getStart().getLatitude() > line.getEnd().getLatitude()) {
                LineLatLong line1 = new LineLatLong(line.getEnd(),line.getStart());
                trimedGrid.set(i, line1);
            }
        }

        Gps mydrone = this.drone.getAttribute(AttributeType.GPS);
        try{
            LatLong dronePosition = new LatLong(mydrone.getPosition());
            Log.d("GPS","gps position has been activated ");
            double dist1 = MathUtils.pointToLineDistance(trimedGrid.get(0).getStart(), trimedGrid.get(0).getEnd(), dronePosition);
            double dist2 = MathUtils.pointToLineDistance(trimedGrid.get(trimedGrid.size()-1).getStart(), trimedGrid.get(trimedGrid.size()-1).getEnd(), dronePosition);

            if (dist2 < dist1) {
                Collections.reverse(trimedGrid);
                double distStart = MathUtils.getDistance2D(dronePosition, trimedGrid.get(trimedGrid.size()-1).getStart());
                double distEnd = MathUtils.getDistance2D(dronePosition, trimedGrid.get(trimedGrid.size()-1).getEnd());
                if (distStart > distEnd) {
                    for (int i = 0; i < trimedGrid.size(); i++) {
                        LineLatLong line = trimedGrid.get(i);
                        LineLatLong line1 = new LineLatLong(line.getEnd(),line.getStart());
                        trimedGrid.set(i, line1);
                    }
                }
            }
        }catch(NullPointerException e){
            Log.d("GPS","gps null point , initial position setted");
            LatLong dronePosition = new LatLong(37.5670135, 126.9783740);
            double dist1 = MathUtils.pointToLineDistance(trimedGrid.get(0).getStart(), trimedGrid.get(0).getEnd(), dronePosition);
            double dist2 = MathUtils.pointToLineDistance(trimedGrid.get(trimedGrid.size()-1).getStart(), trimedGrid.get(trimedGrid.size()-1).getEnd(), dronePosition);

            if (dist2 < dist1) {
                Collections.reverse(trimedGrid);
                double distStart = MathUtils.getDistance2D(dronePosition, trimedGrid.get(trimedGrid.size()-1).getStart());
                double distEnd = MathUtils.getDistance2D(dronePosition, trimedGrid.get(trimedGrid.size()-1).getEnd());
                if (distStart > distEnd) {
                    for (int i = 0; i < trimedGrid.size(); i++) {
                        LineLatLong line = trimedGrid.get(i);
                        LineLatLong line1 = new LineLatLong(line.getEnd(),line.getStart());
                        trimedGrid.set(i, line1);
                    }
                }
            }
        }



        for (int i = 0; i < trimedGrid.size(); i++) {
            LineLatLong line = trimedGrid.get(i);
            if (i % 2 != 0) {
                line = new LineLatLong(line.getEnd(), line.getStart());
                trimedGrid.set(i,line);
            }
        }

        sprayPointList.clear();
        for(LineLatLong lineLatLong : trimedGrid) {
            sprayPointList.add(lineLatLong.getStart());
            sprayPointList.add(lineLatLong.getEnd());
        }

        manageOverlays.drawSprayPoint(sprayPointList);

    }
    public void resetMarker(){
        manageOverlays.reset();
    }
//guidemode
    public boolean mydronestate(){
        State vehiclestate = this.drone.getAttribute(AttributeType.STATE);
        if(vehiclestate.isFlying())
            return true;
        else
            return false;
    }

    public void droneguide(LatLng latLng){

        if(dronestate){
            guide.mGuidedPoint = latLng;
            guide.mMarkerGuide.setPosition(latLng);
            guide.mMarkerGuide.setMap(mymap);
            guide.DialogSimple(this.drone,new LatLong(latLng.latitude,latLng.longitude));
        }



    }
    public void delMarker(){
        try{
            if(guide.CheckGoal(this.drone,guide.mGuidedPoint))
            {
                guide.mMarkerGuide.setMap(null);
                VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LOITER, new AbstractCommandListener() {
                    @Override
                    public void onSuccess() {
                        alertUser("목적지 도착");
                    }

                    @Override
                    public void onError(int executionError) {

                    }

                    @Override
                    public void onTimeout() {

                    }
                });

            }
        }catch(NullPointerException e){
            Log.d("NONMARKER","no marker exist");
        }

    }
//경로선
    public void pathline(){
        Gps dronegps = this.drone.getAttribute(AttributeType.GPS);
        LatLng droneposition = new LatLng(dronegps.getPosition().getLatitude(),dronegps.getPosition().getLongitude());
        try{
            pathcoords.add(droneposition);
            dronepath.setCoords(pathcoords);

            dronepath.setMap(mymap);

            Log.d("DRONEPATH","list size:"+pathcoords.size());
        }catch(NullPointerException e){
            Log.d("DRONEPATH","gps position list is null");
        }


    }
    protected void alertUser(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, message);
        alertlist.add(message);

        recyclerView.setAdapter(adapter);
        recyclerView.scrollToPosition(alertlist.size()-1);
    }

    public void onLandButtonTap(){
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        if (vehicleState.isFlying()) {
            // Land
            VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LAND, new SimpleCommandListener() {
                @Override
                public void onError(int executionError) {
                    alertUser("Unable to land the vehicle.");
                }

                @Override
                public void onTimeout() {
                    alertUser("Unable to land the vehicle.");
                }
            });
        }
    }

    public void takeoffsetTap(){

        altitudeset = !altitudeset;
        if(altitudeset)
        {
            setlist.setVisibility(View.VISIBLE);
        }
        else
            setlist.setVisibility(View.INVISIBLE);
    }
    public void onAsecTap(){

       if(dronealtitude<10){
           dronealtitude += 0.5;
           takeoffsetbtn.setText("이륙고도 "+dronealtitude);
       }
    }
    public void onDescTap(){

        if(dronealtitude>3){
            dronealtitude -= 0.5;
            takeoffsetbtn.setText("이륙고도 "+dronealtitude);
        }

    }
    //distance , gap increase button
    public void onIncBtnTap(int id){
        switch(id){
            case R.id.abinc:
                Button abinc = (Button)findViewById(R.id.abdistance);
                maxSprayDistance += 10;
                abinc.setText("AB거리 "+maxSprayDistance);
                break;
            case R.id.sprayinc:
                Button sprayinc = (Button)findViewById(R.id.spraygap);
                sprayDistance += 0.5;
                sprayinc.setText("비행폭 "+sprayDistance);
                break;

        }
    }
    public void onDecBtnTap(int id){
        switch(id){
            case R.id.abdec:
                Button abdec = (Button)findViewById(R.id.abdistance);
                if(maxSprayDistance>10)
                    maxSprayDistance-=10;
                abdec.setText("AB거리 "+maxSprayDistance);

                break;
            case R.id.spraydec:
                Button spraydec = (Button)findViewById(R.id.spraygap);
                if(sprayDistance>0.5)
                    sprayDistance-=0.5;
                spraydec.setText("비행폭 "+sprayDistance);
                break;
        }
    }
    public void alertMessage(){
        Drone mydrone = this.drone;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("arming alert");
        builder.setMessage("모터를 가동합니다");
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int id)
            {
                VehicleApi.getApi(mydrone).arm(true, false, new SimpleCommandListener() {
                    @Override
                    public void onError(int executionError) {
                        alertUser("Unable to arm vehicle.");
                    }

                    @Override
                    public void onTimeout() {
                        alertUser("Arming operation timed out.");
                    }
                });
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                dialog.cancel();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
    public void takeoffAlert(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        Drone mydrone = this.drone;
        builder.setTitle("takeoff alert");
        builder.setMessage("기체가 상승합니다 안전거리 유지 바랍니다.");
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ControlApi.getApi(mydrone).takeoff(dronealtitude,new AbstractCommandListener(){
                    @Override
                    public void onSuccess() {
                        alertUser("Taking off...");
                    }

                    @Override
                    public void onError(int i) {
                        alertUser("Unable to take off.");
                    }

                    @Override
                    public void onTimeout() {
                        alertUser("Unable to take off.");
                    }
                });
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();

            }
        });

        AlertDialog alertdialog = builder.create();
        alertdialog.show();
    }
    public void onArmButtonTap() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);

         if (vehicleState.isArmed()) {
            // Take off
            takeoffAlert();

        } else if (!vehicleState.isConnected()) {
            // Connect
            alertUser("Connect to a drone first");
        } else {
             alertMessage();
            // Connected but not Armed

        }
    }

    private void checkSoloState() {
        final SoloState soloState = drone.getAttribute(SoloAttributes.SOLO_STATE);
        if (soloState == null) {
            alertUser("Unable to retrieve the solo state.");
        } else {
            alertUser("Solo state is up to date.");
        }
    }
    public void onFlightModeSelected(View view) {
        VehicleMode vehicleMode = (VehicleMode) this.modeSelector.getSelectedItem();

        VehicleApi.getApi(this.drone).setVehicleMode(vehicleMode, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Vehicle mode change successful.");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Vehicle mode change failed: " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Vehicle mode change timed out.");
            }
        });
    }


    @Override
    public void onDroneEvent(String event, Bundle extras) {
        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                alertUser("Drone Connected");
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();

                checkSoloState();
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("Drone Disconnected");
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();

                break;
            case AttributeEvent.STATE_UPDATED:

                break;
            case AttributeEvent.STATE_ARMING:
                updateArmButton();
                dronestate = mydronestate();
                break;
            case AttributeEvent.TYPE_UPDATED:
                Type newDroneType = this.drone.getAttribute(AttributeType.TYPE);
                if (newDroneType.getDroneType() != this.droneType) {
                    this.droneType = newDroneType.getDroneType();
                    updateVehicleModesForType(this.droneType);
                }
                break;
            case AttributeEvent.STATE_VEHICLE_MODE:
                updateVehicleMode();

                break;
            case AttributeEvent.SPEED_UPDATED:
                updateSpeed();
                break;
            case AttributeEvent.ALTITUDE_UPDATED:
                updateAltitude();
                break;
            case AttributeEvent.GPS_POSITION:
                updatetrack();
                delMarker();
                pathline();
                break;
            case AttributeEvent.BATTERY_UPDATED:
                updateVolt();
                break;
            case AttributeEvent.ATTITUDE_UPDATED:
                updateYaw();
                break;
            case AttributeEvent.GPS_COUNT:
                updateNumberOfSatellites();
                break;
            case AttributeEvent.MISSION_SENT:
                Mission_sent();
                break;
            case AttributeEvent.MISSION_ITEM_REACHED:

                break;
            default:
                // Log.i("DRONE_EVENT", event); //Uncomment to see events from the drone
                break;
        }
    }


    protected void updatetrack(){
        try{

            Gps dronegps = this.drone.getAttribute(AttributeType.GPS);
            LatLng droneposition = new LatLng(dronegps.getPosition().getLatitude(),dronegps.getPosition().getLongitude());

            Log.d("GPSERROR1",""+droneposition.latitude);
            this.locationOverlay = mymap.getLocationOverlay();
            locationOverlay.setVisible(true);
            locationOverlay.setIcon(OverlayImage.fromResource(R.drawable.gcsmarker));
            locationOverlay.setPosition(droneposition);
            if(mapfollow)
                mymap.moveCamera(CameraUpdate.scrollTo(droneposition));
        }catch(NullPointerException e){
            Log.d("GPSERROR","GPS POSITION NULL");
           // locationOverlay = mymap.getLocationOverlay();
            this.locationOverlay = mymap.getLocationOverlay();
            locationOverlay.setVisible(true);
            locationOverlay.setIcon(OverlayImage.fromResource(R.drawable.gcsmarker));
            locationOverlay.setPosition(new LatLng(35.945378,126.682110));
            //locationOverlay.setAnchor(new PointF((float)0.5,(float)0.5));
            if(mapfollow)
                mymap.moveCamera(CameraUpdate.scrollTo(new LatLng(35.945378,126.682110)));

        }
        //
        //mymap.setLocationTrackingMode(LocationTrackingMode.Follow);
    }
    protected void updateNumberOfSatellites() {
        TextView numberOfSatellitesTextView = (TextView)findViewById(R.id.satenum);
        Gps droneNumberOfSatellites = this.drone.getAttribute(AttributeType.GPS);

        numberOfSatellitesTextView.setText(String.format("%d", droneNumberOfSatellites.getSatellitesCount()));
    }
    protected void updateVehicleModesForType(int droneType) {

        List<VehicleMode> vehicleModes = VehicleMode.getVehicleModePerDroneType(droneType);
        ArrayAdapter<VehicleMode> vehicleModeArrayAdapter = new ArrayAdapter<VehicleMode>(this, android.R.layout.simple_spinner_item, vehicleModes);
        vehicleModeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.modeSelector.setAdapter(vehicleModeArrayAdapter);
    }

    protected void updateVehicleMode() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        ArrayAdapter arrayAdapter = (ArrayAdapter) this.modeSelector.getAdapter();
        this.modeSelector.setSelection(arrayAdapter.getPosition(vehicleMode));
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }
   /* protected void updateTakeOffDrawer(){
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        if(vehicleState.isArmed())
        {
            armstatus = true;
            takeoffsetbtn.setVisibility(View.VISIBLE);

        }
        else{
            armstatus = false;
            takeoffsetbtn.setVisibility(View.INVISIBLE);

        }

    }*/
    protected void updateArmButton() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        Button armButton = (Button) findViewById(R.id.arm);

        if (!this.drone.isConnected()) {
            armingbtn.setVisibility(View.INVISIBLE);
        } else {
            armingbtn.setVisibility(View.VISIBLE);
        }


        if (vehicleState.isArmed()) {
            // Take off

            armButton.setText("TAKE-OFF");

        } else if (vehicleState.isConnected()) {
            // Connected but not Armed
            armButton.setText("ARM");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect((TowerListener) this);

    }
    @Override
    public void onStop() {
        super.onStop();
        if (this.drone.isConnected()) {
            this.drone.disconnect();
            updateConnectedButton(false);
        }

        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();

    }

    protected void updateConnectedButton(Boolean isConnected) {
        Button connectButton = (Button) findViewById(R.id.connect);
        if (isConnected) {
            connectButton.setText("Disco");
            connectDrone = false;
            armingbtn.setVisibility(View.INVISIBLE);
        } else {
            connectButton.setText("Conn");
            connectDrone = true;
            armingbtn.setVisibility(View.VISIBLE);
        }
    }

    public void onBtnConnectTap() {
        if (this.drone.isConnected()) {
            this.drone.disconnect();
        } else {
            ConnectionParameter connectionParams = ConnectionParameter.newUdpConnection(null);
            this.drone.connect(connectionParams);
        }

    }

    //button event list
    public void btn_event(View v){
        LinearLayout missiondrawlist = (LinearLayout)findViewById(R.id.missiondrawer);
        Button misBtn = (Button)findViewById(R.id.mission);
        switch(v.getId()){
            case R.id.connect:
                onBtnConnectTap();
                break;
            case R.id.arm:
                onArmButtonTap();
                break;
            case R.id.land:
                onLandButtonTap();
                break;
            case R.id.takeoffset:
                takeoffsetTap();
                break;
            case R.id.drone_asec:
                onAsecTap();
                break;
            case R.id.drone_desc:
                onDescTap();
                break;
            case R.id.maplockbtn:

                LinearLayout list = (LinearLayout)findViewById(R.id.maplocklayer);

                onlistbtnTap(list);
                break;
            case R.id.mapoptionbtn:

                LinearLayout list1 = (LinearLayout)findViewById(R.id.mapoptionlayer);
                onlistbtnTap(list1);
                break;
            case R.id.mapcadastral:

                LinearLayout list2 = (LinearLayout)findViewById(R.id.mapcadstrallayer);
                onlistbtnTap(list2);
                break;
            case R.id.maplock:
                mapfollow = true;
                mapfollowTap();
                break;
            case R.id.mapmove:
                mapfollow = false;
                mapfollowTap();
                break;
            case R.id.basicmap:
                onMapOptionTap(R.id.basicmap);
                break;
            case R.id.satellitemap:
                onMapOptionTap(R.id.satellitemap);
                break;
            case R.id.hybridmap:
                onMapOptionTap(R.id.hybridmap);
                break;
            case R.id.cadaston:
                onCadastTap(R.id.cadaston);
                break;
            case R.id.cadastoff:
                onCadastTap(R.id.cadastoff);
                break;
            case R.id.toggle:
                onToggleTap();
                break;
            case R.id.mission:
                missiondrawlist = (LinearLayout)findViewById(R.id.missiondrawer);
                if(missiondrawlist.getVisibility() == View.INVISIBLE)
                    missiondrawlist.setVisibility(View.VISIBLE);
                else
                    missiondrawlist.setVisibility(View.INVISIBLE);
                break;
            case R.id.nomission:
                resetMarker();
                polygonPointList.clear();
                missionClear();

                misBtn = (Button)findViewById(R.id.mission);
                misBtn.setText("NONE");
                missiondrawlist.setVisibility(View.INVISIBLE);
                break;
            case R.id.PolygonMission:
                mission[1] = true;
                misBtn = (Button)findViewById(R.id.mission);
                misBtn.setText("다각형");
                missiondrawlist.setVisibility(View.INVISIBLE);
                break;
            case R.id.ABMission:
                mission[0] = true;
                misBtn = (Button)findViewById(R.id.mission);
                misBtn.setText("AB");
                missiondrawlist.setVisibility(View.INVISIBLE);
                break;
            case R.id.customMission:
                mission[2] = true;
                misBtn = (Button)findViewById(R.id.mission);
                misBtn.setText("Cust");
                missiondrawlist.setVisibility(View.INVISIBLE);
                break;

            case R.id.abinc:
                onIncBtnTap(R.id.abinc);
                break;
            case R.id.abdec:
                onDecBtnTap(R.id.abdec);
                break;
            case R.id.sprayinc:
                onIncBtnTap(R.id.sprayinc);
                break;
            case R.id.spraydec:
                onDecBtnTap(R.id.spraydec);
                break;
        }
    }
    public void onToggleTap(){
        togglebtn = !togglebtn;
        LinearLayout list1 = (LinearLayout)findViewById(R.id.maplocklayer);
        LinearLayout list2 = (LinearLayout)findViewById(R.id.mapoptionlayer);
        LinearLayout list3 = (LinearLayout)findViewById(R.id.mapcadstrallayer);
        if(togglebtn){
            btnset.setVisibility(View.VISIBLE);
        }
        else{
            maplock = false;
            mapoption = false;
            mapcads = false;
            list1.setVisibility(View.INVISIBLE);
            list2.setVisibility(View.INVISIBLE);
            list3.setVisibility(View.INVISIBLE);
            btnset.setVisibility(View.INVISIBLE);
        }

    }
    public void onCadastTap(int id){
        Button cadastbtn = (Button)findViewById(R.id.mapcadastral);
        LinearLayout list = (LinearLayout)findViewById(R.id.mapcadstrallayer);

        switch(id){
            case R.id.cadaston:
                mymap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, true);
                cadastbtn.setText("지적도on");
                break;
            case R.id.cadastoff:
                mymap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, false);
                cadastbtn.setText("지적도off");
                break;
        }
        mapcads = false;
        list.setVisibility(View.INVISIBLE);
    }
    public void onMapOptionTap(int id){
        Button mapoptionbtn = (Button)findViewById(R.id.mapoptionbtn);
        LinearLayout list = (LinearLayout)findViewById(R.id.mapoptionlayer);

        switch(id)
        {
            case R.id.basicmap:
                mymap.setMapType(NaverMap.MapType.Basic);
                mapoptionbtn.setText("기본지도");
                break;
            case R.id.satellitemap:
                mymap.setMapType(NaverMap.MapType.Satellite);
                mapoptionbtn.setText("위성지도");
                break;
            case R.id.hybridmap:
                mymap.setMapType(NaverMap.MapType.Hybrid);
                mapoptionbtn.setText("hybrid");
                break;

        }
        mapoption = false;
        list.setVisibility(View.INVISIBLE);
    }
    public void mapfollowTap(){
        Button lockbtn = (Button)findViewById(R.id.maplockbtn);
        LinearLayout list = (LinearLayout)findViewById(R.id.maplocklayer);

        if(mapfollow)
            lockbtn.setText("맵 잠금");
        else
            lockbtn.setText("맵 이동");

        maplock = false;
        list.setVisibility(View.INVISIBLE);
    }

    public void onlistbtnTap(LinearLayout list){
        if(list.getVisibility() == View.INVISIBLE){
            list.setVisibility(View.VISIBLE);
        }
        else{
            list.setVisibility(View.INVISIBLE);
        }
    }


    protected void updateAltitude() {

        TextView altitudeTextView = (TextView) findViewById(R.id.altitude);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        altitudeTextView.setText(String.format("%3.1f", droneAltitude.getAltitude()) + "m");



    }

    protected void updateSpeed() {
        TextView speedTextView = (TextView) findViewById(R.id.speed);
        Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
        speedTextView.setText(String.format("%3.1f", droneSpeed.getGroundSpeed()) + "m/s");
    }

    protected void updateVolt(){
        TextView voltTextView = (TextView)findViewById(R.id.volt);
        Battery droneVolt = this.drone.getAttribute(AttributeType.BATTERY);
        voltTextView.setText(String.format("%3.2f",droneVolt.getBatteryVoltage())+"V");
    }

    protected void updateYaw(){
        double yawvalue=0;
        TextView yawTextView = (TextView)findViewById(R.id.YAW1);
        Attitude droneyaw = this.drone.getAttribute(AttributeType.ATTITUDE);
        if(droneyaw.getYaw()<0)
            yawvalue = droneyaw.getYaw()+360;
        else
            yawvalue = droneyaw.getYaw();
        
        yawTextView.setText(String.format("%3.1f",yawvalue));
        locationOverlay.setBearing((float) droneyaw.getYaw());
    }

    protected double distanceBetweenPoints(LatLongAlt pointA, LatLongAlt pointB) {
        if (pointA == null || pointB == null) {
            return 0;
        }
        double dx = pointA.getLatitude() - pointB.getLatitude();
        double dy = pointA.getLongitude() - pointB.getLongitude();
        double dz = pointA.getAltitude() - pointB.getAltitude();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }



    @Override
    public void onLinkStateUpdated(@NonNull LinkConnectionStatus connectionStatus) {
        switch(connectionStatus.getStatusCode()){
            case LinkConnectionStatus.FAILED:
                Bundle extras = connectionStatus.getExtras();
                String msg = null;
                if (extras != null) {
                    msg = extras.getString(LinkConnectionStatus.EXTRA_ERROR_MSG);
                }
                alertUser("Connection Failed:" + msg);
                break;
        }
    }

    @Override
    public void onTowerConnected() {
        alertUser("DroneKit-Android Connected");
        this.controlTower.registerDrone(this.drone, this.handler);
        this.drone.registerDroneListener(this);
    }

    @Override
    public void onTowerDisconnected() {
        alertUser("DroneKit-Android Interrupted");
    }


    private boolean hasPermission() {
        return PermissionChecker.checkSelfPermission(this, PERMISSIONS[0])
                == PermissionChecker.PERMISSION_GRANTED
                && PermissionChecker.checkSelfPermission(this, PERMISSIONS[1])
                == PermissionChecker.PERMISSION_GRANTED;
    }


    //가이드 모드
    class GuideMode {
        LatLng mGuidedPoint; //가이드모드 목적지 저장
        Marker mMarkerGuide = new Marker(); //GCS 위치 표마커 옵션

        void DialogSimple(final Drone drone, final LatLong point) {
            AlertDialog.Builder alt_bld = new AlertDialog.Builder(MainActivity.this);
            alt_bld.setMessage("확인하시면 가이드모드로 전환후 기체가 이동합니다.").setCancelable(false).setPositiveButton("확인", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
// Action for 'Yes' Button
                    VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_GUIDED,
                            new AbstractCommandListener() {
                                @Override

                                public void onSuccess() {

                                    ControlApi.getApi(drone).goTo(point, true, null);
                                }
                                @Override
                                public void onError(int i) {

                                }
                                @Override
                                public void onTimeout() {
                                }
                            });
                }
            }).setNegativeButton("취소", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();

                }
            });
            AlertDialog alert = alt_bld.create();
            // Title for AlertDialog
            alert.setTitle("Title");
            // Icon for AlertDialog

            alert.show();
        }
        public boolean CheckGoal(final Drone drone, LatLng recentLatLng) {
            GuidedState guidedState = drone.getAttribute(AttributeType.GUIDED_STATE);
            LatLng target = new LatLng(guidedState.getCoordinate().getLatitude(),
                    guidedState.getCoordinate().getLongitude());
            return target.distanceTo(recentLatLng) <= 1;
        }
    }
}


