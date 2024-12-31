package protocol

import upickle.default.ReadWriter

sealed trait ServerMessage derives ReadWriter
object ServerMessage {
  case class Println(text: String) extends ServerMessage
  case class RunSubprocess(cmd: Seq[String]) extends ServerMessage
  // server has nothing else to send to the client, client should exit after it finishes its work
  case class Done() extends ServerMessage
}

sealed trait ClientMessage derives ReadWriter
object ClientMessage {
  case class ExecuteCommand(text: String) extends ClientMessage
  case class Shutdown() extends ClientMessage
}
