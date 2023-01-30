package http

import io.circe.syntax.*
import org.http4s.HttpRoutes
import org.http4s.circe.*
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.dsl.Http4sDsl
import service.BestRatedService
import zio.{Task, ZIO}
import zio.interop.catz.*

object BestRatedRoute {
  val bestRatedRoute: HttpRoutes[Task] = {
    val dsl = new Http4sDsl[Task] {}
    import dsl.*
    HttpRoutes
      .of[Task] { case req @ GET -> Root / "amazon" / "best-rated" =>
        for {
          request <- req.as[BestRatedRequest]
          _ <- ZIO.logInfo(s"Received request: $request")
          r <- BestRatedService
            .run(request)
            .onError(cause => ZIO.logErrorCause("Something went wrong.", cause))
          response <- Ok(r.asJson)
        } yield response
      }
  }
}
