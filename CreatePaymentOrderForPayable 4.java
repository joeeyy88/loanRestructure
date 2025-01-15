
package com.temenos.training;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.SynchronousTransactionData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionControl;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.currency.CurrencyMarketClass;
import com.temenos.t24.api.records.currency.CurrencyRecord;
import com.temenos.t24.api.records.ebaiopayablefile.EbAioPayableFileRecord;
import com.temenos.t24.api.records.ebaiopayablefile.IdClass;
import com.temenos.t24.api.records.paymentorder.PaymentOrderRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author sabuhalawa
 *
 */
public class CreatePaymentOrderForPayable extends ServiceLifecycle {
    
    public static String FEE_PL = "52064";
    public static String CASHBACK_PL = "52065";
    public static String POOL_ACCOUNT = "145000001"; //ACCOUNT TO DEBIT
    public static String VAT_ACCOUNT = "SAR172110001";
    public static String TRANSFER_PRODUCT = "ACTOACPAY";
    public static String FEES_PRODUCT = "ACFXPAY";
    public static String VAT_PRODUCT = "ACVATPAY";
    public static String ON_US_ACCOUNT = "SAR145040001";
    public static String ATM_POS_LOCAL_ACCT = "SAR145050001";
    public static String ATM_POS_INT_ACCT = "USD145080001";
    public static String SAR_MEMO_ACCT = "SAR145060001";
    public static String FRGN_MEMO_ACCT = "145060001";
    public static String ORDERING_CUSTOMER = "999999999";
   
    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
 //       LocalDate today = LocalDate.now();
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
 //       String formattedDate = today.format(formatter);

        List<String> returnIds = new ArrayList<String>();

        DataAccess data = new DataAccess(this);

        String condition = " WITH FILE.TYPE EQ 'PAYABLE' AND IS.PROCESSED.FLAG NE 'DONE'";

        List<String> recIds = data.selectRecords("", "EB.AIO.PAYABLE.FILE", "", condition);
        EbAioPayableFileRecord record = new EbAioPayableFileRecord(data.getRecord("EB.AIO.PAYABLE.FILE", recIds.get(0)));
        int size = record.getId().size();

        for (int i = 0; i < size; i++) {
            String rec = recIds.get(0) + "#" + i;
            returnIds.add(rec);
        }

