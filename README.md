# Corda settler
For full details, please visit https://github.com/corda/corda-settler.

## Background

This repository demonstrates an off-ledger settlement using corda-settler with my spring boot REST
Api https://github.com/kaiilim/spring-boot-rest-2. These 2 repositories show 
how an obligation raised on Corda can be settled off-ledger using the settler libraries. 
An Api request is send by CorDapp to the REST Api and settlement occurs if the injected
logic passes. CorDapp will receive a hashed transaction reference that is to be updated
 to the obligation and the obligation will be marked settled when the Oracle checks 
 that the transfer of assets are made. At the moment, this is structured in a way that
  only the obligee is suppose to raise the obligation and not the obligor. Obligor updates
  the settlement method and account to be paid to as instructed by the obligee.

## Usage

Steps:
* To kick start the nodes, open command prompt and run these steps:

      cd C:\YOUR_PATH_TO_PROJECT\corda-settler
      gradlew clean deployNodes
    

* Wait for the build to be finished, then:

      cd build\nodes
      runnodes
    

* 4 windows should be opened, if not please close all of them and run command runnodes again.


Once all the nodes have started, start with `Party A` and paste the following command to create a new
obligation, PartyA is obligee and PartyB is obligor:

    start CreateObligation amount: { quantity: 1000, token: { currencyCode: USD, type: fiat } }, role: OBLIGEE, counterparty: PartyB, dueBy: 1577793600, anonymous: false


The node shell will output the result of the flow which should print the
details of the new obligation that looks something like this:

    OUTPUT:     Obligation(d6f9bb92-c903-4c54-9121-97a2b3afb1b2): PartyB owes PartyA 10.00 USD (0.00 USD paid).
                Settlement status: UNSETTLED
                SettlementMethod: No settlement method added
                Payments:
                    No payments made.
    COMMAND:    com.r3.corda.finance.obligation.commands.ObligationCommands.Create with pubkeys DL4AeA53y7qHJDEQrEJYiEsycihxhz1uNEoc5jEFvuyAt9, DLDnLmKJ5kfJm2qNv3NpbD8QD9dcZGNm2YXXXvptrLmcdg
    ATTACHMENT: BE850C17C89B5B55B1962AEC78947404A36EC05FD8FA1AE52207EEB052F8B977

From the output, copy the UUID for the obligation which was output
on the first line `OUTPUT:     Obligation(UUID)`.

Next, from the `Party B` node, update the settlement method and account 
to be paid to as instructed by the obligee:

    start UpdateSettlementMethod linearId: UUID, settlementMethod: { accountToPay: MockAccount123, settlementOracle: Oracle, _type: com.r3.corda.finance.banking.types.BankingSettlement }

Next, from the `Party B` node, start off-ledger settlement flow to settle
the obligation, make sure that the spring boot REST server has been started:

    start OffLedgerSettleObligation amount: { quantity: 1000, token: { currencyCode: USD, type: fiat } }, linearId: UUID

That is it. The obligation should now be marked settled and the end-to-end flow
is completed. The following command retrieves any obligation given the UUID if
you wish to refer to them once again:

    start RetrieveObligation linearId: UUID

## TODO

* Still implementing a front end web server to interact with the nodes

**Disclaimer: This repository is purely for experimentation purposes.
