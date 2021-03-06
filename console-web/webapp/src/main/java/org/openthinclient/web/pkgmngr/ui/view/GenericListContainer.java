package org.openthinclient.web.pkgmngr.ui.view;

import java.util.Collection;
import java.util.Collections;

import org.vaadin.viritin.FilterableListContainer;

import com.vaadin.data.Container;

public class GenericListContainer<T> extends FilterableListContainer<T> implements Container.Hierarchical {

  /** serialVersionUID */
  private static final long serialVersionUID = -3581375135383207086L;

  public GenericListContainer(Class<T> type) {
    super(type);
  }

  @Override
  public Collection<T> getChildren(Object itemId) {
    // no grouping/child support yet.
    return Collections.emptyList();
  }

  @Override
  public Object getParent(Object itemId) {
    // no grouping/child support yet.
    return null;
  }

  @Override
  public Collection<T> rootItemIds() {
    return getItemIds();
  }

  @Override
  public boolean setParent(Object itemId, Object newParentId) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean areChildrenAllowed(Object itemId) {
    return false;
  }

  @Override
  public boolean setChildrenAllowed(Object itemId, boolean areChildrenAllowed) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isRoot(Object itemId) {

    return getItemIds().contains(itemId);
  }

  @Override
  public boolean hasChildren(Object itemId) {
    return false;
  }

}