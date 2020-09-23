import Dependencies._

ThisBuild / scalaVersion     := "2.12.12"
ThisBuild / organization     := "uk.ac.ed.inf"

ThisBuild / autoAPIMappings  := true

lazy val root = (project in file("."))
  .settings(
    name := "SubAkka",
    libraryDependencies += akkaActor,
    libraryDependencies += akkaStream,
    libraryDependencies += scalaTest % Test,
    libraryDependencies += scalaCheck % Test,
    libraryDependencies += akkaTest % Test,
    publishArtifact in Test := true
  )

