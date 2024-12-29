package client

import mainargs.{main, arg, ParserForMethods, Flag}
import java.net.UnixDomainSocketAddress
import java.nio.channels.SocketChannel
import java.io.PipedInputStream
import java.io.PipedOutputStream
import scala.io.StdIn
import java.nio.ByteBuffer

object ClientMain {

  @main
  def run(
      @arg(short = 'c', doc = "Command to execute")
      command: Seq[String] = Seq("--version")
  ) = {
    command match {
      case Seq("--version") => println("Mill version 0.12")
      case _                => executeServerCommand()
    }
  }

  def executeServerCommand(): Unit = {
    val socketChannel = connectToServer("./out/serversocket")
    var reading = true
    while (reading) {
      readMessageFromSocket(socketChannel) match {
        case Some(msg) =>
          println(s"GOT MSG: ${msg}")
          if msg == "Done!" then reading = false
        case None =>
      }
      Thread.sleep(100)
    }
  }

  def connectToServer(socketPath: String): SocketChannel = {
    val socketAddress = UnixDomainSocketAddress.of(socketPath)
    try {
      SocketChannel.open(socketAddress)
    } catch {
      case e: java.net.ConnectException =>
        println("Could not connect to server. Starting a new one...")
        val res = os.spawn(cmd = ("java", "-jar", "out/server/assembly.dest/out.jar"))
        println(res.wrapped.info())
        Thread.sleep(1000) // wait for server to start
        SocketChannel.open(socketAddress)
    }
  }

  def readMessageFromSocket(channel: SocketChannel) = {
    val buffer = ByteBuffer.allocate(1024)
    val bytesRead = channel.read(buffer)
    Option.when(bytesRead > 0) {
      val bytes = Array.ofDim[Byte](bytesRead)
      buffer.flip();
      buffer.get(bytes);
      val message = new String(bytes);
      message
    }
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args)
}
