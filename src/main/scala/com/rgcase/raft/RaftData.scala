package com.rgcase.raft

import akka.actor.{ ActorPath, ActorRef }

case class CommonData(leader: ActorRef, currentTerm: Int, votedFor: Option[ActorRef], eventLog: Vector[RaftEvent], commitIndex: Int, lastApplied: Int)

object RaftData {
  def firstLeader(leader: ActorRef, joiner: ActorRef): RaftData =
    LeaderData(
      members = Set(leader, joiner),
      nextIndex = Map(joiner → 3),
      matchIndex = Map(joiner → 0),
      CommonData(
        leader,
        currentTerm = 1,
        votedFor = None,
        eventLog = RaftEvent.emptyLog :+ MemberUp(1, leader) :+ MemberUp(1, joiner),
        commitIndex = 2,
        lastApplied = 2
      )
    )
}

sealed trait RaftData
case object AloneData extends RaftData
sealed trait ActiveRaftData extends RaftData {
  val members: Set[ActorRef]
  val common: CommonData
}
case class JoiningData(paht: ActorPath, retries: Int = 0) extends RaftData
case class FollowerData(members: Set[ActorRef], common: CommonData) extends ActiveRaftData
case class CandidateData(members: Set[ActorRef], common: CommonData) extends ActiveRaftData
case class LeaderData(members: Set[ActorRef], nextIndex: Map[ActorRef, Int], matchIndex: Map[ActorRef, Int], common: CommonData) extends ActiveRaftData {
  def afterJoin(joiner: ActorRef) =
    copy(common = common.copy(eventLog = common.eventLog :+ MemberUp(common.currentTerm, joiner)))
  def afterLeave(leaver: ActorRef) =
    copy(common = common.copy(eventLog = common.eventLog :+ MemberDown(common.currentTerm, leaver)))
}
case class LeavingData(members: Set[ActorRef], common: CommonData) extends ActiveRaftData
