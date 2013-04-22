import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "app"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    //jdbc,
    //anorm
    "org.fusesource.scalate" % "scalate-core_2.10" % "1.6.1"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )

}
