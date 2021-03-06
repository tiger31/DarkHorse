package ru.spbstu.competition.game

import ru.spbstu.competition.protocol.Protocol
import Graph.*
import com.sun.jmx.remote.internal.ArrayQueue
import ru.spbstu.competition.protocol.data.River
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

    var fullDisconnectedGraph = false

    var lastStepLambda: Vertex<Int>? = null
    var lastStepLambdaIndex: Int = 0
    var lastStepCurrent: Vertex<Int>? = null
    var lastStepPath: MutableList<Vertex<Int>> = mutableListOf()

    var riverCount = 0
    var siteCount = 0

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
        riverCount = state.rivers.size
        siteCount = graph.vertexes.size
        for (i in 0..state.mines.size - 2)
            for (j in i..state.mines.size - 1) {
                pathList.add(Path(state.matrix[i][j], graph, state.mines[i], state.mines[j]))
            }
        val secondList = mutableListOf<Path>()
        for (i in 0..state.mines.size - 1)
            for (j in 0..state.mines.size - 1) {
                secondList.add(Path(state.matrix[i][j], graph, state.mines[i], state.mines[j]))
            }
        lambdaClosest = Array(state.mines.size, { _ -> 0 })
        pathList.sortBy { it.filterNeutral().size }
        state.mines.forEach { pathDistance.add(Pair(it, graph.waveSearch(graph[it]))) }
        pathDistance.sortByDescending { it.second.values.max() }
        fullDisconnectedGraph = (state.matrix.sumBy { it.sumBy { it.size } } == 0)
    }

    fun makeMove() {
        if (pathNeedsRecount) {
            pathListRecount()
            pathNeedsRecount = false
        }

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
            if (pathList.isNotEmpty()) {
                val trait = checkTrait(10)
                if (trait != null) {
                    println("Traited!")
                    return protocol.claimMove(trait.source, trait.target)
                }
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
            val trait = checkTrait(25)
            if (trait != null) {
                println("Traited!")
                return protocol.claimMove(trait.source, trait.target)
            }
            for (i in lastStepLambdaIndex..pathDistance.size - 1) {
                val move = findNext(i)
                if (move != null)
                    return protocol.claimMove(move.river.source, move.river.target)
                lastStepCurrent = null
                lastStepPath.clear()
            }
        } else {
            val trait = checkTrait(10)
            if (trait != null) {
                println("Traited!")
                return protocol.claimMove(trait.source, trait.target)
            }
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
        pathList.sortBy { it.filterNeutral().size }
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
                if ((vertex1.links.filter { state.rivers[it.river] == RiverState.Neutral }
                        .map { moveToVertexValue(it.end) }
                        .maxBy { it } ?: 0.0) > 0) return vertex1
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

    fun checkTrait(max: Int) : River? {
        val vars = state.rivers.filter {
            it.value == RiverState.Neutral &&
            graph[it.key.source].links.count() == 2 && graph[it.key.source].links.count { state.rivers[it.river] == RiverState.Enemy } == 1 &&
                    graph[it.key.target].links.count() == 2 && graph[it.key.target].links.count { state.rivers[it.river] == RiverState.Enemy } == 1
        }
        if (vars.isNotEmpty()) {
            val chance = Random().nextInt(100)
            if (chance <= max)
                return vars.keys.first()
        }
        return null
    }
}