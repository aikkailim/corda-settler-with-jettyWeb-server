package com.r3.corda.finance.banking.types

import com.fasterxml.jackson.annotation.JsonProperty

data class BankingPaymentInstruction(
        // Using linearId as payment_identification
        @JsonProperty("payment_identification")
        val paymentIdentification: String,
        @JsonProperty("requested_execution_date")
        val requestedExecutionDate: String,
        @JsonProperty("amount")
        val amount: BankingPaymentAmount,
        @JsonProperty("obligor_name")
        val obligorName: String,
        @JsonProperty("obligee_name")
        val obligeeName: String,
        @JsonProperty("obligee_account")
        val obligeeAccount: String
)

data class BankingPaymentResponse(
        @JsonProperty("credit_transfer_transaction_resource_identification")
        val creditTransferTransactionResourceIdentification: String?,
        @JsonProperty("uetr")
        val uetr: String,
        @JsonProperty("status")
        val status: String
)

data class BankingPaymentAmount(
        @JsonProperty("instructed_amount")
        val instructedAmount: BankingInstructedAmount
)

data class BankingInstructedAmount(
        @JsonProperty("currency")
        val currency: String,
        @JsonProperty("amount")
        val amount: String
)

data class BankingPaymentStatus(
        @JsonProperty("payment_identification")
        val payment_identification: String,
        @JsonProperty("requested_execution_date")
        val requested_execution_date: String,
        @JsonProperty("amount")
        val amount: BankingPaymentAmount,
        @JsonProperty("obligor_name")
        val obligor_name: String,
        @JsonProperty("obligee_name")
        val obligee_name: String,
        @JsonProperty("obligee_account")
        val obligee_account: String,
        @JsonProperty("uetr")
        val uetr: String,
        @JsonProperty("status")
        val status: String
)

//data class BankingTransactionStatus(
//        @JsonProperty("status")
//        val status: BankingPaymentStatusType
//)
//
//enum class BankingPaymentStatusType {
//    REJECTED, SUCCESS, PENDING
//}