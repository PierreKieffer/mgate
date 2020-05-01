name := "mgate"

version := "0.1"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
"com.typesafe.akka" %% "akka-http"   % "10.1.3",
"com.typesafe.akka" %% "akka-stream" % "2.5.12",
"com.typesafe.akka" %% "akka-http-spray-json" % "10.1.3",
"io.spray" %% "spray-json" % "1.3.4")

libraryDependencies += "org.yaml" % "snakeyaml" % "1.18"
libraryDependencies += "net.liftweb" %% "lift-json" % "3.3.0"
libraryDependencies += "com.github.java-json-tools" % "json-schema-validator" % "2.2.13"
libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "2.7.0"
libraryDependencies += "org.mongodb" % "mongo-java-driver" % "3.12.1"
libraryDependencies += "org.scalaj" % "scalaj-http_2.11" % "2.3.0"
libraryDependencies += "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.10.2"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"

assemblyMergeStrategy in assembly := {
  entry: String => {
    val strategy = (assemblyMergeStrategy in assembly).value(entry)
    if (strategy == MergeStrategy.deduplicate) MergeStrategy.first
    else strategy
  }
}


