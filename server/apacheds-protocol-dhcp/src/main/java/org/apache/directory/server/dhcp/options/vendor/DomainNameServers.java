/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.apache.directory.server.dhcp.options.vendor;


import org.apache.directory.server.dhcp.options.AddressListOption;


/**
 * The domain name server option specifies a list of Domain Name System (STD 13,
 * RFC 1035 [8]) name servers available to the client. Servers SHOULD be listed
 * in order of preference. The code for the domain name server option is 6. The
 * minimum length for this option is 4 octets, and the length MUST always be a
 * multiple of 4.
 */
public class DomainNameServers extends AddressListOption
{
    /*
     * @see org.apache.directory.server.dhcp.options.DhcpOption#getTag()
     */
    public byte getTag()
    {
        return 6;
    }
}
