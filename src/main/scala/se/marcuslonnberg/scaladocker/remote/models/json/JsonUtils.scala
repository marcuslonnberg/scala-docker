package se.marcuslonnberg.scaladocker.remote.models.json

import play.api.libs.functional.Functor
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.collection.{immutable, mutable}

object JsonUtils {
  private def mapKeys[KI, KO, V](pairs: Seq[(KI, V)])(f: KI => KO): Seq[(KO, V)] = {
    pairs.map {
      case (k, v) => f(k) -> v
    }
  }

  private def toUpperCamelCase(str: String) = str.capitalize

  private def toLowerCamelCase(str: String) = {
    if (str == null) null
    else if (str.length == 0) ""
    else if (str.charAt(0).isLower) str
    else {
      val chars = str.toCharArray
      chars(0) = chars(0).toLower
      new String(chars)
    }
  }

  def upperCamelCase[T](format: Format[T]): Format[T] = new Format[T] {
    def reads(json: JsValue): JsResult[T] = {
      val converted = json match {
        case obj: JsObject => JsObject(mapKeys(obj.fields)(toLowerCamelCase))
        case x => x
      }
      format.reads(converted)
    }

    def writes(o: T): JsValue = {
      format.writes(o) match {
        case obj: JsObject => JsObject(mapKeys(obj.fields)(toUpperCamelCase))
        case x => x
      }
    }
  }

  implicit val functorJsResult: Functor[JsResult] = new Functor[JsResult] {
    override def fmap[A, B](m: JsResult[A], f: A => B) = m map f
  }

  implicit class RichSeq[A](seq: Seq[A]) {
    def toMapGroup[T, U](implicit ev: A <:< (T, U)): immutable.Map[T, Seq[U]] = {
      val m = mutable.Map.empty[T, Seq[U]]

      for (a <- seq) {
        val k = a._1
        val v = a._2
        val xs = m.getOrElse(k, Seq.empty[U])
        val elem = k -> (v +: xs)
        m += elem
      }

      m.toMap
    }
  }

  implicit class RichJsPath(path: JsPath) {
    def readWithDefault[T: Reads](default: T): Reads[T] = path.readNullable[T].map(_.getOrElse(default))

    def formatWithDefault[T](default: T)(implicit format: Format[T]): OFormat[T] = path.formatNullable[T].inmap[T](_.getOrElse(default), Some(_))
  }

}
