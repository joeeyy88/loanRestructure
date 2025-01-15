package com.temenos.tdf.loanRestructure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.iscontract.IsContractRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author azidan, mhismail, anouh
 *
 */
public class CsdEmkBnplOrderPost extends RecordLifecycle {

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<com.temenos.t24.api.complex.eb.servicehook.TransactionData> transactionData,
            List<TStructure> currentRecords, TransactionContext transactionContext) {

        System.out.println("Hi,i'm excuted");
        DataAccess dataAccess = new DataAccess(this);

        String losId = null;
        String orderId = null;
        String totalBillAmount = null;
        BigDecimal integerTBA = null;
        String equipmentID = null;
        String merchantId = null;
        String iban = null;
        String refundableCommissionRate = null;
        BigDecimal integerRCR = null;
        String nonRefundableCommissionRate = null;
        BigDecimal integerNRCR = null;
        String refundableCommissionAmount = null;
        String nonRefundableCommissionAmount = null;
        String rma = null;
        Long integerRma = null;
        String today = null;
        String newDateString = null;
        String commissionRate = null;
        String fixedFee = null;
        String merchantCode = null;
        String orderTableId = null;
        BigDecimal vatRate = null;
        BigDecimal totalFee = null;
        BigDecimal vatAmount = null;
        String totalDeductionAmount = null;
        String arabicMerchantName = null;
        String merchantName = null;
        EbEmkanMerchantRecord merchantRecord = null;
        EbEmkanOrderTable orderTable = null;
        EbEmkanOrderRecord newOrder = null;

        try {
            IsContractRecord isContract = new IsContractRecord(currentRecord);
            losId = isContract.getLocalRefField("L.LOS.APPL.ID").getValue();
            orderId = isContract.getLocalRefField("L.ORDER.ID").getValue();
            totalBillAmount = isContract.getTotPurchasePrice().getValue();
            integerTBA = new BigDecimal(totalBillAmount);
            today = isContract.getValueDate().getValue();
            equipmentID = isContract.getCommodity(0).getAssetRef(0).getAssetRef().getValue();
            System.out.println("equipmentID" + equipmentID);
        } catch (Exception e) {
        }

        try {
            IsEquipmentRecord equipmentRecord = new IsEquipmentRecord(
                    dataAccess.getRecord("IS.EQUIPMENT", equipmentID));
            merchantId = equipmentRecord.getDealerName().getValue();
            System.out.println("merchantId" + merchantId);
        } catch (Exception e) {
        }

        try {
            merchantRecord = new EbEmkanMerchantRecord(dataAccess.getRecord("EB.EMKAN.MERCHANT", merchantId));
            iban = merchantRecord.getIban().getValue();
            refundableCommissionRate = merchantRecord.getRefundCommissionRate().getValue();
            integerRCR = new BigDecimal(refundableCommissionRate).divide(new BigDecimal("100"));
            nonRefundableCommissionRate = merchantRecord.getNonRefundCommissionRate().getValue();
            integerNRCR = new BigDecimal(nonRefundableCommissionRate).divide(new BigDecimal("100"));
            refundableCommissionAmount = integerTBA.multiply(integerRCR).toString();
            nonRefundableCommissionAmount = integerTBA.multiply(integerNRCR).toString();
            rma = merchantRecord.getRma().getValue();
            integerRma = Long.parseLong(rma);
            System.out.println("integerRma" + integerRma);
        } catch (Exception e) {
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            System.out.println("today" + today);
            LocalDate date = LocalDate.parse(today, formatter);
            System.out.println("integerRma" + integerRma);
            LocalDate newDate = date.plusDays(integerRma);
            System.out.println("newDate" + newDate);
            newDateString = newDate.format(formatter);
            System.out.println("newDateString" + newDateString);
        } catch (Exception e) {
        }

        try {
            commissionRate = merchantRecord.getCommissionRate().getValue();
            fixedFee = merchantRecord.getFixedFee().getValue();
            merchantCode = merchantRecord.getMerchantCode().getValue();
            orderTableId = newDateString + "-" + losId;
            vatRate = new BigDecimal(merchantRecord.getVatRate().getValue());
            totalFee = ((new BigDecimal(commissionRate).divide(new BigDecimal("100"))).multiply(integerTBA))
                    .add(new BigDecimal(fixedFee)); // Calculate total fee
            vatAmount = vatRate.divide(new BigDecimal("100")).multiply(totalFee);
            totalDeductionAmount = totalFee.add(vatAmount).toString();
            try {
                String arName1 = "";
                try {
                    arName1 = merchantRecord.getMerchantArName1().getValue();
                } catch (Exception e) {
                }

                String arName2 = "";
                try {
                    arName2 = merchantRecord.getMerchantArName2().getValue();
                } catch (Exception e) {
                }

                String arName3 = "";
                try {
                    arName3 = merchantRecord.getMerchantArName3().getValue();
                } catch (Exception e) {
                }

                arabicMerchantName = arName1 + arName2 + arName3;
            } catch (Exception e) {
            }
            try {
                String enName1 = "";
                try {
                    enName1 = merchantRecord.getMerchantEnName1().getValue();
                } catch (Exception e) {
                }

                String enName2 = "";
                try {
                    enName2 = merchantRecord.getMerchantEnName2().getValue();
                } catch (Exception e) {
                }
                merchantName = enName1 + enName2;
            } catch (Exception e) {
            }
        } catch (Exception e) {
        }

        try {
            orderTable = new EbEmkanOrderTable(this);
            newOrder = new EbEmkanOrderRecord(this);
            newOrder.setCommissionRate(commissionRate);
            newOrder.setFixedFee(fixedFee);
            newOrder.setIban(iban);
            newOrder.setLosId(losId);
            newOrder.setMerchantCode(merchantCode);
            newOrder.setMerchantId(merchantId);
            newOrder.setMerchantName(merchantName);
            newOrder.setMerchantNameAr(arabicMerchantName);
            newOrder.setNonRefundCommissionAmount(nonRefundableCommissionAmount);
            newOrder.setRefundCommissionAmount(refundableCommissionAmount);
            newOrder.setOrderId(orderId);
            newOrder.setNonRefundCommissionRate(nonRefundableCommissionRate);
            newOrder.setRefundCommissionRate(refundableCommissionRate);
            newOrder.setRma(rma);
            newOrder.setStatus("ACTIVE");
            newOrder.setTotalBillAmount(totalBillAmount);
            newOrder.setTotalDeductionAmount(totalDeductionAmount);
            newOrder.setVatRate(vatRate.toString());
            newOrder.setTotalFeeAmount(totalFee.toString());
            System.out.println("newOrder" + newOrder);

            orderTable.write(orderTableId, newOrder);
        } catch (Exception e) {
        }
    }
}
