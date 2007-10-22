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
 *******************************************************************************/
package org.openthinclient.console.wizards.newdirobject;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openide.ErrorManager;
import org.openide.WizardDescriptor;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openthinclient.common.model.Realm;
import org.openthinclient.console.MainTreeTopComponent;
import org.openthinclient.console.Messages;
import org.openthinclient.console.nodes.RealmNode;
import org.openthinclient.console.nodes.RealmsNode;
import org.openthinclient.ldap.DirectoryException;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class SelectObjectTypePanel extends JPanel
    implements
      WizardDescriptor.Panel {

  private final Set<ChangeListener> listeners = new HashSet<ChangeListener>(1);

  private javax.swing.JComboBox realmComboBox;

  private ArrayList<Realm> realms = new ArrayList<Realm>();

  private javax.swing.JComboBox typeComboBox;

  private WizardDescriptor wd;

  public final void addChangeListener(ChangeListener l) {
    synchronized (listeners) {
      listeners.add(l);
    }
  }

  protected final void fireChangeEvent() {
    Iterator<ChangeListener> it;
    synchronized (listeners) {
      it = new HashSet<ChangeListener>(listeners).iterator();
    }
    ChangeEvent ev = new ChangeEvent(this);
    while (it.hasNext()) {
      it.next().stateChanged(ev);
    }
  }

  // Get the visual component for the panel. In this template, the component
  // is kept separate. This can be more efficient: if the wizard is created
  // but never displayed, or not all panels are displayed, it is better to
  // create only those which really need to be visible.
  public Component getComponent() {
    return this;
  }

  public HelpCtx getHelp() {
    // Show no Help button for this panel:
    return HelpCtx.DEFAULT_HELP;
    // If you have context help:
    // return new HelpCtx(SampleWizardPanel1.class);
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  private void initComponents() {
    DefaultFormBuilder dfb = new DefaultFormBuilder(new FormLayout(
        "r:p,3dlu,f:p:g,3dlu,p,3dlu,p"), Messages.getBundle(), this); //$NON-NLS-1$

    String[] types = Messages.getString("NewDirObject0_type_dropdown_types") //$NON-NLS-1$
        .split("\\s*,\\s*"); //$NON-NLS-1$
    Class classes[] = new Class[types.length];
    for (int i = 0; i < types.length; i++)
      try {
        classes[i] = Class.forName(types[i]);
      } catch (ClassNotFoundException e1) {
        // should not happen.
        throw new RuntimeException(e1);
      }

    typeComboBox = new JComboBox(types);
    typeComboBox.setRenderer(new DefaultListCellRenderer() {
      /*
       * @see javax.swing.DefaultListCellRenderer#getListCellRendererComponent(javax.swing.JList,
       *      java.lang.Object, int, boolean, boolean)
       */
      @Override
      public Component getListCellRendererComponent(JList list, Object value,
          int index, boolean isSelected, boolean cellHasFocus) {
        String v = value.toString();
        v = v.substring(v.lastIndexOf('.') + 1);
        return super.getListCellRendererComponent(list, Messages
            .getString("NewDirObject0_type_dropdown_item_" + v), index, //$NON-NLS-1$
            isSelected, cellHasFocus);
      }
    });
    typeComboBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        fireChangeEvent();
      }
    });

    realmComboBox = new JComboBox(realms.toArray());
    realmComboBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        fireChangeEvent();
      }
    });

    dfb.appendI15d("NewDirObject0_realm_label", realmComboBox); //$NON-NLS-1$
    dfb.nextLine();

    dfb.appendI15d("NewDirObject0_type_label", typeComboBox); //$NON-NLS-1$
    dfb.nextLine();

  }

  public boolean isValid() {
    if (typeComboBox.getSelectedItem() == null) {
      wd.putProperty("WizardPanel_errorMessage", Messages //$NON-NLS-1$
          .getString("NewDirObject0_type_error")); //$NON-NLS-1$
      return false;
    }
    if (realmComboBox.getSelectedItem() == null) {
      wd.putProperty("WizardPanel_errorMessage", Messages //$NON-NLS-1$
          .getString("NewDirObject0_realm_error")); //$NON-NLS-1$
      return false;
    }
    return true;
  }

  private Realm[] loadRealms() {
    Realm[] realms = null;
    Node rootNode = MainTreeTopComponent.getDefault().getExplorerManager()
        .getRootContext();
    Node[] childNodes = rootNode.getChildren().getNodes();
    for (Node node : childNodes) {
      if (node instanceof RealmsNode) {
        Node[] realmNodes = node.getChildren().getNodes(true);
        realms = new Realm[realmNodes.length];
        int i = 0;
        for (Node realmNode : realmNodes) {
          if (realmNode instanceof RealmNode) {
            RealmNode currNode = (RealmNode) realmNode;
            Realm realm = (Realm) currNode.getLookup().lookup(Realm.class);
            try {
              realm.ensureInitialized();
            } catch (DirectoryException e) {
              ErrorManager.getDefault().notify(e);
            }
            realms[i] = realm;
            i++;
          }
        }
      }
    }
    return realms;
  }

  /** Creates new form NewDirObjectVisualPanel1 */
  public SelectObjectTypePanel() {
    setName(Messages.getString("NewDirObject0_name")); //$NON-NLS-1$
    Realm[] arrayRealm = loadRealms();
    for(int i=0; arrayRealm.length > i; i++) {
    	realms.add(arrayRealm[i]);
    }
    
    initComponents();
  }

  // You can use a settings object to keep track of state. Normally the
  // settings object will be the WizardDescriptor, so you can use
  // WizardDescriptor.getProperty & putProperty to store information entered
  // by the user.
  public void readSettings(Object settings) {
    wd = (WizardDescriptor) settings;
    realmComboBox.setSelectedItem(wd.getProperty("realm")); //$NON-NLS-1$
    typeComboBox.setSelectedItem(wd.getProperty("type")); //$NON-NLS-1$
  }

  public final void removeChangeListener(ChangeListener l) {
    synchronized (listeners) {
      listeners.remove(l);
    }
  }

  public void storeSettings(Object settings) {
    wd = (WizardDescriptor) settings;
    wd.putProperty("type", typeComboBox.getSelectedItem()); //$NON-NLS-1$
    wd.putProperty("realm", realmComboBox.getSelectedItem()); //$NON-NLS-1$
  }
}
