/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.rasapi.repositories

import org.scalatest.BeforeAndAfter
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerTest
import play.api.Logger
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.rasapi.repositories.RepositoriesHelper.{
  createFile,
  rasFileRepository
}

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

class RasFileRepositorySpec
    extends UnitSpec
    with MockitoSugar
    with OneAppPerTest
    with BeforeAndAfter {

  val userId: String = "A1234567"

  before {
    rasFileRepository.removeAll()
  }
  after {
    rasFileRepository.removeAll()
  }

  "RasFileRepository" should {
    "saveFile" in {
      val file = await(
        rasFileRepository
          .saveFile("user111", "envelope111", createFile, "file111"))

      file.filename.get shouldBe "file111"
      val result = await(rasFileRepository.getFile(file))
      val actual = result.toArray
      Logger.debug(actual.mkString)
      actual shouldBe RepositoriesHelper.resultsArr
    }

    "get File" in {
      val resultFile = await(
        RepositoriesHelper.saveTempFile("user123", "envelope123", "file123"))
      Logger.debug("resultFile.id.toString  -> " + resultFile.id.toString)
      val res =
        await(rasFileRepository.fetchFile(resultFile.filename.get, userId))
      // res.get.data. shouldBe tempFile
      val result = ListBuffer[String]()
      res.get.data run RepositoriesHelper.getAll map { bytes =>
        result += new String(bytes)
      }
    }

    "removeFile" in {
      val resultFile = await(
        RepositoriesHelper.saveTempFile("user222", "envelope222", "file222"))
      Logger.debug(
        s"file to remove ---> name : ${resultFile.filename.get} id = ${resultFile.id}  ")

      val res = await(
        rasFileRepository
          .removeFile(resultFile.filename.get, resultFile.id.toString, userId))
      res shouldBe true
      val fileData =
        await(rasFileRepository.fetchFile(resultFile.filename.get, userId))
      fileData.isDefined shouldBe false
    }

    "check if File exists" in {
      val resultFile = await(
        RepositoriesHelper.saveTempFile("user124", "envelope124", "file444"))
      Logger.debug("resultFile.id.toString  -> " + resultFile.id.toString)
      val res = await(
        rasFileRepository.isFileExists(
          resultFile.id.asInstanceOf[BSONObjectID]))
      // res.get.data. shouldBe tempFile
      res.isDefined shouldBe true
      await(
        rasFileRepository
          .removeFile(resultFile.filename.get, resultFile.id.toString, userId))

    }
  }
}
