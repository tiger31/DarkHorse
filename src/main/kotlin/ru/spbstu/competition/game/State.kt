package ru.spbstu.competition.game

import ru.spbstu.competition.protocol.data.Claim
import ru.spbstu.competition.protocol.data.River
import ru.spbstu.competition.protocol.data.Setup
import Graph.*

enum class RiverState { Our, Enemy, Neutral }

class State {
    val rivers = mutableMapOf<River, RiverState>()
    var mines = listOf<Int>()
    var myId = -1

    val Graph = Graph<Int>()
    lateinit var currentTarget: Vertex<Int>

    lateinit var moves: List<Edge<Int>>

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
        this.moves = Graph.aStar(Graph[setup.map.mines[0]], Graph[setup.map.mines[1]])?: listOf()
        currentTarget = Graph[setup.map.mines[1]]
    }

    fun update(claim: Claim) {
        rivers[River(claim.source, claim.target)] = when (claim.punter) {
            myId -> RiverState.Our
            else -> RiverState.Enemy
        }
    }
}
