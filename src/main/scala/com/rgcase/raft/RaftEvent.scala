package com.rgcase.raft

import akka.actor.ActorRef

object RaftEvent {
  val emptyLog = Vector(ClusterStart)
  def applyEvents(members: Set[ActorRef], events: RaftEvent*) =
    events.foldLeft(members) {
      case (acc, ClusterStart)          => acc
      case (acc, MemberUp(_, member))   => acc + member
      case (acc, MemberDown(_, member)) => acc - member
    }
}

sealed trait RaftEvent { val term: Int }
case object ClusterStart extends RaftEvent { val term = 0 }
case class MemberUp(term: Int, member: ActorRef) extends RaftEvent
case class MemberDown(term: Int, member: ActorRef) extends RaftEvent
