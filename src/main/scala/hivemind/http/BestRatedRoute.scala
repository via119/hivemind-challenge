package hivemind.http

import hivemind.Main.BestRatedIO
import hivemind.service.BestRatedService
import io.circe.syntax.*
import org.http4s.HttpRoutes
import org.http4s.circe.*
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.dsl.Http4sDsl
import zio.interop.catz.*

object BestRatedRoute {
  val service: HttpRoutes[BestRatedIO] = {
    val dsl = new Http4sDsl[BestRatedIO] {}
    import dsl.*
    HttpRoutes
      .of[BestRatedIO] { case req @ GET -> Root / "amazon" / "best-rated" =>
        for {
          request <- req.as[BestRatedRequest]
          response <- BestRatedService
            .run(request)
            .foldZIO(
              _ => InternalServerError("Unexpected error occurred."),
              result => Ok(result.asJson)
            )
        } yield response
      }
  }
}
