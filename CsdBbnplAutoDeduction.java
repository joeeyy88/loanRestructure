package com.temenos.tdf.loanRestructure;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import com.temenos.api.TStructure;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaaccountdetails.BillIdClass;
import com.temenos.t24.api.records.aaaccountdetails.BillPayDateClass;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aabilldetails.AaBillDetailsRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * TODO: Document me!
 *
 * @author ykumar, anouh, mhismail
 *
 */
public class CsdBbnplAutoDeduction extends ServiceLifecycle {

    DataAccess da = new DataAccess(this);

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        

        return da.selectRecords("", "EB.BNPL.L.AUTO.DEDUCT.ID", "", "");

    }

    @Override
    public void postUpdateRequest(String arrangementId, ServiceData serviceData, String controlItem,
            List<TransactionData> transactionData, List<TStructure> records) {

        String infoBillId = "";
        String instAmount = "";
        String instDate = "";
        String output = "";

        Contract contract = new Contract(this);
        contract.setContractId(arrangementId);
        try {
            AaAccountDetailsRecord accDetsRec = contract.getAccountDetailsRecord();
            List<BillPayDateClass> billPayDateList = accDetsRec.getBillPayDate();
            int payDateSize = billPayDateList.size();

            List<BillIdClass> billIds = billPayDateList.get(payDateSize - 1).getBillId();
            int billIdSize = billIds.size();
            for (int billCount = billIdSize - 1; billCount >= 0; billCount--) {

                String billType = billIds.get(billCount).getBillType().getValue();
                if (billType.equalsIgnoreCase("INSTALLMENT")) {
                    infoBillId = billIds.get(billCount).getBillId().getValue();
                    break;

                }
            }

            AaBillDetailsRecord aaBillDets = new AaBillDetailsRecord(
                    da.getRecord("", "AA.BILL.DETAILS", "", infoBillId));
            
            // -------------------------------------------------------------------------------
            AaArrangementRecord aaRec = new AaArrangementRecord(da.getRecord("AA.ARRANGEMENT", arrangementId));
            String customerId = aaRec.getCustomer(0).getCustomer().getValue();
            CustomerRecord customerRec = new CustomerRecord(da.getRecord("CUSTOMER", customerId));
            String nationalId = customerRec.getLocalRefField("CSD.INDV.ID.NUM").getValue();
            String losId = aaRec.getAltIdType(0).getAlternateId().getValue();
            // -------------------------------------------------------------------------------

            instAmount = aaBillDets.getOrTotalAmount().getValue();
            instDate = aaBillDets.getPaymentDate().getValue();
            output = "|" + arrangementId + "|" + instAmount + "|" + instDate + "|" + nationalId + "|" + losId;

            this.callFileWrite(output, serviceData);
        } catch (Exception e) {

        }

    }
    
    private void callFileWrite(String output, ServiceData serviceData) {

        String extractPath = null;
        String ddmFileName = "/T24_Retail_";
        
        EbEmkHStaticParamRecord emkParam = new EbEmkHStaticParamRecord(da.getRecord("EB.EMK.H.STATIC.PARAM", "EMKAN"));

        for (DataNameClass dxn : emkParam.getDataName()) {
            String DataName = dxn.getDataName().getValue();
            if (DataName.equalsIgnoreCase("BNPL.DDM.PATH")) {
                extractPath = dxn.getDataValue(0).toString();
            }
        }

        FileWriter csvWriter = null;
        String fileName = extractPath + ddmFileName + "-Temp" + serviceData.getSessionId() + ".csv";

        try {

            csvWriter = new FileWriter(fileName, true);

            csvWriter.write(output);
            csvWriter.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (csvWriter != null) {
                    csvWriter.flush();
                    csvWriter.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }
    

    @Override
    public void processSingleThreaded(ServiceData serviceData) {
        // TODO Auto-generated method stub

        String extractPath = "";
        Session session = new Session(this);
        String today = session.getCurrentVariable("!TODAY");
        String ddmFileName = "/T24_Retail_";

        EbEmkHStaticParamRecord emkParam = new EbEmkHStaticParamRecord(da.getRecord("EB.EMK.H.STATIC.PARAM", "EMKAN"));

        for (DataNameClass dxn : emkParam.getDataName()) {
            String DataName = dxn.getDataName().getValue();
            if (DataName.equalsIgnoreCase("BNPL.DDM.PATH")) {
                extractPath = dxn.getDataValue(0).toString();
            }
        }

        String directoryPath = extractPath;
        String outputFilePath = extractPath + ddmFileName + "-" + today + ".csv";
        String filePrefix = ddmFileName + "-Temp";
        File directory = new File(directoryPath);

        File[] files = directory.listFiles((dir, name) -> name.startsWith(filePrefix) && name.endsWith(".csv"));

        if (files == null || files.length == 0) {

            return;
        }

        // try (FileOutputStream fos = new FileOutputStream(outputFilePath, true); // Append
        try (FileOutputStream fos = new FileOutputStream(outputFilePath); // Overwrite mode

                BufferedOutputStream bos = new BufferedOutputStream(fos);
                PrintWriter writer = new PrintWriter(bos)) {
            String yHeaderFlag = "N";

            for (File file : files) {

                if (file.isFile()) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        String Header = "ARRANGEMENT ID,INSTALLMENT AMOUNT,INSTALLMENT DATE,NATIONAL ID,LOS ID";

                        while ((line = reader.readLine()) != null) {

                            if (yHeaderFlag.equalsIgnoreCase("N")) {
                                writer.println(Header);
                            }

                            writer.println(line);
                            yHeaderFlag = "Y";

                        }

                    } catch (IOException e) {

                        e.printStackTrace();
                    }

                    if (file.delete()) {

                    } else {

                    }

                }

            }

        } catch (IOException e) {

            e.printStackTrace();

        }

    }
}
