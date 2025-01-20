package com.temenos.tdf.loanRestructure;
import com.temenos.t24.api.system.DataAccess;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.api.exceptions.T24IOException;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.activityhook.ArrangementContext;
import com.temenos.t24.api.complex.aa.activityhook.TransactionData;
import com.temenos.t24.api.complex.aa.contractapi.BalanceMovement;
import com.temenos.t24.api.hook.arrangement.ActivityLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aabilldetails.AaBillDetailsRecord;

import com.temenos.t24.api.records.aabilldetails.PropertyClass;
import com.temenos.t24.api.records.aaprddestermamount.AaPrdDesTermAmountRecord;
import com.temenos.t24.api.records.aaproductcatalog.AaProductCatalogRecord;
import com.temenos.t24.api.records.aasimulationcapture.AaSimulationCaptureRecord;
import com.temenos.t24.api.records.aasimulationrunner.AaSimulationRunnerRecord;
import com.temenos.t24.api.tables.ebtdfpayoffbills.EbTdfPayoffBillsRecord;
import com.temenos.t24.api.tables.ebtdfpayoffbills.EbTdfPayoffBillsTable;
import com.temenos.t24.api.tables.ebtdfpayoffbills.LOldLoanArrClass;
import com.temenos.t24.api.tables.ebtdfpayoffbillref.EbTdfPayoffBillRefRecord;
import com.temenos.t24.api.tables.ebtdfpayoffbillref.EbTdfPayoffBillRefRecord;
import com.temenos.t24.api.tables.ebtdfpayoffbillref.EbTdfPayoffBillRefTable;
import com.temenos.t24.api.tables.ebtdfpayoffbillref.BillRefClass;
// Class Definition
//before new table
public class postBillDetails extends ActivityLifecycle {

