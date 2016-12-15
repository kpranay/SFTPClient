/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jschsftp;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

/**
 *
 * @author Pranay
 */
public class JSCHsftp {

    /**
     * @param args the command line arguments
     */
    private static final String strXlsPath = "bloodbanks_installation.xls";
    private static final String strLocalPath = "D:\\kp\\stable\\release\\h4a_ah_viswa";
    private static final int FOLDER_NAME_INDEX = 0;
    
    private Session mSession = null;
    private Channel mChannel = null;
    private ChannelSftp mChannelSftp = null;

    public static void main(String[] args) throws Exception{
        JSCHsftp mJSCHsftp = new JSCHsftp();
        mJSCHsftp.createConnection();
        FileInputStream fin = new FileInputStream(strXlsPath);
        HSSFWorkbook myWorkBook = new HSSFWorkbook (fin);
        HSSFSheet mySheet = myWorkBook.getSheetAt(0);
        Iterator<Row> rowIterator = mySheet.iterator(); 
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next(); 
            String strFolderName = getCellValue(row.getCell(FOLDER_NAME_INDEX));
            System.out.println("Home Directory >> "+mJSCHsftp.mChannelSftp.getHome());
            System.out.println("Present Directory >> "+mJSCHsftp.mChannelSftp.pwd());
            if(mJSCHsftp.mChannelSftp != null)
            mJSCHsftp.startTransfer(mJSCHsftp.mChannelSftp, "./"+strFolderName, strLocalPath);
        }
        mJSCHsftp.closeConnections();
    }

    private void createConnection(){
        String SFTPHOST = "host";
        int SFTPPORT = 22;
        String SFTPUSER = "username";
        String SFTPPASS = "password";
        String SFTPWORKINGDIR = "./public_html/";
       
        System.out.println("preparing the host information for sftp.");
        
        try {
             JSch jsch = new JSch();
            mSession = jsch.getSession(SFTPUSER, SFTPHOST, SFTPPORT);
            mSession.setPassword(SFTPPASS);
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            mSession.setConfig(config);
            mSession.connect();
            System.out.println("Host connected.");
            mChannel = mSession.openChannel("sftp");
            mChannel.connect();
            System.out.println("sftp channel opened and connected.");
            mChannelSftp = (ChannelSftp) mChannel;
            mChannelSftp.cd(SFTPWORKINGDIR);
        } catch (Exception e) {
            System.out.println("createConnection :: Exception found while tranfer the response."+e.getMessage());
        }
    }
    private void closeConnections(){
        mChannelSftp.exit();
        System.out.println("sftp Channel exited.");
        mChannel.disconnect();
        System.out.println("Channel disconnected.");
        mSession.disconnect();
        System.out.println("Host Session disconnected.");
    }
    private void startTransfer(ChannelSftp channelSftp, String strRemoteDir, String strLocalDir){
        try {
            channelSftp.cd(channelSftp.getHome()+"/public_html/");
            createDirectoryIfNotExist(channelSftp, strRemoteDir);
            uploadDirectory(channelSftp,strRemoteDir,strLocalDir);
            System.out.println("File transfered successfully to host.");
        } catch (Exception ex) {
             System.out.println("startTransfer :: Exception found while tranfer the response."+ex.getMessage());
        }
    }
    
    
    private boolean uploadSingleFile(ChannelSftp channelSftp, String localFileName){
        try {
            File f = new File(localFileName);
            long lastMod = getFileLastModTime(channelSftp,f.getName());
            if(f.lastModified() > lastMod){
                channelSftp.put(new FileInputStream(f), f.getName());
                System.out.println("UPLOADED a file to: " + channelSftp.pwd()+"/"+f.getName());
            }else{
                System.out.println("NOT UPLOADED a file because File on server is latest : "+ channelSftp.pwd()+"/"+f.getName());
            }
            return true;
        } catch (Exception e) {
            System.out.println("COULD NOT upload the file: "+ localFileName);
            e.printStackTrace();
            return false;
        }
    }
    private void uploadDirectory(ChannelSftp channelSftp,
        String remoteDirPath, String localParentDir)
        throws IOException,SftpException {
 
        System.out.println("LISTING directory: " + localParentDir);

        File localDir = new File(localParentDir);
        File[] subFiles = localDir.listFiles();
        if (subFiles != null && subFiles.length > 0) {
            for (File item : subFiles) {
                String remoteFilePath = channelSftp.pwd() + "/" + item.getName();
                if (item.isFile()) {
                    // upload the file
                    String localFilePath = item.getAbsolutePath();
                    System.out.println("About to upload the file: " + localFilePath);
                    boolean uploaded = uploadSingleFile(channelSftp, localFilePath);
                    if (uploaded) {
                       
                    } else {
                       
                    }
                } else {
                    createDirectoryIfNotExist(channelSftp,item.getName());
                    localParentDir = item.getAbsolutePath();
                    uploadDirectory(channelSftp, remoteDirPath, localParentDir);
                    channelSftp.cd("..");
                }
            }
        }
    }
    
    private long getFileLastModTime(ChannelSftp sftp, String strFileName){
        SftpATTRS mFileAttr = null;
        try{
            mFileAttr = sftp.lstat(strFileName);
        }catch(SftpException se){
            
        }
        /// getMTime deliver a value reduced by the milliseconds
        
        return mFileAttr == null ? 0 : mFileAttr.getMTime()* 1000L;
    }
    
    private static String getCellValue(Cell cell){
        
            
        String strCellValue = "";
        if(cell == null)
            return "";
        switch (cell.getCellType()) {
            case Cell.CELL_TYPE_STRING: 
                strCellValue = cell.getStringCellValue();
                break; 
            case Cell.CELL_TYPE_NUMERIC: 
                strCellValue = ""+cell.getNumericCellValue();
                break; 
            case Cell.CELL_TYPE_BOOLEAN: 
                strCellValue = ""+cell.getBooleanCellValue();
                break; 
            default : break;
        }
        return strCellValue;
    }
    
    private void createDirectoryIfNotExist(ChannelSftp channelSftp , String strDirName) throws SftpException{
        try{
            channelSftp.lstat(strDirName);
            System.out.println("Already Exists directory: "+ strDirName);

        }
        catch(SftpException se){
            try {
                System.out.println("Creating the directory: "+ channelSftp.pwd()+"\n"+se.getLocalizedMessage());
                // create directory on the server
                channelSftp.mkdir(strDirName);
                System.out.println("CREATED the directory: "+ strDirName);
            } catch (SftpException se2) {
            System.out.println("COULD NOT create the directory: "+ channelSftp.pwd()+"\n"+se2.getLocalizedMessage());
            }
        }finally{
            channelSftp.cd(strDirName);
        }
    }
}
