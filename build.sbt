import Dependencies._

ThisBuild / scalaVersion     := "2.12.6"
ThisBuild / version          := "0.1-SNAPSHOT"
ThisBuild / organization     := "uk.ac.ed.inf"

ThisBuild / autoAPIMappings  := true

lazy val root = (project in file("."))
  .settings(
    name := "SubAkka",
    libraryDependencies += akkaActor,
    libraryDependencies += akkaStream,
    libraryDependencies += scalaTest % Test,
    libraryDependencies += scalaCheck % Test,
    libraryDependencies += akkaTest % Test
  )
