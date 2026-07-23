package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Успешная отправка уведомления"

    request {
        method 'POST'
        url '/notifications'
        headers {
            header('Authorization', value(
                    consumer(regex('Bearer .+')),
                    producer('Bearer dummy-token')
            ))
            header('X-Idempotency-Key', 'notif-key-123')
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
                status: 'sent'
        ])
    }
}