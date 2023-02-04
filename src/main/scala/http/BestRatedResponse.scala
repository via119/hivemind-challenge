package http

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

case class BestRatedResponse(asin: String, average_rating: BigDecimal)

object BestRatedResponse {
  implicit val encodeInstant: Encoder[BigDecimal] =
    Encoder.encodeBigDecimal.contramap[BigDecimal](_.underlying().stripTrailingZeros())
  implicit val encoder: Encoder[BestRatedResponse] = deriveEncoder
}
