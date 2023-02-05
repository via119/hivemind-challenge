package hivemind

import fs2.Stream
import hivemind.http.BestRatedRoute
import hivemind.service.{BestRatedService, FileStream, ReviewRepository}
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import org.http4s.blaze.server.BlazeServerBuilder
import scopt.OParser
import zio.interop.catz.*
import zio.{Scope, ZIO, ZIOAppArgs}

case class MainArgs(reviewFilePath: String)

object MainArgs {
  private val builder = OParser.builder[MainArgs]
  val argParser = {
    import builder.*
    OParser.sequence(
      opt[String]('f', "reviewFilePath")
        .required()
        .action((a, c) => c.copy(reviewFilePath = a))
        .text("file path is required")
    )
  }
}

object Main extends CatsApp {
  type BestRatedIO[A] = ZIO[BestRatedService, Throwable, A]

  private val quillLayer = Quill.Postgres.fromNamingStrategy(SnakeCase)
  private val dsLayer = Quill.DataSource.fromPrefix("amazonReviewDatabaseConfig")

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] = for {
    args <- getArgs
    parsedArgs = OParser.parse(MainArgs.argParser, args, MainArgs(""))
    _ <- parsedArgs match {
      case Some(args) =>
        run(args.reviewFilePath).provide(
          BestRatedService.live,
          ReviewRepository.live,
          FileStream.live,
          quillLayer,
          dsLayer
        )
      case None => ZIO.unit
    }
  } yield ()

  private def run(reviewFilePath: String): ZIO[BestRatedService, Throwable, Unit] = {
    for {
      _ <- BestRatedService.setup(reviewFilePath, batchSize = 1000)
      _ <- ZIO.logInfo("Starting server.")
      _ <- serviceStream.compile.drain
    } yield ()
  }

  private val serviceStream: Stream[BestRatedIO, Nothing] = {
    import org.http4s.implicits.*
    val httpApp = BestRatedRoute.service.orNotFound
    for {
      exitCode <- BlazeServerBuilder[BestRatedIO]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(httpApp)
        .serve
    } yield exitCode
  }.drain
}
