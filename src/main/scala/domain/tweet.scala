package domain

import java.util.Date

case class User(user: String) extends AnyVal
object User {
  implicit def toUser(user: String): User = User(user)
}

case class Text(text: String) extends AnyVal
object Text {
  implicit def toText(text: String): Text = Text(text)
}

case class TweetId(id: String) extends AnyVal
object TweetId {
  implicit def toTweetId(id: String): TweetId = TweetId(id)
}

case class Tweet(id: TweetId, user: User, text: Text, createdAt: Date)