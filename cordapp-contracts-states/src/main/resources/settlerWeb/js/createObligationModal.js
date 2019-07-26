"use strict";

angular.module('demoAppModule').controller('CreateObligationModalCtrl', function ($http, $uibModalInstance, $uibModal, apiBaseURL, peers, refreshCallback) {
    const createObligationModal = this;

    createObligationModal.peers = peers;
    createObligationModal.form = {};
    createObligationModal.formError = false;

    /** Validate and create an Obligation. */
    createObligationModal.create = () => {
        if (invalidFormInput()) {
            createObligationModal.formError = true;
        } else {
            createObligationModal.formError = false;

            const role = createObligationModal.form.role
            const party = createObligationModal.form.counterparty;
            const currency = createObligationModal.form.currency;
            const amount = createObligationModal.form.amount;

            const duedate = new Date(createObligationModal.form.duedate);
            const unixEpoch = new Date('1970-01-01T00:00:00');
            const unixDuedate = (duedate - unixEpoch) / 1000

            window.alert(role + ", " + party + ", " + currency + ", " + amount);
            window.alert(unixDuedate)

            $uibModalInstance.close();

            // We define the Obligation creation endpoint.
            const issueObligationEndpoint =
                apiBaseURL +
                `issue-obligation?role=${role}&party=${party}&currency=${currency}&amount=${amount}&duedate=${unixDuedate}`;

            window.alert("!! TESTING - createObligationModal.js 1.0")

            // We hit the endpoint to create the Obligation and handle success/failure responses.
            $http.get(issueObligationEndpoint).then(
                (result) => {
                    createObligationModal.displayMessage(result);
                    refreshCallback();
                },
                (result) => {
                    createObligationModal.displayMessage(result);
                    refreshCallback();
                }
            );
        }
    };

    /** Displays the success/failure response from attempting to create an Obligation. */
    createObligationModal.displayMessage = (message) => {
        const createObligationMsgModal = $uibModal.open({
            templateUrl: 'createObligationMsgModal.html',
            controller: 'createObligationMsgModalCtrl',
            controllerAs: 'createObligationMsgModal',
            resolve: {
                message: () => message
            }
        });

        // No behaviour on close / dismiss.
        createObligationMsgModal.result.then(() => {
        }, () => {
        });
    };

    /** Closes the Obligation creation modal. */
    createObligationModal.cancel = () => $uibModalInstance.dismiss();

    // Validates the Obligation.
    function invalidFormInput() {
        return isNaN(createObligationModal.form.amount) || (createObligationModal.form.counterparty === undefined);
    }
});

// Controller for the success/fail modal.
angular.module('demoAppModule').controller('createObligationMsgModalCtrl', function ($uibModalInstance, message) {
    const createObligationMsgModal = this;
    createObligationMsgModal.message = message.data;
});