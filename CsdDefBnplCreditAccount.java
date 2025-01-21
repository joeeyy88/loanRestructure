package com.temenos.tdf.loanRestructure;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.paymentorder.PaymentOrderRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author anouh
 *
 */
public class CsdDefBnplCreditAccount extends RecordLifecycle {

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        DataAccess da = new DataAccess(this);

        String losId = "";
        String aaArrangementId = "";
        String creditAccountNumber = "";

        PaymentOrderRecord pORec = new PaymentOrderRecord(currentRecord);

        losId = pORec.getCreditAccount().getValue(); 

        String companyPlusAaArangementId = da.getConcatValues("AA.ARRANGEMENT.ALTERNATE.ID", losId).toString();

        String[] parts = companyPlusAaArangementId.split("\\*"); 

        if (parts.length > 1) {
            aaArrangementId = parts[1];
            System.out.println("The aaArrangementId = " + aaArrangementId);
        } else {
            System.out.println("The string did not contain '*', or there was no aaArrangementId.");
        }

        try {
            System.out.println("Entered try");

            AaArrangementRecord aaArrRec = new AaArrangementRecord(da.getRecord("AA.ARRANGEMENT", aaArrangementId));

            creditAccountNumber = aaArrRec.getLinkedAppl(0).getLinkedApplId().getValue();

            System.out.println("creditAccountNumber = " + creditAccountNumber);

            pORec.setCreditAccount(creditAccountNumber);

            System.out.println("settled credit account");

            currentRecord.set(pORec.toStructure());

            System.out.println("Exiting try");
        } catch (Exception ex) {
            System.out.println("Entered exception");
        }
        System.out.println("Exiting default");
    }

}
