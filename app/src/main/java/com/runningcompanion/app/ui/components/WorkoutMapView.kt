package com.runningcompanion.app.ui.components

import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.runningcompanion.app.domain.model.GpsPoint
import com.runningcompanion.app.ui.theme.AppColors
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun WorkoutMapView(
    track: List<GpsPoint>,
    modifier: Modifier = Modifier,
    routeColorHex: String = "#FC4C02" // Strava Orange
) {
    if (track.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(AppColors.Paper),
            contentAlignment = Alignment.Center
        ) {
            Text("No GPS route recorded for this session", color = AppColors.InkMuted)
        }
        return
    }

    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        AndroidView(
            factory = {
                mapView.apply {
                    overlays.clear()

                    val geoPoints = track.map { GeoPoint(it.latitude, it.longitude) }

                    if (geoPoints.size >= 2) {
                        // 1. Draw route polyline
                        val polyline = Polyline().apply {
                            setPoints(geoPoints)
                            outlinePaint.color = android.graphics.Color.parseColor(routeColorHex)
                            outlinePaint.strokeWidth = 10f
                            outlinePaint.strokeCap = Paint.Cap.ROUND
                            outlinePaint.strokeJoin = Paint.Join.ROUND
                        }
                        overlays.add(polyline)

                        // 2. Start Marker (Green)
                        val startMarker = Marker(this).apply {
                            position = geoPoints.first()
                            title = "Start"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        }
                        overlays.add(startMarker)

                        // 3. Finish Marker (Red)
                        val finishMarker = Marker(this).apply {
                            position = geoPoints.last()
                            title = "Finish"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        }
                        overlays.add(finishMarker)

                        // 4. Zoom to fit the entire route with padding
                        val minLat = geoPoints.minOf { it.latitude }
                        val maxLat = geoPoints.maxOf { it.latitude }
                        val minLon = geoPoints.minOf { it.longitude }
                        val maxLon = geoPoints.maxOf { it.longitude }

                        // Expand tiny bounds if running on the spot
                        val latDiff = (maxLat - minLat).coerceAtLeast(0.001)
                        val lonDiff = (maxLon - minLon).coerceAtLeast(0.001)

                        val boundingBox = BoundingBox(
                            maxLat + latDiff * 0.15,
                            maxLon + lonDiff * 0.15,
                            minLat - latDiff * 0.15,
                            minLon - lonDiff * 0.15
                        )

                        post {
                            try {
                                zoomToBoundingBox(boundingBox, false, 80)
                            } catch (_: Exception) {
                                controller.setCenter(geoPoints.first())
                                controller.setZoom(16.0)
                            }
                        }
                    } else if (geoPoints.isNotEmpty()) {
                        controller.setCenter(geoPoints.first())
                        controller.setZoom(16.0)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
