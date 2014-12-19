name := "activator-akka-cassandra"

version := "1.1"

scalaVersion := "2.10.4"

packageArchetype.java_application

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "spray repo" at "http://repo.spray.io"

resolvers += "spray nightlies" at "http://nightlies.spray.io"

libraryDependencies ++= {
  val akkaVersion = "2.3.6"
  val sprayVersion = "1.3.1"
  Seq(
    "com.typesafe.akka"      %% "akka-actor"            % akkaVersion,
    "com.typesafe.akka"      %% "akka-slf4j"            % akkaVersion,
    "io.spray"                % "spray-can"             % sprayVersion,
    "io.spray"                % "spray-client"          % sprayVersion,
    "io.spray"                % "spray-routing"         % sprayVersion,
    "io.spray"               %% "spray-json"            % "1.3.0",
    "com.datastax.cassandra"  % "cassandra-driver-core" % "2.1.1"  exclude("org.xerial.snappy", "snappy-java"),
    "org.xerial.snappy"       % "snappy-java"           % "1.1.1.3",
    "org.scala-lang"          % "scala-reflect"         % "2.10.4",
    "org.specs2"             %% "specs2"                % "2.4.6"        % "test",
    "io.spray"                % "spray-testkit"         % sprayVersion   % "test",
    "com.typesafe.akka"      %% "akka-testkit"          % akkaVersion    % "test",
    "com.novocode"            % "junit-interface"       % "0.11"         % "test->default"
  )
}

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-Ywarn-dead-code",
  "-language:_",
  "-target:jvm-1.7",
  "-encoding", "UTF-8"
)

parallelExecution in Test := false

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")

Revolver.settings : Seq[sbt.Def.Setting[_]]

net.virtualvoid.sbt.graph.Plugin.graphSettings
