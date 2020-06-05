import com.lightbend.cinnamon.sbt.Cinnamon.CinnamonKeys._
import com.typesafe.sbteclipse.core.EclipsePlugin.EclipseKeys
import sbt.Keys._
import sbt._

object CommonSettings {
  import sbtstudent.AdditionalSettings

  lazy val commonSettings = Seq(
    organization := "com.lightbend.training",
    version := "1.0.0",
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-v"),
    EclipseKeys.eclipseOutput := Some(".target"),
    EclipseKeys.withSource := true,
    EclipseKeys.skipParents in ThisBuild := true,
    EclipseKeys.skipProject := true,
    parallelExecution in GlobalScope := false,
    logBuffered in Test := false,
    parallelExecution in ThisBuild := false,
    cinnamon in run := true,
    cinnamonLogLevel := "INFO",
    libraryDependencies ++= Dependencies.dependencies
  ) ++
    AdditionalSettings.initialCmdsConsole ++
    AdditionalSettings.initialCmdsTestConsole ++
    AdditionalSettings.cmdAliases
}
