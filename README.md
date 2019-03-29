# Telegram-Currency-Bot

Telegram bot implementation using Google App Engine + Kotlin + Ktor.

See [@easycurrbot](https://t.me/easycurrbot)


Bot allows to convert single/multi currency expressions. Available 11 currencies and the following features:
* Currencies can be set using nature russian names and some symbols.
* Currencies can be added or removed from the output using keys after expression.
* Basic `+`, `-`, `*`, `/` operators supported as well as groupings using brackets.
* Restricted `*`, `/` operations where right operand is currencied (e.g. `1 EUR / 2 USD`)
* Exchange rates dashboards (for empty query) and simple calculation function supported for [inline mode](https://core.telegram.org/bots/inline)
* Bot shows extended and readable error messages with exact position of the fail.


Implementation details which can be useful:
* Custom input grammar using [better-parse](https://github.com/h0tk3y/better-parse).
* Custom [Ktor client engine](https://ktor.io/clients/http-client/engines.html) in order to make requiests via [URLFetchService](https://cloud.google.com/appengine/docs/standard/java/javadoc/com/google/appengine/api/urlfetch/URLFetchService)
