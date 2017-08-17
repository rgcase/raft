package com.rgcase.raft

import akka.actor.{ ActorPath, LoggingFSM, Props }

import scala.concurrent.duration.FiniteDuration

object RaftActor {

  case object JoinTimeout
  val joinTimer = "join"

  case object ElectionTimeout
  val electionTimer = "election"

  case object HeartbeatTimeout
  val heartbeatTimer = "heartbeat"

  case class LeaveTimeout(retries: Int = 0)
  val leaveTimer = "leave"

  val timers = List(joinTimer, electionTimer, heartbeatTimer, leaveTimer)

  def props(
    joinTimeout: FiniteDuration,
    maxJoinRetries: Int,
    heartbeatTimeout: FiniteDuration,
    electionTimeout: FiniteDuration,
    leaveTimeout: FiniteDuration,
    seed: Option[ActorPath] = None
  ) = {
    Props(new RaftActor(joinTimeout, maxJoinRetries, heartbeatTimeout, electionTimeout, leaveTimeout, seed))
  }
}

class RaftActor(
    joinTimeout: FiniteDuration,
    maxJoinRetries: Int,
    heartbeatTimeout: FiniteDuration,
    electionTimeout: FiniteDuration,
    leaveTimeout: FiniteDuration,
    seed: Option[ActorPath]
) extends LoggingFSM[RaftState, RaftData] {

  import RaftActor._

  seed.fold {
    log.debug("RaftActor {} has no seed node. Starting in Alone state.", self.path)
    startWith(Alone, AloneData)
  } { path =>
    log.debug("RaftActor {} starting with seed node {}. Attempting to join.", self.path, path)
    contactSeed(path)
    setTimer(joinTimer, JoinTimeout, joinTimeout)
    startWith(Joining, JoiningData(path))
  }

  when(Alone) {
    case Event(Join, AloneData) =>
      log.debug("RaftActor {} in Alone state received Join from {}.", self.path, sender().path)
      goto(Leader) using RaftData.firstLeader(self, sender())
    case Event(AttemptLeave, AloneData) =>
      log.debug("RaftActor {} in Alone state received Leave. Shutting down.", self.path)
      stop(akka.actor.FSM.Shutdown)
  }

  when(Joining) {
    case Event(AppendEntries(term, _, _, entries, leaderCommit), _: JoiningData) =>
      log.debug("RaftActor {} in Joining state received first AppendEntries from {}, transitioning to Follower.", self.path, sender().path)
      val events = entries.map(_._2)
      sender() ! AppendResponse(term, success = true)
      goto(Follower) using
        FollowerData(
          RaftEvent.applyEvents(Set.empty, events.take(leaderCommit + 1): _*),
          CommonData(sender(), term, None, events, leaderCommit, leaderCommit)
        )
    case Event(JoinTimeout, JoiningData(path, retries)) =>
      if (retries > maxJoinRetries) {
        log.error("RaftActor {} failed to join the cluster after {} retries. Shutting down.", self.path, maxJoinRetries)
        stop(akka.actor.FSM.Shutdown)
      } else {
        log.debug("RaftActor {} in Joining state received JoinTimeout. Number of retries: {}.", self.path, retries + 1)
        contactSeed(path)
        setTimer(joinTimer, JoinTimeout, joinTimeout)
        stay using JoiningData(path, retries + 1)
      }
  }

  when(Follower) {
    case Event(Join, data: FollowerData) =>
      log.debug("RaftActor {} in Follower state received Join from {}, forwarding to Leader {}.", self.path, sender().path, data.common.leader.path)
      data.common.leader forward Join
      stay
    case Event(Leave, data: FollowerData) =>
      log.debug("RaftActor {} in Follower state received Leave from {}, forwarding to Leader {}.", self.path, sender().path, data.common.leader.path)
      data.common.leader forward Leave
      stay

  }

  when(Candidate) {
    case _ => stay
  }

  when(Leader) {
    case Event(Join, data: LeaderData) =>
      val afterJoinData = data.afterJoin(sender())
      val append = AppendEntries.createJoinResponse(afterJoinData)
      log.debug("RaftActor {} in Leader state received Join from {}. Replying with AppendEntries {}.", self.path, sender().path, append)
      sender() ! append
      stay using afterJoinData
    case Event(Leave, data: LeaderData) =>
      stay using data.afterLeave(sender())
    case Event(HeartbeatTimeout, data: LeaderData) =>
      data.members.foreach { member => member ! AppendEntries.heartbeatAppend(data, member) }
      stay
  }

  when(Leaving) {
    case Event(Join, data: FollowerData) =>
      log.debug("RaftActor {} in Follower state received Join from {}, forwarding to Leader {}.", self.path, sender().path, data.common.leader.path)
      data.common.leader forward Join
      stay
    case _ => stay
  }

  whenUnhandled {
    case Event(AttemptLeave, data: ActiveRaftData) =>
      log.debug("RaftActor {} in {} state received AttemptLeave. Sending Leave to leader {}.", self.path, stateName, data.common)
      data.common.leader ! Leave
      setTimer(leaveTimer, LeaveTimeout(), leaveTimeout)
      stay
    case Event(LeaveResponse(term), data: ActiveRaftData) if term >= data.common.currentTerm =>
      log.debug("RaftActor {} in {} state received LeaveResponse. Transitioning to ")
      goto(Leaving) using data
    case Event(event: RaftMessage, data) =>
      log.debug("RaftActor in state {} with data {} received unhandled RaftMessage {}.", stateName, data, event)
      stay
    case Event(msg, data) =>
      log.warning("RaftActor in state {} with data {} received unknown message {}.", stateName, data, msg)
      stay
  }

  onTransition {
    case Joining -> _ =>
      setElectionTimeout()
    case _ -> Leader =>
      setTimer(heartbeatTimer, HeartbeatTimeout, heartbeatTimeout, repeat = true)
    case Leader -> _ =>
      cancelTimer(heartbeatTimer)
  }

  onTermination {
    case _: StopEvent => timers.foreach(cancelTimer)
  }

  initialize()

  def contactSeed(path: ActorPath) = context.actorSelection(path) ! Join
  def setElectionTimeout() = setTimer(electionTimer, ElectionTimeout, electionTimeout)

}