package hivemind.http

import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.{Decoder, Encoder}

/** Request format for /amazon/best-rated API
  * @param start
  *   start of the date range in dd.MM.yyyy format
  * @param end
  *   end of the date range in dd.MM.yyyy format
  * @param limit
  *   maximum number of products to return
  * @param minNumberReviews
  *   only consider products that have minReviews number of reviews
  */
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
