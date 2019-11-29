package shared
import java.time.{Instant, ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter

import play.api.libs.json._

object JavaReads extends JavaReads

trait JavaReads {
  def getZoneId(zone: String) = ZoneId.of(zone)
  /**
    * Reads for the `java.time.ZonedDateTime` type.
    *
    * @param pattern a date pattern, as specified in `org.joda.time.format.ZonedDateTimeFormat`, or "" to use ISO format.
    * @param corrector a simple string transformation function that can be used to transform input String before parsing.
    *                  Useful when standards are not respected and require a few tweaks. Defaults to identity function.
    */
  def jodaDateReads(pattern: String, zone: String = "UTC", corrector: String => String = identity): Reads[ZonedDateTime] = new Reads[ZonedDateTime] {

    val df = if (pattern == "") DateTimeFormatter.ISO_ZONED_DATE_TIME else DateTimeFormatter.ofPattern(pattern)

    def reads(json: JsValue): JsResult[ZonedDateTime] = json match {
      case JsNumber(d) => JsSuccess(ZonedDateTime.ofInstant(Instant.ofEpochMilli(d.longValue()), getZoneId(zone)))
      case JsString(s) => parseDate(corrector(s)) match {
        case Some(d) => JsSuccess(d)
        case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.date.format", pattern))))
      }
      case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.date"))))
    }

    private def parseDate(input: String): Option[ZonedDateTime] =
      scala.util.control.Exception.nonFatalCatch[ZonedDateTime] opt (ZonedDateTime.parse(input, df))

  }

  /**
    * The default implicit reads, using yyyy-MM-dd format
    */
  val JavaDateReads = jodaDateReads("yyyy-MM-dd")

  /**
    * The default implicit reads, using ISO-8601 format
    */
  implicit val DefaultJavaZonedDateTimeReads = jodaDateReads("")
}