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
package org.openthinclient.service.dhcp;

import org.apache.directory.server.dhcp.DhcpException;
import org.apache.directory.server.dhcp.messages.DhcpMessage;
import org.apache.directory.server.dhcp.messages.HardwareAddress;
import org.apache.directory.server.dhcp.messages.MessageType;
import org.apache.directory.server.dhcp.options.AddressOption;
import org.apache.directory.server.dhcp.options.OptionsField;
import org.apache.directory.server.dhcp.options.dhcp.ServerIdentifier;
import org.apache.directory.server.dhcp.options.dhcp.VendorClassIdentifier;
import org.apache.directory.server.dhcp.options.vendor.RootPath;
import org.apache.directory.server.dhcp.service.AbstractDhcpService;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.UnrecognizedClient;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaLoadingException;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.RealmService;
import org.openthinclient.common.model.service.UnrecognizedClientService;
import org.openthinclient.ldap.DirectoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author levigo
 */
public abstract class AbstractPXEService extends AbstractDhcpService {

  /**
   *
   */
  public static final int PXE_DHCP_PORT = 4011;
  /**
   * A map of on-going conversations.
   */
  protected static final Map<RequestID, Conversation> conversations = Collections
          .synchronizedMap(new HashMap<RequestID, Conversation>());
  private static final Logger logger = LoggerFactory.getLogger(AbstractPXEService.class);
  private final RealmService realmService;
  private final ClientService clientService;
  private final UnrecognizedClientService unrecognizedClientService;
  private final Schema realmSchema;
  private final Set<Realm> realms;
  private String defaultNextServerAddress;
  private volatile boolean trackUnrecognizedPXEClients;
  private DhcpServiceConfiguration.PXEPolicy policy;

  public AbstractPXEService(RealmService realmService, ClientService clientService, UnrecognizedClientService unrecognizedClientService, SchemaProvider schemaProvider) throws DirectoryException {
    this.realmService = realmService;
    this.clientService = clientService;
    this.unrecognizedClientService = unrecognizedClientService;

    try {
      realmSchema = schemaProvider.getSchema(Realm.class, null);
      realms = this.realmService.findAllRealms();

      for (final Realm realm : realms) {
        logger.info("Serving realm " + realm);
        realm.setSchema(realmSchema);
      }
    } catch (final SchemaLoadingException e) {
      throw new DirectoryException("Can't load schemas", e);
    } catch (final Exception e) {
      logger.error("Can't init directory", e);
      throw e;
    }
  }

  protected static void expireConversations() {
    synchronized (conversations) {
      for (final Iterator<Conversation> i = conversations.values().iterator(); i
              .hasNext(); ) {
        final Conversation c = i.next();
        if (c.isExpired()) {
          if (logger.isInfoEnabled())
            logger.info("Expiring expired conversation " + c);
          i.remove();
        }
      }
    }
  }

  /**
   * Determine whether the given address is the all-zero address 0.0.0.0
   */
  protected static boolean isZeroAddress(InetAddress a) {
    final byte addr[] = a.getAddress();
    for (int i = 0; i < addr.length; i++)
      if (addr[i] != 0)
        return false;

    return true;
  }

  /**
   * Determine whether the given address is in the subnet specified by the
   * network address and the address prefix.
   */
  protected static boolean isInSubnet(byte[] ip, byte[] network, short prefix) {
    if (ip.length != network.length)
      return false;

    if (prefix / 8 > ip.length)
      return false;

    int i = 0;
    while (prefix >= 8 && i < ip.length) {
      if (ip[i] != network[i])
        return false;
      i++;
      prefix -= 8;
    }
    final byte mask = (byte) ~((1 << 8 - prefix) - 1);

    return (ip[i] & mask) == (network[i] & mask);
  }

  public boolean isTrackUnrecognizedPXEClients() {
    return trackUnrecognizedPXEClients;
  }

  public void setTrackUnrecognizedPXEClients(boolean trackUnrecognizedPXEClients) {
    this.trackUnrecognizedPXEClients = trackUnrecognizedPXEClients;
  }

  protected boolean assertCorrectPort(InetSocketAddress localAddress, int port,
                                      DhcpMessage m) {
    // assert correct port
    if (localAddress.getPort() != port) {
      logger.debug("Ignoring " + m.getMessageType() + " on wrong port "
              + localAddress.getPort());
      return false;
    }

    return true;
  }

