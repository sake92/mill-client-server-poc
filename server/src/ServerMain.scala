package server

import java.io.RandomAccessFile
import java.net.{ServerSocket, SocketException}
import java.nio.file.Paths
import java.util.concurrent.locks.ReentrantLock
import scala.util.Using
import mainargs.{ParserForMethods, main}

object ServerMain {

  private val serverLockFile = Paths.get("server.lock")

  var serverSocket: ServerSocket = scala.compiletime.uninitialized
  @volatile var shutdownRequested = false

  @main
  def run(): Unit = {
    println(s"Starting Mill Server...")
    // acquire the server file lock
    // there can only be **one server running at a time**
    def runWithServerFileLock(serverCode: => Unit): Unit =
      Using.resource(new RandomAccessFile(serverLockFile.toFile, "rw")) { raf =>
        Using.resource(raf.getChannel) { ch =>
          var attempts = 1
          var didRun = false
          while attempts <= 5 && !didRun do {
            val lock = ch.tryLock()
            if lock == null then {
              attempts += 1
              println("Server file lock is locked by another server process, retrying in 1s...")
              Thread.sleep(1000)
            } else {
              // all good, we can proceed
              didRun = true
              serverCode
            }
          }
          if !didRun then println("Server file lock is locked by another server process, gave up after 5 tries")
        }
      }

    runWithServerFileLock(runServer())
  }

  private def runServer(): Unit = {
    serverSocket = new ServerSocket(9999)
    println(s"Started Mill Server")
    try {
      while !shutdownRequested do {
        println("Waiting for a new client...")
        val clientSocket = serverSocket.accept()
        println("A new client connected!")
        val clientHandler = new ClientHandler(clientSocket)
        new Thread(clientHandler).start()
      }
    } catch {
      case e: SocketException =>
        // on shutdown (server socket .close()) exception is thrown
        if e.getMessage == "Socket closed"
        then println("Exiting server..")
        else e.printStackTrace()
    }
  }

  def main(args: Array[String]): Unit =
    ParserForMethods(this).runOrExit(args)
}
