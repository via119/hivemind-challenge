package hivemind.service

import hivemind.http.BestRatedResponse
import zio.{Chunk, Ref, ZIO, ZLayer}

trait ReviewRepositoryMock extends ReviewRepository {
  val cleanupCalled: ZIO[Any, Nothing, Boolean]
  val savedReviews: ZIO[Any, Nothing, List[List[AmazonReview]]]
  val bestRatedParameters: ZIO[Any, Nothing, Option[(Long, Long, Int, Int)]]
}

object ReviewRepositoryMock {
  val cleanupCalled: ZIO[ReviewRepositoryMock, Throwable, Boolean] =
    ZIO.environmentWithZIO[ReviewRepositoryMock](_.get.cleanupCalled)

  val savedReviews: ZIO[ReviewRepositoryMock, Throwable, List[List[AmazonReview]]] =
    ZIO.environmentWithZIO[ReviewRepositoryMock](_.get.savedReviews)

  val bestRatedParameters: ZIO[ReviewRepositoryMock, Throwable, Option[(Long, Long, Int, Int)]] =
    ZIO.environmentWithZIO[ReviewRepositoryMock](_.get.bestRatedParameters)

  def make(bestRatedResult: List[BestRatedResponse]): ZLayer[Any, Throwable, ReviewRepositoryMock & ReviewRepository] =
    ZLayer.fromZIO(for {
      cleanupCalledRef <- Ref.make(false)
      savedReviewsRef <- Ref.make(Nil: List[List[AmazonReview]])
      bestRatedParametersRef <- Ref.make(None: Option[(Long, Long, Int, Int)])
    } yield new ReviewRepositoryMock {
      override val cleanupCalled: ZIO[Any, Nothing, Boolean] =
        cleanupCalledRef.get
      override val savedReviews: ZIO[Any, Nothing, List[List[AmazonReview]]] = savedReviewsRef.get
      override val bestRatedParameters: ZIO[Any, Nothing, Option[(Long, Long, Int, Int)]] = bestRatedParametersRef.get

      override def save(reviews: Chunk[AmazonReview]): ZIO[Any, Throwable, Unit] =
        savedReviewsRef.update(reviews.toList :: _)

      override def getBestRated(
          start: Long,
          end: Long,
          limit: Int,
          minReviews: Int
      ): ZIO[Any, Throwable, List[BestRatedResponse]] =
        bestRatedParametersRef.set(Some(start, end, limit, minReviews)) *> ZIO.succeed(bestRatedResult)

      override def cleanup(): ZIO[Any, Throwable, Unit] = cleanupCalledRef.set(true)
    })
}