  /**
   * Track an unrecognized client.
   *
   * @param discover      the initial discover message sent by the client
   * @param hostname      the client's host name (if known)
   * @param clientAddress the client's ip address (if known)
   */
  protected void trackUnrecognizedClient(DhcpMessage discover, String hostname,
                                         String clientAddress) {
    final String hwAddressString = discover.getHardwareAddress()
            .getNativeRepresentation().toLowerCase();

    try {
      if (isTrackUnrecognizedPXEClients())
        if (unrecognizedClientService.findByHwAddress(hwAddressString).isEmpty()) {
          final VendorClassIdentifier vci = (VendorClassIdentifier) discover
                  .getOptions().get(VendorClassIdentifier.class);

          final UnrecognizedClient uc = new UnrecognizedClient();

          // invent a client name, if it is not yet known.
          if (null == hostname)
            hostname = hwAddressString;

          uc.setName(hostname);

          uc.setMacAddress(hwAddressString);
          uc.setIpHostNumber(clientAddress);
          uc.setDescription((vci != null ? vci.getString() : "")
                  + " first seen: " + new Date());

          unrecognizedClientService.add(uc);
        }
    } catch (final RuntimeException e) {
      logger.error("Can't track unrecognized client", e);
    }
  }

  /**
   * @param localAddress
   * @param clientAddress
   * @param request
   * @return
   */
  protected String getLogDetail(InetSocketAddress localAddress,
                                InetSocketAddress clientAddress, DhcpMessage request) {
    final VendorClassIdentifier vci = (VendorClassIdentifier) request
            .getOptions().get(VendorClassIdentifier.class);
    return " on "
            + (null != localAddress ? localAddress : "<null>")
            + " from "
            + (null != clientAddress ? clientAddress : "<null>")
            + " MAC="
            + (null != request.getHardwareAddress()
            ? request.getHardwareAddress()
            : "<null>") + " ID=" + (null != vci ? vci.getString() : "<???>");
  }

  /**
   * Check if the request comes from a PXE client by looking at the
   * VendorClassIdentifier.
   */
  protected boolean isPXEClient(DhcpMessage request) {
    final VendorClassIdentifier vci = (VendorClassIdentifier) request
            .getOptions().get(VendorClassIdentifier.class);
    return null != vci && vci.getString().startsWith("PXEClient:");
  }

  /**
   * Check whether the PXE client which originated the message is elegible for
   * PXE proxy service.
   */
  protected Client getClient(String hwAddressString,
                             InetSocketAddress clientAddress, DhcpMessage request) {
    try {
      Set<Client> found = clientService.findByHwAddress(hwAddressString);

      if (found.size() > 0) {
        if (found.size() > 1)
          logger.warn("Found more than one client for hardware address "
                  + request.getHardwareAddress());

        return found.iterator().next();
      } else if (found.size() == 0) {
        // all clients may be served, if there is a default client configured
        if (policy == DhcpServiceConfiguration.PXEPolicy.ANY_CLIENT) {
          return clientService.getDefaultClient();
        }
      }
      return null;
    } catch (final RuntimeException e) {
      logger.error("Can't query for client for PXE service", e);
      return null;
    }
  }

  /**
   * @param localAddress
   * @param client
   * @return
   */
  protected InetAddress getNextServerAddress(String paramName,
                                             InetSocketAddress localAddress, Client client) {
    InetAddress nsa = null;
    final String value = client.getValue(paramName);
    if (value != null && !value.contains("${myip}"))
      nsa = safeGetInetAddress(value);

    if (null == nsa && null != defaultNextServerAddress)
      nsa = safeGetInetAddress(defaultNextServerAddress);

    if (null == nsa)
      nsa = localAddress.getAddress();

    return nsa;
  }

  /**
   * @param name
   * @return
   */
  private InetAddress safeGetInetAddress(String name) {
    try {
      return InetAddress.getByName(name);
    } catch (final IOException e) {
      logger.warn("Invalid inet address: " + name);
      return null;
    }
  }

