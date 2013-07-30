package core

import akka.actor.ActorSystem
import org.specs2.mutable.SpecificationLike
import akka.testkit.{TestActorRef, TestKit, ImplicitSender}
import core.TweetReaderActor.FindAll
import domain.Tweet

class TweetScanActorSpec extends TestKit(ActorSystem())
  with SpecificationLike with ImplicitSender with CleanCassandra with TestCassandraCluster {

  sequential

  val port = 12345
  def testQueryUrl(query: String) = s"http://localhost:$port/q=$query"

  val tweetRead  = TestActorRef(new TweetReaderActor(cluster))
  val tweetWrite = TestActorRef(new TweetWriterActor(cluster))
  val tweetScan  = TestActorRef(new TweetScannerActor(tweetWrite, testQueryUrl))

  "Getting all 'typesafe' tweets" >> {

    "should return more than 10 last entries" in {
      val twitterApi = TwitterApi(port)
      tweetScan ! "typesafe"
      Thread.sleep(1000)
      tweetRead ! FindAll(100)
      val tweets = expectMsgType[List[Tweet]]
      tweets.size mustEqual 4
      twitterApi.stop()
      success
    }
  }
}
