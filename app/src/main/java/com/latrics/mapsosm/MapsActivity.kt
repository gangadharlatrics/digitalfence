package com.latrics.mapsosm

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.latrics.mapsosm.databinding.ActivityMapsBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import java.io.File


class MapsActivity : AppCompatActivity() {

    val locationPermissions = arrayOf(
        ACCESS_COARSE_LOCATION,
        ACCESS_FINE_LOCATION
    )

    private val locationPermissionLauncher by lazy {
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions[ACCESS_FINE_LOCATION] == true && permissions[ACCESS_COARSE_LOCATION] == true -> {
                    Log.d("ganga", "given ${permissions.toString()}")
                }

//                permissions[ACCESS_COARSE_LOCATION] == true -> {
//                    Toast.makeText(this, "Coarse location granted", Toast.LENGTH_SHORT).show()
//                }

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
        binding.map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
//        binding.map.setZoomLevel(3.0)

        binding.add.setOnClickListener {

        }
        binding.myLocation.setOnClickListener {
            if (hasPermissions(context = this, permissions = locationPermissions)) {

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
}