package com.ardog.utils

import com.ardog.extensions.getAdjacentPointList
import com.ardog.model.DogPoint
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import java.lang.IllegalArgumentException


class PathFinder {

    lateinit var map: Map<String, DogPoint>
    lateinit var idMaps: Map<Long, DogPoint>
    lateinit var points: List<DogPoint>

    lateinit var finder: DijkstraShortestPath<Long, DefaultEdge>

    fun initMap(points: List<DogPoint>) {
        map = points.map { it.name to it }.toMap()
        this.idMaps = points.map { it.id to it }.toMap()
        this.points = points
        buildGraph()
    }

    fun findRoutes(start: DogPoint, destination: DogPoint): List<DogPoint> {

        if (!idMaps.containsKey(start.id) || !idMaps.containsKey(destination.id)) {
            return ArrayList()
        }

        val startId = start.id
        var destinationId = destination.id
        val result = finder.getPath(startId, destinationId);

        return result.vertexList.map { idMaps[it] ?: error("invalid point") }.toList()
    }

    fun findRoutes(startPointName: String, destinationName: String): List<DogPoint> {

        if (!map.containsKey(startPointName) || !map.containsKey(destinationName)){
            return ArrayList()
        }
        return findRoutes(map[startPointName]!!,map[destinationName]!!)
    }

    private fun buildGraph() {
        val graph = DefaultDirectedGraph<Long, DefaultEdge>(DefaultEdge::class.java)

        points.forEach {
            graph.addVertex(it.id)

            it.getAdjacentPointList().forEach { that ->
                graph.addEdge(it.id, that)
            }
        }

        finder = DijkstraShortestPath(graph)
    }
}