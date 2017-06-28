/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.rasapi.connectors

import java.util.UUID

import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ShouldMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers.{await, _}
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.rasapi.models.{CustomerDetails, ResidencyStatus}
import uk.gov.hmrc.rasapi.models._

import scala.concurrent.Future

class DesConnectorSpec extends WordSpec with OneAppPerSuite with MockitoSugar with ShouldMatchers{

  implicit val hc = HeaderCarrier()

  val mockHttp = mock[HttpPost]

  object TestDesConnector extends DesConnector {
    override val http: HttpPost = mockHttp
  }

  val residencyStatus = Json.parse(
    """{
         "currentYearResidencyStatus" : "scotResident",
         "nextYearForecastResidencyStatus" : "scotResident"
        }
    """.stripMargin
  )

  "DESConnector"  should {

    "return 200 and a residency status when a customer body is passed" in {
        when(mockHttp.POST[HttpResponse, HttpResponse](Matchers.any(),Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, Some(residencyStatus))))

        val result = await(TestDesConnector.getResidencyStatus(Nino("LE241131B")))

        result shouldBe ResidencyStatus("scotResident","scotResident")
      }
    }


    "return an error when 404 is returned" in {
      when(mockHttp.POST[HttpResponse, HttpResponse](Matchers.any(),Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(404, None)))

      val result = TestDesConnector.getResidencyStatus(Nino("LE241131B"))
      intercept[Upstream4xxResponse] {
        await(result)
      }
    }

    "return an error when 500 is returned" in {
      when(mockHttp.POST[HttpResponse, HttpResponse](Matchers.any(),Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(500, None)))

      val result = TestDesConnector.getResidencyStatus(Nino("LE241131B"))
      intercept[Upstream5xxResponse] {
        await(result)
      }
    }

}
