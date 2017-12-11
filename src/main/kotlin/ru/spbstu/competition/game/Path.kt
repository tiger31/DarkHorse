package ru.spbstu.competition.game

import Graph.*

class Path(var path: List<Edge<Int>>, val graph: Graph<Int>, val from: Int, val to: Int) {
    var complete = false
    var unreachable = false
    fun isClear(): Boolean = path.count { graph.riverStateMap[it.river] == RiverState.Enemy } == 0
    fun filterNeutral(): List<Edge<Int>> = path.filter { graph.riverStateMap[it.river] == RiverState.Neutral }.toMutableList()
    fun recount(): List<Edge<Int>>  {
        path = graph.bidirectionalSearch(graph[from], graph[to]) ?: listOf()
        return path
    }
}
