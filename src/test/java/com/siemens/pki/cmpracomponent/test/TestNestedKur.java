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
package com.siemens.pki.cmpracomponent.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyStoreException;

import org.bouncycastle.asn1.cmp.CMPCertificate;
import org.bouncycastle.asn1.cmp.CMPObjectIdentifiers;
import org.bouncycastle.asn1.cmp.CertRepMessage;
import org.bouncycastle.asn1.cmp.PKIBody;
import org.bouncycastle.asn1.cmp.PKIMessage;
import org.bouncycastle.asn1.crmf.AttributeTypeAndValue;
import org.bouncycastle.asn1.crmf.CertId;
import org.bouncycastle.asn1.crmf.CertTemplateBuilder;
import org.bouncycastle.asn1.crmf.Controls;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.siemens.pki.cmpracomponent.configuration.CheckAndModifyResult;
import com.siemens.pki.cmpracomponent.configuration.CkgContext;
import com.siemens.pki.cmpracomponent.configuration.CmpMessageInterface;
import com.siemens.pki.cmpracomponent.configuration.Configuration;
import com.siemens.pki.cmpracomponent.configuration.CredentialContext;
import com.siemens.pki.cmpracomponent.configuration.InventoryInterface;
import com.siemens.pki.cmpracomponent.configuration.NestedEndpointContext;
import com.siemens.pki.cmpracomponent.configuration.SupportMessageHandlerInterface;
import com.siemens.pki.cmpracomponent.configuration.VerificationContext;
import com.siemens.pki.cmpracomponent.msggeneration.PkiMessageGenerator;
import com.siemens.pki.cmpracomponent.protection.ProtectionProvider;
import com.siemens.pki.cmpracomponent.test.framework.ConfigurationFactory;
import com.siemens.pki.cmpracomponent.test.framework.EnrollmentResult;
import com.siemens.pki.cmpracomponent.test.framework.HeaderProviderForTest;
import com.siemens.pki.cmpracomponent.test.framework.SignatureValidationCredentials;
import com.siemens.pki.cmpracomponent.test.framework.TrustChainAndPrivateKey;
import com.siemens.pki.cmpracomponent.util.MessageDumper;

public class TestNestedKur extends OnlineEnrollmentTestcaseBase {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(TestNestedKur.class);

