ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "bvs_praktikum"
  )


Compile / PB.targets := Seq(
  scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
)

val grpcVersion="1.55.1"
libraryDependencies += "io.grpc" % "grpc-netty" % grpcVersion
libraryDependencies += "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
libraryDependencies += "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion


libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % "2.8.0"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.4.6"
libraryDependencies += "com.typesafe.akka" %% "akka-cluster-typed" % "2.8.0"
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % "2.8.0"
libraryDependencies += "com.typesafe.akka" %% "akka-serialization-jackson" %"2.8.0"
libraryDependencies += "com.typesafe.akka" %% "akka-cluster-sharding-typed" % "2.8.0"

libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.8.0"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.5.2"

libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.5.2"


libraryDependencies += "com.lihaoyi" %% "requests" % "0.8.0"

libraryDependencies += "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % "3.8.13"
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.32"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.6"

libraryDependencies += "io.spray" %% "spray-json" % "1.3.6"

libraryDependencies += "com.github.cb372" %% "scalacache-caffeine" % "0.28.0"