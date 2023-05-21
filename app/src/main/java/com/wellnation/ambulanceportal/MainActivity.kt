package com.wellnation.ambulanceportal
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.opengl.Visibility
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.location.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.messaging.FirebaseMessaging
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.android.PolyUtil
import com.google.maps.model.DirectionsResult
import com.wellnation.ambulanceportal.databinding.ActivityMainBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private  lateinit var locationRequest: LocationRequest
    private  val pERMISSION_CODE = 100
    private lateinit var mMap: GoogleMap
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.mapView.onCreate(savedInstanceState)
        fusedLocationProviderClient =LocationServices.getFusedLocationProviderClient(this)
        getLastLocation()
        plotrode()
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { tokenResult ->
                val token = tokenResult
                val db = FirebaseFirestore.getInstance().collection("ambulance").document(ambulanceData.id)
                val hashmap: HashMap<String, String> = HashMap()
                hashmap["fcmToken"] = token
                db.update(hashmap as Map<String, Any>)
                    .addOnSuccessListener {
                        Log.d("TAG", "FCM token updated in Firestore")
                    }
                    .addOnFailureListener { e ->
                        Log.e("TAG", "Error updating FCM token in Firestore", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("TAG", "Error retrieving FCM token", e)
            }

        setContentView(binding.root)
    }
    @OptIn(DelicateCoroutinesApi::class)
    private fun plotrode(){
          val db = FirebaseFirestore.getInstance().collection("ambulance").document(ambulanceData.id)
          db.get().addOnSuccessListener {
            var origin = LatLng(37.422, -122.084) // Replace with your origin
            var destination = LatLng(37.7749, -122.4194) // Replace with your destination
            if (it != null) {
                val pickupLocation = it.getGeoPoint("pickuplocation")
                if (pickupLocation != null) {
                    destination = LatLng(pickupLocation.latitude,pickupLocation.longitude)
                }
                var mylocation = it.getGeoPoint("currentlocation")
                if (mylocation != null) {
                    origin = LatLng(mylocation.latitude,mylocation.longitude)
                }
                val status = it.getBoolean("status")
                if (!status!!){
                    binding.mapView.visibility = View.VISIBLE
                    binding.eta.visibility = View.VISIBLE
                    binding.bookingstatus.visibility = View.INVISIBLE
                    val apiKey = "AIzaSyAOjr36nWK1pfruFvU8w49Pb_BKZSmWlYk" // Replace with your Google Maps API key
                    val geoApiContext = GeoApiContext.Builder()
                        .apiKey(apiKey)
                        .build()

                    GlobalScope.launch(Dispatchers.IO) {
                        try {
                            val directionsResult: DirectionsResult = DirectionsApi
                                .newRequest(geoApiContext)
                                .origin(origin.latitude.toString() + "," + origin.longitude)
                                .destination(destination.latitude.toString() + "," + destination.longitude)
                                .await()

                            withContext(Dispatchers.Main) {
                                val distanceInMeters = directionsResult.routes[0].legs[0].distance.inMeters
                                val distanceInKm = distanceInMeters / 1000
                                val durationInSeconds = directionsResult.routes[0].legs[0].duration.inSeconds
                                val durationInMinutes = durationInSeconds / 60
                                binding.eta.text = "Distance-" + distanceInKm +" Km"+" And Arriving In-"+durationInMinutes+" mins"
                                Toast.makeText(this@MainActivity,durationInMinutes.toString(),Toast.LENGTH_SHORT).show()
                                val points = mutableListOf<LatLng>()
                                for (step in directionsResult.routes[0].legs[0].steps) {
                                    points.addAll(PolyUtil.decode(step.polyline.encodedPath))
                                }
                                Log.d("lists",points.toString())
                                val mapView = binding.mapView
                                val originalBitmap = BitmapFactory.decodeResource(resources, com.wellnation.ambulanceportal.R.drawable.ambulance)

// Define the desired width and height of the resized bitmap
                                val width = 80
                                val height = 80

// Create a new bitmap with the desired width and height
                                val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, false)

// Create a BitmapDescriptor from the resized bitmap
                                val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(resizedBitmap)
                                mapView.onCreate(null)
                                mapView.onResume()
                                mapView.getMapAsync { googleMap ->
                                    googleMap.clear()
                                    val markerOptions = MarkerOptions()
                                        .position(origin)
                                        .icon(bitmapDescriptor)
                                    googleMap.addMarker(markerOptions)
                                    val polylineOptions = PolylineOptions()
                                        .addAll(points)
                                        .color(Color.BLUE)
                                        .width(10f)
                                    googleMap.addPolyline(polylineOptions)
                                    googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this@MainActivity,com.wellnation.ambulanceportal.R.raw.mapstyle))
                                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(origin, 15f))
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.d("Error",e.toString())
                        }
                    }
                }
                else{
                    binding.mapView.visibility = View.INVISIBLE
                    binding.eta.visibility = View.INVISIBLE
                    binding.bookingstatus.visibility = View.VISIBLE
                    binding.bookingstatus.text = "Waiting for Calls"
                }

            }
        }
    }
    @SuppressLint("MissingPermission")
    private fun getLastLocation(){
        //check for the permissions
        if (checkPermissions()){
            //check if location service is enabled
            if (isLocationEnabled()){
                //lets get the location
                fusedLocationProviderClient.lastLocation.addOnCompleteListener { task ->
                    val location: Location? = task.result
                    if (location == null) {
                        //if location is null we will get new user location
                        //add new location function here
                        Log.d("check","if called")
                        startLocationUpdates()

                    } else {
                        Log.d("check","else called")
                        startLocationUpdates()
                    }
                }
            }else Toast.makeText(this,"Please enable the Location Services",Toast.LENGTH_SHORT).show()
        }else RequestPermission()
    }
    //Function to check the user permissions
    private fun checkPermissions() :Boolean{
        return ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
    }
    //Function to check if location service of the device is enabled
    private fun isLocationEnabled(): Boolean {
        var locationManager : LocationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)|| locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    }
    //Function that will allow us to get user permissions
    private fun RequestPermission(){
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION,android.Manifest.permission.ACCESS_FINE_LOCATION),pERMISSION_CODE)
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode==pERMISSION_CODE){
            if (grantResults.isNotEmpty()&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                Log.d("Debug:","You have the permission")
            }
        }
    }

    private val DEFAULT_UPDATE_INTERVAL = 10 * 1000 // 30 seconds
    private val FAST_UPDATE_INTERVAL:Long = 5000 // 20 seconds
    private fun startLocationUpdates() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = FAST_UPDATE_INTERVAL
        }

        //locationRequest = LocationRequest.create()
//        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
//        locationRequest.interval = DEFAULT_UPDATE_INTERVAL.toLong()
//        locationRequest.fastestInterval = FAST_UPDATE_INTERVAL.toLong()
//

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            RequestPermission()
            return
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                if (p0 != null) {
                    val location = p0.lastLocation
                    // Use location data
                    val currentLocation = location?.let {
                        GeoPoint(location.latitude,
                            it.longitude)
                    }
                    val db = FirebaseFirestore.getInstance().collection("ambulance").document(ambulanceData.id)
                    db.update("currentlocation",currentLocation)
                    plotrode()
                }
            }
        }, Looper.myLooper())
    }

}
