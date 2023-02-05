package hivemind.http

import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}

case class BestRatedRequest(
    start: String,
    end: String,
    limit: Int,
    minNumberReviews: Int
)

object BestRatedRequest {
  implicit lazy val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames
  implicit val decoder: Decoder[BestRatedRequest] = deriveConfiguredDecoder
  implicit val encoder: Encoder[BestRatedRequest] = deriveConfiguredEncoder
}