  /*
   * @see
   * org.apache.directory.server.dhcp.service.AbstractDhcpService#handleREQUEST
   * (java.net.InetSocketAddress,
   * org.apache.directory.server.dhcp.messages.DhcpMessage)
   */
  @Override
  protected DhcpMessage handleREQUEST(InetSocketAddress localAddress,
                                      InetSocketAddress clientAddress, DhcpMessage request)
          throws DhcpException {
    // detect PXE client
    if (!isPXEClient(request)) {
      if (logger.isDebugEnabled())
        logger.debug("Ignoring non-PXE REQUEST"
                + getLogDetail(localAddress, clientAddress, request));
      return null;
    }

    if (logger.isInfoEnabled())
      logger.info("Got PXE REQUEST"
              + getLogDetail(localAddress, clientAddress, request));

    // we don't react to requests here, unless they go to port 4011
    if (!assertCorrectPort(localAddress, 4011, request))
      return null;

    // find conversation
    final RequestID id = new RequestID(request);
    final Conversation conversation = conversations.get(id);

    if (null == conversation) {
      if (logger.isInfoEnabled())
        logger.info("Got PXE REQUEST for which there is no conversation"
                + getLogDetail(localAddress, clientAddress, request));
      return null;
    }

    synchronized (conversation) {
      if (conversation.isExpired()) {
        if (logger.isInfoEnabled())
          logger.info("Got PXE REQUEST for an expired conversation: "
                  + conversation);
        conversations.remove(id);
        return null;
      }

      final Client client = conversation.getClient();
      if (null == client) {
        logger.warn("Got PXE request which we didn't send an offer. "
                + "Someone else is serving PXE around here?");
        return null;
      }

      if (logger.isDebugEnabled())
        logger.debug("Got PXE REQUEST within " + conversation);

      // check server ident
      final AddressOption serverIdentOption = (AddressOption) request
              .getOptions().get(ServerIdentifier.class);
      if (null != serverIdentOption
              && serverIdentOption.getAddress().isAnyLocalAddress()) {
        if (logger.isDebugEnabled())
          logger.debug("Ignoring PXE REQUEST for server " + serverIdentOption);
        return null; // not me!
      }

      final DhcpMessage reply = initGeneralReply(
              conversation.getApplicableServerAddress(), request);

      reply.setMessageType(MessageType.DHCPACK);

      final OptionsField options = reply.getOptions();

      reply.setNextServerAddress(getNextServerAddress(
              "BootOptions.TFTPBootserver",
              conversation.getApplicableServerAddress(), client));

      final String rootPath = getNextServerAddress("BootOptions.NFSRootserver",
              conversation.getApplicableServerAddress(), client).getHostAddress()
              + ":" + client.getValue("BootOptions.NFSRootPath");
      options.add(new RootPath(rootPath));

      reply.setBootFileName(client.getValue("BootOptions.BootfileName"));

      if (logger.isInfoEnabled())
        logger
                .info("Sending PXE proxy ACK rootPath=" + rootPath
                        + " bootFileName=" + reply.getBootFileName()
                        + " nextServerAddress="
                        + reply.getNextServerAddress().getHostAddress() + " reply="
                        + reply);
      return reply;
    }
  }

  /**
   * Bind service to the appropriate sockets for this type of service.
   *
   * @param acceptor the {@link SocketAcceptor} to be bound
   * @param handler  the {@link IoHandler} to use
   * @param config   the {@link IoServiceConfig} to use
   */
  public abstract void init(IoAcceptor acceptor, IoHandler handler,
                            IoServiceConfig config) throws IOException;

  public DhcpServiceConfiguration.PXEPolicy getPolicy() {
    return policy;
  }

  public void setPolicy(DhcpServiceConfiguration.PXEPolicy policy) {
    this.policy = policy;
  }

  /**
   * Key object used to index conversations.
   */
  public static final class RequestID {
    private final HardwareAddress mac;
    private final int transactionID;

    public RequestID(DhcpMessage m) {
      this.mac = m.getHardwareAddress();
      this.transactionID = m.getTransactionId();
    }

    @Override
    public boolean equals(Object obj) {
      return obj != null //
              && obj.getClass().equals(getClass())
              && transactionID == ((RequestID) obj).transactionID
              && mac.equals(((RequestID) obj).mac);
    }

    @Override
    public int hashCode() {
      return 834532 ^ transactionID ^ mac.hashCode();
    }
  }

  /**
   * Conversation models a DHCP conversation from DISCOVER through REQUEST.
   */
  public final class Conversation {
    private static final int CONVERSATION_EXPIRY = 60000;
    private final DhcpMessage discover;
    private Client client;
    private DhcpMessage offer;
    private long lastAccess;
    private InetSocketAddress applicableServerAddress;

    public Conversation(DhcpMessage discover) {
      this.discover = discover;
      touch();
    }

    private void touch() {
      this.lastAccess = System.currentTimeMillis();
    }

    public boolean isExpired() {
      return lastAccess < System.currentTimeMillis() - CONVERSATION_EXPIRY;
    }

    public DhcpMessage getOffer() {
      touch();
      return offer;
    }

    public void setOffer(DhcpMessage offer) {
      touch();
      this.offer = offer;
    }

    public DhcpMessage getDiscover() {
      touch();
      return discover;
    }

    public Client getClient() {
      touch();
      return client;
    }

    public void setClient(Client client) {
      this.client = client;
    }

    @Override
    public String toString() {
      return "Conversation[" + discover.getHardwareAddress() + "/"
              + discover.getTransactionId() + "]: age="
              + (System.currentTimeMillis() - lastAccess) + ", client=" + client;
    }

    public InetSocketAddress getApplicableServerAddress() {
      return applicableServerAddress;
    }

    public void setApplicableServerAddress(
            InetSocketAddress applicableServerAddress) {
      this.applicableServerAddress = applicableServerAddress;
    }
  }
}
