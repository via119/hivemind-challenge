package hivemind

import fs2.Stream
import hivemind.http.BestRatedRoute.bestRatedRoute
import hivemind.service.FileStream.getStream
import hivemind.service.ReviewRepository.save
import hivemind.service.{FileStream, ReviewRepository}
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import org.http4s.blaze.server.BlazeServerBuilder
import scopt.OParser
import zio.interop.catz.*
import zio.{Scope, Task, ZIO, ZIOAppArgs}

case class MainArgs(filePath: String)

object MainArgs {
  private val builder = OParser.builder[MainArgs]
  val argParser = {
    import builder.*
    OParser.sequence(
      opt[String]('f', "filePath")
        .required()
        .action((a, c) => c.copy(filePath = a))
        .text("file path is required")
    )
  }
}

object Main extends CatsApp {
  private val serverStream: Stream[Task, Nothing] = {
    import org.http4s.implicits.*
    val httpApp = bestRatedRoute.orNotFound
    for {
      exitCode <- BlazeServerBuilder[Task]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(httpApp)
        .serve
    } yield exitCode
  }.drain

  private def fillRepository(filePath: String): ZIO[FileStream & ReviewRepository, Throwable, Unit] = {
    for {
      _ <- ZIO.logInfo("Init db.")
      stream <- getStream(filePath)
      _ <- stream.grouped(1000).foreach(reviews => save(reviews))
    } yield ()
  }

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] = for {
    args <- getArgs
    parsedArgs = OParser.parse(MainArgs.argParser, args, MainArgs(""))
    _ <- parsedArgs match {
      case Some(args) =>
        for {
          _ <- fillRepository(args.filePath).provide(
            FileStream.live,
            ReviewRepository.live,
            Quill.Postgres.fromNamingStrategy(SnakeCase),
            Quill.DataSource.fromPrefix("amazonReviewDatabaseConfig")
          )
          _ <- ZIO.logInfo("Starting server.")
          _ <- serverStream.compile.drain
        } yield ()
      case None => ZIO.unit
    }
  } yield ()
}
