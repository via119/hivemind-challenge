package hivemind.service

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

case class AmazonReview(asin: String, overall: Int, unixReviewTime: Long)

object AmazonReview {
  implicit val decoder: Decoder[AmazonReview] = deriveDecoder
}
