package com.vipgp.tinyurl.dubbo.provider.persistence;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.List;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/4/27 13:49
 */
@Slf4j
@Component
public class FileSnap implements SnapShot {

    @NacosValue(value = "${snapshot.log.dir}",autoRefreshed = true)
    private String snapshotDirPath;
    private File snapshotDir;
    private File snapFileWrite;

    private volatile FileOutputStream fos=null;
    private volatile BufferedOutputStream bos=null;


    @PostConstruct
    private void init(){
        this.snapshotDir=new File(snapshotDirPath);
    }


    @Override
    public String deserialize() {
        return null;
    }

    @Override
    public void serialize(SnapShotModel model) throws IOException {

        if(snapFileWrite==null){
            snapFileWrite=new File(snapshotDir,Util.makeSnapShotName(model.getLastXid()));
            fos=new FileOutputStream(snapFileWrite);
            bos=new BufferedOutputStream(fos);
        }

        byte[] data=ProtostuffUtils.serialize(model);
        bos.write(data);
        bos.flush();
    }

    @Override
    public long getLastProcessedXid(){
        List<File> files=Util.sortDataDir(snapshotDir.listFiles(),Const.SNAP_FILE_PREFIX,false);
        if(files==null || files.size()==0){
            return 0;
        }else {
            File snapshot=files.get(0);
            try(InputStream snapBis=new BufferedInputStream(new FileInputStream(snapshot))){
                byte[] data=new byte[snapBis.available()];
                snapBis.read(data);
                SnapShotModel model=ProtostuffUtils.deserialize(data,SnapShotModel.class);
                return model.getLastXid();
            }catch (IOException ex){
                log.error("problem reading snap file "+snapshot,ex);
                return -1;
            }

        }
    }

    @Override
    public void archive(File backupDir, String suffix){
        Util.moveTo(backupDir,snapshotDir.listFiles(),Const.SNAP_FILE_PREFIX,suffix);
    }
}
