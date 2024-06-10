package org.usth.ict.ulake.textr.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "file_index", uniqueConstraints = {
        @UniqueConstraint(columnNames = "core_id"),
        @UniqueConstraint(columnNames = "file_id")
})
public class IndexFiles {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @Column(name = "core_id")
    private String coreId;
    
    @NotNull
    @Column(name = "file_id")
    private Long fileId;
    
    @NotNull
    @Column(name = "size")
    private Long size;
    
    @NotNull
    @JsonIgnore
    @Column(name = "status")
    private IndexingStatus status;
    
    public IndexFiles(String coreId, Long fileId, Long size, IndexingStatus status) {
        this.coreId = coreId;
        this.fileId = fileId;
        this.size = size;
        this.status = status;
    }
}
