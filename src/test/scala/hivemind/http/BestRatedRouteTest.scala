package hivemind.http

import hivemind.Main.BestRatedIO
import hivemind.service.{BestRatedService, BestRatedServiceMock}
import io.circe.parser.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.implicits.*
import zio.interop.catz.*
import zio.test.*
import zio.test.Assertion.{equalTo, isSome}
import zio.{Scope, ZIO}

object BestRatedRouteTest extends ZIOSpecDefault {
  val request = BestRatedRequest("03.04.2002", "19.08.2012", 100, 10)
  val expectedResponse = List(BestRatedResponse("r1", BigDecimal(4.5)))

  override def spec: Spec[TestEnvironment & Scope, Any] = suite("BestRatedRoute")(
    test("returns response from best rated service") {
      for {
        response <- BestRatedRoute.service
          .run(
            Request[BestRatedIO](Method.GET, uri"/amazon/best-rated").withEntity(request.asJson)
          )
          .value
        body <- parseResponseBody(response)
      } yield assert(body)(equalTo(expectedResponse))
    }.provide(BestRatedServiceMock.make(expectedResponse, None)),
    test("returns internal server error in case of failure") {
      for {
        status <- BestRatedRoute.service
          .run(
            Request[BestRatedIO](Method.GET, uri"/amazon/best-rated").withEntity(request.asJson)
          )
          .map(_.status)
          .value
      } yield assert(status)(isSome(equalTo(Status.InternalServerError)))
    }.provide(BestRatedServiceMock.make(Nil, Some(new Throwable("error"))))
  ) @@ TestAspect.sequential

  private def parseResponseBody(
      maybeResponse: Option[Response[BestRatedIO]]
  ): ZIO[BestRatedService, Throwable, List[BestRatedResponse]] = {
    maybeResponse match {
      case Some(response) =>
        response.body.compile.toVector
          .map(x => x.map(_.toChar).mkString(""))
          .flatMap(responseString => ZIO.fromEither(decode[List[BestRatedResponse]](responseString)))
      case None => ZIO.fail(new Throwable("test failure"))
    }
  }
}
