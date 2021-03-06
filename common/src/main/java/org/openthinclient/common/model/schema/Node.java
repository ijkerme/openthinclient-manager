/*******************************************************************************
 * openthinclient.org ThinClient suite
 *
 * Copyright (C) 2004, 2007 levigo holding GmbH. All Rights Reserved.
 *
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 ******************************************************************************/
package org.openthinclient.common.model.schema;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

/**
 * The node base class. Handles basic properties like the node name, parent and
 * children.
 *
 * @author levigo
 */
@XmlType(name = "node", propOrder = {
        "labels", "tips", "children"
})
@XmlAccessorType(XmlAccessType.NONE)
//@XmlAccessorOrder(value = XmlAccessOrder.UNDEFINED)
public abstract class Node implements Iterable<Node>, Serializable {

  private static final long serialVersionUID = 1L;

  @XmlElements({ //
          @XmlElement(name = "choice", type = ChoiceNode.class), //
          @XmlElement(name = "entry", type = EntryNode.class), //
          @XmlElement(name = "group", type = GroupNode.class), //
          @XmlElement(name = "password", type = PasswordNode.class), //
          @XmlElement(name = "value", type = ValueNode.class), //
          @XmlElement(name = "section", type = SectionNode.class), //
  })
  private final List<Node> children = new ArrayList<>(2);
  /**
   * Map of language->label.
   */
  @XmlElement(name = "label")
  private final List<Label> labels = new ArrayList<Label>(4);
  /**
   * Map of language->tip.
   */
  @XmlElement(name = "tip")
  private final List<Label> tips = new ArrayList<Label>(4);
  /**
   * The node's name
   */
  @XmlAttribute(name = "name")
  private String name;
  /**
   * The node's parent
   */
  private transient Node parent;
  /**
   * The node's key consisting of the names of the nodes along the path to this
   * node, joined using "."s. Lazily initialized.
   */
  private transient String key;

  /**
   * Get the Node's childern.
   */
  public List<Node> getChildren() {
    return Collections.unmodifiableList(children);
  }

  /**
   * Get the child with a given name or <code>null</code> if no child with the
   * given name exists.
   */
  public Node getChild(String name) {
    for (Node child : children) {
      if (child.getName().equals(name))
        return child;
    }

    return null;
  }

  /**
   * Add a child to this node.
   */
  public void addChild(Node child) {
    child.setParent(this);
    children.add(child);
  }

  public boolean removeChild(Node child) {
    if (!children.contains(child))
      return false;
    child.setParent(null);
    children.remove(child);
    return true;
  }

  /**
   * Get the node's name.
   */
  public String getName() {
    return name;
  }

  public void setName(String name) {
    if (name.contains("/"))
      throw new IllegalArgumentException("Node name may not contain a '/'");
    this.name = name;
  }

  /**
   * Get the node's parent. The parent is <code>null</code> for root nodes.
   */
  public Node getParent() {
    return parent;
  }

  /**
   * Set the node's parent.
   */
  protected void setParent(Node parent) {
    this.parent = parent;

    clearPathCache();
  }

  /**
   * Clear the path cache of this node as well as of all of the children.
   *
   * @deprecated
   */
  @Deprecated
  public void clearPathCache() {
    // if I don't have a cached path, my children don't have one either
    if (null != key) {
      key = null;
      for (Node child : children)
        child.clearPathCache();
    }
  }

  /**
   * Get the node's key. Initialize the path if necessary. Calling this method
   * repeatedly will return cached paths.
   */
  public String getKey() {
    if (null == key)
      if (null == parent || parent instanceof Schema)
        key = name;
      else
        key = parent.getKey() + "." + name;

    return key;
  }

  /*
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return getLabel();
  }

  public Iterator<Node> iterator() {
    return children.iterator();
  }

  public List<Label> getLabels() {
    return labels;
  }

  public String getLabel() {
    for (Label label : labels) {
      if (label.getLang().equals(Locale.getDefault().getLanguage())) {
        String labelText = label.getLabel();
        if (labelText != null) {
          return labelText;
        }
      }
    }
    return name;
  }

  public List<Label> getTips() {
    return tips;
  }

  public String getTip() {
    for (Label tip : tips)
      if (tip.getLang().equals(Locale.getDefault().getLanguage()))
        return tip.getLabel();

    return null;
  }

  protected long getUID() {
    long uid = getClass().getSimpleName().hashCode();

    for (Iterator i = children.iterator(); i.hasNext(); ) {
      Node child = (Node) i.next();
      uid ^= child.getUID();
    }

    uid ^= getKey().hashCode() ^ getName().hashCode();

    return uid;
  }

  /**
   * JAXB callback method, notifying about construction of the node. This method will be used to
   * establish the parent child relationship between Nodes.
   */
  @SuppressWarnings("unused")
  void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
    children.forEach(c -> c.setParent(this));
  }
}
