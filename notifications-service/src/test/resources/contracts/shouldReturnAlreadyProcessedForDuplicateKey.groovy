package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Возврат status = already-processed при повторном idempotencyKey"

    request {
        method 'POST'
        url '/notifications'
        headers {
            header('Authorization', value(
                    consumer(regex('Bearer .+')),
                    producer('Bearer dummy-token')
            ))
            header('X-Idempotency-Key', 'duplicate-notif-key')
            contentType(applicationJson())
        }
        body([
                type: 'DEPOSIT',
                login: 'testuser',
                amount: 500,
                message: 'Пополнение счета'
        ])
    }

    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body([
                status: 'already-processed'
        ])
    }
}