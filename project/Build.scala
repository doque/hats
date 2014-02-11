import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val name = "SixThinkingHats"
    val version = "0.1-SNAPSHOT"
    
    val appDependencies = Seq(
    	javaCore,
      	"com.feth" %% "play-easymail" % "0.5-SNAPSHOT",
        //"ws.wamplay" %% "wamplay" % "0.2.6-SNAPSHOT", // exclude("org.scala-stm", "scala-stm_2.10.0")
        "com.google.guava" % "guava" % "14.0"
    )

    val main = play.Project(name, version, appDependencies).settings(
       resolvers += Resolver.url("play-easymail (release)", url("http://joscha.github.com/play-easymail/repo/releases/"))(Resolver.ivyStylePatterns)
      ,resolvers += Resolver.url("play-easymail (snapshot)", url("http://joscha.github.com/play-easymail/repo/snapshots/"))(Resolver.ivyStylePatterns)
      //resolvers ++= Seq("Bo's Repo" at "http://blopker.github.com/maven-repo/")
      //,resolvers += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)
    )
}
