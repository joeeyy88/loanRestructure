package com.temenos.tdf.loanRestructure;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.paymentorder.PaymentOrderRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.system.Session;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
/**
 * TODO: Document me!
 *
 * @author mmmoussa
 *
 */
public class EmkInpVibanDets extends RecordLifecycle {

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        // TODO Auto-generated method stub
        Session ySession = new Session(this);
        
        PaymentOrderRecord po = new PaymentOrderRecord(currentRecord);
        
        
        DataAccess da = new DataAccess();
        String todayDate = ySession.getCurrentVariable("!TODAY");
        
       String account = po.getCreditAccount().getValue();
       AccountRecord acctRec = new AccountRecord(da.getRecord("ACCOUNT",account)); 
       String aaId = acctRec.getArrangementId().getValue();
       
       String vibanId= po.getLocalRefField("L.VIBAN.ID").getValue();
       String expiryDate= po.getLocalRefField("L.EXPIRY.DATE").getValue();
        EbEmkAaVibanDetsRecord recored=null;
        String vibanFromTable=null;
        recored = new EbEmkAaVibanDetsRecord(da.getRecord("EMK.AA.VIBAN.DETS", aaId));
        vibanFromTable =recored.getLVibanId().getValue();
        
       LocalDate DateFromTable = LocalDate.parse(expiryDate, DateTimeFormatter.BASIC_ISO_DATE);
       LocalDate todayDateSession = LocalDate.parse(todayDate, DateTimeFormatter.BASIC_ISO_DATE);

       
        
       if (!vibanId.equals(vibanFromTable)){
           po.getLocalRefField("L.VIBAN.ID").setError("PO-VIBAN.MISMATCH");
           
       }
        
       if (todayDateSession.isAfter(DateFromTable)){
           po.getLocalRefField("L.EXPIRY.DATE").setError("PO-EXPIRY.DATE");
       }
       
        return po.getValidationResponse();
   
    
    
    }

}

