package core

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.specs2.mutable.SpecificationLike
import domain.Tweet
import java.util.Date
import core.TweetReadActor.FindAll

class TweetActorsSpec extends TestKit(ActorSystem())
  with SpecificationLike with TestCassandraCluster with CleanCassandra with ImplicitSender {
  sequential

  val writer = TestActorRef(new TweetWriterActor(cluster))
  val reader = TestActorRef(new TweetReadActor(cluster))

  "Slow & steady" >> {
    def write(count: Int): List[Tweet] = {
      val tweets = (1 to count).map(id => Tweet(id.toString, "@honzam399", "Yay!", new Date))
      tweets.foreach(writer !)
      Thread.sleep(1000)    // wait for the tweets to hit the db
      tweets.toList
    }

    "Single tweet" in {
      val tweet = write(1).head

      reader ! FindAll(1)
      val res = expectMsgType[Either[ErrorMessage, List[Tweet]]]
      res mustEqual Right(List(tweet))
    }

    "100 tweets" in {
      val writtenTweets = write(100)

      reader ! FindAll(100)
      val readTweets = expectMsgType[Either[ErrorMessage, List[Tweet]]].right.get
      readTweets must containTheSameElementsAs(writtenTweets)
    }
  }

}
