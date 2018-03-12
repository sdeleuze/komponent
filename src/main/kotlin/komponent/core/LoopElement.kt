package komponent.core

import org.w3c.dom.Node

abstract class LoopElement<T> : CustomElement(false) {
	companion object {
		const val tag = "k-loop"
		fun define() = defineElement<LoopElement<*>>(tag)
	}

	var elements: Collection<T> by observable(emptyList())
	var function by observable<(Node.(T) -> Unit)?>(null)

	private val appendChildForwarder = appendChildForwarder(::doAppendChild)
	private val childrenNodes = arrayListOf<Node>()

	override fun Node.render() {
		subscribe(::elements) { doRender() }
		subscribe(::function) { doRender() }
	}

	private fun doRender() {
		var i = childrenNodes.size - 1
		while (i >= 0) {
			parentNode!!.removeChild(childrenNodes[i--])
		}
		childrenNodes.clear()

		function?.let { f ->
			elements.forEach { appendChildForwarder.f(it) }
		}
	}

	private fun doAppendChild(child: Node): Node {
		val node = parentNode!!.insertBefore(child, this)
		childrenNodes.add(node)
		return node
	}
}

fun <T> Node.loop(elements: Collection<T> = emptyList(), function: Node.(T) -> Unit): LoopElement<T> {
	return createElement<LoopElement<T>>(LoopElement.tag, this, null).apply {
		this.function = function
		this.elements = elements
	}
}

fun appendChildForwarder(forwardTo: (Node) -> Node): Node {
	val forwarder = js("{}")
	forwarder.appendChild = forwardTo
	return forwarder
}