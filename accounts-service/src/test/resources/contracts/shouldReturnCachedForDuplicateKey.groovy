package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Возврат кэшированного результата при дубле idempotencyKey"

    request {
        method 'POST'
        url '/accounts/testuser/balance'
        headers {
            contentType(applicationJson())
        }
        body([
                amount: 200,
                idempotencyKey: 'duplicate-key'
        ])
    }

    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body([
                login: 'testuser',
                name: 'Иванов Иван',
                birthdate: '2000-05-15',
                balance: $(regex('[0-9]+'))
        ])
    }
}