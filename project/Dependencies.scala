import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.0-SNAP10"
  lazy val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.14.0"
  lazy val akkaActor = "com.typesafe.akka" %% "akka-actor" % "2.6.1"
  lazy val akkaTest = "com.typesafe.akka" %% "akka-testkit" % "2.6.1" 
  lazy val akkaStream = "com.typesafe.akka" %% "akka-stream" % "2.6.1"
}
