package service

import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.{Chunk, ZIO, ZLayer}
trait ReviewRepository {
  def save(reviews: Chunk[AmazonReview]): ZIO[Any, Throwable, Unit]
}

object ReviewRepository {
  def save(
      reviews: Chunk[AmazonReview]
  ): ZIO[ReviewRepository, Throwable, Unit] =
    ZIO.environmentWithZIO[ReviewRepository](_.get.save(reviews))

  val live: ZLayer[Quill.Postgres[SnakeCase], Nothing, ReviewRepository] =
    ZLayer.fromFunction(new Live(_))

  class Live(quill: Quill.Postgres[SnakeCase]) extends ReviewRepository {
    import quill.*

    override def save(
        reviews: Chunk[AmazonReview]
    ): ZIO[Any, Throwable, Unit] = {
      val q = quote {
        liftQuery(reviews).foreach(e => query[AmazonReview].insertValue(e))
      }
      run(q, 4).unit
    }
  }
}
