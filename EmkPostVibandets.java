package com.temenos.tdf.loanRestructure;


import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.api.exceptions.T24IOException;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.activityhook.ArrangementContext;
import com.temenos.t24.api.complex.aa.activityhook.TransactionData;
import com.temenos.t24.api.hook.arrangement.ActivityLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaprddesaccount.AaPrdDesAccountRecord;
import com.temenos.t24.api.records.aaproductcatalog.AaProductCatalogRecord;

/**
 * TODO: Document me!
 *
 * @author mmmoussa
 *
 */
public class EmkPostVibandets extends  ActivityLifecycle{

    @Override
    public void postCoreTableUpdate(AaAccountDetailsRecord accountDetailRecord,
            AaArrangementActivityRecord arrangementActivityRecord, ArrangementContext arrangementContext,
            AaArrangementRecord arrangementRecord, AaArrangementActivityRecord masterActivityRecord,
            TStructure productPropertyRecord, AaProductCatalogRecord productRecord, TStructure record,
            List<TransactionData> transactionData, List<TStructure> transactionRecord) {
        String  arrangementid = arrangementContext.getArrangementId(); 
//       String VIBAN = arrangementActivityRecord.getLocalRefField("L.VIBAN.ID").getValue();
//       String expiryDate = arrangementActivityRecord.getLocalRefField("L.EXPIRY.DATE").getValue();  
        String VIBAN = "";
        String expiryDate = "";
        Contract contract = new Contract(this);
        contract.setContractId(arrangementid);
        System.out.println("this is Arrangement Id "+contract);
        System.out.println("this is Arrangement Id2 "+arrangementid);
        AaPrdDesAccountRecord accountObj = new AaPrdDesAccountRecord(
                contract.getConditionForProperty("ACCOUNT"));
       
        VIBAN=  accountObj.getLocalRefField("L.VIBAN").getValue();
        System.out.println("this is viban  "+VIBAN);
        expiryDate = accountObj.getLocalRefField("L.EXPIRY.DATE").getValue();
        System.out.println("this is expiry date  "+expiryDate);
       EbEmkAaVibanDetsRecord recored = new EbEmkAaVibanDetsRecord();
       EbEmkAaVibanDetsTable table = new EbEmkAaVibanDetsTable(this); 
       recored.setLExpiryDate(expiryDate);
       recored.setLVibanId(VIBAN);
       try {
        table.write(arrangementid, recored);
    } catch (T24IOException e) {
        // TODO Auto-generated catch block
        // Uncomment and replace with appropriate logger
        // LOGGER.error(e, e);
    }
       
        
        // TODO Auto-generated method stub
    }

}
