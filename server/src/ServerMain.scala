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
import java.util.concurrent.locks.ReentrantLock

object ServerMain {

  val taskLock = new ReentrantLock()

  @main
  def run() = {
    println(s"Starting Mill Server...")
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
    val task: Runnable = () => {
      // acquire the lock needed for task
      while !taskLock.tryLock() do
        writeToChannel(clientSocketChannel, "Task lock busy, waiting for it to be released...")
        Thread.sleep(1000)

      // write something to client
      writeToChannel(clientSocketChannel, "Working on a task...")
      Thread.sleep(5_000) // busy working on task
      writeToChannel(clientSocketChannel, "Done!")
      clientSocketChannel.close()
      taskLock.unlock()
    }
    new Thread(task).start()
  }

  def writeToChannel(channel: SocketChannel, msg: String) = {
    val buffer = ByteBuffer.allocate(1024)
    buffer.clear()
    buffer.put(msg.getBytes("utf8"))
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
