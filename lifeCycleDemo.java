package com.temenos.tdf.loanRestructure;
import java.math.BigDecimal;
import java.util.List;
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

import com.temenos.t24.api.system.DataAccess;

public class allPayoffBillDetails extends ActivityLifecycle{
    
    
    
    
    
    
    @Override
    public void postCoreTableUpdate(AaAccountDetailsRecord accountDetailRecord,
            AaArrangementActivityRecord arrangementActivityRecord, ArrangementContext arrangementContext,
            AaArrangementRecord arrangementRecord, AaArrangementActivityRecord masterActivityRecord,
            TStructure productPropertyRecord, AaProductCatalogRecord productRecord, TStructure record,
            List<TransactionData> transactionData, List<TStructure> transactionRecord) {
 
        if (arrangementContext.getActivityStatus().equalsIgnoreCase("AUTH")) {
 
            DataAccess da = new DataAccess(this);
 
            Contract contract = new Contract(this);
            String relatedPartyFlag = "";
            String tableAmount = "";
            BigDecimal uncBal = BigDecimal.ZERO;
            BigDecimal amount = BigDecimal.ZERO;
 
            EbEmkRetailRelatedPartyRecord retailRelatedPartyRec;
 
            if (arrangementContext.getCurrentActivity().equals("LENDING-NEW-ARRANGEMENT")) {
 
                AaPrdDesTermAmountRecord termAmtRecord = new AaPrdDesTermAmountRecord(record);
                relatedPartyFlag = termAmtRecord.getLocalRefField("L.RELATED.FLAG").getValue();
                System.out.println("relatedPartyFlag " + relatedPartyFlag);
                if (relatedPartyFlag.equals("Y")) {
                    System.out.println("inside related if ");
 
                    try {
 
                        retailRelatedPartyRec = new EbEmkRetailRelatedPartyRecord(
                                da.getRecord("EB.EMK.RETAIL.RELATED.PARTY", "RETAIL"));
 
                        amount = new BigDecimal(retailRelatedPartyRec.getAmount().getValue());
                    } catch (Exception var1) {
 
                    }
 
                    System.out.println("new arr ");
 
                    try {
 
                        // String term =
                        // termAmtRecord.getAmount().getValue().toString();
                        BigDecimal term = new BigDecimal(termAmtRecord.getAmount().getValue().toString());
                        // BigDecimal term = new
                        // BigDecimal(contract.getTermAmount().toString());
                        System.out.println("term " + term);
 
                        amount = amount.add(term);
                        tableAmount = amount.toString();
 
                    } catch (Exception var1) {
                    }
 
                }
                if (arrangementContext.getCurrentActivity().equals("LENDING-APPLYPAYMENT-PR.REPAYMENT")
                        || arrangementContext.getCurrentActivity().equals("LENDING-SETTLE-PAYOFF")
                        || arrangementContext.getCurrentActivity().equals("LENDING-ISSUE-PAYOFF")) {
 
                    retailRelatedPartyRec = new EbEmkRetailRelatedPartyRecord(
                            da.getRecord("EB.EMK.RETAIL.RELATED.PARTY", "RETAIL"));
 
                    amount = new BigDecimal(retailRelatedPartyRec.getAmount().getValue());
 
                    String arrStatus = arrangementRecord.getArrStatus().getValue();
                    System.out.println("close arr ");
 
                    System.out.println("close arrStatus " + arrStatus);
                    try {
 
                        BigDecimal commitment = new BigDecimal(
                                contract.getOutstandingBalance().getAccountBalance().toString());
                        System.out.println("getOutstandingBalance commitment " + commitment);
 
                        amount = amount.subtract(commitment);
                        tableAmount = amount.toString();
                        System.out.println("getOutstandingBalance tableAmount" + tableAmount);
                    } catch (Exception var2) {
 
                    }
                    try {
                        List<BalanceMovement> curAccountBal = contract.getContractBalanceMovements("CURACCOUNT",
                                "VALUE");
 
                        if (!curAccountBal.isEmpty()) {
 
                            uncBal = new BigDecimal(curAccountBal.get(0).getBalance().toString());
                            System.out.println("curAccountBal " + curAccountBal);
 
                            amount = amount.subtract(uncBal);
                            tableAmount = amount.toString();
                            System.out.println("curAccountBal tableAmount" + tableAmount);
                        }
 
                    } catch (Exception var3) {
 
                    }
                }
                try {
                    EbEmkRetailRelatedPartyRecord retailAdditionalRec = new EbEmkRetailRelatedPartyRecord(this);
                    EbEmkRetailRelatedPartyTable retailAdditionalTable = new EbEmkRetailRelatedPartyTable(this);
 
                    retailAdditionalRec.setAmount(tableAmount);
                    System.out.println("retailAdditionalRec " + retailAdditionalRec);
 
                    // retailAdditionalRec.wait(200+60);
                    // retailAdditionalRec.notifyAll();
                    retailAdditionalTable.write("RETAIL", retailAdditionalRec);
 
                } catch (Exception var2) {
                }
            }
        }
    }
    
    
    
    
    
    
    

}


