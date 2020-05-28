package jozua.sijsling.snippets

import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert
import org.junit.Test
import java.lang.ref.WeakReference


// [snippet comments]
// At most projects unit tests are bread and butter. At one project we went for high coverage
// and maximum descriptiveness. You should read the class name + method name.


class ReclaimableReferenceShould {

    private val ref = ReclaimableReference<Any>()

    private var strong : Any? = Any()
    private val weak = WeakReference<Any>(strong)

    @Test
    fun `Not destroy reference if any claims left`() {

        ref.claim { strong!! }
        ref.claim { strong!! }

        strong = null
        ref.release(true) {
            Assert.fail()
        }

        Runtime.getRuntime().gc()
        assertThat(weak.get()).isNotNull()
    }

    @Test
    fun `Destroy reference when no claims left`() {

        ref.claim { strong!! }

        strong = null
        var check = false
        ref.release(true) {
            check = true
        }

        Runtime.getRuntime().gc()
        Thread.sleep(1000)
        assertThat(check).isTrue()
        assertThat(weak.get()).isNull()
    }

    @Test
    fun `Not destroy reference when parameter remove is false`() {

        ref.claim { strong!! }

        strong = null
        ref.release(false) {
            Assert.fail()
        }

        Runtime.getRuntime().gc()
        assertThat(weak.get()).isNotNull()
    }
}
