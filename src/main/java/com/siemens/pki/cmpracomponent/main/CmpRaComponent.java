/*
 *  Copyright (c) 2022 Siemens AG
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may
 *  not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 */
package com.siemens.pki.cmpracomponent.main;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.siemens.pki.cmpracomponent.configuration.Configuration;
import com.siemens.pki.cmpracomponent.msgprocessing.CmpRaImplementation;
import com.siemens.pki.cmpracomponent.msgprocessing.P10X509RaImplementation;

public class CmpRaComponent {

    /**
     * interface to access the RA instance
     * with synchronous and/or asynchronous upstream transfer
     * providing support for delayed delivery of responses
     */
    public interface CmpRaInterface {
        /**
         * application provides response received asynchronously from upstream.
         * Must be called after returning null via upstreamExchange()
         * when later receiving a delayed response from upstream.
         *
         * @param response
         *            ASN.1 DER-encoded response received from upstream
         *
         * @throws Exception
         *             on error not handled at CMP level
         */
        void gotResponseAtUpstream(byte[] response) throws Exception;

        /**
         * used by application to provide CMP request from downstream to RA
         * (which may be a poll request) and obtain the corresponding response.
         *
         * @param request
         *            ASN.1 DER-encoded request
         *
         * @return the corresponding ASN.1 DER-encoded response
         *         (which may be a waiting indication or error)
         *         in due time: before timeout on downstream
         * @throws Exception
         *             on error not handled at CMP level
         */
        byte[] processRequest(byte[] request) throws Exception;
    }

    /**
     * create an RA instance that can handle all CMP message types
     * and use cases described in the Lightweight CMP Profile
     *
     * @param configuration
     *            RA configuration provided by embedding application
     * @param upstreamExchange
     *            function to send a ASN.1 DER-encoded CMP request upstream and
     *            potentially receive the related ASN.1 DER-encoded response.
     *            The second argument is the certificate profile extracted from
     *            the CMP request header generalInfo field or <code>null</code>
     *            if no certificate profile was specified.
     *            Must return <code>null</code> if synchronous transfer is not
     *            supported or did not receive a response after relatively short
     *            timeout. May throw an exception on any other (non-recoverable)
     *            error.
     *
     * @return interface to access the RA instance (a)synchronously
     * @throws Exception
     *             in case of invalid configuration
     */
    public static final CmpRaInterface instantiateCmpRaComponent(
            final Configuration configuration,
            final BiFunction<byte[], String, byte[]> upstreamExchange)
            throws Exception {
        return new CmpRaImplementation(configuration, upstreamExchange);
    }

    /**
     * create an RA instance that can directly respond to general messages
     * and handle certificate enrollment using p10cr as described in section
     * "Requesting a certificate from a legacy PKI using a PKCS#10request" in
     * <a href=
     * "https://tools.ietf.org/wg/lamps/draft-ietf-lamps-lightweight-cmp-profile/">Lightweight
     * Certificate Management Protocol (CMP) Profile</a>
     *
     * @param configuration
     *            RA configuration provided by embedding application.
     *            The generated RA instance will not make use of
     *            {@link Configuration#getCkgConfiguration(String, int)},
     *            {@link Configuration#getUpstreamConfiguration(String, int)},
     *            {@link Configuration#getForceRaVerifyOnUpstream(String, int)},
     *            and
     *            {@link Configuration#getRetryAfterTimeInSeconds(String, int)}
     *            because central key generation, upstream CMP messages,
     *            and delayed delivery are not supported here.
     * @param upstreamP10X509Exchange
     *            function to send ASN.1 DER-encoded PKCS#10 CSR upstream and
     *            return the resulting ASN.1 DER-encoded X509 certificate.
     *            The second argument is the certificate profile extracted from
     *            the CMP request header generalInfo field or <code>null</code>
     *            if no certificate profile was specified.
     *            Asynchronous transfer is not supported here.
     *            Must return <code>null</code> if did not receive a response
     *            after relatively short timeout.
     *            Must throw an exception with a suitable message text in case
     *            the upstream server responded with an application-level error.
     *            May throw an exception on any other (non-recoverable) error.
     *
     * @return function to use by the embedding application
     *         to deliver an ASN.1 DER-encoded CMP request
     *         from the downstream interface to the RA instance and
     *         to obtain the related ASN.1 DER-encoded CMP response
     *         for further delivery towards the end entity.
     * @throws Exception
     *             in case of invalid configuration
     */
    public static final Function<byte[], byte[]> instantiateP10X509CmpRaComponent(
            final Configuration configuration,
            final BiFunction<byte[], String, byte[]> upstreamP10X509Exchange)
            throws Exception {
        return new P10X509RaImplementation(configuration,
                upstreamP10X509Exchange);
    }

    private CmpRaComponent() {

    }
}
