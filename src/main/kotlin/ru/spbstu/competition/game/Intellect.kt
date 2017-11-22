package ru.spbstu.competition.game

import Graph.Vertex
import ru.spbstu.competition.protocol.Protocol

class Intellect(val state: State, val protocol: Protocol) {
    var i = 0
    fun makeMove() {
        //mines = лямбды
        //poitsees = вершины

        if (i < state.moves.size) {
            var source = state.moves[i].start
            var target = state.moves[i].end
            var river = state.moves[i].river
            if (state.rivers[river] == RiverState.Enemy) {
                state.moves = state.Graph.aStar(state.currentTarget, source)!!
                        .filter { state.rivers[it.river] != RiverState.Our }
                if (state.moves.isNotEmpty()) {
                    source = state.moves[0].start
                    target = state.moves[0].end
                }
                i = 0
            } else {
                i++
            }
            return protocol.claimMove(source.value, target.value)
        }

        // If there is a free river near a mine, take it!
        val try0 = state.rivers.entries.find { (river, riverState) ->
            riverState == RiverState.Neutral && (river.source in state.mines || river.target in state.mines)
        }
        if(try0 != null) return protocol.claimMove(try0.key.source, try0.key.target)

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
        if(try1 != null) return protocol.claimMove(try1.key.source, try1.key.target)

        // If there is a river near our pointsee, take it!
        val try2 = state.rivers.entries.find { (river, riverState) ->
            riverState == RiverState.Neutral && (river.source in ourSites || river.target in ourSites)
        }
        if(try2 != null) return protocol.claimMove(try2.key.source, try2.key.target)

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

}