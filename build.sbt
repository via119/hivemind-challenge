ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "hivemind-amazon"
  )

scalacOptions ++= Seq("-Xsource:3")

libraryDependencies += "dev.zio" %% "zio" % "2.0.6"
libraryDependencies += "dev.zio" %% "zio-streams" % "2.0.6"
libraryDependencies += "dev.zio" %% "zio-test" % "2.0.6" % Test
libraryDependencies += "dev.zio" %% "zio-test-sbt" % "2.0.6" % Test
libraryDependencies += "dev.zio" %% "zio-interop-cats" % "23.0.0.0"

libraryDependencies += "org.http4s" %% "http4s-blaze-server" % "0.23.13"
libraryDependencies += "org.http4s" %% "http4s-dsl" % "0.23.18"
libraryDependencies += "org.http4s" %% "http4s-circe" % "0.23.18"
libraryDependencies += "com.github.scopt" %% "scopt" % "4.1.0"

val circeVersion = "0.14.1"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)
