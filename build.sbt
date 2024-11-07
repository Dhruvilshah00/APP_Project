name := """YoutubeLyrics"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.13.15"

resolvers += "Maven Central" at "https://repo1.maven.org/maven2/"
// Add Play framework dependencies
libraryDependencies += guice

// Add the org.json library for JSON handling
libraryDependencies += "org.json" % "json" % "20210307"

libraryDependencies += "com.google.guava" % "guava" % "31.0-jre"  // Example library
