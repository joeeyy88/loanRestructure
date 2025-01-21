package Ejada.TDF.Loan.Restructure;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.temenos.api.LocalRefClass;
import com.temenos.api.LocalRefGroup;
import com.temenos.api.LocalRefList;
import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.activityhook.ActivityContext;
import com.temenos.t24.api.complex.aa.activityhook.ArrangementContext;
import com.temenos.t24.api.complex.aa.activityhook.FieldPair;
import com.temenos.t24.api.complex.aa.activityhook.Property;
import com.temenos.t24.api.hook.arrangement.ActivityLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaarraccount.AaArrAccountRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaprddesaccount.AaPrdDesAccountRecord;
import com.temenos.t24.api.records.aaproductcatalog.AaProductCatalogRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.tables.ebtdfpayoffbills.*;

/**
 * TODO: Document me!
 *
 * @author c-welsheikh
 *
 */
public class PayoffBillRetrieval extends ActivityLifecycle {


    @Override
    public void defaultFieldValues(AaAccountDetailsRecord accountDetailRecord,
            AaArrangementActivityRecord arrangementActivityRecord, ArrangementContext arrangementContext,
            AaArrangementRecord arrangementRecord, AaArrangementActivityRecord masterActivityRecord,
            TStructure productPropertyRecord, AaProductCatalogRecord productRecord, TStructure record) {
        //Local fields:
        String CIF ="";
        String CurrentAaId="";
        String OldAaId ="";
        BigDecimal AccountAMT=BigDecimal.ZERO;
        BigDecimal ProfitAMT =BigDecimal.ZERO;
        BigDecimal PenaltyAMT=BigDecimal.ZERO;
        BigDecimal sumBal=BigDecimal.ZERO;
        BigDecimal sumTot=BigDecimal.ZERO;
        BigDecimal accTot=BigDecimal.ZERO;
        BigDecimal prfTot=BigDecimal.ZERO;
        int localTableSize;
        String arrStatus;
    
 try{ 
     
     System.out.println("========start Routine 1:=========");
        Contract contract = new Contract(this);
        DataAccess da = new DataAccess(this);        
        String property = arrangementContext.getPropertyId();
        System.out.println("prop "+property);
        contract.setContractId(arrangementContext.getArrangementId());
        CurrentAaId=contract.getContractId().toString();
        System.out.println("CURRENT AA ID:"+CurrentAaId);
        CIF=masterActivityRecord.getCustomer(0).getCustomer().getValue();
        System.out.println("CIF:"+CIF);
   
        if (property.equals("ACCOUNT")) {
           AaPrdDesAccountRecord aaAccountRecord3= new AaPrdDesAccountRecord(record);
           
           
      //-----------Get data from the local table-----------     
           EbTdfPayoffBillsRecord payoffbillsRec = new EbTdfPayoffBillsRecord(da.getRecord("EB.TDF.PAYOFF.BILLS", CIF));
           List<LOldLoanArrClass> oldloanArrList = payoffbillsRec.getLOldLoanArr();
           localTableSize = oldloanArrList.size();
           System.out.println("local table size " + localTableSize);
     try{
         for (int i =0; i < oldloanArrList.size(); i++) {
             System.out.println("inside the for loop, index: " + i);
             
             LocalRefGroup LoanArrGrp = aaAccountRecord3.createLocalRefGroup("L.OLD.LOAN.ARR");
             LocalRefClass localRefCl= new LocalRefClass();            
             LOldLoanArrClass oldloanArrCls = oldloanArrList.get(i);
             System.out.println("CLASS OLDLOAN:" + oldloanArrCls);

             OldAaId = oldloanArrCls.getLOldLoanArr().getValue();
             AaArrangementRecord aaRec =new AaArrangementRecord(da.getRecord("AA.ARRANGEMENT", OldAaId));
             
             //------CHECKING THAT THE AA ARRANGEMENT IS NOT CLOSED -----------
             arrStatus=aaRec.getArrStatus().getValue();            
             
             if(!arrStatus.equals("CLOSE")){
                 
                 LoanArrGrp.getLocalRefField("L.OLD.LOAN.ARR").setValue(OldAaId);
                 System.out.println("AFTER OLD LOAN");

                 AccountAMT = new BigDecimal(oldloanArrCls.getLAccountAmt().getValue());
                 System.out.println("ACC" + AccountAMT);
                 LoanArrGrp.getLocalRefField("L.ACCOUNT.AMT").setValue(AccountAMT.toString());

                 ProfitAMT = new BigDecimal(oldloanArrCls.getLProfitAmt().getValue());
                 System.out.println("PRF" + ProfitAMT);
                 LoanArrGrp.getLocalRefField("L.PROFIT.AMT").setValue(ProfitAMT.toString());
                 

                 PenaltyAMT = new BigDecimal(oldloanArrCls.getLPenaltyAmt().getValue());
                 System.out.println("PEN" + PenaltyAMT);
                 LoanArrGrp.getLocalRefField("L.PENALTY.AMT").setValue(PenaltyAMT.toString());
                 
                 sumBal=AccountAMT.add(ProfitAMT).add(PenaltyAMT);
                 System.out.println("sum bal " + sumBal);
                 LoanArrGrp.getLocalRefField("L.SUM.BAL").setValue(sumBal.toString());
                            
                 System.out.println("-----after line---");
                 LocalRefList refList = aaAccountRecord3.getLocalRefGroups("L.OLD.LOAN.ARR");              
                              
                 refList.add(LoanArrGrp);
                 
             
                 System.out.println("-----BEF TOTAL SUM---");
                 accTot=accTot.add(AccountAMT);
                 System.out.println("ACC " + accTot);
                 prfTot=prfTot.add(ProfitAMT);
                 System.out.println("PROFIT" + prfTot);
                 sumTot=accTot.add(prfTot);
                 System.out.println("SUM TOTAL " + sumTot);
                 aaAccountRecord3.getLocalRefField("L.SUM.TOT.BAL").setValue(sumTot.toString());
                 System.out.println("----------END SUM---------");
             }
             
             
             
                       
         }   
         
        
     }catch(Exception e){
         
         System.out.println(e);
     }       
      
                        record.set(aaAccountRecord3.toStructure());
     
        } 
        
 }catch(Exception e){
     
     System.out.println(e);
 }   
         
        }


    }
    
    

