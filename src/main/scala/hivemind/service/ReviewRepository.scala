package hivemind.service

import hivemind.http.BestRatedResponse
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.{Chunk, ZIO, ZLayer}

/** Able to perform queries on 'amazon_review' table in PostgreSQL. */
trait ReviewRepository {

  /** Saves amazon reviews to the table. */
  def save(reviews: Chunk[AmazonReview]): ZIO[Any, Throwable, Unit]

  /** @param start
    *   start of the date range in UTC
    * @param end
    *   end of the date range in UTC
    * @param limit
    *   maximum number of products to return
    * @param minReviews
    *   only consider products that have minReviews number of reviews
    * @return
    *   list of best rated products
    */
  def getBestRated(start: Long, end: Long, limit: Int, minReviews: Int): ZIO[Any, Throwable, List[BestRatedResponse]]

  /** Deletes every row from the table. */
  def cleanup(): ZIO[Any, Throwable, Unit]
}

object ReviewRepository {
  def cleanup(): ZIO[ReviewRepository, Throwable, Unit] =
    ZIO.environmentWithZIO[ReviewRepository](_.get.cleanup())

  def save(reviews: Chunk[AmazonReview]): ZIO[ReviewRepository, Throwable, Unit] =
    ZIO.environmentWithZIO[ReviewRepository](_.get.save(reviews))

  val live: ZLayer[Quill.Postgres[SnakeCase], Nothing, ReviewRepository] =
    ZLayer.fromFunction(new Live(_))

  private class Live(quill: Quill.Postgres[SnakeCase]) extends ReviewRepository {
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
          .filter(r => r.unixReviewTime >= lift(start) && r.unixReviewTime <= lift(end))
          .groupByMap(r => r.asin)(r => (r.asin, count(r.overall), avg(r.overall)))
          .filter(r => r._2 >= lift(minReviews))
          .sortBy(r => r._3)(Ord.desc)
          .take(lift(limit))
          .map(r => BestRatedResponse(r._1, r._3))
      }
      run(q)
    }

    override def cleanup(): ZIO[Any, Throwable, Unit] = {
      val q = quote { query[AmazonReview].delete }
      run(q).unit
    }
  }
}
