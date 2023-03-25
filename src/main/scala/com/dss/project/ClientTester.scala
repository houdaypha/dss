package com.dss.project

import scala.util.Random
import akka.actor.{Actor, ActorRef, Props}

import com.dss.project.Request._
import com.dss.project.RequestAPI

object ClientTester{
    case object Operate
    case object Populate
    def props(Store: ActorRef) = Props(new ClientTester(Store))
}

class  ClientTester(store: ActorRef) extends Actor {
    import ClientTester._
    
    val numOperations: Int = 1000
    val random = new Random()
    val operations = Seq("store", "delete", "lookup")
    var startTime:Long = 0

    override def preStart() = {
        self ! Populate
    }

    def operate() = {
        var lookupKey = 1
        for (i <- 1 to numOperations) {
            println("Operation " + i)
            val randomOperation = operations(random.nextInt(operations.length))
            randomOperation match {
                case "store" =>
                    val storeKey = random.nextInt(500) + 501
                    store ! Store(storeKey.toString(), "Value " + storeKey.toString())
                    // perform store operation
                case "delete" =>
                    val deleteKey = random.nextInt(500) + 501
                    store ! Delete(deleteKey.toString())
                case "lookup" =>
                    // 50% Bias towards last key looked up
                    if(random.nextBoolean()){
                        // 90% Bias to 1-500
                        if (random.nextDouble() < 0.9) {
                            lookupKey = random.nextInt(500) + 1
                        } else {
                            lookupKey = random.nextInt(500) + 501
                        }
                        store ! Lookup(lookupKey.toString())
                    } else {
                        // use the last key
                        store ! Lookup(lookupKey.toString())
                    }
            }
        }
    }

    def stopTimer(startTime: Long) = {
        val endTime = System.currentTimeMillis()
        val elapsedTime = (endTime - startTime) / 1000000000.0
        println("Time needed for " + numOperations + " operation is " + 
            elapsedTime + " second(s)")
    }

    // override def postStop() = {
    //     stopTimer()
    // }

    def receive = {
        case Populate =>
            for (i <- 1 to 500) {
                store ! Store(i.toString(), "Value " + i.toString())
            }
            startTime = System.currentTimeMillis()
            self ! Operate
        case Operate => 
            operate()
            stopTimer(startTime)
        case _ => // do nothing
    }
}