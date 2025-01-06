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

  @volatile private var running = true

  // cant interrupt inputStream.read, so we just inputStream.close !
  private var serverSocket = Option.empty[Socket]

  // propagate subprocess' exit code if available
  private var exitCode = 0

  var mainThread: Thread = scala.compiletime.uninitialized

  @main
  def run(@arg(short = 'c', doc = "Command to execute") command: String): Unit = {
    mainThread = Thread.currentThread()
    command match {
      case "version" =>
        println("Mill version 0.12")
      case _ =>
        // read messages from server in separate thread
        val serverExecuteThread = new Thread(executeServerCommand(command))
        serverExecuteThread.start()
        // do the client work sequentially
        handleServerMessages()
        serverSocket.foreach(_.getInputStream.close()) // stop reading from server
        System.exit(exitCode)
    }
  }

  private def executeServerCommand(command: String): Runnable = () =>
    try {
      val socket = connectToServer()
      serverSocket = Some(socket)
      // send command
      val clientMessage: ClientMessage =
        if command == "shutdown" then ClientMessage.Shutdown()
        else ClientMessage.ExecuteCommand(command)
      sendMessageToServer(socket, clientMessage)
      // read from server and act on message
      val inputStream = socket.getInputStream
      while true do {
        if inputStream.available() > 0 then {
          val size = inputStream.read()
          val msgBytes = inputStream.readNBytes(size)
          val msg = upickle.default.readBinary[ServerMessage](msgBytes)
          if msg.isInstanceOf[ServerMessage.WatchUpdate] then {
            // stop running process and just wait for another message..
            // this is to avoid situations like this:
            /*
            An unexpected error occurred java.nio.file.FileSystemException: D:\projects\sake\mill-client-server-poc\out\server\assembly.dest\out.jar:
                The process cannot access the file because it is being used by another process
            java.base/sun.nio.fs.WindowsException.translateToIOException(WindowsException.java:92)
             */
            println("Shutting down currently running subprocess")
            mainThread.interrupt()
          } else {
            messagesQueue.put(msg)
          }
        }
        Thread.sleep(1)
      }
    } catch {
      // expected
      case e: SocketException if e.getMessage == "Socket closed" =>
    }

  private def handleServerMessages(): Unit = {
    while running do {
      try {
        println("Waiting for a command from server..")
        val serverCommand = messagesQueue.take()
        println(s"Got a command from server: ${serverCommand}")
        serverCommand match
          case ServerMessage.Println(text) =>
            println(text)
          case ServerMessage.RunSubprocess(cmd) =>
            println(s"Running a subprocess: ${cmd}")
            var process: os.SubProcess = null
            try {
              process = os.proc(cmd).spawn(stdin = os.Inherit, stdout = os.Inherit, stderr = os.Inherit)
              process.waitFor()
            } catch {
              case e: InterruptedException =>
                process.destroy(shutdownGracePeriod = 0)
            }
          case ServerMessage.WatchUpdate() => // noop, should never happen
          case ServerMessage.Done() =>
            println("Done")
            running = false
      } catch {
        case e: InterruptedException => // noop
        // this happens when watched process has finished
        // and the server sends a WatchUpdate
      }
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
