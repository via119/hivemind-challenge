package service

import http.{BestRatedRequest, BestRatedResponse}
import service.ReviewRepository.getBestRated
import zio.ZIO

import java.time.{LocalDate, LocalTime, ZoneOffset}
import java.time.format.DateTimeFormatter

object BestRatedService {
  def run(
      request: BestRatedRequest
  ): ZIO[ReviewRepository, Throwable, List[BestRatedResponse]] = {
    val startTime = getTimeStamp(request.start, LocalTime.MIN)
    val endTime = getTimeStamp(request.end, LocalTime.MAX)

    getBestRated(startTime, endTime, request.limit, request.min_number_reviews)
  }

  private def getTimeStamp(date: String, localTime: LocalTime): Long = {
    val dateFormatter = { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    LocalDate.parse(date, dateFormatter).toEpochSecond(localTime, ZoneOffset.UTC)
  }
}
