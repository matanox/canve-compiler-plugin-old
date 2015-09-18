organization  := "canve"

name := "compiler-plugin"

version := "0.0.1"

scalaVersion  := "2.11.7"

crossScalaVersions := Seq("2.10.4", "2.11.7")

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
  "canve" %% "compiler-plugin-unit-test-lib" % "1.0.0",
  "com.lihaoyi" %% "utest" % "0.3.1"
)

testFrameworks += new TestFramework("utest.runner.Framework")

unmanagedSourceDirectories in Test <+= baseDirectory(_ / "src" / "test" / "resources")

// Sonatype
publishArtifact in Test := false

publishTo <<= version { (v: String) =>
  Some("releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
}

pomExtra := (
  <url>https://github.com/matanster/extractor</url>
    <licenses>
      <license>
        <name>MIT license</name>
        <url>http://www.opensource.org/licenses/mit-license.php</url>
      </license>
    </licenses>
    <developers>
      <developer>
        <id>matanster</id>
        <name>matanster</name>
        <url>https://github.com/matanster</url>
      </developer>
    </developers>
  )
