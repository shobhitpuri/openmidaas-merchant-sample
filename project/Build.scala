import sbt._
import Keys._
import play.Project._

import aether.Aether._
import scala.sys.process._
import sbtbuildinfo.Plugin._
import com.github.play2war.plugin._

object ApplicationBuild extends Build {

  val appName         = "openmidaas-merchant-sample"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "org.fusesource.scalate" % "scalate-core_2.10" % "1.6.1",
    "net.minidev" % "json-smart" % "1.1.1"
  )

  lazy val publishSettings = Seq(
    // disable publishing the usual jars
    //publishArtifact in Compile := false,

    //publishArtifact in (Compile, packageBin) := false,
    publishArtifact in (Compile, packageDoc) := false,
    publishArtifact in (Compile, packageSrc) := false,

    // don't use the compiled jar, use the dist output
    //artifact in (Compile, dist) ~= { (art: Artifact) => art.copy(`type` = "zip", extension = "zip")  },

    // disable using the Scala version in output paths and artifacts
    // (this is optional, but it makes for a cleaner name in the repository)
    crossPaths := false,

    // publish to Artefactory/Nexus
    organization := "org.openmidaas",
    publishMavenStyle := true,

    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),

    publishTo <<= version { (v: String) =>
      val repo = "https://maven.securekeylabs.com/content/repositories/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at repo + "openmidaas-snapshots")
      else
        Some("releases"  at repo + "openmidaas-releases")
    }

  )

  val main = play.Project(appName, appVersion, appDependencies,
      settings = Defaults.defaultSettings ++ buildInfoSettings
    ).settings(
      sourceGenerators in Compile <+= buildInfo,
      buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
      buildInfoKeys ++= Seq[BuildInfoKey](
      "branch" -> (Seq("sh", "-c", "git rev-parse --abbrev-ref HEAD").!!).trim(),
      "commit" -> (Seq("sh", "-c", "git rev-list HEAD --max-count=1").!!).trim()),
      buildInfoPackage := "GTs").settings(Play2WarPlugin.play2WarSettings: _*).settings(
                Play2WarKeys.servletVersion := "3.0"
//                Play2WarKeys.targetName := Some("GTs")
        )
}
