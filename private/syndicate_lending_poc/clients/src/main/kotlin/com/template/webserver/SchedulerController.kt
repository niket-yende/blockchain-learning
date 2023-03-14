//package com.template.webserver
//import com.template.util.QuartzScheduler.TaskScheduler
//import org.quartz.SchedulerException
//import org.slf4j.LoggerFactory
//import org.springframework.web.bind.annotation.*
//import javax.ws.rs.core.Response
//
//@RestController
//@RequestMapping("/api/schedule") // The paths for HTTP requests are relative to this base path.
//class SchedulerController (rpc: NodeRPCConnection) {
//
//    companion object {
//        private val logger = LoggerFactory.getLogger(RestController::class.java)
//    }
//
//    private val proxy = rpc.proxy
//
//    @PostMapping(value = "/start")
//    fun startScheduler(): Response {
//        println("Starting the scheduler")
//        //perform a vault query to get subscription state based on the subscriptionRef
//        val t: TaskScheduler = TaskScheduler()
//        try {
//            t.startJob()
//        } catch (se: SchedulerException ){
//            println("SchedulerException caught "+se)
//        }
//
//
//        val msg: String = "Scheduler started !!"
//        return Response.status(Response.Status.OK).entity(msg).build()
//    }
//
//    @PostMapping(value = "/stop")
//    fun stopScheduler(): Response {
//        println("Stopping the scheduler")
//        //perform a vault query to get bid state based on the subscriptionRef
//        val t: TaskScheduler = TaskScheduler()
//        try {
//            t.stopJob()
//        } catch (se: SchedulerException ){
//            println("SchedulerException caught "+se)
//        }
//
//        val msg: String = "Scheduler stoppped !!"
//        return Response.status(Response.Status.OK).entity(msg).build()
//    }
//
//}