package domain

import java.util.Date

case class Handle(author: String) extends AnyVal

case class HashTag(hashTag: String) extends AnyVal

case class Body(body: String) extends AnyVal

case class Tweet(author: Handle, body: Body, timestamp: Date) {

  def hashTags: List[HashTag] = Nil

  def mentions: List[Handle] = Nil

  def replyTo: Option[Handle] = None

}
