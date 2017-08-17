package com.rgcase.raft

sealed trait RaftState
case object Alone extends RaftState
case object Joining extends RaftState
case object Follower extends RaftState
case object Candidate extends RaftState
case object Leader extends RaftState
case object Leaving extends RaftState