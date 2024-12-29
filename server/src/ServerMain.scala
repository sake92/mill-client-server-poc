package server

import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.locks.ReentrantLock
import scala.util.Using
import mainargs.{main, arg, ParserForMethods, Flag}

object ServerMain {

  val taskLock = new ReentrantLock()

  @main
  def run() = {
    println(s"Starting Mill Server...")
    log(s"Starting Mill Server...")
    val serverSocket = new ServerSocket(9999)
    while (true) {
      log("Waiting for a new client...")
      val clientSocket = serverSocket.accept()
      log("A new client connected!")
      handleNewClient(clientSocket)
    }
    log("Exiting server..")
  }

  def handleNewClient(socket: Socket) = {

    val task: Runnable = () => {
      val pw = new PrintWriter(socket.getOutputStream(), true)
      // acquire the lock needed for task
      while !taskLock.tryLock() do {
        pw.println("Task lock busy, waiting for it to be released...")
        Thread.sleep(1000)
      }

      pw.println("Working on a task...")
      Thread.sleep(5_000) // busy working on task
      pw.println("Done!")
      pw.close()
      socket.close()
      taskLock.unlock()
    }
    new Thread(task).start()
  }

  def log(str: String) =
    os.write.append(os.pwd / "server.txt", s"${str}\n")

  def main(args: Array[String]): Unit =
    ParserForMethods(this).runOrExit(args)
}
