package core

import org.specs2.mutable.SpecificationLike
import akka.actor.{Props, ActorSystem}
import akka.routing.RoundRobinRouter
import domain.Tweet
import java.util.Date
import core.TweetReaderActor.CountAll
import scala.concurrent.Await
import akka.util.Timeout
import org.specs2.execute.Result

class TweetActorsPerformanceSpec extends SpecificationLike
  with TestCassandraCluster with CleanCassandra {
  sequential

  lazy val system: ActorSystem = ActorSystem()
  val writer = system.actorOf(Props(new TweetWriterActor(cluster)).withRouter(RoundRobinRouter(nrOfInstances = 50)))
  val reader = system.actorOf(Props(new TweetReaderActor(cluster)))
  import akka.pattern.ask
  import scala.concurrent.duration._

  "Fast writes" >> {
    def read: Long = {
      val duration = 10000.milli
      Await.result(reader.ask(CountAll)(Timeout(duration)).mapTo[Long], duration)
    }
    def write(count: Int, seconds: Int): Result = {
      (1 to count).foreach(id => writer ! Tweet(id.toString, "@honzam399", "Yay!", new Date))
      Thread.sleep(seconds * 1000)
      read mustEqual count
    }

    "1k writes under 1s" in {
      write(count = 1000, seconds = 1)
    }

    "10k writes under 1s" in {
      write(count = 10000, seconds = 1)
    }

    "250k writes under 10s" in {
      write(count = 250000, seconds = 10)
    }

    "1M writes under 25s" in {
      write(count = 1000000, seconds = 25)
    }

  }

}
