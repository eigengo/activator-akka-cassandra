package core

import com.datastax.driver.core.{ResultSet, ResultSetFuture}
import scala.concurrent.{CanAwait, Future, ExecutionContext}
import scala.util.{Success, Try}
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

private[core] case class ExecutionContextExecutor(executonContext: ExecutionContext) extends java.util.concurrent.Executor {
  def execute(command: Runnable): Unit = { executonContext.execute(command) }
}

class RichResultSetFuture(resultSetFuture: ResultSetFuture) extends Future[ResultSet] {
  @throws(classOf[InterruptedException])
  @throws(classOf[scala.concurrent.TimeoutException])
  def ready(atMost: Duration)(implicit permit: CanAwait): this.type = {
    resultSetFuture.get(atMost.toMillis, TimeUnit.MILLISECONDS)
    this
  }

  @throws(classOf[Exception])
  def result(atMost: Duration)(implicit permit: CanAwait): ResultSet = {
    resultSetFuture.get(atMost.toMillis, TimeUnit.MILLISECONDS)
  }

  def onComplete[U](func: (Try[ResultSet]) => U)(implicit executionContext: ExecutionContext): Unit = {
    if (resultSetFuture.isDone) {
      func(Success(resultSetFuture.getUninterruptibly))
    } else {
      resultSetFuture.addListener(new Runnable {
        def run() {
          func(Success(resultSetFuture.get()))
        }
      }, ExecutionContextExecutor(executionContext))
    }
  }

  def isCompleted: Boolean = resultSetFuture.isDone

  def value: Option[Try[ResultSet]] = if (resultSetFuture.isDone) Some(Success(resultSetFuture.get())) else None
}

object RichResultSetFuture {

  implicit def toFuture(resultSetFuture: ResultSetFuture): RichResultSetFuture = new RichResultSetFuture(resultSetFuture)

}