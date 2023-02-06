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

  /** Reads command line arguments and provides ZIO layers for runBestRatedService. It reads PostgreSQL config from
    * application.conf
    */
  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] = for {
    args <- getArgs
    parsedArgs = OParser.parse(MainArgs.argParser, args, MainArgs(""))
    _ <- parsedArgs match {
      case Some(args) =>
        runBestRatedService(args.reviewFilePath).provide(
          BestRatedService.live,
          ReviewRepository.live,
          FileStream.live,
          Quill.Postgres.fromNamingStrategy(SnakeCase),
          Quill.DataSource.fromPrefix("amazonReviewDatabaseConfig")
        )
      case None => ZIO.unit
    }
  } yield ()

  /** Prepares the PostgreSQL table and starts a web service for best rated product requests.
    * @param reviewFilePath
    *   Path to data file containing amazon reviews.
    */
  private def runBestRatedService(reviewFilePath: String): ZIO[BestRatedService, Throwable, Unit] = {
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
