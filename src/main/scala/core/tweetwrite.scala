package core

import akka.actor.Actor
import domain.Tweet
import com.datastax.driver.core.{BoundStatement, Cluster}

class TweetWriterActor(cluster: Cluster) extends Actor {
  val session = cluster.connect(Keyspaces.akkaCassandra)
  val preparedStatement = session.prepare("INSERT INTO tweets(key, user_user, text, createdat) VALUES (?, ?, ?, ?);")
  val boundStatement = new BoundStatement(preparedStatement)

  def saveTweet(tweet: Tweet) =
    session.executeAsync(boundStatement.bind(tweet.id.id, tweet.user.user, tweet.text.text, tweet.createdAt))

  def receive: Receive = {
    case tweet: Tweet => saveTweet(tweet)
  }
}
