/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.rasapi.services

import org.scalatest.BeforeAndAfter
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.Logger
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.rasapi.repositories.RepositoriesHelper

import scala.concurrent.ExecutionContext.Implicits.global

class DataCleansingServiceSpec extends UnitSpec with MockitoSugar with OneAppPerSuite
with BeforeAndAfter  {
  before{
    await(RepositoriesHelper.rasFileRepository.removeAll())
    await(RepositoriesHelper.rasBulkOperationsRepository.removeAll())
  }
  after{
    await(RepositoriesHelper.rasFileRepository.removeAll())
    await(RepositoriesHelper.rasBulkOperationsRepository.removeAll())

  }

  "DataCleansingService" should{

    " not remove chunks that are not orphoned" in  {
      val testData1 = await(RepositoriesHelper.saveTempFile("user14","envelope14","fileId14"))
      val testData2=  await(RepositoriesHelper.saveTempFile("user15","envelope15","fileId15"))

      val result = await(DataCleansingService.removeOrphanedChunks())

      result.size shouldEqual 0
      await(RepositoriesHelper.rasFileRepository.remove("fileId14"))
      await(RepositoriesHelper.rasFileRepository.remove("fileId15"))
    }

    "remove orphaned chunks" in  {
      Logger.warn("1 ~~~~~~~~####### Testing Data Cleansing" )
      val testFiles = RepositoriesHelper.createTestDataForDataCleansing().map(_.id.asInstanceOf[BSONObjectID])

      val result = await(DataCleansingService.removeOrphanedChunks())
      Logger.warn("7 ~~~~~~~~####### results complete" )

      result shouldEqual  testFiles
    }

  }

}
