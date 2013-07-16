package core

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.specs2.mutable.SpecificationLike
import domain.Tweet
import java.util.Date
import core.TweetReadActor.FindAll

class TweetWriterActorSpec extends TestKit(ActorSystem())
  with SpecificationLike with TestCassandraCluster with ImplicitSender {

  val writer = TestActorRef(new TweetWriterActor(cluster))
  val reader = TestActorRef(new TweetReadActor(cluster))

  "Slow & steady" >> {
    "Single tweet" in {
      writer ! Tweet("123", "@honzam399", "Yay!", new Date)
      reader ! FindAll
      val res = expectMsgType[Either[ErrorMessage, List[Tweet]]]
      println(res)
      success
    }
  }
}
