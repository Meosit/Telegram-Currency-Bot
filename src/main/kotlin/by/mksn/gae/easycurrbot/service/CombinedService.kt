package by.mksn.gae.easycurrbot.service

data class CombinedService(
        val exchange: ExchangeRateService,
        val input: InputQueryService,
        val output: OutputMessageService
)