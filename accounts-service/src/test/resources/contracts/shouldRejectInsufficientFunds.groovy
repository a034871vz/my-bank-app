package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Отказ при недостатке средств"

    request {
        method 'POST'
        url '/accounts/testuser/balance'
        headers {
            contentType(applicationJson())
        }
        body([
                amount: -1500,
                idempotencyKey: 'key-456'
        ])
    }

    response {
        status 400
        headers {
            contentType(applicationJson())
        }
        body([
                error: 'Недостаточно средств на счету'
        ])
    }
}