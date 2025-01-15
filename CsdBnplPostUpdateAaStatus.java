package com.temenos.tdf.loanRestructure;

import java.util.List;
import com.temenos.api.TStructure;
import com.temenos.api.exceptions.T24IOException;
import com.temenos.t24.api.complex.aa.activityhook.ArrangementContext;
import com.temenos.t24.api.complex.aa.activityhook.TransactionData;
import com.temenos.t24.api.hook.arrangement.ActivityLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaprddestermamount.AaPrdDesTermAmountRecord;
import com.temenos.t24.api.records.aaproductcatalog.AaProductCatalogRecord;
import com.temenos.t24.api.records.iscontract.IsContractRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author anouh , mhismail
 *
 */
public class CsdBnplPostUpdateAaStatus extends ActivityLifecycle {
    @Override
    public void postCoreTableUpdate(AaAccountDetailsRecord accountDetailRecord,
            AaArrangementActivityRecord arrangementActivityRecord, ArrangementContext arrangementContext,
            AaArrangementRecord arrangementRecord, AaArrangementActivityRecord masterActivityRecord,
            TStructure productPropertyRecord, AaProductCatalogRecord productRecord, TStructure record,
            List<TransactionData> transactionData, List<TStructure> transactionRecord) {

        DataAccess da = new DataAccess(this);

        AaPrdDesTermAmountRecord termAmountObj = null;

        String arrangementId = "";
        String losId = "";
        String downPayment = "";
        String arrangementStatus = "";
        String termAmount = "";
        String term = "";
        String isContractId = "";

        try {            
            arrangementId = arrangementContext.getArrangementId();
            System.out.println("arrangementId " + arrangementId);
            losId = arrangementRecord.getAltIdType(0).getAlternateId().getValue();
            System.out.println("losId " + losId);
        }
        catch(Exception ex) {
            System.out.println("losId exception");
        }
        
        System.out.println("activity status = " + arrangementContext.getActivityStatus());
        System.out.println("property class = " + arrangementContext.getPropertyClassId().toString());
        
        if (arrangementContext.getCurrentActivity().equals("LENDING-NEW-ARRANGEMENT")) {
            System.out.println("Entered into activity LENDING-NEW-ARRANGEMENT");
            if (arrangementContext.getActivityStatus().equalsIgnoreCase("AUTH")
                    && arrangementContext.getPropertyClassId().toString().equalsIgnoreCase("TERM.AMOUNT")) {
                System.out.println("Enter into case one");
                // first creating the record

                // ------------------------------------------------------------------------------------
                // reading downpayment
                List<String> isContractIdList = da.selectRecords("", "IS.CONTRACT", "",
                        "WITH L.LOS.APPL.ID EQ " + losId);
                System.out.println("isContractIdList" + isContractIdList);
                if (!isContractIdList.isEmpty()) {
                    // first downpayment reading logic
                    System.out.println("first downpayment reading logic");
                    isContractId = isContractIdList.get(0);
                    System.out.println("isContractId " + isContractId);
                    IsContractRecord isContract = new IsContractRecord(da.getRecord("IS.CONTRACT", isContractId));
                    System.out.println("isContract is opened");
                    downPayment = isContract.getTotalDpAmt().getValue();
                    System.out.println("downPayment " + downPayment);
                } else {
                    // second downpayment reading logic
                    List<String> isRecords = null;

                    isRecords = da.getConcatValues("IS.CONTRACT.L.LOS.APPL.ID", losId);
                    String yparts = isRecords.toString();

                    if (isRecords != null && !isRecords.isEmpty()) {
                        String trimmedInput = yparts.substring(1, yparts.length() - 1);
                        String[] parts = trimmedInput.split("\\*");
                        isContractId = parts[1];
                        System.out.println("isContractId " + isContractId);
                    }

                    try {
                        IsContractRecord isconrec = new IsContractRecord(da.getRecord("IS.CONTRACT", isContractId));
                        downPayment = isconrec.getTotalDpAmt().getValue();
                        System.out.println("downPayment " + downPayment);
                    } catch (Exception e) {
                    }
                }
                // -------------------------------------------------------------------------------------------------------------

                arrangementStatus = arrangementRecord.getArrStatus().getValue();
                System.out.println("arrangementStatus " + arrangementStatus);

                termAmountObj = new AaPrdDesTermAmountRecord(record);
                termAmount = "";
                term = "";
                try {
                    termAmount = termAmountObj.getAmount().toString();
                    System.out.println("Amount " + termAmount);
                    term = termAmountObj.getTerm().toString();
                    System.out.println("Term " + term);
                } catch (Exception e) {
                    System.out.println("Exception = " + e);
                }

                try {
                    System.out.println("Start saving try");
                    EbBnplCancelAaDetsRecord bnplCancelAaDetsRecord = new EbBnplCancelAaDetsRecord(this);
                    System.out.println("Opening bnpl record");
                    EbBnplCancelAaDetsTable bnplCancelAaDetsTable = new EbBnplCancelAaDetsTable(this);
                    System.out.println("Opening bnpl table");

                    bnplCancelAaDetsRecord.setArrangementId(arrangementId);
                    bnplCancelAaDetsRecord.setDpAmount(downPayment);
                    bnplCancelAaDetsRecord.setStatus(arrangementStatus);
                    bnplCancelAaDetsRecord.setTermAmount(termAmount);
                    bnplCancelAaDetsRecord.setTerm(term);
                    bnplCancelAaDetsRecord.setLosId(losId);
                    bnplCancelAaDetsRecord.setIsContractId(isContractId);
                    System.out.println("End saving try");
                    bnplCancelAaDetsTable.write(arrangementId, bnplCancelAaDetsRecord);
                    System.out.println("Exit saving try");
                } catch (T24IOException e) {
                    System.out.println("Enter first catch");
                }
            } else if (arrangementContext.getActivityStatus().equalsIgnoreCase("AUTH-REV")) {
                System.out.println("Enter into case two");
                // second canceling the record
                arrangementStatus = "Cancelled";

                try {
                    System.out.println("Start second saving try");
                    EbBnplCancelAaDetsRecord bnplCancelAaDetsRecord = new EbBnplCancelAaDetsRecord(
                            da.getRecord("EB.BNPL.CANCEL.AA.DETS", arrangementId));
                    System.out.println("Opening bnpl record");
                    EbBnplCancelAaDetsTable bnplCancelAaDetsTable = new EbBnplCancelAaDetsTable(this);
                    System.out.println("Opening bnpl table");

                    bnplCancelAaDetsRecord.setStatus(arrangementStatus);
                    System.out.println("End second saving try");
                    bnplCancelAaDetsTable.write(arrangementId, bnplCancelAaDetsRecord);
                    System.out.println("Exit second saving try");
                } catch (T24IOException e) {
                    System.out.println("Enter second catch");
                }
            }
        } else if (arrangementContext.getCurrentActivity().equals("LENDING-CLOSE-ARRANGEMENT")) {
            // third closed the record
            System.out.println("Entered into activity LENDING-NEW-ARRANGEMENT");

            System.out.println("Enter into case three");
            arrangementStatus = "CLOSE";

            try {
                System.out.println("Start third saving try");
                EbBnplCancelAaDetsRecord bnplCancelAaDetsRecord = new EbBnplCancelAaDetsRecord(
                        da.getRecord("EB.BNPL.CANCEL.AA.DETS", arrangementId));
                System.out.println("Opening bnpl record");
                EbBnplCancelAaDetsTable bnplCancelAaDetsTable = new EbBnplCancelAaDetsTable(this);
                System.out.println("Opening bnpl table");

                bnplCancelAaDetsRecord.setStatus(arrangementStatus);
                System.out.println("End third saving try");
                bnplCancelAaDetsTable.write(arrangementId, bnplCancelAaDetsRecord);
                System.out.println("Exit third saving try");
            } catch (T24IOException e) {
                System.out.println("Enter third catch");
            }
        }

    }

}