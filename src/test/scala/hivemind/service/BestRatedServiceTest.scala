package hivemind.service

import hivemind.http.{BestRatedRequest, BestRatedResponse}
import hivemind.service.BestRatedService.{getBestRatedReviews, setupRepository}
import hivemind.service.FileStreamMock.streamFromFile
import hivemind.service.ReviewRepositoryMock.{bestRatedParameters, cleanupCalled, savedReviews}
import zio.Scope
import zio.test.Assertion.{equalTo, isSome, isTrue}
import zio.test.*
import zio.Runtime

import java.time.{LocalDateTime, ZoneOffset}

object BestRatedServiceTest extends ZIOSpecDefault {
  val filePath = "path"
  val reviews: List[AmazonReview] = (1 to 10).map(i => AmazonReview(s"$i", 5, 398328320)).toList
  val request = BestRatedRequest("03.04.2002", "19.08.2012", 100, 10)
  val response = List(BestRatedResponse("1", BigDecimal(4.5)))

  override def spec: Spec[TestEnvironment & Scope, Any] = suite("BestRatedService")(
    suite("setupRepository")(
      test("calls cleanup") {
        for {
          _ <- setupRepository(filePath, 10)
          cleanupCalled <- cleanupCalled
        } yield assert(cleanupCalled)(isTrue)
      }.provide(FileStreamMock.make(Nil), ReviewRepositoryMock.make(Nil)),
      test("saves reviews from file to the repository") {
        for {
          _ <- setupRepository(filePath, 10)
          streamFromFile <- streamFromFile
        } yield assertTrue(streamFromFile.contains(filePath))
      }.provide(FileStreamMock.make(Nil), ReviewRepositoryMock.make(Nil)),
      test("calls save in batches") {
        for {
          _ <- setupRepository(filePath, 9)
          savedReviews <- savedReviews
        } yield assert(savedReviews)(equalTo(List(List(reviews.last), reviews.init)))
      }.provide(FileStreamMock.make(reviews), ReviewRepositoryMock.make(Nil))
    ),
    suite("getBestRatedReviews")(
      test("gives back result from repository") {
        for {
          result <- getBestRatedReviews(request)
        } yield assertTrue(result == response)
      }.provide(ReviewRepositoryMock.make(response)),
      test("calls repository with the correct parameters") {
        for {
          _ <- getBestRatedReviews(request)
          params <- bestRatedParameters
        } yield assert(params)(isSome) && {
          val (start, end, limit, minReviews) = params.get
          assert(LocalDateTime.ofEpochSecond(start, 0, ZoneOffset.UTC))(
            equalTo(LocalDateTime.parse("2002-04-03T00:00:00"))
          ) &&
          assert(LocalDateTime.ofEpochSecond(end, 0, ZoneOffset.UTC))(
            equalTo(LocalDateTime.parse("2012-08-19T23:59:59"))
          ) && assertTrue(limit == request.limit) && assertTrue(minReviews == request.minNumberReviews)
        }
      }.provide(ReviewRepositoryMock.make(Nil))
    )
  ).provideLayer(Runtime.removeDefaultLoggers)
}
