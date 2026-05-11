package tools.konsole.rich.filesize

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class FilesizeTest : StringSpec({

    "decimal under a kilobyte" {
        Filesize.decimal(0) shouldBe "0 B"
        Filesize.decimal(999) shouldBe "999 B"
    }

    "decimal kilobytes and beyond" {
        Filesize.decimal(1500) shouldBe "1.5 kB"
        Filesize.decimal(1_500_000) shouldBe "1.5 MB"
        Filesize.decimal(1_500_000_000L) shouldBe "1.5 GB"
    }

    "binary uses 1024" {
        Filesize.binary(1024) shouldBe "1.0 KiB"
        Filesize.binary(1024 * 1024) shouldBe "1.0 MiB"
    }

    "negative values keep sign" {
        Filesize.decimal(-2000) shouldBe "-2.0 kB"
    }
})
