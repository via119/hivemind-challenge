package hivemind.service

import hivemind.http.{BestRatedRequest, BestRatedResponse}
import zio.{Task, ZIO, ZLayer}

trait BestRatedServiceMock extends BestRatedService

object BestRatedServiceMock {
  def make(
      expectedResponse: List[BestRatedResponse],
      runFailure: Option[Throwable]
  ): ZLayer[Any, Throwable, BestRatedServiceMock & BestRatedService] =
    ZLayer.succeed(new BestRatedServiceMock {
      override def setup(reviewFilePath: String, batchSize: Int): Task[Unit] = ZIO.unit
      override def run(request: BestRatedRequest): Task[List[BestRatedResponse]] = runFailure match {
        case Some(error) => ZIO.fail(error)
        case None        => ZIO.succeed(expectedResponse)
      }
    })
}
