package http

import io.circe.Decoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder

case class BestRatedRequest(
    start: String,
    end: String,
    limit: Int,
    minNumberReviews: Int
)

object BestRatedRequest {
  implicit lazy val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames
  implicit val decoder: Decoder[BestRatedRequest] = deriveConfiguredDecoder
}
