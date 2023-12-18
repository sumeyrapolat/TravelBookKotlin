package com.sumeyra.kotlinmaps.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.sumeyra.kotlinmaps.R
import com.sumeyra.kotlinmaps.adapter.PlaceAdapter
import com.sumeyra.kotlinmaps.databinding.ActivityMainBinding
import com.sumeyra.kotlinmaps.model.Place
import com.sumeyra.kotlinmaps.roomdb.PlaceDao
import com.sumeyra.kotlinmaps.roomdb.PlaceDatabase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

class MainActivity : AppCompatActivity() {

    private lateinit var  binding: ActivityMainBinding
    private val compositeDisposable= CompositeDisposable()
    private lateinit var db: PlaceDatabase
    private lateinit var placeDao: PlaceDao
    private lateinit var placeAdapter: PlaceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        db= Room.databaseBuilder(applicationContext, PlaceDatabase::class.java,"Places")
            //.allowMainThreadQueries() -> uygulama çökmez çünkü çok küçük verilerle uğraşıyoruz
            .build()
        placeDao = db.placeDao()

        compositeDisposable.add(
            placeDao.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleResponse)
        )


    }

    private fun handleResponse(placeList: List<Place>){
        //ADAPTER
        binding.recyclerView.layoutManager= LinearLayoutManager(this)
        placeAdapter= PlaceAdapter(placeList)
        binding.recyclerView.adapter= placeAdapter
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        //inflater
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.add_place,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //intent
        if (item.itemId == R.id.add_place){
            val intent= Intent(this@MainActivity, MapsActivity::class.java)
            intent.putExtra("info","new")
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }

}