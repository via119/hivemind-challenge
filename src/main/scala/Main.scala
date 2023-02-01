import fs2.Stream
import http.BestRatedRoute.bestRatedRoute
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
  def stream(filePath: String): Stream[Task, Nothing] = {
    import org.http4s.implicits.*
    val httpApp = bestRatedRoute(filePath).orNotFound
    for {
      exitCode <- BlazeServerBuilder[Task]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(httpApp)
        .serve
    } yield exitCode
  }.drain

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] = for {
    args <- getArgs
    parsedArgs = OParser.parse(MainArgs.argParser, args, MainArgs(""))
    _ <- parsedArgs match {
      case Some(args) =>
        ZIO.logInfo("Starting server.") *> stream(args.filePath).compile.drain
      case None => ZIO.unit
    }
  } yield ()
}
