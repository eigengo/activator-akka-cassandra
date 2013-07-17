package core

import akka.actor.Actor
import domain.Tweet
import com.datastax.driver.core.{Session, Cluster}

object TweetWriterActor {

}

class TweetWriterActor(cluster: Cluster) extends Actor with TwitterWriterActorOperations {
  val session = cluster.connect()

  def receive: Receive = {
    case tweet: Tweet => saveTweet(tweet)
  }
}

private[core] trait TwitterWriterActorOperations extends CassandraCrud {
  import com.datastax.driver.core.querybuilder.{ QueryBuilder => QB }

  implicit def session: Session

  def saveTweet(tweet: Tweet) =
    doQuery(QB.update(Keyspaces.akkaCassandra, ColumnFamilies.tweets)
      .`with`(QB.set("user_user", tweet.user.user))
      .and(QB.set("text", tweet.text.text))
      .and(QB.set("createdAt", tweet.createdAt))
      .where(QB.eq("key", tweet.id.id)))

}