package com.dss.project

import akka.actor.{Actor, ActorRef, Props, Terminated, ActorSystem}

object State {
    case object Connect
    case object Disconnect
    case object Disconnected
}

object Request {
    case class Store(key : String, value : String)
    case class Lookup(key : String)
    case class Delete(key : String)
    // case class SotreFailed(message: String)
    // case class StoreSuccess(message: String)
    // case class LookupFailed(message: String)
    // case class LookupSuccess(message: String)    
}

object RequestAPI {
    case class Store(key : String, value : String, requseter : ActorRef)
    case class Lookup(key : String, requseter : ActorRef)
    case class Delete(key : String, requseter : ActorRef)

    case class StoreResponse(message : String, requester : ActorRef)
    case class LookupResponse(message : String, requester : ActorRef)
    case class DeleteResponse(message : String, requester : ActorRef)
}

object Input {
    case class InputFailure(message : String)
    case object InputSuccess
}

object StoreServer {
    def props(Cache: ActorRef) = Props(new StoreServer(Cache))
}


class StoreServer(cache: ActorRef) extends Actor {
    import State._
    import Input._
    import Request._

    def processInput(input: String): Unit = {
        // This function should return something that we will send to the user
        // Example: Error 404
        input match {
            // Store
            case s if s.startsWith("store") =>
                if (s.length == 5){
                    sender ! InputFailure("Key and value not provided")
                }
                else {
                    val kv = s.substring(6).split(",", 2)
                    if(kv.length==0) {
                        sender ! InputFailure("Key can't be empty.")
                    }
                    else if(kv.length==1) {
                        sender ! InputFailure("Can't store empty values.") 
                    }
                    else {
                        cache ! RequestAPI.Store(kv(0), kv(1), sender)
                        // sender ! InputSuccess
                    }
                }
            // Lookup
            case s if s.startsWith("lookup") =>
                if (s.length == 6) {
                    sender ! InputFailure("Key not provided")
                }
                else {
                    cache ! RequestAPI.Lookup(s.substring(7), sender)
                    // sender ! InputSuccess
                }
            // Delete
            case s if s.startsWith("delete") =>
                if (s.length == 6){
                    sender ! InputFailure("Key not provided")
                }
                else {
                    cache ! RequestAPI.Delete(s.substring(7), sender)
                    // sender ! InputSuccess
                }
            
            case _ => 
                sender ! InputFailure("Unknown operationg please starts your request with store, lookup or delete")
        }
    }

    def receive = {
        case Disconnect =>
            println("Sotre system disconnecting")
            context.stop(self)
            context.system.terminate() // NOTE: Remove it 
        case input : String =>
            processInput(input)
        case Store(key: String, value: String) =>
            cache ! RequestAPI.Store(key, value, sender)
        case Delete(key: String) =>
            cache ! RequestAPI.Delete(key, sender)
        case Lookup(key: String) =>
            cache ! RequestAPI.Lookup(key, sender)
        case RequestAPI.LookupResponse(message, requester) =>
            requester ! RequestAPI.LookupResponse(message, requester)
        case RequestAPI.DeleteResponse(message, requester) =>
            requester ! RequestAPI.DeleteResponse(message, requester)
        case RequestAPI.StoreResponse(message, requester) =>
            requester ! RequestAPI.StoreResponse(message, requester)
        case _ => println("Not expected behaviour")
    }
}