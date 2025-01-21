package com.temenos.tdf.loanRestructure;

import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.activityhook.ArrangementContext;
import com.temenos.t24.api.complex.aa.activityhook.TransactionData;
import com.temenos.t24.api.hook.arrangement.ActivityLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaarrangementactivity.CustomerClass;
import com.temenos.t24.api.records.aaarrangementactivity.FieldNameClass;
import com.temenos.t24.api.records.aaarrangementactivity.PropertyClass;
import com.temenos.t24.api.records.aaproductcatalog.AaProductCatalogRecord;
import com.temenos.t24.api.system.DataAccess;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO: Document me!
 *
 * @author c-yabdelnaby
 *
 */
public class TriggerRoutine extends ActivityLifecycle {

    public void postCoreTableUpdate(AaAccountDetailsRecord accountDetailRecord,
            AaArrangementActivityRecord arrangementActivityRecord, ArrangementContext arrangementContext,
            AaArrangementRecord arrangementRecord, 
            AaArrangementActivityRecord masterActivityRecord, 
            TStructure productPropertyRecord, AaProductCatalogRecord productRecord,
            TStructure record, List<TransactionData> transactionData, List<TStructure> transactionRecord) {
       
        
        if (arrangementContext.getActivityStatus().equalsIgnoreCase("AUTH")) {
            DataAccess dataAccess = new DataAccess(this);
            System.out.println("--Executed--");
            String aact =      arrangementContext.getArrangementActivityId().toString();
            System.out.println("Acct Id:" + aact);
            AaArrangementActivityRecord arrx = new AaArrangementActivityRecord(dataAccess.getRecord("AA.ARRANGEMENT.ACTIVITY", aact));
            
            
            System.out.println("Acct Record: " + arrx);
     String flag =  arrx.getLocalRefField("L.RESTRUCT.FLAG").getValue().toString();
     System.out.println("Flag Status: " + flag);
       if(flag.equalsIgnoreCase("YES") )
       {
        
        
        try {
//            Contract contract = new Contract(this);
//            contract.setContractId(arrangementContext.getArrangementId().toString());
            String aaidx = arrangementContext.getArrangementId().toString();
            System.out.println("Contract Id: " + aaidx);

            // Create Transaction Data
            TransactionData trx = new TransactionData();
            trx.setVersionId("AA.ARRANGEMENT.ACTIVITY,");
            trx.setSourceId("BULK.OFS");
            trx.setFunction("INPUT");
            trx.setNumberOfAuthoriser("0");
            transactionData.add(trx);

            // Create Arrangement Activity Record
            AaArrangementActivityRecord arr = new AaArrangementActivityRecord(this);
            
            arr.setArrangement((CharSequence)aaidx);
            arr.setActivity("LENDING-ISSUE-PAYOFF-RESTRUCTURE");

            // Add Properties if Required
//            PropertyClass propertyClass = new PropertyClass();
//            propertyClass.setProperty("RESTRUCTURE");
//            arr.addProperty(propertyClass);

            // Add to Transaction Records
            transactionRecord.add(arr.toStructure());
            System.out.println("Done ");
        } catch (Exception e) {
            System.out.println("Error in TriggerRoutine: " + e.getMessage());
        }
       }
    }
}
}













