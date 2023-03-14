package com.template.webserver

import com.template.Models.TermSheetModel
import com.template.flows.*
import com.template.schema.TermSheetSchema1
import com.template.states.TermSheetState
import net.corda.core.contracts.StateAndRef
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.transactions.SignedTransaction
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.HashMap
import java.util.jar.JarInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

class KeyDataModel {
    val issuerName: String = ""
    val entityType: String = ""
    val syndicateType: String = ""
    val loanType: String = ""
    val tenure: Int = 0
}

@RestController
@RequestMapping("/api/termSheet") // The paths for HTTP requests are relative to this base path.
class TermSheetController (val rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

//    private val proxy = rpc.proxy

    /**
     * Uploads the attachment at [attachmentPath] to the node.
     */
    @PostMapping("/upload",consumes = arrayOf(org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE))
    fun uploadTermSheet( @RequestParam file: MultipartFile, @RequestParam loanRef: String,@RequestHeader(value = "PartyName") callingParty: String):  LinkedHashMap<String,Any>  {
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps

        val pathName=System.getProperty("user.dir")+"/src/main/resources/"

        println("pathName "+pathName)

        println("user dir "+System.getProperty("user.dir"))

//        println("current path "+Paths.get("").toAbsolutePath().toString())

        val response: JSONObject = JSONObject()
        var bytes = file.bytes
        var path: Path = Paths.get( pathName+ file.originalFilename)
        Files.write(path, bytes)

        //ZIP the downloaded file
        var downloadedFile = File(pathName + file.originalFilename)
        var zipFileName: String = downloadedFile.name + ".zip"
        print("file ------------------ " + zipFileName)

        var fos = FileOutputStream(pathName + zipFileName)
        var zos = ZipOutputStream(fos)

        zos.putNextEntry(ZipEntry(downloadedFile.name))

        var bytes2 = Files.readAllBytes(Paths.get(pathName + downloadedFile.name))
        zos.write(bytes2, 0, bytes2.size)
        zos.closeEntry()
        zos.close()

        fos.close()

        val attachmentUploadInputStream = File(pathName+ zipFileName).inputStream()
        val myHash = proxy.uploadAttachment(attachmentUploadInputStream).toString()
//        return "File Uploaded and Zipped!!! SecureHash = " + myHash

        if (myHash.isNullOrEmpty()) {
            return linkedMapOf(Pair("status",Response.Status.BAD_REQUEST), Pair("message","Term sheet file not uploaded successfully, hash: "+ myHash), Pair("data",{}))
        }

        val leadArranger = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse("O=LeadArranger,L=New York,C=US")) as Party

        val signedTx: SignedTransaction = proxy.startTrackedFlowDynamic(CreateTermSheetFlow::class.java,loanRef,myHash,leadArranger).returnValue.get()
        println("signedtransaction: " + signedTx)

        var loanRefObject = HashMap<String,Any>()
        loanRefObject["loanRef"] = loanRef
//        loanRefObject["fileHash"] = myHash

        return linkedMapOf(Pair("status","To be approved"), Pair("message","Term sheet file uploaded successfully"), Pair("data",loanRefObject))
    }

    /**
     * Downloads the attachment with hash [attachmentHash] from the node.
     */
    @GetMapping("/download/{loanRef}", produces = arrayOf(MediaType.MULTIPART_FORM_DATA))
    fun downloadTermSheet(@PathVariable("loanRef") loanRef: String,@RequestHeader(value = "PartyName") callingParty: String): ResponseEntity<InputStreamResource> {
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps

        println("loanRef "+loanRef)
        //get the file hash based on loanRef
        //Check if the loanRef is present in the staticDataState
        val loanIdCriteria = builder { TermSheetSchema1.PersistentTermSheet::loan_ref.equal(loanRef) }
        val termSheet_criteria = QueryCriteria.VaultCustomQueryCriteria(loanIdCriteria)
        val termSheetStates = proxy.vaultQueryByCriteria(termSheet_criteria, TermSheetState::class.java).states
        if (termSheetStates.size == 0)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()

        var termSheetState = termSheetStates.get(0).state.data

        var fileHash = termSheetState.fileHash

        println("Term sheet file received " + fileHash)

//        val PathName="../../../../resources"
        val pathName=System.getProperty("user.dir")+"/src/main/resources/"
        println("pathName in download "+pathName)

//        val file = File(pathName+"noFile.pdf").createNewFile()

        if (proxy.attachmentExists(SecureHash.parse(fileHash))) {
            print("hash exists ---")


            val inputJar = proxy.openAttachment(SecureHash.parse(fileHash))

            var file_name_data: Pair<String, ByteArray>? = null
            JarInputStream(inputJar).use { jar ->
                while (true) {
                    val nje = jar.nextEntry ?: break
                    if (nje.isDirectory) {
                        continue
                    }
                    file_name_data = Pair(nje.name, jar.readBytes())
                }
            }



            val file = File(pathName+file_name_data?.first)

            val bytes=file_name_data?.second as ByteArray
            file.writeBytes(bytes)

            val resource: InputStreamResource = InputStreamResource(FileInputStream(file))


            var headers = HttpHeaders()

            headers.add("Content-Disposition", String.format("attachment; filename=\"%s\"", file.name))
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate")
            headers.add("Pragma", "no-cache")
            headers.add("Expires", "0")

            var responseEntity: ResponseEntity<InputStreamResource> = ResponseEntity.ok().headers(headers).contentLength(file.length()).contentType(org.springframework.http.MediaType.parseMediaType(MediaType.MULTIPART_FORM_DATA)).body(resource)


            return responseEntity

        }


        return ResponseEntity.noContent().build()
    }
