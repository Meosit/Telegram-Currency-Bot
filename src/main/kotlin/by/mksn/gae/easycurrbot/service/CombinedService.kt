package by.mksn.gae.easycurrbot.service

/**
 * @author Mikhail Snitavets
 */
data class CombinedService(
        val exchange: ExchangeRateService,
        val input: InputQueryService,
        val output: OutputMessageService
)