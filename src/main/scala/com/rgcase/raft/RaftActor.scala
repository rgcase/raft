package com.rgcase.raft

import akka.actor.{Actor, ActorLogging, ActorPath, ActorRef, Cancellable}

import scala.concurrent.duration.FiniteDuration


object RaftActor {

  sealed trait RaftMessage

  case object Join extends RaftMessage

  case class AppendEntries(term: Int, prevLogIndex: Int, prevLogTerm: Event, entries: Vector[(Int, Event)], leaderCommit: Int) extends RaftMessage
  case class AppendResponse(term: Int, success: Boolean) extends RaftMessage

  case class RequestVote(term: Int, lastLogIndex: Int, lastLogTerm: Event) extends RaftMessage
  case class VoteResponse(term: Int, voteGranted: Boolean) extends RaftMessage

  case object JoinTimeout
  case object RaftTimeout

  sealed trait Event { val term: Int }
  case class MemberUp(term: Int, member: ActorRef) extends Event
  case class MemberDown(term: Int, member: ActorRef) extends Event

}

class RaftActor(joinTimeout: FiniteDuration, electionTimeout: FiniteDuration, seed: ActorPath) extends Actor with ActorLogging {
  import RaftActor._
  import context.dispatcher

  var currentTerm: Int = 0
  var votedFor: Option[ActorRef] = None

  var commitIndex: Int = 0
  var lastApplied: Int = 0

  var eventLog: Vector[Event] = Vector(new Event { val term = 0 })
  var members: Set[ActorRef] = Set.empty

  var electionTimer: Option[Cancellable] = None

  override def preStart() = {  }

  def receive = ???

  def follower(leader: ActorRef): Receive = ???

  def candidate(leader: ActorRef): Receive = ???

  def leader(nextIndex: Map[ActorRef, Int], matchIndex: Map[ActorRef, Int]): Receive = ???

}