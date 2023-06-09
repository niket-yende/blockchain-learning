/*
 * Copyright 2018 IBM All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package main

import (
	"crypto/x509"
	"encoding/pem"
	"errors"
	"fmt"

	"github.com/golang/protobuf/proto"
	"github.com/hyperledger/fabric/protos/msp"
)

func getTxCreatorInfo(creator []byte) (string, string, error) {
	var certASN1 *pem.Block
	var cert *x509.Certificate
	var err error

	creatorSerializedId := &msp.SerializedIdentity{}
	err = proto.Unmarshal(creator, creatorSerializedId)
	if err != nil {
		fmt.Printf("Error unmarshalling creator identity: %s\n", err.Error())
		return "", "", err
	}

	if len(creatorSerializedId.IdBytes) == 0 {
		return "", "", errors.New("Empty certificate")
	}
	certASN1, _ = pem.Decode(creatorSerializedId.IdBytes)
	cert, err = x509.ParseCertificate(certASN1.Bytes)
	if err != nil {
		return "", "", err
	}

	return creatorSerializedId.Mspid, cert.Issuer.CommonName, nil
}

// For now, just hardcode an ACL
// We will support attribute checks in an upgrade

func authenticateExporterOrg(mspID string, certCN string) bool {
	return (mspID == "ExporterOrgMSP") && (certCN == "ca.exporterorg.loc.com")
}

func authenticateImporterOrg(mspID string, certCN string) bool {
	return (mspID == "ImporterOrgMSP") && (certCN == "ca.importerorg.loc.com")
}

func authenticateCarrierOrg(mspID string, certCN string) bool {
	return (mspID == "CarrierOrgMSP") && (certCN == "ca.carrierorg.loc.com")
}

func authenticateIssuingBankOrg(mspID string, certCN string) bool {
	return (mspID == "IssuingBankOrgMSP") && (certCN == "ca.issuingbankorg.loc.com")
}
func authenticateAdvisingBankOrg(mspID string, certCN string) bool {
	return (mspID == "AdvisingBankOrgMSP") && (certCN == "ca.advisingbankorg.loc.com")
}