    public static Configuration buildSignatureBasedLraConfiguration()
            throws KeyStoreException, Exception {
        return new Configuration() {
            @Override
            public CkgContext getCkgConfiguration(final String certProfile,
                    final int bodyType) {
                fail(String.format(
                        "LRA: getCkgConfiguration called with certprofile: {}, type: {}",
                        certProfile, MessageDumper.msgTypeAsString(bodyType)));
                return null;
            }

            @Override
            public CmpMessageInterface getDownstreamConfiguration(
                    final String certProfile, final int bodyType) {
                LOGGER.debug(
                        "LRA: getDownstreamConfiguration called with certprofile: {}, type: {}",
                        certProfile, MessageDumper.msgTypeAsString(bodyType));
                return new CmpMessageInterface() {

                    @Override
                    public VerificationContext getInputVerification() {
                        switch (certProfile) {
                        case "certProfileForKur":
                        case "certProfileForRr":
                            return new SignatureValidationCredentials(
                                    "credentials/ENROLL_Root.pem", null);
                        }
                        return new SignatureValidationCredentials(
                                "credentials/CMP_EE_Root.pem", null);
                    }

                    @Override
                    public NestedEndpointContext getNestedEndpointContext() {
                        return null;
                    }

                    @Override
                    public CredentialContext getOutputCredentials() {
                        try {
                            return new TrustChainAndPrivateKey(
                                    "credentials/CMP_LRA_DOWNSTREAM_Keystore.p12",
                                    "Password".toCharArray());
                        } catch (final Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public ReprotectMode getReprotectMode() {
                        return ReprotectMode.reprotect;
                    }

                    @Override
                    public boolean getSuppressRedundantExtraCerts() {
                        return false;
                    }

                    @Override
                    public boolean isCacheExtraCerts() {
                        return false;
                    }

                    @Override
                    public boolean isMessageTimeDeviationAllowed(
                            final long deviation) {
                        return true;
                    }
                };
            }

            @Override
            public VerificationContext getEnrollmentTrust(
                    final String certProfile, final int bodyType) {
                LOGGER.debug(
                        "LRA: getEnrollmentTrust called with certprofile: {}, type: {}",
                        certProfile, MessageDumper.msgTypeAsString(bodyType));
                return new SignatureValidationCredentials(
                        "credentials/ENROLL_Root.pem", null);
            }

            @Override
            public boolean getForceRaVerifyOnUpstream(final String certProfile,
                    final int bodyType) {
                LOGGER.debug(
                        "LRA: getForceRaVerifyOnUpstream called with certprofile: {}, type: {}",
                        certProfile, MessageDumper.msgTypeAsString(bodyType));
                return false;
            }

            @Override
            public InventoryInterface getInventory(final String certProfile,
                    final int bodyType) {
                LOGGER.debug(
                        "LRA: getInventory called with certprofile: {}, type: {}",
                        certProfile, MessageDumper.msgTypeAsString(bodyType));
                return new InventoryInterface() {

                    @Override
                    public CheckAndModifyResult checkAndModifyCertRequest(
                            final byte[] transactionID,
                            final String requesterDn, final byte[] certTemplate,
                            final String requestedSubjectDn) {
                        LOGGER.debug(
                                "LRA: checkAndModifyCertRequest called with transactionID: {}, requesterDn: {}, requestedSubjectDn: {}",
                                new BigInteger(transactionID), requesterDn,
                                requestedSubjectDn);
                        return new CheckAndModifyResult() {

                            @Override
                            public byte[] getUpdatedCertTemplate() {
                                return null;
                            }

                            @Override
                            public boolean isGranted() {
                                return true;
                            }
                        };
                    }

                    @Override
                    public boolean checkP10CertRequest(
                            final byte[] transactionID,
                            final String requesterDn,
                            final byte[] pkcs10CertRequest,
                            final String requestedSubjectDn) {
                        fail(String.format(
                                "LRA: checkP10CertRequest called with transactionID: {}, requesterDn: {}, requestedSubjectDn: {}",
                                new BigInteger(transactionID), requesterDn,
                                requestedSubjectDn));
                        return false;
                    }

                    @Override
                    public boolean learnEnrollmentResult(
                            final byte[] transactionID,
                            final byte[] certificate, final String serialNumber,
                            final String subjectDN, final String issuerDN) {
                        LOGGER.debug(
                                "LRA: learnEnrollmentResult called with transactionID: {}, serialNumber: {}, subjectDN: {}, issuerDN: {}",
                                new BigInteger(transactionID), serialNumber,
                                subjectDN, issuerDN);
                        return true;
                    }
                };
            }

            @Override
            public int getRetryAfterTimeInSeconds(final String certProfile,
                    final int bodyType) {
                LOGGER.debug(
                        "LRA: getRetryAfterTimeInSeconds called with certprofile: {}, type: {}",
                        certProfile, MessageDumper.msgTypeAsString(bodyType));
                return 1;
            }

            @Override
            public SupportMessageHandlerInterface getSupportMessageHandler(
                    final String certProfile, final String infoTypeOid) {
                LOGGER.debug(
                        "LRA: getSupportMessageHandler called with certprofile: {}, infoTypeOid: {}",
                        certProfile, infoTypeOid);
                return null;
            }

            @Override
            public CmpMessageInterface getUpstreamConfiguration(
                    final String certProfile, final int bodyType) {
                LOGGER.debug(
                        "LRA: getUpstreamConfiguration called with certprofile: {}, type: {}",
                        certProfile, MessageDumper.msgTypeAsString(bodyType));
                return new CmpMessageInterface() {

                    @Override
                    public VerificationContext getInputVerification() {
                        return new SignatureValidationCredentials(
                                "credentials/CMP_CA_Root.pem", null);
                    }

                    @Override
                    public NestedEndpointContext getNestedEndpointContext() {
                        return new NestedEndpointContext() {
                            //misuse CA and EE credentials for nested endpoints
                            @Override
                            public VerificationContext getInputVerification() {
                                return new SignatureValidationCredentials(
                                        "credentials/CMP_EE_Root.pem", null);
                            }

                            @Override
                            public CredentialContext getOutputCredentials() {
                                try {
                                    return new TrustChainAndPrivateKey(
                                            "credentials/CMP_CA_Keystore.p12",
                                            "Password".toCharArray());
                                } catch (final Exception e) {
                                    fail(e.getMessage());
                                    return null;
                                }
                            }

                            @Override
                            public boolean isIncomingRecipientValid(
                                    final String recipient) {
                                LOGGER.debug(
                                        "LRA: isIncomingRecipientValid called with recipient: {}",
                                        recipient);
                                return true;
                            }
                        };
                    }

                    @Override
                    public CredentialContext getOutputCredentials() {

                        try {
                            return new TrustChainAndPrivateKey(
                                    "credentials/CMP_LRA_UPSTREAM_Keystore.p12",
                                    "Password".toCharArray());
                        } catch (final Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public ReprotectMode getReprotectMode() {
                        return ReprotectMode.keep;
                    }

                    @Override
                    public boolean getSuppressRedundantExtraCerts() {
                        return false;
                    }

                    @Override
                    public boolean isCacheExtraCerts() {
                        return false;
                    }

                    @Override
                    public boolean isMessageTimeDeviationAllowed(
                            final long deviation) {
                        return true;
                    }
                };
            }

            @Override
            public boolean isRaVerifiedAcceptable(final String certProfile,
                    final int bodyType) {
                LOGGER.debug(
                        "LRA: isRaVerifiedAcceptable called with certprofile: {}, type: {}",
                        certProfile, MessageDumper.msgTypeAsString(bodyType));
                return false;
            }

        };
    }

    public static Configuration buildSignaturebasedRaConfiguration()
            throws KeyStoreException, Exception {
        return new Configuration() {
            @Override
            public CkgContext getCkgConfiguration(final String certProfile,
                    final int bodyType) {
                fail(String.format(
                        "getCkgConfiguration called with certprofile: {}, type: {}",
                        certProfile, MessageDumper.msgTypeAsString(bodyType)));
                return null;
            }

            @Override
            public CmpMessageInterface getDownstreamConfiguration(
                    final String certProfile, final int bodyType) {
                LOGGER.debug(
                        "getDownstreamConfiguration called with certprofile: {}, type: {}",
                        certProfile, MessageDumper.msgTypeAsString(bodyType));
                return new CmpMessageInterface() {

                    @Override
                    public VerificationContext getInputVerification() {
                        switch (certProfile) {
                        case "certProfileForKur":
                        case "certProfileForRr":
                            return new SignatureValidationCredentials(
                                    "credentials/ENROLL_Root.pem", null);
                        }
                        return new SignatureValidationCredentials(
                                "credentials/CMP_EE_Root.pem", null);
                    }

                    @Override
                    public NestedEndpointContext getNestedEndpointContext() {
                        return new NestedEndpointContext() {
                            //misuse CA and EE credentials for nested endpoints
                            @Override
                            public VerificationContext getInputVerification() {
                                return new SignatureValidationCredentials(
                                        "credentials/CMP_CA_Root.pem", null);
                            }

                            @Override
                            public CredentialContext getOutputCredentials() {
                                try {
                                    return new TrustChainAndPrivateKey(
                                            "credentials/CMP_EE_Keystore.p12",
                                            "Password".toCharArray());
                                } catch (final Exception e) {
                                    fail(e.getMessage());
                                    return null;
                                }
                            }

                            @Override
                            public boolean isIncomingRecipientValid(
                                    final String recipient) {
                                LOGGER.debug(
                                        "isIncomingRecipientValid called with recipient: {}",
                                        recipient);
                                return true;
                            }
                        };
                    }

                    @Override
                    public CredentialContext getOutputCredentials() {
                        try {
                            return new TrustChainAndPrivateKey(
                                    "credentials/CMP_LRA_DOWNSTREAM_Keystore.p12",
                                    "Password".toCharArray());
                        } catch (final Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public ReprotectMode getReprotectMode() {
                        return ReprotectMode.keep;
                    }

                    @Override
                    public boolean getSuppressRedundantExtraCerts() {
                        return false;
                    }

                    @Override
                    public boolean isCacheExtraCerts() {
                        return false;
                    }

                    @Override
                    public boolean isMessageTimeDeviationAllowed(
                            final long deviation) {
                        return true;
                    }
                };
            }

            @Override
            public VerificationContext getEnrollmentTrust(
                    final String certProfile, final int bodyType) {
                LOGGER.debug(
                        "getEnrollmentTrust called with certprofile: {}, type: {}",
                        certProfile, MessageDumper.msgTypeAsString(bodyType));
                return new SignatureValidationCredentials(
                        "credentials/ENROLL_Root.pem", null);
            }

            @Override
            public boolean getForceRaVerifyOnUpstream(final String certProfile,
                    final int bodyType) {
                LOGGER.debug(
                        "getForceRaVerifyOnUpstream called with certprofile: {}, type: {}",
                        certProfile, MessageDumper.msgTypeAsString(bodyType));
                return false;
            }

            @Override
            public InventoryInterface getInventory(final String certProfile,
                    final int bodyType) {
                LOGGER.debug(
                        "getInventory called with certprofile: {}, type: {}",
                        certProfile, MessageDumper.msgTypeAsString(bodyType));
                return new InventoryInterface() {

                    @Override
                    public CheckAndModifyResult checkAndModifyCertRequest(
                            final byte[] transactionID,
                            final String requesterDn, final byte[] certTemplate,
                            final String requestedSubjectDn) {
                        LOGGER.debug(
                                "checkAndModifyCertRequest called with transactionID: {}, requesterDn: {}, requestedSubjectDn: {}",
                                new BigInteger(transactionID), requesterDn,
                                requestedSubjectDn);
                        return new CheckAndModifyResult() {

                            @Override
                            public byte[] getUpdatedCertTemplate() {
                                return null;
                            }

                            @Override
                            public boolean isGranted() {
                                return true;
                            }
                        };
                    }

                    @Override
                    public boolean checkP10CertRequest(
                            final byte[] transactionID,
                            final String requesterDn,
                            final byte[] pkcs10CertRequest,
                            final String requestedSubjectDn) {
                        fail(String.format(
                                "checkP10CertRequest called with transactionID: {}, requesterDn: {}, requestedSubjectDn: {}",
                                new BigInteger(transactionID), requesterDn,
                                requestedSubjectDn));
                        return false;
                    }

                    @Override
                    public boolean learnEnrollmentResult(
                            final byte[] transactionID,
                            final byte[] certificate, final String serialNumber,
                            final String subjectDN, final String issuerDN) {
                        LOGGER.debug(
                                "learnEnrollmentResult called with transactionID: {}, serialNumber: {}, subjectDN: {}, issuerDN: {}",
                                new BigInteger(transactionID), serialNumber,
                                subjectDN, issuerDN);
                        return true;
                    }
                };
            }

            @Override
            public int getRetryAfterTimeInSeconds(final String certProfile,
                    final int bodyType) {
                LOGGER.debug(
                        "getRetryAfterTimeInSeconds called with certprofile: {}, type: {}",
                        certProfile, MessageDumper.msgTypeAsString(bodyType));
                return 1;
            }

            @Override
            public SupportMessageHandlerInterface getSupportMessageHandler(
                    final String certProfile, final String infoTypeOid) {
                LOGGER.debug(
                        "getSupportMessageHandler called with certprofile: {}, infoTypeOid: {}",
                        certProfile, infoTypeOid);
                return null;
            }

            @Override
            public CmpMessageInterface getUpstreamConfiguration(
                    final String certProfile, final int bodyType) {
                LOGGER.debug(
                        "getUpstreamConfiguration called with certprofile: {}, type: {}",
                        certProfile, MessageDumper.msgTypeAsString(bodyType));
                return new CmpMessageInterface() {

                    @Override
                    public VerificationContext getInputVerification() {
                        return new SignatureValidationCredentials(
                                "credentials/CMP_CA_Root.pem", null);
                    }

                    @Override
                    public NestedEndpointContext getNestedEndpointContext() {
                        return null;
                    }

                    @Override
                    public CredentialContext getOutputCredentials() {

                        try {
                            return new TrustChainAndPrivateKey(
                                    "credentials/CMP_LRA_UPSTREAM_Keystore.p12",
                                    "Password".toCharArray());
                        } catch (final Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public ReprotectMode getReprotectMode() {
                        return ReprotectMode.reprotect;
                    }

                    @Override
                    public boolean getSuppressRedundantExtraCerts() {
                        return false;
                    }

                    @Override
                    public boolean isCacheExtraCerts() {
                        return false;
                    }

                    @Override
                    public boolean isMessageTimeDeviationAllowed(
                            final long deviation) {
                        return true;
                    }
                };
            }

            @Override
            public boolean isRaVerifiedAcceptable(final String certProfile,
                    final int bodyType) {
                LOGGER.debug(
                        "isRaVerifiedAcceptable called with certprofile: {}, type: {}",
                        certProfile, MessageDumper.msgTypeAsString(bodyType));
                return false;
            }

        };
    }

    @Before
    public void setUp() throws Exception {
        launchCmpCaAndRaAndLra(buildSignaturebasedRaConfiguration(),
                buildSignatureBasedLraConfiguration());
    }

    /**
     * Updating a Valid Certificate
     *
     * @throws Exception
     */
    @Test
    public void testKur() throws Exception {
        final EnrollmentResult certificateToUpdate =
                executeCrmfCertificateRequest(PKIBody.TYPE_CERT_REQ,
                        PKIBody.TYPE_CERT_REP,
                        ConfigurationFactory
                                .getEeSignaturebasedProtectionProvider(),
                        getEeClient());
        final ProtectionProvider kurProtector = getEnrollmentCredentials()
                .setEndEntityToProtect(certificateToUpdate.getCertificate(),
                        certificateToUpdate.getPrivateKey());
        final KeyPair keyPair =
                ConfigurationFactory.getKeyGenerator().generateKeyPair();
        final Certificate x509v3pkCertToUpdate =
                certificateToUpdate.getCertificate().getX509v3PKCert();
        final X500Name issuer = x509v3pkCertToUpdate.getIssuer();
        final CertTemplateBuilder ctb = new CertTemplateBuilder()
                .setPublicKey(SubjectPublicKeyInfo
                        .getInstance(keyPair.getPublic().getEncoded()))
                .setSubject(x509v3pkCertToUpdate.getSubject())
                .setIssuer(issuer);
        final Controls controls = new Controls(new AttributeTypeAndValue(
                CMPObjectIdentifiers.regCtrl_oldCertID,
                new CertId(new GeneralName(issuer),
                        x509v3pkCertToUpdate.getSerialNumber())));

        final PKIBody kurBody = PkiMessageGenerator.generateIrCrKurBody(
                PKIBody.TYPE_KEY_UPDATE_REQ, ctb.build(), controls,
                keyPair.getPrivate());
        final PKIMessage kur = PkiMessageGenerator.generateAndProtectMessage(
                new HeaderProviderForTest("certProfileForKur"), kurProtector,
                kurBody);

        if (LOGGER.isDebugEnabled()) {
            // avoid unnecessary string processing, if debug isn't enabled
            LOGGER.debug("send:\n" + MessageDumper.dumpPkiMessage(kur));
        }
        final PKIMessage kurResponse = getEeClient().apply(kur);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("got:\n" + MessageDumper.dumpPkiMessage(kurResponse));
        }
        assertEquals("message type", PKIBody.TYPE_KEY_UPDATE_REP,
                kurResponse.getBody().getType());

        final CMPCertificate enrolledCertificate =
                ((CertRepMessage) kurResponse.getBody().getContent())
                        .getResponse()[0].getCertifiedKeyPair()
                                .getCertOrEncCert().getCertificate();

        final PKIMessage certConf =
                PkiMessageGenerator.generateAndProtectMessage(
                        new HeaderProviderForTest(kurResponse.getHeader()),
                        kurProtector, PkiMessageGenerator
                                .generateCertConfBody(enrolledCertificate));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("send:\n" + MessageDumper.dumpPkiMessage(certConf));
        }
        final PKIMessage pkiConf = getEeClient().apply(certConf);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("got:\n" + MessageDumper.dumpPkiMessage(pkiConf));
        }
        assertEquals("message type", PKIBody.TYPE_CONFIRM,
                pkiConf.getBody().getType());

    }
}
