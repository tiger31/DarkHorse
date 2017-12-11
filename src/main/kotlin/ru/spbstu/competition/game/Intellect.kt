package ru.spbstu.competition.game

import ru.spbstu.competition.protocol.Protocol


class Intellect(val state: State, val protocol: Protocol) {
    val pathList: MutableList<Path> = mutableListOf()
    val graph = state.Graph
    var currentLambda = 0
    var swap = false
    var pathNeedsRecount = false

    fun init() {
        for (i in 0..state.mines.size - 2)
            for (j in i..state.mines.size - 1) {
                pathList.add(Path(state.matrix[i][j], graph, state.mines[i], state.mines[j]))
            }
        pathList.sortByDescending { it.path.size }
    }

    fun makeMove() {
        if (pathNeedsRecount) {
            pathListRecount()
            pathNeedsRecount = false
        }

        if (state.firstToClaim.isNotEmpty()) {
            for (i in 0..state.mines.size) {
                val mine = (currentLambda + i) % state.mines.size
                if (state.firstToClaim[mine].isEmpty()) continue
                val edge = state.firstToClaim[mine].find { state.rivers[it.river] == RiverState.Neutral }
                if (edge == null) {
                    state.firstToClaim[mine].clear()
                    continue
                } else {
                    currentLambda = (mine + 1) % state.mines.size
                    return protocol.claimMove(edge.river.source, edge.river.target)
                }
            }
            state.firstToClaim.clear()
        }

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


        // If there is a free river near a mine, take it!

        // Look at all our pointsees
        val ourSites = state
                .rivers
                .entries
                .filter { it.value == RiverState.Our }
                .flatMap { listOf(it.key.source, it.key.target) }
                .toSet()
        val theirSites = state
                .rivers
                .entries
                .filter { it.value == RiverState.Enemy }
                .flatMap { listOf(it.key.source, it.key.target) }
                .toSet()

        // If there is a river between two our pointsees, take it!
        val try1 = state.rivers.entries.find { (river, riverState) ->
            riverState == RiverState.Neutral && (river.source in ourSites && river.target in ourSites)
        }
        if (try1 != null) return protocol.claimMove(try1.key.source, try1.key.target)

        // If there is a river near our pointsee, take it!
        val try2 = state.rivers.entries.find { (river, riverState) ->
            riverState == RiverState.Neutral && (river.source in ourSites || river.target in ourSites)
        }
        if (try2 != null) return protocol.claimMove(try2.key.source, try2.key.target)

        // If there is a river between two their pointsees, take it!
        val try4 = state.rivers.entries.find { (river, riverState) ->
            riverState == RiverState.Enemy && (river.source in theirSites && river.target in theirSites)
        }
        if (try4 != null) return protocol.claimMove(try4.key.source, try4.key.target)

        // If there is a river near their pointsee, take it!
        val try5 = state.rivers.entries.find { (river, riverState) ->
            riverState == RiverState.Enemy && (river.source in theirSites || river.target in theirSites)
        }
        if (try5 != null) return protocol.claimMove(try5.key.source, try5.key.target)

        //

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
        pathList.sortByDescending { it.path.size }
    }

}