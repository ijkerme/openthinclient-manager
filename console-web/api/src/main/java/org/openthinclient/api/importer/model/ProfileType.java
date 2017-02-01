package org.openthinclient.api.importer.model;

import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Device;
import org.openthinclient.common.model.HardwareType;
import org.openthinclient.common.model.Location;
import org.openthinclient.common.model.Printer;
import org.openthinclient.common.model.Profile;

public enum ProfileType {

  APPLICATION(Application.class),
  HARDWARE_TYPE(HardwareType.class),
  DEVICE(Device.class),
  LOCATION(Location.class),
  CLIENT(Client.class),
  PRINTER(Printer.class);

  private final Class<? extends Profile> targetType;

  ProfileType(Class<? extends Profile> targetType) {
    this.targetType = targetType;
  }

  public Class<? extends Profile> getTargetType() {
    return targetType;
  }
}
