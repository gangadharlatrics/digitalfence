package com.latrics.mapsosm

import org.osmdroid.util.GeoPoint

class MapLayerManager() {


}

//class PolylineManager {
//    fun addPoint(markerData: MarkerData, polyLineData: PolyLineData): PolyLineData {
//        polyLineData.polyline[markerData.id] = markerData
//        return polyLineData
//    }
//
//    fun removePoint(markerData: MarkerData, polyLineData: PolyLineData): PolyLineData {
//        polyLineData.polyline.remove(markerData.id)
//        return polyLineData
//    }
//}
//
//class PolygonManager {
//    fun addPoint(markerData: MarkerData, polygonData: PolygonData): PolygonData {
//        polygonData.polygon[markerData.id] = markerData
//        return polygonData
//    }
//
//    fun removePoint(markerData: MarkerData, polygonData: PolygonData): PolygonData {
//        polygonData.polygon.remove(markerData.id)
//        return polygonData
//    }
//}

data class MarkerData(val id: String, val marker: GeoPoint, val title: String)

data class LineData(
    val id: String,
    val shapeId: String,
    val layerId: String,
    val title: String,
    val line: Pair<MarkerData, MarkerData>
)

data class ShapeData(
    val id: String,
    val title: String,
    val layerId: String,
    val shape: LinkedHashMap<String, MarkerData>, val shapeType: ShapeType
)

data class LayerData(val id: String, val title: String, val shapesList: List<ShapeData>)
enum class ShapeType {
    POINT,
    POLYLINE,
    POLYGON,
    SHAPE
}

//data class Overlays(
//    val markers: Set<ShapeData.MarkerData>,
//    val polylines: LinkedHashMap<String, ShapeData.PolyLineData>,
//    val polygons: LinkedHashMap<String, ShapeData.PolygonData>
//)

