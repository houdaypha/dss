package com.dss.project

import scala.io.StdIn._
import akka.actor.{Actor, ActorRef, Props}

import com.dss.project.State.Disconnect
import com.dss.project.Input._
import com.dss.project.RequestAPI._

object StoreInterface {
    case object Check
    def props(Store: ActorRef) = Props(new StoreInterface(Store))
}

class StoreInterface(store: ActorRef) extends Actor {
    import StoreInterface._
    
    override def preStart() = {
        println(
            "=" * 30 + " WELCOME TO YOUR STORING SYSTEM " + "=" * 30 + "\n" +
            "You are connected to the store system. \n" +
            "Use 'disconnect' to disconnect. \n" +
            "Use \"store 'key','value'\" to store an input.\n" +
            "Use \"delete 'key'\" to delete an input.\n" +
            "Use \"lookup 'key'\" to lookup for an input.\n")
        self ! Check
    }

    def receive = {
        case Check =>
            readLine() match {
                case "disconnect" => 
                    store ! Disconnect
                    println("Input interface disconnecting")
                    context.stop(self)
                case input:String =>
                    store ! input
                case _ => 
                    println("Input type not recognised")
                    self ! Check
            }
        case InputFailure(message : String) =>
            println("Input Error: " + message)
            self ! Check
        case LookupResponse(message, requester) =>
            println("Lookup: " + message)
            self ! Check
        case StoreResponse(message, requester) =>
            println("Store: " + message)
            self ! Check
        case DeleteResponse(message, requester) =>
            println("Delete: " + message)
            self ! Check
    }
}