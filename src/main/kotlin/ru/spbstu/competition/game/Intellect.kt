package ru.spbstu.competition.game

import ru.spbstu.competition.protocol.Protocol
import Graph.*
import com.sun.jmx.remote.internal.ArrayQueue
import java.util.*

class Intellect(val state: State, val protocol: Protocol) {
    val pathList: MutableList<Path> = mutableListOf()
    val pathDistance: MutableList<Pair<Int, Map<Vertex<Int>, Int>>> = mutableListOf()
    val graph = state.Graph
    var currentLambda = 0
    var swap = false
    var lambdaClosest = arrayOf<Int>()
    var pathNeedsRecount = false
    val states = Array(4, { _ -> false })

    val dynamicPath = mutableListOf<Path>()

    var fullDisconnectedGraph = false

    var lastStepLambda: Vertex<Int>? = null
    var lastStepLambdaIndex: Int = 0
    var lastStepCurrent: Vertex<Int>? = null
    var lastStepPath: MutableList<Vertex<Int>> = mutableListOf()

    fun Graph<Int>.waveSearch(from: Vertex<Int>): Map<Vertex<Int>, Int> {
        val value = mutableMapOf<Vertex<Int>, Int>()
        var wave = 0

        value.put(from, wave)

        while (value.count { it.value == wave } > 0) {
            val vertex = value.filter { it.value == wave }.map { it.key }
            for (point in vertex) {
                for (edge in point.links) {
                    if (!value.containsKey(edge.end) || value[edge.end]!! > wave + 1) {
                        value.put(edge.end, wave + 1)
                    }
                }
            }
            wave++
        }

        return value
    }

    fun init() {
        for (i in 0..state.mines.size - 2)
            for (j in i..state.mines.size - 1) {
                pathList.add(Path(state.matrix[i][j], graph, state.mines[i], state.mines[j]))
            }
        val secondList = mutableListOf<Path>()
        for (i in 0..state.mines.size - 1)
            for (j in 0..state.mines.size - 1) {
                secondList.add(Path(state.matrix[i][j], graph, state.mines[i], state.mines[j]))
            }
        lambdaClosest = Array(state.mines.size, {_ -> 0})
        pathList.sortByDescending { it.filterNeutral().size }
        state.mines.forEach { pathDistance.add(Pair(it, graph.waveSearch(graph[it]))) }
        pathDistance.sortByDescending { it.second.values.max() }
        fullDisconnectedGraph = (state.matrix.sumBy { it.sumBy { it.size } } == 0)
        dynamicPath.addAll(dynamicPathStart(secondList))
        dynamicPath.forEach {
            println("${it.from} -> ${it.to}")
        }
    }

    fun makeMove() {
        if (pathNeedsRecount) {
            pathListRecount()
            pathNeedsRecount = false
        }

        //Часть только для связных Графов (отсылка к одному из разработчиков)
        if (!fullDisconnectedGraph) {
            if (!states[0]) {
                states[0] = true
                println("State 1 started")
            }

            //Предзахват отходов от лямбд
            if (state.firstToClaim.isNotEmpty()) {
                for (i in 0..state.mines.size) {
                    val mine = (currentLambda + i) % state.mines.size
                    if (state.firstToClaim[mine].isEmpty()) continue
                    if (lambdaClosest[mine] >= 1) {
                        state.firstToClaim[mine].clear()
                        continue
                    }
                    val edge = state.firstToClaim[mine].find { state.rivers[it.river] == RiverState.Neutral }
                    if (edge == null) {
                        state.firstToClaim[mine].clear()
                        continue
                    } else {
                        currentLambda = (mine + 1) % state.mines.size
                        lambdaClosest[mine]++
                        return protocol.claimMove(edge.river.source, edge.river.target)
                    }
                }
                state.firstToClaim.clear()
            }

            if (!states[1]) {
                states[1] = true
                println("State 2 started")
            }

            //Двунаправленный захват переходов от каждой до каждой лямбы
            if (pathList.isNotEmpty()) {
                pathList.forEach {
                    if (it.path.isNotEmpty() && !it.complete && !it.unreachable) {
                        if (!it.isClear())
                            it.recount()
                        if (it.path.isNotEmpty()) {
                            val path = it.filterNeutral()
                            if (path.isNotEmpty()) {
                                if (path.size == 1) pathNeedsRecount = true
                                val index = if (swap) path.size - 1 else 0
                                swap = !swap
                                return protocol.claimMove(path[index].river.source, path[index].river.target)
                            } else {
                                it.complete = true
                            }
                        } else {
                            it.unreachable = true
                        }
                    } else {
                        it.unreachable = true
                    }
                }
                pathListRecount()
            }


        }

        if (!states[2]) {
            states[2] = true
            println("State 3 started")
        }

        //Отходной вариант - идем от лямбы к самой удаленной точки
        if (lastStepLambda == null) {
            for (i in lastStepLambdaIndex..pathDistance.size - 1) {
                val move = findNext(i)
                if (move != null)
                    return protocol.claimMove(move.river.source, move.river.target)
                lastStepCurrent = null
                lastStepPath.clear()
            }
        } else {
            for (i in lastStepLambdaIndex..pathDistance.size - 1) {
                val move = findNext(i)
                if (move != null)
                    return protocol.claimMove(move.river.source, move.river.target)
                lastStepCurrent = null
                lastStepPath.clear()
            }
        }


        if (!states[3]) {
            states[3] = true
            println("Somehow state 4 started too")
        }

        // Bah, take anything left
        val try3 = state.rivers.entries.find { (_, riverState) ->
            riverState == RiverState.Neutral
        }
        if (try3 != null) return protocol.claimMove(try3.key.source, try3.key.target)

        // (╯°□°)╯ ┻━┻
        protocol.passMove()
    }

