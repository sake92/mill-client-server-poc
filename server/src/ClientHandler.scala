package server

import java.net.Socket
import java.net.SocketException
import scala.util.Properties
import protocol.ClientMessage
import protocol.ServerMessage

class ClientHandler(socket: Socket) extends Runnable {

  override def run(): Unit = try {
    var reading = true
    while (reading) {
      val inputStream = socket.getInputStream
      val size = inputStream.read()
      val msgBytes = inputStream.readNBytes(size)
      val msg = upickle.default.readBinary[ClientMessage](msgBytes)
      msg match
        case ClientMessage.ExecuteCommand("noop") =>
          // this is to test how quick is the round-trip from client-server-client
          sendMessageToClient(ServerMessage.Done())
        case ClientMessage.ExecuteCommand("subprocess") =>
          // non-interactive process, just run to completion
          sendMessageToClient(ServerMessage.RunSubprocess(Seq("java", "--version")))
          sendMessageToClient(ServerMessage.Done())
        case ClientMessage.ExecuteCommand("interactiveSubprocess") =>
          // interactive process, reads from user input or whatever
          val command =
            if Properties.isWin then Seq("powershell", "Read-Host",  "\"Enter input\"")
            else Seq("read", "-p", "\"Enter input: \"", "user_input")
          sendMessageToClient(ServerMessage.RunSubprocess(command))
          sendMessageToClient(ServerMessage.Done())
        case ClientMessage.ExecuteCommand("task1") =>
          // acquire the lock needed for task
          while !ServerMain.shutdownRequested && !ServerMain.taskLock.tryLock() do {
            sendMessageToClient(ServerMessage.Println("Task lock busy, waiting for it to be released..."))
            Thread.sleep(1000)
          }
          // do the task
          sendMessageToClient(ServerMessage.Println(s"Working on task ..."))
          Thread.sleep(5_000) // busy working on task
          sendMessageToClient(ServerMessage.Println("Done!"))
          if ServerMain.taskLock.isLocked then ServerMain.taskLock.unlock()
          sendMessageToClient(ServerMessage.Done())
          reading = false
        case ClientMessage.ExecuteCommand("task2") =>
          // TODO different lock !
          // acquire the lock needed for task
          while !ServerMain.shutdownRequested && !ServerMain.taskLock.tryLock() do {
            sendMessageToClient(ServerMessage.Println("Task lock busy, waiting for it to be released..."))
            Thread.sleep(1000)
          }
          // do the task
          sendMessageToClient(ServerMessage.Println(s"Working on task ..."))
          Thread.sleep(5_000) // busy working on task
          sendMessageToClient(ServerMessage.Println("Done!"))
          if ServerMain.taskLock.isLocked then ServerMain.taskLock.unlock()
          sendMessageToClient(ServerMessage.Done())
          reading = false
        case ClientMessage.ExecuteCommand(unknown) =>
          sendMessageToClient(ServerMessage.Println(s"Error: unknown task '${unknown}'"))
          sendMessageToClient(ServerMessage.Done())
        case ClientMessage.Shutdown() =>
          println("Shutdown requested.")
          sendMessageToClient(ServerMessage.Done())
          ServerMain.shutdownRequested = true
          ServerMain.serverSocket.close()
      Thread.sleep(100)
    }
  } catch {
    case e: SocketException if e.getMessage == "Connection reset" =>
    // noop, client disconnected
  } finally {
    if !socket.isClosed then socket.close()
  }

  private def sendMessageToClient(msg: ServerMessage): Unit =
    val bytes = upickle.default.writeBinary(msg)
    socket.getOutputStream.write(bytes.length) // size acts as a delimiter too..
    socket.getOutputStream.write(bytes)

}
