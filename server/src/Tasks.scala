package server

import java.util.concurrent.locks.ReentrantLock
import protocol.ServerMessage

object Tasks {

  private val taskLocks = Map(
    "task1" -> new ReentrantLock(),
    "task2" -> new ReentrantLock()
  )
  private val MaxRetries = 10

  def runWithLock(taskName: String, clientComms: ClientComms)(taskCode: => Unit): Unit = {
    val taskLock = taskLocks(taskName)
    var attempts = 1
    var didRun = false
    while attempts <= MaxRetries && !didRun do {
      if taskLock.tryLock() then {
        // all good, we can proceed
        didRun = true
        try taskCode
        finally taskLock.unlock()
      } else {
        clientComms.sendMessage(
          ServerMessage.Println(s"Task lock for ${taskName} is locked by another task, retrying in 1s...")
        )
        attempts += 1
        Thread.sleep(1000)
      }
    }
    // if taskLock.isLocked then taskLock.unlock()
    if !didRun then {
      clientComms.sendMessage(
        ServerMessage.Println(s"Task lock for ${taskName} is locked by another task, gave up after ${MaxRetries} tries")
      )
      clientComms.sendMessage(ServerMessage.Done())
    }
  }

}
