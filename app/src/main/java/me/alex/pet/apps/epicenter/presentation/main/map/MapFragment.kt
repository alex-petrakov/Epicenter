package me.alex.pet.apps.epicenter.presentation.main.map


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.transition.TransitionInflater
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.maps.android.clustering.ClusterManager
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.android.synthetic.main.sheet_map.*
import me.alex.pet.apps.epicenter.R
import me.alex.pet.apps.epicenter.common.extensions.onStopTrackingTouch
import me.alex.pet.apps.epicenter.common.extensions.setRestrictiveCheckListener
import me.alex.pet.apps.epicenter.domain.model.Coordinates
import me.alex.pet.apps.epicenter.domain.model.filters.MagnitudeLevel
import me.alex.pet.apps.epicenter.presentation.common.EventMarker
import me.alex.pet.apps.epicenter.presentation.details.DetailsActivity
import me.alex.pet.apps.epicenter.presentation.main.ToolbarProvider
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf

class MapFragment : BaseMapFragment(), OnMapReadyCallback, MapContract.View {

    val presenter: MapPresenter by inject { parametersOf(this) }

    private lateinit var map: GoogleMap

    private lateinit var clusterManager: ClusterManager<EventMarker>

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomSheetBehavior = BottomSheetBehavior.from(filtersSheet)
        bottomSheetBehavior.skipCollapsed = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        magnitudeChipGroup.setRestrictiveCheckListener { group, checkedId ->
            val minMag = when (checkedId) {
                R.id.magnitudeZeroChip -> MagnitudeLevel.ZERO_OR_LESS
                R.id.magnitudeTwoChip -> MagnitudeLevel.TWO
                R.id.magnitudeFourChip -> MagnitudeLevel.FOUR
                R.id.magnitudeSixChip -> MagnitudeLevel.SIX
                R.id.magnitudeEightChip -> MagnitudeLevel.EIGHT
                else -> MagnitudeLevel.ZERO_OR_LESS
            }
            presenter.setMinMagnitude(minMag)
        }

        numOfDaysSeekBar.onStopTrackingTouch { seekBar ->
            presenter.setNumberOfDaysToShow(seekBar.progress + 1)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_filter -> {
                bottomSheetBehavior.state = when (bottomSheetBehavior.state) {
                    BottomSheetBehavior.STATE_EXPANDED -> BottomSheetBehavior.STATE_HIDDEN
                    else -> BottomSheetBehavior.STATE_EXPANDED
                }
                true
            }
            R.id.action_refresh -> {
                presenter.reloadEvents()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPause() {
        val cameraPosition = map.cameraPosition
        val cameraTarget = cameraPosition.target
        val coordinates = Coordinates(cameraTarget.latitude, cameraTarget.longitude)
        presenter.saveCameraPosition(coordinates, cameraPosition.zoom)
        super.onPause()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        clusterManager = ClusterManager<EventMarker>(requireContext(), googleMap).apply {
            renderer = EventsRenderer(requireContext(), googleMap, this)
            setOnClusterItemInfoWindowClickListener { onMarkerInfoWindowClick(it) }
            setOnClusterClickListener { cluster ->
                val position = cluster.position
                presenter.onZoomIn(position.latitude, position.longitude)
                true
            }
        }

        this.map = googleMap.apply {
            uiSettings.isMapToolbarEnabled = false
            setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            activity, R.raw.map_style
                    )
            )
            setOnCameraIdleListener(clusterManager)
            setOnMarkerClickListener(clusterManager)
            setOnInfoWindowClickListener(clusterManager)
        }

        presenter.start()
    }

    override fun setCameraPosition(coordinates: Coordinates, zoom: Float) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(coordinates.latitude, coordinates.longitude), zoom))
    }

    override fun attachPresenter(presenter: MapContract.Presenter) {
        // Intentionally do nothing
    }

    override fun isActive() = isAdded

    override fun showTitle() {
        (requireActivity() as ToolbarProvider).setTitleText(getString(R.string.app_map))
    }

    override fun showProgress(show: Boolean) {
        if (show) progressBar.show() else progressBar.hide()
    }

    override fun showFilters() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun showCurrentMagnitudeFilter(magnitudeLevel: MagnitudeLevel) {
        val id = when (magnitudeLevel) {
            MagnitudeLevel.ZERO_OR_LESS -> R.id.magnitudeZeroChip
            MagnitudeLevel.TWO -> R.id.magnitudeTwoChip
            MagnitudeLevel.FOUR -> R.id.magnitudeFourChip
            MagnitudeLevel.SIX -> R.id.magnitudeSixChip
            MagnitudeLevel.EIGHT -> R.id.magnitudeEightChip
            else -> R.id.magnitudeZeroChip
        }
        magnitudeChipGroup.check(id)
    }

    override fun showCurrentDaysFilter(days: Int) {
        numOfDaysSeekBar.progress = days - 1
    }

    override fun showEventMarkers(markers: List<EventMarker>) {
        clusterManager.clearItems()
        clusterManager.addItems(markers)
        clusterManager.cluster()
    }

    override fun showEventDetails(eventId: String) {
        DetailsActivity.start(requireActivity(), eventId)
    }

    override fun zoomIn(latitude: Double, longitude: Double) {
        val position = LatLng(latitude, longitude)
        val zoom = map.cameraPosition.zoom + 2
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, zoom))
    }

    private fun onMarkerInfoWindowClick(eventMarker: EventMarker) {
        presenter.openEventDetails(eventMarker.eventId)
    }


    companion object {

        fun newInstance(context: Context): MapFragment {
            val transitionInflater = TransitionInflater.from(context)
            val enterAnim = transitionInflater.inflateTransition(R.transition.transition_main_enter)
            val exitAnim = transitionInflater.inflateTransition(R.transition.transition_main_exit)
            return MapFragment().apply {
                enterTransition = enterAnim
                exitTransition = exitAnim
            }
        }
    }
}