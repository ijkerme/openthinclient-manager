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
package org.openthinclient.console.nodes.views;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.text.MessageFormat;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.decorator.AlternateRowHighlighter;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.decorator.HighlighterPipeline;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;
import org.openide.ErrorManager;
import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.schema.ChoiceNode;
import org.openthinclient.common.model.schema.Node;
import org.openthinclient.common.model.schema.PasswordNode;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaLoadingException;
import org.openthinclient.console.Messages;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.Sizes;
import com.jgoodies.forms.util.LayoutStyle;

/**
 * @author Natalie Bohnert
 */
@SuppressWarnings("serial")
public class ProfileView extends JXPanel {
	private static class ProfileTreeTableModel extends AbstractTreeTableModel {
		private final Profile profile;

		ProfileTreeTableModel(org.openthinclient.common.model.schema.Node root,
				Profile profile) {
			super(root);
			this.profile = profile;
		}

		/*
		 * @see org.jdesktop.swingx.treetable.TreeTableModel#getValueAt(java.lang.Object,
		 *      int)
		 */
		public Object getValueAt(Object node, int index) {

			Node n = (Node) node;
			switch (index){
				case 0 :
					return n.getLabel();
				case 1 :
					if (node instanceof PasswordNode)
						return "***********"; //$NON-NLS-1$
					if (node instanceof ChoiceNode)
						return ((ChoiceNode) node).getSelectedOption();
					else
						return profile.getValue(n.getKey());

				case 2 :
					final String k = n.getKey();
					final String value = profile.getValue(k);
					final boolean containsValue = profile.containsValue(k);
					final String definingProfile = profile.getDefiningProfile(k, true);
					if (containsValue) {
						final String overriddenValue = profile.getOverriddenValue(k);
						if (null != overriddenValue)
							return MessageFormat.format(Messages
									.getString("ProfileViewFactory.overrides"), //$NON-NLS-1$
									overriddenValue, definingProfile);
					} else {
						if (null != value)
							return MessageFormat.format(Messages
									.getString("ProfileViewFactory.defaultFrom"), //$NON-NLS-1$
									definingProfile);
						else
							return Messages.getString("ProfileViewFactory.noDefault"); //$NON-NLS-1$
					}
			}
			return ""; //$NON-NLS-1$
		}

		/*
		 * @see org.jdesktop.swingx.treetable.AbstractTreeTableModel#getColumnName(int)
		 */
		@Override
		public String getColumnName(int index) {
			switch (index){
				case 0 :
					return Messages.getString("ProfileViewFactory.Name"); //$NON-NLS-1$
				case 1 :
					return Messages.getString("ProfileViewFactory.Value"); //$NON-NLS-1$
				case 2 :
					return Messages.getString("ProfileViewFactory.Remarks"); //$NON-NLS-1$
			}
			return "?"; //$NON-NLS-1$
		}

		/*
		 * @see org.jdesktop.swingx.treetable.TreeTableModel#setValueAt(java.lang.Object,
		 *      java.lang.Object, int)
		 */
		public void setValueAt(Object arg0, Object arg1, int arg2) {
			throw new IllegalArgumentException("not editable"); //$NON-NLS-1$
		}

		/*
		 * @see org.jdesktop.swingx.treetable.AbstractTreeTableModel#getColumnCount()
		 */
		@Override
		public int getColumnCount() {
			return 3;
		}

		/*
		 * @see org.jdesktop.swingx.treetable.AbstractTreeTableModel#getChild(java.lang.Object,
		 *      int)
		 */
		@Override
		public Object getChild(Object parent, int idx) {
			return ((org.openthinclient.common.model.schema.Node) parent).getChildren()
					.get(idx);
		}

		/*
		 * @see org.jdesktop.swingx.treetable.AbstractTreeTableModel#getChildCount(java.lang.Object)
		 */
		@Override
		public int getChildCount(Object parent) {
			return ((org.openthinclient.common.model.schema.Node) parent).getChildren()
					.size();
		}
	}

	@SuppressWarnings("serial")
	private static class MyTreeCellRenderer extends DefaultTreeCellRenderer {
		private JTree tree;

		/*
		 * @see javax.swing.tree.DefaultTreeCellRenderer#getTreeCellRendererComponent(javax.swing.JTree,
		 *      java.lang.Object, boolean, boolean, boolean, int, boolean)
		 */
		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			this.tree = tree;

			if (value instanceof Node)
				value = ((Node) value).getLabel();

			JLabel r = (JLabel) super.getTreeCellRendererComponent(tree, value, sel,
					expanded, leaf, row, hasFocus);
			r.setIcon(null);

			return r;
		}

		public void paint(Graphics g) {
			String fullText = super.getText();
			// getText() calls tree.convertValueToText();
			// tree.convertValueToText() should call treeModel.convertValueToText(),
			// if possible

			String shortText = SwingUtilities.layoutCompoundLabel(this, g
					.getFontMetrics(), fullText, getIcon(), getVerticalAlignment(),
					getHorizontalAlignment(), getVerticalTextPosition(),
					getHorizontalTextPosition(), getItemRect(itemRect), iconRect,
					textRect, getIconTextGap());

			/**
			 * TODO: setText is more heavyweight than we want in this situation. Make
			 * JLabel.text protected instead of private.
			 */

			setText(shortText); // temporarily truncate text
			super.paint(g);
			setText(fullText); // restore full text
		}

		private Rectangle getItemRect(Rectangle itemRect) {
			getBounds(itemRect);
			itemRect.width = tree.getWidth() - itemRect.x;
			return itemRect;
		}

		// Rectangles filled in by SwingUtilities.layoutCompoundLabel();
		private final Rectangle iconRect = new Rectangle();
		private final Rectangle textRect = new Rectangle();
		// Rectangle filled in by this.getItemRect();
		private final Rectangle itemRect = new Rectangle();
	}

	public ProfileView(Profile profile, Realm realm) {
		setLayout(new BorderLayout());
		try {
			Schema schema = profile.getSchema(realm);
			
			Node invisibleNote = null;		
			if(profile.getClass() == Realm.class) {
				invisibleNote = schema.getChild("invisibleObjects");
				schema.removeChild(invisibleNote);
			}

			if (schema != null) {
				JXTreeTable tt = new JXTreeTable(new ProfileTreeTableModel(schema,
						profile));
				tt.setShowHorizontalLines(false);
				tt.setShowVerticalLines(false);
				tt.expandAll();
				tt.setTreeCellRenderer(new MyTreeCellRenderer());
				tt.setHighlighters(new HighlighterPipeline(
						new Highlighter[]{AlternateRowHighlighter.genericGrey}));

				add(tt.getTableHeader(), BorderLayout.NORTH);
				add(tt, BorderLayout.CENTER);

				setBorder(Borders.createEmptyBorder(LayoutStyle.getCurrent()
						.getRelatedComponentsPadY(), Sizes.ZERO, LayoutStyle.getCurrent()
						.getRelatedComponentsPadY(), Sizes.ZERO));
				setOpaque(false);

				if (invisibleNote != null) {
					schema.addChild(invisibleNote);
				}
			}
		} catch (SchemaLoadingException e) {
			ErrorManager
					.getDefault()
					.annotate(
							e,
							ErrorManager.EXCEPTION,
							Messages
									.getString("ProfileViewFactory.errors.couldNotLoadSchema"), null, null, null); //$NON-NLS-1$
			ErrorManager.getDefault().notify(e);
		}

		setName(Messages.getString("ProfileViewFactory.title")); //$NON-NLS-1$
	}
}