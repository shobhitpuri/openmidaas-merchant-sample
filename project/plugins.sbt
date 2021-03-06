// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository 
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("play" % "sbt-plugin" % "2.1.0")

// Scalate template builder
addSbtPlugin("com.mojolly.scalate" % "xsbt-scalate-generator" % "0.4.2")

// this plugin allows to access build-time info (from Build.scala)
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.2.2")

//SBT aether deploy plugin
addSbtPlugin("no.arktekk.sbt" % "aether-deploy" % "0.9")

addSbtPlugin("com.github.play2war" % "play2-war-plugin" % "0.9")
