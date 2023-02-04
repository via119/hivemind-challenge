package hivemind.service

import hivemind.http.{BestRatedRequest, BestRatedResponse}
import hivemind.service.FileStream.getAmazonReviewStream
import hivemind.service.ReviewRepository.{cleanup, save}
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.ZIO

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalTime, ZoneOffset}

object BestRatedService {
  val quillLayer = Quill.Postgres.fromNamingStrategy(SnakeCase)
  val dsLayer = Quill.DataSource.fromPrefix("amazonReviewDatabaseConfig")

  def setup(reviewFilePath: String): ZIO[Any, Throwable, Unit] = {
    setupRepository(reviewFilePath, 1000).provide(ReviewRepository.live, FileStream.live, quillLayer, dsLayer)
  }

  def run(request: BestRatedRequest): ZIO[Any, Throwable, List[BestRatedResponse]] = {
    getBestRatedReviews(request).provide(ReviewRepository.live, quillLayer, dsLayer)
  }

  def getBestRatedReviews(request: BestRatedRequest): ZIO[ReviewRepository, Throwable, List[BestRatedResponse]] = {
    val startTimestamp = getTimestamp(request.start, LocalTime.MIN)
    val endTimestamp = getTimestamp(request.end, LocalTime.MAX)
    for {
      _ <- ZIO.logInfo(s"Received request: $request")
      response <- ReviewRepository.getBestRated(startTimestamp, endTimestamp, request.limit, request.minNumberReviews)
    } yield response
  }

  def setupRepository(reviewFilePath: String, batchSize: Int): ZIO[FileStream & ReviewRepository, Throwable, Unit] = {
    for {
      _ <- ZIO.logInfo("Init db.")
      _ <- cleanup().onError(err => ZIO.logErrorCause("Failed to delete data from repository at startup.", err))
      stream <- getAmazonReviewStream(reviewFilePath)
      _ <- stream.grouped(batchSize).foreach(reviews => save(reviews))
    } yield ()
  }

  private def getTimestamp(date: String, localTime: LocalTime): Long = {
    val dateFormatter = { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    LocalDate.parse(date, dateFormatter).toEpochSecond(localTime, ZoneOffset.UTC)
  }
}
