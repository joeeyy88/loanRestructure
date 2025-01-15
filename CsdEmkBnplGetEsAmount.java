package com.temenos.tdf.loanRestructure;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.contractapi.BalanceMovement;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.aabilldetails.AaBillDetailsRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 *
 *
 * @author anouh
 *
 */
public class CsdEmkBnplGetEsAmount extends Enquiry {

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        List<String> outDetails = new ArrayList<String>();
        String contractId = "";
        String callUniqueReference = "";
        if (!filterCriteria.isEmpty()) {
            Iterator<FilterCriteria> var8 = filterCriteria.iterator();

            while (var8.hasNext()) {
                FilterCriteria selectionCrit = var8.next();
                if (selectionCrit.getFieldname().equals("CallUniqueReference")) {
                    callUniqueReference = selectionCrit.getValue();
                }

                if (selectionCrit.getFieldname().equals("contractId")) {
                    contractId = selectionCrit.getValue();
                }
            }
        }

        if (!contractId.isEmpty()) {
            String outPut = "";
            Session session = new Session(this);
            String today = session.getCurrentVariable("!TODAY");
            String expiryDate = "";
            AaBillDetailsRecord settledBillRecord;
            DataAccess dataAccess = new DataAccess(this);
            BigDecimal calculatedAmount = BigDecimal.ZERO;
            BigDecimal uncBal = BigDecimal.ZERO;
            BigDecimal paidAmount = BigDecimal.ZERO;
            BigDecimal settledBillAmount = BigDecimal.ZERO;
            BigDecimal contractTotalAmount = BigDecimal.ZERO;

            Contract contract = new Contract(this);

            try {
                contract.setContractId(contractId);

                List<String> settledBillsIdList = contract.getBillIds(null, null, "INSTALLMENT", null, "SETTLED", null,
                        null, null, null);

                // Start calculating unspecified balance "UNC"
                List<BalanceMovement> uncAccountBal = contract.getContractBalanceMovements("UNCACCOUNT", "VALUE");
                if (!uncAccountBal.isEmpty()) {
                    uncBal = new BigDecimal(uncAccountBal.get(0).getBalance().toString());
                }
                // End calculating unspecified balance "UNC"

                // Start calculating paidAmount
                for (String settledBillId : settledBillsIdList) {
                    settledBillRecord = new AaBillDetailsRecord(dataAccess.getRecord("AA.BILL.DETAILS", settledBillId));
                    settledBillAmount = (new BigDecimal(settledBillRecord.getOrTotalAmount().getValue())).abs();
                    paidAmount = paidAmount.add(settledBillAmount);
                }
                // End calculating paidAmount

                // calculatedAmount = contractTotalAmount - paidAmount - uncBal
                contractTotalAmount = new BigDecimal(contract.getTermAmount().get());
                calculatedAmount = contractTotalAmount.subtract(paidAmount);
                calculatedAmount = calculatedAmount.subtract(uncBal);

                outPut = contractId + "*" + calculatedAmount.toString() + "*" + expiryDate + "*" + today + "*"
                        + callUniqueReference;
                outDetails.add(outPut);
            } catch (Exception ex) {
            }
        }

        return outDetails;
    }
}
