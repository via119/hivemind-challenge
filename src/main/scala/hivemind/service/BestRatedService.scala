package hivemind.service

import hivemind.http.{BestRatedRequest, BestRatedResponse}
import zio.{Task, ZIO, ZLayer}

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalTime, ZoneOffset}

trait BestRatedService {

  /** Cleans up the PostgreSQL table and then inserts the content of the file.
    * @param reviewFilePath
    *   path to data file containing amazon reviews.
    * @param batchSize
    *   processing size for batch inserts
    */
  def setup(reviewFilePath: String, batchSize: Int): Task[Unit]

  /** @param request
    *   request from web service for best rated products
    * @return
    *   list of best rated products
    */
  def run(request: BestRatedRequest): Task[List[BestRatedResponse]]
}

object BestRatedService {

  def setup(reviewFilePath: String, batchSize: Int): ZIO[BestRatedService, Throwable, Unit] = {
    ZIO.environmentWithZIO[BestRatedService](_.get.setup(reviewFilePath, batchSize))
  }

  def run(request: BestRatedRequest): ZIO[BestRatedService, Throwable, List[BestRatedResponse]] = {
    ZIO.environmentWithZIO[BestRatedService](_.get.run(request))
  }

  val live: ZLayer[FileStream & ReviewRepository, Nothing, BestRatedService] = ZLayer.fromZIO(
    for {
      fileStream <- ZIO.service[FileStream]
      reviewRepository <- ZIO.service[ReviewRepository]
    } yield new BestRatedService {
      override def setup(reviewFilePath: String, batchSize: Int): Task[Unit] = for {
        _ <- ZIO.logInfo("Init db.")
        _ <- reviewRepository
          .cleanup()
          .onError(err => ZIO.logErrorCause("Failed to delete data from repository at startup.", err))
        stream = fileStream.getAmazonReviewStream(reviewFilePath)
        _ <- stream.grouped(batchSize).foreach(reviews => reviewRepository.save(reviews))
      } yield ()

      override def run(request: BestRatedRequest): Task[List[BestRatedResponse]] = {
        val startTimestamp = getTimestamp(request.start, LocalTime.MIN)
        val endTimestamp = getTimestamp(request.end, LocalTime.MAX)
        for {
          _ <- ZIO.logInfo(s"Received request: $request")
          response <- reviewRepository.getBestRated(
            startTimestamp,
            endTimestamp,
            request.limit,
            request.minNumberReviews
          )
        } yield response
      }

      private def getTimestamp(date: String, localTime: LocalTime): Long = {
        val dateFormatter = {
          DateTimeFormatter.ofPattern("dd.MM.yyyy")
        }
        LocalDate.parse(date, dateFormatter).toEpochSecond(localTime, ZoneOffset.UTC)
      }
    }
  )
}
