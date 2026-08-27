package fjallkartan.fjallkartan.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import fjallkartan.fjallkartan.measurement.DistanceMeasurement
import fjallkartan.fjallkartan.measurement.GeoCoordinate
import fjallkartan.fjallkartan.measurement.LineSimplifier
import fjallkartan.fjallkartan.measurement.ScreenPoint
import kotlin.math.hypot
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

@SuppressLint("ViewConstructor")
internal class MapContainerView(
    context: Context,
    val mapView: MapView,
    val captureView: MeasureCaptureView,
) : FrameLayout(context) {
    init {
        addView(mapView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(captureView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun bindMap(map: MapLibreMap) {
        captureView.map = map
        captureView.mapView = mapView
    }
}

internal class MeasureCaptureView(context: Context) : View(context) {
    var map: MapLibreMap? = null
    var mapView: MapView? = null
    var anchor: GeoCoordinate? = null
        set(value) {
            field = value
            anchorLatLng = value?.let { LatLng(it.latitude, it.longitude) }
        }
    var onStrokeFinished: (List<GeoCoordinate>) -> Unit = {}
    var onPreviewChanged: (Double?) -> Unit = {}

    private val density = resources.displayMetrics.density
    private val points = mutableListOf<ScreenPoint>()
    private var manipulatingMap = false
    private var anchorLatLng: LatLng? = null

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
    private val drawPath = Path()

    init {
        visibility = GONE
        isClickable = true
    }

    fun setMeasuring(enabled: Boolean) {
        visibility = if (enabled) VISIBLE else GONE
        if (!enabled) clearStroke()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (map == null) return true
        // Delegate any multi-touch handling to MapLibre's own gesture detector so pan,
        // pinch-zoom and rotation behave exactly as they do outside of measuring mode.
        // A hand-rolled reimplementation based on raw per-frame midpoint/distance deltas
        // was jittery and let pinch gestures visibly drag the map.
        mapView?.onTouchEvent(event)
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
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2) {
                    if (!manipulatingMap) {
                        manipulatingMap = true
                        points.clear()
                        onPreviewChanged(null)
                        invalidate()
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
        val path = drawPath.apply { rewind() }
        val start = anchorLatLng?.let { map?.projection?.toScreenLocation(it) }
        if (start != null) {
            path.moveTo(start.x, start.y)
            path.lineTo(points.first().x.toFloat(), points.first().y.toFloat())
        } else {
            path.moveTo(points.first().x.toFloat(), points.first().y.toFloat())
        }
        for (i in 1 until points.size) {
            path.lineTo(points[i].x.toFloat(), points[i].y.toFloat())
        }
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
}
