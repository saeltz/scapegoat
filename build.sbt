// compiler plugins
addCompilerPlugin("org.scalameta" % "semanticdb-scalac" % "4.3.14" cross CrossVersion.full)

name := "scalac-scapegoat-plugin"
organization := "com.sksamuel.scapegoat"
description := "Scala compiler plugin for static code analysis"
homepage := Some(url("https://github.com/sksamuel/scapegoat"))
licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/sksamuel/scapegoat"),
    "scm:git@github.com:sksamuel/scapegoat.git",
    Some("scm:git@github.com:sksamuel/scapegoat.git")
  )
)
developers := List(
  Developer(
    "sksamuel",
    "sksamuel",
    "@sksamuel",
    url("https://github.com/sksamuel")
  )
)

scalaVersion := "2.13.2"
crossScalaVersions := Seq("2.11.12", "2.12.10", "2.12.11", "2.13.1", "2.13.2")
autoScalaLibrary := false
crossVersion := CrossVersion.full
crossTarget := {
  // workaround for https://github.com/sbt/sbt/issues/5097
  target.value / s"scala-${scalaVersion.value}"
}

// https://github.com/sksamuel/scapegoat/issues/298
ThisBuild / useCoursier := false

val scalac13Options = Seq(
  "-Xlint:adapted-args",
  "-Xlint:inaccessible",
  "-Xlint:infer-any",
  "-Xlint:nullary-override",
  "-Xlint:nullary-unit",
  "-Yrangepos",
  "-Ywarn-unused"
)
val scalac12Options = Seq(
  "-Xlint:adapted-args",
  "-Ywarn-inaccessible",
  "-Ywarn-infer-any",
  "-Xlint:nullary-override",
  "-Xlint:nullary-unit",
  "-Xmax-classfile-name",
  "254"
)
val scalac11Options = Seq(
  "-Ywarn-adapted-args",
  "-Ywarn-inaccessible",
  "-Ywarn-infer-any",
  "-Ywarn-nullary-override",
  "-Ywarn-dead-code",
  "-Ywarn-nullary-unit",
  "-Ywarn-numeric-widen",
  "-Xmax-classfile-name",
  "254"
  //"-Ywarn-value-discard"
)
scalacOptions := {
  val common = Seq(
    "-unchecked",
    "-deprecation",
    "-feature",
    "-encoding",
    "utf8",
    "-Xlint"
  )
  common ++ (scalaBinaryVersion.value match {
    case "2.11" => scalac11Options
    case "2.12" => scalac12Options
    case "2.13" => scalac13Options
  })
}
javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

// because that's where "PluginRunner" is
fullClasspath in console in Compile ++= (fullClasspath in Test).value
initialCommands in console := s"""
import com.sksamuel.scapegoat._
def check(code: String) = {
  val runner = new PluginRunner { val inspections = ScapegoatConfig.inspections }
  // Not sufficient for reuse, not sure why.
  // runner.reporter.reset
  val c = runner compileCodeSnippet code
  val feedback = c.scapegoat.feedback
  feedback.warnings map (x => "%-40s %s %s".format(x.text, x.explanation, x.snippet.getOrElse(""))) foreach println
  feedback
}
"""

libraryDependencies ++= Seq(
  "org.scala-lang"         % "scala-reflect"  % scalaVersion.value % "provided",
  "org.scala-lang"         % "scala-compiler" % scalaVersion.value % "provided",
  "org.scala-lang.modules" %% "scala-xml"     % "1.3.0" excludeAll (
    ExclusionRule(organization = "org.scala-lang")
  ),
  "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.6" excludeAll (
    ExclusionRule(organization = "org.scala-lang")
  ),
  "org.scala-lang" % "scala-compiler" % scalaVersion.value % "test",
  "commons-io"     % "commons-io"     % "2.7"              % "test",
  "org.scalatest"  %% "scalatest"     % "3.1.2"            % "test",
  "org.mockito"    % "mockito-all"    % "1.10.19"          % "test",
  "joda-time"      % "joda-time"      % "2.10.6"           % "test",
  "org.joda"       % "joda-convert"   % "2.2.1"            % "test",
  "org.slf4j"      % "slf4j-api"      % "1.7.30"           % "test"
)

// Test
fork in (Test, run) := true
logBuffered in Test := false
parallelExecution in Test := false

// ScalaTest reporter config:
// -o - standard output,
// D - show all durations,
// T - show reminder of failed and cancelled tests with short stack traces,
// F - show full stack traces.
testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oDTF")

// Assembly
// include the scala xml and compat modules into the final jar, shaded
assemblyShadeRules in assembly := Seq(
  ShadeRule.rename("scala.xml.**" -> "scapegoat.xml.@1").inAll,
  ShadeRule.rename("scala.collection.compat.**" -> "scapegoat.compat.@1").inAll
)
packageBin in Compile := crossTarget.value / (assemblyJarName in assembly).value
makePom := makePom.dependsOn(assembly).value
test in assembly := {} // do not run tests during assembly
publishArtifact in Test := false

// Scalafix
scalafixDependencies in ThisBuild += "com.nequissimus" %% "sort-imports" % "0.3.1"
addCommandAlias("fix", "all compile:scalafix test:scalafix; fixImports")
addCommandAlias("fixImports", "compile:scalafix SortImports; test:scalafix SortImports")
addCommandAlias("fixCheck", "compile:scalafix --check; test:scalafix --check; fixCheckImports")
addCommandAlias("fixCheckImports", "compile:scalafix --check SortImports; test:scalafix --check SortImports")

// Scalafmt
scalafmtOnCompile in ThisBuild :=
  sys.env
    .get("GITHUB_ACTIONS")
    .forall(_.toLowerCase == "false")
