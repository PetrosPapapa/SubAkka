import Dependencies._

ThisBuild / scalaVersion     := "2.12.10"
ThisBuild / version          := "0.1"
ThisBuild / organization     := "uk.ac.ed.inf"

ThisBuild / autoAPIMappings  := true

githubOwner := "PetrosPapapa"
githubRepository := "SubAkka"
publishMavenStyle := true

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
