package name.wildswift.testapp.generated

import name.wildswift.mapache.events.Event

sealed class TestAppEvent: Event

data class BuyCrypto(val tiker:String): TestAppEvent()
data class SellCrypto(val tiker:String): TestAppEvent()
data class ProceedBuy(val tiker:String, val amount: Float, val paymentType: Int): TestAppEvent()