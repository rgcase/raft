package com.rgcase.raft

import akka.actor.ActorRef

sealed trait RaftMessage

sealed trait RaftCommand extends RaftMessage
case object Join extends RaftCommand

case object AttemptLeave extends RaftCommand
case object Leave extends RaftCommand

case class LeaveResponse(term: Int) extends RaftMessage

object AppendEntries {
  def createJoinResponse(data: LeaderData): AppendEntries =
    AppendEntries(
      data.common.currentTerm,
      0,
      data.common.eventLog(0),
      data.common.eventLog.zipWithIndex.tail.map(_.swap),
      data.common.commitIndex
    )

  def heartbeatAppend(data: LeaderData, member: ActorRef): AppendEntries =
    AppendEntries(
      data.common.currentTerm,
      data.common.eventLog.size - 1,
      data.common.eventLog(data.common.eventLog.size - 1),
      Vector.empty,
      data.common.currentTerm
    )
}

case class AppendEntries(term: Int, prevLogIndex: Int, prevLogTerm: RaftEvent, entries: Vector[(Int, RaftEvent)], leaderCommit: Int) extends RaftMessage
case class AppendResponse(term: Int, success: Boolean) extends RaftMessage

case class RequestVote(term: Int, lastLogIndex: Int, lastLogTerm: RaftEvent) extends RaftMessage
case class VoteResponse(term: Int, voteGranted: Boolean) extends RaftMessage
