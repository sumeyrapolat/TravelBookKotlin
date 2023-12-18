package com.sumeyra.kotlinmaps.view

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.google.android.gms.common.util.AndroidUtilsLight

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.sumeyra.kotlinmaps.R
import com.sumeyra.kotlinmaps.databinding.ActivityMapsBinding
import com.sumeyra.kotlinmaps.model.Place
import com.sumeyra.kotlinmaps.roomdb.PlaceDao
import com.sumeyra.kotlinmaps.roomdb.PlaceDatabase
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnMapLongClickListener{

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener : LocationListener
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var sharedPreferences: SharedPreferences
    private var trackBoolean : Boolean? = null
    private var selectedLatitude : Double? =null
    private var selectedLongitute : Double? = null
    private lateinit var db: PlaceDatabase
    private lateinit var placeDao: PlaceDao
    val compositeDisposable = CompositeDisposable()
    var selectedPlace: Place? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        registerLauncher()

        sharedPreferences= this.getSharedPreferences("com.sumeyra.kotlinmaps", MODE_PRIVATE)
        trackBoolean = false
        selectedLatitude = 0.0
        selectedLongitute =0.0

        db= Room.databaseBuilder(applicationContext,PlaceDatabase::class.java,"Places")
            //.allowMainThreadQueries() -> uygulama çökmez çünkü çok küçük verilerle uğraşıyoruz
            .build()
        placeDao = db.placeDao()

        binding.saveButton.isEnabled= false


    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapLongClickListener(this)

        //intent al

        val intent= intent
        val info = intent.getStringExtra("info")
        if (info.equals("new")){
            //ekleme sayfası
            binding.deleteButton.visibility= View.GONE
            binding .saveButton.visibility = View.VISIBLE
            //casting
            locationManager= this.getSystemService(LOCATION_SERVICE) as LocationManager
            locationListener = object: LocationListener{


                override fun onLocationChanged(location: Location) {

                    trackBoolean = sharedPreferences.getBoolean("trackBoolean",false)
                    if (trackBoolean == false){ // if(!trackBoolean!!))

                        val userLocation = LatLng(location.latitude,location.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,15f))
                        sharedPreferences.edit().putBoolean("trackBoolean",false).apply()
                    }
                }
            }
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                //request permission
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION )){
                    //Snackbar + request permission
                    Snackbar.make(binding.root,"permission needed for location",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission",
                        View.OnClickListener {
                            //request permission
                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }).show()
                }else{
                    //request permission
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

                }
            }else{
                //permission granted
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,10f,locationListener)
                val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if(lastLocation != null){
                    val lastUserLocation = LatLng(lastLocation.latitude,lastLocation.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,15f))

                }
                mMap.isMyLocationEnabled= true
            }

        }else{
            //kaydedileni göster sayfası
            mMap.clear()
            selectedPlace = intent.getSerializableExtra("selectedPlace") as? Place
            selectedPlace?.let {place->
                val latlng = LatLng(place.latitude,place.longitude)
                mMap.addMarker(MarkerOptions().position(latlng).title(place.name))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng,15f))
                binding.placeText.setText(place.name)
                binding.deleteButton.visibility= View.VISIBLE
                binding.saveButton.visibility= View.GONE

                }


        }






       /*
        // 48.858617126749856, 2.294427652886829
        val eifell = LatLng(48.858617126749856, 2.294427652886829)
        mMap.addMarker(MarkerOptions().position(eifell).title("Eifell Tower"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(eifell,15f))*/
    }

    private fun registerLauncher(){
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result ->
            if (result){
                if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
                    //permission granted
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,10f,locationListener)
                    val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if(lastLocation != null){
                        val lastUserLocation = LatLng(lastLocation.latitude,lastLocation.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,15f))

                    }
                    mMap.isMyLocationEnabled= true
                }
            }else{
                //permission denied
                Toast.makeText(this@MapsActivity,"Permission Needed!", Toast.LENGTH_LONG).show()
            }

        }
    }

    override fun onMapLongClick(p0: LatLng) {
        //mMap.setOnMapLongClickListener(this) -> bunu onmapready e ekle
        mMap.clear()
        mMap.addMarker(MarkerOptions().position(p0))

        selectedLatitude= p0.latitude
        selectedLongitute =p0.longitude
        binding.saveButton.isEnabled= true

    }

    fun save(view : View){
        val place = Place(binding.placeText.text.toString(),selectedLatitude!!,selectedLongitute!!)
        //placeDao.insert(place) // çöküyor çünkü main thread bu işlem için uygun değil
        compositeDisposable.add(
            placeDao.insert(place)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleResponse)
        )

    }

    fun handleResponse(){
        val intent= Intent(this@MapsActivity,MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)

    }

    fun delete(view : View){
        selectedPlace?.let{
            compositeDisposable.add(
                placeDao.delete(it)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponse)

            )
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }
}