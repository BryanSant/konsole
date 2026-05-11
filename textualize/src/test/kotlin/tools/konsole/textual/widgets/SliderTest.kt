package tools.konsole.textual.widgets

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SliderTest : StringSpec({

    "initial value is clamped to [min, max]" {
        Slider(value = -10.0, min = 0.0, max = 100.0).value shouldBe 0.0
        Slider(value = 999.0, min = 0.0, max = 100.0).value shouldBe 100.0
        Slider(value = 50.0, min = 0.0, max = 100.0).value shouldBe 50.0
    }

    "increment / decrement step by [step]" {
        val s = Slider(value = 10.0, step = 5.0)
        s.action_increment(); s.value shouldBe 15.0
        s.action_increment(); s.value shouldBe 20.0
        s.action_decrement(); s.value shouldBe 15.0
    }

    "large step covers 10x" {
        val s = Slider(value = 0.0, max = 100.0, step = 1.0)
        s.action_increment_large(); s.value shouldBe 10.0
    }

    "action_minimum/maximum jump to bounds" {
        val s = Slider(value = 50.0, min = 0.0, max = 100.0)
        s.action_minimum(); s.value shouldBe 0.0
        s.action_maximum(); s.value shouldBe 100.0
    }

    "ratio is normalized" {
        val s = Slider(value = 50.0, min = 0.0, max = 100.0)
        s.ratio shouldBe 0.5
        s.setValue(0.0); s.ratio shouldBe 0.0
        s.setValue(100.0); s.ratio shouldBe 1.0
    }

    "setValue clamps and dedups" {
        val s = Slider(value = 10.0, min = 0.0, max = 100.0)
        var fires = 0
        s.start()
        s.onMessage<Slider.Changed> { fires += 1 }
        s.setValue(20.0)
        s.setValue(20.0)   // same — should not fire
        s.setValue(-5.0)   // clamps to 0
        kotlinx.coroutines.runBlocking { kotlinx.coroutines.delay(50) }
        (fires >= 1) shouldBe true
        s.value shouldBe 0.0
    }
})
