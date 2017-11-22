package ru.spbstu.competition

import org.kohsuke.args4j.CmdLineParser
import org.kohsuke.args4j.Option
import ru.spbstu.competition.game.Intellect
import ru.spbstu.competition.game.State
import ru.spbstu.competition.protocol.Protocol
import ru.spbstu.competition.protocol.data.*
import java.lang.UnsupportedOperationException

object Arguments {
    @Option(name = "-u", usage = "Specify server url")
    var url: String = "91.151.191.57"

    @Option(name = "-p", usage = "Specify server port")
    var port: Int = 50006

    fun use(args: Array<String>): Arguments =
            CmdLineParser(this).parseArgument(*args).let{ this }
}

fun main(args: Array<String>) {
    val thread = Thread(Runnable {
        val arr = arrayOf(
            "So you wanna play with magic",
            "Boy, you should know what you're falling for",
            "Baby do you dare to do this?",
            "Cause I’m coming at you like a dark horse",
            "Are you ready for, ready for",
            "A perfect storm, perfect storm",
            "Cause once you’re mine, once you’re mine",
            "There’s no going back"
        )
        for (i in 0..arr.size - 1) {
            println(arr[i])
            Thread.sleep(1000)
        }
    })
    thread.start()
    Arguments.use(args)
    //Baby do you dare to do this?
    // Протокол обмена с сервером
    val protocol = Protocol(Arguments.url, Arguments.port)
    // Состояние игрового поля
    val gameState = State()
    // Джо очень умный чувак, вот его ум
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
