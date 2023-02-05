package hivemind.service

import hivemind.http.{BestRatedRequest, BestRatedResponse}
import hivemind.service.ReviewRepository.{cleanup, save}
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.test.Assertion.approximatelyEquals
import zio.test.{Spec, TestAspect, TestEnvironment, ZIOSpecDefault, assert, assertTrue}
import zio.{Chunk, Runtime, Scope}

import java.time.{LocalDateTime, ZoneOffset}

object BestRatedServiceIntegrationTest extends ZIOSpecDefault {
  val request = BestRatedRequest("01.01.2000", "31.12.2001", limit = 2, minNumberReviews = 2)
  val reviewsOutsideTimeRange = List(
    AmazonReview("p1", 1, getTimestamp("1999-12-31T23:59:59")),
    AmazonReview("p1", 1, getTimestamp("2002-01-01T00:00:00"))
  )
  val relevantReviews = List(
    AmazonReview("p1", 5, getTimestamp("2000-01-01T00:00:00")),
    AmazonReview("p1", 5, getTimestamp("2000-12-31T23:59:59"))
  )
  val notEnoughReviews = List(
    AmazonReview("p2", 5, getTimestamp("2000-04-03T00:00:00"))
  )
  val expectedResponse = List(BestRatedResponse("p1", 5))

  private val quillLayer = Quill.Postgres.fromNamingStrategy(SnakeCase)
  private val dsLayer = Quill.DataSource.fromPrefix("amazonReviewTestDatabaseConfig")

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("BestRatedServiceIntegrationTest")(
      test("gives back result if the number of products is less than the limit") {
        for {
          _ <- save(Chunk.fromIterable(relevantReviews))
          bestRatedResponse <- BestRatedService.run(request)
        } yield assertTrue(bestRatedResponse == expectedResponse)
      },
      test("ignores reviews outside the time range") {
        for {
          _ <- save(Chunk.fromIterable(relevantReviews ++ reviewsOutsideTimeRange))
          bestRatedResponse <- BestRatedService.run(request)
        } yield assertTrue(bestRatedResponse == expectedResponse)
      },
      test("ignores products where the minimum number of reviews doesn't reach the limit") {
        for {
          _ <- save(Chunk.fromIterable(relevantReviews ++ notEnoughReviews))
          bestRatedResponse <- BestRatedService.run(request)
        } yield assertTrue(bestRatedResponse == expectedResponse)
      },
      test("gives back product with the best average rating") {
        val reviewsForProduct1 = List(
          AmazonReview("p1", 4, getTimestamp("2000-03-01T14:34:04")),
          AmazonReview("p1", 3, getTimestamp("2000-04-30T22:11:51")),
          AmazonReview("p1", 4, getTimestamp("2000-12-30T12:10:33"))
        )
        val reviewsForProduct2 = List(
          AmazonReview("p2", 1, getTimestamp("2000-05-03T12:00:12")),
          AmazonReview("p2", 2, getTimestamp("2000-08-16T14:43:32")),
          AmazonReview("p2", 2, getTimestamp("2000-07-12T14:50:00"))
        )
        for {
          _ <- save(Chunk.fromIterable(reviewsForProduct1 ++ reviewsForProduct2))
          bestRatedResponse <- BestRatedService.run(
            BestRatedRequest("01.01.2000", "31.12.2001", limit = 1, minNumberReviews = 1)
          )
        } yield assertTrue(bestRatedResponse.size == 1) &&
          assertTrue(bestRatedResponse.head.asin == "p1") &&
          assert(bestRatedResponse.head.average_rating)(
            approximatelyEquals(BigDecimal(3.6666666666666667), BigDecimal(0.000001))
          )
      }
    ).provide(
      BestRatedService.live,
      ReviewRepository.live,
      FileStream.live,
      quillLayer,
      dsLayer,
      Runtime.removeDefaultLoggers
    ) @@ TestAspect.sequential @@ TestAspect.after(cleanup().provide(ReviewRepository.live, quillLayer, dsLayer))

  private def getTimestamp(time: String): Long =
    LocalDateTime.parse(time).toEpochSecond(ZoneOffset.UTC)
}
