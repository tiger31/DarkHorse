package ru.spbstu.competition

import Graph.*
import org.kohsuke.args4j.CmdLineParser
import org.kohsuke.args4j.Option
import ru.spbstu.competition.game.Intellect
import ru.spbstu.competition.game.State
import ru.spbstu.competition.protocol.Protocol
import ru.spbstu.competition.protocol.data.*

object Arguments {
    @Option(name = "-u", usage = "Specify server url")
    var url: String = "91.151.191.57"

    @Option(name = "-p", usage = "Specify server port")
    var port: Int = 50003

    fun use(args: Array<String>): Arguments =
            CmdLineParser(this).parseArgument(*args).let{ this }
}
interface GraphAnalyser {
    fun gg(): Graph<Int>
    fun matrix(mines: List<Int>): Array<Array<List<Edge<Int>>>>
    fun printMatrix(mines: List<Int>): Unit

}
fun main(args: Array<String>) {
    val thread = object : GraphAnalyser, Thread() {
        val graph = Graph<Int>()
        override fun run() {
            print("2nd thread started")
        }
        override fun gg(): Graph<Int> = graph //Get Graph or Good Game; have fun :3
        override fun matrix(mines: List<Int>): Array<Array<List<Edge<Int>>>> {
            //Двумерный масив путей. Матрица путей лямбд
            val arr = Array(mines.size, {_ -> Array(mines.size, { _ -> listOf<Edge<Int>>()})})
            for (i in 0..mines.size - 2) {
                for (j in (i + 1)..mines.size - 1) {
                    if (arr[i][j].isEmpty()) {
                        val path = graph.bidirectionalSearch(graph[mines[i]], graph[mines[j]]) ?: listOf()
                        arr[i][j] = path
                        arr[j][i] = path.reversed()
                    }
                }
            }
            return arr
        }
        override fun printMatrix(mines: List<Int>) {
            val matrix = matrix(mines)
            matrix.forEach { it.forEach { print("${it.size}  ") }; println() }
        }
    }
    thread.start()
    Arguments.use(args)
    //Baby do you dare to do this?
    // Протокол обмена с сервером
    val protocol = Protocol(Arguments.url, Arguments.port)
    val gameState = State(thread, thread.gg())
    val intellect = Intellect(gameState, protocol)

    protocol.handShake("DarkHorse")
    val setupData = protocol.setup()
    gameState.init(setupData)

    println("Received id = ${setupData.punter}")

    protocol.ready()

    gameloop@ while(true) {
        val message = protocol.serverMessage()
        when(message) {
            is GameResult -> {
                println("The game is over!")
                val myScore = message.stop.scores[protocol.myId]
                println("Horse got scored ${myScore.score} points!")
                break@gameloop
            }
            is Timeout -> {
                println("Look's like we've shot ourselves in the foot")
            }
            is GameTurnMessage -> {
                for(move in message.move.moves) {
                    when(move) {
                        is PassMove -> {}
                        is ClaimMove -> gameState.update(move.claim)
                    }
                }
            }
        }

        intellect.makeMove()
    }
}
