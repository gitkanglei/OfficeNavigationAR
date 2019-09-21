package com.ardog.utils

import android.util.Log
import com.ardog.extensions.getAdjacentPointList
import com.ardog.model.DogPoint
import com.google.ar.core.Pose
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge

class PathFinder {

  lateinit var map: Map<String, DogPoint>
  lateinit var idMaps: Map<Long, DogPoint>
  lateinit var points: List<DogPoint>

  lateinit var finder: DijkstraShortestPath<Long, DefaultEdge>

  fun init(points: List<DogPoint>) {
    map = points.map { it.name to it }
        .toMap()
    this.idMaps = points.map { it.id to it }
        .toMap()
    this.points = points
    buildGraph()
  }

  fun findRoutes(
    start: DogPoint,
    destination: DogPoint
  ): List<DogPoint> {

    if (!idMaps.containsKey(start.id) || !idMaps.containsKey(destination.id)) {
      return ArrayList()
    }

    val startId = start.id
    var destinationId = destination.id
    val result = finder.getPath(startId, destinationId)

    if (result == null || result.vertexList.isEmpty()) {
      return ArrayList()
    }

    return result.vertexList.map { idMaps[it]!! }
        .toList()
  }

  fun findRoutes(
    startPointName: String,
    destinationName: String
  ): List<DogPoint?> {

    if (!map.containsKey(startPointName) || !map.containsKey(destinationName)) {
      return ArrayList()
    }
    return findRoutes(map[startPointName]!!, map[destinationName]!!)
  }

  fun findPoint(name: String): DogPoint? {
   val mapSet= map.entries
    mapSet.forEach {
      if (it.key.isNotEmpty() &&name.contains(it.key)){
      return it.value
    } }
    return null
  }

  fun findNearestPoint(pose: Pose): DogPoint? {
    val copyPoints = points.toList()
    return copyPoints.minBy { calculateDistance(it, pose) }
  }

  private fun calculateDistance(
    point: DogPoint,
    pose: Pose
  ): Float {
    val x = (point.position[0] - pose.tx()) * (point.position[0] - pose.tx())
    val y = (point.position[1] - pose.ty()) * (point.position[1] - pose.ty())
    val z = (point.position[2] - pose.tz()) * (point.position[2] - pose.tz())
    return x + y + z
  }

  private fun buildGraph() {
    val graph = DefaultDirectedGraph<Long, DefaultEdge>(DefaultEdge::class.java)

    points.forEach {
      graph.addVertex(it.id)
    }

    points.forEach {
      it.getAdjacentPointList()
          .forEach { that ->
            graph.addEdge(it.id, that)
          }
    }

    finder = DijkstraShortestPath(graph)
  }
}