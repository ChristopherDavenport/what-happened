package io.chrisdavenport.whathappened

import cats.effect._
import fs2._
import cats.implicits._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeBuilder
import scala.concurrent.ExecutionContext.Implicits.global
import _root_.io.chrisdavenport.whathappened._

object HelloWorldServer extends StreamApp[IO] with Http4sDsl[IO] {

  def stream(args: List[String], requestShutdown: IO[Unit]) = for {
    alg <- Stream.eval(WhatHappenedAlg.impl[IO])
    service = WhatHappenedAlg.service(Sync[IO], alg)
    exitCode <- BlazeBuilder[IO]
      .bindHttp(8080, "0.0.0.0")
      .mountService(service, "/")
      .serve 
  } yield exitCode
    
}
