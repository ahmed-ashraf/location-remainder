package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private var map: GoogleMap? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var marker: Marker? = null
    private var circleMarker: Circle? = null
    private var poi: PointOfInterest? = null
    val uiHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1001
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 1002
        private const val DEFAULT_ZOOM = 16
    }

    var retrieveLocationRetry = 0
    private val RETRIE_LOCATION_MAXIMUM = 5
    private val RETRIE_LOCATION_DELAY = 1000L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        binding.btnSave.setOnClickListener {
            onLocationSelected()
        }
        binding.btnSave.visibility = View.GONE

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        // Build the map.
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return binding.root
    }


    private fun onLocationSelected() {
        _viewModel.selectedPOI.value = poi
        _viewModel.reminderSelectedLocationStr.value = poi?.name
        _viewModel.latitude.value = poi?.latLng?.latitude
        _viewModel.longitude.value = poi?.latLng?.longitude
        _viewModel.navigationCommand.value = NavigationCommand.Back
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map?.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map?.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map?.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map?.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(p0: GoogleMap?) {
        map = p0

        _viewModel.selectedPOI.value?.let { poi ->
            updatePoi(poi)
        }

        setMapClick(map)
        setMapStyle(map)
        map?.setOnPoiClickListener { poi ->
            binding.btnSave.visibility = View.VISIBLE
            updatePoi(poi)
        }
        requestUserLocationAndMoveCamera()
    }
    private fun setMapClick(map: GoogleMap?) {
        map?.setOnMapClickListener { latLng ->
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresses.size > 0) {
                binding.btnSave.visibility = View.VISIBLE
                val address: String = addresses[0].getAddressLine(0)
                updatePoi(PointOfInterest(latLng, null, address))
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestUserLocationAndMoveCamera() {
        when {
            isFineLocationPermissionGranted() -> {
                map?.isMyLocationEnabled = true
                enableLocationMoveCameraToUserLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Snackbar.make(
                    requireActivity().findViewById(android.R.id.content),
                    "location_permission_required_rationale",
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction(android.R.string.ok) {
                        requestPermissions(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            REQUEST_LOCATION_PERMISSION
                        )
                    }.show()
            }
            else -> {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_LOCATION_PERMISSION
                )
            }
        }
    }

    private fun enableLocationMoveCameraToUserLocation(needResolve: Boolean = true) {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(
            LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_LOW_POWER
            }
        )
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && needResolve) {
                startIntentSenderForResult(exception.resolution.intentSender,
                    REQUEST_TURN_DEVICE_LOCATION_ON, null, 0, 0, 0, null)
            } else {
                Snackbar.make(
                    requireActivity().findViewById(android.R.id.content),
                    "Error, try again", Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    enableLocationMoveCameraToUserLocation()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                fetchLocationAndMoveCamera()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    requestUserLocationAndMoveCamera()
                } else {
                    Snackbar.make(
                        requireActivity().findViewById(android.R.id.content),
                        getString(R.string.permission_denied_explanation),
                        Snackbar.LENGTH_INDEFINITE
                    )
                        .setAction(R.string.settings) {
                            startActivity(Intent().apply {
                                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            })
                        }.show()
                }
            }
        }
    }

    private fun updatePoi(poi: PointOfInterest) {
        marker?.remove()
        circleMarker?.remove()

        this.poi = poi

        // Add Marker
        marker = map?.addMarker(
            MarkerOptions()
                .position(poi.latLng)
                .title(poi.name)
        )
        marker?.showInfoWindow()

        // Add circle range
        circleMarker = map?.addCircle(
            CircleOptions()
                .center(poi.latLng)
                .radius(100.0)
                .fillColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.geofencing_circle_fill_color
                    )
                )
                .strokeColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.geofencing_circle_stroke_color
                    )
                )
        )
    }

    private fun setMapStyle(map: GoogleMap?) {
        try {
            map?.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.google_map_style)
            )

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun fetchLocationAndMoveCamera() {
        try {
            if (isFineLocationPermissionGranted()) {
                fusedLocationProviderClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            map?.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(location.latitude, location.longitude),
                                    DEFAULT_ZOOM.toFloat()
                                )
                            )
                            retrieveLocationRetry = 0
                        } else {
                            if (retrieveLocationRetry < RETRIE_LOCATION_MAXIMUM) {
                                retrieveLocationRetry += 1
                                uiHandler.postDelayed({
                                    fetchLocationAndMoveCamera()
                                }, RETRIE_LOCATION_DELAY)
                            }
                        }
                    }
            }
        } catch (_: SecurityException) {}
    }

    private fun isFineLocationPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
}
