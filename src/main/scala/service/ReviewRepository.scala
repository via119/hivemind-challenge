package service

import http.BestRatedResponse
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.{Chunk, ZIO, ZLayer}
trait ReviewRepository {
  def save(reviews: Chunk[AmazonReview]): ZIO[Any, Throwable, Unit]
  def getBestRated(start: Long, end: Long, limit: Int, minReviews: Int): ZIO[Any, Throwable, List[BestRatedResponse]]
}

object ReviewRepository {
  def save(
      reviews: Chunk[AmazonReview]
  ): ZIO[ReviewRepository, Throwable, Unit] =
    ZIO.environmentWithZIO[ReviewRepository](_.get.save(reviews))

  def getBestRated(
      start: Long,
      end: Long,
      limit: Int,
      minReviews: Int
  ): ZIO[ReviewRepository, Throwable, List[BestRatedResponse]] =
    ZIO.environmentWithZIO[ReviewRepository](_.get.getBestRated(start, end, limit, minReviews))

  val live: ZLayer[Quill.Postgres[SnakeCase], Nothing, ReviewRepository] =
    ZLayer.fromFunction(new Live(_))

  class Live(quill: Quill.Postgres[SnakeCase]) extends ReviewRepository {
    import quill.*

    override def save(reviews: Chunk[AmazonReview]): ZIO[Any, Throwable, Unit] = {
      val q = quote {
        liftQuery(reviews).foreach(e => query[AmazonReview].insertValue(e))
      }
      run(q, 4).unit
    }

    override def getBestRated(
        start: Long,
        end: Long,
        limit: Int,
        minReviews: Int
    ): ZIO[Any, Throwable, List[BestRatedResponse]] = {
      val q = quote {
        query[AmazonReview]
          .filter(r => r.unixReviewTime > lift(start) && r.unixReviewTime < lift(end))
          .groupByMap(r => r.asin)(r => (r.asin, count(r.overall), avg(r.overall)))
          .filter(r => r._2 >= lift(minReviews))
          .sortBy(r => r._3)(Ord.desc)
          .take(lift(limit))
          .map(r => BestRatedResponse(r._1, r._3))
      }
      run(q)
    }
  }
}
