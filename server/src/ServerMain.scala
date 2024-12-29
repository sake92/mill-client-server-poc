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
    val serverSocket = new ServerSocket(9999)
    println(s"Started Mill Server...")
    while true do {
      println("Waiting for a new client...")
      val clientSocket = serverSocket.accept()
      println("A new client connected!")
      handleNewClient(clientSocket)
    }
    println("Exiting server..")
  }

  def handleNewClient(socket: Socket) = {
    val task: Runnable = () => {
      val pw = new PrintWriter(socket.getOutputStream(), true)
      // acquire the lock needed for task
      while !taskLock.tryLock() do {
        pw.println("Task lock busy, waiting for it to be released...")
        Thread.sleep(1000)
      }
      // do the task
      pw.println("Working on a task...")
      Thread.sleep(5_000) // busy working on task
      pw.println("DONE")
      pw.close()
      socket.close()
      taskLock.unlock()
    }
    new Thread(task).start()
  }

  def main(args: Array[String]): Unit =
    ParserForMethods(this).runOrExit(args)
}
