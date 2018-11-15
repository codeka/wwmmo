package au.com.codeka.warworlds.common;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Iterator;

/**
 * This is an implementation of {@link Iterator} which lets us more easily iterator over
 * Java XML nodes.
 */
public class XmlIterator {

  /**
   * This iterator iterates child elements of the given parent element.
   */
  public static Iterable<Element> childElements(Element elem) {
    return childElements(elem, null);
  }

  /**
   * This iterator iterates child elements of the given parent element, with the given node name.
   */
  public static Iterable<Element> childElements(Element elem, final String name) {
    Node node;
    for (node = elem.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() == Node.ELEMENT_NODE &&
          (name == null || node.getNodeName().equals(name))) {
        break;
      }
    }
    final Node firstNode = node;

    return new Iterable<Element>() {
      @Override
      public Iterator<Element> iterator() {
        return new Iterator<Element>() {
          Node childNode = firstNode;

          private Node findNext(Node startingNode) {
            for (Node n = startingNode.getNextSibling(); n != null; n = n.getNextSibling()) {
              if (n.getNodeType() == Node.ELEMENT_NODE &&
                  (name == null || n.getNodeName().equals(name))) {
                return n;
              }
            }

            return null;
          }

          @Override
          public boolean hasNext() {
            return childNode != null;
          }

          @Override
          public Element next() {
            Element nextElement = (Element) childNode;
            childNode = findNext(childNode);
            return nextElement;
          }

          @Override
          public void remove() {
            childNode = findNext(childNode);
          }
        };
      }
    };
  }
}
