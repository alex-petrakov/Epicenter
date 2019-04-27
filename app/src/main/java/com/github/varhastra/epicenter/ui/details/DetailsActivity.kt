package com.github.varhastra.epicenter.ui.details

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.widget.Toolbar
import butterknife.BindColor
import butterknife.BindView
import butterknife.ButterKnife
import com.github.varhastra.epicenter.R
import com.github.varhastra.epicenter.data.EventsRepository
import com.github.varhastra.epicenter.data.Prefs
import com.github.varhastra.epicenter.device.LocationProvider
import com.github.varhastra.epicenter.domain.interactors.EventLoaderInteractor
import com.github.varhastra.epicenter.domain.model.Coordinates
import com.github.varhastra.epicenter.utils.UnitsLocale
import com.github.varhastra.epicenter.utils.kmToMi
import com.github.varhastra.epicenter.views.TileTwolineView
import com.github.varhastra.epicenter.views.TileView
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import java.text.DecimalFormat
import kotlin.math.roundToInt

class DetailsActivity : AppCompatActivity(), DetailsContract.View {

    @JvmField
    @BindColor(R.color.colorAlert0)
    @ColorInt
    var colorAlert0: Int = 0

    @JvmField
    @BindColor(R.color.colorAlert2)
    @ColorInt
    var colorAlert2: Int = 0

    @JvmField
    @BindColor(R.color.colorAlert4)
    @ColorInt
    var colorAlert4: Int = 0

    @JvmField
    @BindColor(R.color.colorAlert6)
    @ColorInt
    var colorAlert6: Int = 0

    @JvmField
    @BindColor(R.color.colorAlert8)
    @ColorInt
    var colorAlert8: Int = 0

    @BindView(R.id.tb_details)
    lateinit var toolbar: Toolbar

    @BindView(R.id.tile_details_datetime)
    lateinit var dateTile: TileTwolineView

    @BindView(R.id.tile_depth_datetime)
    lateinit var depthTile: TileView

    @BindView(R.id.tile_details_dyfi)
    lateinit var dyfiTile: TileView

    @BindView(R.id.tile_details_source_link)
    lateinit var sourceTile: TileView

    @BindView(R.id.tv_details_header_magnitude)
    lateinit var magnitudeTextView: TextView

    @BindView(R.id.tv_details_header_magnitude_type)
    lateinit var magnitudeTypeTextView: TextView

    @BindView(R.id.tv_details_place_name)
    lateinit var placeNameTextView: TextView

    @BindView(R.id.tv_details_distance)
    lateinit var distanceTextView: TextView

    @BindView(R.id.tv_details_coordinates)
    lateinit var coordinatesTextView: TextView

    @BindView(R.id.tv_details_tsunami_alert)
    lateinit var tsunamiAlertTextView: TextView

    private lateinit var presenter: DetailsContract.Presenter

    private var alertAccentColor: Int = 0

    private val magFormatter: DecimalFormat = DecimalFormat("0.0")
    private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)
        ButterKnife.bind(this)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sourceTile.setOnClickListener { presenter.openSourceLink() }


        val presenter = DetailsPresenter(this, EventLoaderInteractor(EventsRepository.getInstance(), LocationProvider()))
        intent?.apply {
            val eventId = getStringExtra(EXTRA_EVENT_ID)
            presenter.init(eventId)
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        presenter.start()
    }

    override fun attachPresenter(presenter: DetailsContract.Presenter) {
        this.presenter = presenter
    }

    override fun isActive() = !(isFinishing || isDestroyed)

    override fun setAlertColor(alertType: DetailsContract.View.AlertType) {
        alertAccentColor = getAlertColor(alertType)
    }

    override fun showEventMagnitude(magnitude: Double, type: String) {
        magnitudeTextView.text = magFormatter.format(magnitude)
        magnitudeTypeTextView.text = type

        magnitudeTextView.setTextColor(alertAccentColor)
        magnitudeTypeTextView.setTextColor(alertAccentColor)
    }

    override fun showEventPlace(place: String) {
        placeNameTextView.text = place
    }

    override fun showEventDistance(distance: Double?) {
        distanceTextView.text = getLocalizedDistanceStr(distance)
    }

    override fun showEventCoordinates(coordinates: Coordinates) {
        coordinatesTextView.text = getString(R.string.details_event_coordinates, coordinates.latitude, coordinates.longitude)
        coordinatesTextView.setTextColor(alertAccentColor)
    }

    override fun showTsunamiAlert(show: Boolean) {
        tsunamiAlertTextView.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun showEventDate(localDateTime: LocalDateTime, daysAgo: Int) {
        dateTile.setFirstLineText(dateTimeFormatter.format(localDateTime))
        dateTile.setSecondLineText(resources.getQuantityString(R.plurals.plurals_details_days_ago, daysAgo, daysAgo))
    }

    override fun showEventDepth(depth: Double) {
        depthTile.setText(getLocalizedDepthStr(depth))
    }

    override fun showEventReports(reportsCount: Int) {
        dyfiTile.setText(reportsCount.toString())
    }

    override fun showEventLink(linkUrl: String) {
        sourceTile.setText(linkUrl)
    }

    override fun showErrorNoData() {
//        TODO("stub, not implemented")
    }

    override fun showSourceLinkViewer(link: String) {
        val uri = Uri.parse(link)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    @ColorInt
    private fun getAlertColor(alertType: DetailsContract.View.AlertType): Int {
        return when (alertType) {
            DetailsContract.View.AlertType.ALERT_0 -> colorAlert0
            DetailsContract.View.AlertType.ALERT_2 -> colorAlert2
            DetailsContract.View.AlertType.ALERT_4 -> colorAlert4
            DetailsContract.View.AlertType.ALERT_6 -> colorAlert6
            DetailsContract.View.AlertType.ALERT_8 -> colorAlert8
            else -> colorAlert0
        }
    }

    private fun getLocalizedDistance(distanceInKm: Double?): Double? {
        if (distanceInKm == null) {
            return null
        }

        return when (Prefs.getPreferredUnits()) {
            UnitsLocale.METRIC -> distanceInKm
            UnitsLocale.IMPERIAL -> kmToMi(distanceInKm)
            else -> distanceInKm
        }
    }

    private fun getLocalizedDistanceStr(distanceInKm: Double?): String {
        val distance = getLocalizedDistance(distanceInKm)
        return when (Prefs.getPreferredUnits()) {
            UnitsLocale.METRIC -> getString(R.string.details_event_distance_km, distance?.roundToInt().toString())
            UnitsLocale.IMPERIAL -> getString(R.string.details_event_distance_mi, distance?.roundToInt().toString())
            else -> getString(R.string.details_event_distance_km, distance?.roundToInt().toString())
        }
    }

    private fun getLocalizedDepthStr(depthInKm: Double?): String {
        val depth = getLocalizedDistance(depthInKm)
        return when (Prefs.getPreferredUnits()) {
            UnitsLocale.METRIC -> getString(R.string.details_event_depth_km, depth?.roundToInt().toString())
            UnitsLocale.IMPERIAL -> getString(R.string.details_event_depth_mi, depth?.roundToInt().toString())
            else -> getString(R.string.details_event_depth_km, depth?.roundToInt().toString())
        }
    }


    companion object {
        const val EXTRA_EVENT_ID = "EVENT_ID"
    }
}
