package hivemind.service

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

/** Represents a row in 'amazon_review' table.
  * @param asin
  *   product id
  * @param overall
  *   rating of the product
  * @param unixReviewTime
  *   time of the review
  */
case class AmazonReview(asin: String, overall: Int, unixReviewTime: Long)

object AmazonReview {
  implicit val decoder: Decoder[AmazonReview] = deriveDecoder
}