//    private fun downloadAttachment(attachmentHash: SecureHash): JarInputStream {
//        val attachmentDownloadInputStream = proxy.openAttachment(attachmentHash)
//        return JarInputStream(attachmentDownloadInputStream)
//    }

    @GetMapping("/getAll",produces = arrayOf("application/json"))
    private fun getAllTermSheet(@RequestHeader(value = "PartyName") callingParty: String): List<TermSheetModel>{
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        var allRecords: List<StateAndRef<TermSheetState>> = proxy.vaultQuery(TermSheetState::class.java).states
        var records : MutableList<TermSheetModel> = mutableListOf()
        for (i in allRecords){
            var record = i.state.data
            var recordStatus: String = ""
            if (record.status == TermSheetState.TermSheetStatus.CREATED){
                recordStatus = "To be approved"
            } else if (record.status == TermSheetState.TermSheetStatus.APPROVED){
                recordStatus = "Approved by Borrower"
            }

            records.add(TermSheetModel(record.loanRef,record.borrower.name.organisation.toString(),record.typeOfLoan,record.leadArranger.name.organisation.toString(),recordStatus,record.fileHash))
        }
        return records
    }

    @GetMapping("/getById/{id}",produces = arrayOf("application/json"))
    private fun getAllTermSheetById(@PathVariable("id") id:String,@RequestHeader(value = "PartyName") callingParty: String): TermSheetModel{
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        val loadIdCriteria = builder { TermSheetSchema1.PersistentTermSheet::loan_ref.equal(id) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(loadIdCriteria)
        val termSheetStates = proxy.vaultQueryByCriteria(criteria, TermSheetState::class.java).states
        val record = termSheetStates.get(0).state.data
        var recordStatus: String = ""
        if (record.status == TermSheetState.TermSheetStatus.CREATED){
            recordStatus = "To be approved"
        } else if (record.status == TermSheetState.TermSheetStatus.APPROVED){
            recordStatus = "Approved by Borrower"
        }
        return TermSheetModel(record.loanRef,record.borrower.name.organisation.toString(),record.typeOfLoan,record.leadArranger.name.organisation.toString(),recordStatus,record.fileHash)
    }

    @GetMapping("/getHistoryById/{id}",produces = arrayOf("application/json"))
    private fun getTermSheetHistoryById(@PathVariable("id") id:String,@RequestHeader(value = "PartyName") callingParty: String): List<TermSheetModel>{
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        val loadIdCriteria = builder { TermSheetSchema1.PersistentTermSheet::loan_ref.equal(id) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(loadIdCriteria, Vault.StateStatus.ALL)
        val termSheetStates = proxy.vaultQueryByCriteria(criteria, TermSheetState::class.java).states
        var records : MutableList<TermSheetModel> = mutableListOf()
        for (i in termSheetStates){
            var record = i.state.data
            var recordStatus: String = ""
            if (record.status == TermSheetState.TermSheetStatus.CREATED){
                recordStatus = "To be approved"
            } else if (record.status == TermSheetState.TermSheetStatus.APPROVED){
                recordStatus = "Approved by Borrower"
            }
            records.add(TermSheetModel(record.loanRef,record.borrower.name.organisation.toString(),record.typeOfLoan,record.leadArranger.name.organisation.toString(),recordStatus,record.fileHash))
        }
        return records
    }

    @GetMapping("/getAllApprovedTermSheets/{borrower}",produces = arrayOf("application/json"))
    private fun getApprovedTermSheets(@PathVariable("borrower") borrower:String,@RequestHeader(value = "PartyName") callingParty: String): List<TermSheetModel>{
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        //val borrowerParty = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse("O=LeadArranger,L=New York,C=US")) as Party
        //Hardcoding the borrower name as cordaX500Name
        println("borrower name : "+borrower)

//        val borrowerName = "O=Borrower,L=London,C=GB"
        val loadIdCriteria = builder { TermSheetSchema1.PersistentTermSheet::status.equal(TermSheetState.TermSheetStatus.APPROVED.toString()) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(loadIdCriteria)
        val termSheetStates = proxy.vaultQueryByCriteria(criteria, TermSheetState::class.java).states
        println("termSheetStates.size : "+termSheetStates.size)
        var records : MutableList<TermSheetModel> = mutableListOf()
        if (termSheetStates.size > 0) {
            for (i in termSheetStates){
                var record = i.state.data
                var recordStatus: String = ""
                if (record.status == TermSheetState.TermSheetStatus.CREATED){
                    recordStatus = "To be approved"
                } else if (record.status == TermSheetState.TermSheetStatus.APPROVED){
                    recordStatus = "Approved by Borrower"
                }
                records.add(TermSheetModel(record.loanRef,record.borrower.name.organisation.toString(),record.typeOfLoan,record.leadArranger.name.organisation.toString(),recordStatus,record.fileHash))
            }
        }
        return records
    }


    @GetMapping("/getLoanIds/{borrower}",produces = arrayOf("application/json"))
    private fun getLoanIds(@PathVariable("borrower") borrower:String,@RequestHeader(value = "PartyName") callingParty: String): List<String>{
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        //val borrowerParty = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse("O=LeadArranger,L=New York,C=US")) as Party
        //Hardcoding the borrower name as cordaX500Name
        println("borrower name : "+borrower)

//        val borrowerName = "O=Borrower,L=London,C=GB"
        val loadIdCriteria = builder { TermSheetSchema1.PersistentTermSheet::status.equal(TermSheetState.TermSheetStatus.CREATED.toString()) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(loadIdCriteria)
        val termSheetStates = proxy.vaultQueryByCriteria(criteria, TermSheetState::class.java).states
        println("termSheetStates.size : "+termSheetStates.size)
        var loanRefs : MutableList<String> = mutableListOf()
        if (termSheetStates.size > 0) {
            for (i in termSheetStates){
                var record = i.state.data
                loanRefs.add(record.loanRef)
            }
        }
        return loanRefs
    }

    @PutMapping("/approveByBorrower")
    private fun approveTermSheet(@RequestBody name: LoanRef,@RequestHeader(value = "PartyName") callingParty: String): LinkedHashMap<String,Any>{
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
//        if (name == null)
//            return Response.status(Response.Status.BAD_REQUEST).entity("The Loan Ref cannot be null").build()
        val signedTx: SignedTransaction = proxy
                .startTrackedFlowDynamic(ApproveTermSheetFlow::class.java,name.loanRef).returnValue.get()
//        return Response.status(Response.Status.ACCEPTED).entity("Term sheet Approved").build()
        return linkedMapOf(Pair("status","Approved by Borrower"), Pair("message","Term sheet Approved"))
    }

    //    @PostMapping(value = "/addKeyData")
//    fun addKeyData(@RequestBody keyData: KeyDataModel): Response {
////        try {
//        println("~~~Input State" + keyData)
//        val msg: String = "Key data added succesfully"
//        return Response.status(Response.Status.CREATED).entity(msg).build()
//
//    }

//    @PostMapping(value = "/uploadTermSheet1", consumes = arrayOf(org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE))
//    fun uploadTermSheet(@QueryParam("loanRef") loanRef: String, @RequestParam file: MultipartFile): String {
////        println("Term sheet file received "+termSheetFile.inputStream.toString())
////
////        var zipFileName: String = termSheetFile.name + ".zip"
////        print("file ------------------ " + zipFileName)
////        var fos = FileOutputStream( zipFileName)
////        var zos = ZipOutputStream(fos)
////
////        zos.putNextEntry(ZipEntry(termSheetFile.name))
////
////        termSheetFile.inputStream.bufferedReader().use {
////            print(it.readLine()+"---********-------")
////        }
////
////        var bytes2 = termSheetFile.bytes
////        print(bytes2.toString())
////        zos.write(bytes2, 0, bytes2.size)
//
//
//        var bytes = file.bytes
//        var path: Path = Paths.get("/Users/venkat.bandaru/Downloads/" + file.originalFilename)
//        Files.write(path, bytes)
//
//        //ZIP the downloaded file
//        var downloadedFile = File("/Users/venkat.bandaru/Downloads/" + file.originalFilename)
//        var zipFileName: String = downloadedFile.name + ".zip"
//        print("file ------------------ " + zipFileName)
//
//        var fos = FileOutputStream("/Users/venkat.bandaru/Downloads/" + zipFileName)
//        var zos = ZipOutputStream(fos)
//
//        zos.putNextEntry(ZipEntry(downloadedFile.name))
//
//        var bytes2 = Files.readAllBytes(Paths.get("/Users/venkat.bandaru/Downloads/" + downloadedFile.name))
//        zos.write(bytes2, 0, bytes2.size)
//        zos.closeEntry()
//        zos.close()
//
//        fos.close()
//
//        val attachmentUploadInputStream = File("/Users/venkat.bandaru/Downloads/" + zipFileName).inputStream()
//        val myHash = proxy.uploadAttachment(attachmentUploadInputStream)
////        return "File Uploaded and Zipped!!! SecureHash = " + myHash
//
//
//        print(myHash)
//
//        val termSheet = TermSheetState(loanRef, proxy.wellKnownPartyFromX500Name(CordaX500Name.parse("O=Borrower,L=London,C=GB")) as Party, "",
//                proxy.wellKnownPartyFromX500Name(CordaX500Name.parse("O=LeadArranger,L=New York,C=US")) as Party,
//                TermSheetState.TermSheetStatus.CREATED, 25000.0, , myHash)
//
//        val signedTx = proxy.startTrackedFlowDynamic(CreateTermSheetFlow::class.java, termSheet).returnValue.get()
//
//
//
//        return "File Uploaded and Zipped!!!  "
//
//    }
//
//
//    //
//    @GetMapping(value = "/getTermSheet", produces = arrayOf(MediaType.MULTIPART_FORM_DATA))
//    fun getTermSheet(@QueryParam("loanRef") fileHash: String): ResponseEntity<InputStreamResource> {
//        println("Term sheet file received " + fileHash)
//
//        val termSheetCriteria = builder { TermSheetSchema1.PersistentTermSheet::file_hash.equal(fileHash) }
//        val static_criteria = QueryCriteria.VaultCustomQueryCriteria(termSheetCriteria)
//        val termSheetStates = proxy.vaultQueryByCriteria(static_criteria, TermSheetState::class.java).states
//        if (termSheetStates.size == 0)
//            throw error("no termsheet with loan ref")
//
//        val termSheet = termSheetStates.get(0).state.data as TermSheetState
//
//        val hash = termSheet.fileHash
//
//        val file = File("/Users/venkat.bandaru/Downloads/OutputFile.pdf")
//        if (proxy.attachmentExists(hash)) {
//            print("hash exists ---")
//
////        val contents=proxy.openAttachment(termSheet.fileHash).copyTo(Paths.get("/Users/venkat.bandaru/Downloads/OutputFile.txt"))
////        bufferedReader().use {
////            var line=it.readLine()
////            file.appendText(line.toString())
////            print(line.toString())
////            file.appendText("/n")
////        }
//
//
//            val inputJar = proxy.openAttachment(termSheet.fileHash)
//
//            var file_name_data: Pair<String, ByteArray>? = null
//            JarInputStream(inputJar).use { jar ->
//                while (true) {
//                    val nje = jar.nextEntry ?: break
//                    if (nje.isDirectory) {
//                        continue
//                    }
//                    file_name_data = Pair(nje.name, jar.readBytes())
//                }
//            }
//
//
////            val attachmentJar = JarInputStream(inputJar)
//
////            print(attachmentJar.nextEntry)
//
////            val content = attachmentJar.bufferedReader().readText()
//
//            val file = File("/Users/venkat.bandaru/Desktop/"+file_name_data?.first)
//
//            val bytes=file_name_data?.second as ByteArray
////        val file = File("OutputFile.txt")
//            file.writeBytes(bytes)
//
//        }
//
//
//        val resource: InputStreamResource = InputStreamResource(FileInputStream(file))
//
//
//        var headers = HttpHeaders()
//
//        headers.add("Content-Disposition", String.format("attachment; filename=\"%s\"", file.name))
//        headers.add("Cache-Control", "no-cache, no-store, must-revalidate")
//        headers.add("Pragma", "no-cache")
//        headers.add("Expires", "0")
//
//        var responseEntity: ResponseEntity<InputStreamResource> = ResponseEntity.ok().headers(headers).contentLength(file.length()).contentType(org.springframework.http.MediaType.parseMediaType(MediaType.MULTIPART_FORM_DATA)).body(resource)
//
//
////    return Response.status(Response.Status.CREATED).header("Content-Disposition",String.format("attachment; filename=\"%s\"",file.name)).entity(resource).build()
//
////    print("-----************----")
//
//
////    val attachmentJar = JarInputStream(attachmentDownloadInputStream)
//
////    while (attachmentJar.nextEntry.name != null) {
////        attachmentJar.nextEntry
////    }
//
//
////    return "success"
//
////
////    @PostMapping(value = "/approveTermSheet")
////    fun approveTermSheet(@RequestParam status: String): Response {
////        println("approval status "+status)
////        //update the status for term sheet
////        return Response.status()
////    }
//    return responseEntity
//}


}
//}
