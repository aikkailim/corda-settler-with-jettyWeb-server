package com.r3.corda.finance.banking.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.r3.corda.finance.banking.types.*
import com.r3.corda.finance.obligation.types.FiatCurrency
import net.corda.core.contracts.Amount
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

//import testing.WriteFile

@CordaService
class BankingService(val appServiceHub: AppServiceHub) : SingletonSerializeAsToken() {

    companion object {
        // TODO: this should be driven by configuration parameter
        fun privateKey(): PrivateKey? {
            return null
        }

        // TODO: this should be driven by configuration parameter
        fun certificate(): X509Certificate? {
            return null
        }

        private val logger = LoggerFactory.getLogger(BankingService::class.java)!!
    }

    fun makePayment(e2eId: String,
                    executionDate: Date,
                    amount: Amount<FiatCurrency>,
                    obligorName: String,
                    obligeeName: String,
                    obligeeAccount: String): BankingPaymentResponse {
        println("!! TESTING in BANKINGSERVICE.makePayment 1.4.1")

        val paymentResponse = submitPaymentInstruction(
                e2eId,
                executionDate,
                amount,
                obligorName,
                obligeeName,
                obligeeAccount)
        println("!! TESTING in BANKINGSERVICE.makePayment 1.4.2")

//        WriteFile.WriteToFile(
//                e2eId,
//                executionDate.toString(),
//                amount.toString(),
//                obligorName,
//                obligeeName,
//                obligeeAccount
//        )
        println("!! TESTING in BANKINGSERVICE.makePayment 1.4.3")

        return paymentResponse
    }

    private fun submitPaymentInstruction(e2eId: String,
                                         executionDate: Date,
                                         amount: Amount<FiatCurrency>,
                                         obligorName: String,
                                         obligeeName: String,
                                         obligeeAccount: String
    ): BankingPaymentResponse {
        println("!! TESTING in BANKINGSERVICE.submitPaymentInstruction 1.4.1.1")

        val dateFormatter = SimpleDateFormat("yyyy-MM-dd")
        val amountFormatter = DecimalFormat("#0.##")
        // we need to convert amount from Corda representation before submitting to swift
        val doubleAmount = BigDecimal.valueOf(amount.quantity).multiply(amount.displayTokenSize)
        println("!! TESTING in BANKINGSERVICE.submitPaymentInstruction 1.4.1.2")

        // create Banking payment instruction
        val bankingPaymentInstruction = BankingPaymentInstruction(
                e2eId,
                dateFormatter.format(executionDate),
                BankingPaymentAmount(BankingInstructedAmount(amount.token.currencyCode, amount = amountFormatter.format(doubleAmount))),
                obligorName,
                obligeeName,
                obligeeAccount
        )
        println("!! TESTING in BANKINGSERVICE.submitPaymentInstruction 1.4.1.3")

        val mapper = jacksonObjectMapper()
//        val paymentUrl = "https://demo8309926.mockable.io/submitpayment"
        val paymentUrl = "http://localhost:8083/register/payment"
        val paymentInstructionId = bankingPaymentInstruction.paymentIdentification
        println("!! TESTING in BANKINGSERVICE.submitPaymentInstruction 1.4.1.4")

        logger.info(messageWithParams("Submitting payment instruction $bankingPaymentInstruction to $paymentUrl", "PAYMENT_INSTRUCTION_ID" to paymentInstructionId))
        println("!! TESTING in BANKINGSERVICE.submitPaymentInstruction 1.4.1.5")

        // making HTTP request
        val (req, res, result) = paymentUrl
                .httpPost()
                .header("accept" to "application/json")
                .header("content-type" to "application/json")
                .body(mapper.writeValueAsString(bankingPaymentInstruction))
                .response()
        println("!! TESTING in BANKINGSERVICE.submitPaymentInstruction 1.4.1.6")

        if (res.httpStatusCode == -1) {
            println("Connect to the internet & run your API server first you stupid or what")
        }

        val responseData = String(res.data)
        println("!! TESTING in BANKINGSERVICE.submitPaymentInstruction 1.4.1.7")
        println(res.httpResponseMessage)
        println(res.httpStatusCode)
        println(res.data)
        println("--------------------")
        println(responseData)
        println("--------------------")

        // if the payment attempt resulted to error - logging and throwing FlowException
        if (res.httpStatusCode >= 400) {
            println("!! TESTING in BANKINGSERVICE.submitPaymentInstruction 1.4.1.8")

            val message = httpResultMessage("Error while submitting payment instruction", res.httpStatusCode, responseData, "PAYMENT_INSTRUCTION_ID" to paymentInstructionId)
            logger.warn(message)
            throw BankingPaymentException(message)
        } else {
            println("!! TESTING in BANKINGSERVICE.submitPaymentInstruction 1.4.1.9")

            logger.info(httpResultMessage("Successfully submitted payment instruction", res.httpStatusCode, responseData, "PAYMENT_INSTRUCTION_ID" to paymentInstructionId))
            println("!! TESTING in BANKINGSERVICE.submitPaymentInstruction 1.4.1.10")
            println(responseData)

            return mapper.readValue(responseData)
        }
    }

