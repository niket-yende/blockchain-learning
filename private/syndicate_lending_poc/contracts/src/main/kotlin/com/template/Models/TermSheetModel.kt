package com.template.Models

import com.template.states.TermSheetState
import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.CordaSerializable


@CordaSerializable
data class TermSheetModel(val loanRef:String,
                          val borrower: String,
                          val typeOfLoan: String,
                          val leadArranger: String,
                          val status:String,
                          val fileHash: String){
    constructor():this(
            "","","","", "",""
    )

//    override fun toString(): String {
//       return
//    }
}