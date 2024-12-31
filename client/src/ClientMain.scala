package client

import java.net.ConnectException
import java.net.Socket
import mainargs.{ParserForMethods, arg, main}
import protocol.ClientMessage
import protocol.ServerMessage

object ClientMain {

  @main
  def run(
      @arg(short = 'c', doc = "Command to execute")
      command: String = "--version"
  ) = {
    command match {
      case "--version" =>
        println("Mill version 0.12")
      case _ =>
        executeServerCommand(command)
    }
  }

  private def executeServerCommand(command: String): Unit = {
    val socket = connectToServer()
    // send command
    val clientMessage = ClientMessage.ExecuteCommand(command)
    sendMessageToServer(socket, clientMessage)
    // read from server and act on message
    var reading = true
    while (reading) {
      val inputStream = socket.getInputStream
      val size = inputStream.read()
      val msgBytes = inputStream.readNBytes(size)
      val msg = upickle.default.readBinary[ServerMessage](msgBytes)
      msg match
        case ServerMessage.Print(text) =>
          print(s"[server] ${text}")
        case ServerMessage.Println(text) =>
          println(s"[server] ${text}")
        case ServerMessage.RunSubprocess(cmd) =>
          val res = os.call(cmd = cmd)
          println("[client] Executing subprocess done Output:")
          println(res.out.text())
        case ServerMessage.Done() =>
          reading = false
      Thread.sleep(100)
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

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args)
}
