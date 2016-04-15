package auth.mongo

import reactivemongo.api.DB
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands._
import reactivemongo.api.indexes.Index
import reactivemongo.bson._

import scala.concurrent.{ ExecutionContext, Future }

private[auth] abstract class AuthDao[Model, ID](db: DB, collectionName: String)(implicit
  modelWriter: BSONDocumentWriter[Model],
                                                                                modelReader: BSONDocumentReader[Model],
                                                                                idWriter:    BSONWriter[ID, _ <: BSONValue],
                                                                                idReader:    BSONReader[_ <: BSONValue, ID],
                                                                                ctx:         ExecutionContext) {

  val collection = db[BSONCollection](collectionName)

  val defaultWriteConcern: WriteConcern = WriteConcern.Default

  def $doc(elements: Producer[BSONElement]*): BSONDocument = {
    BSONDocument(elements: _*)
  }

  def $id[T](id: T)(implicit writer: BSONWriter[T, _ <: BSONValue]): BSONDocument = BSONDocument("_id" → id)

  protected def findOne(selector: BSONDocument = BSONDocument.empty): Future[Option[Model]] = collection.find(selector).one[Model]

  def autoIndexes: Traversable[Index] = Seq.empty

  def getById(id: ID): Future[Model] = findById(id)
    .flatMap {
      case Some(or) ⇒ Future.successful(or)
      case None     ⇒ Future.failed(new NoSuchElementException(s"Cannot find record with id = $id"))
    }

  def findById(id: ID): Future[Option[Model]] =
    findOne($id(id))

  def insert(record: Model, writeConcern: WriteConcern = defaultWriteConcern): Future[Model] = {
    collection.insert(modelWriter.write(record), writeConcern).filter(_.ok).map(_ ⇒ record)
  }

  def updateById(id: ID, update: BSONDocument): Future[Model] = {
    collection.update($id(id), update).flatMap(_ ⇒ getById(id))
  }

}
