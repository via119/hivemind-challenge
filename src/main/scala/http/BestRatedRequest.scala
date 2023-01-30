package http

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

case class BestRatedRequest(
    start: String,
    end: String,
    limit: Int,
    min_number_reviews: Int
)

object BestRatedRequest {
  implicit val decoder: Decoder[BestRatedRequest] = deriveDecoder
}
