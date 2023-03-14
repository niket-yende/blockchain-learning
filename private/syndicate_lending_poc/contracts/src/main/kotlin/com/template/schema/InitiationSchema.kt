package com.template.schema

import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object InitiationSchema

object InitiationSchema1 : MappedSchema(
        schemaFamily = InitiationSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentInitiation::class.java)){
            @Entity
            @Table(name = "initiation_state")
            class PersistentInitiation(
                    @Column(name = "loan_ref")
                    val loan_ref: String,

                    @Column(name = "issuer_name")
                    val issuerName: String,

                    @Column(name = "entity_type")
                    val entityType:String,

                    @Column(name = "syndication_type")
                    val syndicationType:String,

                    @Column(name = "loan_type")
                    val loanType:String,

                    @Column(name = "tenure")
                    val tenure:Int,

                    @Column(name = "leadArranger")
                    val leadArranger:String,

                    @Column(name = "linear_id")
                    var linearId: UUID
                    ) : PersistentState(){
                constructor(): this("","","","","",0,"",UUID.randomUUID())
            }
        }
