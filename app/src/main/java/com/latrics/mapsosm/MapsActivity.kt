package com.latrics.mapsosm

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.DashPathEffect
import android.graphics.Path
import android.graphics.PathDashPathEffect
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.tasks.CancellationTokenSource
import com.latrics.mapsosm.databinding.ActivityMapsBinding
import com.latrics.mapsosm.ui.MapViewmodel
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.shape.ShapeConverter
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView.getTileSystem
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random


class MapsActivity : AppCompatActivity(), MapEventsReceiver {

    val viewmodel by viewModels<MapViewmodel>()

    var shapeType = ShapeType.POLYLINE
    val locationPermissions = arrayOf(
        ACCESS_COARSE_LOCATION,
        ACCESS_FINE_LOCATION
    )
    var center = GeoPoint(0.0, 0.0)
    val fusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    val distanceMarker by lazy {
        Marker(binding.map)
    }


    val polyline by lazy {
        Polyline(binding.map)
    }
    val polygon by lazy {
        Polygon(binding.map)
    }

    var dashedPolyline = Polyline()


    private val locationPermissionLauncher by lazy {
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions[ACCESS_FINE_LOCATION] == true && permissions[ACCESS_COARSE_LOCATION] == true -> {
                    Log.d("ganga", "given ${permissions.toString()}")
                }

                else -> {
                    Log.d("ganga", "denied ${permissions.toString()}")
                }
            }
        }
    }


    private val binding: ActivityMapsBinding by lazy {
        ActivityMapsBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        requestLocationPermission()
        configureCacheTilePath()
        configureMap()
        binding.map.overlayManager.add(polyline)
        binding.map.overlayManager.add(polygon)
        binding.add.setOnClickListener {
            when (shapeType) {
                ShapeType.POINT -> addMarkers()
                ShapeType.POLYLINE -> addPolyLine()
                ShapeType.POLYGON -> addPolygon()
                ShapeType.SHAPE -> addShape()
            }
        }

        binding.addLocation.setOnClickListener {
            shapeType = ShapeType.POINT
        }
        binding.addPolyline.setOnClickListener {
            shapeType = ShapeType.POLYLINE
        }
        binding.addPolygon.setOnClickListener {
            shapeType = ShapeType.POLYGON
        }
        binding.addShape.setOnClickListener {
            shapeType = ShapeType.SHAPE
        }
        binding.map.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {

                when (shapeType) {
                    ShapeType.POINT -> {

                    }

                    ShapeType.POLYLINE -> {
                        center = GeoPoint(binding.map.getMapCenter())
                        binding.map.overlayManager.remove(dashedPolyline)
                        binding.map.overlayManager.remove(distanceMarker)
                        dashedPolyline = Polyline().apply {

                            addPoint(center)
                            if (polyline.actualPoints.isNotEmpty()) {
                                addPoint(polyline.actualPoints.last())
                            }
                        }


                        val circlePathEffect = PathDashPathEffect(Path().apply {
                            addCircle(0f, 0f, 2f, Path.Direction.CW)
                        }, 10f, 0f, PathDashPathEffect.Style.TRANSLATE)
                        dashedPolyline.outlinePaint.setPathEffect(
                            /*DashPathEffect(
                                floatArrayOf(
                                    20f,
                                    20f
                                ), 0f
                            )*/
                            circlePathEffect
                        )

                        binding.map.overlayManager.add(dashedPolyline)
                        binding.map.overlayManager.add(
                            configureDistanceMarker(
                                marker = distanceMarker,
                                dashedPolyline
                            )
                        )

                        Log.d("ganga", "${dashedPolyline.distance} ${dashedPolyline.distance}")
                    }

                    ShapeType.POLYGON -> {
                        center = GeoPoint(binding.map.getMapCenter())
                        binding.map.overlayManager.remove(dashedPolyline)
                        dashedPolyline = Polyline().apply {

                            if (polygon.actualPoints.isNotEmpty()) {
                                addPoint(polygon.actualPoints.first())
                                addPoint(center)
                                addPoint(polygon.actualPoints.last())
                            }
                        }
                        dashedPolyline.outlinePaint.setPathEffect(
                            DashPathEffect(
                                floatArrayOf(
                                    20f,
                                    20f
                                ), 0f
                            )
                        )
                        binding.map.overlayManager.add(dashedPolyline)
                        Log.d("ganga", "${dashedPolyline.distance} ${polygon.actualPoints}")
                    }

                    ShapeType.SHAPE -> {

                    }
                }


                return true
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                return true
            }

        })
        binding.myLocation.setOnClickListener {
            if (hasPermissions(context = this, permissions = locationPermissions)) {
                updateLocation()
            } else {
                requestLocationPermission()
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

    }

    private fun configureDistanceMarker(marker: Marker, polyline: Polyline): Marker {
        marker.setIcon(null)
        marker.setTextIcon(
            polyline.distance.toString() + "\n ${
                angleFromCoordinate(
                    lat1 = polyline.actualPoints.first().latitude,
                    lat2 = polyline.actualPoints.last().latitude,
                    long1 = polyline.actualPoints.first().longitude,
                    long2 = polyline.actualPoints.last().longitude
                ).toFloat()
            }"
        )
        marker.rotation =  angleFromCoordinate(
            lat1 = polyline.actualPoints.first().latitude,
            lat2 = polyline.actualPoints.last().latitude,
            long1 = polyline.actualPoints.first().longitude,
            long2 = polyline.actualPoints.last().longitude
        ).toFloat() - 90f
        marker.textLabelBackgroundColor = ContextCompat.getColor(this, R.color.teal_200)
        marker.position = polyline.bounds.centerWithDateLine
        return marker
    }

    private fun angleFromCoordinate(
        lat1: Double, long1: Double, lat2: Double,
        long2: Double
    ): Double {
//        val dLon = (long2 - long1)
//
//        val y = sin(dLon) * cos(lat2)
//        val x = cos(lat1) * sin(lat2) - (sin(lat1) * cos(lat2) * cos(dLon))

        var brng = atan2(long2 - long1, lat2 - lat1)

        brng = Math.toDegrees(brng)
        brng = (brng + 360) % 360
        brng = 360 - brng

        return brng
    }

    private fun updateLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationProviderClient.getCurrentLocation(
            PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener {
            if (it != null) {
                binding.map.controller.setCenter(GeoPoint(it.latitude, it.longitude))
                addMyLocationOverlay()
            }
        }.addOnFailureListener {
            Log.d("ganga", "location fetch failed")
        }
    }

    private fun addMyLocationOverlay() {
        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), binding.map)
        locationOverlay.enableMyLocation()
        binding.map.overlays.add(locationOverlay)
    }

    private fun addShape() {
        val folder: List<Overlay> =
            ShapeConverter.convert(binding.map, getAssetFileUri(this, assets.list("")?.first()))
        binding.map.apply {
            overlayManager.addAll(folder)
            invalidate()
            val geopointOverlay = folder.first()
            controller.apply {
                setZoomLevel(20.0)
                setCenter(
                    GeoPoint(
                        geopointOverlay.bounds.centerLatitude,
                        geopointOverlay.bounds.centerLongitude
                    )
                )
            }
        }
    }

    fun getAssetFileUri(context: Context, assetFileName: String?): File? {
        val cacheFile = File(context.cacheDir, assetFileName)
        try {
            context.assets.open(assetFileName!!).use { inputStream ->
                FileOutputStream(cacheFile).use { outputStream ->
                    val buffer = ByteArray(1024)
                    var length: Int
                    while ((inputStream.read(buffer).also { length = it }) > 0) {
                        outputStream.write(buffer, 0, length)
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

        return cacheFile // Returns the Uri of the copied file
    }

    private fun addPolyLine() {
        val startPoint =
            GeoPoint(binding.map.getMapCenter().latitude, binding.map.getMapCenter().longitude)
        polyline.addPoint(startPoint)
        binding.map.invalidate()


    }

    val markerListener = object : Marker.OnMarkerDragListener {
        override fun onMarkerDrag(marker: Marker?) {
            Log.d("ganga", "onMarkerDrag ${marker?.id}")
//        marker?.position = marker?.position
        }

        override fun onMarkerDragEnd(marker: Marker?) {
//        Log.d("ganga", "onMarkerDragEnd")
        }

        override fun onMarkerDragStart(marker: Marker?) {
//        Log.d("ganga", "onMarkerDragStart")
        }

    }

    private fun addPolygon() {
        val startPoint =
            GeoPoint(binding.map.getMapCenter().latitude, binding.map.getMapCenter().longitude)
        polygon.addPoint(startPoint)
        binding.map.invalidate()
    }

    private fun addMarkers() {
        val startPoint =
            GeoPoint(binding.map.getMapCenter().latitude, binding.map.getMapCenter().longitude)
        val startMarker = Marker(binding.map)
        startMarker.position = startPoint
        startMarker.isDraggable = true
        startMarker.id = Random.nextInt().toString()
        startMarker.setOnMarkerDragListener(markerListener)
        startMarker.setIcon(
            ContextCompat.getDrawable(
                this,
                R.drawable.marker
            )
        )
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)

        startMarker.setTitle("Start point")

        binding.map.getOverlays().add(startMarker)

        binding.map.controller.setCenter(startPoint)
        binding.map.invalidate()
    }


    private fun configureMap() {
        binding.map.apply {
            post {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                setScrollableAreaLimitDouble(BoundingBox(85.0, 180.0, -85.0, -180.0))
                setMinZoomLevel(4.0)
                setHorizontalMapRepetitionEnabled(false)
                setVerticalMapRepetitionEnabled(false)
                setScrollableAreaLimitLatitude(
                    getTileSystem().getMaxLatitude(),
                    getTileSystem().getMinLatitude(), 0
                )
                getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                controller.apply {
                    setZoomLevel(8.0)
                }

            }
        }
    }

    private fun showDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Location access is needed for the app to function properly. Please grant this permission in settings.")
            .setPositiveButton("Go to Settings") { dialog, which ->
                openSettings()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openSettings() {
        val intent: Intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.setData(uri)
        startActivity(intent)
    }

    private fun hasPermissions(context: Context, vararg permissions: String): Boolean =
        permissions.all {
            ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun shouldShowPermissionRationale(
        activity: Activity,
        vararg permissions: String
    ): Boolean {
        return permissions.all {
            ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }
    }

    private fun requestLocationPermission() {

        when {
            hasPermissions(context = this, permissions = locationPermissions) -> {
                Log.d("ganga", "has permission")

            }

            shouldShowPermissionRationale(activity = this, permissions = locationPermissions) -> {
                Log.d("ganga", "show rationale")

                showDialog()
            }

            else -> {

                locationPermissionLauncher.launch(
                    locationPermissions
                )
            }   
        }

    }



    private fun configureCacheTilePath() {
        val osmConfig = Configuration.getInstance()
        osmConfig.userAgentValue = packageName
        val basePath = File(cacheDir.absolutePath, "osmdroid")
        osmConfig.osmdroidBasePath = basePath
        val tileCache = File(osmConfig.osmdroidBasePath, "tile")
        osmConfig.osmdroidTileCache = tileCache
    }

    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
        TODO("Not yet implemented")

    }

    override fun longPressHelper(p: GeoPoint?): Boolean {
        TODO("Not yet implemented")
    }
}