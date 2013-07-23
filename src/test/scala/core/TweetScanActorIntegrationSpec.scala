package core

import akka.actor.ActorSystem
import org.specs2.mutable.SpecificationLike
import akka.testkit.{TestActorRef, TestKit, ImplicitSender}
import core.TweetReadActor.FindAll
import domain.Tweet

class TweetScanActorIntegrationSpec extends TestKit(ActorSystem())
  with SpecificationLike with ImplicitSender with CleanCassandra with TestCassandraCluster {

  sequential

  def testQueryUrl(query: String) = s"http://twitter-search-proxy.herokuapp.com/search/tweets?q=$query"

  val tweetRead  = TestActorRef(new TweetReadActor(cluster))
  val tweetWrite = TestActorRef(new TweetWriterActor(cluster))
  val tweetScan  = TestActorRef(new TweetScanActor(tweetWrite, testQueryUrl))

  "Getting all real 'typesafe' tweets" >> {

    "should return more than 10 last entries" in {
      tweetScan ! "typesafe"
      Thread.sleep(20000)
      tweetRead ! FindAll
      val tweets = expectMsgType[Either[ErrorMessage, List[Tweet]]].right.get
      println(tweets.size)
      success
    }
  }
}
