package client

import java.io.*
import java.net.ConnectException
import java.net.Socket
import mainargs.{main, arg, ParserForMethods, Flag}

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

  def executeServerCommand(command: String): Unit = {
    println(s"Executing server command '${command}'")
    val socket = connectToServer()
    // send command
    val pw = new PrintWriter(socket.getOutputStream(), true)
    pw.println(command)
    // read from server
    var reading = true
    while (reading) {
      val isr = new BufferedReader(new InputStreamReader(socket.getInputStream()))
      isr.readLine() match {
        case "DONE" =>
          reading = false
        case msg =>
          println(s"GOT MSG: ${msg}")
      }
      Thread.sleep(100)
    }
  }

  def connectToServer(): Socket = {
    try {
      new Socket("127.0.0.1", 9999)
    } catch {
      case e: ConnectException =>
        println("Could not connect to server. Starting a new one...")
        val res = os.spawn(cmd = ("java", "-jar", "out/server/assembly.dest/out.jar"))
        println(res.wrapped)
        Thread.sleep(1000) // wait for server to start
        new Socket("127.0.0.1", 9999)
    }
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args)
}
