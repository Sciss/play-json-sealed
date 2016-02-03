lazy val projectName = "play-json-sealed"

name         in ThisBuild := projectName

version      in ThisBuild := "0.2.1-SNAPSHOT"

organization in ThisBuild := "de.sciss"

scalaVersion in ThisBuild := "2.11.7"

crossScalaVersions in ThisBuild := Seq("2.11.7", "2.10.6")

description  in ThisBuild := "Automatic formats for case classes based on Play-JSON"

homepage     in ThisBuild := Some(url(s"https://github.com/Sciss/$projectName"))

licenses     in ThisBuild := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt"))

resolvers    in ThisBuild += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/maven-releases/"

scalacOptions in ThisBuild ++= Seq("-deprecation", "-unchecked", "-feature", "-Xfuture")

publishTo in ThisBuild :=
  Some(if (isSnapshot.value)
    "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  else
    "Sonatype Releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
  )

