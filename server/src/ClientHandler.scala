package server

import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.Socket
import protocol.*

import java.nio.charset.StandardCharsets

class ClientHandler(socket: Socket) extends Runnable {

  override def run(): Unit = {
    var reading = true
    while (reading) {
      val inputStream = socket.getInputStream
      val size = inputStream.read()
      val msgBytes = inputStream.readNBytes(size)
      val msg = upickle.default.readBinary[ClientMessage](msgBytes)
      msg match
        case ClientMessage.ExecuteCommand(cmd) =>
          // acquire the lock needed for task
          while !ServerMain.shutdownRequested && !ServerMain.taskLock.tryLock() do {
            sendMessageToClient(ServerMessage.Println("Task lock busy, waiting for it to be released..."))
            Thread.sleep(1000)
          }
          // do the task
          sendMessageToClient(ServerMessage.Println(s"Working on task '${cmd}' ..."))
          Thread.sleep(5_000) // busy working on task
          sendMessageToClient(ServerMessage.Println("Done!"))
          if ServerMain.taskLock.isLocked then ServerMain.taskLock.unlock()
          sendMessageToClient(ServerMessage.Done())
          reading = false
        case ClientMessage.Shutdown() =>
          println("Shutdown requested.")
          sendMessageToClient(ServerMessage.Done())
          ServerMain.shutdownRequested = true
          ServerMain.serverSocket.close()
      Thread.sleep(100)
    }
    socket.close()
  }

  private def sendMessageToClient(msg: ServerMessage): Unit = {
    val bytes = upickle.default.writeBinary(msg)
    socket.getOutputStream.write(bytes.length) // size acts as a delimiter too..
    socket.getOutputStream.write(bytes)
  }

}
