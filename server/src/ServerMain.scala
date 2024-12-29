package server

import mainargs.{main, arg, ParserForMethods, Flag}
import java.nio.ByteBuffer
import java.nio.file.Files
import java.net.UnixDomainSocketAddress
import java.nio.channels.ServerSocketChannel
import java.net.StandardProtocolFamily
import java.nio.charset.StandardCharsets
import scala.util.Using
import java.nio.channels.SocketChannel

object ServerMain {

  @main
  def run() = {
    log(s"Starting Mill Server...")
    val address = UnixDomainSocketAddress.of("./out/serversocket")
    Files.deleteIfExists(address.getPath())
    Using.resource(ServerSocketChannel.open(StandardProtocolFamily.UNIX)) { serverSocketChannel =>
      serverSocketChannel.bind(address)
      while (true) {
        // serverSocketChannel.configureBlocking(true)
        log("Waiting for a new client...")
        val clientSocketChannel = serverSocketChannel.accept()
        log("A new client connected!")
        handleNewClient(clientSocketChannel)
      }
    }
    log("Exiting server..")
    Files.deleteIfExists(address.getPath())
  }

  def handleNewClient(clientSocketChannel: SocketChannel) = {
    var done = false
    val task: Runnable = () => {
      // write something to client
      log("Writing hello..")
      writeToChannel(clientSocketChannel, "Hello!")
      Thread.sleep(1_000)
      log("Writing task..")
      writeToChannel(clientSocketChannel, "Working on a task...")
      Thread.sleep(5_000)
      writeToChannel(clientSocketChannel, "Done!")
      clientSocketChannel.close()
    }
    new Thread(task).start()
  }

  def writeToChannel(channel: SocketChannel, msg: String) = {
    val buffer = ByteBuffer.allocate(1024)
    buffer.clear()
    buffer.put(msg.getBytes())
    buffer.flip()
    while (buffer.hasRemaining()) {
      channel.write(buffer)
    }
  }

  def log(str: String) =
    os.write.append(os.pwd / "server.txt", s"${str}\n")

  def main(args: Array[String]): Unit =
    ParserForMethods(this).runOrExit(args)
}
