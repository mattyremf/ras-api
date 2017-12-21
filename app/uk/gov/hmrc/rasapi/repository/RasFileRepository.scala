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

package uk.gov.hmrc.rasapi.repository

import java.io.FileInputStream
import java.nio.file.Path

import play.api.Logger
import play.api.libs.iteratee.Enumerator
import play.modules.reactivemongo.MongoDbConnection
import reactivemongo.api.gridfs.Implicits._
import reactivemongo.api.gridfs._
import reactivemongo.api.{BSONSerializationPack, DB, DBMetaCommands}
import reactivemongo.bson.{BSONDocument, BSONObjectID, BSONValue}
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.rasapi.models.{CallbackData, ResultsFile}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

object RasRepository extends MongoDbConnection{

  private implicit val connection = mongoConnector.db

  lazy val filerepo: RasFileRepository = new RasFileRepository(connection)
}

case class FileData(length: Long = 0, data: Enumerator[Array[Byte]], _id:BSONValue)

class RasFileRepository(mongo: () => DB with DBMetaCommands)(implicit ec: ExecutionContext)
  extends ReactiveRepository[CallbackData, BSONObjectID]("rasFileStore", mongo, CallbackData.formats) {

  private val contentType =  "text/csv"
  val gridFSG = new GridFS[BSONSerializationPack.type](mongo(), "resultsFiles")

  def saveFile(filePath: Path, fileId: String): Future[ResultsFile] =
  {
    val fileToSave = DefaultFileToSave(s"${fileId}.csv", Some(contentType))

    gridFSG.writeFromInputStream(fileToSave,new FileInputStream(filePath.toFile)).map{ res=>
      logger.warn("Saved File id is " + res.id)
      res }
      .recover{case ex:Throwable =>
        Logger.error(s"error saving file -> ${fileId} due to error ${ex.getMessage}")
        throw new RuntimeException("failed to save file due to error" + ex.getMessage) }
  }

  def fetchFile(_fileName: String)(implicit ec: ExecutionContext): Future[Option[FileData]] = {
      Logger.debug("file to fetch => id : " + _fileName)
      gridFSG.find[BSONDocument, ResultsFile](BSONDocument("filename" -> _fileName)).headOption.map {
      case Some(file) =>   logger.warn("file fetched " + file.id); Some(FileData(file.length, gridFSG.enumerate(file),file.id))
      case None => logger.warn("file not found "); None
      }.recover{
        case ex:Throwable =>
        Logger.error("error trying to fetch file " + _fileName + " " + ex.getMessage)
        throw new RuntimeException("failed to fetch file due to error" + ex.getMessage)
      }
  }

  def  removeFile(id:BSONValue): Future[Boolean] = {
    Logger.debug("file to remove => id : " + id)
    gridFSG.remove[BSONDocument](BSONDocument("_id" -> id)).map(res => res.hasErrors).recover {
      case ex: Throwable =>
        Logger.error("error trying to remove file " + id + " " + ex.getMessage)
        throw new RuntimeException("failed to remove file due to error" + ex.getMessage)
    }
  }

}