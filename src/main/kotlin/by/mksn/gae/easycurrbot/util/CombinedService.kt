package by.mksn.gae.easycurrbot.util

import by.mksn.gae.easycurrbot.exchange.ExchangeRateService
import by.mksn.gae.easycurrbot.input.InputQueryParser
import by.mksn.gae.easycurrbot.output.OutputMessageService

data class CombinedService(
        val exchange: ExchangeRateService,
        val input: InputQueryParser,
        val output: OutputMessageService
)