import fs2.Stream
import http.BestRatedRoute.bestRatedRoute
import org.http4s.blaze.server.BlazeServerBuilder
import zio.interop.catz.*
import zio.{Scope, Task, ZIO, ZIOAppArgs}

object Main extends CatsApp {
  def stream: Stream[Task, Nothing] = {
    import org.http4s.implicits.*
    val httpApp = bestRatedRoute.orNotFound
    for {
      exitCode <- BlazeServerBuilder[Task]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(httpApp)
        .serve
    } yield exitCode
  }.drain

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] = for {
    _ <- ZIO.logInfo("Starting server.")
    _ <- stream.compile.drain
  } yield ()
}
