package client

import java.time.ZonedDateTime

import org.scalajs.dom.raw._
import org.scalajs.dom.{document, window}
import play.api.libs.json.{JsError, JsSuccess, Json}
import shared._

import scala.scalajs.js.timers.setTimeout

case class ClientWebsocket(uiState: UIState)
  extends UIStore {

  private lazy val wsURL = s"ws://${window.location.host}/ws"

  lazy val socket = new WebSocket(wsURL)

  def connectWS() {

    socket.onmessage = {
      (e: MessageEvent) =>
        val message = Json.parse(e.data.toString)
        message.validate[AdapterMsg] match {
          case JsSuccess(AdapterRunning(logReport), _) =>
            changeIsRunning(true)
            newLogEntries(logReport)
          case JsSuccess(AdapterNotRunning(logReport), _) =>
            changeIsRunning(false)
            logReport.foreach { lr =>
              changeLastLogLevel(lr)
              newLogEntries(lr)
            }
          case JsSuccess(LogEntryMsg(le, at), _) =>
            println(s"AT: $at")
            val log = ZonedDateTime.now()
            println(s"$log")
            newLogEntry(le)
          case JsSuccess(RunStarted, _) =>
            changeIsRunning(true)
          case JsSuccess(RunFinished(logReport), _) =>
            changeIsRunning(false)
            changeLastLogLevel(logReport)
          case JsSuccess(other, _) =>
            println(s"Other message: $other")
          case JsError(errors) =>
            errors foreach println
        }
    }
    socket.onerror = { (e: Event) =>
      println(s"exception with websocket: ${e}!")
      socket.close(0, e.toString)
    }
    socket.onopen = { (_: Event) =>
      println("websocket open!")
      clearLogData()
    }
    socket.onclose = { (e: CloseEvent) =>
      println("closed socket" + e.reason)
      setTimeout(1000) {
        connectWS() // try to reconnect automatically
      }
    }
  }

  def runAdapter() {
    println("run Adapter")
    socket.send(Json.toJson(RunAdapter()).toString())
  }

  private def newLogEntries(logReport: LogReport) {
    logReport.logEntries.foreach(newLogEntry)
  }

  private def newLogEntry(logEntry: LogEntry) {
    addLogEntry(logEntry)

    val objDiv = document.getElementById("log-panel")
    objDiv.scrollTop = objDiv.scrollHeight - 20
  }

}
