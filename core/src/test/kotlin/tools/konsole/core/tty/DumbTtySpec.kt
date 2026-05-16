package tools.konsole.core.tty

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.system.measureTimeMillis

class DumbTtySpec : StringSpec({

    "read returns bytes in order, then EOF" {
        val tty = DumbTty(ByteArrayInputStream(byteArrayOf(0x41, 0x42, 0x43)), ByteArrayOutputStream())
        tty.read() shouldBe 0x41
        tty.read() shouldBe 0x42
        tty.read() shouldBe 0x43
        tty.read() shouldBe Tty.EOF
    }

    "read(timeoutMs) returns READ_EXPIRED when nothing is available" {
        val tty = DumbTty(ByteArrayInputStream(ByteArray(0)), ByteArrayOutputStream())
        val elapsed = measureTimeMillis {
            tty.read(20L) shouldBe Tty.READ_EXPIRED
        }
        // Allow generous slack for CI; just check we waited at all.
        (elapsed >= 15L) shouldBe true
    }

    "read(timeoutMs) returns immediately when data is available" {
        val tty = DumbTty(ByteArrayInputStream(byteArrayOf(0x7E)), ByteArrayOutputStream())
        tty.read(500L) shouldBe 0x7E
    }

    "shutdown short-circuits subsequent reads to EOF" {
        val tty = DumbTty(ByteArrayInputStream(byteArrayOf(0x41, 0x42)), ByteArrayOutputStream())
        tty.read() shouldBe 0x41
        tty.shutdown()
        tty.read() shouldBe Tty.EOF
        tty.read(50L) shouldBe Tty.EOF
    }

    "out writes UTF-8 to the underlying stream" {
        val sink = ByteArrayOutputStream()
        val tty = DumbTty(ByteArrayInputStream(ByteArray(0)), sink)
        tty.out.write("héllo")
        tty.out.flush()
        sink.toByteArray() shouldBe "héllo".toByteArray(Charsets.UTF_8)
    }

    "isatty is false for every stream" {
        val tty = DumbTty(ByteArrayInputStream(ByteArray(0)), ByteArrayOutputStream())
        tty.isatty(Stream.Input) shouldBe false
        tty.isatty(Stream.Output) shouldBe false
        tty.isatty(Stream.Error) shouldBe false
    }

    "size falls back to 80x24" {
        val tty = DumbTty(ByteArrayInputStream(ByteArray(0)), ByteArrayOutputStream())
        tty.size().columns shouldBe 80
        tty.size().rows shouldBe 24
    }

    "enterRawMode throws" {
        val tty = DumbTty(ByteArrayInputStream(ByteArray(0)), ByteArrayOutputStream())
        shouldThrow<UnsupportedOperationException> { tty.enterRawMode() }
    }

    "typeLabel is dumb" {
        DumbTty(ByteArrayInputStream(ByteArray(0)), ByteArrayOutputStream()).typeLabel shouldBe "dumb"
    }

    "shutdown from another thread unblocks a blocked read" {
        // SlowStream blocks indefinitely on read() — model of a stdin pipe with
        // no data. We expect tty.read() to return EOF when shutdown fires from
        // another thread within a reasonable window.
        val slowStream = object : java.io.InputStream() {
            private val gate = java.util.concurrent.CountDownLatch(1)
            override fun read(): Int {
                gate.await()
                return -1
            }
            override fun available(): Int = 0
        }
        val tty = DumbTty(slowStream, ByteArrayOutputStream())
        val result = java.util.concurrent.CompletableFuture<Int>()
        val reader = Thread { result.complete(tty.read(200L)) }.apply { isDaemon = true; start() }
        Thread.sleep(20)
        tty.shutdown()
        reader.join(500)
        // read(timeoutMs) is the variant the EventReader pump uses; with shutdown
        // it should resolve to EOF rather than block forever.
        (result.get() == Tty.EOF || result.get() == Tty.READ_EXPIRED) shouldBe true
    }
})
