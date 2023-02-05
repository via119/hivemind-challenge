package hivemind.service

import zio.stream.ZStream
import zio.{Ref, ZIO, ZLayer}

trait FileStreamMock extends FileStream {
  val streamFromFile: ZIO[Any, Nothing, Option[String]]
}

object FileStreamMock {
  val streamFromFile: ZIO[FileStreamMock, Throwable, Option[String]] =
    ZIO.environmentWithZIO[FileStreamMock](_.get.streamFromFile)

  def make(reviews: List[AmazonReview]): ZLayer[Any, Throwable, FileStreamMock & FileStream] =
    ZLayer.fromZIO(for {
      streamFromFileRef <- Ref.make(None: Option[String])
    } yield new FileStreamMock {
      override val streamFromFile: ZIO[Any, Nothing, Option[String]] = streamFromFileRef.get
      override def getAmazonReviewStream(filePath: String): ZStream[Any, Throwable, AmazonReview] =
        ZStream.fromIterableZIO(
          streamFromFileRef.set(Some(filePath)) *> ZIO.succeed(reviews)
        )
    })
}
