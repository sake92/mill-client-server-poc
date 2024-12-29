package server

import java.io.*
import java.net.*
import java.nio.charset.StandardCharsets
import java.util.concurrent.locks.ReentrantLock
import scala.util.Using
import mainargs.{main, arg, ParserForMethods, Flag}

object ServerMain {

  val taskLock = new ReentrantLock()
  @volatile var shutdownRequested = false

  var serverSocket: ServerSocket = scala.compiletime.uninitialized

  @main
  def run() = {
    println(s"Starting Mill Server...")
    serverSocket = new ServerSocket(9999)
    println(s"Started Mill Server")
    try {
      while !shutdownRequested do {
        println("Waiting for a new client...")
        val clientSocket = serverSocket.accept()
        println("A new client connected!")
        handleNewClient(clientSocket)
      }
    } catch {
      case e: SocketException =>
        // on shutdown (server socket .close()) exception is thrown
        if e.getMessage() == "Socket closed" then println("Exiting server..")
        else e.printStackTrace()
    }
  }

  def handleNewClient(socket: Socket) = {
    val task: Runnable = () => {
      val isr = new BufferedReader(new InputStreamReader(socket.getInputStream()))
      val pw = new PrintWriter(socket.getOutputStream(), true)
      // figure out what is the command
      isr.readLine() match {
        case "SHUTDOWN" =>
          println("Shutdown requested.")
          pw.println("DONE") // tell client it's over with comms
          shutdownRequested = true
          serverSocket.close()
        case other =>
          // acquire the lock needed for task
          while !shutdownRequested && !taskLock.tryLock() do {
            pw.println("Task lock busy, waiting for it to be released...")
            Thread.sleep(1000)
          }
          // do the task
          pw.println(s"Working on task '${other}' ...")
          Thread.sleep(5_000) // busy working on task
          pw.println("DONE")
      }
      pw.close()
      socket.close()
      if taskLock.isLocked then taskLock.unlock()
    }
    new Thread(task).start()
  }

  def main(args: Array[String]): Unit =
    ParserForMethods(this).runOrExit(args)
}
