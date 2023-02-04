package hivemind

import fs2.Stream
import hivemind.http.BestRatedRoute.bestRatedRoute
import hivemind.service.BestRatedService
import hivemind.service.BestRatedService.setup
import org.http4s.blaze.server.BlazeServerBuilder
import scopt.OParser
import zio.interop.catz.*
import zio.{Scope, Task, ZIO, ZIOAppArgs}

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
  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] = for {
    args <- getArgs
    parsedArgs = OParser.parse(MainArgs.argParser, args, MainArgs(""))
    _ <- parsedArgs match {
      case Some(args) =>
        for {
          _ <- BestRatedService.setup(args.reviewFilePath)
          _ <- ZIO.logInfo("Starting server.")
          _ <- serverStream.compile.drain
        } yield ()
      case None => ZIO.unit
    }
  } yield ()

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
}
