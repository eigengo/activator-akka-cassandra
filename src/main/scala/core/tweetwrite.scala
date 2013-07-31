package core

import akka.actor.Actor
import domain.Tweet
import com.datastax.driver.core.{BoundStatement, Cluster}
import scala.util.Success

class TweetWriterActor(cluster: Cluster) extends Actor {
  val session = cluster.connect(Keyspaces.akkaCassandra)
  val preparedStatement = session.prepare("INSERT INTO tweets(key, user_user, text, createdat) VALUES (?, ?, ?, ?);")

  def saveTweet(tweet: Tweet): Unit =
    session.executeAsync(preparedStatement.bind(tweet.id.id, tweet.user.user, tweet.text.text, tweet.createdAt))

  def receive: Receive = {
    case tweets: List[Tweet] => tweets.foreach(saveTweet)
    case tweet: Tweet        => saveTweet(tweet)
  }
}
