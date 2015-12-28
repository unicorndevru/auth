import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import sbt.Keys._

import scalariform.formatter.preferences._

name := "auth-gather"

val authV = "0.3.2"

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
  .dependsOn(`auth`)
  .aggregate(`auth`)

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

lazy val `auth-akka` = (project in file("akka")).settings(commons: _*).settings(
  name := "auth-akka",
  version := authV,
  libraryDependencies ++= Seq(
    // "com.pauldijou" %% "jwt-circe" % "0.4.1",
    "org.mindrot" % "jbcrypt" % "0.3m",
    "io.circe" %% "circe-core" % "0.2.1",
    "io.circe" %% "circe-generic" % "0.2.1",
    "io.circe" %% "circe-parse" % "0.2.1",
    "com.typesafe.akka" %% "akka-slf4j" % "2.4.1",
    "com.typesafe.akka" %% "akka-stream-experimental" % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-experimental" % akkaHttpV,
    "de.heikoseeberger" %% "akka-http-circe" % "1.4.1",
    "org.spire-math" %% "cats-core" % "0.3.0",
    "com.iheart" %% "ficus" % "1.2.1",
    "com.typesafe.akka" %% "akka-http-testkit-experimental" % akkaHttpV % Test,
    "org.scalatest" %% "scalatest" % "2.2.5" % Test,
    "junit" % "junit" % "4.12" % Test
  )
)

resolvers += Resolver.sonatypeRepo("releases")

resolvers += Resolver.jcenterRepo

testOptions in Test += Tests.Argument("junitxml")

parallelExecution in Test := false

fork in Test := true

ivyScala := ivyScala.value map {
  _.copy(overrideScalaVersion = true)
}