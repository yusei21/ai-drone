package com.sloth.registerapp.features.mission.data.preview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapSnapshotOptions
import com.mapbox.maps.Size
import com.mapbox.maps.SnapshotOverlayOptions
import com.mapbox.maps.Snapshotter
import com.mapbox.maps.Style
import com.sloth.registerapp.R
import com.sloth.registerapp.BuildConfig
import com.sloth.registerapp.features.mission.data.mapper.inDisplayOrder
import com.sloth.registerapp.features.mission.data.remote.dto.ServerMissionDto
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max

object MissionPreviewStorage {
    private const val PREVIEW_DIR = "mission-previews"
    private const val WIDTH = 420
    private const val HEIGHT = 240
    private const val SNAPSHOT_TIMEOUT_MS = 4_000L

    suspend fun savePreview(context: Context, mission: ServerMissionDto): File? {
        return runCatching {
            val bitmap = createPreviewBitmap(context, mission)
            val file = fileFor(context, mission.id) ?: return null
            try {
                file.outputStream().use { output ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                }
            } finally {
                bitmap.recycle()
            }
            file
        }.getOrNull()
    }

    fun loadPreview(context: Context, missionId: Int): Bitmap? {
        val file = fileFor(context, missionId) ?: return null
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    fun renamePreview(context: Context, fromMissionId: Int, toMissionId: Int) {
        if (fromMissionId == toMissionId) return
        val source = fileFor(context, fromMissionId) ?: return
        if (!source.exists()) return
        val target = fileFor(context, toMissionId) ?: return
        source.copyTo(target, overwrite = true)
        source.delete()
    }

    fun deletePreview(context: Context, missionId: Int) {
        fileFor(context, missionId)?.takeIf(File::exists)?.delete()
    }

    private fun fileFor(context: Context, missionId: Int): File? {
        val baseDirectory = context.getExternalFilesDir(null) ?: context.filesDir
        val directory = File(baseDirectory, PREVIEW_DIR)
        if (!directory.exists() && !directory.mkdirs()) {
            return null
        }
        return File(directory, "mission_$missionId.png")
    }

    private suspend fun createPreviewBitmap(context: Context, mission: ServerMissionDto): Bitmap {
        if (BuildConfig.MAPBOX_ACCESS_TOKEN.isBlank()) {
            return createFallbackPreviewBitmap(context, mission)
        }

        return withContext(Dispatchers.Main.immediate) {
            withTimeoutOrNull(SNAPSHOT_TIMEOUT_MS) {
                runCatching {
                    createMapSnapshotBitmap(context, mission)
                }.getOrNull()
            } ?: createFallbackPreviewBitmap(context, mission)
        }
    }

    private suspend fun createMapSnapshotBitmap(context: Context, mission: ServerMissionDto): Bitmap {
        return runCatching {
            createMapSnapshotBitmapInternal(context, mission)
        }.getOrElse {
            createFallbackPreviewBitmap(context, mission)
        }
    }

    private suspend fun createMapSnapshotBitmapInternal(context: Context, mission: ServerMissionDto): Bitmap {
        MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN

        val points = mission.snapshotPoints()
        val snapshotter = Snapshotter(
            context,
            MapSnapshotOptions.Builder()
                .size(Size(WIDTH.toFloat(), HEIGHT.toFloat()))
                .pixelRatio(context.resources.displayMetrics.density)
                .build(),
            SnapshotOverlayOptions(
                showLogo = false,
                showAttributes = false
            )
        )

        snapshotter.setStyleUri(Style.SATELLITE_STREETS)
        snapshotter.setCamera(
            snapshotter.cameraForCoordinates(
                points,
                EdgeInsets(40.0, 40.0, 40.0, 40.0),
                null,
                null
            )
        )

        return suspendCoroutine { continuation ->
            snapshotter.start(
                overlayCallback = { overlay ->
                    drawMissionOverlay(overlay.canvas, overlay, mission)
                },
                resultCallback = { snapshot, errorMessage ->
                    val result = snapshot ?: createFallbackPreviewBitmap(context, mission)
                    snapshotter.destroy()
                    continuation.resume(result)
                }
            )
        }
    }

    private fun drawMissionOverlay(
        canvas: Canvas,
        overlay: com.mapbox.maps.SnapshotOverlay,
        mission: ServerMissionDto
    ) {
        val orderedWaypoints = mission.waypoints.inDisplayOrder()
        val routePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CC00C2FF")
            style = Paint.Style.STROKE
            strokeWidth = 4f
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        val poiOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val poiInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E5A0")
            style = Paint.Style.FILL
        }
        val waypointOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF0B1526")
            style = Paint.Style.FILL
        }
        val waypointInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF00C2FF")
            style = Paint.Style.FILL
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val textHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B3000000")
            textSize = 18f
            textAlign = Paint.Align.CENTER
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isFakeBoldText = true
        }

        val waypointPoints = orderedWaypoints.map {
            Point.fromLngLat(it.longitude, it.latitude)
        }

        if (waypointPoints.size > 1) {
            val path = Path()
            waypointPoints.forEachIndexed { index, point ->
                val screen = overlay.screenCoordinate(point)
                val x = screen.x.toFloat()
                val y = screen.y.toFloat()
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            canvas.drawPath(path, routePaint)
        }

        waypointPoints.forEachIndexed { index, point ->
            val screen = overlay.screenCoordinate(point)
            val x = screen.x.toFloat()
            val y = screen.y.toFloat()
            canvas.drawCircle(x, y, 10f, waypointOuterPaint)
            canvas.drawCircle(x, y, 6.5f, waypointInnerPaint)
            val labelY = y + 5f
            canvas.drawText((index + 1).toString(), x, labelY, textHaloPaint)
            canvas.drawText((index + 1).toString(), x, labelY, textPaint)
        }

        val poiPoint = Point.fromLngLat(mission.poi_longitude, mission.poi_latitude)
        val poiScreen = overlay.screenCoordinate(poiPoint)
        val poiX = poiScreen.x.toFloat()
        val poiY = poiScreen.y.toFloat()
        canvas.drawCircle(poiX, poiY, 8.5f, poiOuterPaint)
        canvas.drawCircle(poiX, poiY, 5.5f, poiInnerPaint)

        drawCompactAttribution(canvas, overlay.attributions)
    }

    private fun drawCompactAttribution(canvas: Canvas, attributions: List<String>) {
        val labels = attributions.mapNotNull { attribution ->
            val lower = attribution.lowercase()
            when {
                "mapbox" in lower -> "Mapbox"
                "openstreetmap" in lower || "osm" in lower -> "OpenStreetMap"
                "maxar" in lower -> "Maxar"
                else -> null
            }
        }.distinct()

        val creditText = if (labels.isNotEmpty()) {
            labels.joinToString(" | ")
        } else {
            "Mapbox | OpenStreetMap | Maxar"
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9f
        }
        val paddingHorizontal = 8f
        val paddingVertical = 5f
        val textWidth = textPaint.measureText(creditText)
        val boxHeight = textPaint.textSize + paddingVertical * 2
        val left = WIDTH - textWidth - paddingHorizontal * 2 - 10f
        val top = HEIGHT - boxHeight - 8f

        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#80000000")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            RectF(left, top, WIDTH - 10f, HEIGHT - 8f),
            8f,
            8f,
            backgroundPaint
        )
        canvas.drawText(
            creditText,
            left + paddingHorizontal,
            HEIGHT - 8f - paddingVertical,
            textPaint
        )
    }

    private fun createFallbackPreviewBitmap(context: Context, mission: ServerMissionDto): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val background = Paint().apply {
            shader = LinearGradient(
                0f,
                0f,
                WIDTH.toFloat(),
                HEIGHT.toFloat(),
                intArrayOf(
                    Color.parseColor("#061018"),
                    Color.parseColor("#0A1628"),
                    Color.parseColor("#071120")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), background)

        val gridPaint = Paint().apply {
            color = Color.parseColor("#1400C2FF")
            strokeWidth = 1f
        }
        val step = 32f
        var x = 0f
        while (x <= WIDTH) {
            canvas.drawLine(x, 0f, x, HEIGHT.toFloat(), gridPaint)
            x += step
        }
        var y = 0f
        while (y <= HEIGHT) {
            canvas.drawLine(0f, y, WIDTH.toFloat(), y, gridPaint)
            y += step
        }

        val points = mission.snapshotPoints().map { point ->
            point.longitude() to point.latitude()
        }

        val minX = points.minOf { it.first }
        val maxX = points.maxOf { it.first }
        val minY = points.minOf { it.second }
        val maxY = points.maxOf { it.second }
        val spanX = max(0.0001, maxX - minX)
        val spanY = max(0.0001, maxY - minY)
        val padding = 26f

        val normalized = points.map { (lng, lat) ->
            val px = ((lng - minX) / spanX).toFloat() * (WIDTH - padding * 2) + padding
            val py = ((maxY - lat) / spanY).toFloat() * (HEIGHT - padding * 2) + padding
            px to py
        }

        if (normalized.size > 1) {
            val routePaint = Paint().apply {
                color = Color.parseColor("#8800C2FF")
                style = Paint.Style.STROKE
                strokeWidth = 4f
                isAntiAlias = true
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
            }
            val path = Path().apply {
                moveTo(normalized.first().first, normalized.first().second)
                normalized.drop(1).forEach { (px, py) -> lineTo(px, py) }
            }
            canvas.drawPath(path, routePaint)
        }

        val poiPaint = Paint().apply {
            color = Color.parseColor("#00E5A0")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val dotPaint = Paint().apply {
            color = Color.parseColor("#00C2FF")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val ringPaint = Paint().apply {
            color = Color.parseColor("#3300C2FF")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        normalized.forEachIndexed { index, (px, py) ->
            canvas.drawCircle(px, py, if (index == 0) 10f else 7f, ringPaint)
            canvas.drawCircle(px, py, if (index == 0) 5f else 3.5f, dotPaint)
        }

        val poiX = ((mission.poi_longitude - minX) / spanX).toFloat() * (WIDTH - padding * 2) + padding
        val poiY = ((maxY - mission.poi_latitude) / spanY).toFloat() * (HEIGHT - padding * 2) + padding
        canvas.drawCircle(poiX, poiY, 4.5f, poiPaint)

        val tagPaint = Paint().apply {
            color = Color.parseColor("#80000000")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(14f, HEIGHT - 34f, 132f, HEIGHT - 12f), 10f, 10f, tagPaint)
        val textPaint = Paint().apply {
            color = Color.parseColor("#00C2FF")
            textSize = 18f
            isAntiAlias = true
        }
        canvas.drawText(
            context.getString(R.string.mission_preview_route_label),
            24f,
            HEIGHT - 18f,
            textPaint
        )

        return bitmap
    }

    private fun ServerMissionDto.snapshotPoints(): List<Point> {
        val routePoints = waypoints.inDisplayOrder().map { waypoint ->
            Point.fromLngLat(waypoint.longitude, waypoint.latitude)
        }
        val poiPoint = Point.fromLngLat(poi_longitude, poi_latitude)
        return (routePoints + poiPoint).ifEmpty { listOf(poiPoint) }
    }
}
