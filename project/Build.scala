import sbt._
import Keys._

object BuildSettings {
  val buildSettings = Defaults.defaultSettings ++ Seq(
    name := "compiler-plugin",
    organization := "canve",
    version := "0.0.1",
    scalacOptions ++= Seq("-deprecation"),
    scalaVersion := "2.11.7",
    crossScalaVersions := Seq("2.10.4", "2.11.7"),
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += Resolver.sonatypeRepo("releases"),
    
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
      "org.scala-lang" % "scala-library" % scalaVersion.value % "provided",
      //"org.apache.tinkerpop" % "tinkergraph-gremlin" % "3.0.1-incubating",
      "canve" %% "simple-graph" % "0.0.1",
      "canve" %% "compiler-plugin-unit-test-lib" % "1.0.0" % "test",
      "com.lihaoyi" %% "utest" % "0.3.1" % "test"
    ),
    
    testFrameworks += new TestFramework("utest.runner.Framework")
    
  )
}

object MyBuild extends Build {
  import BuildSettings._

  lazy val root: Project = Project(
    "root",
    file("."),
    settings = buildSettings
  )
}
