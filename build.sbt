name := "udemy-akka-essentials"

version := "0.1"

scalaVersion := "2.13.2"

val akkaVersion = "2.6.5"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % "3.1.2",

)