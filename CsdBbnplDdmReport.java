package com.temenos.tdf.loanRestructure;

import com.temenos.t24.api.hook.system.ServiceLifecycle;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * TODO: Document me!
 *
 * @author mhismail
 *
 */

public class CsdBbnplDdmReport extends ServiceLifecycle {
    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        List<String> outList = new ArrayList<String>();
        outList.add("DUMMY");
        return outList;
    }

    @Override
    public void postUpdateRequest(String id, ServiceData serviceData, String controlItem,
            List<TransactionData> transactionData, List<TStructure> records) {

        DataAccess da = new DataAccess(this);
        String extractPath = "";
        String extractOutputPath = "";

        EbEmkHStaticParamRecord emkParam = new EbEmkHStaticParamRecord(da.getRecord("EB.EMK.H.STATIC.PARAM", "EMKAN"));

        for (DataNameClass dxn : emkParam.getDataName()) {
            String DataName = dxn.getDataName().getValue();
            if (DataName.equalsIgnoreCase("BNPL.DDM.PATH")) {
                extractPath = dxn.getDataValue(0).toString();
                extractOutputPath = dxn.getDataValue(1).toString();
            }
        }

        Session session = new Session(this);
        String today = session.getCurrentVariable("!TODAY");

        try {

            try (FileOutputStream fileOutputStream = new FileOutputStream(
                    extractOutputPath + "/T24_Retail_" + today + ".csv", true)) {

                StringBuilder fileContent = new StringBuilder();
                fileContent.append("arrangementId |instAmount |instDate ");
                fileContent.append("\n");

                String csvString = fileContent.toString();
                byte[] bytes = csvString.getBytes();
                fileOutputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }

            StringBuilder stringBuilder = new StringBuilder();
            DataOutputStream fileStream = null;
            File directoryPath = new File(extractPath + "/");
            File[] filesList = directoryPath.listFiles();
            File[] var19 = filesList;
            int listLength = filesList.length;

            for (int i = 0; i < listLength; ++i) {

                File file = var19[i];

                try {
                    Scanner scanner = new Scanner(file);

                    while (scanner.hasNextLine()) {
                        String input = scanner.nextLine();
                        stringBuilder.append(input);
                        stringBuilder.append("\n");
                    }

                    FileOutputStream fileOut = new FileOutputStream(extractOutputPath + "/T24_Retail_" + today + ".csv",
                            true);
                    fileStream = new DataOutputStream(fileOut);
                    fileStream.write(stringBuilder.toString().getBytes());
                    fileStream.flush();
                    fileStream.close();
                    stringBuilder = new StringBuilder();
                    scanner.close();
                } catch (Exception e) {
                } finally {
                    try {
                        if (fileStream != null) {
                            fileStream.close();
                        }
                    } catch (Exception var29) {
                    }

                }
                file.delete();

            }

        } catch (Exception var30) {
            System.out.println(var30);
        }

    }
}
