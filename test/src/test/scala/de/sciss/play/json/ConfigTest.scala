package de.sciss.play.json

import java.io.File

import play.api.libs.json.{Reads, Format}

import scala.annotation.switch
import scala.collection.immutable.{IndexedSeq => Vec}
import scala.language.implicitConversions

object Analysis {
  object Function {
    case object Power     extends Function { final val id = 0 }
    case object MagSum    extends Function { final val id = 1 }
    case object Complex   extends Function { final val id = 2 }
    case object RComplex  extends Function { final val id = 3 }
    case object Phase     extends Function { final val id = 4 }
    case object WPhase    extends Function { final val id = 5 }
    case object MKL       extends Function { final val id = 6 }

    val seq: Vec[Function] = Vector(Power, MagSum, Complex, RComplex, Phase, WPhase, MKL)

    def apply(id: Int): Function = (id: @switch) match {
      case Power    .id => Power
      case MagSum   .id => MagSum
      case Complex  .id => Complex
      case RComplex .id => RComplex
      case Phase    .id => Phase
      case WPhase   .id => WPhase
      case MKL      .id => MKL
    }
  }
  sealed trait Function {
    def id: Int
    def productPrefix: String
    def name: String = productPrefix.toLowerCase
  }

  sealed trait ConfigLike

  trait ConfigBuilder extends ConfigLike {
    def build: Config
  }

  object Config {
    def default: Config = ???
    // def apply() = new ConfigBuilder

    implicit def build(b: ConfigBuilder): Config = b.build
  }
  final case class Config(input: File, thresh: Float, function: Function, fftSize: Int, fftOverlap: Int,
                          decay: Float, noiseFloor: Float, minGap: Int, median: Int, inputGain: Float)
    extends ConfigLike

  type Product = Vec[Long]

  type ConfigAndProduct = (Config, Option[Product])
}

object ConfigTest {
  import Analysis._
  import Formats.{FileFormat, Tuple2Format, VecFormat, OptionFormat}
  implicit val fmtFun : Format[Function]                  = AutoFormat[Function]
  implicit val fmtCfg : Format[Config]                    = AutoFormat[Config]
  // val fmtProd: Format[Product]                   = implicitly[Format[Product]]
  // implicit val fmtCfgs: Format[Vec[(Config, Vec[Long])]]  = IndexedSeq[(Config, Vec[Long])]

  // implicit def fmtOption[A](implicit peer: Format[A]): Format[Option[A]] = ???

  implicitly[Format[Vec[Long]]]
  implicitly[Format[Product]]
  implicitly[Reads[Option[String]]]
  implicitly[Format[(Config, Option[Product])]]
  implicitly[Format[Vec[ConfigAndProduct]]]
}