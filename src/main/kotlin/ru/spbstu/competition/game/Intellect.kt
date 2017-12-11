package ru.spbstu.competition.game

import Graph.*
import ru.spbstu.competition.protocol.Protocol


class Intellect(val state: State, val protocol: Protocol) {
    var i = 0
    val depth = 0
    var currentLambda = 0
    var firstStepComplete = false
    val graph = state.Graph

    fun makeMove() {
        //mines = лямбды
        //poitsees = вершины

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

        if (!pathMatrixEmpty()) {
            for (i in 0..state.mines.size - 1)
                for (j in 0..state.mines.size - 1) {
                    //Получаем путь из матрицы
                    var path = state.matrix[i][j]
                    //Если он не пройден или существует
                    if (path.isNotEmpty()) {
                        //Если на пути есть хотя бы одна из клеток противника - перестраиваем путь
                        if (!pathClear(path))
                            rebuildPathBetween(state.mines[i], state.mines[j])
                        //Снова получаем уже измененный путь
                        path = state.matrix[i][j]
                        //Если он пустой, значит одна из лямбд больше не достижима
                        if (path.isNotEmpty()) {
                            //Очищаем маршрут от уже наших рек
                            path = pathNeutral(path)
                            //Если он пустой - значит весь путь был захвачен - пора переходить к другим лямбдам
                            if (path.isNotEmpty()) {
                                protocol.claimMove(path[0].river.source, path[0].river.target)
                            } else {
                                //Если пустой - обнулили, чтобы больше не возвращаться
                                state.matrix[i][j] = listOf()
                            }
                        }
                    }
                }

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
    fun pathClear(path: List<Edge<Int>> ): Boolean = path.count { graph.riverStateMap[it.river] == RiverState.Enemy } == 0
    fun pathNeutral(path: List<Edge<Int>>): List<Edge<Int>> = path
            .filter { graph.riverStateMap[it.river] == RiverState.Neutral }
            .toMutableList()
    fun rebuildPathBetween(mine1: Int, mine2: Int): Unit {
        val mine1Index = state.mines.indexOf(mine1)
        val mine2Index = state.mines.indexOf(mine2)
        val path = graph.bidirectionalSearch(graph[mine1], graph[mine2]) ?: listOf()
        state.matrix[mine1Index][mine2Index] = path
        state.matrix[mine2Index][mine1Index] = path.reversed()
    }
    fun pathMatrixEmpty(): Boolean = state.matrix.sumBy { it.sumBy { it.size } } == 0
}