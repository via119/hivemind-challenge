package http

import io.circe.syntax.*
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import org.http4s.HttpRoutes
import org.http4s.circe.*
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.dsl.Http4sDsl
import service.{BestRatedService, ReviewRepository}
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
          response <- BestRatedService
            .run(request)
            .foldZIO(
              _ => InternalServerError("Unexpected error occurred."),
              result => Ok(result.asJson)
            )
            .provide(
              ReviewRepository.live,
              Quill.Postgres.fromNamingStrategy(SnakeCase),
              Quill.DataSource.fromPrefix("amazonReviewDatabaseConfig")
            )
        } yield response
      }
  }
}
