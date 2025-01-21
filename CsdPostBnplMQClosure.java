package com.temenos.tdf.loanRestructure;

import java.util.HashMap;
import java.util.List;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.aa.activityhook.ArrangementContext;
import com.temenos.t24.api.complex.aa.activityhook.TransactionData;
import com.temenos.t24.api.hook.arrangement.ActivityLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaproductcatalog.AaProductCatalogRecord;
import com.temenos.t24.api.system.DataAccess;

public class CsdPostBnplMQClosure extends ActivityLifecycle {

    @Override
    public void postCoreTableUpdate(AaAccountDetailsRecord accountDetailRecord,
            AaArrangementActivityRecord arrangementActivityRecord, ArrangementContext arrangementContext,
            AaArrangementRecord arrangementRecord, AaArrangementActivityRecord masterActivityRecord,
            TStructure productPropertyRecord, AaProductCatalogRecord productRecord, TStructure record,
            List<TransactionData> transactionData, List<TStructure> transactionRecord) {

        System.out.println("START  ==== ");

        DataAccess da = new DataAccess(this);
        String arrangementId = arrangementContext.getArrangementId().toString();
        String status = arrangementContext.getActivityStatus().toString();
        String functionality = determineFunctionality(status);

        System.out.println("arrangementId: " + arrangementId);
        System.out.println("status: " + status);

        if (status.equalsIgnoreCase("AUTH") || status.equalsIgnoreCase("AUTH-REV")) {
            handleBnplCancel(da, arrangementId, status, functionality);
        }
    }

    // Method to determine functionality based on status
    private String determineFunctionality(String status) {
        if (status.equalsIgnoreCase("AUTH")) {
            return "Loan Success Closed";
        } else if (status.equalsIgnoreCase("AUTH-REV")) {
            return "Loan Success Refunded";
        }
        return "";
    }

    // Method to handle BNPL cancellation
    private void handleBnplCancel(DataAccess da, String arrangementId, String status, String functionality) {
        try {
            // Retrieve BNPL cancel record
            EbBnplCancelAaDetsRecord bnplCancelAaDetsRecord = new EbBnplCancelAaDetsRecord(
                    da.getRecord("EB.BNPL.CANCEL.AA.DETS", arrangementId));

            String downPayment = bnplCancelAaDetsRecord.getDpAmount().getValue();
            String termAmount = bnplCancelAaDetsRecord.getTermAmount().getValue();
            String term = bnplCancelAaDetsRecord.getTerm().getValue();
            String losId = bnplCancelAaDetsRecord.getLosId().getValue();
            String isContractId = bnplCancelAaDetsRecord.getIsContractId().getValue();

            System.out.println("AFTER CANCEL REC");

            HashMap<String, String> map = new HashMap<>();
            map.put("arrangementId", arrangementId);
            map.put("downPayment", downPayment);
            map.put("termAmount", termAmount);
            map.put("term", term);
            map.put("isContractId", isContractId);
            map.put("losId", losId);

            JSONObject jSONMessageData = new JSONObject(map);

            EmkDataToMq mq = new EmkDataToMq();
            mq.sendDatatoMQ(jSONMessageData.toString(), da, "BNPL.POST.MQ.MSG.CONFIG", functionality, this,
                    arrangementId);

            System.out.println("AFTER SEND");

        } catch (Exception e) {
            handleError(da, arrangementId, status, functionality, e);
        }
    }

    // Method to handle errors and retry MQ message send
    private void handleError(DataAccess da, String arrangementId, String status, String functionality, Exception e) {
        try {
            System.out.println("IN CATCH");

            if (status.equalsIgnoreCase("AUTH") || status.equalsIgnoreCase("AUTH-REV")) {
                AaArrangementRecord aaArrRec = new AaArrangementRecord(da.getRecord("AA.ARRANGEMENT", arrangementId));

                // Retrieve alternate ID from arrangement record
                String losId = aaArrRec.getAltIdType(0).getAlternateId().getValue();

                // Prepare data for MQ
                HashMap<String, String> map = new HashMap<>();
                map.put("arrangementId", arrangementId);
                map.put("losId", losId);

                // Create JSON message data
                JSONObject jSONMessageData = new JSONObject(map);

                // Send the message to MQ
                EmkDataToMq mq = new EmkDataToMq();
                mq.sendDatatoMQ(jSONMessageData.toString(), da, "BNPL.POST.MQ.MSG.CONFIG", functionality, this,
                        arrangementId);

                // Log completion
                System.out.println("AFTER SEND");
            }
        } catch (Exception E) {
            System.out.println("CATCH: Unable to send MQ message.");
        }
    }
}
