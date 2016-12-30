lazy val baseName         = "play-json-sealed"
lazy val baseNameL        = baseName.toLowerCase

lazy val projectVersion   = "0.4.1"
lazy val mimaVersion      = "0.4.0"

lazy val playVersionOLD   = "2.3.10"    // they dropped Java 6/7 support, thus NOT: "2.4.6"
lazy val playVersion      = "2.6.0-M1"  // yes, you are reading right, there seems to be no stable artifact for Lightbend's own platform...
lazy val scalaTestVersion = "3.0.1"
lazy val paradiseVersion  = "2.1.0"

lazy val commonSettings = Seq(
  version             := projectVersion,
  organization        := "de.sciss",
  scalaVersion        := "2.11.8",
  crossScalaVersions  := Seq("2.12.1", "2.11.8", "2.10.6"),
  description         := "Automatic formats for case classes based on Play-JSON",
  homepage            := Some(url(s"https://github.com/Sciss/$baseName")),
  licenses            := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt")),
  // resolvers           += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/maven-releases/",
  scalacOptions      ++= Seq("-deprecation", "-unchecked", "-feature", "-Xfuture", "-encoding", "utf8", "-Xlint"),
  publishTo           :=
    Some(if (isSnapshot.value)
      "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    else
      "Sonatype Releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
    )
)

lazy val full = Project(id = s"$baseNameL-full", base = file("."))
  .aggregate(core, test)
  .settings(commonSettings)
  .settings(
    publish := {},
    publishArtifact := false,
    packagedArtifacts := Map.empty // prevent publishing anything!
  )

def nameCompileOnly = "compile-only"

lazy val core = Project(id = baseNameL, base = file("core"))
  .settings(commonSettings)
  .settings(
    name := baseName,
    // cf. http://stackoverflow.com/questions/21515325/add-a-compile-time-only-dependency-in-sbt
    ivyConfigurations += config("compile-only").hide,
    // needs paradise for quasi-quotes
    resolvers += Resolver.sonatypeRepo("releases"),
    addCompilerPlugin("org.scalamacros" % "paradise" % paradiseVersion cross CrossVersion.full),
    libraryDependencies ++= {
      val sv = scalaVersion.value
      val sq0 = CrossVersion.partialVersion(sv) match {
        // if scala 2.11+ is used, quasiquotes are merged into scala-reflect
        case Some((2, scalaMajor)) if scalaMajor >= 11 =>
          Nil
        // in Scala 2.10, quasiquotes are provided by macro paradise
        case Some((2, 10)) =>
          Seq(
            compilerPlugin("org.scalamacros" %  "paradise"    % paradiseVersion cross CrossVersion.full),
            "org.scalamacros" %% "quasiquotes" % paradiseVersion cross CrossVersion.binary)
      }
      val playV = if (sv.startsWith("2.10") || sv.startsWith("2.11")) playVersionOLD else playVersion

      sq0 ++ Seq(
        "com.typesafe.play" %% "play-json" % playV
      )
    },
    mimaPreviousArtifacts := Set("de.sciss" %% baseNameL % mimaVersion),
    unmanagedClasspath in Compile ++= update.value.select(configurationFilter(nameCompileOnly)),
    publishMavenStyle := true,
    publishTo := {
      Some(if (isSnapshot.value)
        "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
      else
        "Sonatype Releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
      )
    },
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    pomExtra := { val n = baseName
      <scm>
        <url>git@github.com:Sciss/{n}.git</url>
        <connection>scm:git:git@github.com:Sciss/{n}.git</connection>
      </scm>
        <developers>
          <developer>
            <id>sciss</id>
            <name>Hanns Holger Rutz</name>
            <url>http://www.sciss.de</url>
          </developer>
        </developers>
    }
  )

lazy val test = Project(id = s"$baseNameL-test", base = file("test"))
  .dependsOn(core)
  .settings(commonSettings)
  .settings(
    libraryDependencies += "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
    publish := {},
    publishArtifact := false,
    packagedArtifacts := Map.empty           // prevent publishing anything!
  )
