package org.openthinclient.web.pkgmngr.ui.design;

import com.vaadin.annotations.AutoGenerated;
import com.vaadin.annotations.DesignRoot;
import com.vaadin.ui.declarative.Design;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Button;
import com.vaadin.ui.TextField;

/**
 * !! DO NOT EDIT THIS FILE !!
 * <p>
 * This class is generated by Vaadin Designer and will be overwritten.
 * <p>
 * Please make a subclass with logic and additional interfaces as needed, e.g class LoginView
 * extends LoginDesign implements View { }
 */
@DesignRoot
@AutoGenerated
@SuppressWarnings("serial")
public class PackageCardsDesign extends VerticalLayout {
  protected CheckBox packageFilerCheckbox;
  protected Button searchButton;
  protected TextField searchTextField;
  protected VerticalLayout packagesListLayout;

  public PackageCardsDesign() {
    Design.read(this);
  }
}