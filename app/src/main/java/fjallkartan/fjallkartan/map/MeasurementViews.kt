package fjallkartan.fjallkartan.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import fjallkartan.fjallkartan.measurement.DistanceMeasurement
import fjallkartan.fjallkartan.measurement.GeoCoordinate
import fjallkartan.fjallkartan.measurement.LineSimplifier
import fjallkartan.fjallkartan.measurement.ScreenPoint
import kotlin.math.hypot
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.widgets.CompassView

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
        color = Color.rgb(255, 149, 0)
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
        if (event.actionMasked == MotionEvent.ACTION_DOWN && touchesCompass(event.x, event.y)) {
            // Let the touch fall through to MapLibre's own compass button instead of
            // starting a stroke, so tapping it still resets bearing while measuring.
            return false
        }
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
                map?.uiSettings?.isScrollGesturesEnabled = false
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                manipulatingMap = true
                points.clear()
                onPreviewChanged(null)
                invalidate()
                map?.uiSettings?.isScrollGesturesEnabled = true
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
                map?.uiSettings?.isScrollGesturesEnabled = false
            }
            MotionEvent.ACTION_POINTER_UP -> {
                manipulatingMap = true
                if (event.pointerCount - 1 <= 1) {
                    map?.uiSettings?.isScrollGesturesEnabled = false
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                clearStroke()
                map?.uiSettings?.isScrollGesturesEnabled = false
            }
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

    private fun touchesCompass(x: Float, y: Float): Boolean {
        val compass = mapView?.let { findCompassView(it) } ?: return false
        if (compass.visibility != VISIBLE || compass.isHidden) return false
        val compassLocation = IntArray(2)
        compass.getLocationOnScreen(compassLocation)
        val selfLocation = IntArray(2)
        getLocationOnScreen(selfLocation)
        // Pad the hit rect slightly beyond the drawable bounds to match the button's
        // usual tap target size.
        val padding = (8 * density).toInt()
        val rect = Rect(
            compassLocation[0] - selfLocation[0] - padding,
            compassLocation[1] - selfLocation[1] - padding,
            compassLocation[0] - selfLocation[0] + compass.width + padding,
            compassLocation[1] - selfLocation[1] + compass.height + padding,
        )
        return rect.contains(x.toInt(), y.toInt())
    }

    private fun findCompassView(view: View): CompassView? {
        if (view is CompassView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findCompassView(view.getChildAt(i))?.let { return it }
            }
        }
        return null
    }
}
