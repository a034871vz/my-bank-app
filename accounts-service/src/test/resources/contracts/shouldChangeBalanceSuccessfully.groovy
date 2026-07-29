package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Успешное изменение баланса (пополнение)"

    request {
        method 'POST'
        url '/accounts/testuser/balance'
        headers {
            header('Authorization', value(
                    consumer(regex('Bearer .+')),
                    producer('Bearer dummy-token')
            ))
            contentType(applicationJson())
        }
        body([
                amount: 500,
                idempotencyKey: 'key-123'
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
                balance: 1500
        ])
    }
}