package com.r3.corda.finance.banking.types

import net.corda.core.flows.FlowException

class BankingPaymentException(message: String) : FlowException(message)