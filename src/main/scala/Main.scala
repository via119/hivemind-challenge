import cats.Applicative
import cats.effect.Async
import fs2.Stream
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.dsl.Http4sDsl
import zio.interop.catz.*
import zio.{Scope, Task, ZIO, ZIOAppArgs}

object Main extends CatsApp {
  def stream: Stream[Task, Nothing] = {
    import org.http4s.implicits.*
    val httpApp = helloWorldRoute.orNotFound
    for {
      exitCode <- BlazeServerBuilder[Task]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(httpApp)
        .serve
    } yield exitCode
  }.drain

  def helloWorldRoute: HttpRoutes[Task] = {
    val dsl = new Http4sDsl[Task] {}
    import dsl.*
    HttpRoutes
      .strict[Task] { case GET -> Root / "amazon" / "best-rated" =>
        Ok("Hello World!")
      }
  }

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] = for {
    _ <- ZIO.logInfo("Starting server.")
    _ <- stream.compile.drain
  } yield ()
}
