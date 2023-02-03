package service

import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.{ZIO, ZLayer}
trait ReviewRepository {
  def save(review: AmazonReview): ZIO[Any, Throwable, Unit]
}

object ReviewRepository {
  def save(review: AmazonReview): ZIO[ReviewRepository, Throwable, Unit] =
    ZIO.environmentWithZIO[ReviewRepository](_.get.save(review))

  val live: ZLayer[Quill.Postgres[SnakeCase], Nothing, ReviewRepository] =
    ZLayer.fromFunction(new Live(_))

  class Live(quill: Quill.Postgres[SnakeCase]) extends ReviewRepository {
    import quill.*

    override def save(review: AmazonReview): ZIO[Any, Throwable, Unit] =
      run(quote(query[AmazonReview].insertValue(lift(review)))).unit
  }
}
