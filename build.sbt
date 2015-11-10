import com.typesafe.sbt.SbtScalariform.ScalariformKeys

import sbt.Keys._

import scalariform.formatter.preferences._

name := "auth-gather"

val authV = "0.2.4"

val expanderV = "0.3.3"

val reactiveMongoVersion = "0.11.7.play24"

scalaVersion := "2.11.7"

val commonScalariform = scalariformSettings :+ (ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignParameters, true)
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(PreserveSpaceBeforeArguments, true)
  .setPreference(RewriteArrowSymbols, true))

val commons = Seq(
  organization := "me.passenger",
  scalaVersion := "2.11.7",
  resolvers ++= Seq(
    "dgtl" at "http://dev.dgtl.pro/repo/",
    "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
    "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
    Resolver.sonatypeRepo("snapshots")
  ),
  publishTo := Some(Resolver.file("file", new File("/mvn-repo")))
) ++ commonScalariform

commons

lazy val `psgr-auth-gather` = (project in file("."))
  .dependsOn(`psgr-auth`)
  .aggregate(`psgr-auth`)

lazy val `psgr-auth` = (project in file("core")).settings(commons: _*).settings(
  name := "auth",
  version := authV,
  libraryDependencies ++= Seq(
    "com.github.t3hnar" %% "scala-bcrypt" % "2.4",
    "org.reactivemongo" %% "reactivemongo-extensions-bson" % reactiveMongoVersion % Provided,
    "org.reactivemongo" %% "play2-reactivemongo" % reactiveMongoVersion % Provided,
    "me.passenger" %% "eventbus" % "0.2.0",
    ws % Provided,
    json % Provided,
    specs2 % Test
  )
).enablePlugins(PlayScala)
  .disablePlugins(PlayLayoutPlugin)
  .dependsOn(`psgr-auth-actions`, `psgr-auth-protocol`)
  .aggregate(`psgr-auth-actions`, `psgr-auth-protocol`)

lazy val `psgr-auth-actions` = (project in file("actions")).settings(commons: _*).settings(
  name := "auth-actions",
  version := authV,
  libraryDependencies ++= Seq(
    "com.pauldijou" %% "jwt-play" % "0.4.0",
    "net.ceedubs" %% "ficus" % "1.1.2",
    "me.passenger" %% "failures" % "0.1.0"
  )
).enablePlugins(PlayScala)
  .disablePlugins(PlayLayoutPlugin)
  .dependsOn(`psgr-auth-protocol`)
  .aggregate(`psgr-auth-protocol`)

lazy val `psgr-auth-protocol` = (project in file("protocol")).settings(commons: _*).settings(
  name := "auth-protocol",
  version := authV,
  libraryDependencies ++= Seq(
    "me.passenger" %% "expander-protocol" % expanderV,
    json % Provided
  )
)

resolvers += Resolver.sonatypeRepo("releases")

resolvers += Resolver.jcenterRepo

testOptions in Test += Tests.Argument("junitxml")

javaOptions in Test += "-Dconfig.file=conf/test.conf"

parallelExecution in Test := false

fork in Test := true

ivyScala := ivyScala.value map {
  _.copy(overrideScalaVersion = true)
}