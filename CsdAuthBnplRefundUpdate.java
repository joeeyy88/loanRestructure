package com.temenos.tdf.loanRestructure;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.complex.eb.templatehook.TransactionData;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.iscontract.IsContractRecord;
import com.temenos.t24.api.records.paymentorder.PaymentOrderRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author anouh
 *
 */
public class CsdAuthBnplRefundUpdate extends RecordLifecycle {

    @Override
    public void updateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext,
            List<TransactionData> transactionData, List<TStructure> currentRecords) {
        
        System.out.println("entered update routine");
        String refundFlag = "";
        String losId = "";
        String isContractId = "";
        IsContractRecord isContract = null;
        DataAccess da = new DataAccess(this);
        PaymentOrderRecord po = new PaymentOrderRecord(currentRecord);
        losId = po.getOrderingReference().getValue(); 

        List<String> isContractIdList = da.selectRecords("", "IS.CONTRACT", "", "WITH L.LOS.APPL.ID EQ " + losId);

        System.out.println("isContractIdList" + isContractIdList);

        try {
            if (!isContractIdList.isEmpty()) {
                System.out.println("reading refundFlag logic");
                isContractId = isContractIdList.get(0);
                System.out.println("isContractId " + isContractId);
                isContract = new IsContractRecord(da.getRecord("IS.CONTRACT", isContractId));
                System.out.println("isContract is opened");
                refundFlag = isContract.getLocalRefField("L.REFUND.FLAG").getValue();
                System.out.println("refundFlag = " + refundFlag);
            }

            if (refundFlag.isEmpty() || refundFlag.equalsIgnoreCase("N")) {
                // set refundFlag to "Y"
                isContract = new IsContractRecord(this); // Without this line, we were getting an error from the IS.CONTRACT. Solution is to open an empty contract and just mention "txnData.setTransactionId(isContractId);" and this will update only the field we set 
                
                System.out.println("enter into empty flag");
                isContract.getLocalRefField("L.REFUND.FLAG").setValue("Y");
                System.out.println("read the flag with value3 ===>" + isContract.getLocalRefField("L.REFUND.FLAG").getValue() + "<==");
                
                System.out.println("setted the isContract");

                TransactionData txnData = new TransactionData();
                txnData.setFunction("INPUT");
                System.out.println("a0");
                txnData.setNumberOfAuthoriser("0");
                System.out.println("a1");
                txnData.setVersionId("IS.CONTRACT,BNPL.OFS");
                System.out.println("a2, isContractId = " + isContractId);
                txnData.setTransactionId(isContractId);
                
                transactionData.add(txnData);
                System.out.println("a3");
                currentRecords.add(isContract.toStructure());
                System.out.println("a4");
            }
        } catch (Exception ex) {
            System.out.println("error1 = " + ex);
        }

    }

}
