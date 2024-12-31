package client

import java.net.ConnectException
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ArrayBlockingQueue
import mainargs.{ParserForMethods, arg, main}
import protocol.ClientMessage
import protocol.ServerMessage

object ClientMain {

  private val messagesQueue = new ArrayBlockingQueue[ServerMessage](10)

  @volatile private var active = true

  private var serverExecuteThread: Thread = scala.compiletime.uninitialized

  // cant interrupt inputStream.read, so we just inputStream.close !
  private var serverSocket = Option.empty[Socket]

  @main
  def run(
      @arg(short = 'c', doc = "Command to execute")
      command: String = "--version"
  ): Unit = {
    command match {
      case "--version" =>
        println("Mill version 0.12")
      case _ =>
        // read messages from server in separate thread
        serverExecuteThread = new Thread(executeServerCommand(command))
        serverExecuteThread.start()
        // do the client work sequentially
        handleServerMessages()
        serverSocket.foreach(_.getInputStream.close()) // stop reading from server
    }
  }

  private def executeServerCommand(command: String): Runnable = () =>
    try {
      val socket = connectToServer()
      serverSocket = Some(socket)
      // send command
      val clientMessage = ClientMessage.ExecuteCommand(command)
      sendMessageToServer(socket, clientMessage)
      // read from server and act on message
      while active do {
        println("Waiting for a message...")
        val inputStream = socket.getInputStream
        val size = inputStream.read()
        val msgBytes = inputStream.readNBytes(size)
        val msg = upickle.default.readBinary[ServerMessage](msgBytes)
        messagesQueue.put(msg)
        Thread.sleep(100)
        println("Read a message!")
      }
    } catch {
      // expected
      case e: SocketException if e.getMessage == "Socket closed" =>
    }

  private def handleServerMessages(): Unit = {
    while active do {
      messagesQueue.take() match
        case ServerMessage.Println(text) =>
          println(s"[server] ${text}")
        case ServerMessage.RunSubprocess(cmd) =>
          println(s"Running a subprocess ${cmd}")
          val res = os.proc(cmd).call(stdin = os.Inherit, stdout = os.Inherit, stderr = os.Inherit)

          println(s"Opppp ${res.exitCode}")
        //  println("[client] Executing subprocess done Output:")
        // println(res.out.text())
        case ServerMessage.Done() =>
          println("[client] Done")
          active = false
    }
  }

  private def connectToServer(): Socket = {
    try {
      new Socket("127.0.0.1", 9999)
    } catch {
      case e: ConnectException =>
        println("Could not connect to server. Starting a new one...")
        val res = os.proc("java", "-jar", "out/server/assembly.dest/out.jar").spawn()
        //  println(res.wrapped)
        Thread.sleep(2000) // wait for server to start
        new Socket("127.0.0.1", 9999)
    }
  }

  private def sendMessageToServer(socket: Socket, msg: ClientMessage): Unit =
    val bytes = upickle.default.writeBinary(msg)
    socket.getOutputStream.write(bytes.length) // size acts as a delimiter too!
    socket.getOutputStream.write(bytes)

  def main(args: Array[String]): Unit =
    ParserForMethods(this).runOrExit(args)
}
