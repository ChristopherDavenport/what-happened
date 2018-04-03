package io.chrisdavenport.whathappened

import cats.effect._
import cats.implicits._
import fs2._
import org.http4s._
import org.http4s.dsl._
import org.http4s.circe._
import _root_.io.circe._

trait WhatHappenedAlg[F[_]]{
  import WhatHappenedAlg._

  def sayWhatsUp(i: Interaction): F[Unit]
  def getWhatHasHappened: F[List[Interaction]]
}

object WhatHappenedAlg {
  def apply[F[_]](implicit ev: WhatHappenedAlg[F]) = ev

  final case class Interaction(method: Method, path: String)
  object Interaction{
    implicit val interactionEncoder: Encoder[Interaction] = Encoder.instance[Interaction](i => 
      Json.obj(
        "method" -> Json.fromString(i.method.show),
        "path" -> Json.fromString(i.path)
      )
    )
  }

  def impl[F[_]: Sync]: F[WhatHappenedAlg[F]] = async.refOf[F, Vector[Interaction]](Vector.empty[Interaction])
    .map(ref => 
      new WhatHappenedAlg[F]{
        def sayWhatsUp(i: Interaction): F[Unit] = ref.modify{vec =>
        val newVec = if (vec.length <= 500){
          vec
        } else {
          vec.drop(vec.length - 500)
        }
        newVec :+ i 
        }.void
        def getWhatHasHappened: F[List[Interaction]] = ref.get.map(_.toList)
      }
    )

  def service[F[_] : Sync: WhatHappenedAlg]: HttpService[F] = {
    import Interaction._
    HttpService{
      case req => {
        val method = req.method
        val path = req.uri.path
        val interaction = Interaction(method, path)
        for {
          _ <- WhatHappenedAlg[F].sayWhatsUp(interaction)
          happened <- WhatHappenedAlg[F].getWhatHasHappened
          resp <- Response[F](Status.Ok).withBody(happened)(Sync[F], jsonEncoderOf[F, List[Interaction]])
        } yield resp
      }
    }

  } 



}