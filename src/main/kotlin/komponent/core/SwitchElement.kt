package komponent.core

import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.asList
import kotlin.dom.addClass
import kotlin.dom.hasClass
import kotlin.dom.removeClass

abstract class SwitchElement<T> : CustomElement() {

	companion object {
		const val tag = "k-switch"
		fun define() = defineElement<SwitchElement<*>>(tag)
		const val hiddenClass = "k-switch-hidden"
	}

	var value by observable<T?>(null)
	var cases by observable<(Node.(T?) -> Unit)?>(null)
	var lazy by observable(false)

	private var currentChildren = emptyList<Node>()
	private val lazyChildren = hashMapOf<T?, List<Node>>()

	override fun Node.render() {
		style { textContent = """
			|:host {
			|	display: block;
			|}
			|
			|:host > .$hiddenClass {
			|	display: none !important;
			|}
		""".trimMargin()
		}

		subscribe(::value) { doRender(it, cases) }
		subscribe(::cases) { resetAndRender () }
		subscribe(::lazy) { resetAndRender () }
	}

	private fun Node.resetAndRender() {
		currentChildren = emptyList()
		lazyChildren.clear()
		removeAllChildren()
		doRender(value, cases)
	}

	private fun Node.removeAllChildren() {
		// Do not remove first (<style>) child
		while (firstChild != null && firstChild !== lastChild) {
			removeChild(lastChild!!)
		}
	}

	private fun Node.doRender(value: T?, cases: (Node.(T?) -> Unit)?) {
		if (lazy) {
			if (currentChildren.isNotEmpty()) {
				currentChildren.forEach { (it as? Element)?.addClass(hiddenClass) }
			}
		} else {
			removeAllChildren()
		}

		cases?.let {
			if (lazy) {
				if (lazyChildren.containsKey(value)) {
					currentChildren = lazyChildren[value]!!
					currentChildren.forEach { (it as? Element)?.removeClass(hiddenClass) }
				} else {
					it(value)
					currentChildren = childNodes.asList().filter { it is Element && !it.hasClass(hiddenClass) }
					lazyChildren[value] = currentChildren
				}
			} else {
				it(value)
			}
		}
	}
}

fun <T> Node.switch(value: T? = null, lazy: Boolean = false, cases: Node.(T?) -> Unit): SwitchElement<T> {
	return createElement<SwitchElement<T>>(SwitchElement.tag, this, null).apply {
		this.value = value
		this.cases = cases
		this.lazy = lazy
	}
}

fun Node.switchIf(value: Boolean? = null, lazy: Boolean = false, action: Node.() -> Unit): SwitchElement<Boolean> {
	return switch(value, lazy) {
		if (it == true) {
			action()
		}
	}
}