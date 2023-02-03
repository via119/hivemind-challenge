package service

import http.BestRatedRequest
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

case class AmazonReview(asin: String, overall: Int, reviewTime: Long)

object AmazonReview {
  implicit val decoder: Decoder[BestRatedRequest] = deriveDecoder
}
