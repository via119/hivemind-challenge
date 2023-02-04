package service

import io.circe
import zio.{Cause, ZIO, ZLayer}
import zio.stream.ZStream
import io.circe.parser.decode
trait FileStream {
  def getStream(filePath: String): ZStream[Any, Throwable, AmazonReview]
}

object FileStream {
  def getStream(filePath: String): ZIO[FileStream, Throwable, ZStream[Any, Throwable, AmazonReview]] =
    ZIO.environmentWith[FileStream](_.get.getStream(filePath))

  val live = ZLayer.succeed(new FileStream {
    override def getStream(filePath: String): ZStream[Any, Throwable, AmazonReview] =
      ZStream
        .fromIteratorScoped(ZIO.fromAutoCloseable(ZIO.attempt(scala.io.Source.fromFile(filePath))).map(_.getLines()))
        .mapZIO(line => decodeReview(line))

    def decodeReview(line: String): ZIO[Any, Throwable, AmazonReview] = {
      val parseResult: Either[circe.Error, AmazonReview] = decode[AmazonReview](line)

      parseResult match {
        case Left(decodingFailure) =>
          ZIO.logErrorCause("Invalid line", Cause.fail(decodingFailure)) *>
            ZIO.fail(new Throwable("Creating stream failed."))
        case Right(review) => ZIO.succeed(review)
      }
    }

  })
}
