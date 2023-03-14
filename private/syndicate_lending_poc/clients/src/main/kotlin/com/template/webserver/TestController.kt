package com.template.webserver

import net.corda.core.crypto.SecureHash
import net.corda.core.messaging.CordaRPCOps
import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@RestController
@RequestMapping("testapi/")
class TestController(val rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

//    private val proxy = rpc.proxy

    @GetMapping(value = "/templateendpoint", produces = arrayOf("text/plain"))
    private fun templateendpoint(): String {
        return "Define an endpoint here."
    }

    @PostMapping(value = "/uploadFile", consumes = arrayOf(MediaType.MULTIPART_FORM_DATA_VALUE))
    private fun uplaodFile(@RequestParam("file") file: MultipartFile,@RequestHeader(value = "PartyName") callingParty: String): String {
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        var bytes = file.bytes
        var path: Path = Paths.get("/Users/anurag07/Downloads/" + file.originalFilename)
        Files.write(path, bytes)

        //ZIP the downloaded file
        var downloadedFile = File("/Users/anurag07/Downloads/" + file.originalFilename)
        var zipFileName: String = downloadedFile.name + ".zip"
        print("file ------------------ " + zipFileName)

        var fos = FileOutputStream("/Users/anurag07/Downloads/" + zipFileName)
        var zos = ZipOutputStream(fos)

        zos.putNextEntry(ZipEntry(downloadedFile.name))

        var bytes2 = Files.readAllBytes(Paths.get("/Users/anurag07/Downloads/" + downloadedFile.name))
        zos.write(bytes2, 0, bytes2.size)
        zos.closeEntry()
        zos.close()

        fos.close()

        val attachmentUploadInputStream = File("/Users/anurag07/Downloads/" + zipFileName).inputStream()
        val myHash = proxy.uploadAttachment(attachmentUploadInputStream).toString()
        return "File Uploaded and Zipped!!! SecureHash = " + myHash
    }

    @GetMapping(value = "/downloadFile")
    private fun downloadedFile(@RequestParam("fileHash") fileHash: String,@RequestHeader(value = "PartyName") callingParty: String) : ResponseEntity<InputStreamResource>{
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        val secureHash = SecureHash.parse(fileHash)
        val attachmentDownloadInputStream = proxy.openAttachment(secureHash)
//        val fileName = "/Users/anurag07/Desktop/Corda/myDownload.zip"
        val inputStreamResource = ZipInputStream(attachmentDownloadInputStream) as InputStreamResource
        val httpHeader = HttpHeaders()
        httpHeader.add("Cache-Control", "no-cache, no-store, must-revalidate")
        httpHeader.add("Pragma", "no-cache")
        httpHeader.add("Expires", "0")

        val responseEntity = ResponseEntity.ok()
                .headers(httpHeader)
                .contentLength(inputStreamResource.contentLength())
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(inputStreamResource)

        return responseEntity
    }
}