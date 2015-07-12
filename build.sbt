
organization  := "matanster"

name := "extractor"

version := "0.0.1"

scalaVersion  := "2.11.6"

libraryDependencies ++= Seq(
  "com.lihaoyi" %% "utest" % "0.1.4" % "test",
  "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided"
)

testFrameworks += new TestFramework("utest.runner.JvmFramework")

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
