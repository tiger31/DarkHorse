package ru.spbstu.competition.game

import ru.spbstu.competition.protocol.data.Claim
import ru.spbstu.competition.protocol.data.River
import ru.spbstu.competition.protocol.data.Setup
import Graph.*
import ru.spbstu.competition.GraphAnalyser

enum class RiverState { Our, Enemy, Neutral }

class State(val thread: GraphAnalyser, val Graph: Graph<Int>) {
    val rivers = mutableMapOf<River, RiverState>()
    var mines = listOf<Int>()
    var myId = -1

    lateinit var firstToClaim: MutableList<MutableList<Edge<Int>>>
    lateinit var matrix: Array<Array<List<Edge<Int>>>>

    fun init(setup: Setup) {
        myId = setup.punter
        for ((id, x, y) in setup.map.sites) {
            Graph.addVertex(Vertex(id, x!!, y!!))
        }
        println()
        println()
        for (river in setup.map.rivers) {
            Graph[river.source].linkBoth(Graph[river.target], river)
            rivers[river] = RiverState.Neutral
        }
        println()
        println()
        for (mine in setup.map.mines) {
            Graph[mine].is_mine = true
            mines += mine
        }

        val list = mutableListOf<MutableList<Edge<Int>>>()
        for (mine in mines) {
            val curr = Graph[mine]
            val subList = mutableListOf<Edge<Int>>()
            subList.addAll(curr.links)
            for (link in curr.links) {
                subList.addAll(link.end.links)
            }
            list.add(subList)
        }
        firstToClaim = list
        matrix = thread.matrix(mines)
    }

    fun update(claim: Claim) {
        rivers[River(claim.source, claim.target)] = when (claim.punter) {
            myId -> RiverState.Our
            else -> RiverState.Enemy
        }
    }
}
