import sbt._
import Keys._

object Build extends sbt.Build {
  lazy val full: Project = Project(
    id        = "full",
    base      = file("."),
    aggregate = Seq(core, test),
    settings  = /* Defaults.coreDefaultSettings ++ */ Project.defaultSettings ++ Seq(
      publish := {},
      publishArtifact := false,
      packagedArtifacts := Map.empty           // prevent publishing anything!
    )
  )

  private def nameCompileOnly = "compile-only"

  private def projectName0 = "play-json-sealed"  // yeah, screw you too sbt

  lazy val core = Project(
    id        = "play-json-sealed",
    base      = file("core"),
    settings  = Project.defaultSettings ++ Seq(
      // cf. http://stackoverflow.com/questions/21515325/add-a-compile-time-only-dependency-in-sbt
      ivyConfigurations += config("compile-only").hide,
      // needs paradise for quasi-quotes
      resolvers += Resolver.sonatypeRepo("releases"),
      addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
      libraryDependencies ++= {
        val sq0 = if (scalaVersion.value startsWith "2.10")
          ("org.scalamacros" %% "quasiquotes" % "2.0.0" % nameCompileOnly) :: Nil
        else
          Nil

        sq0 ++ Seq(
          "com.typesafe.play" %% "play-json" % "2.3.10"
          // "org.scalatest" %% "scalatest" % "2.2.0" % "test"
        )
      },
      unmanagedClasspath in Compile ++= update.value.select(configurationFilter(nameCompileOnly)),
      publishMavenStyle := true,
      publishTo := {
        Some(if (version.value endsWith "-SNAPSHOT")
          "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
        else
          "Sonatype Releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
        )
      },
      publishArtifact in Test := false,
      pomIncludeRepository := { _ => false },
      pomExtra := { val n = projectName0
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
  )

  lazy val test = Project(
    id            = "test",
    base          = file("test"),
    dependencies  = Seq(core),
    settings      = /* Defaults.coreDefaultSettings ++ */ Project.defaultSettings ++ Seq(
      libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % "test",
      publish := {},
      publishArtifact := false,
      packagedArtifacts := Map.empty           // prevent publishing anything!
    )
  )
}
