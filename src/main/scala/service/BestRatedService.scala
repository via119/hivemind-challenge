package service

import http.{BestRatedRequest, BestRatedResponse}
import zio.ZIO

object BestRatedService {
  def run(
      filePath: String,
      request: BestRatedRequest
  ): ZIO[Any, Throwable, List[BestRatedResponse]] =
    ZIO.attempt(List(BestRatedResponse("1", 1.1)))
}
