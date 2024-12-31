package server

import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.Socket
import protocol.*

import java.nio.charset.StandardCharsets

class ClientHandler(socket: Socket) extends Runnable {

  override def run(): Unit = {
    val isr = new BufferedReader(new InputStreamReader(socket.getInputStream))
    // figure out what is the command
    isr.readLine() match {
      case "SHUTDOWN" =>
        println("Shutdown requested.")
        sendMessageToClient(ServerMessage.Done())
        ServerMain.shutdownRequested = true
        ServerMain.serverSocket.close()
      case other =>
        // acquire the lock needed for task
        while !ServerMain.shutdownRequested && !ServerMain.taskLock.tryLock() do {
          sendMessageToClient(ServerMessage.Println("Task lock busy, waiting for it to be released..."))
          Thread.sleep(1000)
        }
        // do the task
        sendMessageToClient(ServerMessage.Println(s"Working on task '${other}' ..."))
        Thread.sleep(5_000) // busy working on task
        sendMessageToClient(ServerMessage.Println("Done!"))
        if ServerMain.taskLock.isLocked then ServerMain.taskLock.unlock()
        sendMessageToClient(ServerMessage.Done())
    }
    socket.close()
  }

  private def sendMessageToClient(msg: ServerMessage): Unit = {
    println(s"Sending message ${msg}")
    val bytes = upickle.default.writeBinary(msg)
    socket.getOutputStream.write(bytes.length) // size acts as a delimiter too..
    socket.getOutputStream.write(bytes)
  }

 /* def readMessage(bytes: Array[Byte]): ServerMessage = {
    upickle.default.readBinary[ServerMessage](bytes)
  }*/
}
