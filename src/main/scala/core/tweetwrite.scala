package core

import akka.actor.Actor
import domain.Tweet
import com.datastax.driver.core.{BoundStatement, Session, Cluster}

object TweetWriterActor {

}

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

/*
  doQuery(QB.update(ColumnFamilies.tweets)
    .`with`(QB.set("user_user", tweet.user.user))
    .and(QB.set("text", tweet.text.text))
    .and(QB.set("createdat", tweet.createdAt))
    .where(QB.eq("key", tweet.id.id)))
*/
