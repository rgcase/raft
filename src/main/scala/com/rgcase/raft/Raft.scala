package com.rgcase.raft

import java.util.concurrent.TimeUnit

import akka.actor.{ ActorPath, ActorSystem }
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

object Raft extends App with LazyLogging {

  logger.info("Starting Raft")

  logger.info("Creating ActorSystem...")
  val system = ActorSystem("raft")
  logger.info("ActorSystem started.")

  val raftConfig = system.settings.config.getConfig("raft")

  logger.info("Creating RaftActor...")
  val raftActor = system.actorOf(
    RaftActor.props(
      FiniteDuration(raftConfig.getDuration("join-timeout", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS),
      raftConfig.getInt("max-join-retries"),
      FiniteDuration(raftConfig.getDuration("heartbeat-timeout", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS),
      FiniteDuration(raftConfig.getDuration("election-timeout", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS),
      FiniteDuration(raftConfig.getDuration("leave-timeout", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS),
      Try(raftConfig.getString("seed")).toOption.map(ActorPath.fromString)
    ),
    "raft"
  )

  logger.info("RaftActor {} created.", raftActor.path)

}
