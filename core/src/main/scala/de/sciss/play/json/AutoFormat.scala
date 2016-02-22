/*
 * AutoFormat.scala
 * (play-json-sealed)
 *
 * Copyright (c) 2013-2016 Hanns Holger Rutz. All rights reserved.
 *
 * This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 * For further information, please contact Hanns Holger Rutz at
 * contact@sciss.de
 */

package de.sciss.play.json

import scala.reflect.macros.Context
import language.experimental.macros
import play.api.libs.json.{JsError, Writes, JsValue, JsResult, JsMacroImpl, Format, JsSuccess, JsString, JsObject}
import collection.immutable.{IndexedSeq => Vec}

object AutoFormat {
  private val DEBUG   = false
  private val PACKAGE = false

  def apply[A]: Format[A] = macro applyImpl[A]

  private def log(what: => String): Unit = println(s"<sealed> $what")

  def applyImpl[A: c.WeakTypeTag](c: Context): c.Expr[Format[A]] = {
    import c.universe._
    val aTpeW   = c.weakTypeOf[A]
    val aClazz  = aTpeW.typeSymbol.asClass

    // directly support Vec
    if (aTpeW <:< typeOf[Vec[Any]]) {
      // cf. stackoverflow nr. 12842729
      val ta    = aTpeW.asInstanceOf[TypeRefApi].args.head
      val tree  = q"_root_.de.sciss.play.json.Formats.VecFormat[$ta]"
      return c.Expr[Format[A]](tree)
    }

    // directly support Vec
    if (aTpeW <:< typeOf[(Any, Any)]) {
      val ta :: tb :: Nil = aTpeW.asInstanceOf[TypeRefApi].args // XXX TODO: doesn't work with type aliases, e.g. type X = (Y, Z)
      val tree = q"_root_.de.sciss.play.json.Formats.Tuple2Format[$ta, $tb]"
      return c.Expr[Format[A]](tree)
    }

    // fall back to Json.format
    if (!aClazz.isTrait) {
      if (DEBUG) log(s"Type $aTpeW is not sealed")
      return JsMacroImpl.formatImpl[A](c)
    }

    def id(s: String) = Ident(newTermName(s))

    val aIdent  = Ident(aClazz)

    require(aClazz.isSealed, s"Type $aTpeW is not sealed")
    aClazz.typeSignature  // SI-7046 !
    val subs    = aClazz.knownDirectSubclasses
    require(subs.nonEmpty  , s"Type $aTpeW does not have known direct subclasses")

    if (DEBUG) {
      log(s"Known direct subclasses of $aTpeW")
      subs.foreach(s => log(s.toString))
      log("")
    }

    val subsAll = subs.toList.sortBy(_.name.toString)
    // for each sub type a tuple of writer-case-body, (bool, reader-case-body) where `bool` is true
    // if the sub type is a singleton object and false if it is a case class
    val cases   = subsAll.map { sub =>
      val subC      = sub.asClass
      val isObject  = subC.isModuleClass
      val subIdent  = Ident(if (isObject) {
        val companion = subC.companionSymbol
        require(companion.isModule, s"Sub type $subC of $aTpeW does not have a companion object")
        companion.asModule
      } else subC)
      val patWrite  = Bind(newTermName("x"), Typed(id("_"), subIdent))
      val subName0  = if (PACKAGE) sub.fullName else sub.name.toString
      val subName   = Literal(Constant(subName0))
      val patRead   = subName
      val (bodyWrite, bodyRead) = if (isObject) {
        val jsSuccessTree = Ident(typeOf[JsSuccess.type].typeSymbol.asClass.companionSymbol.asModule) // frickin' hell
        Apply(id("writeObject"), subName :: Nil) ->
        Apply(TypeApply(jsSuccessTree, aIdent :: Nil), subIdent :: Nil) // JsSuccess[A](MyCaseObject)

      } else {
        // val jsonTree  = Ident(typeOf[Json.type].typeSymbol.asClass)
        // val subFmt    = TypeApply(Select(jsonTree, "format"), subIdent :: Nil)
        val jsonTree  = Ident(typeOf[AutoFormat.type].typeSymbol.asClass)
        val subFmt    = TypeApply(Select(jsonTree, newTermName("apply")), subIdent :: Nil)
        Apply(TypeApply(id("writeClass"), subIdent:: Nil), subName :: id("x") :: subFmt :: Nil) ->
        Apply(Select(subFmt, newTermName("reads")), id("data") :: Nil)
      }
      CaseDef(pat = patWrite, guard = EmptyTree, body = bodyWrite) ->
        (isObject, CaseDef(pat = patRead, guard = EmptyTree, body = bodyRead))
    }
    val (casesWrite, casesRead) = cases.unzip
    val matchWrite      = Match(id("value"), casesWrite)
    val matchWriteExpr  = c.Expr[JsValue](matchWrite)
    val casesReadC      = casesRead collect { case (false, tree) => tree }
    val matchReadC      = Match(id("name"), casesReadC) // XXX TODO add catch all
    val matchReadExprC  = c.Expr[JsResult[A]](matchReadC)
    val casesReadO      = casesRead collect { case (true, tree) => tree }
    val matchReadO      = Match(id("name"), casesReadO) // XXX TODO add catch all
    val matchReadExprO  = c.Expr[JsResult[A]](matchReadO)
    val r               = reify {
      new Format[A] {
        private def writeClass[A1](name: String, obj: A1, w: Writes[A1]): JsValue =
          JsObject(Seq("class" -> JsString(name), "data" -> w.writes(obj)))

        private def writeObject(name: String): JsValue =
          JsObject(Seq("class" -> JsString(name)))

        def writes(value: A): JsValue = matchWriteExpr.splice

        def reads(json: JsValue): JsResult[A] = try {
          (json: @unchecked) match {
            // this crashes upon macro expansion (probably a bug)
            // case JsObject(Seq(("class", JsString(name)), rest @ _*)) => ...
            case JsObject(sq) =>
              (sq.toList: @unchecked) match {
                case ("class", JsString(name)) :: tail =>
                  (tail: @unchecked) match {
                    case ("data", data) :: Nil  => matchReadExprC.splice
                    case Nil                    => matchReadExprO.splice
                  }
              }
          }
        } catch {
          case _: MatchError => JsError(json.toString)
        }
      }
    }
    val t = r.tree

    if (DEBUG) {
      log("Tree:")
      println(t)
    }
    c.Expr[Format[A]](t)
  }
}
