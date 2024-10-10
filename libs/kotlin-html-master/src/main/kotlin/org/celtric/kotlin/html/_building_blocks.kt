package org.celtric.kotlin.html

class HTMLException(message: String) : RuntimeException(message)

data class RenderingOptions(
        val indentWith: String = "    ",
        val indentLevel: Int = 0,
        val lineSeparator: String = System.lineSeparator()) {
    fun indent() = indentWith.repeat(indentLevel)
    fun nextLevel() = copy(indentLevel = indentLevel + 1)
}

private typealias Options = RenderingOptions

//---[ Nodes ]--------------------------------------------------------------------------//

sealed class Node {
    abstract fun render(opt: Options = Options()): String
    abstract fun isBlock(): Boolean
    operator fun plus(text: String): List<Node> = plus(Text(text))
    operator fun plus(node: Node): List<Node> = listOf(this, node)
    operator fun plus(nodes: List<Node>): List<Node> = listOf(this) + nodes
    fun toList(): List<Node> = listOf(this)
}

class DocumentType(val type: String) : Node() {
    override fun render(opt: Options) = "${opt.indent()}<!DOCTYPE $type>${opt.lineSeparator}"
    override fun isBlock() = true
}

class Text(val content: String) : Node() {
    override fun render(opt: Options) = opt.indent() + content
    override fun isBlock() = false
}

// Due to Kotlin's function overloading limitations, we are forced to use Any for content and check for valid types at
// runtime.
// @see https://discuss.kotlinlang.org/t/overloaded-function-with-lambda-parameter-of-different-return-types/6053
sealed class Element(val name: String, val _isBlock: Boolean, unvalidatedContent: Any, val attributes: AllAttributes) : Node() {

    val content = when {
        unvalidatedContent == Unit -> Text("")
        unvalidatedContent is String -> Text(unvalidatedContent)
        unvalidatedContent is Number -> Text(unvalidatedContent.toString())
        unvalidatedContent is Node -> unvalidatedContent
        (unvalidatedContent is List<*> && (unvalidatedContent.isEmpty() || unvalidatedContent.first() is Node)) -> @Suppress("UNCHECKED_CAST") NodeList(unvalidatedContent as List<Node>)
        else -> throw HTMLException("Content must be String, Number, Node or List<Node>, ${contentType(unvalidatedContent)} given.")
    }

    override fun render(opt: Options): String {
        val renderedContent =
                if (content.isBlock()) "${opt.lineSeparator}${content.render(opt.nextLevel())}${opt.indent()}"
                else content.render()

        return "${opt.indent()}<$name${attributes.render()}>$renderedContent</$name>" + if (isBlock()) opt.lineSeparator else ""
    }

    override fun isBlock() = _isBlock

    private fun contentType(content: Any): String = when {
        content !is Collection<*> -> content.javaClass.simpleName
        content.isEmpty() -> "${content.javaClass.simpleName}<?>"
        content.first() is Collection<*> -> "${content.javaClass.simpleName}<${contentType(content.first()!!)}>"
        else -> "${content.javaClass.simpleName}<${content.first()!!.javaClass.simpleName}>"
    }
}

class BlockElement(name: String, content: Any, attributes: AllAttributes) : Element(name, true, content, attributes)
class InlineElement(name: String, content: Any, attributes: AllAttributes) : Element(name, false, content, attributes)

sealed class EmptyElement(val name: String, val _isBlock: Boolean, val attributes: AllAttributes? = null) : Node() {
    override fun render(opt: Options) =
            "${opt.indent()}<$name${attributes?.render()?:""}>" + if (isBlock()) opt.lineSeparator else ""
    override fun isBlock() = _isBlock
}

class EmptyBlockElement(name: String, attributes: AllAttributes? = null) : EmptyElement(name, true, attributes)
class EmptyInlineElement(name: String, attributes: AllAttributes? = null) : EmptyElement(name, false, attributes)

//---[ Lists of nodes ]-----------------------------------------------------------------//

private class NodeList(val nodes: List<Node>) : Node() {
    override fun render(opt: Options) = nodes.render(opt)
    override fun isBlock() = nodes.any { it.isBlock() }
}

fun List<Node>.render(opt: Options = Options()) = joinToString("") {
    it.render(opt) + if (!it.isBlock() && any { it.isBlock() }) "\n" else ""
}

operator fun List<Node>.plus(text: String): List<Node> = plus(Text(text))

//---[ Attributes ]---------------------------------------------------------------------//

typealias Attributes = Map<String, Any?>

private fun Attributes.renderAttributes(prefix: String = "") =
        filter { it.value != null && it.value != false && it.value != "" }
        .map { Pair(it.key, if (it.value is Boolean) "" else "=\"${it.value}\"") }
        .joinToString("") { " " + prefix + it.first + it.second }

class AllAttributes(val common: Attributes, val other: Attributes, val data: Attributes) {
    fun render() = common.renderAttributes() + other.renderAttributes() + data.renderAttributes("data-")
}
