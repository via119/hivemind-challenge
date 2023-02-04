package service

import http.{BestRatedRequest, BestRatedResponse}
import zio.ZIO

object BestRatedService {
  def run(
      request: BestRatedRequest
  ): ZIO[ReviewRepository, Throwable, List[BestRatedResponse]] =
    ZIO.attempt(List(BestRatedResponse("1", 1.1)))
}