    fun pathListRecount() {
        pathList.removeIf { it.complete || it.unreachable }
        pathList.forEach { it.recount() }
        pathList.sortByDescending { it.filterNeutral().size }
    }

    fun findNext(i: Int): Edge<Int>? {
        do {
            lastStepLambdaIndex = i
            lastStepLambda = graph[pathDistance[i].first]
            val vertex = (if (lastStepCurrent != null) lastStepCurrent!! else choseVertexAroundLambda(lastStepLambda!!)) ?: break
            val neighbours = vertex.links.filter { state.rivers[it.river] == RiverState.Neutral }
                    .map { Pair(moveToVertexValue(it.end), it) }
                    .filter { it.first > 0 }
            if (neighbours.isNotEmpty()) {
                val bestNext = neighbours.maxBy { it.first }!!.second
                lastStepPath.add(vertex)
                lastStepCurrent = bestNext.end
                return bestNext
            } else {
                fallBack()
            }
        } while (lastStepCurrent != lastStepLambda)
        return null
    }

    fun fallBack(): Unit {
        if (lastStepPath.size > 1) {
            lastStepPath.removeAt(lastStepPath.size - 1)
            lastStepCurrent = lastStepPath.last()
        } else if (lastStepPath.size == 1) {
            lastStepPath.removeAt(lastStepPath.size - 1)
            lastStepCurrent = lastStepLambda
        } else {
            lastStepCurrent = lastStepLambda
        }
    }

    fun choseVertexAroundLambda(vertex: Vertex<Int>): Vertex<Int>? {
        val queue = ArrayDeque<Vertex<Int>>(10)
        val visited = mutableListOf<Vertex<Int>>()
        queue.add(vertex)
        while (queue.isNotEmpty()) {
            val vertex1 = queue.poll()
            if (!visited.contains(vertex1)) {
                if ((vertex1.links.filter{ state.rivers[it.river] == RiverState.Neutral }
                        .map { moveToVertexValue(it.end) }
                        .maxBy { it }?: 0.0) > 0) return vertex1
                val next = vertex1.links.filter { state.rivers[it.river] == RiverState.Our }.map { it.end }
                queue.addAll(next)
                visited.add(vertex1)
            }
        }
        return null
    }

    fun moveToVertexValue(vertex: Vertex<Int>, forbidOurs: Boolean = true): Double =
            necessaryVertexCondition(vertex, forbidOurs) * (pathDistance[lastStepLambdaIndex].second[vertex] ?: 0)

    fun necessaryVertexCondition(vertex: Vertex<Int>, forbidOurs: Boolean = true): Double =
            if (vertex.links.count { state.rivers[it.river] == RiverState.Our } != 0 && forbidOurs) 0.0
            else if (vertex.links.count { state.rivers[it.river] == RiverState.Neutral } > 0) 1.0 else 0.0

    fun dynamicPathStart(items: List<Path>): List<Path> {
        var path = listOf<Path>()
        var cost = Int.MAX_VALUE
        for (item in items) {
            val newPath = dynamicPath(path + item, items.filter { it.to != item.to && it.to != item.from})
            if (pathCost(newPath) < cost) {
                path = newPath
                cost = pathCost(newPath)
            }
        }
        return path
    }
    fun dynamicPath(path: List<Path>, items: List<Path>): List<Path> {
        val available = items.filter { it.from == path.last().to }
        var bestPath = if (available.isNotEmpty()) path + available[0] else path
        var bestCost = Int.MAX_VALUE
        for (item in available) {
            val newPath = dynamicPath(path + item, items.filter { it.to != item.to })

            if (pathCost(newPath) < bestCost) {
                bestPath = newPath
                bestCost = pathCost(newPath)
            }

        }
        return bestPath
    }
    fun pathCost(path: List<Path>): Int = path.sumBy { it.path.size }
}