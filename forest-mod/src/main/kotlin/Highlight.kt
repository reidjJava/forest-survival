import dev.xdark.clientapi.event.lifecycle.GameLoop
import dev.xdark.feder.NetUtil
import ru.cristalix.clientapi.JavaMod.clientApi
import ru.cristalix.clientapi.mod
import ru.cristalix.clientapi.registerHandler
import ru.cristalix.uiengine.UIEngine
import ru.cristalix.uiengine.element.AbstractElement
import ru.cristalix.uiengine.eventloop.animate
import ru.cristalix.uiengine.utility.*

class Highlight {
    private val timeLife = 4 * 1000

    init {
        val hints = ArrayList<Pair<Long, AbstractElement>>()

        App::class.mod.registerChannel("highlight") {
            val string = NetUtil.readUtf8(this)
            val hint = rectangle {
                offset = Relative.CENTER
                align = Relative.CENTER
                scale = V3(1.1, 1.1, 1.1)
                addChild(text {
                    offset.x -= clientApi.fontRenderer().getStringWidth(string) / 2 * (1.15) // 1 + половина %30
                    offset.y += 5
                    scale = V3(1.0, 1.0, 1.0)
                    content = string
                    color = WHITE
                })
            }
            UIEngine.overlayContext.addChild(hint)
            hint.animate(timeLife / 1000, Easings.SINE_BOTH) {
                offset.y += 60
            }
            hint.children[0].animate(timeLife / 1000, Easings.QUAD_OUT) {
                color.alpha = 0.2
            }
            hints.add(Pair(System.currentTimeMillis(), hint))
        }

        registerHandler<GameLoop> {
            // Удаление уведомлений под курсором
            val time = System.currentTimeMillis()

            hints.removeIf {
                val remove = time - it.first > timeLife

                if (remove)
                    UIEngine.overlayContext.removeChild(it.second)

                return@removeIf remove
            }
        }
    }
}