        return returnIds;

    }

    @Override
    public void updateRecord(String id, ServiceData serviceData, String controlItem,
            TransactionControl transactionControl, List<SynchronousTransactionData> transactionData,
            List<TStructure> records) {
        
        String[] split = id.split("#", 2);
        String recordId = split[0];
        int index = Integer.parseInt(split[1]);

        DataAccess data = new DataAccess(this);

        EbAioPayableFileRecord record = new EbAioPayableFileRecord(data.getRecord("EB.AIO.PAYABLE.FILE", recordId));

        IdClass currentGroup = record.getId(index);
        appendLog(String.valueOf(index));
        
        try{
            //First, we check if transaction is reverse or normal
            boolean isReverse = false;
            try{
                String reverseFlag = currentGroup.getIsReverseFlag().getValue();
                isReverse = isTransactionReverse(reverseFlag);
            }
            catch(Exception e){
                appendLog(e.toString());
            }
            
            String transactionType = currentGroup.getTxnType().getValue();
            String destinationCurrency = currentGroup.getDestinationCcy().getValue();
            String txnCurrency = currentGroup.getTxnCcy().getValue();
            String terminalId = currentGroup.getTerminalId().getValue();
            String groupReference = currentGroup.getGroupReference().getValue();
           
           //To check if transaction is fees, vat or normal transaction 
            if (index % 3 == 0) 
            {   
                switch(transactionType){
                   case "POSONUS":
                   case "ATMONUS":
                   {    if(isReverse){
                            if(!destinationCurrency.equals("SAR")){
                                String foreignAmount = currentGroup.getTxnAmount().getValue();
                                String rate = currentGroup.getRate(0).getValue();
                                
                                String localAmount = calculateAmount(foreignAmount, rate);
                                records.add(firstReverseForeignTransaction(localAmount, ON_US_ACCOUNT, terminalId, groupReference, transactionType));
                                transactionData.add(buildPaymentTransactionData());
                                
                                records.add(secondReverseForeignTransaction(destinationCurrency, rate, foreignAmount, terminalId, groupReference, transactionType));
                                transactionData.add(buildPaymentTransactionData());
                            }
                            else{
                                String txnAmount = currentGroup.getTxnAmount().getValue();
                                records.add(directReverseTransaction(txnAmount, ON_US_ACCOUNT, terminalId, groupReference, transactionType));
                                transactionData.add(buildPaymentTransactionData());
                            }
                        }
                         else{
                             if(!txnCurrency.equals("SAR")){
                                 String orginalAmount = currentGroup.getTxnAmount().getValue();
                                 String sarAmount = currentGroup.getAmount(0).getValue();
                                 String rate = currentGroup.getRate(0).getValue();
                                 
                                 records.add(firstTxnForeign(txnCurrency, orginalAmount, terminalId, groupReference, rate, transactionType));
                                 transactionData.add(buildPaymentTransactionData());
                                 
                                 records.add(secondTxnForeign(ON_US_ACCOUNT, sarAmount, terminalId, groupReference, transactionType));
                                 transactionData.add(buildPaymentTransactionData());
                             }
                             else{
                                 String txnAmount = currentGroup.getAmount(0).getValue();
                                 records.add(directTransaction(txnAmount, ON_US_ACCOUNT, terminalId, groupReference, transactionType));
                                 transactionData.add(buildPaymentTransactionData());
                             }
                         }   
                       break;
                   }
                   case "LATMPOS":
                   {
                       if(isReverse){
                           String txnAmount = currentGroup.getTxnAmount().getValue();
                           records.add(directReverseTransaction(txnAmount, ATM_POS_LOCAL_ACCT, terminalId, groupReference, transactionType));
                           transactionData.add(buildPaymentTransactionData());
                           
                           }
                        else{
                           String txnAmount = currentGroup.getAmount(0).getValue();
                           records.add(directTransaction(txnAmount, ATM_POS_LOCAL_ACCT, terminalId, groupReference, transactionType));
                           transactionData.add(buildPaymentTransactionData());
                           }
                       break;
                   }
                   case "IATMPOS":
                   {
                       if(isReverse){
                           if(!(destinationCurrency.equals("USD") && txnCurrency.equals("USD"))){
                               String foreignAmount = currentGroup.getTxnAmount().getValue();
                               String toUSDRate = currentGroup.getRate(0).getValue();
                               String toSARRate = currentGroup.getRate(1).getValue();
                               
                               Double inverseRate = 1 / Double.valueOf(toUSDRate);
                               String sarAmount = calculateAmount(foreignAmount, toSARRate);
                               String usdAmount = calculateAmount(sarAmount, String.valueOf(inverseRate));
                                
                               
                               records.add(firstInternationalReverseTxn(usdAmount, toUSDRate, terminalId, groupReference, transactionType));
                               transactionData.add(buildPaymentTransactionData());
                               
                               records.add(secondInternationalReverseTxn(destinationCurrency, foreignAmount, toSARRate, terminalId, groupReference, transactionType));
                               transactionData.add(buildPaymentTransactionData());
                           }
                           else{
                               String usdAmount = currentGroup.getTxnAmount().getValue();
                               records.add(directInternationalReversal(usdAmount, terminalId, groupReference, transactionType));
                               transactionData.add(buildPaymentTransactionData());
                           }
                       }
                        else{
                            if(!(destinationCurrency.equals("USD") && txnCurrency.equals("USD"))){
                                String foreignAmount = currentGroup.getTxnAmount().getValue();
                                String toSARRate = currentGroup.getRate(0).getValue();
                                String toUSDRate = currentGroup.getRate(1).getValue();

                                Double inverseRate = 1 / Double.valueOf(toUSDRate);
                                String sarAmount = calculateAmount(foreignAmount, toSARRate);
                                String usdAmount = calculateAmount(sarAmount, String.valueOf(inverseRate));
                                
                                records.add(firstInternationalTransaction(foreignAmount, toSARRate, txnCurrency, terminalId, groupReference, transactionType));
                                transactionData.add(buildPaymentTransactionData());
                                
                                records.add(secondInternationalTransaction(usdAmount, toUSDRate, terminalId, groupReference, transactionType));
                                transactionData.add(buildPaymentTransactionData());
                            }
                            else{
                                String usdAmount = currentGroup.getTxnAmount().getValue();
                                records.add(directInternationalTxn(usdAmount, terminalId, groupReference, transactionType));
                                transactionData.add(buildPaymentTransactionData());
                            }
                        }
                       break;
                   }
                   case "CASHBACK":
                   {
                       String txnAmount = currentGroup.getAmount(0).getValue();
                       if(isReverse){
                           records.add(reverseCashbackTransaction(txnAmount, terminalId, groupReference, transactionType));
                           transactionData.add(buildPaymentTransactionData());
                       }
                       else{
                           records.add(cashbackTransaction(txnAmount, terminalId, groupReference, transactionType));
                           transactionData.add(buildPaymentTransactionData());
                       }
                       break;
                   }
                }
            }
            
            
            if (index % 3 == 1) 
            {   
                String feesAmountPosted = currentGroup.getFeeAmountPosted().getValue();
                
                if(isReverse){
                    if(!destinationCurrency.equals("SAR")){
                       
                            String rate = currentGroup.getRate(0).getValue();
                            String sarAmount = calculateAmount(feesAmountPosted, rate);
                            
                            records.add(firstFeesReverseTxn(sarAmount, terminalId, groupReference, transactionType));
                            transactionData.add(buildPaymentTransactionData());
                            
                            records.add(secondFeesReverseTxn(destinationCurrency, feesAmountPosted, rate, terminalId, groupReference, transactionType));
                            transactionData.add(buildPaymentTransactionData());
                        
                    }
                    else{
                        records.add(directFeesReverseTxn(feesAmountPosted, terminalId, groupReference, transactionType));
                        transactionData.add(buildPaymentTransactionData());
                    }
                }
                else{
                    if(!txnCurrency.equals("SAR")){
                        
                            String rate = currentGroup.getRate(0).getValue();
                            String sarAmount = calculateAmount(feesAmountPosted, rate);;
                            
                            records.add(firstForeignFeesTxn(txnCurrency, feesAmountPosted, rate, terminalId, groupReference, transactionType));
                            transactionData.add(buildPaymentTransactionData());
                            
                            records.add(secondForeginFeesTxn(sarAmount, terminalId, groupReference, transactionType));
                            transactionData.add(buildPaymentTransactionData());
                        
                    }
                    else{
                        records.add(directFeesTxn(feesAmountPosted, terminalId, groupReference, transactionType));
                        transactionData.add(buildPaymentTransactionData());
                    }
                }
            }
            if (index % 3 == 2) 
            {
                String vatAmountPosted = currentGroup.getVatAmountPosted().getValue();
                
                if(isReverse){
                    if(!destinationCurrency.equals("SAR")){
                      
                            String rate = currentGroup.getRate(0).getValue();
                            String sarAmount = calculateAmount(vatAmountPosted, rate);
                            
                            records.add(firstVatReverseTxn(sarAmount, terminalId, groupReference, transactionType));
                            transactionData.add(buildPaymentTransactionData());
                            
                            records.add(secondVatReverseTxn(destinationCurrency, vatAmountPosted, terminalId, rate, groupReference, transactionType));
                            transactionData.add(buildPaymentTransactionData());
                    }
                    else{
                       records.add(directVatReverseTxn(vatAmountPosted, terminalId, groupReference, transactionType));
                       transactionData.add(buildPaymentTransactionData());
                    }
                }
                else{
                    if(!txnCurrency.equals("SAR")){
                            String rate = currentGroup.getRate(0).getValue();
                            String sarAmount = calculateAmount(vatAmountPosted, rate);
                            
                            records.add(firstForeignVatTxn(txnCurrency, vatAmountPosted, rate, terminalId, groupReference, transactionType));
                            transactionData.add(buildPaymentTransactionData());
                            
                            records.add(secondForeginVatTxn(sarAmount, terminalId, groupReference, transactionType));
                            transactionData.add(buildPaymentTransactionData());
                       
                    }
                    else{
                        records.add(directVatTxn(vatAmountPosted, terminalId, groupReference, transactionType));
                        transactionData.add(buildPaymentTransactionData());
                    }
                }
            }
          currentGroup.setIsProcessedFlag("DONE");
          record.setId(currentGroup, index);
          records.add(record.toStructure());
          transactionData.add(buildPayableTransactionData(recordId));
        }
        catch(Exception e){
            appendLog(e.toString());
        }
        
        

    }
    
    public SynchronousTransactionData buildPayableTransactionData(String recordId){
        SynchronousTransactionData transactionData = new SynchronousTransactionData();
        transactionData.setFunction("INPUT");
        transactionData.setNumberOfAuthoriser("0");
        transactionData.setSourceId("BULK.OFS");
        transactionData.setVersionId("EB.AIO.PAYABLE.FILE,POST.PAYABLE");
        transactionData.setTransactionId(recordId);
        
        return transactionData;
    }
    
    public SynchronousTransactionData buildPaymentTransactionData(){
        SynchronousTransactionData paymentTransactionData = new SynchronousTransactionData();
        
        paymentTransactionData.setFunction("INPUT");
        paymentTransactionData.setSourceId("PAYABLE.OFS");
        paymentTransactionData.setVersionId("PAYMENT.ORDER,CRO.PAYABLE.FILE");
        paymentTransactionData.setNumberOfAuthoriser("0");
        
        return paymentTransactionData;
        
    }
    
    public boolean isTransactionReverse(String txnFlag){
           
        if(txnFlag.equals("REV")){
                return true;
            }
        else
            return false;
       
    }
    
    public TStructure firstTxnForeign(String transactionCurrency, String originalAmount, String terminalId, 
            String groupReference, String rate, String transactionType){
        PaymentOrderRecord firstTransaction = new PaymentOrderRecord(this);
        
        firstTransaction.setPaymentOrderProduct(TRANSFER_PRODUCT);
        firstTransaction.setOrderingCustomer(ORDERING_CUSTOMER);
        
        firstTransaction.setPaymentAmount(originalAmount);
        firstTransaction.setDebitAccount(transactionCurrency + POOL_ACCOUNT); 
        firstTransaction.setCreditAccount(transactionCurrency + FRGN_MEMO_ACCT);
        firstTransaction.setPaymentCurrency(transactionCurrency);
        
        firstTransaction.addNarrative(transactionType);
        firstTransaction.getLocalRefField("L.TERMINAL.ID").setValue(terminalId);
        firstTransaction.getLocalRefField("L.REF.GROUP").setValue(groupReference);
        
        return firstTransaction.toStructure();
        
    }
    
    public TStructure secondTxnForeign(String creditAccount, String sarAmount, 
            String terminalId, String groupReference, String transactionType)
    {  
        PaymentOrderRecord secondTransaction = new PaymentOrderRecord(this);
        secondTransaction.setOrderingCustomer(ORDERING_CUSTOMER);
        secondTransaction.setPaymentOrderProduct(TRANSFER_PRODUCT);
        
        secondTransaction.setDebitAccount(SAR_MEMO_ACCT);
        secondTransaction.setCreditAccount(creditAccount);
        
        secondTransaction.setPaymentCurrency("SAR");
        secondTransaction.setPaymentAmount(sarAmount);
        secondTransaction.addNarrative(transactionType);
        secondTransaction.getLocalRefField("L.TERMINAL.ID").setValue(terminalId);
        secondTransaction.getLocalRefField("L.REF.GROUP").setValue(groupReference);
    
        return secondTransaction.toStructure();
      
    }
    
    public TStructure directTransaction(String txnAmount, String creditAccount, String terminalId, 
            String groupReference, String transactionType)
    {
        
        PaymentOrderRecord directTransaction = new PaymentOrderRecord(this);
        
        directTransaction.setPaymentOrderProduct(TRANSFER_PRODUCT);
        directTransaction.setOrderingCustomer(ORDERING_CUSTOMER);
        
        directTransaction.setDebitAccount("SAR" + POOL_ACCOUNT);
        directTransaction.setCreditAccount(creditAccount);
        
        directTransaction.setPaymentCurrency("SAR");
        directTransaction.setPaymentAmount(txnAmount);
        directTransaction.addNarrative(transactionType);
        directTransaction.getLocalRefField("L.TERMINAL.ID").setValue(terminalId);
        directTransaction.getLocalRefField("L.REF.GROUP").setValue(groupReference);
        
        return directTransaction.toStructure();
    
        
    }
    
    public TStructure cashbackTransaction(String txnAmount, String terminalId, String groupReference, String transactionType){
        
        PaymentOrderRecord cashback = new PaymentOrderRecord(this);
        cashback.setPaymentOrderProduct(TRANSFER_PRODUCT);
        cashback.setOrderingCustomer(ORDERING_CUSTOMER);
       
        cashback.setDebitPl(CASHBACK_PL);
        cashback.setCreditAccount("SAR" + POOL_ACCOUNT);
        cashback.setDebitCcy("SAR");
        cashback.setPaymentCurrency("SAR");
        
        cashback.setPaymentAmount(txnAmount);
        cashback.addNarrative(transactionType);
        cashback.getLocalRefField("L.TERMINAL.ID").setValue(terminalId);
        cashback.getLocalRefField("L.REF.GROUP").setValue(groupReference);
        
        return cashback.toStructure();
       
        
    }
    
    public TStructure reverseCashbackTransaction(String txnAmount, String terminalId, String groupReference, String transactionType)
    {
        PaymentOrderRecord cashbackReverse = new PaymentOrderRecord(this);
        cashbackReverse.setPaymentOrderProduct(TRANSFER_PRODUCT);
        cashbackReverse.setOrderingCustomer(ORDERING_CUSTOMER);
       
        cashbackReverse.setCreditPl(CASHBACK_PL);
        cashbackReverse.setDebitAccount("SAR" + POOL_ACCOUNT);
        cashbackReverse.setCreditCurrency("SAR");
        cashbackReverse.setPaymentCurrency("SAR");
        
        cashbackReverse.setPaymentAmount(txnAmount);
        cashbackReverse.addNarrative(transactionType);
        cashbackReverse.getLocalRefField("L.TERMINAL.ID").setValue(terminalId);
        cashbackReverse.getLocalRefField("L.REF.GROUP").setValue(groupReference);
        
        return cashbackReverse.toStructure();
    }
    
    public String calculateAmount(String fromAmount, String rate){
        
        Float exchange = Float.valueOf(fromAmount) * Float.valueOf(rate);
        String toAmount = String.format("%.2f", exchange);
        return toAmount;
        
    }
    
    public TStructure firstReverseForeignTransaction(String localAmount, String debitAccount, 
            String terminalId, String groupReference, String transactionType){
        PaymentOrderRecord firstReverse = new PaymentOrderRecord(this);
      
        firstReverse.setPaymentOrderProduct(TRANSFER_PRODUCT);
        firstReverse.setOrderingCustomer(ORDERING_CUSTOMER);
        
        firstReverse.setPaymentAmount(localAmount);
        firstReverse.setDebitAccount(debitAccount);
        firstReverse.setCreditAccount(SAR_MEMO_ACCT);
        firstReverse.setPaymentCurrency("SAR");
        
        firstReverse.addNarrative(transactionType);
        firstReverse.getLocalRefField("L.TERMINAL.ID").setValue(terminalId);
        firstReverse.getLocalRefField("L.REF.GROUP").setValue(groupReference);
        
        
        return firstReverse.toStructure();
        
    }
    
    public TStructure secondReverseForeignTransaction(String currency, String rate, String foreignAmount, 
            String terminalId, String groupReference, String transactionType){
        PaymentOrderRecord secondReverse = new PaymentOrderRecord(this);
        
        secondReverse.setPaymentOrderProduct(TRANSFER_PRODUCT);
        secondReverse.setOrderingCustomer(ORDERING_CUSTOMER);
        
        secondReverse.setDebitAccount(currency + FRGN_MEMO_ACCT);
        secondReverse.setCreditAccount(currency + POOL_ACCOUNT);
        secondReverse.setPaymentAmount(foreignAmount);
        secondReverse.setPaymentCurrency(currency);
        
        secondReverse.addNarrative(transactionType);
        secondReverse.getLocalRefField("L.TERMINAL.ID").setValue(terminalId);
        secondReverse.getLocalRefField("L.REF.GROUP").setValue(groupReference);
        
        return secondReverse.toStructure();
        
    }
    
    public TStructure directReverseTransaction(String txnAmount, String debitAccount, String terminalId,
            String groupReference, String transactionType){
        PaymentOrderRecord directReverse = new PaymentOrderRecord(this);
        
        directReverse.setPaymentOrderProduct(TRANSFER_PRODUCT);
        directReverse.setOrderingCustomer(ORDERING_CUSTOMER);
        
        directReverse.setPaymentAmount(txnAmount);
        directReverse.setDebitAccount(debitAccount);
        directReverse.setCreditAccount("SAR" + POOL_ACCOUNT);
        directReverse.setPaymentCurrency("SAR");
        
        directReverse.addNarrative(transactionType);
        directReverse.getLocalRefField("L.TERMINAL.ID").setValue(terminalId);
        directReverse.getLocalRefField("L.REF.GROUP").setValue(groupReference);
        
        return directReverse.toStructure();
        
    }
    
    public TStructure firstInternationalTransaction(String forgeinAmount, String toSARRate, String txnCurrency, 
            String terminalId, String groupReference, String transactionType){
        PaymentOrderRecord firstTxnInt = new PaymentOrderRecord(this);
        firstTxnInt.setPaymentOrderProduct(TRANSFER_PRODUCT);
        firstTxnInt.setOrderingCustomer(ORDERING_CUSTOMER);
        
        firstTxnInt.setDebitAccount(txnCurrency + POOL_ACCOUNT);
        firstTxnInt.setCreditAccount(txnCurrency + FRGN_MEMO_ACCT);
        firstTxnInt.setPaymentAmount(forgeinAmount);
        firstTxnInt.setPaymentCurrency(txnCurrency);
        
        firstTxnInt.addNarrative(transactionType);
        firstTxnInt.getLocalRefField("L.TERMINAL.ID").setValue(terminalId);
        firstTxnInt.getLocalRefField("L.REF.GROUP").setValue(groupReference);
        
        return firstTxnInt.toStructure();
        
    }
    
    public TStructure secondInternationalTransaction(String usdAmount, String toUSDRate, String terminalId, 
            String groupReference, String transactionType){
        
        PaymentOrderRecord secondTxnInt = new PaymentOrderRecord(this);
        secondTxnInt.setPaymentOrderProduct(TRANSFER_PRODUCT);
        secondTxnInt.setOrderingCustomer(ORDERING_CUSTOMER);
        
        secondTxnInt.setDebitAccount("USD" + FRGN_MEMO_ACCT);
        secondTxnInt.setCreditAccount(ATM_POS_INT_ACCT);
        secondTxnInt.setPaymentCurrency("USD");
        secondTxnInt.setPaymentAmount(usdAmount);
        
        secondTxnInt.addNarrative(transactionType);
        secondTxnInt.getLocalRefField("L.TERMINAL.ID").setValue(terminalId);
        secondTxnInt.getLocalRefField("L.REF.GROUP").setValue(groupReference);
        
        return secondTxnInt.toStructure();
    }
    
    public TStructure directInternationalTxn(String usdAmount, String terminalId, String groupReference, String transactionType){
        PaymentOrderRecord directIntTxn = new PaymentOrderRecord(this);
        directIntTxn.setPaymentOrderProduct(TRANSFER_PRODUCT);
        directIntTxn.setOrderingCustomer(ORDERING_CUSTOMER);
        
        directIntTxn.setDebitAccount("USD" + POOL_ACCOUNT);
        directIntTxn.setCreditAccount(ATM_POS_INT_ACCT);
        directIntTxn.setPaymentAmount(usdAmount);
        directIntTxn.setPaymentCurrency("USD");
        
        directIntTxn.addNarrative(transactionType);
        directIntTxn.getLocalRefField("L.TERMINAL.ID").setValue(terminalId);
        directIntTxn.getLocalRefField("L.REF.GROUP").setValue(groupReference);
        
        return directIntTxn.toStructure();
       
    }
    
    public TStructure firstInternationalReverseTxn(String usdAmount, String toUSDRate, String terminalId, 
            String groupReference, String transactionType){
        
        PaymentOrderRecord firstIntReversal = new PaymentOrderRecord(this);
        firstIntReversal.setPaymentOrderProduct(TRANSFER_PRODUCT);
        firstIntReversal.setOrderingCustomer(ORDERING_CUSTOMER);
        
        firstIntReversal.setDebitAccount(ATM_POS_INT_ACCT);
        firstIntReversal.setCreditAccount("USD" + FRGN_MEMO_ACCT);
        firstIntReversal.setPaymentCurrency("USD");
        firstIntReversal.setPaymentAmount(usdAmount);
        
        firstIntReversal.addNarrative(transactionType);
        firstIntReversal.getLocalRefField("L.TERMINAL.ID").setValue(terminalId);
        firstIntReversal.getLocalRefField("L.REF.GROUP").setValue(groupReference);
        
        return firstIntReversal.toStructure();   
    }
    
    public TStructure secondInternationalReverseTxn(String currency, String foreignAmount, String toSARRate, 
            String terminalId, String groupReference, String transactionType){
        
        PaymentOrderRecord secondIntReversal = new PaymentOrderRecord(this);
        secondIntReversal.setPaymentOrderProduct(TRANSFER_PRODUCT);
        secondIntReversal.setOrderingCustomer(ORDERING_CUSTOMER);
        
        secondIntReversal.setDebitAccount(currency + FRGN_MEMO_ACCT);
        secondIntReversal.setCreditAccount(currency + POOL_ACCOUNT);
        secondIntReversal.setPaymentCurrency(currency);
        secondIntReversal.setPaymentAmount(foreignAmount);
        
        secondIntReversal.addNarrative(transactionType);
        secondIntReversal.getLocalRefField("L.TERMINAL.ID").setValue(terminalId);
        secondIntReversal.getLocalRefField("L.REF.GROUP").setValue(groupReference);
        
        return secondIntReversal.toStructure();
    }
    
    public TStructure directInternationalReversal(String usdAmount, String terminalId, String groupReference, String transactionType){
        PaymentOrderRecord directReversal = new PaymentOrderRecord(this);
        directReversal.setPaymentOrderProduct(TRANSFER_PRODUCT);
        directReversal.setOrderingCustomer(ORDERING_CUSTOMER);
        
        directReversal.setDebitAccount(ATM_POS_INT_ACCT);
        directReversal.setCreditAccount("USD" + POOL_ACCOUNT);
        directReversal.setPaymentCurrency("USD");
        directReversal.setPaymentAmount(usdAmount);
        
        directReversal.addNarrative(transactionType);
        directReversal.getLocalRefField("L.TERMINAL.ID").setValue(terminalId);
        directReversal.getLocalRefField("L.REF.GROUP").setValue(groupReference);
        
        return directReversal.toStructure();
        
    }
    
    public TStructure firstForeignFeesTxn(String txnCurrency, String forgeinAmount, String toSARRate, 
            String terminalId, String groupReference, String transactionType){
        PaymentOrderRecord firstFeesTxn = new PaymentOrderRecord(this);
        firstFeesTxn.setPaymentOrderProduct(FEES_PRODUCT);
        firstFeesTxn.setOrderingCustomer(ORDERING_CUSTOMER);
        
        firstFeesTxn.setDebitAccount(txnCurrency + POOL_ACCOUNT);
        firstFeesTxn.setCreditAccount(txnCurrency + FRGN_MEMO_ACCT);
        firstFeesTxn.setPaymentCurrency(txnCurrency);
        firstFeesTxn.setPaymentAmount(forgeinAmount);
        
        firstFeesTxn.addNarrative( "FEE" + transactionType);
        firstFeesTxn.getLocalRefField("L.TERMINAL.ID").setValue(terminalId);
        firstFeesTxn.getLocalRefField("L.REF.GROUP").setValue(groupReference);
        
        return firstFeesTxn.toStructure();
       
    }
    
    public TStructure secondForeginFeesTxn(String sarAmount, String terminalId, String groupReference, String transactionType){
        PaymentOrderRecord secondFeesTxn = new PaymentOrderRecord(this);
        secondFeesTxn.setPaymentOrderProduct(FEES_PRODUCT);
        secondFeesTxn.setOrderingCustomer(ORDERING_CUSTOMER);
        
        secondFeesTxn.setDebitAccount(SAR_MEMO_ACCT);
        secondFeesTxn.setCreditPl(FEE_PL);
        secondFeesTxn.setCreditCurrency("SAR");
        secondFeesTxn.setPaymentCurrency("SAR");
        secondFeesTxn.setPaymentAmount(sarAmount);
        
        secondFeesTxn.addNarrative("FEE" + transactionType);
        secondFeesTxn.getLocalRefField("L.TERMINAL.ID").setValue(terminalId);
        secondFeesTxn.getLocalRefField("L.REF.GROUP").setValue(groupReference);
        
        return secondFeesTxn.toStructure();
    }
    
    public TStructure directFeesTxn(String feesAmount, String terminalId, String groupReference, String transactionType){
        PaymentOrderRecord directFeesTxn = new PaymentOrderRecord(this);
        directFeesTxn.setPaymentOrderProduct(FEES_PRODUCT);
        directFeesTxn.setOrderingCustomer(ORDERING_CUSTOMER);
        
        directFeesTxn.setCreditPl(FEE_PL);
        directFeesTxn.setDebitAccount("SAR" + POOL_ACCOUNT);
        directFeesTxn.setPaymentCurrency("SAR");
        directFeesTxn.setCreditCurrency("SAR");
        directFeesTxn.setPaymentAmount(feesAmount);
        
        directFeesTxn.addNarrative("FEE" + transactionType);
        directFeesTxn.getLocalRefField("L.TERMINAL.ID").setValue(terminalId);
        directFeesTxn.getLocalRefField("L.REF.GROUP").setValue(groupReference);
        
        return directFeesTxn.toStructure();
    }
    
    public TStructure firstFeesReverseTxn(String sarAmount, String terminalId, String groupReference, String transactionType){
        PaymentOrderRecord firstFeesReverseTxn = new PaymentOrderRecord(this);
        firstFeesReverseTxn.setPaymentOrderProduct(FEES_PRODUCT);
        firstFeesReverseTxn.setOrderingCustomer(ORDERING_CUSTOMER);
        
        firstFeesReverseTxn.setDebitPl(FEE_PL);
        firstFeesReverseTxn.setCreditAccount(SAR_MEMO_ACCT);
        firstFeesReverseTxn.setPaymentCurrency("SAR");
        firstFeesReverseTxn.setDebitCcy("SAR");
        firstFeesReverseTxn.setPaymentAmount(sarAmount);
        
        firstFeesReverseTxn.addNarrative("FEE" + transactionType);
        firstFeesReverseTxn.getLocalRefField("L.TERMINAL.ID").setValue(terminalId);
        firstFeesReverseTxn.getLocalRefField("L.REF.GROUP").setValue(groupReference);
        
        return firstFeesReverseTxn.toStructure();
        
    }
    
    public TStructure secondFeesReverseTxn(String currency, String foreignAmount, String toSARRate, 
            String terminalId, String groupReference, String transactionType){
        PaymentOrderRecord secondFeesReverseTxn = new PaymentOrderRecord(this);
        secondFeesReverseTxn.setPaymentOrderProduct(FEES_PRODUCT);
        secondFeesReverseTxn.setOrderingCustomer(ORDERING_CUSTOMER);
        
        secondFeesReverseTxn.setDebitAccount(currency + FRGN_MEMO_ACCT);
        secondFeesReverseTxn.setCreditAccount(currency + POOL_ACCOUNT);
        secondFeesReverseTxn.setPaymentCurrency(currency);
        secondFeesReverseTxn.setPaymentAmount(foreignAmount);
        
        secondFeesReverseTxn.addNarrative("FEE" + transactionType);
        secondFeesReverseTxn.getLocalRefField("L.TERMINAL.ID").setValue(terminalId);
        secondFeesReverseTxn.getLocalRefField("L.REF.GROUP").setValue(groupReference);
        
        return secondFeesReverseTxn.toStructure();
        
    }
    
    public TStructure directFeesReverseTxn(String sarAmount, String terminalId, String groupReference, String transactionType){
        PaymentOrderRecord directFeesReverse = new PaymentOrderRecord(this);
        directFeesReverse.setPaymentOrderProduct(FEES_PRODUCT);
        directFeesReverse.setOrderingCustomer(ORDERING_CUSTOMER);
        
        directFeesReverse.setDebitAccount("SAR" + POOL_ACCOUNT);
        directFeesReverse.setCreditPl(FEE_PL);
        directFeesReverse.setCreditCurrency("SAR");
        directFeesReverse.setPaymentCurrency("SAR");
        directFeesReverse.setPaymentAmount(sarAmount);
        
        directFeesReverse.addNarrative("FEE" + transactionType);
        directFeesReverse.getLocalRefField("L.TERMINAL.ID").setValue(terminalId);
        directFeesReverse.getLocalRefField("L.REF.GROUP").setValue(groupReference);
        
        return directFeesReverse.toStructure();
        
    }
    
    public TStructure firstForeignVatTxn(String txnCurrency, String forgeinAmount, String toSARRate,
            String terminalId, String groupReference, String transactionType){
        PaymentOrderRecord firstVatTxn = new PaymentOrderRecord(this);
        firstVatTxn.setPaymentOrderProduct(VAT_PRODUCT);
        firstVatTxn.setOrderingCustomer(ORDERING_CUSTOMER);
        
        firstVatTxn.setDebitAccount(txnCurrency + POOL_ACCOUNT);
        firstVatTxn.setCreditAccount(txnCurrency + FRGN_MEMO_ACCT);
        firstVatTxn.setPaymentCurrency(txnCurrency);
        firstVatTxn.setPaymentAmount(forgeinAmount);
       
        firstVatTxn.addNarrative("VAT" + transactionType);
        firstVatTxn.getLocalRefField("L.TERMINAL.ID").setValue(terminalId);
        firstVatTxn.getLocalRefField("L.REF.GROUP").setValue(groupReference);
        
        return firstVatTxn.toStructure();
    }
    
    public TStructure secondForeginVatTxn(String sarAmount, String terminalId, String groupReference, String transactionType){
        PaymentOrderRecord secondVatTxn = new PaymentOrderRecord(this);
        secondVatTxn.setPaymentOrderProduct(VAT_PRODUCT);
        secondVatTxn.setOrderingCustomer(ORDERING_CUSTOMER);
        
        secondVatTxn.setDebitAccount(SAR_MEMO_ACCT);
        secondVatTxn.setCreditAccount(VAT_ACCOUNT);
        secondVatTxn.setPaymentCurrency("SAR");
        secondVatTxn.setPaymentAmount(sarAmount);
        
        secondVatTxn.addNarrative("VAT" + transactionType);
        secondVatTxn.getLocalRefField("L.TERMINAL.ID").setValue(terminalId);
        secondVatTxn.getLocalRefField("L.REF.GROUP").setValue(groupReference);
       
        return secondVatTxn.toStructure();
    }
    
    public TStructure directVatTxn(String sarAmount, String terminalId, String groupReference, String transactionType){
        PaymentOrderRecord directVatTxn = new PaymentOrderRecord(this);
        directVatTxn.setPaymentOrderProduct(VAT_PRODUCT);
        directVatTxn.setOrderingCustomer(ORDERING_CUSTOMER);
        
        directVatTxn.setDebitAccount("SAR" + POOL_ACCOUNT);
        directVatTxn.setCreditAccount(VAT_ACCOUNT);
        directVatTxn.setPaymentCurrency("SAR");
        directVatTxn.setPaymentAmount(sarAmount);
        
        directVatTxn.addNarrative("VAT" + transactionType);
        directVatTxn.getLocalRefField("L.TERMINAL.ID").setValue(terminalId);
        directVatTxn.getLocalRefField("L.REF.GROUP").setValue(groupReference);
       
        return directVatTxn.toStructure();
        
    }
    
    public TStructure firstVatReverseTxn(String sarAmount, String terminalId, String groupReference, String transactionType){
        PaymentOrderRecord firstVatReverse = new PaymentOrderRecord(this);
        firstVatReverse.setPaymentOrderProduct(VAT_PRODUCT);
        firstVatReverse.setOrderingCustomer(ORDERING_CUSTOMER);
        
        firstVatReverse.setDebitAccount(VAT_ACCOUNT);
        firstVatReverse.setCreditAccount(SAR_MEMO_ACCT);
        firstVatReverse.setPaymentAmount(sarAmount);
        firstVatReverse.setPaymentCurrency("SAR");
        
        firstVatReverse.addNarrative("VAT" + transactionType);
        firstVatReverse.getLocalRefField("L.TERMINAL.ID").setValue(terminalId);
        firstVatReverse.getLocalRefField("L.REF.GROUP").setValue(groupReference);
        
        return firstVatReverse.toStructure();
        
    }
    
    public TStructure secondVatReverseTxn(String currency, String foreignAmount, String terminalId, String toSARRate,
            String groupReference, String transactionType){
        PaymentOrderRecord secondVatReverse = new PaymentOrderRecord(this);
        secondVatReverse.setPaymentOrderProduct(VAT_PRODUCT);
        secondVatReverse.setOrderingCustomer(ORDERING_CUSTOMER);
        
        secondVatReverse.setDebitAccount(currency + FRGN_MEMO_ACCT);
        secondVatReverse.setCreditAccount(currency + POOL_ACCOUNT);
        secondVatReverse.setPaymentCurrency(currency);
        secondVatReverse.setPaymentAmount(foreignAmount);
        
        secondVatReverse.addNarrative("VAT" + transactionType);
        secondVatReverse.getLocalRefField("L.TERMINAL.ID").setValue(terminalId);
        secondVatReverse.getLocalRefField("L.REF.GROUP").setValue(groupReference);
        
        return secondVatReverse.toStructure();
        
    }
    
    public TStructure directVatReverseTxn(String sarAmount, String terminalId, String groupReference, String transactionType){
        PaymentOrderRecord directVatReverse = new PaymentOrderRecord(this);
        directVatReverse.setPaymentOrderProduct(VAT_PRODUCT);
        directVatReverse.setOrderingCustomer(ORDERING_CUSTOMER);
        
        directVatReverse.setDebitAccount("SAR" + POOL_ACCOUNT);
        directVatReverse.setCreditAccount(VAT_ACCOUNT);
        directVatReverse.setPaymentCurrency("SAR");
        directVatReverse.setPaymentAmount(sarAmount);
        
        directVatReverse.addNarrative("VAT" + transactionType);
        directVatReverse.getLocalRefField("L.TERMINAL.ID").setValue(terminalId);
        directVatReverse.getLocalRefField("L.REF.GROUP").setValue(groupReference);
        
        return directVatReverse.toStructure();
    }
    
    public TStructure updateCurrencyRate(String currencyId, String rate){
        DataAccess data = new DataAccess(this);
        CurrencyRecord currency = new CurrencyRecord(data.getRecord("CURRENCY", currencyId));
        String buyRate = currency.getCurrencyMarket(2).getBuyRate().getValue();
        String sellRate = currency.getCurrencyMarket(2).getSellRate().getValue();
        if(rate.equals(buyRate) && rate.equals(sellRate)){
            return null;
        }
        else{
            CurrencyRecord currencyUpdate = new CurrencyRecord();
            CurrencyMarketClass newRates = new CurrencyMarketClass();
            newRates.setBuyRate(rate);
            newRates.setSellRate(rate);
            currencyUpdate.setCurrencyMarket(newRates, 2);
            return currencyUpdate.toStructure();
        }
        
    }
   
    public SynchronousTransactionData buildCurrencyTransactionData(String currencyId){
        SynchronousTransactionData currencyTransactionData = new SynchronousTransactionData();
        
        currencyTransactionData.setFunction("INPUT");
        currencyTransactionData.setSourceId("PAYABLE.OFS");
        currencyTransactionData.setVersionId("CURRENCY,RAD");
        currencyTransactionData.setTransactionId(currencyId);
        currencyTransactionData.setNumberOfAuthoriser("0");
        
        return currencyTransactionData;
        
    }
     
    public void appendLog(String text) {
        try {
            String home = System.getenv("T24_HOME");
            String directory = home + "/&SAVEDLISTS&/";
            FileWriter writer = new FileWriter(directory + "CreatePayableTransactionLog.txt", true);

            writer.write(text);
            writer.write("\n");
            writer.close();
        } catch (Exception e) {
        }
    }
    
}
