import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import sbt.Keys._

import scalariform.formatter.preferences._

name := "auth-gather"

val authV = "0.4.0"

version := authV

val reactiveMongoVersion = "0.11.9"

scalaVersion := "2.11.7"

val gitHeadCommitSha = settingKey[String]("current git commit SHA")

val commonScalariform = scalariformSettings :+ (ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignParameters, true)
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(PreserveSpaceBeforeArguments, true)
  .setPreference(RewriteArrowSymbols, true))

val commons = Seq(
  organization := "ru.unicorndev",
  scalaVersion := "2.11.7",
  resolvers ++= Seq(
    "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
    "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
    Resolver.sonatypeRepo("snapshots"),
    Resolver.bintrayRepo("alari", "generic")
  ),
  gitHeadCommitSha in ThisBuild := Process("git rev-parse --short HEAD").lines.head,
  licenses +=("MIT", url("http://opensource.org/licenses/MIT")),
  bintrayPackageLabels := Seq("scala", "play", "api"),
  bintrayRepository := "generic"
) ++ commonScalariform

commons

lazy val `auth-gather` = (project in file("."))
  .dependsOn(`auth`, `auth-testkit`)
  .aggregate(`auth`, `auth-testkit`)

lazy val `auth` = (project in file("core")).settings(commons: _*).settings(
  name := "auth",
  version := authV,
  libraryDependencies ++= Seq(
    "com.github.t3hnar" %% "scala-bcrypt" % "2.4",
    "org.reactivemongo" %% "reactivemongo-extensions-bson" % "0.11.7.play24" % Provided,
    "org.reactivemongo" %% "play2-reactivemongo" % reactiveMongoVersion % Provided,
    "ru.unicorndev" %% "eventbus" % "0.2.1",
    ws % Provided,
    json % Provided,
    specs2 % Test
  )
).enablePlugins(PlayScala)
  .disablePlugins(PlayLayoutPlugin)
  .dependsOn(`auth-actions`, `auth-protocol`)
  .aggregate(`auth-actions`, `auth-protocol`)

lazy val `auth-actions` = (project in file("actions")).settings(commons: _*).settings(
  name := "auth-actions",
  version := authV,
  libraryDependencies ++= Seq(
    "com.pauldijou" %% "jwt-play" % "0.4.0",
    "net.ceedubs" %% "ficus" % "1.1.2",
    "ru.unicorndev" %% "failures" % "0.1.0"
  )
).enablePlugins(PlayScala)
  .disablePlugins(PlayLayoutPlugin)
  .dependsOn(`auth-protocol`)
  .aggregate(`auth-protocol`)

lazy val `auth-protocol` = (project in file("protocol")).settings(commons: _*).settings(
  name := "auth-protocol",
  version := authV,
  libraryDependencies ++= Seq(
    json % Provided
  )
)

val akkaHttpV = "2.0.1"

val circeV = "0.2.1"

lazy val `auth-akka` = (project in file("akka")).settings(commons: _*).settings(
  name := "auth-akka",
  version := authV + "." + gitHeadCommitSha.value,
  libraryDependencies ++= Seq(
    "com.pauldijou" %% "jwt-circe" % "0.5.0",
    "org.mindrot" % "jbcrypt" % "0.3m",
    "io.circe" %% "circe-core" % circeV,
    "io.circe" %% "circe-generic" % circeV,
    "io.circe" %% "circe-parse" % circeV,
    "com.typesafe.akka" %% "akka-slf4j" % "2.4.1",
    "com.typesafe.akka" %% "akka-stream-experimental" % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-experimental" % akkaHttpV,
    "de.heikoseeberger" %% "akka-http-circe" % "1.4.1",
    "org.spire-math" %% "cats-core" % "0.3.0"
  )
)

lazy val `auth-mongo` = (project in file("auth-mongo")).settings(commons: _*).settings(
  name := "auth-mongo",
  version := authV + "." + gitHeadCommitSha.value,
  libraryDependencies ++= Seq(
    "org.reactivemongo" %% "reactivemongo" % reactiveMongoVersion % Provided,
    "org.reactivemongo" %% "reactivemongo-extensions-bson" % "0.11.7.play24" % Provided
  )
).dependsOn(`auth-akka`).aggregate(`auth-akka`)

lazy val `auth-testkit` = (project in file("auth-testkit")).settings(commons: _*).settings(
  name := "auth-testkit",
  version := authV + "." + gitHeadCommitSha.value,
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-http-testkit-experimental" % akkaHttpV,
    "org.scalatest" %% "scalatest" % "2.2.5",
    "junit" % "junit" % "4.12",
    "de.flapdoodle.embed" % "de.flapdoodle.embed.mongo" % "1.50.1" % Test,
    "org.reactivemongo" %% "reactivemongo" % reactiveMongoVersion % Test,
    "org.reactivemongo" %% "reactivemongo-extensions-bson" % "0.11.7.play24" % Test
  )
).dependsOn(`auth-akka`, `auth-mongo`).aggregate(`auth-akka`, `auth-mongo`)

resolvers += Resolver.sonatypeRepo("releases")

resolvers += Resolver.jcenterRepo

//testOptions in Test += Tests.Argument("junitxml")

parallelExecution in Test := false

fork in Test := true

ivyScala := ivyScala.value map {
  _.copy(overrideScalaVersion = true)
}