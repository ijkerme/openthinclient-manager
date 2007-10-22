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
package org.openthinclient.ldap;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.log4j.Logger;

/**
 * @author levigo
 */
public class ManyToOneMapping extends AttributeMapping {
  private static final Logger logger = Logger.getLogger(ManyToOneMapping.class);

  private Class refereeType;
  private TypeMapping refereeMapping;

  public ManyToOneMapping(String fieldName, String fieldType)
      throws ClassNotFoundException {
    super(fieldName, fieldType);
    this.refereeType = Class.forName(fieldType);

    if (!Object.class.isAssignableFrom(this.refereeType))
      throw new IllegalArgumentException("The field " + fieldName
          + " is not a subclass of Object");
  }

  /*
   * @see org.openthinclient.common.directory.ldap.AttributeMapping#valueFromAttributes(javax.naming.directory.Attribute)
   */
  @Override
  protected Object valueFromAttributes(Attributes attributes, Object o,
      Transaction tx) throws NamingException, DirectoryException {
    Attribute attribute = attributes.get(fieldName);
    if (null != attribute)
      try {
        return type.getMapping() //
            .getMapping(getFieldType()) //
            .load(attribute.get().toString(), tx);
      } catch (Exception e) {
        logger.error(
            "Can't init many-to-one mapping. Returning NULL for member field.",
            e);
      }

    return null;
  }

  /*
   * @see org.openthinclient.common.directory.ldap.AttributeMapping#initPostLoad()
   */
  @Override
  protected void initPostLoad() {
    super.initPostLoad();
    TypeMapping child = type.getMapping().getMapping(refereeType);
    if (null == child)
      throw new IllegalStateException(this + ": no mapping for peer type "
          + refereeType);

    this.refereeMapping = child;

    child.addReferrer(this);
  }

  /*
   * @see org.openthinclient.ldap.AttributeMapping#getValue(java.lang.Object)
   */
  @Override
  protected Object getValue(Object o) throws DirectoryException {
    Object referenced = super.getValue(o);
    if (null == referenced)
      return null;
    return refereeMapping.getDN(referenced);
  }

  /*
   * @see org.openthinclient.ldap.AttributeMapping#cascadePreSave(java.lang.Object,
   *      org.openthinclient.ldap.Transaction)
   */
  @Override
  protected void cascadePreSave(Object o, Transaction tx)
      throws DirectoryException {
    super.cascadePreSave(o, tx);
    Object referenced = super.getValue(o);
    if (null != referenced)
      refereeMapping.save(referenced, null, tx);
  }
}
