ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "hivemind-amazon"
  )

scalacOptions ++= Seq("-Xsource:3")

val zioVersion = "2.0.6"
val zioDependencies = Seq(
  "dev.zio" %% "zio" % zioVersion,
  "dev.zio" %% "zio-streams" % zioVersion,
  "dev.zio" %% "zio-test" % zioVersion % Test,
  "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
  "dev.zio" %% "zio-interop-cats" % "23.0.0.0"
)

val http4sDependencies = Seq(
  "org.http4s" %% "http4s-blaze-server" % "0.23.13",
  "org.http4s" %% "http4s-dsl" % "0.23.18",
  "org.http4s" %% "http4s-circe" % "0.23.18"
)

val circeVersion = "0.14.1"
val circeDependencies = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-generic-extras",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

libraryDependencies ++= Seq(
  "io.getquill" %% "quill-jdbc-zio" % "4.6.0",
  "org.postgresql" % "postgresql" % "42.5.2",
  "com.github.scopt" %% "scopt" % "4.1.0"
)

libraryDependencies ++= zioDependencies ++ circeDependencies ++ http4sDependencies
