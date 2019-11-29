// (5) shadow sbt-scalajs' crossProject and CrossType until Scala.js 1.0.0 is released
import sbtcrossproject.{crossProject, CrossType}

lazy val scalaV = "2.12.9"
lazy val jQueryV = "3.4.1"
lazy val semanticV = "2.4.1"

lazy val server = (project in file("server")).settings(
  scalaVersion := scalaV,
  scalaJSProjects := Seq(client),
  pipelineStages in Assets := Seq(scalaJSPipeline),
  pipelineStages := Seq(digest, gzip),
  // triggers scalaJSPipeline when using compile or continuous compilation
  compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
  libraryDependencies ++= Seq(
    "com.vmunier" %% "scalajs-scripts" % "1.1.4",
    guice,
    filters,
    ws
    // webjars for Semantic-UI
    , "org.webjars" %% "webjars-play" % "2.7.3"
    , "org.webjars" % "Semantic-UI" % semanticV
    , "org.webjars" % "jquery" % jQueryV
    ,
    "com.typesafe.akka" %% "akka-testkit" % "2.5.6" % Test,
    "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.6" % Test,
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
    "org.awaitility" % "awaitility" % "3.0.0" % Test
  ),
  // to have routing also in ScalaJS
  // Create a map of versioned assets, replacing the empty versioned.js
  DigestKeys.indexPath := Some("javascripts/versioned.js"),
  // Assign the asset index to a global versioned var
  DigestKeys.indexWriter ~= { writer => index => s"var versioned = ${writer(index)};" }

).enablePlugins(PlayScala)
  .dependsOn(sharedJvm)

lazy val client = (project in file("client")).settings(
  scalaVersion := scalaV,
  scalaJSUseMainModuleInitializer := true,
  scalacOptions ++= Seq("-Xmax-classfile-name","78"),
  scalaJSUseMainModuleInitializer in Test := false,
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
  jsDependencies ++= Seq(
    "org.webjars" % "jquery" % jQueryV / "jquery.js" minified "jquery.min.js",
    "org.webjars" % "Semantic-UI" % semanticV / "semantic.js" minified "semantic.min.js" dependsOn "jquery.js"
  ),
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.9.7",
    "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
    "com.typesafe.play" %%% "play-json" % "2.7.3",
    "com.thoughtworks.binding" %%% "dom" % "11.9.0",
    "com.thoughtworks.binding" %%% "futurebinding" % "11.9.0",
    "fr.hmil" %%% "roshttp" % "2.0.2",
    // java.time supprot for ScalaJS
    //"org.scala-js" %%% "scalajs-java-time" % "0.2.6",
    "io.github.cquiroz" %%% "scala-java-time-tzdb" % "2.0.0-M13_2018c",
    // jquery support for ScalaJS
    "be.doeraene" %%% "scalajs-jquery" % "0.9.5"
  )
).enablePlugins(ScalaJSWeb).
  dependsOn(sharedJs)

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .settings(scalaVersion := scalaV
    , libraryDependencies ++= Seq(
      "org.julienrf" %%% "play-json-derived-codecs" % "6.0.0",
      "io.github.cquiroz" %%% "scala-java-time" % "2.0.0-M13",
      // logging lib that also works with ScalaJS
      "biz.enef" %%% "slogging" % "0.6.0"
    ))
  .jsSettings(/* ... */) // defined in sbt-scalajs-crossproject
  .jvmSettings(
    libraryDependencies ++= Seq(
            "org.scala-js" %% "scalajs-stubs" % scalaJSVersion % "provided")
  )
  .jsConfigure(_ enablePlugins ScalaJSWeb)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

// loads the server project at sbt startup
//onLoad in Global := (Command.process("project server", _: State)) compose (onLoad in Global).value
onLoad in Global := (onLoad in Global).value andThen { s: State =>
    "project server" :: s
}
