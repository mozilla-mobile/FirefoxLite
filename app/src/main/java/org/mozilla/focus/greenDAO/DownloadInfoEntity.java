package org.mozilla.focus.greenDAO;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by anlin on 27/07/2017.
 */

@Entity
public class DownloadInfoEntity {
    @Id(autoincrement = true)
    private Long id;

    @NotNull
    private Long downLoadId;

    @NotNull
    private String fileName;

    @Generated(hash = 954530575)
    public DownloadInfoEntity(Long id, @NotNull Long downLoadId,
            @NotNull String fileName) {
        this.id = id;
        this.downLoadId = downLoadId;
        this.fileName = fileName;
    }

    @Generated(hash = 1357263013)
    public DownloadInfoEntity() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDownLoadId() {
        return this.downLoadId;
    }

    public void setDownLoadId(Long downLoadId) {
        this.downLoadId = downLoadId;
    }

    public String getFileName() {
        return this.fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    
}
