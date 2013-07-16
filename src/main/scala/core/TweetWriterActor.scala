package core

import akka.actor.Actor
import domain.Tweet
import com.datastax.driver.core.Cluster

object TweetWriterActor {



}

class TweetWriterActor(cluster: Cluster) extends Actor {

  val tweets = cluster.connect("tweets")


  def receive: Receive = {
    case t: Tweet =>

  }
}
