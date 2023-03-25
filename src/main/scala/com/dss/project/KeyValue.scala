package com.dss.project

import scala.collection.mutable.HashMap
import scala.concurrent.Await
import scala.concurrent.duration._

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout

import com.dss.project.RequestAPI._
import com.dss.project.Request
import com.dss.project.FileRequest._


object KeyValue {
    def props(StoreFile : ActorRef) = Props(new KeyValue(StoreFile))
}

class KeyValue(storeFile : ActorRef) extends Actor{
    val kvMap = new HashMap[String, String]()

    override def preStart() : Unit = { 
        storeFile ! GetValues
        context.become(waitingForData)
    }
  
    def waitingForData: Receive = {
        case ValueFound(key, value) =>
            println(s"Received $key")
            kvMap(key) = value
        case GetValuesFailure =>
            println("Dabase file is empty")
            context.unbecome()
        case GetValuesSuccess =>
            context.unbecome()
        case _ =>
            println("Error retreiving data from StoreFile")
            context.unbecome()
    }

    def receive = {
        case Store(key, value, requester) =>
            storeFile ! Request.Store(key, value)
            kvMap(key) = value
            sender ! StoreResponse("Record stored.", requester)
        case Delete(key, requester) =>
            storeFile ! Request.Delete(key)
            if(kvMap.contains(key)){
                kvMap.remove(key)
                sender ! DeleteResponse(s"$key deleted", requester)
            } else {
                sender ! DeleteResponse("Record not found", requester)
            }
        case Lookup(key, requester) =>
            val value = kvMap.get(key).getOrElse("")
            if(value.equals("deleted") || value.equals("")){
                sender ! LookupResponse("Value not found", requester)
            } else {
                sender ! LookupResponse(s"Key: $key, Value: $value", requester)
            }
        case Request.Store(key, value) =>
            storeFile ! Request.Store(key, value)
            kvMap(key) = value
        case Request.Delete(key) =>
            storeFile ! Request.Delete(key)
            kvMap.remove(key)
        case Request.Lookup(key) =>
            val value = kvMap.get(key).getOrElse("")
            if(!value.equals("")){
                sender ! value
            } else {
                sender ! None
            }
    }
}