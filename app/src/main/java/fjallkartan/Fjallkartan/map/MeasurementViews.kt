package fjallkartan.fjallkartan.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import fjallkartan.fjallkartan.measurement.DistanceMarker
import fjallkartan.fjallkartan.measurement.DistanceMeasurement
import fjallkartan.fjallkartan.measurement.GeoCoordinate
import fjallkartan.fjallkartan.measurement.LineSimplifier
import fjallkartan.fjallkartan.measurement.ScreenPoint
import fjallkartan.fjallkartan.saved.SavedPin
import fjallkartan.fjallkartan.search.PlaceResult
import kotlin.math.hypot
import kotlin.math.ln
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

@SuppressLint("ViewConstructor")
internal class MapContainerView(
    context: Context,
    val mapView: MapView,
    private val pinOverlay: PinOverlay,
    private val markerOverlay: DistanceMarkerOverlay,
    val captureView: MeasureCaptureView,
) : FrameLayout(context) {
    init {
        addView(mapView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(pinOverlay, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(markerOverlay, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(captureView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun bindMap(map: MapLibreMap) {
        markerOverlay.map = map
        pinOverlay.map = map
        captureView.map = map
    }

    fun updateMarkers(markers: List<DistanceMarker>) {
        markerOverlay.markers = markers
        markerOverlay.invalidate()
    }

    fun invalidateMarkers() {
        markerOverlay.invalidate()
        pinOverlay.invalidate()
    }

    fun updatePins(pins: List<SavedPin>, selectedPlace: PlaceResult?) {
        pinOverlay.pins = pins
        pinOverlay.selectedPlace = selectedPlace
        pinOverlay.invalidate()
    }
}

internal class PinOverlay(context: Context) : View(context) {
    var map: MapLibreMap? = null
    var pins: List<SavedPin> = emptyList()
    var selectedPlace: PlaceResult? = null

    private val density = resources.displayMetrics.density
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 3 * density
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 12 * density * resources.configuration.fontScale
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    override fun onDraw(canvas: Canvas) {
        val projection = map?.projection ?: return
        pins.forEach { pin ->
            val point = projection.toScreenLocation(LatLng(pin.coordinate.latitude, pin.coordinate.longitude))
            drawPin(canvas, point, Color.rgb(242, 140, 40), 10 * density)
        }
        selectedPlace?.let { place ->
            val point = projection.toScreenLocation(
                LatLng(place.coordinate.latitude, place.coordinate.longitude),
            )
            drawPin(canvas, point, Color.rgb(29, 108, 191), 12 * density)
            val width = text.measureText(place.name) + 18 * density
            val top = point.y - 52 * density
            fill.color = Color.WHITE
            canvas.drawRoundRect(
                point.x - width / 2,
                top,
                point.x + width / 2,
                top + 28 * density,
                14 * density,
                14 * density,
                fill,
            )
            canvas.drawText(
                place.name,
                point.x,
                top + 14 * density - (text.descent() + text.ascent()) / 2,
                text,
            )
        }
    }

    private fun drawPin(canvas: Canvas, point: PointF, color: Int, radius: Float) {
        fill.color = color
        canvas.drawCircle(point.x, point.y - radius, radius, fill)
        canvas.drawCircle(point.x, point.y - radius, radius, stroke)
        val path = Path().apply {
            moveTo(point.x - radius * 0.45f, point.y - radius * 0.25f)
            lineTo(point.x, point.y + radius * 0.8f)
            lineTo(point.x + radius * 0.45f, point.y - radius * 0.25f)
            close()
        }
        canvas.drawPath(path, fill)
    }
}

internal class DistanceMarkerOverlay(context: Context) : View(context) {
    var map: MapLibreMap? = null
    var markers: List<DistanceMarker> = emptyList()

    private val density = resources.displayMetrics.density
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(11, 110, 79)
        style = Paint.Style.STROKE
        strokeWidth = 2 * density
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        textSize = 11 * resources.displayMetrics.density * resources.configuration.fontScale
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val projection = map?.projection ?: return
        for (marker in markers) {
            val point = projection.toScreenLocation(
                LatLng(marker.coordinate.latitude, marker.coordinate.longitude),
            )
            val label = DistanceMeasurement.markerLabel(marker.meters)
            val width = textPaint.measureText(label) + 14 * density
            val height = 24 * density
            val left = point.x - width / 2
            val top = point.y - height / 2
            val radius = height / 2
            canvas.drawRoundRect(left, top, left + width, top + height, radius, radius, circlePaint)
            canvas.drawRoundRect(left, top, left + width, top + height, radius, radius, strokePaint)
            val baseline = point.y - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText(label, point.x, baseline, textPaint)
        }
    }
}

internal class MeasureCaptureView(context: Context) : View(context) {
    var map: MapLibreMap? = null
    var anchor: GeoCoordinate? = null
    var onStrokeFinished: (List<GeoCoordinate>) -> Unit = {}
    var onPreviewChanged: (Double?) -> Unit = {}

    private val density = resources.displayMetrics.density
    private val points = mutableListOf<ScreenPoint>()
    private var manipulatingMap = false
    private var previousMidpoint = PointF()
    private var previousDistance = 0f

    private val casingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 7 * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val routePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(11, 110, 79)
        style = Paint.Style.STROKE
        strokeWidth = 4 * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    init {
        visibility = GONE
        isClickable = true
    }

    fun setMeasuring(enabled: Boolean) {
        visibility = if (enabled) VISIBLE else GONE
        if (!enabled) clearStroke()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val readyMap = map ?: return true
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                manipulatingMap = false
                points.clear()
                points += ScreenPoint(event.x.toDouble(), event.y.toDouble())
                updatePreview()
                invalidate()
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                manipulatingMap = true
                points.clear()
                onPreviewChanged(null)
                rememberTwoFingerGesture(event)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2) {
                    if (!manipulatingMap) {
                        manipulatingMap = true
                        points.clear()
                        onPreviewChanged(null)
                        rememberTwoFingerGesture(event)
                    } else {
                        val midpoint = midpoint(event)
                        val distance = pointerDistance(event)
                        readyMap.scrollBy(
                            previousMidpoint.x - midpoint.x,
                            previousMidpoint.y - midpoint.y,
                        )
                        if (previousDistance > 0 && distance > 0) {
                            val zoomDelta = ln((distance / previousDistance).toDouble()) / ln(2.0)
                            readyMap.moveCamera(
                                CameraUpdateFactory.zoomBy(
                                    zoomDelta,
                                    Point(midpoint.x.toInt(), midpoint.y.toInt()),
                                ),
                            )
                        }
                        previousMidpoint = midpoint
                        previousDistance = distance
                    }
                } else if (!manipulatingMap) {
                    val last = points.lastOrNull()
                    if (last == null || hypot(event.x - last.x, event.y - last.y) >= 1.5 * density) {
                        points += ScreenPoint(event.x.toDouble(), event.y.toDouble())
                        updatePreview()
                        invalidate()
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!manipulatingMap) finishStroke()
                performClick()
                clearStroke()
            }
            MotionEvent.ACTION_POINTER_UP -> {
                manipulatingMap = true
            }
            MotionEvent.ACTION_CANCEL -> clearStroke()
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.isEmpty()) return
        val path = Path()
        val start = anchor?.let {
            map?.projection?.toScreenLocation(LatLng(it.latitude, it.longitude))
        }
        if (start != null) {
            path.moveTo(start.x, start.y)
            path.lineTo(points.first().x.toFloat(), points.first().y.toFloat())
        } else {
            path.moveTo(points.first().x.toFloat(), points.first().y.toFloat())
        }
        points.drop(1).forEach { path.lineTo(it.x.toFloat(), it.y.toFloat()) }
        canvas.drawPath(path, casingPaint)
        canvas.drawPath(path, routePaint)
    }

    private fun finishStroke() {
        val readyMap = map ?: return
        val simplified = LineSimplifier.simplify(points, tolerance = 2.5 * density)
        val coordinates = simplified.map { point ->
            readyMap.projection.fromScreenLocation(PointF(point.x.toFloat(), point.y.toFloat())).let {
                GeoCoordinate(it.latitude, it.longitude)
            }
        }
        onStrokeFinished(coordinates)
    }

    private fun updatePreview() {
        val readyMap = map ?: return
        val coordinates = points.map { point ->
            readyMap.projection.fromScreenLocation(PointF(point.x.toFloat(), point.y.toFloat())).let {
                GeoCoordinate(it.latitude, it.longitude)
            }
        }
        val route = anchor?.let { listOf(it) + coordinates } ?: coordinates
        onPreviewChanged(DistanceMeasurement.length(route))
    }

    private fun clearStroke() {
        points.clear()
        manipulatingMap = false
        onPreviewChanged(null)
        invalidate()
    }

    private fun rememberTwoFingerGesture(event: MotionEvent) {
        previousMidpoint = midpoint(event)
        previousDistance = pointerDistance(event)
    }

    private fun midpoint(event: MotionEvent): PointF {
        return PointF(
            (event.getX(0) + event.getX(1)) / 2,
            (event.getY(0) + event.getY(1)) / 2,
        )
    }

    private fun pointerDistance(event: MotionEvent): Float {
        return hypot(event.getX(1) - event.getX(0), event.getY(1) - event.getY(0))
    }
}
