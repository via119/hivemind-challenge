package hivemind.http

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class BestRatedResponse(asin: String, average_rating: BigDecimal)

object BestRatedResponse {
  implicit val encodeInstant: Encoder[BigDecimal] =
    Encoder.encodeBigDecimal.contramap[BigDecimal](_.underlying().stripTrailingZeros())
  implicit val encoder: Encoder[BestRatedResponse] = deriveEncoder
  implicit val decoder: Decoder[BestRatedResponse] = deriveDecoder
}
