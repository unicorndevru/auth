import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import sbt.Keys._

import scalariform.formatter.preferences._

name := "auth"

val authV = "0.4.0"

val akkaV = "2.4.6"

val reactiveMongoVersion = "0.11.9"

scalaVersion := "2.11.8"

val gitHeadCommitSha = settingKey[String]("current git commit SHA")

val commonScalariform = scalariformSettings :+ (ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignParameters, true)
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(PreserveSpaceBeforeArguments, true)
  .setPreference(RewriteArrowSymbols, true))

val commons = Seq(
  organization := "ru.unicorndev",
  scalaVersion := "2.11.8",
  resolvers ++= Seq(
    "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
    "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
    Resolver.sonatypeRepo("snapshots"),
    Resolver.bintrayRepo("alari", "generic")
  ),
  gitHeadCommitSha in ThisBuild := Process("git rev-parse --short HEAD").lines.head,
  licenses +=("MIT", url("http://opensource.org/licenses/MIT")),
  bintrayPackageLabels := Seq("scala", "akka-http", "auth", "security", "api"),
  bintrayRepository := "generic",
  version := authV + "." + gitHeadCommitSha.value
) ++ commonScalariform

commons

lazy val `auth` = (project in file("."))
  .dependsOn(`auth-testkit`)
  .aggregate(`auth-testkit`)

lazy val `auth-akka` = (project in file("akka")).settings(commons: _*).settings(
  name := "auth-akka",
  version := authV + "." + gitHeadCommitSha.value,
  libraryDependencies ++= Seq(
    "com.pauldijou" %% "jwt-play-json" % "0.7.1",
    "org.mindrot" % "jbcrypt" % "0.3m",
    "commons-validator" % "commons-validator" % "1.5.0",
    "com.typesafe.akka" %% "akka-slf4j" % akkaV,
    "ru.unicorndev" %% "utils-http" % "0.2.21deacd"
  )
)

lazy val `auth-mongo` = (project in file("auth-mongo")).settings(commons: _*).settings(
  name := "auth-mongo",
  version := authV + "." + gitHeadCommitSha.value,
  libraryDependencies ++= Seq(
    "org.reactivemongo" %% "reactivemongo" % reactiveMongoVersion
  )
).dependsOn(`auth-akka`).aggregate(`auth-akka`)

lazy val `auth-testkit` = (project in file("auth-testkit")).settings(commons: _*).settings(
  name := "auth-testkit",
  version := authV + "." + gitHeadCommitSha.value,
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-http-testkit" % akkaV,
    "org.scalatest" %% "scalatest" % "2.2.5",
    "junit" % "junit" % "4.12",
    "de.flapdoodle.embed" % "de.flapdoodle.embed.mongo" % "1.50.2" % Test,
    "org.reactivemongo" %% "reactivemongo" % reactiveMongoVersion % Test
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