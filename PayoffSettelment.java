package Ejada.TDF.Loan.Restructure;

import java.util.List;

import com.temenos.api.LocalRefGroup;
import com.temenos.api.LocalRefList;
import com.temenos.api.TStructure;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.activityhook.ArrangementContext;
import com.temenos.t24.api.complex.aa.activityhook.TransactionData;
import com.temenos.t24.api.hook.arrangement.ActivityLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaprddesaccount.AaPrdDesAccountRecord;
import com.temenos.t24.api.records.aaproductcatalog.AaProductCatalogRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.tables.ebtdfrestructurearrs.*;
/**
 * TODO: Document me!
 *
 * @author c-welsheikh
 *
 */
public class PayoffSettelment extends ActivityLifecycle {

    @Override
    public void postCoreTableUpdate(AaAccountDetailsRecord accountDetailRecord,
            AaArrangementActivityRecord arrangementActivityRecord, ArrangementContext arrangementContext,
            AaArrangementRecord arrangementRecord, AaArrangementActivityRecord masterActivityRecord,
            TStructure productPropertyRecord, AaProductCatalogRecord productRecord, TStructure record,
            List<TransactionData> transactionData, List<TStructure> transactionRecord) {
      
        //---------LOCAL FIELDS---------
        String aaId ="";
        String CIF  ="";
        String newArrId="";   
        String newArr="";
        String OldAaId="";
        int localTableSize=0;
        int innerfieldssize=0;
        
        
        
        if (arrangementContext.getActivityStatus().equalsIgnoreCase("AUTH")) {
            
            System.out.println("Activity status is AUTH. Proceeding with execution.");
            
            DataAccess da = new DataAccess(this);           
            Contract contract = new Contract(this);

            try {
                contract.setContractId(arrangementContext.getArrangementId().toString());
                aaId = arrangementActivityRecord.getArrangement().getValue();
                CIF= arrangementActivityRecord.getCustomer(0).getCustomer().getValue();
                
                System.out.println("Contract ID retrieved: " + aaId);
                System.out.println("customer number retrieved: " + CIF);
           

            
            AaPrdDesAccountRecord aaAccountRecord3= new AaPrdDesAccountRecord(record);
            newArrId=aaAccountRecord3.getLocalRefField("L.NEW.LOAN.ARR").getValue();
            
            //---------Open the Local table to update the flag with YES instead of no------------
            try{
                System.out.println("-----before opening the local table----");
            EbTdfRestructureArrsRecord arrTable = new EbTdfRestructureArrsRecord(da.getRecord("EB.TDF.RESTRUCTURE.ARR", CIF));
            List<NewLnArrClass> newloanArrList = arrTable.getNewLnArr();
            localTableSize = newloanArrList.size();
            System.out.println("local table size " + localTableSize);
            
            
            try{
                System.out.println("-------try of new arr list-------");
          for (int i =0; i < newloanArrList.size(); i++) {
              //-----------------------outer loop--------------------
              System.out.println("inside the for loop, index: " + i);                         
              NewLnArrClass newloanArrCls = newloanArrList.get(i);
              System.out.println("CLASS newLOAN:" + newloanArrCls); 
              newArr = newloanArrCls.getNewLnArr().getValue();
              System.out.println("new arr"+newArr);
                            
              if(newArr.equals(newArrId))
                //-----------------------inner loop--------------------
                  {
                  
                  System.out.println("----inside the if newarr=newArrId------");
                try{ 
                    
                    List<OldLnArrClass> oldloanArrList = arrTable.getNewLnArr(i).getOldLnArr();
                    innerfieldssize = oldloanArrList.size();
                    System.out.println("local table size fields" + innerfieldssize);
                    
                    for (int j =0; j < oldloanArrList.size(); i++) {
                   System.out.println("inside the  inner for loop  , index: " + i);
                  
                  LocalRefGroup LoanArrGrp = aaAccountRecord3.createLocalRefGroup("L.OLD.LOAN.ARR");
                  OldLnArrClass oldloanArrCls = oldloanArrList.get(j);
                  System.out.println("CLASS OLDLOAN:" + oldloanArrCls);

                  OldAaId = oldloanArrCls.getOldLnArr().getValue();
                  if(aaId.equals(OldAaId)){
                      
                      LoanArrGrp.getLocalRefField("").setValue("Y");
                      LocalRefList refList = aaAccountRecord3.getLocalRefGroups("L.OLD.LOAN.ARR");              
                      
                      refList.add(LoanArrGrp);
                      
                  }                
                  
                  }
                  
                    
                }catch (Exception e) {
                    System.out.println(e);
                }                
                  
                  }
          }
    
            }catch (Exception e) {
                System.out.println(e);
            }
                       record.set(aaAccountRecord3.toStructure());

       
            } catch (Exception e) {
                System.out.println(e);
            }
            
            
            }catch (Exception e) {
                System.out.println(e);
            }
       
       
    }
   
    
    }
}
