package com.github.tminglei.slickpg

import slick.jdbc.{JdbcType, PositionedResult, PostgresProfile}
import scala.reflect.classTag

trait PgJson4sSupport extends json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: PostgresProfile =>
  import driver.api._
  import org.json4s._

  ///---
  type DOCType
  def pgjson: String
  def u0000_pHolder = "[\\\\_u_0000]" //!!! change if if necessary

  val jsonMethods: JsonMethods[DOCType]
  ///---

  trait Json4sCodeGenSupport {
    // register types to let `ExModelBuilder` find them
    if (driver.isInstanceOf[ExPostgresProfile]) {
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("json", classTag[JValue])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("jsonb", classTag[JValue])
    }
  }

  /// alias
  trait JsonImplicits extends Json4sJsonImplicits

  trait Json4sJsonImplicits extends Json4sCodeGenSupport {
    implicit val json4sJsonTypeMapper: JdbcType[JValue] =
      new GenericJdbcType[JValue](
        pgjson,
        (s) => jsonMethods.parse(s),
        (v) => jsonMethods.compact(jsonMethods.render(v))
          .replace("""\\u0000""", u0000_pHolder)
          .replace("\\u0000", "")
          .replace(u0000_pHolder, """\\u0000"""),
        hasLiteralForm = false
      )

    implicit def json4sJsonColumnExtensionMethods(c: Rep[JValue]) = {
        new JsonColumnExtensionMethods[JValue, JValue](c)
      }
    implicit def json4sJsonOptionColumnExtensionMethods(c: Rep[Option[JValue]]) = {
        new JsonColumnExtensionMethods[JValue, Option[JValue]](c)
      }
  }

  trait Json4sJsonPlainImplicits extends Json4sCodeGenSupport {
    import utils.PlainSQLUtils._

    implicit class PgJsonPositionedResult(r: PositionedResult) {
      def nextJson() = nextJsonOption().getOrElse(JNull)
      def nextJsonOption() = r.nextStringOption().map(jsonMethods.parse(_))
    }

    //////////////////////////////////////////////////////////
    implicit val getJson = mkGetResult(_.nextJson())
    implicit val getJsonOption = mkGetResult(_.nextJsonOption())
    implicit val setJson = mkSetParameter[JValue](pgjson, (v) => jsonMethods.compact(jsonMethods.render(v)))
    implicit val setJsonOption = mkOptionSetParameter[JValue](pgjson, (v) => jsonMethods.compact(jsonMethods.render(v)))
  }
}
