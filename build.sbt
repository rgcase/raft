import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

name := "raft"

organization := "com.rgcase"

version := "0.1.0"

scalaVersion := "2.12.2"

crossScalaVersions := Seq("2.11.11", "2.12.2")

resolvers ++= Seq(
  "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
)

libraryDependencies ++= Seq(

  //"com.chuusai" %% "shapeless" % "2.3.2",
  //"org.typelevel" %% "cats" % "0.8.0",

  "com.typesafe.akka" %% "akka-actor" % "2.5.0",
  "com.typesafe.akka" %% "akka-remote" % "2.5.0",
  //"com.typesafe.akka" %% "akka-slf4j" % "2.4.12",
  //"com.typesafe.akka" %% "akka-stream" % "2.4.12",
  //"com.typesafe.akka" %% "akka-stream-testkit" % "2.4.12",
  //"com.typesafe.akka" %% "akka-testkit" % "2.4.12" % "test",

  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",

  //"com.typesafe.akka" %% "akka-http-core" % "2.4.11",
  //"com.typesafe.akka" %% "akka-http-experimental" % "2.4.11",
  //"com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.4.11",
  //"com.typesafe.akka" %% "akka-http-testkit" % "2.4.11",

  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"
)

scalacOptions ++= Seq(
    "-target:jvm-1.8",
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:experimental.macros",
    "-unchecked",
    "-Xfuture")

initialCommands := "import com.rgcase.raft._"

SbtScalariform.scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(DoubleIndentClassDeclaration, true)