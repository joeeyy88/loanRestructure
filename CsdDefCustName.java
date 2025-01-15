package com.temenos.tdf.loanRestructure;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.customer.CustomerRecord;

/**
 * TODO: Document me!
 *
 * @author ykumar
 *
 */
public class CsdDefCustName extends RecordLifecycle {

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        // TODO Auto-generated method stub

        CustomerRecord cusRec = new CustomerRecord(currentRecord);
        cusRec.setShortName("C" + currentRecordId, 0);
        cusRec.setName1("C" + currentRecordId, 0);
        currentRecord.set(cusRec.toStructure());

    }

}
