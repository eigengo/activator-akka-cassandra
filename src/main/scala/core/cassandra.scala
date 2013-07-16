package core

import com.datastax.driver.core._
import scalaz._
import Scalaz._
import scalaz.Validation.fromTryCatch
import iteratee._
import Iteratee._
import language.implicitConversions
import scala.collection.JavaConversions._
import scala.reflect.runtime.universe.{ Bind => ReflectBind, _ }
import spray.json._

case class ErrorMessage(message: String) extends AnyVal
object ErrorMessage {
  implicit def toErrorMessage(message: String): ErrorMessage = ErrorMessage(message)
}

case class NotInserted(message: ErrorMessage)
case class Inserted[A](entity: A)

case class UpdateEntity[A, B](id: A, entity: B)
case class Updated[A](entity: A)
case class NotUpdated(message: String)

trait CassandraImplicits {

  implicit def asScalaStream(iter: java.util.Iterator[Row]): Stream[Row] = {
    asScalaIterator(iter).toStream
  }

  implicit def asScalaStreamRS(rs: ResultSet): Stream[Row] = {
    asScalaIterator(rs.iterator()).toStream
  }

  implicit def asQueryList(q: Query): List[Query] = List(q)

  //We do this because Cassandra's QueryBuilder wants column's name
  //to be passed as Array[String]
  implicit def asStringArray(s: List[String]): Array[String] = s.toArray

  //This is a necessary evil in case we want to use the underlying
  //Java API, in particular cfr. method values(...)
  def asObjArray(s: Any*): Array[Object] = s.map(_.asInstanceOf[Object]).toArray

  implicit class RichRow(obj: Row) {

    def getAs[A : RootJsonFormat : TypeTag](columnName: String) : A = {
      val format = implicitly[RootJsonFormat[A]]
      val tt = typeTag[A]

      fromTryCatch(obj.getString(columnName)).toOption match {
        case None =>
          val msg = "getAs[" + tt.tpe + "]: Cassandra Row was empty."
          throw new IllegalArgumentException(msg)
        case Some(json) =>
          fromTryCatch(format.read(json.asJson)).toOption match {
            case None =>
              val msg = "getAs[" + tt.tpe + "]: Found string was not convertible to a valid case class."
              throw new IllegalArgumentException(msg)
            case Some(v) => v
          }
      }
    }

    def getAsOption[A : RootJsonFormat : TypeTag](columnName: String) : Option[A] = {
      fromTryCatch(getAs(columnName)).toOption
    }

  }

  def from[A : RootJsonFormat](obj: A): String = {
    val format = implicitly[RootJsonFormat[A]]
    format.write(obj).compactPrint
  }

}

/* Generally speaking this trait focuses more on the ease of use than
 * on "purity". In theory, we should enumerate directly an ``Iterator`` without
 * converting it into a stream, but doing so would force our entire code to
 * live in the IO context. Using "enumerate" allow us to run pure iteratees
 * in the Id.
 */
trait CassandraIteratees {

  //Implicitly call "collect" under the hood,
  //collecting value trasformed by @f.
  def gather[A,B](f: A => B): IterateeT[A, Id, List[B]] = collect[B, List] %= map(f)

  implicit class RichIterateeT[E, F[_], A](obj: IterateeT[E, F, A]) {
    def runEither(implicit m: Monad[F]) = {
      fromTryCatch(obj.run(m)) match {
        case Success(r) => Right(r)
        case Failure(e) => Left(e.getMessage)
      }
    }
  }

  //Run the enumerated queries, yielding a list of @ResultSet.
  def execute(implicit session: Session): Iteratee[Query, List[ResultSet]] = {
     def step(acc: List[ResultSet])(s: Input[Query]): Iteratee[Query, List[ResultSet]] =
       s(el = e => cont(step(acc :+ session.execute(e))),
         empty = cont(step(acc)),
         eof = done(acc, eofInput[Query])
       )
     cont(step(List()))
  }

  def rows: Iteratee[ResultSet, List[List[Row]]] = {
     def step(acc: List[List[Row]])(s: Input[ResultSet]): Iteratee[ResultSet, List[List[Row]]] =
       s(el = e => {
         val rows:List[Row] = asScalaIterator(e.iterator()).toList
         cont(step(acc :+ rows))},
         empty = cont(step(acc)),
         eof = done(acc, eofInput[ResultSet])
       )
     cont(step(List()))
  }

}

trait CassandraEnumeratees {
  
  def toQuery: EnumerateeT[String, Query, Id] = map(new SimpleStatement(_))

  def toRows: EnumerateeT[ResultSet, Stream[Row], Id] =
    map((rs: ResultSet) => asScalaIterator(rs.iterator()).toStream)

  def toRS(session: Session): EnumerateeT[Query, ResultSet, Id] = map(session.execute(_))

}

trait CassandraEnumerators {

  implicit class RichEnumeratorT[E, F[_]](enum: EnumeratorT[E, F]) {

    //Flipped version of "&=". It allows us to chain iteratees and enumerator
    //in a pipeline fashion.
    def =&[A](i: IterateeT[E, F, A])(implicit F: Bind[F]): IterateeT[E, F, A] = {
      i &= enum
    }
  }

  def enumRS(rs: ResultSet): EnumeratorT[Row, Id] =
    enumerate(asScalaIterator(rs.iterator()).toStream)

  //Handle with care. Enumerating a ResultSetFuture will cause
  //the future to block and wait for the result.
  def enumRS(rsf: ResultSetFuture): EnumeratorT[Row, Id] =
    enumerate(asScalaIterator(rsf.get().iterator()).toStream)

  def enumRSList(rsl: List[ResultSet]): EnumeratorT[Row, Id] =
    enumList(rsl.flatMap(_.all()))

  def enumQueries(queries: List[Query]): EnumeratorT[Query, Id] =
    enumList(queries)

  def enumRawQueries(queries: List[String]): EnumeratorT[String, Id] =
    enumList(queries)
}

trait CassandraCrud extends CassandraSession with CassandraPipes {

  //Syntactic sugar for a recurring pattern: enumQueries(q)
  def doQuery(query: Query) =
    enumRS(session.execute(query))

  def doQuery[A](query: Query, expected: A) = {
    fromTryCatch(enumRS(session.execute(query))).toEither match {
      case Right(_) => Right(Inserted[A](expected))
      case Left(e) => Left(NotInserted("%s failed: %s" format(query, e.getMessage)))
    }
  }

  def getOne[T](f: Row => T)(q: Query): Either[ErrorMessage, T] = {
    fromTryCatch(f(session.execute(q).one())).toEither match {
      case Right(v) => Right(v)
      case Left(e) => Left(e.getMessage)
    }
  }

  def getOneOption[T](f: Row => T)(q: Query): Either[ErrorMessage, Option[T]] = {
    fromTryCatch(f(session.execute(q).one())).toEither match {
      case Right(v) => Right(Some(v))
      case Left(e) => Left(e.getMessage)
    }
  }
}

trait CassandraSession {
  def session: Session
}

trait CassandraPipes extends CassandraImplicits
  with CassandraIteratees
  with CassandraEnumeratees
  with CassandraEnumerators
