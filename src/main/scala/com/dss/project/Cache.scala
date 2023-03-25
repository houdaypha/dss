package com.dss.project

import scala.collection.mutable.HashMap
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.annotation.switch
import scala.collection.mutable

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout

import com.dss.project.RequestAPI._
import com.dss.project.Request


class LFUCache(capacity: Int) {
  case class CacheEntry(value: String, frequency: Int, var timestamp: Long)

  private val cache = mutable.Map.empty[String, CacheEntry]
  private var minFrequency = 1
  private var size = 0

  def get(key: String) : (Boolean, String)= {
    cache.getOrElse(key, None) match {
      case CacheEntry(value, frequency ,timestamp) =>
        cache(key) = CacheEntry(value, frequency + 1, System.currentTimeMillis())
        return (true, value)
      case None => return (false, null)
    }
  }

  def put(key: String, value: String): Unit = {
    if (capacity == 0) return

    cache.getOrElse(key, None) match {
      case CacheEntry(value, frequency ,timestamp) =>
        cache(key) = CacheEntry(value, frequency + 1, System.currentTimeMillis())
      case None =>
        if (size == capacity) {
          val minEntries = cache.filter(_._2.frequency == minFrequency)
          val oldestEntry = minEntries.minBy(_._2.timestamp)._1
          cache.remove(oldestEntry)
          size -= 1
        }
        val entry = CacheEntry(value, 1, System.currentTimeMillis())
        cache.put(key, entry)
        size += 1
        minFrequency = 1
    }
  }

  def remove(key: String): Boolean = {
        if(cache.contains(key)){
            val minEntries = cache.filter(_._2.frequency == minFrequency)
            val oldestEntry = minEntries.minBy(_._2.timestamp)._1
            cache.remove(oldestEntry)
            size -= 1
            return true
        }
        return false
    }
}

object Cache {
    def props(KeyValue : ActorRef) = Props(new Cache(KeyValue))
}

class Cache(keyValue : ActorRef) extends Actor{
    implicit val timeout: Timeout = Timeout(5.seconds)
    val cache = new LFUCache(1000)

    def receive = {

        case Store(key, value, requester) =>
            keyValue ! Request.Store(key, value)
            cache.put(key, value)
            sender ! StoreResponse("Record stored.", requester)
            // cache.print()

        case Delete(key, requester) =>
            val result = cache.remove(key)
            if(result){
                keyValue ! Request.Delete(key)
                sender ! DeleteResponse(s"$key deleted", requester)
            } else {
                val future = keyValue ? Delete(key, requester)
                val result = Await.result(future, 100.seconds)
                sender ! result // Send result to the store server
            }

        case Lookup(key, requester) =>
            val (found, value) = cache.get(key)
            if (found) {
                sender ! LookupResponse(s"Key: $key, Value: $value", requester)
            } else {
                val future = keyValue ? Request.Lookup(key)
                val result = Await.result(future, 100.seconds)
                result match {
                    case value : String =>
                        cache.put(key, value)
                        sender ! LookupResponse(s"Key: $key, Value: $value", requester)
                    case None =>
                        sender ! LookupResponse("Value not found", requester)
                }
            }
            // cache.print()
    }
}