    @Override
    public void postCoreTableUpdate(AaAccountDetailsRecord accountDetailRecord,
            AaArrangementActivityRecord arrangementActivityRecord, ArrangementContext arrangementContext,
            AaArrangementRecord arrangementRecord, AaArrangementActivityRecord masterActivityRecord,
            TStructure productPropertyRecord, AaProductCatalogRecord productRecord, TStructure record,
            List<TransactionData> transactionData, List<TStructure> transactionRecord) {

        // Debug: Start of method
        System.out.println("Entered postCoreTableUpdate method.");

        if (arrangementContext.getActivityStatus().equalsIgnoreCase("AUTH")) {
            System.out.println("Activity status is AUTH. Proceeding with execution.");
            DataAccess dataAccess = new DataAccess(this);

            String aaidx = null;
            Contract contract = new Contract(this);

            try {
                contract.setContractId(arrangementContext.getArrangementId().toString());
                aaidx = contract.getContractId().toString();
                System.out.println("Contract ID retrieved: " + aaidx);
            } catch (Exception e) {
                System.out.println("Error while retrieving Contract ID: " + e.getMessage());
            }

            List<String> billIds = null;
            try {
                billIds = contract.getBillIds(null, null, "PAYOFF", "INFO", "", "", "", null, "");
                System.out.println("Bill IDs retrieved: " + billIds);
            } catch (Exception e) {
                System.out.println("Error while retrieving Bill IDs: " + e.getMessage());
            }

            if (billIds != null && !billIds.isEmpty()) {
                String billId = billIds.get(0);
                System.out.println("Selected Bill ID: " + billId);

                String customerId = arrangementActivityRecord.getCustomer(0).getCustomer().getValue();
                System.out.println("Customer ID: " + customerId);

                AaBillDetailsRecord billRec = null;
                try {
                    billRec = new AaBillDetailsRecord(dataAccess.getRecord("", "AA.BILL.DETAILS", "", billId));
                    System.out.println("Successfully retrieved AA BILL DETAILS for Bill ID: " + billId);
                } catch (Exception e) {
                    System.out.println("Error while retrieving AA BILL DETAILS: " + e.getMessage());
                }

                if (billRec != null) {
                    BigDecimal accountAmount = BigDecimal.ZERO;
                    BigDecimal deferProfitAmount = BigDecimal.ZERO;
                    BigDecimal penaltyAmount = BigDecimal.ZERO;

                    Iterator<PropertyClass> propertyIterator = billRec.getProperty().iterator();
                    System.out.println("Iterating through bill properties.");

                    while (propertyIterator.hasNext()) {
                        PropertyClass property = propertyIterator.next();
                        System.out.println("Processing property: " + property.getProperty().getValue());

                        if ("ACCOUNT".equals(property.getProperty().getValue())) {
                            accountAmount = accountAmount.add(new BigDecimal(property.getOsPropAmount().getValue()));
                            System.out.println("Updated Account Amount: " + accountAmount);
                        }

                        if ("DEFERREDPFT".equals(property.getProperty().getValue())) {
                            deferProfitAmount = deferProfitAmount.add(new BigDecimal(property.getOsPropAmount().getValue()));
                            System.out.println("Updated Deferred Profit Amount: " + deferProfitAmount);
                        }

                        if ("PENALTYPFT".equals(property.getProperty().getValue())) {
                            penaltyAmount = penaltyAmount.add(new BigDecimal(property.getOsPropAmount().getValue()));
                            System.out.println("Updated Penalty Amount: " + penaltyAmount);
                        }
                    }

                    EbTdfPayoffBillsTable billTable = new EbTdfPayoffBillsTable(this);
                    EbTdfPayoffBillsRecord billRecordx = null;

                    try {
                        billRecordx = new EbTdfPayoffBillsRecord(dataAccess.getRecord("EB.TDF.PAYOFF.BILLS", customerId));
                        System.out.println("Opened existing EB.TDF.PAYOFF.BILLS record for Customer ID: " + customerId);
                    } catch (Exception e) {
                        billRecordx = new EbTdfPayoffBillsRecord(this);
                        System.out.println("Created new EB.TDF.PAYOFF.BILLS record for Customer ID: " + customerId);
                    }

                    try {
                        String oldAaId = aaidx.toString();
                        boolean found = false;

                        // Retrieve existing records
                        List<LOldLoanArrClass> existingRecords = billRecordx.getLOldLoanArr();
                        System.out.println("Existing records retrieved: " + (existingRecords != null ? existingRecords.size() : 0));

                        if (existingRecords != null) {
                            for (LOldLoanArrClass recordx : existingRecords) {
                                System.out.println("Checking record with LOldLoanArr: " + recordx.getLOldLoanArr().getValue());
                                if (recordx.getLOldLoanArr().getValue().equals(oldAaId)) {
                                    // Update existing record
                                    recordx.setLAccountAmt(accountAmount.toString());
                                    recordx.setLProfitAmt(deferProfitAmount.toString());
                                    recordx.setLPenaltyAmt(penaltyAmount.toString());
                                    found = true;
                                    System.out.println("Updated existing record for arrangementID: " + oldAaId);
                                    break;
                                }
                            }
                        }
//test
                        //again
                        if (!found) {
                            // Add new record
                            System.out.println("Adding new record for arrangementID: " + oldAaId);
                            LOldLoanArrClass lOldrec = new LOldLoanArrClass();
                            lOldrec.setLOldLoanArr(aaidx.toString());
                            lOldrec.setLAccountAmt(accountAmount.toString());
                            lOldrec.setLProfitAmt(deferProfitAmount.toString());
                            lOldrec.setLPenaltyAmt(penaltyAmount.toString());
                            billRecordx.addLOldLoanArr(lOldrec);
                        }
                    } catch (Exception e) {
                        System.out.println("Error while processing LOldLoanArr: " + e.getMessage());
                    }

                    try {
                        billTable.write(customerId, billRecordx);
                        System.out.println("Successfully wrote to EB.TDF.PAYOFF.BILLS for Customer ID: " + customerId);
                    } catch (T24IOException e) {
                        System.out.println("Error while writing to EB.TDF.PAYOFF.BILLS: " + e.getMessage());
                    }
                }
            } else {
                System.out.println("No Bill IDs found for the given arrangement.");
            }
        } else {
            System.out.println("Activity status is not AUTH. Skipping execution.");
        }

        // Debug: End of method
        System.out.println("Exiting postCoreTableUpdate method.");
    }
}
