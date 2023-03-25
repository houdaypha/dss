package com.dss.project

import java.nio.file.{Files, Paths}
import java.io.File
import java.io.FileWriter
import java.io.Writer

import scala.io.Source
import scala.collection.mutable.HashMap

import akka.actor.{Actor, ActorRef, Props}

import com.dss.project.Request._

object FileRequest{
    case object GetValues
    case class ValueFound(key : String, value : String)
    case object GetValuesFailure
    case object  GetValuesSuccess
}

object StoreFile {
    def props(filePath: String) = Props(new StoreFile(filePath))
}

class StoreFile(filePath: String) extends Actor {
    import FileRequest._

    def createFile() = {
        if (!Files.exists(Paths.get(filePath))) {
            println(s"Store file $filePath doesn't exist, creating it...")
            Files.createFile(Paths.get(filePath))
        }
    }

    def storeLine(line: String) = {
        val file = new File(filePath)
        val writer = new FileWriter(file, true)
        writer.write(line)
        writer.write("\n")
        writer.close()
    }

    def deleteLine(key : String) = {
        val (found, value) = fileLookUp(key)
        if (found) {
            val line = key + ",deleted"
            storeLine(line)
        }
    }

    def fileLookUp(key: String) : (Boolean, String) = {
        val file = Source.fromFile(filePath)
        val lines = file.getLines.toList.reverse
        file.close()
        if(!lines.isEmpty) {
            for (line <- lines) {
                val parts = line.split(",")
                if (parts(0).equals(key)) {
                    if (parts(1).equals("deleted")){
                        return (false, null)
                    } else {
                        return (true, parts(1))
                    }
                }
            }
        }
        return (false, null)
    }

    def getValues() = {
        val kvMap = new HashMap[String, String]()
        val file = Source.fromFile(filePath)
        val lines = file.getLines.toList.reverse
        var keySet = Set.empty[String]
        var found = false
        file.close()
        if(!lines.isEmpty) {
            for (line <- lines) {
                val parts = line.split(",")
                if(!keySet.contains(parts(0))){
                    keySet += parts(0)
                    if (!parts(1).equals("deleted")){
                        // println("Sending " + parts(0))
                        sender() ! ValueFound(parts(0), parts(1))
                        found = true
                    }
                }
            }
        }
        
        // Send GetValuesFailure when file is empty
        if (!found) sender() ! GetValuesFailure else sender() ! GetValuesSuccess
    }

    override def preStart() = {
        println(s"My URL: ${context.self.path}")
        createFile()
    }
    
    def receive = {
        case Store(key, value) =>
            val line = key + "," + value
            storeLine(line)
        case Delete(key) =>
            deleteLine(key)
        case Lookup(key) =>
            val (found, value) = fileLookUp(key)
            if (found) println(s"From store file: $value")
            // else println("Not found")
        case GetValues =>
            getValues()
        case _ => println("Request type not supported by StoreFile actor")

    }
}