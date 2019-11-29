package shared


import java.time.{ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter

import play.api.libs.json.{JsNumber, JsString, JsValue, Writes}

object JavaWrites extends JavaWrites

trait JavaWrites {
  def getZoneId(zone: String) = ZoneId.of(zone)

  def jodaDateWrites(pattern: String): Writes[ZonedDateTime] = new Writes[ZonedDateTime] {
    val df = DateTimeFormatter.ofPattern(pattern)
    def writes(d: ZonedDateTime): JsValue = JsString(d.format(df))
  }

  /**
    * Serializer ZonedDateTime -> JsNumber(d.getMillis (number of milliseconds since the Epoch))
    */
  implicit object JavaZonedDateTimeNumberWrites extends Writes[ZonedDateTime] {
    def writes(d: ZonedDateTime): JsValue = JsNumber(d.toInstant.toEpochMilli)
  }

  object JavaDateTimeWrites extends Writes[ZonedDateTime] {
    def writes(d: ZonedDateTime): JsValue = JsString(d.toString)
  }

}