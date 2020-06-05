import com.lightbend.cinnamon.sbt.Cinnamon
import sbt._

object Version {
  val akkaVer         = "2.5.19"
  val junitVer        = "0.11"
}

object Dependencies {
  val dependencies = Seq(
    "com.typesafe.akka"       %% "akka-actor"                 % Version.akkaVer withSources(),
    "com.typesafe.akka"       %% "akka-testkit"               % Version.akkaVer withSources(),
    "com.typesafe.akka"       %% "akka-stream"                % Version.akkaVer withSources(),
    "com.typesafe.akka"       %% "akka-stream-testkit"        % Version.akkaVer withSources(),
    Cinnamon.library.cinnamonAkkaStream,
    Cinnamon.library.cinnamonPrometheus,
    Cinnamon.library.cinnamonPrometheusHttpServer,
    "com.novocode"            %  "junit-interface"            % Version.junitVer % Test
  )
}
