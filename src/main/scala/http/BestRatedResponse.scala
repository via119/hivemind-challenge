package http

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

case class BestRatedResponse(asin: String, average_rating: Double)

object BestRatedResponse {
  implicit val encoder: Encoder[BestRatedResponse] = deriveEncoder
}
