package hivemind.service

import hivemind.http.{BestRatedRequest, BestRatedResponse}
import hivemind.service.FileStreamMock.streamFromFile
import hivemind.service.ReviewRepositoryMock.{bestRatedParameters, cleanupCalled, savedReviews}
import zio.{Runtime, Scope}
import zio.test.*
import zio.test.Assertion.{equalTo, isSome, isTrue}

import java.time.{LocalDateTime, ZoneOffset}

object BestRatedServiceTest extends ZIOSpecDefault {
  val filePath = "path"
  val reviews: List[AmazonReview] = (1 to 10).map(i => AmazonReview(s"$i", 5, 398328320)).toList
  val request = BestRatedRequest("03.04.2002", "19.08.2012", 100, 10)
  val response = List(BestRatedResponse("1", BigDecimal(4.5)))

  override def spec: Spec[TestEnvironment & Scope, Any] = suite("BestRatedService")(
    suite("setup")(
      test("calls cleanup") {
        for {
          _ <- BestRatedService.setup(filePath, 10)
          cleanupCalled <- cleanupCalled
        } yield assert(cleanupCalled)(isTrue)
      }.provide(BestRatedService.live, FileStreamMock.make(Nil), ReviewRepositoryMock.make(Nil)),
      test("saves reviews from file to the repository") {
        for {
          _ <- BestRatedService.setup(filePath, 10)
          streamFromFile <- streamFromFile
        } yield assertTrue(streamFromFile.contains(filePath))
      }.provide(BestRatedService.live, FileStreamMock.make(Nil), ReviewRepositoryMock.make(Nil)),
      test("calls save in batches") {
        for {
          _ <- BestRatedService.setup(filePath, 9)
          savedReviews <- savedReviews
        } yield assert(savedReviews)(equalTo(List(List(reviews.last), reviews.init)))
      }.provide(BestRatedService.live, FileStreamMock.make(reviews), ReviewRepositoryMock.make(Nil))
    ),
    suite("run")(
      test("gives back result from repository") {
        for {
          result <- BestRatedService.run(request)
        } yield assertTrue(result == response)
      }.provide(BestRatedService.live, FileStreamMock.make(Nil), ReviewRepositoryMock.make(response)),
      test("calls repository with the correct parameters") {
        for {
          _ <- BestRatedService.run(request)
          params <- bestRatedParameters
        } yield assert(params)(isSome) && {
          val (start, end, limit, minReviews) = params.get
          assertTimestamp(start, expectedDate = "2002-04-03T00:00:00") &&
          assertTimestamp(end, expectedDate = "2012-08-19T23:59:59") &&
          assertTrue(limit == request.limit) &&
          assertTrue(minReviews == request.minNumberReviews)
        }
      }.provide(BestRatedService.live, FileStreamMock.make(Nil), ReviewRepositoryMock.make(Nil))
    )
  ).provideLayer(Runtime.removeDefaultLoggers)

  private def assertTimestamp(start: Long, expectedDate: String): TestResult = {
    assert(LocalDateTime.ofEpochSecond(start, 0, ZoneOffset.UTC))(
      equalTo(LocalDateTime.parse(expectedDate))
    )
  }
}
