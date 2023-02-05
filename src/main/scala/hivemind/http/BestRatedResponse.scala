package hivemind.http

import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.{Decoder, Encoder}

case class BestRatedResponse(asin: String, averageRating: BigDecimal)

object BestRatedResponse {
  implicit val encodeInstant: Encoder[BigDecimal] =
    Encoder.encodeBigDecimal.contramap[BigDecimal](_.underlying().stripTrailingZeros())
  implicit lazy val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames
  implicit val encoder: Encoder[BestRatedResponse] = deriveConfiguredEncoder
  implicit val decoder: Decoder[BestRatedResponse] = deriveConfiguredDecoder
}
