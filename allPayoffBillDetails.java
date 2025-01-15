package com.temenos.tdf.loanRestructure;
import java.math.BigDecimal;
import java.util.List;

import com.temenos.api.T24Record;
import com.temenos.api.TStructure;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.activityhook.ArrangementContext;
import com.temenos.t24.api.complex.aa.activityhook.TransactionData;
import com.temenos.t24.api.complex.aa.contractapi.BalanceMovement;
import com.temenos.t24.api.hook.arrangement.ActivityLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaprddestermamount.AaPrdDesTermAmountRecord;
import com.temenos.t24.api.records.aaproductcatalog.AaProductCatalogRecord;
import com.temenos.t24.api.records.aasimulationrunner.AaSimulationRunnerRecord;
import com.temenos.t24.api.system.DataAccess;

import com.temenos.t24.api.records.aacustomerarrangement.AaCustomerArrangementRecord;
import com.temenos.t24.api.records.aacustomerarrangement.ArrangementClass;
import com.temenos.t24.api.records.aacustomerarrangement.ProductLineClass;
import com.temenos.t24.api.records.aabilldetails.AaBillDetailsRecord;
import com.temenos.t24.api.records.aabilldetails.PaymentTypeClass;

import com.temenos.t24.api.system.Session;

import java.util.ArrayList;

public class allPayoffBillDetails extends ActivityLifecycle{
    DataAccess da = new DataAccess();
    Session session = new Session(this);

    private List<String> getArrangementsForCustomer(String customerId) {
        List<String> arrangementIds = new ArrayList<>();
        try {
           
            
            
            AaCustomerArrangementRecord customerArrangement = new AaCustomerArrangementRecord(
                da.getRecord("AA.CUSTOMER.ARRANGEMENT", customerId));
            for (ProductLineClass productLine : customerArrangement.getProductLine()) {
                if (productLine.getProductLine().getValue().equals("LENDING")) {
                    for (ArrangementClass arrangement : productLine.getArrangement()) {
                        arrangementIds.add(arrangement.getArrangement().getValue());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching arrangements for customer: " + e.getMessage());
        }
        return arrangementIds;
    }

    private List<AaBillDetailsRecord> getPayoffBills(String arrangementId) {
        List<AaBillDetailsRecord> payoffBills = new ArrayList<>();
        try {
            
            List<String> billIds = da.selectRecords("","AA.BILL.DETAILS", "","WITH @ID LIKE ..." + arrangementId);
            for (String billId : billIds) {
                AaBillDetailsRecord billRecord = new AaBillDetailsRecord(da.getRecord("AA.BILL.DETAILS", billId));
                // Filter by PAYMENT.TYPE starts with "PAYOFF$" and PAYMENT.METHOD is "INFO"
                PaymentTypeClass paymentType = billRecord.getPaymentType();
                PaymentMethodClass paymentMethod = billRecord.getPaymentType();
                if (paymentType != null && paymentType.getValue().startsWith("PAYOFF$")
                    && paymentMethod != null && paymentMethod.getValue().equals("INFO")) {
                    payoffBills.add(billRecord);
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching payoff bills: " + e.getMessage());
        }
        return payoffBills;
    }

    private void processPayoffBill(AaBillDetailsRecord billRecord) {
        // Get property values: ACCOUNT, DEFERREDPFT, and check for PENALTYPFT
        String account = billRecord.
        String deferredPft = billRecord.getDeferredPft().getValue();
        String penaltyPft = billRecord.getPenaltyPft() != null ? billRecord.getPenaltyPft().getValue() : null;

        // If PENALTYPFT exists, get OUTSTANDING for property number 3 (OS.PROP.AMOUNT)
        if (penaltyPft != null) {
            double outstandingAmount = getOutstandingAmountForProperty(3); // "3" for outstanding
            System.out.println("Outstanding amount for property: " + outstandingAmount);
        }

        // Log or process the account, deferredPft, and other fields
        System.out.println("Account: " + account);
        System.out.println("Deferred PFT: " + deferredPft);
    }

    private double getOutstandingAmountForProperty(int propertyNumber) {
        String outstandingAmount;
        try {
            // Fetch outstanding amount for the specified property number
            List<String> outstandingRecords = da.selectRecords("AA.OUTSTANDING.PROPERTY",
                "WITH @PROPERTY.NUMBER = " + propertyNumber);
            for (String recordId : outstandingRecords) {
                AaBillDetailsRecord outstandingRecord = new AaBillDetailsRecord(da.getRecord("AA.OUTSTANDING.PROPERTY", recordId));
                outstandingAmount = outstandingRecord.getLocalRefField("OS.PROP.AMOUNT").getValue().toString();
                outstandingAmount = outstandingRecord.getOsTotalAmount().getValue().toString();
            }
        } catch (Exception e) {
            System.err.println("Error fetching outstanding amount: " + e.getMessage());
        }
        return outstandingAmount;
    }
}
