package com.dss.project

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.Cluster
import akka.cluster.MemberStatus
import akka.actor.ActorSelection
import akka.util.Timeout
import akka.actor.ActorPath
import scala.util.{Success, Failure}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global


object StoreApplication extends App {
  val path = "db.txt"
  val as = ActorSystem("Store")
  val storefile = as.actorOf(StoreFile.props(path), "StoreFile")
  val kv = as.actorOf(KeyValue.props(storefile), "KeyValue")
  val cache = as.actorOf(Cache.props(kv), "Cache")
  val store = as.actorOf(StoreServer.props(cache), "StoreServer")
  val systemInterface = as.actorOf(StoreInterface.props(store), "StoreInterface")
  // val client = as.actorOf(ClientTester.props(store), "ClientTester")
}


object StoreFileApplication extends App {
  val conf = """
    akka {
      actor {
        provider = "akka.remote.RemoteActorRefProvider"
      }
      remote {
        artery {
          transport = tcp # See Selecting a transport below
          canonical.hostname = "127.0.0.1"
          canonical.port = 25520
        }
      }
    }
    akka.actor.allow-java-serialization = on
  """
  val config = ConfigFactory.parseString(conf)
  val path = "db.txt"
  val as = ActorSystem("Store", config)
  val storefile = as.actorOf(StoreFile.props(path), "storeFile")
}

object KeyValueApplication extends App {
  val conf = """
    akka {
      actor {
        provider = "akka.remote.RemoteActorRefProvider"
      }
      remote {
        artery {
          transport = tcp # See Selecting a transport below
          canonical.hostname = "127.0.0.1"
          canonical.port = 25530
        }
      }
    }
    akka.actor.allow-java-serialization = on
  """
  val config = ConfigFactory.parseString(conf)
  val as = ActorSystem("Store", config)
  implicit val dspch = as.dispatcher
  val storeFile = "akka://Store@127.0.0.1:25520/user/storeFile"
  as.actorSelection(storeFile).resolveOne(3.seconds).onComplete  {
    case Success(value: ActorRef) =>
        as.actorOf(KeyValue.props(value), "keyValue")
    case Failure(e) => 
      println("Retreiving storeFile Actor failure: " + e)
  }

}

object CacheApplication extends App {
  val conf = """
    akka {
      actor {
        provider = "akka.remote.RemoteActorRefProvider"
      }
      remote {
        artery {
          transport = tcp # See Selecting a transport below
          canonical.hostname = "127.0.0.1"
          canonical.port = 25540
        }
      }
    }
    akka.actor.allow-java-serialization = on
  """
  val config = ConfigFactory.parseString(conf)
  val as = ActorSystem("Store", config)
  implicit val dspch = as.dispatcher
  val kv = "akka://Store@127.0.0.1:25530/user/keyValue"
  as.actorSelection(kv).resolveOne(10.seconds).onComplete  {
    case Success(value: ActorRef) =>
        as.actorOf(Cache.props(value), "cache")
    case Failure(e) => 
      println("Retreiving keyValue Actor failure: " + e)
  }
}

object StoreServerApplication extends App {
  val conf = """
    akka {
      actor {
        provider = "akka.remote.RemoteActorRefProvider"
      }
      remote {
        artery {
          transport = tcp # See Selecting a transport below
          canonical.hostname = "127.0.0.1"
          canonical.port = 25550
        }
      }
    }
    akka.actor.allow-java-serialization = on
  """
  val config = ConfigFactory.parseString(conf)
  val as = ActorSystem("Store", config)
  implicit val dspch = as.dispatcher
  val cache = "akka://Store@127.0.0.1:25540/user/cache"
  as.actorSelection(cache).resolveOne(10.seconds).onComplete  {
    case Success(value: ActorRef) =>
        as.actorOf(StoreServer.props(value), "storeServer")
    case Failure(e) => 
      println("Retreiving keyValue Actor failure: " + e)
  }
}


object InterfaceApplication extends App {
  val conf = """
    akka {
      actor {
        provider = "akka.remote.RemoteActorRefProvider"
      }
      remote {
        artery {
          transport = tcp # See Selecting a transport below
          canonical.hostname = "127.0.0.1"
          canonical.port = 25560
        }
      }
    }
    akka.actor.allow-java-serialization = on
  """
  val config = ConfigFactory.parseString(conf)
  val as = ActorSystem("Store", config)
  implicit val dspch = as.dispatcher
  val cache = "akka://Store@127.0.0.1:25550/user/storeServer"
  as.actorSelection(cache).resolveOne(10.seconds).onComplete  {
    case Success(value: ActorRef) =>
        as.actorOf(StoreInterface.props(value), "storeInterface")
    case Failure(e) => 
      println("Retreiving keyValue Actor failure: " + e)
  }
}