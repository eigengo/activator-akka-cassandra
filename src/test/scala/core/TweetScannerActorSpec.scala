package core

import akka.actor.ActorSystem
import org.specs2.mutable.SpecificationLike
import akka.testkit.{TestActorRef, TestKit, ImplicitSender}
import core.TweetReaderActor.FindAll
import domain.Tweet

class TweetScannerActorSpec extends TestKit(ActorSystem())
  with SpecificationLike with ImplicitSender {

  sequential

  val port = 12345
  def testQueryUrl(query: String) = s"http://localhost:$port/q=$query"

  val tweetScan  = TestActorRef(new TweetScannerActor(testActor, testQueryUrl))

  "Getting all 'typesafe' tweets" >> {

    "should return more than 10 last entries" in {
      val twitterApi = TwitterApi(port)
      tweetScan ! "typesafe"
      Thread.sleep(1000)
      val tweets = expectMsgType[List[Tweet]]
      tweets.size mustEqual 4
      twitterApi.stop()
      success
    }
  }
}
