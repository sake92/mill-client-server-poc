package server

import mainargs.{main, arg, ParserForMethods, Flag}

object ServerMain {

  @main
  def run(
      @arg(short = 'c', doc = "Command to execute")
      command: Seq[String] = Seq("--version")
  ) = {
    command match {
      case Seq("--version") => println("Mill version 0.12")
      case _                => println("Hello!")
    }
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args)
}
