package hivemind.service

import io.circe
import io.circe.parser.decode
import zio.stream.ZStream
import zio.{Cause, ZIO, ZLayer}

trait FileStream {

  /** Iterates over the lines of the file and decodes them to AmazonReview class.
    * @param filePath
    *   Path to data file containing amazon reviews.
    * @return
    *   ZStream of reviews
    */
  def getAmazonReviewStream(filePath: String): ZStream[Any, Throwable, AmazonReview]
}

object FileStream {
  val live = ZLayer.succeed(new FileStream {
    override def getAmazonReviewStream(filePath: String): ZStream[Any, Throwable, AmazonReview] =
      ZStream
        .fromIteratorScoped(
          ZIO
            .fromAutoCloseable(ZIO.attempt(scala.io.Source.fromFile(filePath)))
            .map(_.getLines())
        )
        .mapZIO(line => decodeReview(line))

    private def decodeReview(line: String): ZIO[Any, Throwable, AmazonReview] = {
      val parseResult: Either[circe.Error, AmazonReview] = decode[AmazonReview](line)

      parseResult match {
        case Left(decodingFailure) =>
          ZIO.logErrorCause("Could not decode line", Cause.fail(decodingFailure)) *>
            ZIO.fail(new Throwable("Creating stream failed."))
        case Right(review) => ZIO.succeed(review)
      }
    }
  })
}
