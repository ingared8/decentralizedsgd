name := "AkkaDistBelief"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.12",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.12",
  "org.scalanlp" %% "breeze" % "0.11.2",
  "org.scalanlp" %% "breeze-natives" % "0.11.2",
  "org.scalatest" %% "scalatest" % "2.1.7" % "test"
)

// set the main class for 'sbt run'
mainClass in (Compile, run) := Some("edu.stanford.taddair.DecentralizedSGD.examples.MainXOR")