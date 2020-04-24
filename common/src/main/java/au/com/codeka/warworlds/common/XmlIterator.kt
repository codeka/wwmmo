package au.com.codeka.warworlds.common

import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * This is an implementation of [Iterator] which lets us more easily iterator over
 * Java XML nodes.
 */
object XmlIterator {
  /**
   * This iterator iterates child elements of the given parent element.
   */
  fun childElements(elem: Element, name: String? = null): Iterable<Element> {
    var node: Node?
    node = elem.firstChild
    while (node != null) {
      if (node.nodeType == Node.ELEMENT_NODE &&
          (name == null || node.nodeName == name)) {
        break
      }
      node = node.nextSibling
    }
    val firstNode = node
    return object : Iterable<Element> {
      override fun iterator(): Iterator<Element> {
        return object : Iterator<Element> {
          var childNode = firstNode
          private fun findNext(startingNode: Node?): Node? {
            var n = startingNode!!.nextSibling
            while (n != null) {
              if (n.nodeType == Node.ELEMENT_NODE &&
                  (name == null || n.nodeName == name)) {
                return n
              }
              n = n.nextSibling
            }
            return null
          }

          override fun hasNext(): Boolean {
            return childNode != null
          }

          override fun next(): Element {
            val nextElement = childNode as Element
            childNode = findNext(childNode)
            return nextElement
          }
        }
      }
    }
  }
}