    fun getPaymentStatus(uetr: String): BankingPaymentStatus {
        println("!! TESTING in BANKINGSERVICE.getPaymentStatus() in ORACLE module 4.2.1")

//        val checkStatusUrl = "https://demo8309926.mockable.io/paymentstatus"
        val checkStatusUrl = "http://localhost:8083/payment/retrieval/$uetr"

        BankingService.logger.info(messageWithParams("Getting payment status", "UETR" to uetr))
        val (_, res, _) = checkStatusUrl
                .httpGet()
                .header("accept" to "application/json")
                .header("content-type" to "application/json")
                .response()
        println("!! TESTING in BANKINGSERVICE.getPaymentStatus() in ORACLE module 4.2.2")

        if (res.httpStatusCode == -1) {
            println("Connect to the internet & run your API server first you stupid or what")
        }

        val responseData = String(res.data)
        val mapper = jacksonObjectMapper()
        println(uetr)
        println(res.httpResponseMessage)
        println(res.httpStatusCode)
        println(res.data)
        println("--------------------")
        println(responseData)
        println("--------------------")

        if (res.httpStatusCode >= 400) {
            println("!! TESTING in BANKINGSERVICE.getPaymentStatus() in ORACLE module 4.2.3")

            val message = httpResultMessage("Error while retrieving payment status.", res.httpStatusCode, responseData, "UETR" to uetr)
            BankingService.logger.warn(message)
            throw BankingPaymentException(message)
        } else {
            println("!! TESTING in BANKINGSERVICE.getPaymentStatus() in ORACLE module 4.2.4")

            BankingService.logger.info(httpResultMessage("Successfully retrieved payment status", res.httpStatusCode, responseData, "UETR" to uetr))
            println("!! TESTING in BANKINGSERVICE.getPaymentStatus() in ORACLE module 4.2.5")

            return mapper.readValue(responseData)
        }
    }

    private fun messageWithParams(message: String, vararg otherParams: Pair<String, Any>): String {
        return if (otherParams.isNotEmpty()) {
            val initial = "."
            message + otherParams.fold(initial) { acc, s ->
                val pair = "${s.first.toUpperCase()}=${s.second}"
                if (acc == initial) "$acc $pair" else "$acc, $pair"
            }
        } else message
    }

    private fun httpResultMessage(message: String, httpResultCode: Int, responseBody: String, vararg otherParams: Pair<String, Any>): String {
        val newParams = listOf(Pair<String, Any>("BANKING_HTTP_STATUS", httpResultCode), Pair<String, Any>("BANKING_HTTP_RESPONSE", responseBody)) + otherParams.asList()
        return messageWithParams(message, *newParams.toTypedArray())
    }
}