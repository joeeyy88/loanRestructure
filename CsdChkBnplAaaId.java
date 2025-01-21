package com.temenos.tdf.loanRestructure;

import java.util.List;

import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aaactivityhistory.AaActivityHistoryRecord;
import com.temenos.t24.api.records.aaactivityhistory.ActivityRefClass;
import com.temenos.t24.api.records.aaactivityhistory.EffectiveDateClass;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 *  @author mhismail, anouh
 *
 */
public class CsdChkBnplAaaId extends RecordLifecycle {
    @Override
    public String checkId(String currentRecordId, TransactionContext transactionContext) {
        String altId = "";
        String AaId = "";
        DataAccess da = new DataAccess(this);
        try {
            // Case 1: currentRecordId is an losId
            altId = da.getConcatValues("AA.ARRANGEMENT.ALTERNATE.ID", currentRecordId).toString();

            String trimmedInput = altId.substring(1, altId.length() - 1);
            String[] parts = trimmedInput.split("\\*");
            AaId = parts[1];
            new AaArrangementRecord(da.getRecord("AA.ARRANGEMENT", AaId)); // to check if a valid AaId or not
        } catch (Exception e) {
            try {
                // Split currentRecordId on '-'
                String[] parts = currentRecordId.split("-");

                // Check if split length == 1 and starts with 'AA'
                if (parts.length == 1 && parts[0].startsWith("AA")) {
                    // Case 2: currentRecordId is an AA arrangement ID
                    AaId = parts[0];
                    new AaArrangementRecord(da.getRecord("AA.ARRANGEMENT", AaId)); // to check if a valid AaId or not
                } 
                else if (parts.length > 1) {
                    // Case 3: currentRecordId is an AA Activity ID ex: 'AA132134LLM-AMLADFKQE-13LNL3RN'
                    AaId = parts[0]; // Assuming the first part is always THE AA arrangement ID
                    new AaArrangementRecord(da.getRecord("AA.ARRANGEMENT", AaId)); // to check if a valid AaId or not
                } 
                else {
                    System.out.println("Error1 processing currentRecordId: " + currentRecordId);
                }
            } catch (Exception innerException) {
                System.out.println("Error2 processing currentRecordId: " + currentRecordId);
            }
        }

        AaActivityHistoryRecord acchistRec = new AaActivityHistoryRecord(da.getRecord("AA.ACTIVITY.HISTORY", AaId));
        List<EffectiveDateClass> effMultiVal = acchistRec.getEffectiveDate();
        int effSize = effMultiVal.size();

        for (int i = 0; i < effSize; i++) {
            List<ActivityRefClass> actRef = effMultiVal.get(i).getActivityRef();
            int actRefsize = actRef.size();
            for (int j = 0; j < actRefsize; j++) {

                String refval = actRef.get(j).getActivity().toString();
                if (refval.equals("LENDING-NEW-ARRANGEMENT")) {
                    String actId = actRef.get(j).getActivityRef().toString();
                    currentRecordId = actId;
                    break;
                }

            }

        }

        return currentRecordId;
    }

}
